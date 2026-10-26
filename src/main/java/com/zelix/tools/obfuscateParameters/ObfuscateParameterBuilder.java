package com.zelix.tools.obfuscateParameters;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.utils.PrimitiveBoxingUtils;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.CountingSet;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.HierarchicalMultiMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.Workspace;

import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class ObfuscateParameterBuilder {
	private final Map<MethodNode, MethodSignature> baselineSignatureByMethod = GlobalUtils.newHashMap();
	private final DualKeyMap<String, MethodSignature, MethodSignature> oldToNewMethodSignatureMap;
	private final DualKeyMap<String, MethodSignature, MethodSignature> newToOldMethodSignatureMap;

	public ObfuscateParameterBuilder(
		ClassNode[] classNodes,
		Workspace workspace,
		MultiMap<ClassNode, MethodNode> markedMethodsByClass,
		HierarchicalMultiMap<ClassNode, MethodNode, AbstractMethodNode> markedMethodsByCaller,
		Map<String, String> classNameRemap,
		DualKeyMap<String, MethodSignature, MethodSignature> oldToNewMethodSignatureMap,
		DualKeyMap<String, MethodSignature, MethodSignature> newToOldMethodSignatureMap,
		Map<MethodNode, LocalVariableOperand> decryptionLocalVarsByMethod,
		Set<MethodNode> generatedMethods,
		Inheritance inheritance,
		ClassProvider classProvider,
		ScriptContext scriptContext,
		boolean shuffleLocals
	) {
		this.oldToNewMethodSignatureMap = oldToNewMethodSignatureMap;
		this.newToOldMethodSignatureMap = newToOldMethodSignatureMap;
		this.captureBaselineSignatures(classNodes);
		HashSet<ClassNode> classesToProcess = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(classNodes.length));
		classesToProcess.addAll(markedMethodsByClass.keySet());
		classesToProcess.addAll(markedMethodsByCaller.keySet());
		if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
			Vector<ProcessingException> processingErrors = new Vector<>();
			classesToProcess.parallelStream().forEach(classNode -> {
				try {
					this.obfuscateParametersInClass(classNode, markedMethodsByClass.get(classNode), markedMethodsByCaller.get(classNode), generatedMethods, decryptionLocalVarsByMethod, workspace, inheritance, classProvider, shuffleLocals);
				} catch (ProcessingException processingException) {
					processingErrors.add(processingException);
				}
			});
			if (!processingErrors.isEmpty()) {
				throw (ProcessingException)processingErrors.get(0);
			}
		} else {
			for (ClassNode classNode : classesToProcess) {
				this.obfuscateParametersInClass(classNode, markedMethodsByClass.get(classNode), markedMethodsByCaller.get(classNode), generatedMethods, decryptionLocalVarsByMethod, workspace, inheritance, classProvider, shuffleLocals);
			}
		}

		int classCount = 0;
		int methodCount = 0;
		PrintWriter writer = scriptContext.getWriter();

		for (Entry<ClassNode, List<MethodNode>> markedMethodsEntry : markedMethodsByClass.entrySet()) {
			classCount++;
			if (scriptContext.isVerbose()) {
				writer.println("\tObfuscated parameter lists in class '" + ExcludeStatementManager.getFormattedClassSignature(markedMethodsEntry.getKey(), workspace, false) + "'");
			}

			ClassNode markedClass = markedMethodsEntry.getKey();

			for (MethodNode methodNode : markedMethodsEntry.getValue()) {
				if (Options.markParamsAsVarargs && markedClass.isJava5OrNewer()) {
					methodNode.markVarargs();
				}

				String originalParameters = MethodSignature.formatRemappedParameters(methodNode.getDescriptor(), classNameRemap);
				String obfuscatedDescriptor = "([Ljava/lang/Object;)" + methodNode.getReturnDescriptor();
				methodNode.setDescriptor(obfuscatedDescriptor);
				methodCount++;
				if (scriptContext.isVerbose()) {
					String obfuscatedParameters = MethodSignature.formatRemappedParameters(methodNode.getDescriptor(), classNameRemap);
					StringBuilder messageBuilder = new StringBuilder();
					messageBuilder.append("\t\tmethod '" + methodNode.getAccessFlagsString() + methodNode.getOriginalName() + originalParameters + "' => '" + methodNode.getOriginalName() + obfuscatedParameters + "'");
					if (methodNode.isNameChanged()) {
						messageBuilder.append(" (");
						messageBuilder.append(methodNode.getDisplayName());
						messageBuilder.append(')');
					}

					if (methodNode.isParameterLocalsShuffled()) {
						int[] shufflePermutationMap = methodNode.getLocalVariableTable().getShufflePermutationMap();
						if (shufflePermutationMap != null) {
							messageBuilder.append(" [");

							for (int i = 0; i < shufflePermutationMap.length; i++) {
								messageBuilder.append(shufflePermutationMap[i]);
								if (i < shufflePermutationMap.length - 1) {
									messageBuilder.append(',');
								}
							}

							messageBuilder.append(']');
						}
					}

					writer.println(messageBuilder.toString());
				}
			}
		}

		if (scriptContext.isVerbose()) {
			scriptContext.getWriter()
				.println(
					"\tMethod Parameter Obfuscation : Obfuscated parameters in "
						+ methodCount
						+ " method"
						+ (methodCount == 1 ? "" : 's')
						+ " in "
						+ classCount
						+ " class"
						+ (classCount == 1 ? "" : "es")
						+ "."
				);
		}

		workspace.resolveRefsAndSyncMetadata(classNodes, scriptContext, true);
		this.rebuildSignatureMaps(classNodes);
	}

	private void captureBaselineSignatures(ClassNode[] classNodes) {
		for (ClassNode classNode : classNodes) {
			if (!classNode.isInnerClass()) {
				String internalName = classNode.getInternalName();

				for (MethodNode methodNode : classNode.getMethodNodes()) {
					MethodSignature methodSignature = methodNode.getMethodSignature();
					MethodSignature baselineSignature = GlobalUtils.getFromDualKeyMapOrDefault(internalName, methodSignature, this.newToOldMethodSignatureMap);
					this.baselineSignatureByMethod.put(methodNode, baselineSignature);
				}
			}
		}
	}

	private AbstractMethodCallWriter createObjectPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(Ljava/lang/Object;[Ljava/lang/Object;I)[Ljava/lang/Object;", 3);
		ArrayList<AbstractInsnNode> instructions = this.buildObjectPackInsns(localVariableTable);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(Ljava/lang/Object;[Ljava/lang/Object;I)[Ljava/lang/Object;", instructions, 4, 3, 1, localVariableTable, tryCatchBlocks, "Method Parameter Obfuscation", cpEntries, inheritance, workspace, 10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private ArrayList<AbstractInsnNode> buildLongPackInsns(LocalVariableTable localVariableTable, List<TypeWriter> cpEntries, ClassNode classNode, AbstractWorkspace workspace, ClassProvider classProvider) {
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(9);
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxLong(0, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private AbstractMethodCallWriter createThreeIntPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace, ClassProvider classProvider) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(III[Ljava/lang/Object;I)[Ljava/lang/Object;", 5);
		ArrayList<AbstractInsnNode> instructions = this.buildThreeIntPackInsns(localVariableTable, cpEntries, classNode, workspace, classProvider);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(III[Ljava/lang/Object;I)[Ljava/lang/Object;", instructions, 4, 5, 1, localVariableTable, tryCatchBlocks, "Method Parameter Obfuscation", cpEntries, inheritance, workspace, 10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private ArrayList<AbstractInsnNode> buildDoublePackInsns(LocalVariableTable localVariableTable, List<TypeWriter> cpEntries, ClassNode classNode, AbstractWorkspace workspace, ClassProvider classProvider) {
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(9);
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxDouble(0, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private AbstractMethodCallWriter createTwoObjectPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;I)[Ljava/lang/Object;", 4);
		ArrayList<AbstractInsnNode> instructions = this.buildTwoObjectPackInsns(localVariableTable);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;I)[Ljava/lang/Object;",
			instructions,
			4,
			4,
			1,
			localVariableTable,
			tryCatchBlocks,
			"Method Parameter Obfuscation",
			cpEntries,
			inheritance,
			workspace,
			10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private ArrayList<AbstractInsnNode> buildThreeObjectPackInsns(LocalVariableTable localVariableTable) {
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(12);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 10));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.IINC(4, 1, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 10));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.IINC(4, 1, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 10));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private ArrayList<AbstractInsnNode> buildThreeIntPackInsns(LocalVariableTable localVariableTable, List<TypeWriter> cpEntries, ClassNode classNode, AbstractWorkspace workspace, ClassProvider classProvider) {
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(17);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxInt(0, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.IINC(4, 1, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxInt(1, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.IINC(4, 1, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxInt(2, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private ArrayList<AbstractInsnNode> buildObjectPackInsns(LocalVariableTable localVariableTable) {
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(6);
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(2, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 10));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private AbstractMethodCallWriter createDoublePackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace, ClassProvider classProvider) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(D[Ljava/lang/Object;I)[Ljava/lang/Object;", 4);
		ArrayList<AbstractInsnNode> instructions = this.buildDoublePackInsns(localVariableTable, cpEntries, classNode, workspace, classProvider);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(D[Ljava/lang/Object;I)[Ljava/lang/Object;", instructions, 5, 4, 1, localVariableTable, tryCatchBlocks, "Method Parameter Obfuscation", cpEntries, inheritance, workspace, 10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private void rebuildSignatureMaps(ClassNode[] classNodes) {
		this.oldToNewMethodSignatureMap.clearAll();
		this.newToOldMethodSignatureMap.clearAll();

		for (ClassNode classNode : classNodes) {
			if (!classNode.isInnerClass()) {
				String internalName = classNode.getInternalName();

				for (MethodNode methodNode : classNode.getMethodNodes()) {
					if (!methodNode.isClinit() && methodNode.getGenerationType() != 10) {
						MethodSignature baselineSignature = this.baselineSignatureByMethod.get(methodNode);
						if (baselineSignature != null) {
							MethodSignature methodSignature = methodNode.getMethodSignature();
							this.oldToNewMethodSignatureMap.put(internalName, baselineSignature, methodSignature);
							this.newToOldMethodSignatureMap.put(internalName, methodSignature, baselineSignature);
						}
					}
				}
			}
		}
	}

	private void obfuscateParametersInClass(
		ClassNode classNode,
		List<MethodNode> markedMethods,
		MultiMap<MethodNode, AbstractMethodNode> markedMethodsByCaller,
		Set<MethodNode> generatedMethods,
		Map<MethodNode, LocalVariableOperand> decryptionLocalVarsByMethod,
		AbstractWorkspace workspace,
		Inheritance inheritance,
		ClassProvider classProvider,
		boolean shuffleLocals
	) {
		ArrayList<TypeWriter> cpEntries = new ArrayList<>();
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		Map<String, AbstractMethodCallWriter> packMethodWriters = null;
		if (markedMethodsByCaller != null && supportsInterfaceCallPacking(classNode)) {
			packMethodWriters = this.createPackMethodWriters(classNode, markedMethodsByCaller, generatedMethods, cpEntries, inheritance, workspace, classProvider);
		}

		HashSet<MethodNode> methodsToProcess = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity((markedMethods != null ? markedMethods.size() : 0) + (markedMethodsByCaller != null ? markedMethodsByCaller.size() : 0)));
		if (markedMethods != null) {
			methodsToProcess.addAll(markedMethods);
		}

		if (markedMethodsByCaller != null) {
			methodsToProcess.addAll(markedMethodsByCaller.keySet());
		}

		for (MethodNode methodNode : methodsToProcess) {
			List<AbstractMethodNode> markedCallees = null;
			if (markedMethodsByCaller != null) {
				markedCallees = markedMethodsByCaller.get(methodNode);
			}

			LocalVariableOperand decryptionLocalVarOperand = null;
			if (decryptionLocalVarsByMethod != null) {
				decryptionLocalVarOperand = decryptionLocalVarsByMethod.get(methodNode);
			}

			methodNode.applyReflectionObfuscation(
				markedCallees, packMethodWriters, constantPoolManager, cpEntries, decryptionLocalVarOperand, workspace, classProvider, "Method Parameter Obfuscation", shuffleLocals
			);
		}

		if (!cpEntries.isEmpty()) {
			constantPoolManager.appendEntriesToPool(cpEntries);
		}
	}

	public static boolean supportsInterfaceCallPacking(AbstractClassNode classNode) {
		return Options.obfuscateInterfaceCalls && (!classNode.isInterface() || classNode.isJava8OrNewer());
	}

	private ArrayList<AbstractInsnNode> buildTwoIntPackInsns(LocalVariableTable localVariableTable, List<TypeWriter> cpEntries, ClassNode classNode, AbstractWorkspace workspace, ClassProvider classProvider) {
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(13);
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxInt(0, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.IINC(3, 1, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxInt(1, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private ArrayList<AbstractInsnNode> buildIntPackInsns(LocalVariableTable localVariableTable, List<TypeWriter> cpEntries, ClassNode classNode, AbstractWorkspace workspace, ClassProvider classProvider) {
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(9);
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(2, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxInt(0, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private ArrayList<AbstractInsnNode> buildTwoObjectPackInsns(LocalVariableTable localVariableTable) {
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(12);
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 10));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.IINC(3, 1, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadInt(3, localVariableTable, 10));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 10));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private AbstractMethodCallWriter createIntPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace, ClassProvider classProvider) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(I[Ljava/lang/Object;I)[Ljava/lang/Object;", 3);
		ArrayList<AbstractInsnNode> instructions = this.buildIntPackInsns(localVariableTable, cpEntries, classNode, workspace, classProvider);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(I[Ljava/lang/Object;I)[Ljava/lang/Object;", instructions, 4, 3, 1, localVariableTable, tryCatchBlocks, "Method Parameter Obfuscation", cpEntries, inheritance, workspace, 10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private AbstractMethodCallWriter createLongPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace, ClassProvider classProvider) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(J[Ljava/lang/Object;I)[Ljava/lang/Object;", 4);
		ArrayList<AbstractInsnNode> instructions = this.buildLongPackInsns(localVariableTable, cpEntries, classNode, workspace, classProvider);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(J[Ljava/lang/Object;I)[Ljava/lang/Object;", instructions, 5, 4, 1, localVariableTable, tryCatchBlocks, "Method Parameter Obfuscation", cpEntries, inheritance, workspace, 10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private ArrayList<AbstractInsnNode> buildBooleanPackInsns(LocalVariableTable localVariableTable, List<TypeWriter> cpEntries, ClassNode classNode, AbstractWorkspace workspace, ClassProvider classProvider) {
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(9);
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 5));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(2, localVariableTable, 5));
		PrimitiveBoxingUtils.loadAndBoxBoolean(0, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private Map<String, AbstractMethodCallWriter> createPackMethodWriters(
		ClassNode classNode,
		MultiMap<MethodNode, AbstractMethodNode> markedMethodsByCaller,
		Set<MethodNode> generatedMethods,
		List<TypeWriter> cpEntries,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		CountingSet<String> parameterShapes = new CountingSet<>();
		if (markedMethodsByCaller != null) {
			HashSet<AbstractMethodNode> markedMethods = GlobalUtils.newHashSet();

			for (Entry<MethodNode, List<AbstractMethodNode>> markedMethodsEntry : markedMethodsByCaller.entrySet()) {
				markedMethods.addAll(markedMethodsEntry.getValue());
			}

			Iterator<AbstractMethodNode> methodIterator = markedMethods.iterator();

			while (methodIterator.hasNext()) {
				List<String> parameterDescriptors = ((MethodNode)methodIterator.next()).getParameterDescriptors();
				String previousShape = null;

				for (String parameterDescriptor : parameterDescriptors) {
					if (parameterDescriptor.length() == 1) {
						switch (parameterDescriptor.charAt(0)) {
							case 'B':
							case 'C':
							case 'I':
							case 'S':
								if (previousShape != null && previousShape.equals("II")) {
									parameterShapes.add("III");
									parameterShapes.remove("II");
									previousShape = "III";
								} else if (previousShape != null && previousShape.equals("I")) {
									parameterShapes.add("II");
									parameterShapes.remove("I");
									previousShape = "II";
								} else {
									parameterShapes.add("I");
									previousShape = "I";
								}
								break;
							default:
								parameterShapes.add(parameterDescriptor);
								previousShape = parameterDescriptor;
						}
					} else if (previousShape != null && previousShape.equals("OO")) {
						parameterShapes.add("OOO");
						parameterShapes.remove("OO");
						previousShape = "OOO";
					} else if (previousShape != null && previousShape.equals("O")) {
						parameterShapes.add("OO");
						parameterShapes.remove("O");
						previousShape = "OO";
					} else {
						parameterShapes.add("O");
						previousShape = "O";
					}
				}
			}
		}

		HashMap<String, AbstractMethodCallWriter> packMethodWriters = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(11));
		Iterator<String> shapeIterator = parameterShapes.iterator();

		while (shapeIterator.hasNext()) {
			AbstractMethodCallWriter packMethodWriter = null;
			String shape = shapeIterator.next();
			if (shape.equals("I")) {
				packMethodWriter = this.createIntPackMethod(classNode, cpEntries, inheritance, workspace, classProvider);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("Z")) {
				packMethodWriter = this.createBooleanPackMethod(classNode, cpEntries, inheritance, workspace, classProvider);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("J")) {
				packMethodWriter = this.createLongPackMethod(classNode, cpEntries, inheritance, workspace, classProvider);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("F")) {
				packMethodWriter = this.createFloatPackMethod(classNode, cpEntries, inheritance, workspace, classProvider);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("D")) {
				packMethodWriter = this.createDoublePackMethod(classNode, cpEntries, inheritance, workspace, classProvider);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("II")) {
				packMethodWriter = this.createTwoIntPackMethod(classNode, cpEntries, inheritance, workspace, classProvider);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("III")) {
				packMethodWriter = this.createThreeIntPackMethod(classNode, cpEntries, inheritance, workspace, classProvider);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("O")) {
				packMethodWriter = this.createObjectPackMethod(classNode, cpEntries, inheritance, workspace);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("OO")) {
				packMethodWriter = this.createTwoObjectPackMethod(classNode, cpEntries, inheritance, workspace);
				packMethodWriters.put(shape, packMethodWriter);
			} else if (shape.equals("OOO")) {
				packMethodWriter = this.createThreeObjectPackMethod(classNode, cpEntries, inheritance, workspace);
				packMethodWriters.put(shape, packMethodWriter);
			}

			generatedMethods.add((MethodNode)packMethodWriter.getTargetMember());
		}

		return packMethodWriters;
	}

	private AbstractMethodCallWriter createThreeObjectPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;I)[Ljava/lang/Object;", 5);
		ArrayList<AbstractInsnNode> instructions = this.buildThreeObjectPackInsns(localVariableTable);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;[Ljava/lang/Object;I)[Ljava/lang/Object;",
			instructions,
			4,
			5,
			1,
			localVariableTable,
			tryCatchBlocks,
			"Method Parameter Obfuscation",
			cpEntries,
			inheritance,
			workspace,
			10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private ArrayList<AbstractInsnNode> buildFloatPackInsns(LocalVariableTable localVariableTable, List<TypeWriter> cpEntries, ClassNode classNode, AbstractWorkspace workspace, ClassProvider classProvider) {
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>(9);
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadInt(2, localVariableTable, 10));
		PrimitiveBoxingUtils.loadAndBoxFloat(0, instructions, classNode.isJava5OrNewer(), localVariableTable, cpEntries, constantPoolManager, workspace, classProvider, 10);
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(ARETURN));
		return instructions;
	}

	private AbstractMethodCallWriter createFloatPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace, ClassProvider classProvider) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(F[Ljava/lang/Object;I)[Ljava/lang/Object;", 3);
		ArrayList<AbstractInsnNode> instructions = this.buildFloatPackInsns(localVariableTable, cpEntries, classNode, workspace, classProvider);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(F[Ljava/lang/Object;I)[Ljava/lang/Object;", instructions, 4, 3, 1, localVariableTable, tryCatchBlocks, "Method Parameter Obfuscation", cpEntries, inheritance, workspace, 10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private AbstractMethodCallWriter createTwoIntPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace, ClassProvider classProvider) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(II[Ljava/lang/Object;I)[Ljava/lang/Object;", 4);
		ArrayList<AbstractInsnNode> instructions = this.buildTwoIntPackInsns(localVariableTable, cpEntries, classNode, workspace, classProvider);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(II[Ljava/lang/Object;I)[Ljava/lang/Object;", instructions, 4, 4, 1, localVariableTable, tryCatchBlocks, "Method Parameter Obfuscation", cpEntries, inheritance, workspace, 10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}

	private AbstractMethodCallWriter createBooleanPackMethod(ClassNode classNode, List<TypeWriter> cpEntries, Inheritance inheritance, AbstractWorkspace workspace, ClassProvider classProvider) {
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(Z[Ljava/lang/Object;I)[Ljava/lang/Object;", 3);
		ArrayList<AbstractInsnNode> instructions = this.buildBooleanPackInsns(localVariableTable, cpEntries, classNode, workspace, classProvider);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode packMethod = classNode.createUniqueMethod(
			"(Z[Ljava/lang/Object;I)[Ljava/lang/Object;", instructions, 4, 3, 1, localVariableTable, tryCatchBlocks, "Method Parameter Obfuscation", cpEntries, inheritance, workspace, 10
		);
		return classNode.getConstantPoolManager().getOrCreateAnyMethodRef(packMethod, cpEntries);
	}
}

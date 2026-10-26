package com.zelix.framework.code;

import com.zelix.exceptions.*;
import com.zelix.framework.*;
import com.zelix.framework.analyzer.*;
import com.zelix.framework.analyzer.dataset.MethodAnalysisResult;
import com.zelix.framework.analyzer.reference.*;
import com.zelix.framework.analyzer.tracker.ReflectionMethodCallTracker;
import com.zelix.framework.attributes.BootstrapMethodEntry;
import com.zelix.framework.attributes.BootstrapMethodsAttribute;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.code.ExceptionEntryNode;
import com.zelix.framework.attributes.debug.LineNumberEntry;
import com.zelix.framework.attributes.debug.LocalVariableEntry;
import com.zelix.framework.constant.*;
import com.zelix.framework.encryption.IntegerEntry;
import com.zelix.framework.encryption.LongEntry;
import com.zelix.framework.encryption.StringEntry;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.*;
import com.zelix.framework.listener.*;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.reference.ReferenceGraphMap;
import com.zelix.framework.signature.FieldID;
import com.zelix.framework.signature.MethodID;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.utils.PrimitiveBoxingUtils;
import com.zelix.framework.variable.*;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.framework.writer.reflect.ReflectionParameter;
import com.zelix.framework.writer.reflect.ReflectionRewriteSnippet;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.flow.Condition;
import com.zelix.tools.flow.ControlFlowBuilder;
import com.zelix.tools.integerEncryption.IntegerEncryptionExcludes;
import com.zelix.tools.longEncryption.LongEncryptionExcludes;
import com.zelix.tools.obfuscateParameters.ObfuscateParameterBuilder;
import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.tools.parameterChanger.dataset.ChangedMethod;
import com.zelix.tools.parameterChanger.extra.ExtraParameter;
import com.zelix.tools.references.ObfuscateReferencesBuilder;
import com.zelix.tools.string.StringEncryptionExcludes;
import com.zelix.utils.ClassMetrics;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.*;
import com.zelix.utils.dataset.*;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class InstructionListNode extends StructureNode {
	private int minReturnStackDepth = -1;
	private int bytecodeLength;
	private final LocalVariableTable localVariableTable;
	private ArrayList<AbstractInsnNode> instructions;
	private int stringConstantCount;
	private int intConstantCount;
	private int longConstantCount;
	private boolean modified;
	private boolean hasControlFlow;

	public InstructionListNode(ArrayList<AbstractInsnNode> instructions, LocalVariableTable localVariableTable, String operationName) {
		super(null);
		this.localVariableTable = localVariableTable;
		this.localVariableTable.setOwnerMethodBody(this);
		this.instructions = instructions;
		Iterator<AbstractInsnNode> instructionIterator = this.instructions.iterator();

		while (instructionIterator.hasNext()) {
			if (instructionIterator.next().isString()) {
				this.stringConstantCount++;
			}
		}

		this.recalculateOffsets();
		if (this.getByteCodeLength() > 65535) {
			throw new ProcessingException(
				operationName
					+ " : Bytecode length greater than "
					+ '\uffff'
					+ " in "
					+ this.getParentString()
					+ " in file '"
					+ this.getRawSourcePath()
					+ "' (1). (new="
					+ this.getByteCodeLength()
					+ ") : '"
					+ operationName
					+ "'"
			);
		} else {
			this.setModified(true);
		}
	}

	public InstructionListNode(
		StructureNode parentCodeAttribute,
		byte[] bytecodeBytes,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences
	) throws IOException {
		super(parentCodeAttribute);
		this.bytecodeLength = bytecodeBytes.length;
		this.instructions = new ArrayList<>(Math.max(5, (int)(this.bytecodeLength / 2.5)));
		this.localVariableTable = new LocalVariableTable(this, ((CodeAttribute)parentCodeAttribute).getMaxLocals());
		this.readInstructions(ByteDataBuffer.fromBytes(bytecodeBytes, false), this.bytecodeLength, elementsByOffset, this.localVariableTable, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences);
		this.setModified(true);
	}

	public InstructionListNode(
		StructureNode parentCodeAttribute,
		ByteDataBuffer dataBuffer,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences
	) throws IOException {
		super(parentCodeAttribute);
		this.bytecodeLength = dataBuffer.readInt();
		this.instructions = new ArrayList<>(Math.max(5, (int)(this.bytecodeLength / 2.5)));
		this.localVariableTable = new LocalVariableTable(this, ((CodeAttribute)parentCodeAttribute).getMaxLocals());
		this.readInstructions(dataBuffer, this.bytecodeLength, elementsByOffset, this.localVariableTable, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences);
	}

	private static AbstractClassNode findClassFromRepository(String classInternalName, ClassRepository classRepository) {
		Object classNode = ClassHierarchyNode.getClassNodeByInternalName(classInternalName);
		if (classNode == null) {
			try {
				classNode = classRepository.loadClass(classInternalName, false);
			} catch (FileException ignored) {
			}
		}

		return (AbstractClassNode)classNode;
	}

	private static String obfuscateTypeDescriptor(String keyTypeDescriptor, String methodDescriptor) {
		StringBuilder descriptorBuilder = new StringBuilder();
		int closeParenIndex = methodDescriptor.indexOf(41);
		if (!Options.keepRealTypesInDescriptors) {
			try {
				String parameterDescriptorText = methodDescriptor.substring(0, closeParenIndex);
				int charIndex = 0;

				while (charIndex < parameterDescriptorText.length()) {
					char currentChar = parameterDescriptorText.charAt(charIndex++);
					if (currentChar == 'L') {
						while (parameterDescriptorText.charAt(charIndex++) != ';') {
						}

						descriptorBuilder.append("Ljava/lang/Object;");
					} else if (currentChar != '[') {
						descriptorBuilder.append(currentChar);
					} else {
						do {
							currentChar = parameterDescriptorText.charAt(charIndex++);
						} while (currentChar == '[');

						if (currentChar != 'L') {
							descriptorBuilder.append("Ljava/lang/Object;");
						} else {
							while (parameterDescriptorText.charAt(charIndex++) != ';') {
							}

							descriptorBuilder.append("Ljava/lang/Object;");
						}
					}
				}
			} catch (IndexOutOfBoundsException descriptorException) {
				throw new LookupException("ERROR:Invalid method parameter String '" + methodDescriptor + "'.", descriptorException);
			}
		} else {
			descriptorBuilder.append(methodDescriptor.substring(0, closeParenIndex));
		}

		descriptorBuilder.append(keyTypeDescriptor);
		descriptorBuilder.append(methodDescriptor.substring(closeParenIndex));
		return descriptorBuilder.toString();
	}

	private static boolean isMethodParameterChanged(AbstractMethodNode methodNode, ChangedMethod changedMethod, Hierarchy hierarchy) {
		return changedMethod != null && changedMethod.usesStaticSaltField() && ParameterChangerBuilder.canChangeParameters(methodNode, hierarchy);
	}

	private static AbstractInsnNode getConversionInstruction(int opcode) {
		switch (opcode) {
			case 66:
				return InsnNode.of(I2B);
			case 67:
				return InsnNode.of(I2C);
			case 73:
				return null;
			case 83:
				return InsnNode.of(I2S);
			default:
				return null;
		}
	}

	public String getParentString() {
		CodeAttribute parent = (CodeAttribute)this.getParent();
		return parent != null ? parent.getAsString() : "<NO_PARENT_YET>";
	}

	public void encryptIntegerConstants(
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries,
		MethodNode methodNode,
		Set<IntegerNode> excludedIntegers,
		IntegerEncryptionExcludes encryptionExcludes,
		boolean encryptAllIntegers,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		boolean integersEncrypted = false;
		ClassNode ownerClassNode = methodNode.getMethodOwner();
		int instructionCount = this.instructions.size();
		int firstMarkerIndex = 0;

		for (int markerSearchIndex = 0; markerSearchIndex < instructionCount; markerSearchIndex++) {
			AbstractInsnNode scannedInstruction = this.instructions.get(markerSearchIndex);
			if (scannedInstruction.isLabel() && ((LabelNode)scannedInstruction).isMarker()) {
				firstMarkerIndex = markerSearchIndex;
				break;
			}
		}

		for (int instructionIndex = firstMarkerIndex; instructionIndex < instructionCount; instructionIndex++) {
			AbstractInsnNode instruction = this.instructions.get(instructionIndex);
			if (instruction.isIntegerOpcode() || instruction.isIntegerConstant()) {
				boolean targetsWorkspaceField = false;
				if (instructionIndex < instructionCount - 1) {
					AbstractInsnNode nextInstruction = this.instructions.get(instructionIndex + 1);
					if (nextInstruction.isPutField()) {
						AbstractFieldNode targetField = (AbstractFieldNode)((FieldTypeWriter)((WideConstantPoolInsnNode)nextInstruction).getTypeWriter()).getTargetMember();
						if (targetField != null && targetField.isWorkspaceNode()) {
							targetsWorkspaceField = true;
							if (encryptionExcludes == null || !encryptionExcludes.isFieldExcluded((FieldNode)targetField)) {
								if (instruction.isIntegerOpcode()) {
									int fieldStoreValue = -1;
									if (instruction.getOpcode() == BIPUSH) {
										fieldStoreValue = ((BiPushNode)instruction).getValue();
									} else if (instruction.getOpcode() == SIPUSH) {
										fieldStoreValue = ((SiPushNode)instruction).getValue();
									} else if (instruction instanceof InsnNode) {
										switch (instruction.getOpcode()) {
											case ICONST_M1:
												if (!Options.ZKM_INTEGER_ENCRYPT_LOW_VALUE_FIELDS) {
													continue;
												}

												fieldStoreValue = -1;
												break;
											case ICONST_0:
												if (!Options.ZKM_INTEGER_ENCRYPT_LOW_VALUE_FIELDS) {
													continue;
												}

												fieldStoreValue = 0;
												break;
											case ICONST_1:
												if (!Options.ZKM_INTEGER_ENCRYPT_LOW_VALUE_FIELDS) {
													continue;
												}

												fieldStoreValue = 1;
												break;
											case ICONST_2:
												fieldStoreValue = 2;
												break;
											case ICONST_3:
												fieldStoreValue = 3;
												break;
											case ICONST_4:
												fieldStoreValue = 4;
												break;
											case ICONST_5:
												fieldStoreValue = 5;
										}
									}

									IntegerNode fieldStoreNode = constantPoolManager.getOrCreateInteger(fieldStoreValue, typeWriters, false, true);
									WideConstantPoolInsnNode fieldStoreLdcInsn = new WideConstantPoolInsnNode(LDC_W, fieldStoreNode);
									this.instructions.set(instructionIndex, fieldStoreLdcInsn);
									this.intConstantCount++;
									integerConstantEntries.put(ownerClassNode, methodNode, fieldStoreNode, new PriorityEntry<>(instructionIndex, fieldStoreLdcInsn));
									integersEncrypted = true;
								} else {
									instruction.collectIntObfCandidate(integerConstantEntries, excludedIntegers, methodNode, instructionIndex);
								}
							}
						}
					}
				}

				if (!targetsWorkspaceField && !(instruction instanceof InsnNode)) {
					if (instruction.isIntegerOpcode()) {
						if (encryptAllIntegers) {
							int integerValue = -1;
							if (instruction.getOpcode() == BIPUSH) {
								integerValue = ((BiPushNode)instruction).getValue();
							} else if (instruction.getOpcode() == SIPUSH) {
								integerValue = ((SiPushNode)instruction).getValue();
							}

							IntegerNode integerValueNode = constantPoolManager.getOrCreateInteger(integerValue, typeWriters, false, true);
							WideConstantPoolInsnNode integerLdcInsn = new WideConstantPoolInsnNode(LDC_W, integerValueNode);
							this.instructions.set(instructionIndex, integerLdcInsn);
							this.intConstantCount++;
							integerConstantEntries.put(ownerClassNode, methodNode, integerValueNode, new PriorityEntry<>(instructionIndex, integerLdcInsn));
							integersEncrypted = true;
						}
					} else {
						instruction.collectIntObfCandidate(integerConstantEntries, excludedIntegers, methodNode, instructionIndex);
					}
				}
			}
		}

		if (integersEncrypted) {
			this.setModified(true);
			this.recalculateOffsets();
		}
	}

	private void insertObfuscationBlock(
		List<AbstractInsnNode> instructions,
		AbstractInsnNode extraInstruction,
		List<InstructionBlock> instructionBlocks,
		MethodAnalyzer methodAnalyzer,
		int patchOffset,
		InstructionBlock existingBlock,
		int anchorIndex,
		OrderedSet<Integer> insertionPoints,
		Random random
	) {
		AnalyzingNode firstAnalyzingNode = methodAnalyzer.getNodeByIndex(0);
		if (insertionPoints.size() > 1 && !Options.dontInsertFrameBlocks) {
			int extraInsertionIndex = random.nextInt(insertionPoints.size() - 1) + 1;
			int mainInsertionIndex = random.nextInt(extraInsertionIndex);
			ArrayList<AbstractInsnNode> copiedInstructions = new ArrayList<>(instructions.size());
			copiedInstructions.addAll(instructions);
			InstructionBlock copiedBlock = new InstructionBlock(copiedInstructions, insertionPoints.get(mainInsertionIndex), 0, 1);
			instructionBlocks.add(copiedBlock);
			if (extraInstruction != null) {
				InstructionBlock extraBlock = new InstructionBlock(extraInstruction, insertionPoints.get(extraInsertionIndex), 0, 0);
				instructionBlocks.add(extraBlock);
			}
		} else {
			int firstNonLabelIndex = 0;

			while (this.instructions.get(firstNonLabelIndex).isLabel()) {
				firstNonLabelIndex++;
			}

			if (Options.insertSwapObfuscationBlocks
				&& insertionPoints.size() == 1
				&& insertionPoints.get(0) == -1
				&& extraInstruction != null
				&& (patchOffset > firstNonLabelIndex || patchOffset == firstNonLabelIndex && existingBlock != null)
				&& anchorIndex > firstNonLabelIndex
				&& firstNonLabelIndex < firstAnalyzingNode.getEndIndex()
				&& this.instructions.get(firstNonLabelIndex).isStackProducer()
				&& !this.instructions.get(firstNonLabelIndex).isCategory2Producer()
				&& random.nextInt(10) == 9) {
				InstructionBlock appendedBlock = new InstructionBlock(new ArrayList<>(instructions), -1, 0, 1);
				instructionBlocks.add(appendedBlock);
				ArrayList<AbstractInsnNode> swapInstructions = new ArrayList<>(2);
				swapInstructions.add(InsnNode.of(SWAP));
				swapInstructions.add(extraInstruction);
				InstructionBlock swapBlock = new InstructionBlock(swapInstructions, firstNonLabelIndex, 0, 1);
				swapBlock.setPriority(-1);
				instructionBlocks.add(swapBlock);
			} else {
				ArrayList<AbstractInsnNode> combinedInstructions = new ArrayList<>(2);
				combinedInstructions.addAll(instructions);
				if (extraInstruction != null) {
					combinedInstructions.add(extraInstruction);
				}

				InstructionBlock combinedBlock = new InstructionBlock(combinedInstructions, -1, 0, 1);
				instructionBlocks.add(combinedBlock);
			}
		}
	}

	public void fixLabelOffsets(MultiMap<Integer, IGraphElement> elementsByOffset, PrintWriter logWriter) {
		int offsetCount = elementsByOffset.size();
		if (offsetCount != 0) {
			Integer[] sortedOffsets = new Integer[offsetCount];
			ArrayList<LabelNode> labelNodes = new ArrayList<>(offsetCount + 1);
			LabelNode localVariableLabel = null;
			int i = 0;
			Enumeration<Integer> offsetEnumeration = elementsByOffset.keys();

			while (offsetEnumeration.hasMoreElements()) {
				sortedOffsets[i++] = offsetEnumeration.nextElement();
			}

			Arrays.sort(sortedOffsets);

			for (int offsetIndex = 0; offsetIndex < offsetCount; offsetIndex++) {
				Integer offset = sortedOffsets[offsetIndex];
				int settingFlags = 0;
				List<IGraphElement> elementsAtOffset = elementsByOffset.get(offset);
				if (offset == 0) {
					Collections.sort(elementsAtOffset, new InstructionListNode.GraphElementComparator());
					if (elementsAtOffset.get(0) instanceof LocalVariableEntry) {
						elementsAtOffset = new ArrayList<>(elementsAtOffset);
						localVariableLabel = new LabelNode();
						labelNodes.add(localVariableLabel);
						Iterator<IGraphElement> elementIterator = elementsAtOffset.iterator();

						for (IGraphElement localVariableElement = elementIterator.next(); localVariableElement instanceof LocalVariableEntry; localVariableElement = elementIterator.next()) {
							elementIterator.remove();
							localVariableElement.setLabel(0, localVariableLabel);
							if (!elementIterator.hasNext()) {
								break;
							}
						}
					}

					if (elementsAtOffset.size() == 0) {
						continue;
					}
				}

				for (IGraphElement element : elementsAtOffset) {
					settingFlags |= element.getSettingFlag().getFlag();
				}

				LabelNode offsetLabel = new LabelNode(offset, settingFlags);
				labelNodes.add(offsetLabel);

				for (IGraphElement labeledElement : elementsAtOffset) {
					labeledElement.setLabel(offset, offsetLabel);
				}
			}

			int labelCount = labelNodes.size();
			int instructionCount = this.instructions.size();
			ArrayList<AbstractInsnNode> newInstructions = new ArrayList<>(instructionCount + labelCount);
			int labelIndex = 0;
			int currentBytecodeOffset = 0;
			int previousOffset = 0;
			if (localVariableLabel != null) {
				newInstructions.add(localVariableLabel);
				labelIndex++;
			}

			int nextLabelOffset;
			if (labelCount > labelIndex) {
				nextLabelOffset = labelNodes.get(labelIndex).getOffset();
			} else {
				nextLabelOffset = -1;
			}

			for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
				if (labelIndex < labelCount && currentBytecodeOffset > nextLabelOffset) {
					for (IGraphElement offendingElement : elementsByOffset.get(sortedOffsets[localVariableLabel == null ? labelIndex : labelIndex - 1])) {
						if (!(offendingElement instanceof LineNumberEntry)) {
							AbstractInsnNode previousInstruction = this.instructions.get(instructionIndex - 1);
							throw new ProcessingException(
								offendingElement.getMnemonics()
									+ " in method '"
									+ this.getMethodNode().getDisplayName()
									+ "' in class '"
									+ this.getRawSourcePath()
									+ "' points into middle of an instruction : "
									+ previousInstruction.getOpcodeMnemonics()
									+ " : "
									+ previousOffset
									+ " : "
									+ nextLabelOffset
							);
						}

						logWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid LineNumberTable Attribute (C). Can be ignored.");
					}

					boolean lineNumberTableConflict = false;

					do {
						if (!lineNumberTableConflict) {
							List<IGraphElement> elementsAtCurrentOffset = elementsByOffset.get(INTEGER_CACHE.valueOf(currentBytecodeOffset));
							if (elementsAtCurrentOffset != null) {
								for (int elementIndex = 0; elementIndex < elementsAtCurrentOffset.size(); elementIndex++) {
									if (elementsAtCurrentOffset.get(elementIndex) instanceof LineNumberEntry) {
										lineNumberTableConflict = true;
										break;
									}
								}
							}
						}

						if (!lineNumberTableConflict) {
							newInstructions.add(labelNodes.get(labelIndex++));
						} else {
							labelIndex++;
							logWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid LineNumberTable Attribute (D). Could not be corrected!");
						}

						if (labelIndex < offsetCount) {
							nextLabelOffset = labelNodes.get(labelIndex).getOffset();
						}
					} while (labelIndex < offsetCount && currentBytecodeOffset > nextLabelOffset);
				}

				if (labelIndex < labelCount && currentBytecodeOffset == nextLabelOffset) {
					newInstructions.add(labelNodes.get(labelIndex++));
					if (labelIndex < labelCount) {
						nextLabelOffset = labelNodes.get(labelIndex).getOffset();
					}
				}

				AbstractInsnNode instruction = this.instructions.get(instructionIndex);
				newInstructions.add(instruction);
				previousOffset = currentBytecodeOffset;
				currentBytecodeOffset += instruction.getBytecodeSize();
			}

			if (labelIndex < labelCount && currentBytecodeOffset == nextLabelOffset) {
				newInstructions.add(labelNodes.get(labelIndex++));
			}

			this.instructions = newInstructions;
		}
	}

	private void collectIndyDependencies(
		InvokeDynamicCallWriter indyCallWriter, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods, Set<AbstractClassNode> signatureClasses, Set<AbstractClassNode> referencedClasses
	) {
		indyCallWriter.collectDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
	}

	private InstructionBlock buildStringConcatBlock(
		int indyIndex,
		AnalyzedStringConcat analyzedConcat,
		InvokeDynamicCallWriter indyCallWriter,
		Map<InvokeDynamicCallWriter, String> dynCallNames,
		StackFrameType[] operandStack,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters
	) {
		ArrayList<AbstractInsnNode> generatedInsns = new ArrayList<>();
		String indyDescriptor = indyCallWriter.getDescriptor();
		List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(indyDescriptor);
		int parameterCount = parameterDescriptors.size();
		List<StringConcatPart> concatParts = analyzedConcat.getParsedParts();
		int partCount = concatParts.size();
		int totalArgumentCount = parameterCount + partCount;
		StringConcatPart firstPart = concatParts.get(0);
		StringBuilder signatureBuilder = new StringBuilder();
		byte additionalStackRequired = 1;
		if (partCount == 1 && firstPart.getPartIndex() == totalArgumentCount - 1) {
			signatureBuilder.append(indyDescriptor.substring(1, indyDescriptor.indexOf(41)));
			signatureBuilder.append("Ljava/lang/String;");
			StringNode stringNodeTail1 = firstPart.getConstantStringNode();
			if (stringNodeTail1 != null) {
				firstPart.clearConstantNode();
			} else {
				stringNodeTail1 = constantPoolManager.createString(firstPart.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, stringNodeTail1));
		} else if (partCount == 1 && firstPart.getPartIndex() == totalArgumentCount - 2) {
			additionalStackRequired = 2;
			String lastParamDescriptorTail2 = null;

			for (int paramIndexTail2 = 0; paramIndexTail2 < parameterDescriptors.size(); paramIndexTail2++) {
				if (paramIndexTail2 == parameterDescriptors.size() - 1) {
					signatureBuilder.append("Ljava/lang/String;");
					lastParamDescriptorTail2 = parameterDescriptors.get(paramIndexTail2);
				}

				signatureBuilder.append(parameterDescriptors.get(paramIndexTail2));
			}

			StringNode stringNodeTail2 = firstPart.getConstantStringNode();
			if (stringNodeTail2 != null) {
				firstPart.clearConstantNode();
			} else {
				stringNodeTail2 = constantPoolManager.createString(firstPart.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, stringNodeTail2));
			if (StackFrameType.isDescriptorCategory2(lastParamDescriptorTail2)) {
				generatedInsns.add(InsnNode.of(DUP_X2));
				generatedInsns.add(InsnNode.of(POP));
			} else {
				generatedInsns.add(InsnNode.of(SWAP));
			}
		} else if (partCount == 2 && firstPart.getPartIndex() == totalArgumentCount - 3 && concatParts.get(1).getPartIndex() == totalArgumentCount - 1) {
			additionalStackRequired = 2;
			String lastParamDescriptorTail3 = null;

			for (int paramIndexTail3 = 0; paramIndexTail3 < parameterDescriptors.size(); paramIndexTail3++) {
				if (paramIndexTail3 == parameterDescriptors.size() - 1) {
					signatureBuilder.append("Ljava/lang/String;");
					lastParamDescriptorTail3 = parameterDescriptors.get(paramIndexTail3);
				}

				signatureBuilder.append(parameterDescriptors.get(paramIndexTail3));
			}

			signatureBuilder.append("Ljava/lang/String;");
			StringNode stringNodeTail3 = firstPart.getConstantStringNode();
			if (stringNodeTail3 != null) {
				firstPart.clearConstantNode();
			} else {
				stringNodeTail3 = constantPoolManager.createString(firstPart.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, stringNodeTail3));
			if (StackFrameType.isDescriptorCategory2(lastParamDescriptorTail3)) {
				generatedInsns.add(InsnNode.of(DUP_X2));
				generatedInsns.add(InsnNode.of(POP));
			} else {
				generatedInsns.add(InsnNode.of(SWAP));
			}

			StringConcatPart secondPartTail3 = concatParts.get(1);
			StringNode secondStringNodeTail3 = secondPartTail3.getConstantStringNode();
			if (secondStringNodeTail3 != null) {
				secondPartTail3.clearConstantNode();
			} else {
				secondStringNodeTail3 = constantPoolManager.createString(secondPartTail3.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, secondStringNodeTail3));
		} else if (partCount == 2
			&& firstPart.getPartIndex() == totalArgumentCount - 4
			&& concatParts.get(1).getPartIndex() == totalArgumentCount - 2
			&& !StackFrameType.isDescriptorCategory2(parameterDescriptors.get(parameterDescriptors.size() - 1))
			&& !StackFrameType.isDescriptorCategory2(parameterDescriptors.get(parameterDescriptors.size() - 2))) {
			additionalStackRequired = 2;

			for (int paramIndexTail4 = 0; paramIndexTail4 < parameterDescriptors.size(); paramIndexTail4++) {
				if (paramIndexTail4 == parameterDescriptors.size() - 1) {
					signatureBuilder.append("Ljava/lang/String;");
					String lastParamDescriptorTail4 = parameterDescriptors.get(paramIndexTail4);
				} else if (paramIndexTail4 == parameterDescriptors.size() - 2) {
					signatureBuilder.append("Ljava/lang/String;");
					String secondLastParamDescriptorTail4 = parameterDescriptors.get(paramIndexTail4);
				}

				signatureBuilder.append(parameterDescriptors.get(paramIndexTail4));
			}

			StringNode stringNodeTail4 = firstPart.getConstantStringNode();
			if (stringNodeTail4 != null) {
				firstPart.clearConstantNode();
			} else {
				stringNodeTail4 = constantPoolManager.createString(firstPart.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, stringNodeTail4));
			generatedInsns.add(InsnNode.of(DUP_X2));
			generatedInsns.add(InsnNode.of(POP));
			StringConcatPart secondPartTail4 = concatParts.get(1);
			StringNode secondStringNodeTail4 = secondPartTail4.getConstantStringNode();
			if (secondStringNodeTail4 != null) {
				secondPartTail4.clearConstantNode();
			} else {
				secondStringNodeTail4 = constantPoolManager.createString(secondPartTail4.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, secondStringNodeTail4));
			generatedInsns.add(InsnNode.of(SWAP));
		} else if (partCount == 3
			&& firstPart.getPartIndex() == totalArgumentCount - 5
			&& concatParts.get(1).getPartIndex() == totalArgumentCount - 3
			&& concatParts.get(2).getPartIndex() == totalArgumentCount - 1
			&& !StackFrameType.isDescriptorCategory2(parameterDescriptors.get(parameterDescriptors.size() - 1))
			&& !StackFrameType.isDescriptorCategory2(parameterDescriptors.get(parameterDescriptors.size() - 2))) {
			additionalStackRequired = 2;

			for (int paramIndexTail5 = 0; paramIndexTail5 < parameterDescriptors.size(); paramIndexTail5++) {
				if (paramIndexTail5 == parameterDescriptors.size() - 1) {
					signatureBuilder.append("Ljava/lang/String;");
					String lastParamDescriptorTail5 = parameterDescriptors.get(paramIndexTail5);
				} else if (paramIndexTail5 == parameterDescriptors.size() - 2) {
					signatureBuilder.append("Ljava/lang/String;");
					String secondLastParamDescriptorTail5 = parameterDescriptors.get(paramIndexTail5);
				}

				signatureBuilder.append(parameterDescriptors.get(paramIndexTail5));
			}

			signatureBuilder.append("Ljava/lang/String;");
			StringNode stringNodeTail5 = firstPart.getConstantStringNode();
			if (stringNodeTail5 != null) {
				firstPart.clearConstantNode();
			} else {
				stringNodeTail5 = constantPoolManager.createString(firstPart.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, stringNodeTail5));
			generatedInsns.add(InsnNode.of(DUP_X2));
			generatedInsns.add(InsnNode.of(POP));
			StringConcatPart secondPartTail5 = concatParts.get(1);
			StringNode secondStringNodeTail5 = secondPartTail5.getConstantStringNode();
			if (secondStringNodeTail5 != null) {
				secondPartTail5.clearConstantNode();
			} else {
				secondStringNodeTail5 = constantPoolManager.createString(secondPartTail5.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, secondStringNodeTail5));
			generatedInsns.add(InsnNode.of(SWAP));
			StringConcatPart thirdPartTail5 = concatParts.get(2);
			StringNode thirdStringNodeTail5 = thirdPartTail5.getConstantStringNode();
			if (thirdStringNodeTail5 != null) {
				thirdPartTail5.clearConstantNode();
			} else {
				thirdStringNodeTail5 = constantPoolManager.createString(thirdPartTail5.getValue(), typeWriters);
			}

			generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, thirdStringNodeTail5));
		} else {
			int originalMaxLocals = this.getMaxLocals();
			int nextLocalSlot = originalMaxLocals;
			int operandStackIndex = operandStack != null ? operandStack.length - 1 : -1;
			boolean leadingParameterPresent = firstPart.getPartIndex() > 0;

			for (int parameterIndex = 0; parameterIndex < parameterCount - (leadingParameterPresent ? 1 : 0); parameterIndex++) {
				StackFrameType storedFrameType = operandStack[operandStackIndex--];
				if (storedFrameType.isComputationalInt()) {
					generatedInsns.add(AbstractInsnNode.storeInt(nextLocalSlot++, this.localVariableTable, 1));
				} else if (storedFrameType == StackFrameType.FLOAT) {
					generatedInsns.add(AbstractInsnNode.storeFloat(nextLocalSlot++, this.localVariableTable, 1));
				} else if (storedFrameType == StackFrameType.DOUBLE) {
					generatedInsns.add(AbstractInsnNode.storeDouble(nextLocalSlot++, this.localVariableTable, 1));
					nextLocalSlot++;
					additionalStackRequired = 2;
				} else if (storedFrameType == StackFrameType.LONG) {
					generatedInsns.add(AbstractInsnNode.storeLong(nextLocalSlot++, this.localVariableTable, 1));
					nextLocalSlot++;
					additionalStackRequired = 2;
				} else if (storedFrameType.isArray() || storedFrameType.isObjectReference() || storedFrameType.isNullType()) {
					generatedInsns.add(AbstractInsnNode.storeObject(nextLocalSlot++, this.localVariableTable, 1));
				}
			}

			operandStackIndex++;
			int partIndex = 0;
			StringConcatPart currentPart = concatParts.get(0);
			int loadSlotIndex = nextLocalSlot;
			int descriptorIndex = 0;
			if (leadingParameterPresent) {
				descriptorIndex++;
				signatureBuilder.append(parameterDescriptors.get(0));
			}

			for (int argumentIndex = leadingParameterPresent ? 1 : 0; argumentIndex < totalArgumentCount; argumentIndex++) {
				if (currentPart != null && currentPart.getPartIndex() == argumentIndex) {
					signatureBuilder.append("Ljava/lang/String;");
					StringNode currentPartStringNode = currentPart.getConstantStringNode();
					if (currentPartStringNode != null) {
						currentPart.clearConstantNode();
					} else {
						currentPartStringNode = constantPoolManager.createString(currentPart.getValue(), typeWriters);
					}

					generatedInsns.add(new WideConstantPoolInsnNode(LDC_W, currentPartStringNode));
					if (++partIndex < concatParts.size()) {
						currentPart = concatParts.get(partIndex);
					} else {
						currentPart = null;
					}
				} else {
					signatureBuilder.append(parameterDescriptors.get(descriptorIndex++));
					StackFrameType loadedFrameType = operandStack[operandStackIndex];
					loadSlotIndex--;
					if (loadedFrameType.isComputationalInt()) {
						generatedInsns.add(AbstractInsnNode.loadInt(loadSlotIndex, this.localVariableTable, 1));
					} else if (loadedFrameType == StackFrameType.FLOAT) {
						generatedInsns.add(AbstractInsnNode.loadFloat(loadSlotIndex, this.localVariableTable, 1));
					} else if (loadedFrameType == StackFrameType.DOUBLE) {
						generatedInsns.add(AbstractInsnNode.loadDouble(--loadSlotIndex, this.localVariableTable, 1));
					} else if (loadedFrameType == StackFrameType.LONG) {
						generatedInsns.add(AbstractInsnNode.loadLong(--loadSlotIndex, this.localVariableTable, 1));
					} else if (loadedFrameType.isArray() || loadedFrameType.isObjectReference() || loadedFrameType.isNullType()) {
						generatedInsns.add(AbstractInsnNode.loadObject(loadSlotIndex, this.localVariableTable, 1));
					}

					operandStackIndex++;
				}
			}

			if (nextLocalSlot != originalMaxLocals) {
				this.setMaxLocals(nextLocalSlot);
			}
		}

		if (!dynCallNames.containsKey(indyCallWriter)) {
			dynCallNames.put(indyCallWriter, signatureBuilder.toString());
		}

		return new InstructionBlock(generatedInsns, indyIndex - 1, 0, additionalStackRequired);
	}

	public MethodAnalyzer getMethodAnalyzer(TypeResolver typeResolver, ClassHierarchy classHierarchy) {
		return this.createMethodAnalyzer(typeResolver, classHierarchy, false);
	}

	public void encryptLongConstants(
		List<KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry>> longConstantEntries,
		Map<LabelNode, AtomicInt> labelCounters,
		boolean isClinit,
		int[][] exceptionRanges,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelReplacements,
		Long xorKey,
		LocalVariableOperand xorKeyOperand,
		Indexable localSlotIndex,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		InstructionListNode.LongEntryPairComparator comparator = new InstructionListNode.LongEntryPairComparator(this);
		Collections.sort(longConstantEntries, comparator);
		int entryCount = longConstantEntries.size();
		int arrayFieldCacheSlot = -1;
		if (entryCount != 0) {
			MethodAnalyzer methodAnalyzer = null;
			Iterator<KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry>> entryIterator = longConstantEntries.iterator();
			KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry> longConstantEntry = entryIterator.next();
			WritableType targetInstruction = longConstantEntry.getKey().getValue();
			int remainingEntries = entryCount;
			this.setModified(true);
			int extraStack = 0;
			boolean arrayFieldCached = false;
			boolean cacheSlotAllocated = false;
			ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
			int instructionCount = this.instructions.size();

			for (int i = 0; i < instructionCount; i++) {
				AbstractInsnNode currentInsn = this.instructions.get(i);
				if (currentInsn.isLabel()) {
					LabelNode label = (LabelNode)currentInsn;
					if (label.hasFlag(1) || label.hasFlag(1024)) {
						arrayFieldCached = false;
					}
				}

				if (currentInsn == targetInstruction) {
					remainingEntries--;
					LongEntry longEntry = longConstantEntry.getValue();
					extraStack = 0;
					switch (longEntry.getLookupType()) {
						case LOCAL_ARRAY_AND_INDEX:
							ArrayList<AbstractInsnNode> localArrayInsns = new ArrayList<>();
							if (longEntry.hasIndex()) {
								localArrayInsns.add(AbstractInsnNode.loadObject(longEntry.getLocalArrayIndex(), this.localVariableTable, 12));
								this.pushIntConstant(longEntry.getIndex(), localArrayInsns, constantPoolManager, typeWriters, entryCount);
								localArrayInsns.add(InsnNode.of(LALOAD));
							} else {
								localArrayInsns.add(AbstractInsnNode.loadLong(longEntry.getLocalArrayIndex(), this.localVariableTable, 12));
							}

							instructionBlocks.add(new InstructionBlock(localArrayInsns, i - 1, 1, 0));
							break;
						case FIELD_ARRAY_AND_INDEX:
							ArrayList<AbstractInsnNode> fieldArrayInsns = new ArrayList<>();
							FieldTypeWriter arrayFieldWriter = longEntry.getArrayFieldWriter();
							if (longEntry.hasIndex()) {
								if (arrayFieldCached) {
									fieldArrayInsns.add(AbstractInsnNode.loadObject(arrayFieldCacheSlot, this.localVariableTable, 12));
								} else {
									fieldArrayInsns.add(new WideConstantPoolInsnNode(GETSTATIC, arrayFieldWriter));
									if (remainingEntries > 0 && (exceptionRanges.length == 0 || !this.isWithinExceptionRange(i, exceptionRanges))) {
										if (!cacheSlotAllocated) {
											cacheSlotAllocated = true;
											arrayFieldCacheSlot = localSlotIndex.get();
											localSlotIndex.increaseAndGet();
										}

										fieldArrayInsns.add(AbstractInsnNode.storeObject(arrayFieldCacheSlot, this.localVariableTable, 12));
										fieldArrayInsns.add(AbstractInsnNode.loadObject(arrayFieldCacheSlot, this.localVariableTable, 12));
										arrayFieldCached = true;
									}
								}

								AbstractInsnNode.encryptIntConstant(longEntry.getIndex(), fieldArrayInsns, constantPoolManager, typeWriters);
								fieldArrayInsns.add(InsnNode.of(LALOAD));
							} else {
								fieldArrayInsns.add(new WideConstantPoolInsnNode(GETSTATIC, arrayFieldWriter));
							}

							instructionBlocks.add(new InstructionBlock(fieldArrayInsns, i - 1, 1, 0));
							break;
						case LOOKUP_METHOD_AND_INDEX:
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							AbstractMethodCallWriter methodCallWriter = longEntry.getMethodCallWriter();
							ArrayList<AbstractInsnNode> methodIndexInsns = new ArrayList<>();
							ArrayList<AbstractInsnNode> methodKeyInsns = new ArrayList<>();
							int methodIndex = longEntry.getIndex();
							long methodKey = longEntry.getEncryptionKey();
							if (xorKey != null && xorKeyOperand != null) {
								int methodEncryptedIndex = methodIndex ^ (int)(methodKey & 32767L);
								this.pushIntConstant(methodEncryptedIndex, methodIndexInsns, constantPoolManager, typeWriters, entryCount);
								long methodXoredKey = methodKey ^ xorKey;
								AbstractInsnNode.appendLongConst(methodXoredKey, methodKeyInsns, constantPoolManager, typeWriters);
								methodKeyInsns.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 12));
								methodKeyInsns.add(InsnNode.of(LXOR));
							} else {
								int methodEncryptedIndexNoXor = methodIndex ^ (int)(methodKey & 32767L);
								this.pushIntConstant(methodEncryptedIndexNoXor, methodIndexInsns, constantPoolManager, typeWriters, entryCount);
								AbstractInsnNode.appendLongConst(methodKey, methodKeyInsns, constantPoolManager, typeWriters);
							}

							WideConstantPoolInsnNode methodInvokeInsn = new WideConstantPoolInsnNode(INVOKESTATIC, methodCallWriter);
							this.insertInstructionBlock(methodIndexInsns, methodKeyInsns, methodInvokeInsn, instructionBlocks, methodAnalyzer, i, 1);
							break;
						case LOOKUP_METHOD_AND_INDEX_DES:
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							AbstractMethodCallWriter desCallWriter = longEntry.getMethodCallWriter();
							ArrayList<AbstractInsnNode> desIndexInsns = new ArrayList<>();
							ArrayList<AbstractInsnNode> desKeyInsns = new ArrayList<>();
							int desIndex = longEntry.getIndex();
							long desKey = longEntry.getEncryptionKey();
							if (xorKey != null && xorKeyOperand != null) {
								int desEncryptedIndex = desIndex ^ (int)(desKey & 32767L);
								this.pushIntConstant(desEncryptedIndex, desIndexInsns, constantPoolManager, typeWriters, entryCount);
								AbstractInsnNode.appendLongConst(desKey ^ xorKey, desKeyInsns, constantPoolManager, typeWriters);
								desKeyInsns.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 12));
								desKeyInsns.add(InsnNode.of(LXOR));
							} else {
								AbstractInsnNode.encryptIntConstant(desIndex ^ (int)desKey, desIndexInsns, constantPoolManager, typeWriters);
								AbstractInsnNode.appendLongConst(desKey, desKeyInsns, constantPoolManager, typeWriters);
							}

							WideConstantPoolInsnNode desInvokeInsn = new WideConstantPoolInsnNode(INVOKESTATIC, desCallWriter);
							this.insertInstructionBlock(desIndexInsns, desKeyInsns, desInvokeInsn, instructionBlocks, methodAnalyzer, i, 1);
							break;
						case INDY_ENTRY_AND_INDEX:
							InvokeDynamicCallWriter indyCallWriter = longEntry.getIndyCallWriter();
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							ArrayList<AbstractInsnNode> indyIndexInsns = new ArrayList<>();
							ArrayList<AbstractInsnNode> indyKeyInsns = new ArrayList<>();
							int indyIndex = longEntry.getIndex();
							long indyKey = longEntry.getEncryptionKey();
							if (xorKey != null && xorKeyOperand != null) {
								int indyEncryptedIndex = indyIndex ^ (int)(indyKey & 32767L);
								this.pushIntConstant(indyEncryptedIndex, indyIndexInsns, constantPoolManager, typeWriters, entryCount);
								long indyXoredKey = indyKey ^ xorKey;
								AbstractInsnNode.appendLongConst(indyXoredKey, indyKeyInsns, constantPoolManager, typeWriters);
								indyKeyInsns.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 12));
								indyKeyInsns.add(InsnNode.of(LXOR));
							} else {
								int indyEncryptedIndexNoXor = indyIndex ^ (int)indyKey;
								this.pushIntConstant(indyEncryptedIndexNoXor, indyIndexInsns, constantPoolManager, typeWriters, entryCount);
								AbstractInsnNode.appendLongConst(indyKey, indyKeyInsns, constantPoolManager, typeWriters);
							}

							InvokeDynamicNode indyInvokeInsn = new InvokeDynamicNode(indyCallWriter);
							this.insertInstructionBlock(indyIndexInsns, indyKeyInsns, indyInvokeInsn, instructionBlocks, methodAnalyzer, i, 1);
					}

					if (!entryIterator.hasNext()) {
						break;
					}

					longConstantEntry = entryIterator.next();
					targetInstruction = longConstantEntry.getKey().getValue();
				}
			}

			this.applyInstructionPatches(instructionBlocks, "Long Constant Encryption", isClinit);
			if (!isClinit) {
				CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
				int maxStack = codeAttribute.getMaxStack();
				codeAttribute.setMaxStack(maxStack + extraStack);
			}
		}
	}

	public void collectSerialPersistentFields(List<FieldNode> serialFields, AbstractWorkspace workspace) {
		String classInternalName = ((ClassNode)this.getClassNode()).getInternalName();
		String fieldDescriptor = null;
		String fieldName = null;
		int mode = 1;
		int instructionCount = this.instructions.size();
		boolean serialFieldsIsArray = false;
		int insnIndex = instructionCount - 1;

		while (true) {
			label160: {
				if (insnIndex >= 0) {
					AbstractInsnNode currentInsn = this.instructions.get(insnIndex);
					int opcode = currentInsn.getOpcode();
					switch (mode) {
						case 1:
							if (opcode == PUTSTATIC) {
								FieldTypeWriter persistentFieldsWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)currentInsn).getTypeWriter();
								if (persistentFieldsWriter.getName().trim().equals("serialPersistentFields") && persistentFieldsWriter.getOwnerName().trim().equals(classInternalName)) {
									mode = 2;
								}
							}
							break label160;
						case 2:
							if (opcode == INVOKESPECIAL) {
								AbstractMethodCallWriter constructorWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)currentInsn).getTypeWriter();
								if (constructorWriter.getName().trim().equals("<init>")
									&& constructorWriter.getOwnerName().trim().equals("java/io/ObjectStreamField")
									&& constructorWriter.getDescriptor().trim().equals("(Ljava/lang/String;Ljava/lang/Class;)V")) {
									mode = 3;
								}
							} else if (opcode == ANEWARRAY) {
								if (((ClassInfoWriter)((WideConstantPoolInsnNode)currentInsn).getTypeWriter()).getInternalName().trim().equals("java/io/ObjectStreamField")) {
									serialFieldsIsArray = true;
									mode = 4;
								}
							} else if (opcode == GETSTATIC) {
								FieldTypeWriter noFieldsWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)currentInsn).getTypeWriter();
								if (noFieldsWriter.getName().trim().equals("NO_FIELDS") && noFieldsWriter.getOwnerName().trim().equals("java/io/ObjectStreamClass")) {
									mode = 4;
								}
							}
							break label160;
						case 3:
							if (opcode != LDC && opcode != LDC_W) {
								if (opcode != GETSTATIC && opcode != GETFIELD) {
									if (opcode == NEW) {
										if (!((ClassInfoWriter)((WideConstantPoolInsnNode)currentInsn).getTypeWriter()).getInternalName().trim().equals("java/io/ObjectStreamField")) {
											break label160;
										}

										mode = 2;
										if (fieldDescriptor != null && fieldName != null) {
											FieldNode fieldNode = workspace.getFieldByNameAndDescriptor(classInternalName, fieldName, fieldDescriptor);
											if (fieldNode == null) {
												throw new ProcessingException("Unexpected structure (06) (" + fieldDescriptor + "," + fieldName + ")");
											}

											serialFields.add(fieldNode);
											fieldDescriptor = null;
											fieldName = null;
											break label160;
										}

										throw new ProcessingException("Unexpected structure (07) ('" + fieldDescriptor + "', '" + (fieldName != null) + "')");
									}

									if (opcode == ANEWARRAY && ((ClassInfoWriter)((WideConstantPoolInsnNode)currentInsn).getTypeWriter()).getInternalName().trim().equals("java/io/ObjectStreamField")) {
										mode = 4;
									}
									break label160;
								}

								FieldTypeWriter elementFieldWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)currentInsn).getTypeWriter();
								if (fieldDescriptor == null) {
									if (!elementFieldWriter.getDescriptor().equals("Ljava/lang/Class;") || !elementFieldWriter.getName().equals("TYPE")) {
										throw new ProcessingException("Unexpected structure (04) : '" + elementFieldWriter.getValueAsString() + "'");
									}

									String ownerDescriptor = TypeWriter.getDescriptorForWrapper(elementFieldWriter.getOwnerName());
									if (ownerDescriptor == null) {
										throw new ProcessingException("Unexpected structure (03) : '" + elementFieldWriter.getValueAsString() + "'");
									}

									fieldDescriptor = ownerDescriptor;
								}
								break label160;
							}

							TypeWriter ldcTypeWriter;
							if (opcode == LDC) {
								ldcTypeWriter = ((LdcNode)currentInsn).getTypeWriter();
							} else {
								ldcTypeWriter = ((WideConstantPoolInsnNode)currentInsn).getTypeWriter();
							}

							if (ldcTypeWriter instanceof ClassInfoWriter) {
								ClassInfoWriter classWriter = (ClassInfoWriter)ldcTypeWriter;
								if (fieldDescriptor != null) {
									throw new ProcessingException("Unexpected structure (01) : '" + fieldDescriptor + "' : '" + classWriter.getInternalName() + "'");
								}

								fieldDescriptor = classWriter.getInternalName();
							} else if (ldcTypeWriter instanceof StringNode) {
								UTFStringWriter utf8StringWriter = ((StringNode)ldcTypeWriter).getUtf8StringWriter();
								if (fieldDescriptor == null) {
									fieldDescriptor = utf8StringWriter.getValue();
								} else {
									if (fieldName != null) {
										throw new ProcessingException("Unexpected structure (02) : '" + fieldName + "' : '" + fieldDescriptor + "' : '" + utf8StringWriter.getValue() + "'");
									}

									fieldName = utf8StringWriter.getValue();
								}
							}
							break label160;
						case 4:
							if (serialFieldsIsArray) {
								int serialFieldCount = serialFields.size();
								switch (opcode) {
									case ICONST_0:
										if (serialFieldCount != 0) {
											throw new ProcessingException("Unexpected structure (08) (" + serialFieldCount + ",0)");
										}
										break;
									case ICONST_1:
										if (serialFieldCount != 1) {
											throw new ProcessingException("Unexpected structure (08) (" + serialFieldCount + ",1)");
										}
										break;
									case ICONST_2:
										if (serialFieldCount != 2) {
											throw new ProcessingException("Unexpected structure (08) (" + serialFieldCount + ",2)");
										}
										break;
									case ICONST_3:
										if (serialFieldCount != 3) {
											throw new ProcessingException("Unexpected structure (08) (" + serialFieldCount + ",3)");
										}
										break;
									case ICONST_4:
										if (serialFieldCount != 4) {
											throw new ProcessingException("Unexpected structure (08) (" + serialFieldCount + ",4)");
										}
										break;
									case ICONST_5:
										if (serialFieldCount != 5) {
											throw new ProcessingException("Unexpected structure (08) (" + serialFieldCount + ",5)");
										}
									case LCONST_0:
									case LCONST_1:
									case FCONST_0:
									case FCONST_1:
									case FCONST_2:
									case DCONST_0:
									case DCONST_1:
									default:
										break;
									case BIPUSH:
										BiPushNode pushNode = (BiPushNode)currentInsn;
										if (serialFieldCount != pushNode.getValue()) {
											throw new ProcessingException("Unexpected structure (08) (" + serialFieldCount + "," + pushNode.getValue() + ")");
										}
								}
							}
							break;
						default:
							break label160;
					}
				}

				if (mode != 4) {
					throw new ProcessingException("Unexpected structure (09) mode=" + mode);
				}

				return;
			}

			insnIndex--;
		}
	}

	public void readInstructions(
		ByteDataBuffer dataBuffer,
		int bytecodeLength,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		LocalVariableTable localVariableTable,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences
	) throws IOException {
		int instructionOffset = 0;
		AbstractConstantPoolWriter constantPoolWriter = this.getConstantPoolWriter();

		while (instructionOffset < bytecodeLength) {
			AbstractInsnNode insnNode;
			try {
				insnNode = AbstractInsnNode.readInstruction(dataBuffer, instructionOffset, elementsByOffset, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences, constantPoolWriter, localVariableTable);
			} catch (UnknownOpcodeException unknownOpcodeException) {
				throw new ProcessingException(unknownOpcodeException.getMessage() + " in " + this.getParentString() + " : " + this.getRawSourcePath(), unknownOpcodeException);
			}

			if (insnNode.isString()) {
				this.stringConstantCount++;
			}

			if (insnNode.isIntegerConstant2()) {
				this.intConstantCount++;
			}

			if (insnNode.isLong()) {
				this.longConstantCount++;
			}

			insnNode.setOffset(instructionOffset);
			this.instructions.add(insnNode);
			instructionOffset += insnNode.getBytecodeSize();
		}
	}

	private boolean resolveReflectionMethods(
		ReflectionMethodPattern reflectionPattern,
		ReflectionMethodCallTracker callTracker,
		Set<Reference> classReferences,
		Set<Reference> memberNameReferences,
		Set<Reference> memberTypeReferences,
		boolean hasReturnType,
		Set<StringNode> processedStringNodes,
		DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferences,
		DualKeyMap<AbstractMethodNode, Reference, Reference> stringMethodNameReferences,
		Set<StructureNode> resolvedNodes,
		AtomicReference<String> errorMessage,
		ClassRepository classRepository,
		AbstractWorkspace workspace
	) {
		boolean resolved = true;

		for (Reference classReference : classReferences) {
			String typeName = classReference.getTypeName();
			if (typeName != null && typeName.length() > 0) {
				String internalName = GlobalUtils.binaryToInternalName(typeName);
				AbstractClassNode classNode = findClassFromRepository(internalName, classRepository);
				if (classNode != null) {
					resolved = this.matchReflectionMethod(reflectionPattern, classReference, classNode, memberNameReferences, memberTypeReferences, hasReturnType, processedStringNodes, stringClassReferences, stringMethodNameReferences, resolvedNodes, errorMessage, workspace, resolved);
				} else if (classReference instanceof InstructionTypeReference) {
					try {
						LibraryClassNode libraryClassNode = classRepository.loadClassOrThrow(internalName);
						if (libraryClassNode != null) {
							resolved = this.resolveReflectionMethodInLibrary(reflectionPattern, classReference, libraryClassNode, memberNameReferences, memberTypeReferences, hasReturnType, errorMessage, workspace, resolved);
						} else {
							resolved = false;
						}
					} catch (FileException ignored) {
						resolved = false;
					}
				}
			} else {
				resolved = false;
			}
		}

		return resolved;
	}

	public int[] getInstructionOffsets() {
		int instructionCount = this.instructions.size();
		int[] offsets = new int[instructionCount];
		int bytecodeOffset = 0;

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode insnNode = this.instructions.get(i);
			offsets[i] = bytecodeOffset;
			bytecodeOffset += insnNode.getBytecodeSize();
		}

		return offsets;
	}

	public void obfuscateControlFlow(
		Set<InstructionListNode> primaryFlowTargets,
		Set<InstructionListNode> secondaryFlowTargets,
		Condition groupCondition,
		Condition packageCondition,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager,
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		AbstractWorkspace workspace,
		TypeResolver typeResolver,
		ClassProvider classProvider,
		Random random
	) {
		boolean primaryTarget = primaryFlowTargets.contains(this);
		boolean secondaryTarget = secondaryFlowTargets.contains(this);
		ArrayList<Integer> tailVariableSlots = new ArrayList<>();
		this.applyFlowObfuscation(primaryTarget, secondaryTarget, instructionBlocks, groupCondition, packageCondition, memberObfuscationRefs, typeWriters, tailVariableSlots, constantPoolManager, methodAnalyzerCache, workspace, typeResolver, classProvider, random);
		List<InstructionBlock> blocks = instructionBlocks.get(this);
		boolean tailVariablesAdjusted = !tailVariableSlots.isEmpty() && Options.ZKM_ADJUST_LOCALS;
		this.applyInstructionPatches(blocks, "Flow Obfuscation", tailVariablesAdjusted);
		if (!tailVariableSlots.isEmpty() && Options.ZKM_ADJUST_LOCALS) {
			this.moveTailVariables(tailVariableSlots);
			int originalBytecodeLength = this.getByteCodeLength();
			this.recalculateOffsets();
			this.setModified(true);
			if (this.getByteCodeLength() > 65535) {
				throw new ProcessingException(
					"Flow Obfuscation : Bytecode length greater than 65535 in "
						+ this.getParentString()
						+ " in file '"
						+ this.getRawSourcePath()
						+ "' (3). (original="
						+ originalBytecodeLength
						+ ", new="
						+ this.getByteCodeLength()
						+ ") : 'Flow Obfuscation'"
				);
			}
		}
	}

	public int getInstructionCount() {
		return this.instructions.size();
	}

	private void applyFlowObfuscation(
		boolean primaryFlowTarget,
		boolean secondaryFlowTarget,
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		Condition groupCondition,
		Condition packageCondition,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		List<TypeWriter> typeWriters,
		List<Integer> tailVariableSlots,
		ConstantPoolManager constantPoolManager,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		AbstractWorkspace workspace,
		TypeResolver typeResolver,
		ClassProvider classProvider,
		Random random
	) {
		List<InstructionBlock> blocks = instructionBlocks.get(this);
		int labeledPatchCount = 0;
		LabeledInstructionBlockPatch flaggedPatch = null;
		int insertionOffset = this.instructions.size() - 1;
		InstructionBlock previousBlock = null;
		int lastExitInstruction = this.findLastExitInstruction();
		if (blocks != null) {
			int initialBlockCount = blocks.size();

			for (int i = 0; i < initialBlockCount; i++) {
				InstructionBlock block = blocks.get(i);
				if (block.getOffset() < insertionOffset) {
					insertionOffset = block.getOffset();
					previousBlock = block;
				}

				if (block instanceof LabeledInstructionBlockPatch) {
					labeledPatchCount++;
					if (flaggedPatch == null && ((LabeledInstructionBlockPatch)block).isFlagged()) {
						flaggedPatch = (LabeledInstructionBlockPatch)block;
					}
				}
			}
		} else {
			if (instructionBlocks.isThreadSafe()) {
				blocks = new Vector<>();
			} else {
				blocks = new ArrayList<>();
			}

			instructionBlocks.put(this, blocks);
		}

		if (primaryFlowTarget || secondaryFlowTarget) {
			insertionOffset = Math.min(lastExitInstruction - 1, insertionOffset);
			if (lastExitInstruction - 1 < insertionOffset) {
				insertionOffset = lastExitInstruction - 1;
				previousBlock = null;
			}
		}

		if (labeledPatchCount > 0 || primaryFlowTarget || secondaryFlowTarget) {
			MethodAnalyzer methodAnalyzer = this.getOrCreateMethodAnalyzerFromCache(methodAnalyzerCache, typeResolver, workspace);
			boolean getterCalled = false;
			WideConstantPoolInsnNode packageConditionInsn = null;
			if (labeledPatchCount > 0 || primaryFlowTarget || secondaryFlowTarget) {
				if (packageCondition.canCallBooleanGetter() && random.nextBoolean()) {
					packageConditionInsn = packageCondition.createBooleanGetterInsn(constantPoolManager, typeWriters);
					getterCalled = true;
				} else {
					packageConditionInsn = packageCondition.createFieldReadInsn(constantPoolManager, typeWriters);
				}

				this.trackMemberForObfuscation(packageConditionInsn, memberObfuscationRefs);
			}

			boolean groupGetterCalled = false;
			WideConstantPoolInsnNode groupConditionInsn = null;
			if (primaryFlowTarget || secondaryFlowTarget) {
				if (groupCondition.canCallBooleanGetter() && random.nextBoolean()) {
					groupConditionInsn = groupCondition.createBooleanGetterInsn(constantPoolManager, typeWriters);
					groupGetterCalled = true;
				} else {
					groupConditionInsn = groupCondition.createFieldReadInsn(constantPoolManager, typeWriters);
				}

				this.trackMemberForObfuscation(groupConditionInsn, memberObfuscationRefs);
			}

			CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
			int newLocalCount = codeAttribute.getMaxLocals() + 1;
			codeAttribute.setMaxLocals(newLocalCount);
			int tailVariableSlot = newLocalCount - 1;
			tailVariableSlots.add(INTEGER_CACHE.valueOf(tailVariableSlot));
			AbstractInsnNode slotLoadInsn = packageCondition.createLoadInsn(tailVariableSlot, this.localVariableTable);
			AbstractInsnNode slotStoreInsn = packageCondition.createStoreInsn(tailVariableSlot, this.localVariableTable);
			ArrayList<AbstractInsnNode> packageInsns = new ArrayList<>(1);
			packageInsns.add(packageConditionInsn);
			AtomicInt insertionPointCounter = new AtomicInt();
			OrderedSet<Integer> safeInsertionPoints = this.findSafeInsertionPoints(methodAnalyzer, insertionOffset, insertionPointCounter);
			this.insertObfuscationBlock(packageInsns, slotStoreInsn, blocks, methodAnalyzer, insertionOffset, previousBlock, insertionPointCounter.get(), safeInsertionPoints, random);
			Collections.sort(blocks);
			InstructionBlock previouslyPatchedBlock = null;
			ArrayList<InstructionBlock> patchBlocks = new ArrayList<>(blocks);
			int patchInsertionCounter = 0;

			for (int blockIndex = 0; blockIndex < patchBlocks.size(); blockIndex++) {
				InstructionBlock candidateBlock = patchBlocks.get(blockIndex);
				if (candidateBlock instanceof LabeledInstructionBlockPatch) {
					LabeledInstructionBlockPatch patch = (LabeledInstructionBlockPatch)candidateBlock;
					JumpInsnNode patchJumpInsn;
					if (patch.isFlagged() != getterCalled) {
						patchJumpInsn = packageCondition.createJumpIfTrue(patch.getLabelNode());
					} else {
						patchJumpInsn = packageCondition.createJumpIfFalse(patch.getLabelNode());
					}

					if (!Options.dontRelocateFlowPatchLoad) {
						OrderedSet<Integer> patchInsertionPoints = this.findReachableInsertionPoints(methodAnalyzer, patch.getOffset(), previouslyPatchedBlock != null ? previouslyPatchedBlock.getOffset() : -1, null);
						ArrayList<AbstractInsnNode> patchInsns = new ArrayList<>(2);
						if (!patchInsertionPoints.isEmpty()) {
							int lastInsertionPoint = patchInsertionPoints.get(patchInsertionPoints.size() - 1);
							if (lastInsertionPoint < patch.getOffset()) {
								blocks.add(patchInsertionCounter, new InstructionBlock(slotLoadInsn, lastInsertionPoint, 0, 1));
								patchInsertionCounter++;
							} else {
								patchInsns.add(slotLoadInsn);
							}
						} else {
							patchInsns.add(slotLoadInsn);
						}

						patchInsns.add(patchJumpInsn);
						patch.prependInstructions(patchInsns);
						patch.setAdditionalStackRequired(1);
					} else {
						ArrayList<AbstractInsnNode> patchInsns = new ArrayList<>(2);
						patchInsns.add(slotLoadInsn);
						patchInsns.add(patchJumpInsn);
						patch.prependInstructions(patchInsns);
						patch.setAdditionalStackRequired(1);
					}
				}

				previouslyPatchedBlock = candidateBlock;
				patchInsertionCounter++;
			}

			if (secondaryFlowTarget) {
				List<AbstractInsnNode> conditionalBlock = this.generateConditionalBlock(tailVariableSlot, groupConditionInsn, slotLoadInsn, groupCondition, packageCondition, groupGetterCalled, memberObfuscationRefs, typeWriters, constantPoolManager, workspace, classProvider, random);
				blocks.add(new InstructionBlock(conditionalBlock, lastExitInstruction - 1, 0, 1));
			}

			if (primaryFlowTarget) {
				List<AbstractInsnNode> dummyJumpInsns = this.generateDummyJump(groupCondition, groupConditionInsn, memberObfuscationRefs, typeWriters, constantPoolManager, tailVariableSlots, workspace, classProvider, random);
				if (flaggedPatch != null) {
					flaggedPatch.appendInstructions(dummyJumpInsns);
				} else {
					LabelNode dummyLabel = new LabelNode(true, 1);
					int dummyJumpCount = dummyJumpInsns.size();
					ArrayList<AbstractInsnNode> jumpInsns = new ArrayList<>(dummyJumpCount + 3);
					jumpInsns.add(slotLoadInsn);
					if (getterCalled) {
						jumpInsns.add(packageCondition.createJumpIfFalse(dummyLabel));
					} else {
						jumpInsns.add(packageCondition.createJumpIfTrue(dummyLabel));
					}

					for (int jumpIndex = 0; jumpIndex < dummyJumpCount; jumpIndex++) {
						jumpInsns.add(dummyJumpInsns.get(jumpIndex));
					}

					jumpInsns.add(dummyLabel);
					blocks.add(new InstructionBlock(jumpInsns, lastExitInstruction - 1, 0, 1));
				}
			}
		}
	}

	public void insertLabelNodes(MultiMap<Integer, IGraphElement> elementsByOffset) {
		int elementCount = elementsByOffset.size();
		if (elementCount != 0) {
			ArrayList<Integer> offsets = new ArrayList<>(elementsByOffset.keySet());
			Collections.sort(offsets);
			Iterator<Integer> offsetIterator = offsets.iterator();
			Integer currentOffset = offsetIterator.next();
			int instructionCount = this.instructions.size();
			int addedLabelCount = 0;
			ArrayList<AbstractInsnNode> newInstructions = new ArrayList<>(instructionCount + elementCount);
			int bytecodeOffset = 0;

			for (int i = 0; i < this.instructions.size(); i++) {
				AbstractInsnNode insnNode = this.instructions.get(i);
				if ((i > 0 || !insnNode.isLabel() || !((LabelNode)insnNode).isFixed()) && bytecodeOffset == currentOffset) {
					LabelNode label;
					if (insnNode.isLabel()) {
						label = (LabelNode)insnNode;
					} else {
						label = new LabelNode(bytecodeOffset, 0);
						newInstructions.add(label);
						addedLabelCount++;
					}

					for (IGraphElement element : elementsByOffset.get(currentOffset)) {
						label.addUsage(element.getSettingFlag());
						element.setLabel(currentOffset, label);
					}

					if (!offsetIterator.hasNext()) {
						newInstructions.addAll(this.instructions.subList(i, instructionCount));
						break;
					}

					currentOffset = offsetIterator.next();
				}

				newInstructions.add(insnNode);
				bytecodeOffset += insnNode.getBytecodeSize();
			}

			this.instructions = newInstructions;
		}
	}

	public int getCodeLengthWithSizeField() {
		return 4 + this.getByteCodeLength();
	}

	private boolean matchReflectionMethod(
		ReflectionMethodPattern reflectionPattern,
		Reference classReference,
		AbstractClassNode classNode,
		Set<Reference> memberNameReferences,
		Set<Reference> memberTypeReferences,
		boolean hasReturnType,
		Set<StringNode> processedStringNodes,
		DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferences,
		DualKeyMap<AbstractMethodNode, Reference, Reference> stringMethodNameReferences,
		Set<StructureNode> resolvedNodes,
		AtomicReference<String> errorMessage,
		AbstractWorkspace workspace,
		boolean resolved
	) {
		for (Reference nameReference : memberNameReferences) {
			String typeName = nameReference.getTypeName();
			if (typeName != null && typeName.length() > 0) {
				for (Reference typeReference : memberTypeReferences) {
					Set<MethodParameterKey> parameterKeys = this.createMethodParameterKeys(typeName, hasReturnType, typeReference);
					if (parameterKeys != null) {
						AbstractMethodNode matchedMethod = null;

						for (MethodParameterKey parameterKey : parameterKeys) {
							AbstractMethodNode candidateMethod;
							if (classNode.isWorkspaceNode()) {
								candidateMethod = AbstractMethodNode.resolveByCovariantReturn(workspace.getMethodNodesByParameterKey((ClassNode)classNode, parameterKey), workspace);
							} else {
								candidateMethod = AbstractMethodNode.resolveByCovariantReturn(classNode.findMethods(parameterKey), workspace);
							}

							if (candidateMethod != null) {
								if (matchedMethod != null) {
									matchedMethod = null;
									break;
								}

								matchedMethod = candidateMethod;
							}
						}

						if (matchedMethod != null) {
							stringMethodNameReferences.put(matchedMethod, nameReference, nameReference);
							resolvedNodes.add(matchedMethod);
							if (nameReference instanceof StringReference) {
								processedStringNodes.add(((StringReference)nameReference).getStringNode());
							}
						} else {
							resolved = false;
						}
					} else {
						resolved = false;
					}
				}
			} else {
				resolved = false;
			}
		}

		return resolved;
	}

	private void replaceCallsWithReflection(
		List<InstructionBlock> instructionBlocks, List<AbstractMethodNode> reflectedMethods, Map<String, AbstractMethodCallWriter> writersByName, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters
	) {
		int instructionCount = this.instructions.size();
		ClassInfoWriter objectClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Object", typeWriters);

		for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
			AbstractInsnNode instruction = this.instructions.get(instructionIndex);
			if (instruction.isInvoke()) {
				AbstractMethodNode invokedMethod = (AbstractMethodNode)((AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter()).getTargetMember();
				if (invokedMethod != null && reflectedMethods.contains(invokedMethod)) {
					if (instruction.getOpcode() == INVOKEINTERFACE) {
						((InvokeInterfaceNode)instruction).setParameterStackSize(2);
					}

					List<String> parameterDescriptors = invokedMethod.getParameterDescriptors();
					int parameterCount = parameterDescriptors.size();
					ArrayList<AbstractInsnNode> reflectionInsns = new ArrayList<>(parameterCount + 2);
					reflectionInsns.add(AbstractInsnNode.ldcInt(parameterCount, constantPoolManager, typeWriters));
					reflectionInsns.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClassInfo));

					for (int parameterIndex = parameterCount - 1; parameterIndex >= 0; parameterIndex--) {
						AbstractMethodCallWriter callWriter = null;
						String parameterDescriptor = parameterDescriptors.get(parameterIndex);
						if (parameterDescriptor.length() == 1) {
							switch (parameterDescriptor.charAt(0)) {
								case 'B':
								case 'C':
								case 'I':
								case 'S':
									if (parameterIndex > 1) {
										String nextNextParameterDescriptor = parameterDescriptors.get(parameterIndex - 2);
										String nextParameterDescriptor = parameterDescriptors.get(parameterIndex - 1);
										if ((nextNextParameterDescriptor.equals("B") || nextNextParameterDescriptor.equals("S") || nextNextParameterDescriptor.equals("C") || nextNextParameterDescriptor.equals("I"))
											&& (nextParameterDescriptor.equals("B") || nextParameterDescriptor.equals("S") || nextParameterDescriptor.equals("C") || nextParameterDescriptor.equals("I"))) {
											reflectionInsns.add(AbstractInsnNode.ldcInt(parameterIndex - 2, constantPoolManager, typeWriters));
											callWriter = writersByName.get("III");
											parameterIndex -= 2;
										}
									}

									if (callWriter == null && parameterIndex > 0) {
										String followingParameterDescriptor = parameterDescriptors.get(parameterIndex - 1);
										if (followingParameterDescriptor.equals("B") || followingParameterDescriptor.equals("S") || followingParameterDescriptor.equals("C") || followingParameterDescriptor.equals("I")) {
											reflectionInsns.add(AbstractInsnNode.ldcInt(parameterIndex - 1, constantPoolManager, typeWriters));
											callWriter = writersByName.get("II");
											parameterIndex--;
										}
									}

									if (callWriter == null) {
										reflectionInsns.add(AbstractInsnNode.ldcInt(parameterIndex, constantPoolManager, typeWriters));
										callWriter = writersByName.get("I");
									}
									break;
								default:
									reflectionInsns.add(AbstractInsnNode.ldcInt(parameterIndex, constantPoolManager, typeWriters));
									callWriter = writersByName.get(parameterDescriptor);
							}
						} else if (parameterIndex > 1 && parameterDescriptors.get(parameterIndex - 1).length() > 1 && parameterDescriptors.get(parameterIndex - 2).length() > 1) {
							reflectionInsns.add(AbstractInsnNode.ldcInt(parameterIndex - 2, constantPoolManager, typeWriters));
							callWriter = writersByName.get("OOO");
							parameterIndex -= 2;
						} else if (parameterIndex > 0 && parameterDescriptors.get(parameterIndex - 1).length() > 1) {
							reflectionInsns.add(AbstractInsnNode.ldcInt(parameterIndex - 1, constantPoolManager, typeWriters));
							callWriter = writersByName.get("OO");
							parameterIndex--;
						} else {
							reflectionInsns.add(AbstractInsnNode.ldcInt(parameterIndex, constantPoolManager, typeWriters));
							callWriter = writersByName.get("O");
						}

						reflectionInsns.add(new WideConstantPoolInsnNode(INVOKESTATIC, callWriter));
					}

					instructionBlocks.add(new InstructionBlock(reflectionInsns, instructionIndex - 1, 0, 2));
				}
			}
		}
	}

	public int getMaxLocals() {
		return ((CodeAttribute)this.getParent()).getMaxLocals();
	}

	private boolean isStringBuilderAppendChain(int instructionIndex, String expectedOwnerName, AtomicInt consumedCount, Set<AbstractMethodNode> reflectedMethods) {
		ArrayList<String> appendChainStack = new ArrayList<>();
		appendChainStack.add(expectedOwnerName);
		boolean appendChainDetected = false;
		boolean scanComplete = false;
		int consumedInstructionCount = 1;

		do {
			consumedInstructionCount++;
			AbstractInsnNode currentInsn = this.instructions.get(instructionIndex);
			AbstractMethodCallWriter callWriter = null;
			AbstractMethodNode targetMethod = null;
			if (currentInsn.isInvoke()) {
				callWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)currentInsn).getTypeWriter();
				targetMethod = (AbstractMethodNode)callWriter.getTargetMember();
			}

			if (currentInsn.getOpcode() == INVOKEVIRTUAL && appendChainStack.get(appendChainStack.size() - 1).equals(expectedOwnerName)) {
				if (callWriter.getName().equals("append")) {
					if (callWriter.getParameterStackSize() == 1) {
						appendChainStack.add("SINGLE_SLOT");
					} else {
						appendChainStack.add("DOUBLE_SLOT");
					}
				} else {
					scanComplete = true;
					appendChainDetected = false;
				}
			} else if (currentInsn.getOpcode() == INVOKESPECIAL && appendChainStack.size() == 1 && appendChainStack.get(appendChainStack.size() - 1).equals(expectedOwnerName)) {
				if (!callWriter.isConstructorCall() || !callWriter.getDescriptor().equals("()V")) {
					appendChainDetected = false;
				} else if (instructionIndex - 2 >= 0 && appendChainStack.size() == 1 && this.instructions.get(instructionIndex - 1).getOpcode() == DUP && this.instructions.get(instructionIndex - 2).getOpcode() == NEW) {
					consumedInstructionCount += 2;
					appendChainDetected = true;
				} else {
					appendChainDetected = false;
				}

				scanComplete = true;
			} else if (currentInsn.isStandaloneStackProducer() && appendChainStack.size() == 2) {
				appendChainStack.remove(appendChainStack.size() - 1);
			} else if (currentInsn.getOpcode() == GETFIELD && this.instructions.get(instructionIndex - 1).isStandaloneStackProducer()) {
				appendChainStack.remove(appendChainStack.size() - 1);
				consumedInstructionCount++;
				instructionIndex--;
			} else if (currentInsn.isInvoke()
				&& callWriter.getStackReturnType() != null
				&& targetMethod != null
				&& reflectedMethods.contains(targetMethod)
				&& !callWriter.getOwnerName().equals("java/lang/StringBuffer")
				&& !callWriter.getOwnerName().equals("java/lang/StringBuilder")) {
				targetMethod.getNormalizedParameterDescriptors();
				AtomicInt popCountHolder = new AtomicInt();
				if (this.isMethodCallPopable(instructionIndex - 1, targetMethod, popCountHolder)) {
					int poppedCount = popCountHolder.get();
					consumedInstructionCount += poppedCount;
					instructionIndex -= poppedCount;
					String poppedElement = appendChainStack.remove(appendChainStack.size() - 1);
				} else {
					appendChainDetected = false;
					scanComplete = true;
				}
			} else {
				scanComplete = true;
				appendChainDetected = false;
			}

			instructionIndex--;
		} while (!scanComplete);

		consumedCount.set(consumedInstructionCount);
		return appendChainDetected;
	}

	public boolean removeUnusedMethodCalls(Set<AbstractMethodNode> reflectedMethods, ScriptContext scriptContext) {
		PrintWriter logWriter = scriptContext.getWriter();
		ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
		LinkedHashMap<Integer, AbstractMethodNode> methodByOffset = new LinkedHashMap<>();
		AtomicInt consumedInsnCount = new AtomicInt();
		int instructionCount = this.instructions.size();
		ReferenceGraph<LocalVarOperationType, Integer> localVarReferenceGraph = new ReferenceGraph<>();
		AtomicBool referenceGraphBuilt = new AtomicBool(false);

		for (int insnIndex = 0; insnIndex < instructionCount; insnIndex++) {
			AbstractInsnNode instruction = this.instructions.get(insnIndex);
			if (instruction.isInvoke() && instruction.getOpcode() != INVOKESPECIAL) {
				AbstractMethodCallWriter callWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
				AbstractMethodNode targetMethod = (AbstractMethodNode)callWriter.getTargetMember();
				if (targetMethod != null && reflectedMethods.contains(targetMethod)) {
					String returnType = callWriter.getStackReturnType();
					AbstractInsnNode nextInstruction = this.instructions.get(insnIndex + 1);
					if (returnType != null && this.isValueUsedAfterCall(nextInstruction, insnIndex + 1, localVarReferenceGraph, referenceGraphBuilt)) {
						if (scriptContext.isVerbose()) {
							methodByOffset.put(INTEGER_CACHE.valueOf(insnIndex), targetMethod);
						}
					} else {
						List<String> parameterTypes = callWriter.getStackParameterTypes();
						if (!targetMethod.isStatic()) {
							parameterTypes.add(0, "java/lang/Object");
						}

						if (scriptContext.isVerbose()) {
							AbstractMethodNode enclosingMethod = this.getMethodNode();
							logWriter.println(
								"\tRemoving call to method '"
									+ targetMethod.getSignature()
									+ (targetMethod.isNameChanged() ? " (" + targetMethod.getDisplayName() + ')' : "")
									+ "' in class '"
									+ targetMethod.getRawSourcePath()
									+ "' from within method '"
									+ enclosingMethod.getSignature()
									+ (enclosingMethod.isNameChanged() ? " (" + enclosingMethod.getDisplayName() + ')' : "")
									+ "' (A)"
							);
						}

						ArrayList<AbstractInsnNode> replacementInstructions = new ArrayList<>();
						int blockStartOffset = insnIndex - 1;
						int scanOffset = blockStartOffset;
						int replacedInsnCount = 1 + (returnType == null ? 0 : 1);
						boolean popsRequired = false;
						int parameterCount = parameterTypes.size();

						for (int i = 0; i < parameterCount; i++) {
							int parameterIndex = parameterCount - i - 1;
							String parameterType = parameterTypes.get(parameterIndex);
							if (!Options.skipProducerAdjustment && !popsRequired) {
								AbstractInsnNode parameterProducer = this.instructions.get(scanOffset);
								if (parameterProducer.isStandaloneStackProducer()) {
									replacedInsnCount++;
									blockStartOffset--;
									scanOffset--;
								} else if (parameterProducer.getOpcode() == GETFIELD && this.instructions.get(scanOffset - 1).isStandaloneStackProducer()) {
									replacedInsnCount += 2;
									blockStartOffset -= 2;
									scanOffset -= 2;
								} else if (parameterProducer.isInvoke() && this.isMethodCallRemovable(scanOffset, parameterProducer, reflectedMethods, consumedInsnCount)) {
									int removableCallInsnCount = consumedInsnCount.get();
									replacedInsnCount += removableCallInsnCount;
									blockStartOffset -= removableCallInsnCount;
									scanOffset -= removableCallInsnCount;
								} else {
									boolean appendChainConsumed = false;
									if (!Options.dontHandleStringBuilderChain && parameterProducer.getOpcode() == INVOKEVIRTUAL) {
										AbstractMethodCallWriter mcw = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)parameterProducer).getTypeWriter();
										if (mcw.getName().equals("toString")
											&& mcw.getDescriptor().equals("()Ljava/lang/String;")
											&& (mcw.getOwnerName().equals("java/lang/StringBuffer") || mcw.getOwnerName().equals("java/lang/StringBuilder"))) {
											AtomicInt chainInsnCount = new AtomicInt();
											appendChainConsumed = this.isStringBuilderAppendChain(scanOffset - 1, mcw.getOwnerName(), chainInsnCount, reflectedMethods);
											if (appendChainConsumed) {
												int appendChainInsnCount = chainInsnCount.get();
												replacedInsnCount += appendChainInsnCount;
												blockStartOffset -= appendChainInsnCount;
												scanOffset = blockStartOffset;
											}
										}
									}

									if (!appendChainConsumed) {
										popsRequired = true;
										if (TypeWriter.isDoubleSlotType(parameterType)) {
											replacementInstructions.add(InsnNode.of(POP2));
										} else {
											replacementInstructions.add(InsnNode.of(POP));
										}

										scanOffset--;
									}
								}
							} else if (TypeWriter.isDoubleSlotType(parameterType)) {
								replacementInstructions.add(InsnNode.of(POP2));
							} else {
								replacementInstructions.add(InsnNode.of(POP));
							}
						}

						if (replacedInsnCount > 0 && replacementInstructions.isEmpty()) {
							replacementInstructions.add(InsnNode.of(NOP));
						}

						InstructionBlock patchBlock = new InstructionBlock(replacementInstructions, blockStartOffset, replacedInsnCount, 0);
						instructionBlocks.add(patchBlock);
					}
				}
			}
		}

		if (scriptContext.isVerbose()) {
			AbstractMethodNode methodNode = this.getMethodNode();

			for (InstructionBlock instructionBlock : instructionBlocks) {
				int patchStartOffset = instructionBlock.getOffset() + 1;
				int patchEndOffset = patchStartOffset + instructionBlock.getReplacedInsnCount();

				for (int offset = patchStartOffset; offset < patchEndOffset; offset++) {
					Integer offsetKey = INTEGER_CACHE.valueOf(offset);
					AbstractMethodNode removedMethod = methodByOffset.remove(offsetKey);
					if (removedMethod != null) {
						logWriter.println(
							"\tRemoving call to method '"
								+ removedMethod.getSignature()
								+ (removedMethod.isNameChanged() ? " (" + removedMethod.getDisplayName() + ')' : "")
								+ "' in class '"
								+ removedMethod.getRawSourcePath()
								+ "' from within method '"
								+ methodNode.getSignature()
								+ (methodNode.isNameChanged() ? " (" + methodNode.getDisplayName() + ')' : "")
								+ "' (B)"
						);
					}
				}
			}

			for (Entry<Integer, AbstractMethodNode> entry : methodByOffset.entrySet()) {
				AbstractMethodNode notRemovedMethod = entry.getValue();
				logWriter.println(
					"\tNOT removing call to method '"
						+ notRemovedMethod.getSignature()
						+ (notRemovedMethod.isNameChanged() ? " (" + notRemovedMethod.getDisplayName() + ')' : "")
						+ "' in class '"
						+ notRemovedMethod.getRawSourcePath()
						+ "' from within method '"
						+ methodNode.getSignature()
						+ (methodNode.isNameChanged() ? " (" + methodNode.getDisplayName() + ')' : "")
						+ "' because return value may be used : "
						+ this.instructions.get(entry.getKey() + 1).getOpcodeText()
				);
			}
		}

		boolean callsRemoved = instructionBlocks.size() > 0;
		if (callsRemoved) {
			this.applyPatchesWithoutStackCheck(instructionBlocks, "Remove Method Calls");
		}

		return callsRemoved;
	}

	public void collectClassDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			switch (instruction.getOpcode()) {
				case LDC:
				case LDC_W:
					TypeWriter typeWriter = null;
					if (instruction instanceof LdcNode) {
						typeWriter = ((LdcNode)instruction).getTypeWriter();
					} else if (instruction instanceof WideConstantPoolInsnNode) {
						typeWriter = ((WideConstantPoolInsnNode)instruction).getTypeWriter();
					}

					if (typeWriter instanceof ClassInfoWriter) {
						AbstractClassNode ldcClassNode = ((ClassInfoWriter)typeWriter).getClassNode();
						if (ldcClassNode != null) {
							referencedClasses.add(ldcClassNode);
						}
					}
					break;
				case GETSTATIC:
				case PUTSTATIC:
					FieldTypeWriter staticFieldWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectFieldDependenciesForHierarchy(staticFieldWriter, referencedFields, signatureClasses, referencedClasses);
					break;
				case GETFIELD:
				case PUTFIELD:
					FieldTypeWriter instanceFieldWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectFieldDependenciesForHierarchy(instanceFieldWriter, referencedFields, referencedClasses, referencedClasses);
					break;
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKEINTERFACE:
					AbstractMethodCallWriter virtualCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectMethodDependenciesForHierarchy(virtualCallWriter, referencedMethods, referencedClasses, referencedClasses);
					break;
				case INVOKESTATIC:
					AbstractMethodCallWriter staticCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectMethodDependenciesForHierarchy(staticCallWriter, referencedMethods, signatureClasses, referencedClasses);
					break;
				case INVOKEDYNAMIC:
					InvokeDynamicCallWriter indyCallWriter = (InvokeDynamicCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectIndyDependencies(indyCallWriter, referencedFields, referencedMethods, signatureClasses, referencedClasses);
					break;
				case NEW:
				case ANEWARRAY:
				case CHECKCAST:
				case INSTANCEOF:
				case MULTIANEWARRAY:
					AbstractClassNode referencedClassNode = ((ClassInfoWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter()).getClassNode();
					if (referencedClassNode != null) {
						referencedClasses.add(referencedClassNode);
					}
			}
		}
	}

	public void collectClassDependenciesForRenaming(Set<ClassNode> renamedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> renamedFields, Set<MethodNode> renamedMethods) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			switch (instruction.getOpcode()) {
				case LDC:
				case LDC_W:
					TypeWriter typeWriter = null;
					if (instruction instanceof LdcNode) {
						typeWriter = ((LdcNode)instruction).getTypeWriter();
					} else if (instruction instanceof WideConstantPoolInsnNode) {
						typeWriter = ((WideConstantPoolInsnNode)instruction).getTypeWriter();
					}

					if (typeWriter instanceof ClassInfoWriter) {
						ClassNode ldcClassNode = ((ClassInfoWriter)typeWriter).getWorkspaceClassNode();
						if (ldcClassNode != null) {
							renamedClasses.add(this.getEquivalentClassNode(ldcClassNode));
						}
					}
					break;
				case GETSTATIC:
				case PUTSTATIC:
					FieldTypeWriter staticFieldWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectFieldDependencies(staticFieldWriter, renamedFields, signatureClasses, renamedClasses);
					break;
				case GETFIELD:
				case PUTFIELD:
					FieldTypeWriter instanceFieldWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectFieldDependencies(instanceFieldWriter, renamedFields, renamedClasses, renamedClasses);
					break;
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKEINTERFACE:
					AbstractMethodCallWriter virtualCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectMethodDependencies(virtualCallWriter, renamedMethods, renamedClasses, renamedClasses);
					break;
				case INVOKESTATIC:
					AbstractMethodCallWriter staticCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectMethodDependencies(staticCallWriter, renamedMethods, signatureClasses, renamedClasses);
					break;
				case INVOKEDYNAMIC:
					InvokeDynamicCallWriter indyCallWriter = (InvokeDynamicCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					this.collectIndyDependenciesForRenaming(indyCallWriter, renamedFields, renamedMethods, signatureClasses, renamedClasses);
					break;
				case NEW:
				case ANEWARRAY:
				case CHECKCAST:
				case INSTANCEOF:
				case MULTIANEWARRAY:
					ClassNode referencedClassNode = ((ClassInfoWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter()).getWorkspaceClassNode();
					if (referencedClassNode != null) {
						renamedClasses.add(this.getEquivalentClassNode(referencedClassNode));
					}
			}
		}
	}

	public void setMaxLocals(int maxLocals) {
		((CodeAttribute)this.getParent()).setMaxLocals(maxLocals);
	}

	private void trackMemberForObfuscation(WideConstantPoolInsnNode memberAccessInsn, HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs) {
		AbstractClassNode ownerClass = this.getClassNode();
		if (memberObfuscationRefs != null && ObfuscateReferencesBuilder.indySupported(ownerClass)) {
			MemberNode targetMember = ((MemberTypeWriter)memberAccessInsn.getTypeWriter()).getTargetMember();
			AbstractClassNode memberClass = targetMember.getClassNode();
			MemberContext memberContext = new MemberContext(targetMember, memberClass);
			memberObfuscationRefs.put(memberContext, this, memberAccessInsn);
		}
	}

	public void applyInstructionPatches(List<InstructionBlock> instructionBlocks, String reason, boolean deferOffsetUpdate) {
		this.applyInstructionBlockPatches(instructionBlocks, reason, deferOffsetUpdate, true);
	}

	private String prependReceiverType(String receiverInternalName, String methodDescriptor) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		builder.append("L");
		builder.append(receiverInternalName);
		builder.append(";");
		builder.append(methodDescriptor.substring(1));
		return builder.toString();
	}

	public boolean isPrivateMethod() {
		return ((CodeAttribute)this.getParent()).isPrivate();
	}

	private void collectFieldDependencies(FieldTypeWriter fieldWriter, Set<FieldNode> fields, Set<ClassNode> dependencyClasses, Set<ClassNode> unused) {
		ClassNode ownerClass = ClassHierarchyNode.getWorkspaceClassNode(fieldWriter.getOwnerName());
		if (ownerClass != null) {
			dependencyClasses.add(this.getEquivalentClassNode(ownerClass));
		}

		AbstractFieldNode targetField = (AbstractFieldNode)fieldWriter.getTargetMember();
		if (targetField != null && targetField.isWorkspaceNode()) {
			fields.add((FieldNode)targetField);
			ClassNode fieldClass = (ClassNode)targetField.getClassNode();
			if (fieldClass != ownerClass) {
				dependencyClasses.add(this.getEquivalentClassNode(fieldClass));
			}
		}
	}

	public void obfuscateMethodParams(Long xorKey, Long xorKeyOperand, ParameterPattern parameterPattern, List<InstructionBlock> instructionBlocks, List<TypeWriter> typeWriters, Random random) {
		this.findFirstControlFlowLabel();
		this.obfuscateLongBitwise(xorKey, xorKeyOperand, parameterPattern, instructionBlocks, typeWriters, random);
		this.applyInstructionPatches(instructionBlocks, "Flow Obfuscation using Method Parameter List Changing", false);
	}

	public void obfuscateStringConcat(
		StringConcatAnalysis stringConcatAnalysis,
		ConstantPoolManager constantPoolManager,
		Map<InvokeDynamicCallWriter, String> dynCallNames,
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		MethodAnalyzer methodAnalyzer = null;
		FrameNode[] frames = null;
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			if (instruction.getOpcode() == INVOKEDYNAMIC) {
				InvokeDynamicCallWriter indyCallWriter = ((InvokeDynamicNode)instruction).getIndyCallWriter();
				BootstrapMethodEntry bootstrapEntry = indyCallWriter.getBootstrapMethodEntry();
				if (stringConcatAnalysis.isEntryAnalyzed(bootstrapEntry)) {
					if (methodAnalyzer == null) {
						methodAnalyzer = this.getMethodAnalyzer(typeResolver, classHierarchy);
						frames = methodAnalyzer.getFrames();
					}

					AnalyzedStringConcat analyzedConcat = stringConcatAnalysis.getAnalyzedEntry(bootstrapEntry);
					StackFrameType[] operandStack = null;
					if (i > 0) {
						operandStack = frames[i - 1].getOperandStack();
					}

					InstructionBlock concatBlock = this.buildStringConcatBlock(i, analyzedConcat, indyCallWriter, dynCallNames, operandStack, constantPoolManager, typeWriters);
					instructionBlocks.put(this, concatBlock);
				}
			}
		}
	}

	private MemberContext createMemberContext(MemberNode member, MemberTypeWriter memberTypeWriter, ObfuscateReferencesBuilder referencesBuilder, ClassProvider classProvider) {
		String ownerName = memberTypeWriter.getOwnerName();
		if (ownerName.charAt(0) == '[') {
			ownerName = "java/lang/Object";
		}

		AbstractClassNode ownerClass = this.getClassNode();
		AbstractClassNode resolvedClass = classProvider.getClassNode(ownerName, ownerClass.hasInnerClassOrdinal() ? ownerClass.getInnerClassOrdinal() : null, "in Reference Obfuscation");
		MemberContext memberContext;
		if (member.isWorkspaceNode() && !Options.useRefClassForWorkspaceNodes) {
			memberContext = referencesBuilder.getOrCreateMemberContext(member, member.getClassNode());
		} else {
			memberContext = referencesBuilder.getOrCreateMemberContext(member, resolvedClass);
		}

		return memberContext;
	}

	public int analyzeControlFlow(
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		TypeResolver typeResolver,
		ScriptContext scriptContext,
		ClassHierarchy classHierarchy,
		int analysisMode,
		int flowObfuscationConfigId,
		boolean licenseVerified,
		boolean keepBalancedLocks,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		Random random
	) {
		String analysisContext = "analyzing the control flow of method '"
			+ this.getMethodNode().toDeclarationWithParamNames()
			+ "' in class "
			+ this.getRawSourcePath()
			+ "' : '"
			+ this.getInternalName()
			+ "' (B)";
		ArrayList<InstructionBlock> blockPatches = new ArrayList<>();
		int patchCount = 0;

		MethodAnalyzer methodAnalyzer;
		try {
			methodAnalyzer = this.getOrCreateMethodAnalyzerFromCache(methodAnalyzerCache, typeResolver, classHierarchy);
		} catch (StackOverflowError stackOverflowError) {
			throw new ZKMUnexpectedException("StackOverflowError trapped in '" + this.getParentString() + "' (1)", stackOverflowError);
		}

		int firstControlFlowLabelIndex = this.findFirstControlFlowLabel();
		this.minReturnStackDepth = methodAnalyzer.getMinReturnStackDepth();
		FrameNode[] frames = methodAnalyzer.getFrames();
		Set<StackFrameType> monitorStackTypes = methodAnalyzer.collectMonitorStackTypes();
		HashSet<AnalyzingNode> patchedNodes = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(methodAnalyzer.getAnalyzingNodeCount()));
		HashMap<AnalyzingNode, AnalyzingNode> coveredNodes = GlobalUtils.newHashMap();
		List<ControlFlowNode> controlFlowNodes;
		if (flowObfuscationConfigId == 1) {
			controlFlowNodes = new ArrayList<>();
		} else {
			controlFlowNodes = methodAnalyzer.getControlFlowNodes();
		}

		for (int i = 0; i < controlFlowNodes.size(); i++) {
			ControlFlowNode controlFlowNode = controlFlowNodes.get(i);
			if (firstControlFlowLabelIndex <= -1 || !controlFlowNode.containsReachableInstruction(firstControlFlowLabelIndex)) {
				AnalyzingNode anchorNode = controlFlowNode.getAnchorNode();
				AnalyzingNode firstSourceNode = controlFlowNode.getFirstSourceNode();
				coveredNodes.put(anchorNode, anchorNode);
				coveredNodes.put(firstSourceNode, firstSourceNode);
				AnalyzingNode alternateNode = controlFlowNode.getAlternateNode();
				AnalyzingNode fallThroughNode = controlFlowNode.getFallThroughNode();
				if (fallThroughNode != null && alternateNode != null) {
					AtomicInt fallThroughOffset = new AtomicInt(-1);
					AtomicInt alternateOffset = new AtomicInt(-1);
					AtomicReference<AnalyzingNode> alternateFrameNode = new AtomicReference<>();
					this.findCommonStackFrameWithSuccessor(fallThroughOffset, alternateOffset, alternateFrameNode, fallThroughNode, alternateNode, anchorNode, firstSourceNode, monitorStackTypes, frames, typeResolver, scriptContext, keepBalancedLocks, random, analysisContext);
					if (alternateOffset.get() != -1) {
						if (analysisMode == 2) {
							patchedNodes.add(fallThroughNode);
							patchedNodes.add(alternateFrameNode.get());
						}

						LabelNode controlFlowLabel = new LabelNode(true, 1);
						InstructionBlock controlFlowBlock = new InstructionBlock(controlFlowLabel, alternateOffset.get(), 0, 0);
						blockPatches.add(controlFlowBlock);
						LabeledInstructionBlockPatch controlFlowPatch = new LabeledInstructionBlockPatch(false, controlFlowLabel, null, fallThroughOffset.get(), 0, 0);
						patchCount++;
						blockPatches.add(controlFlowPatch);
					}
				}
			}
		}

		List<AnalyzingNode> analyzingNodes = null;
		if (flowObfuscationConfigId == 4) {
			analyzingNodes = methodAnalyzer.getAnalyzingNodes();
			Collections.sort(analyzingNodes);

			for (AnalyzingNode analyzingNode : analyzingNodes) {
				if (!coveredNodes.containsKey(analyzingNode) && (firstControlFlowLabelIndex <= -1 || analyzingNode.getStartIndex() > firstControlFlowLabelIndex)) {
					List<AnalyzingNode> successors = analyzingNode.getSuccessors();
					if (successors != null && successors.size() > 1) {
						AtomicInt sourceFrameOffset = new AtomicInt(-1);
						AtomicInt matchedFrameOffset = new AtomicInt(-1);
						AnalyzingNode frameSourceNode = null;
						AtomicReference<AnalyzingNode> matchedFrameNode = new AtomicReference<>();
						Collections.sort(successors);
						AnalyzingNode firstSuccessor = successors.get(0);
						AnalyzingNode lastSuccessor = successors.get(successors.size() - 1);
						if (analysisMode == 1 || !patchedNodes.contains(firstSuccessor)) {
							if (analysisMode == 1 || !patchedNodes.contains(lastSuccessor)) {
								frameSourceNode = lastSuccessor;
								this.findCommonStackFrame(sourceFrameOffset, matchedFrameOffset, matchedFrameNode, lastSuccessor, firstSuccessor, monitorStackTypes, frames, typeResolver, scriptContext, keepBalancedLocks, random, analysisContext);
							}

							if (matchedFrameOffset.get() == -1 && (analysisMode == 1 || !patchedNodes.contains(analyzingNode))) {
								frameSourceNode = analyzingNode;
								this.findCommonStackFrame(sourceFrameOffset, matchedFrameOffset, matchedFrameNode, analyzingNode, firstSuccessor, monitorStackTypes, frames, typeResolver, scriptContext, keepBalancedLocks, random, analysisContext);
							}

							if (matchedFrameOffset.get() != -1) {
								if (analysisMode == 2) {
									patchedNodes.add(frameSourceNode);
									patchedNodes.add(matchedFrameNode.get());
								}

								LabelNode successorLabel = new LabelNode(true, 1);
								InstructionBlock successorBlock = new InstructionBlock(successorLabel, matchedFrameOffset.get(), 0, 0);
								blockPatches.add(successorBlock);
								LabeledInstructionBlockPatch successorPatch = new LabeledInstructionBlockPatch(false, successorLabel, null, sourceFrameOffset.get(), 0, 0);
								patchCount++;
								blockPatches.add(successorPatch);
							}
						}
					}
				}
			}
		}

		if (flowObfuscationConfigId == 3 || flowObfuscationConfigId == 4) {
			if (analyzingNodes == null) {
				analyzingNodes = methodAnalyzer.getAnalyzingNodes();
				Collections.sort(analyzingNodes);
			}

			for (AnalyzingNode nodeWithSuccessors : analyzingNodes) {
				if (!coveredNodes.containsKey(nodeWithSuccessors) && (firstControlFlowLabelIndex <= -1 || nodeWithSuccessors.getStartIndex() > firstControlFlowLabelIndex)) {
					List<AnalyzingNode> nodeSuccessors = nodeWithSuccessors.getSuccessors();
					if (nodeSuccessors != null && nodeSuccessors.size() > 1) {
						AtomicInt nodeFrameOffset = new AtomicInt(-1);
						AtomicInt successorFrameOffset = new AtomicInt(-1);
						AnalyzingNode nodeFrameSource = null;
						AtomicReference<AnalyzingNode> successorFrameNode = new AtomicReference<>();
						Collections.sort(nodeSuccessors);
						AnalyzingNode firstNodeSuccessor = nodeSuccessors.get(0);
						AnalyzingNode lastNodeSuccessor = nodeSuccessors.get(nodeSuccessors.size() - 1);
						if (analysisMode == 1 || !patchedNodes.contains(nodeWithSuccessors)) {
							if (analysisMode == 1 || !patchedNodes.contains(lastNodeSuccessor)) {
								nodeFrameSource = nodeWithSuccessors;
								this.findCommonStackFrame(nodeFrameOffset, successorFrameOffset, successorFrameNode, nodeWithSuccessors, lastNodeSuccessor, monitorStackTypes, frames, typeResolver, scriptContext, keepBalancedLocks, random, analysisContext);
							}

							if (successorFrameOffset.get() == -1 && (analysisMode == 1 || !patchedNodes.contains(firstNodeSuccessor))) {
								this.findCommonStackFrame(nodeFrameOffset, successorFrameOffset, successorFrameNode, nodeWithSuccessors, firstNodeSuccessor, monitorStackTypes, frames, typeResolver, scriptContext, keepBalancedLocks, random, analysisContext);
							}

							if (successorFrameOffset.get() != -1) {
								if (analysisMode == 2) {
									patchedNodes.add(nodeFrameSource);
									patchedNodes.add(successorFrameNode.get());
								}

								LabelNode nodeLabel = new LabelNode(true, 1);
								InstructionBlock nodeBlock = new InstructionBlock(nodeLabel, successorFrameOffset.get(), 0, 0);
								blockPatches.add(nodeBlock);
								LabeledInstructionBlockPatch nodePatch = new LabeledInstructionBlockPatch(false, nodeLabel, null, nodeFrameOffset.get(), 0, 0);
								patchCount++;
								blockPatches.add(nodePatch);
							}
						}
					}
				}
			}
		}

		analyzingNodes = methodAnalyzer.getAnalyzingNodes();
		Collections.sort(analyzingNodes);
		int instructionCount = this.instructions.size();

		for (AnalyzingNode gotoNode : analyzingNodes) {
			if (firstControlFlowLabelIndex <= -1 || gotoNode.getStartIndex() > firstControlFlowLabelIndex) {
				AbstractInsnNode endInstruction = this.instructions.get(gotoNode.getEndIndex());
				int nextInstructionIndex = gotoNode.getEndIndex() + 1;
				if (endInstruction.getOpcode() == GOTO
					&& gotoNode.getInstructionCount() > 1
					&& nextInstructionIndex < instructionCount
					&& (analysisMode == 1 || !patchedNodes.contains(gotoNode) && !patchedNodes.contains(methodAnalyzer.getNodeByStartIndex(nextInstructionIndex)))) {
					FrameNode endFrame = frames[gotoNode.getEndIndex()];
					FrameNode nextFrame = frames[nextInstructionIndex];
					LabelNode targetLabel = ((JumpInsnNode)endInstruction).getTargetLabel();
					if (endFrame != null
						&& nextFrame != null
						&& targetLabel != this.instructions.get(nextInstructionIndex)
						&& (endFrame.isFullyCompatibleWith(typeResolver, nextFrame, monitorStackTypes, keepBalancedLocks, analysisContext) || !licenseVerified)
						&& endFrame.getInitializationLabel() == nextFrame.getInitializationLabel()) {
						LabeledInstructionBlockPatch gotoPatch = new LabeledInstructionBlockPatch(true, targetLabel, null, gotoNode.getEndIndex() - 1, 1, 0);
						patchCount++;
						blockPatches.add(gotoPatch);
					}
				}
			}
		}

		if (patchCount > 0) {
			instructionBlocks.put(this, blockPatches);
		}

		return patchCount;
	}

	public void recordInvokeOpcode(HashMap<AbstractMethodCallWriter, Integer> opcodeCounts) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			int opcode = instruction.getOpcode();
			switch (opcode) {
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKESTATIC:
				case INVOKEINTERFACE:
					AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					opcodeCounts.put(methodCallWriter, INTEGER_CACHE.valueOf(opcode));
					break;
			}
		}
	}

	public boolean hasLongConstants() {
		return this.longConstantCount > 0;
	}

	private void rewriteMethodParameters(
		ParameterPattern changedPattern,
		long xorKey,
		LocalVariableSlot keyLocalSlot,
		MethodAnalyzer methodAnalyzer,
		int instructionIndex,
		String descriptor,
		AbstractMethodNode targetMethod,
		ParameterPattern newPattern,
		int[] extraParameterSlots,
		MultiMap<Integer, AbstractInsnNode> instructionPatches,
		List<Integer> usedLocalSlots,
		int firstLocalSlot,
		AtomicInt addedLocalSlotCount,
		List<TypeWriter> typeWriters
	) {
		WideConstantPoolInsnNode invokeInstruction = (WideConstantPoolInsnNode)this.instructions.get(instructionIndex);
		int previousInstructionIndex = instructionIndex - 1;
		int stackBaseSlot;
		if (instructionIndex == 0) {
			stackBaseSlot = this.getMethodNode().getFirstNonParamLocalSlot();
		} else {
			stackBaseSlot = methodAnalyzer.getStackSizeAtInsn(previousInstructionIndex);
		}

		List<String> targetParameterTypes = TypeWriter.parseParameterDescriptors(descriptor);
		List<String> newParameterTypes;
		ExtraParameter[] extraParameters;
		if (invokeInstruction.getOpcode() == INVOKEDYNAMIC) {
			InvokeDynamicCallWriter invokeDynamicWriter = ((InvokeDynamicNode)invokeInstruction).getIndyCallWriter();
			AbstractMethodNode lambdaTarget = invokeDynamicWriter.getLambdaParamChangeTarget();
			newParameterTypes = new ArrayList<>();
			extraParameters = this.adjustParameterListForIndy(targetParameterTypes, newPattern, lambdaTarget, newParameterTypes, invokeDynamicWriter);
			StringBuilder argumentDescriptorBuilder = new StringBuilder();

			for (String parameterType : newParameterTypes) {
				argumentDescriptorBuilder.append(parameterType);
			}

			invokeDynamicWriter.getDescriptor();
			invokeDynamicWriter.prependArgumentDescriptor(argumentDescriptorBuilder.toString());
		} else {
			newParameterTypes = TypeWriter.parseParameterDescriptors(newPattern.getDescriptor());
			extraParameters = newPattern.getExtraParameters();
		}

		int targetParameterCount = targetParameterTypes.size();
		int newParameterCount = newParameterTypes.size();
		int firstArgumentSlot = stackBaseSlot - targetParameterCount;
		if (invokeInstruction.getOpcode() == INVOKEINTERFACE) {
			((InvokeInterfaceNode)invokeInstruction).setParameterStackSize(TypeWriter.calculateSlotSize(newParameterTypes) + 1);
		}

		int consumedParameterCount = 0;
		int extraParameterIndex = 0;
		HashSet<Integer> stackProducerIndexes = GlobalUtils.newHashSet();
		HashSet<AnalyzingNode> visitedNodes = GlobalUtils.newHashSet();
		ArrayList<AbstractInsnNode> argumentInstructions = new ArrayList<>();
		MultiMap<Integer, AbstractInsnNode> insertPatchesByOffset = new MultiMap<>();
		MultiMap<Integer, AbstractInsnNode> prependPatchesByOffset = new MultiMap<>();

		for (int newParameterIndex = 0; newParameterIndex < newParameterCount; newParameterIndex++) {
			if (extraParameterIndex < extraParameters.length && newParameterIndex == extraParameters[extraParameterIndex].getParameterIndex()) {
				argumentInstructions.clear();
				char extraParameterType = extraParameters[extraParameterIndex].getTypeChar();
				if (extraParameterType == 'J') {
					argumentInstructions.add(AbstractInsnNode.loadLong(extraParameterSlots[extraParameterIndex], this.localVariableTable, 5));
				} else {
					argumentInstructions.add(AbstractInsnNode.loadInt(extraParameterSlots[extraParameterIndex], this.localVariableTable, 5));
					AbstractInsnNode conversionInstruction = getConversionInstruction(extraParameterType);
					if (conversionInstruction != null) {
						argumentInstructions.add(conversionInstruction);
					}
				}

				if (consumedParameterCount >= targetParameterCount) {
					insertPatchesByOffset.putAll(INTEGER_CACHE.valueOf(previousInstructionIndex), argumentInstructions);
				} else {
					AnalyzingNode analyzingNode = methodAnalyzer.getNodeForInstructionIndex(previousInstructionIndex);
					StackFrameType[] operandStack = methodAnalyzer.getFrames()[previousInstructionIndex].getOperandStack();
					stackProducerIndexes.clear();
					visitedNodes.clear();
					if (methodAnalyzer.collectStackProducerIndexes(analyzingNode, instructionIndex, firstArgumentSlot, stackProducerIndexes, visitedNodes)) {
						if (stackProducerIndexes.size() > 1) {
							for (Integer producerIndex : stackProducerIndexes) {
								int producerInstructionIndex = producerIndex;
								AbstractInsnNode producerInstruction = this.instructions.get(producerInstructionIndex);
								if (producerInstruction.isStandaloneStackProducer() && !producerInstruction.isInvoke()) {
									insertPatchesByOffset.putAll(INTEGER_CACHE.valueOf(producerInstructionIndex - 1), argumentInstructions);
								} else {
									StackFrameType[] producerOperandStack = methodAnalyzer.getFrames()[producerInstructionIndex].getOperandStack();
									ArrayList<AbstractInsnNode> producerInstructions;
									if (producerOperandStack[producerOperandStack.length - 1].isCategory2()) {
										producerInstructions = new ArrayList<>(argumentInstructions.size() + 2);
										producerInstructions.addAll(argumentInstructions);
										if (extraParameterType == 'J') {
											producerInstructions.add(InsnNode.of(DUP2_X2));
											producerInstructions.add(InsnNode.of(POP2));
										} else {
											producerInstructions.add(InsnNode.of(DUP_X2));
											producerInstructions.add(InsnNode.of(POP));
										}
									} else {
										producerInstructions = new ArrayList<>(argumentInstructions.size() + 1);
										producerInstructions.addAll(argumentInstructions);
										if (extraParameterType == 'J') {
											producerInstructions.add(InsnNode.of(DUP2_X1));
											producerInstructions.add(InsnNode.of(POP2));
										} else {
											producerInstructions.add(InsnNode.of(SWAP));
										}
									}

									prependPatchesByOffset.putAll(producerIndex, producerInstructions);
								}
							}
						} else {
							int stackProducerIndex = stackProducerIndexes.iterator().next();
							AbstractInsnNode singleProducerInstruction = this.instructions.get(stackProducerIndex);
							if (singleProducerInstruction.isStandaloneStackProducer() && !singleProducerInstruction.isInvoke()) {
								insertPatchesByOffset.putAll(INTEGER_CACHE.valueOf(stackProducerIndex - 1), argumentInstructions);
							} else {
								StackFrameType[] singleProducerOperandStack = methodAnalyzer.getFrames()[stackProducerIndex].getOperandStack();
								if (operandStack[singleProducerOperandStack.length - 1].isCategory2()) {
									if (extraParameterType == 'J') {
										argumentInstructions.add(InsnNode.of(DUP2_X2));
										argumentInstructions.add(InsnNode.of(POP2));
									} else {
										argumentInstructions.add(InsnNode.of(DUP_X2));
										argumentInstructions.add(InsnNode.of(POP));
									}

									prependPatchesByOffset.putAll(INTEGER_CACHE.valueOf(stackProducerIndex), argumentInstructions);
								} else {
									if (extraParameterType == 'J') {
										argumentInstructions.add(InsnNode.of(DUP2_X1));
										argumentInstructions.add(InsnNode.of(POP2));
									} else {
										argumentInstructions.add(InsnNode.of(SWAP));
									}

									prependPatchesByOffset.putAll(INTEGER_CACHE.valueOf(stackProducerIndex), argumentInstructions);
								}
							}
						}
					} else if (extraParameterSlots.length == 1 && firstArgumentSlot == stackBaseSlot - 1 && !operandStack[stackBaseSlot - 1].isCategory2()) {
						if (extraParameterType == 'J') {
							argumentInstructions.add(InsnNode.of(DUP2_X1));
							argumentInstructions.add(InsnNode.of(POP2));
						} else {
							argumentInstructions.add(InsnNode.of(SWAP));
						}

						insertPatchesByOffset.putAll(INTEGER_CACHE.valueOf(previousInstructionIndex), argumentInstructions);
					} else if (extraParameterSlots.length == 1 && firstArgumentSlot == stackBaseSlot - 1) {
						if (extraParameterType == 'J') {
							argumentInstructions.add(InsnNode.of(DUP2_X2));
							argumentInstructions.add(InsnNode.of(POP2));
						} else {
							argumentInstructions.add(InsnNode.of(DUP_X2));
							argumentInstructions.add(InsnNode.of(POP));
						}

						insertPatchesByOffset.putAll(INTEGER_CACHE.valueOf(previousInstructionIndex), argumentInstructions);
					} else {
						if (extraParameterSlots.length != 1 || firstArgumentSlot != stackBaseSlot - 2 || operandStack[stackBaseSlot - 1].isCategory2() || operandStack[stackBaseSlot - 2].isCategory2()) {
							int localSlotIndex = firstLocalSlot;
							this.localVariableTable.resetWideFlagsFrom(firstLocalSlot);
							insertPatchesByOffset.clear();
							prependPatchesByOffset.clear();
							argumentInstructions.clear();
							ArrayList<AbstractInsnNode> patchInstructions = new ArrayList<>();
							int firstExtraParameterIndex = newPattern.getExtraParameters()[0].getParameterIndex();
							int trailingParameterCount = targetParameterCount - firstExtraParameterIndex;
							int[] trailingParameterSlots = new int[trailingParameterCount];
							int reverseParameterIndex = targetParameterCount - 1;

							for (int trailingIndex = 0; trailingIndex < trailingParameterCount; trailingIndex++) {
								String targetParameterType = targetParameterTypes.get(reverseParameterIndex);
								reverseParameterIndex--;
								boolean takesTwoSlots = TypeWriter.isDoubleSlotType(targetParameterType);
								patchInstructions.add(AbstractInsnNode.createStoreInsnByTypeDesc(targetParameterType, localSlotIndex, this.localVariableTable, 5));
								trailingParameterSlots[trailingIndex] = localSlotIndex;
								if (localSlotIndex > usedLocalSlots.get(usedLocalSlots.size() - 1)) {
									usedLocalSlots.add(INTEGER_CACHE.valueOf(localSlotIndex));
									addedLocalSlotCount.increaseAndGet();
								}

								if (takesTwoSlots) {
									if (++localSlotIndex > usedLocalSlots.get(usedLocalSlots.size() - 1)) {
										usedLocalSlots.add(INTEGER_CACHE.valueOf(localSlotIndex));
										addedLocalSlotCount.increaseAndGet();
									}
								}

								localSlotIndex++;
							}

							int extraParameterCursor = 0;
							int newParameterCursor = firstExtraParameterIndex;
							int unusedParameterIndexCounter = firstExtraParameterIndex;

							for (int slotIndex = trailingParameterSlots.length - 1; newParameterCursor < newParameterCount; newParameterCursor++) {
								String newParameterType = newParameterTypes.get(newParameterCursor);
								if (extraParameterCursor < extraParameters.length && newParameterCursor == extraParameters[extraParameterCursor].getParameterIndex()) {
									char extraParameterTypeChar = extraParameters[extraParameterCursor].getTypeChar();
									if (extraParameterTypeChar == 'J') {
										patchInstructions.add(AbstractInsnNode.loadLong(extraParameterSlots[extraParameterCursor], this.localVariableTable, 5));
									} else {
										patchInstructions.add(AbstractInsnNode.loadInt(extraParameterSlots[extraParameterCursor], this.localVariableTable, 5));
										AbstractInsnNode rebuildConversionInstruction = getConversionInstruction(extraParameterTypeChar);
										if (rebuildConversionInstruction != null) {
											patchInstructions.add(rebuildConversionInstruction);
										}
									}

									extraParameterCursor++;
								} else {
									patchInstructions.add(AbstractInsnNode.createLoadByTypeDesc(newParameterType, trailingParameterSlots[slotIndex], this.localVariableTable, 5));
									slotIndex--;
									unusedParameterIndexCounter++;
									firstArgumentSlot++;
								}
							}

							insertPatchesByOffset.putAll(INTEGER_CACHE.valueOf(previousInstructionIndex), patchInstructions);
							break;
						}

						if (extraParameterType == 'J') {
							argumentInstructions.add(InsnNode.of(DUP2_X2));
							argumentInstructions.add(InsnNode.of(POP2));
						} else {
							argumentInstructions.add(InsnNode.of(DUP_X2));
							argumentInstructions.add(InsnNode.of(POP));
						}

						insertPatchesByOffset.putAll(INTEGER_CACHE.valueOf(previousInstructionIndex), argumentInstructions);
					}
				}

				extraParameterIndex++;
			} else {
				consumedParameterCount++;
				firstArgumentSlot++;
			}
		}

		for (Entry<Integer, List<AbstractInsnNode>> prependPatchEntry : prependPatchesByOffset.entrySet()) {
			instructionPatches.prependAll(prependPatchEntry.getKey(), prependPatchEntry.getValue());
		}

		for (Entry<Integer, List<AbstractInsnNode>> insertPatchEntry : insertPatchesByOffset.entrySet()) {
			instructionPatches.putAll(insertPatchEntry.getKey(), insertPatchEntry.getValue());
		}

		prependPatchesByOffset.clear();
		insertPatchesByOffset.clear();
	}

	public void buildReferenceGraph(
		ReferenceGraphMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> referenceGraphMap, ObfuscateReferencesBuilder referencesBuilder, TypeResolver typeResolver, ClassHierarchy classHierarchy
	) {
		MethodAnalyzer methodAnalyzer = this.getMethodAnalyzer(typeResolver, classHierarchy);
		ClassProvider classProvider = typeResolver.getClassProvider();
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			if (instruction.isInvoke()) {
				AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
				MemberNode targetMethod = methodCallWriter.getTargetMember();
				if (targetMethod != null
					&& !((AbstractMethodNode)targetMethod).isConstructor()
					&& !((AbstractMethodNode)targetMethod).isClinit()
					&& !methodAnalyzer.hasUninitializedTypesAtInsn(i)
					&& (instruction.getOpcode() != INVOKESPECIAL || targetMethod.isPrivate())
					&& (!targetMethod.getNameDisplay().equals("identityHashCode") || !targetMethod.getInternalName().equals("java/lang/System"))
					&& (!targetMethod.isProtected() && !targetMethod.isNative() || !targetMethod.getInternalName().equals("java/lang/Object"))
					&& (!targetMethod.isProtected() || !targetMethod.isStatic() || this.getPackageName().equals(targetMethod.getPackageName()) || targetMethod.getClassNode().isJava9OrNewer())
					&& (targetMethod.getClassNode().isPublic() || targetMethod.getClassNode().getPackageName().equals(this.getPackageName()) || targetMethod.getClassNode().isJava6OrNewer())
					&& !MethodSignature.isHandleMethod(targetMethod.getInternalName(), targetMethod.getNameDisplay(), classHierarchy)
					&& (
						!targetMethod.isMethod()
							|| !targetMethod.getClassNode().isInterface()
							|| !targetMethod.isAbstract()
							|| !classHierarchy.isObjectMethodSignature(((AbstractMethodNode)targetMethod).getMethodSignature())
					)) {
					MemberContext memberContext = this.createMemberContext(targetMethod, methodCallWriter, referencesBuilder, classProvider);
					referenceGraphMap.addReference(memberContext, this, (WideConstantPoolInsnNode)instruction);
				}
			} else if (instruction.isFieldOpcode()) {
				FieldTypeWriter fieldTypeWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
				MemberNode targetField = fieldTypeWriter.getTargetMember();
				if (targetField != null
					&& (!instruction.isPutField() || !targetField.isFinal())
					&& (!targetField.isProtected() || !targetField.isStatic() || this.getPackageName().equals(targetField.getPackageName()) || targetField.getClassNode().isJava9OrNewer())) {
					referenceGraphMap.addReference(this.createMemberContext(targetField, fieldTypeWriter, referencesBuilder, classProvider), this, (WideConstantPoolInsnNode)instruction);
				}
			}
		}
	}

	public HashSet<LabelNode> collectLabels(int[] instructionIndexes) {
		HashSet<LabelNode> labels = GlobalUtils.newHashSet();

		for (int i = 0; i < instructionIndexes.length; i++) {
			AbstractInsnNode instruction = this.instructions.get(instructionIndexes[i]);
			if (instruction instanceof LabelNode) {
				labels.add((LabelNode)instruction);
			}
		}

		return labels;
	}

	private boolean resolveReflectionFields(
		ReflectionMethodPattern reflectionPattern,
		ReflectionMethodCallTracker callTracker,
		Set<Reference> classReferences,
		Set<Reference> fieldNameReferences,
		Set<Reference> fieldTypeReferences,
		Set<StringNode> processedStringNodes,
		DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferences,
		DualKeyMap<AbstractFieldNode, Reference, Reference> stringFieldNameReferences,
		Set<StructureNode> resolvedNodes,
		AtomicReference<String> errorMessage,
		ClassRepository classRepository,
		AbstractWorkspace workspace
	) {
		boolean fullyResolved = true;

		for (Reference classReference : classReferences) {
			String binaryClassName = classReference.getTypeName();
			if (binaryClassName != null && binaryClassName.length() > 0) {
				String internalClassName = GlobalUtils.binaryToInternalName(binaryClassName);
				AbstractClassNode classNode = findClassFromRepository(internalClassName, classRepository);
				if (classNode != null) {
					for (Reference fieldNameReference : fieldNameReferences) {
						String fieldName = fieldNameReference.getTypeName();
						if (fieldName != null && fieldName.length() > 0) {
							AbstractFieldNode[] matchedFields;
							if (fieldTypeReferences != null && fieldTypeReferences.size() > 0) {
								ArrayList<AbstractFieldNode> fieldCandidates = new ArrayList<>(fieldTypeReferences.size());
								Iterator<Reference> fieldTypeReferenceIterator = fieldTypeReferences.iterator();

								while (fieldTypeReferenceIterator.hasNext()) {
									String fieldDescriptor = fieldTypeReferenceIterator.next().getValue();
									AbstractFieldNode resolvedField;
									if (classNode.isWorkspaceNode()) {
										resolvedField = workspace.getFieldByNameAndDescriptor(internalClassName, fieldName, MethodSignature.fromInternalName(fieldDescriptor));
									} else {
										resolvedField = classNode.findField(fieldName, MethodSignature.fromInternalName(fieldDescriptor));
									}

									if (resolvedField != null) {
										fieldCandidates.add(resolvedField);
									}
								}

								matchedFields = fieldCandidates.toArray(new AbstractFieldNode[fieldCandidates.size()]);
							} else if (classNode.isWorkspaceNode()) {
								matchedFields = workspace.getFieldsByClassAndName(internalClassName, fieldName);
							} else {
								matchedFields = classNode.findFieldsByName(fieldName);
							}

							if (matchedFields != null && matchedFields.length > 0) {
								if (matchedFields.length == 1) {
									AbstractFieldNode matchedField = matchedFields[0];
									stringFieldNameReferences.put(matchedField, fieldNameReference, fieldNameReference);
									resolvedNodes.add(matchedField);
									if (fieldNameReference instanceof StringReference) {
										processedStringNodes.add(((StringReference)fieldNameReference).getStringNode());
									}
								} else {
									errorMessage.set("Field inadequately resolved (C) - more than one possible match");
									fullyResolved = false;
								}
							} else {
								fullyResolved = false;
							}
						} else {
							fullyResolved = false;
						}
					}
				} else if (classReference instanceof InstructionTypeReference) {
					fullyResolved = false;
				}
			} else {
				fullyResolved = false;
			}
		}

		return fullyResolved;
	}

	private void insertInstructionBlock(
		List<AbstractInsnNode> indexInstructions, List<AbstractInsnNode> keyInstructions, AbstractInsnNode invokeInstruction, List<InstructionBlock> instructionBlocks, MethodAnalyzer methodAnalyzer, int instructionIndex, int additionalStackRequired
	) {
		int firstFlowLabelIndex = this.findFirstControlFlowLabel();
		if (methodAnalyzer != null && !Options.dontInsertFrameBlocks) {
			AnalyzingNode instructionNode = methodAnalyzer.getNodeForInstructionIndex(instructionIndex);
			FrameNode[] frames = methodAnalyzer.getFrames();
			OrderedSet<Integer> insertionPoints = new OrderedSet<>();
			if (instructionIndex > 0 && frames[instructionIndex - 1].getOperandStack().length == 0 && instructionNode.getStartIndex() > firstFlowLabelIndex) {
				int startIndex = instructionNode.getStartIndex();
				int lastBlockOffset = -1;
				if (!instructionBlocks.isEmpty()) {
					lastBlockOffset = instructionBlocks.get(instructionBlocks.size() - 1).getOffset();
				}

				int scanStartIndex = Math.max(startIndex, lastBlockOffset + 1);

				for (int offset = instructionIndex - 1; offset >= scanStartIndex; offset--) {
					AbstractInsnNode instruction = this.instructions.get(offset);
					LabelNode label = null;
					if (instruction.isLabel()) {
						label = (LabelNode)instruction;
						if (label.hasFlag(1) || label.hasFlag(256)) {
							break;
						}
					}

					boolean labelAtOffsetZero = label != null && label.getOffset() == 0;
					if (frames[offset].getOperandStack().length == 0 && !instruction.isJump() && instruction.getOpcode() != ATHROW && (label == null || label.getOffset() <= 0)) {
						insertionPoints.add(labelAtOffsetZero ? INTEGER_CACHE.valueOf(0) : INTEGER_CACHE.valueOf(offset));
					}
				}
			}

			switch (insertionPoints.size()) {
				case 0:
					ArrayList<AbstractInsnNode> mergedInstructions = new ArrayList<>();
					if (indexInstructions != null) {
						mergedInstructions.addAll(indexInstructions);
					}

					if (keyInstructions != null) {
						mergedInstructions.addAll(keyInstructions);
					}

					mergedInstructions.add(invokeInstruction);
					instructionBlocks.add(new InstructionBlock(mergedInstructions, instructionIndex - 1, additionalStackRequired, 2));
					break;
				case 1:
					ArrayList<AbstractInsnNode> indexAndKeyInstructions = new ArrayList<>();
					if (indexInstructions != null) {
						indexAndKeyInstructions.addAll(indexInstructions);
					}

					if (keyInstructions != null) {
						indexAndKeyInstructions.addAll(keyInstructions);
					}

					instructionBlocks.add(new InstructionBlock(indexAndKeyInstructions, insertionPoints.get(0), 0, 2));
					instructionBlocks.add(new InstructionBlock(invokeInstruction, instructionIndex - 1, additionalStackRequired, 0));
					break;
				default:
					int lastInsertionPointIndex = insertionPoints.size() - 1;
					if (indexInstructions != null && !indexInstructions.isEmpty()) {
						instructionBlocks.add(new InstructionBlock(indexInstructions, insertionPoints.get(lastInsertionPointIndex--), 0, 2));
					}

					if (keyInstructions != null && !keyInstructions.isEmpty()) {
						instructionBlocks.add(new InstructionBlock(keyInstructions, insertionPoints.get(lastInsertionPointIndex), 0, 2));
					}

					instructionBlocks.add(new InstructionBlock(invokeInstruction, instructionIndex - 1, additionalStackRequired, 0));
			}
		} else {
			ArrayList<AbstractInsnNode> fallbackInstructions = new ArrayList<>();
			if (indexInstructions != null) {
				fallbackInstructions.addAll(indexInstructions);
			}

			if (keyInstructions != null) {
				fallbackInstructions.addAll(keyInstructions);
			}

			fallbackInstructions.add(invokeInstruction);
			instructionBlocks.add(new InstructionBlock(fallbackInstructions, instructionIndex - 1, additionalStackRequired, 2));
		}
	}

	public int getMaxStack() {
		return ((CodeAttribute)this.getParent()).getMaxStack();
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			switch (instruction.getOpcode()) {
				case LDC:
					TypeWriter ldcTypeWriter = ((LdcNode)instruction).getTypeWriter();
					visitor.visit(ldcTypeWriter, instruction, this);
					break;
				case LDC_W:
					TypeWriter ldcWTypeWriter = ((WideConstantPoolInsnNode)instruction).getTypeWriter();
					visitor.visit(ldcWTypeWriter, instruction, this);
					break;
				case LDC2_W:
					TypeWriter ldc2WTypeWriter = ((WideConstantPoolInsnNode)instruction).getTypeWriter();
					visitor.visit(ldc2WTypeWriter, instruction, this);
					break;
				case GETSTATIC:
				case PUTSTATIC:
				case GETFIELD:
				case PUTFIELD:
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKESTATIC:
				case INVOKEINTERFACE:
					TypeWriter memberTypeWriter = ((WideConstantPoolInsnNode)instruction).getTypeWriter();
					visitor.visit(memberTypeWriter, instruction, this);
					break;
				case INVOKEDYNAMIC:
					TypeWriter indyTypeWriter = ((WideConstantPoolInsnNode)instruction).getTypeWriter();
					visitor.visit(indyTypeWriter, instruction, this);
					break;
				case NEW:
				case ANEWARRAY:
				case CHECKCAST:
				case INSTANCEOF:
				case MULTIANEWARRAY:
					TypeWriter classRefTypeWriter = ((WideConstantPoolInsnNode)instruction).getTypeWriter();
					visitor.visit(classRefTypeWriter, instruction, this);
			}
		}
	}

	private String formatReflectionParameters(ReflectionMethodCallTracker callTracker, int referenceIndex, boolean useDescriptors, List<String> parameterTypes) {
		Set<Reference> parameterReferences = callTracker.getParameterReferences(referenceIndex);
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		Iterator<Reference> referenceIterator = parameterReferences.iterator();

		while (referenceIterator.hasNext()) {
			Reference reference = referenceIterator.next();
			builder.append(useDescriptors ? (reference instanceof ArrayReference ? ((ArrayReference)reference).toDisplayString(true) : reference.getValue()) : reference.getDisplayString());
			if (parameterTypes != null) {
				if (reference.isResolved()) {
					parameterTypes.add(reference.getTypeName());
				} else {
					parameterTypes.add(null);
				}
			}

			if (referenceIterator.hasNext()) {
				builder.append(", ");
			}
		}

		builder.append("]");
		return builder.toString();
	}

	public void collectLongNodes(Set<LongNode> longNodes) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			if (instruction.isLong()) {
				TypeWriter typeWriter = ((WritableType)instruction).getTypeWriter();
				if (typeWriter instanceof LongNode) {
					longNodes.add((LongNode)typeWriter);
				}
			}
		}
	}

	public ExceptionEntryNode[] copyExceptionTableEntries() {
		ExceptionEntryNode[] sourceEntries = ((CodeAttribute)this.getParent()).getExceptionEntries();
		ExceptionEntryNode[] copiedEntries = new ExceptionEntryNode[sourceEntries.length];
		System.arraycopy(sourceEntries, 0, copiedEntries, 0, sourceEntries.length);
		return copiedEntries;
	}

	private OrderedSet<Integer> findReachableInsertionPoints(MethodAnalyzer methodAnalyzer, int instructionIndex, int previousBlockOffset, Integer excludedSlotIndex) {
		AnalyzingNode instructionNode = methodAnalyzer.getNodeForInstructionIndex(instructionIndex);
		FrameNode[] frames = methodAnalyzer.getFrames();
		OrderedSet<Integer> insertionPoints = new OrderedSet<>();
		int firstFlowLabelIndex = this.findFirstControlFlowLabel();
		if (instructionIndex > 0 && frames[instructionIndex].getOperandStack().length == 0 && instructionNode.getStartIndex() > firstFlowLabelIndex) {
			int startIndex = instructionNode.getStartIndex();
			int scanStartIndex = Math.max(startIndex, previousBlockOffset + 1);

			for (int offset = instructionIndex - 1; offset >= scanStartIndex; offset--) {
				AbstractInsnNode instruction = this.instructions.get(offset);
				FrameNode frame = frames[offset];
				LabelNode label = null;
				if (instruction.isLabel()) {
					label = (LabelNode)instruction;
					if (label.hasFlag(1) || label.hasFlag(256)) {
						break;
					}
				}

				if (excludedSlotIndex != null) {
					StackFrameType excludedSlotType = frame.getLocalVariables()[excludedSlotIndex];
					if (excludedSlotType.isUnknown()) {
						break;
					}
				}

				boolean labelAtOffsetZero = label != null && label.getOffset() == 0;
				if (frame.getOperandStack().length == 0 && !instruction.isJump() && instruction.getOpcode() != ATHROW && (label == null || label.getOffset() <= 0)) {
					insertionPoints.add(labelAtOffsetZero ? INTEGER_CACHE.valueOf(0) : INTEGER_CACHE.valueOf(offset));
				}
			}
		}

		return insertionPoints;
	}

	private OrderedSet<Integer> findSafeInsertionPoints(MethodAnalyzer methodAnalyzer, int maxIndex, AtomicInt anchorIndex) {
		int firstFlowLabelIndex = this.findFirstControlFlowLabel();
		AnalyzingNode anchorNode;
		int scanStartIndex;
		if (firstFlowLabelIndex == -1) {
			anchorNode = methodAnalyzer.getNodeByIndex(0);
			scanStartIndex = anchorNode.getStartIndex();
		} else {
			anchorNode = methodAnalyzer.getNodeForInstructionIndex(firstFlowLabelIndex);
			scanStartIndex = firstFlowLabelIndex;
		}

		OrderedSet<Integer> insertionPoints = new OrderedSet<>();
		FrameNode[] frames = methodAnalyzer.getFrames();
		int anchorEndIndex = anchorNode.getEndIndex();
		AbstractInsnNode anchorEndInstruction = this.instructions.get(anchorEndIndex);
		int scanEndIndex = Math.min(anchorEndInstruction.canFallThrough() && !anchorEndInstruction.isJump() ? anchorEndIndex + 1 : anchorEndIndex, maxIndex);
		int chosenAnchorIndex = anchorEndIndex;

		for (int labelScanIndex = scanStartIndex; labelScanIndex <= scanEndIndex; labelScanIndex++) {
			AbstractInsnNode scannedInstruction = this.instructions.get(labelScanIndex);
			if (scannedInstruction.isLabel()) {
				LabelNode boundaryLabel = (LabelNode)scannedInstruction;
				if (boundaryLabel.hasFlag(1) && labelScanIndex < scanEndIndex) {
					scanEndIndex = labelScanIndex;
					break;
				}

				if (boundaryLabel.hasFlag(256)) {
					chosenAnchorIndex = labelScanIndex;
					scanEndIndex = labelScanIndex;
					break;
				}
			}
		}

		anchorIndex.set(chosenAnchorIndex);
		scanEndIndex = scanEndIndex == 0 ? scanEndIndex : scanEndIndex - 1;
		boolean allLabels = scanStartIndex == 0;
		int previousInsertionIndex = -1;

		for (int insertionScanIndex = scanStartIndex; insertionScanIndex <= scanEndIndex; insertionScanIndex++) {
			AbstractInsnNode instruction = this.instructions.get(insertionScanIndex);
			allLabels = allLabels && instruction.isLabel();
			if (frames[insertionScanIndex].getOperandStack().length == 0 && !instruction.isJump() && instruction.getOpcode() != ATHROW) {
				Integer insertionPoint = allLabels ? IntegerCache.NEGATIVE_ONE : INTEGER_CACHE.valueOf(insertionScanIndex);
				if (insertionPoint == IntegerCache.NEGATIVE_ONE || previousInsertionIndex == -1 || insertionScanIndex > previousInsertionIndex + 1) {
					insertionPoints.add(insertionPoint);
					previousInsertionIndex = insertionPoint;
				}
			}
		}

		return insertionPoints;
	}

	public void applyPatchesWithoutStackCheck(List<InstructionBlock> instructionBlocks, String reason) {
		this.applyInstructionBlockPatches(instructionBlocks, reason, false, true);
	}

	private void replaceWithReflection(
		AbstractInsnNode memberAccessInsn,
		int instructionIndex,
		MemberContext memberContext,
		AbstractMethodCallWriter methodCallWriter,
		LongNode longConstant,
		AbstractInsnNode keyLoadInsn,
		List<InstructionBlock> instructionBlocks,
		AbstractMethodCallWriter methodHelperWriter,
		Map<MemberContext, AbstractMethodCallWriter> memberHelperWriters,
		MethodAnalyzer methodAnalyzer,
		Map<AbstractInsnNode, Boolean> invokeDynamicFlags,
		ObfuscateReferencesBuilder referencesBuilder,
		ConstantPoolManager constantPoolManager,
		List<ExceptionEntryNode> exceptionEntries,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		AbstractMethodCallWriter memberHelperWriter = memberHelperWriters.get(memberContext);
		if (memberHelperWriter != null) {
			ArrayList<AbstractInsnNode> replacementInstructions = new ArrayList<>();
			replacementInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, memberHelperWriter));
			replacementInstructions.add(new WideConstantPoolInsnNode(LDC2_W, longConstant));
			if (keyLoadInsn != null) {
				replacementInstructions.add(keyLoadInsn);
			}

			replacementInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, methodHelperWriter));
			switch (memberAccessInsn.getOpcode()) {
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKEINTERFACE:
					replacementInstructions.add(InsnNode.of(DUP_X2));
					replacementInstructions.add(InsnNode.of(POP));
					break;
				case INVOKESTATIC:
					replacementInstructions.add(InsnNode.of(SWAP));
					replacementInstructions.add(InsnNode.of(ACONST_NULL));
					replacementInstructions.add(InsnNode.of(SWAP));
			}

			MethodCallWriter invokeCallWriter = referencesBuilder.genMethodInvokeCall(constantPoolManager, typeWriters, workspace, classProvider);
			ClassInfoWriter exceptionClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/reflect/InvocationTargetException", typeWriters);
			LabelNode tryStartLabel = new LabelNode(true, 128);
			LabelNode tryEndLabel = new LabelNode(true, 128);
			LabelNode handlerLabel = new LabelNode(true, 128);
			ExceptionEntryNode exceptionEntry = new ExceptionEntryNode((CodeAttribute)this.getParent(), exceptionClassInfo, tryStartLabel, tryEndLabel, handlerLabel);
			exceptionEntries.add(exceptionEntry);
			replacementInstructions.add(tryStartLabel);
			replacementInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, invokeCallWriter));
			if (this.isReturnValueUsed(instructionIndex, methodCallWriter, invokeDynamicFlags, methodAnalyzer)) {
				List<AbstractInsnNode> returnValueConversionInsns = referencesBuilder.genReturnValueConversion(methodCallWriter, constantPoolManager, typeWriters, workspace, classProvider);
				if (returnValueConversionInsns != null) {
					replacementInstructions.addAll(returnValueConversionInsns);
				}
			}

			this.generateHandlerBlock(replacementInstructions, tryEndLabel, handlerLabel, referencesBuilder, typeWriters, constantPoolManager, workspace, classProvider);
			int parameterStackSize = memberHelperWriter.getParameterStackSize();
			InstructionBlock instructionBlock = null;
			switch (memberAccessInsn.getOpcode()) {
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKEINTERFACE:
					int instanceBlockOffset = instructionIndex - 1;
					int instanceAdditionalStackRequired = parameterStackSize >= 4 ? 0 : 4 - parameterStackSize;
					instructionBlock = new InstructionBlock(replacementInstructions, instanceBlockOffset, 1, instanceAdditionalStackRequired);
					break;
				case INVOKESTATIC:
					int staticBlockOffset = instructionIndex - 1;
					int staticAdditionalStackRequired = parameterStackSize >= 3 ? 0 : 3 - parameterStackSize;
					instructionBlock = new InstructionBlock(replacementInstructions, staticBlockOffset, 1, staticAdditionalStackRequired);
			}

			instructionBlocks.add(instructionBlock);
		}
	}

	public MethodAnalyzer getOrCreateMethodAnalyzer(MethodAnalyzer methodAnalyzer, TypeResolver typeResolver, ClassHierarchy classHierarchy) {
		if (methodAnalyzer == null) {
			methodAnalyzer = this.getOrCreateMethodAnalyzerWithFlag(methodAnalyzer, typeResolver, classHierarchy, false);
		}

		return methodAnalyzer;
	}

	public boolean isConstructor() {
		return ((CodeAttribute)this.getParent()).isConstructor();
	}

	public void collectExcludedIntegerConstants(Set<IntegerNode> excludedIntegers, IntegerEncryptionExcludes encryptionExcludes) {
		ClassNode classNode = (ClassNode)this.getClassNode();
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			int opcode = instruction.getOpcode();
			if (opcode == PUTSTATIC || opcode == PUTFIELD) {
				AbstractFieldNode targetField = (AbstractFieldNode)((FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter()).getTargetMember();
				if (targetField != null && targetField.isWorkspaceNode() && targetField.getParent() == classNode && encryptionExcludes.isFieldExcluded((FieldNode)targetField)) {
					AbstractInsnNode previousInstruction = this.instructions.get(i - 1);
					int previousOpcode = previousInstruction.getOpcode();
					if (previousOpcode == LDC || previousOpcode == LDC_W) {
						TypeWriter constantWriter;
						if (previousOpcode == LDC) {
							constantWriter = ((LdcNode)previousInstruction).getTypeWriter();
						} else {
							constantWriter = ((WideConstantPoolInsnNode)previousInstruction).getTypeWriter();
						}

						if (constantWriter instanceof IntegerNode) {
							excludedIntegers.add((IntegerNode)constantWriter);
						}
					}
				}
			}
		}
	}

	private boolean isValueUsedAfterCall(AbstractInsnNode instruction, int instructionIndex, ReferenceGraph<LocalVarOperationType, Integer> referenceGraph, AtomicBool referenceGraphComplete) {
		if (instruction.getOpcode() == POP || instruction.getOpcode() == POP2) {
			return false;
		} else if (instruction.isVarStore()) {
			VarInsnNode varStoreInstruction = (VarInsnNode)instruction;
			LocalVarOperationType operationType = varStoreInstruction.getLoadOperationType();
			if (operationType != null) {
				int variableIndex = varStoreInstruction.getVariableIndex();
				if (referenceGraph.isReferencedBy(operationType, INTEGER_CACHE.valueOf(variableIndex))) {
					return true;
				} else if (referenceGraphComplete.get()) {
					return false;
				} else {
					for (AbstractInsnNode candidateInstruction : this.instructions) {
						if (candidateInstruction.isVarLoad()) {
							VarInsnNode loadInstruction = (VarInsnNode)candidateInstruction;
							referenceGraph.addReference(loadInstruction.getOperationType(), INTEGER_CACHE.valueOf(loadInstruction.getVariableIndex()));
							if (loadInstruction.getOperationType() == operationType && loadInstruction.getVariableIndex() == variableIndex) {
								return true;
							}
						}
					}

					referenceGraphComplete.set(true);
					return false;
				}
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

	public void collectStringNodes(Set<StringNode> stringNodes) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			if (instruction.isString()) {
				TypeWriter typeWriter = ((WritableType)instruction).getTypeWriter();
				if (typeWriter instanceof StringNode) {
					stringNodes.add((StringNode)typeWriter);
				}
			}
		}
	}

	public void collectBootstrapMethods(Set<BootstrapMethodEntry> bootstrapMethods) {
		for (AbstractInsnNode instruction : this.instructions) {
			if (instruction.getOpcode() == INVOKEDYNAMIC) {
				InvokeDynamicCallWriter indyCallWriter = ((InvokeDynamicNode)instruction).getIndyCallWriter();
				bootstrapMethods.add(indyCallWriter.getBootstrapMethodEntry());
			}
		}
	}

	public void collectIntegerNodes(Set<IntegerNode> integerNodes) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			if (instruction.isIntegerConstant2()) {
				TypeWriter typeWriter = ((WritableType)instruction).getTypeWriter();
				if (typeWriter instanceof IntegerNode) {
					integerNodes.add((IntegerNode)typeWriter);
				}
			}
		}
	}

	private boolean findConstructorsInHierarchy(String classInternalName, Set<MethodParameterKey> methodParameterKeys, Set<StructureNode> resolvedMethods, AbstractWorkspace workspace) {
		boolean constructorFound = false;
		ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(classInternalName);
		HashMap<ClassHierarchyNode, ClassHierarchyNode> descendants = GlobalUtils.newHashMap();
		hierarchyNode.collectAllDescendants(descendants);
		Iterator<ClassHierarchyNode> descendantIterator = descendants.keySet().iterator();

		while (descendantIterator.hasNext()) {
			ClassNode classNode = descendantIterator.next().getClassNodeIfWorkspace();
			if (classNode != null) {
				for (MethodParameterKey parameterKey : methodParameterKeys) {
					MethodNode methodNode = (MethodNode)AbstractMethodNode.resolveByCovariantReturn(workspace.getMethodNodesByParameterKey(classNode, parameterKey), workspace);
					if (methodNode != null) {
						resolvedMethods.add(methodNode);
						constructorFound = true;
					}
				}
			}
		}

		return constructorFound;
	}

	private boolean isReturnValueUsed(int instructionIndex, MemberTypeWriter memberTypeWriter, Map<AbstractInsnNode, Boolean> invokeDynamicFlags, MethodAnalyzer methodAnalyzer) {
		if (Options.treatReturnValuesAsUsed) {
			return true;
		} else {
			int opcode = this.instructions.get(instructionIndex).getOpcode();
			if (opcode != INVOKEVIRTUAL && opcode != INVOKESPECIAL && opcode != INVOKESTATIC && opcode != INVOKEINTERFACE) {
				String memberDescriptor = ((FieldTypeWriter)memberTypeWriter).getDescriptor();
				if (!memberDescriptor.startsWith("[") && (!memberDescriptor.startsWith("L") || !memberDescriptor.endsWith(";"))) {
					return false;
				}
			} else if (TypeWriter.getReturnDescriptor(((AbstractMethodCallWriter)memberTypeWriter).getDescriptor()).equals("V")) {
				return true;
			}

			if (instructionIndex >= this.instructions.size() - 1) {
				return false;
			} else {
				FrameNode[] frames = methodAnalyzer.getFrames();
				int stackIndex = frames[instructionIndex].getStackSize() - 1;
				StackFrameType stackType = frames[instructionIndex].getOperandStack()[stackIndex];

				for (int i = instructionIndex + 1; i < this.instructions.size(); i++) {
					AbstractInsnNode instruction = this.instructions.get(i);
					FrameNode frame = frames[i];
					if (instruction.isJump()) {
						return true;
					}

					if (instruction.isPureStackOpcode()) {
						int[] stackIndexMapping = instruction.mapOldStackIndexToNew(frames[i - 1].getOperandStack(), frame.getOperandStack(), stackIndex);
						if (stackIndexMapping.length == 0) {
							return stackType.isCategory2();
						}

						if (stackIndexMapping.length != 1) {
							return true;
						}

						stackIndex = stackIndexMapping[0];
					} else {
						if (instruction.consumesStackFromIndex(stackIndex, frames[i].getStackSize())) {
							if (invokeDynamicFlags.containsKey(instruction) && !invokeDynamicFlags.get(instruction)) {
								if (stackType.isObjectReference()) {
									return false;
								}

								return true;
							}

							return instruction.isStackSlotConsumed(stackIndex, stackType, frames[i].getStackSize());
						}

						if (instruction.isLabel()) {
							if (i == this.instructions.size() - 1) {
								return false;
							}

							if (((LabelNode)instruction).hasFlag(1) || ((LabelNode)instruction).hasFlag(1024)) {
								return true;
							}
						}
					}
				}

				return true;
			}
		}
	}

	public void registerElementsToGraph(ReferenceGraph<LabelNode, IGraphElement> referenceGraph) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			if (instruction instanceof IGraphElement) {
				((IGraphElement)instruction).registerToGraph(referenceGraph);
			}
		}
	}

	public void removeDeadInstructions(int[] deadInstructionIndexes, boolean verbose, ScriptContext scriptContext) {
		int originalInstructionCount = this.instructions.size();
		int instructionIndex = 0;
		int deadIndex = 0;

		for (Iterator<AbstractInsnNode> iter = this.instructions.iterator(); iter.hasNext() && deadIndex < deadInstructionIndexes.length; instructionIndex++) {
			AbstractInsnNode instruction = iter.next();
			if (instructionIndex == deadInstructionIndexes[deadIndex]) {
				if (instruction.isLabel()) {
					LabelNode labelNode = (LabelNode)instruction;
					if (labelNode.hasNoUsage() || labelNode.isJumpDestination()) {
						iter.remove();
					} else if (labelNode.hasFlag(1)) {
						labelNode.removeFlag(ClearingFlag.CLEAR_USAGE_JUMP_DESTINATION);
					}
				} else {
					iter.remove();
				}

				deadIndex++;
			}
		}

		if (verbose) {
			scriptContext.logMessage(
				"Removed " + deadInstructionIndexes.length + " dead instructions from the method '" + this.getMethodNode().getDisplayName() + "' in class '" + this.getSourcePath() + "'"
			);
		}

		this.recalculateOffsets();
		this.setModified(true);
		if (originalInstructionCount != this.instructions.size()) {
			this.setModified(true);
		}
	}

	public void encryptStringConstants(
		List<KeyValuePair<PriorityEntry<WritableType>, StringEntry>> stringConstantEntries,
		boolean isClinit,
		Map<LabelNode, AtomicInt> labelCounters,
		int[][] exceptionRanges,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelReplacements,
		Long xorKey,
		LocalVariableOperand xorKeyOperand,
		Indexable localSlotIndex,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		InstructionListNode.StringEntryPairComparator entryComparator = new InstructionListNode.StringEntryPairComparator(this);
		Collections.sort(stringConstantEntries, entryComparator);
		int entryCount = stringConstantEntries.size();
		int fieldArraySlotIndex = -1;
		if (entryCount != 0) {
			MethodAnalyzer methodAnalyzer = null;
			Iterator<KeyValuePair<PriorityEntry<WritableType>, StringEntry>> entryIterator = stringConstantEntries.iterator();
			KeyValuePair<PriorityEntry<WritableType>, StringEntry> entryPair = entryIterator.next();
			WritableType targetInsn = entryPair.getKey().getValue();
			int remainingEntries = entryCount;
			this.setModified(true);
			int additionalStackRequired = 0;
			boolean fieldValueCached = false;
			boolean slotAllocated = false;
			ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
			int instructionCount = this.instructions.size();

			for (int insnIndex = 0; insnIndex < instructionCount; insnIndex++) {
				AbstractInsnNode insn = this.instructions.get(insnIndex);
				if (insn.isLabel()) {
					LabelNode label = (LabelNode)insn;
					if (label.hasFlag(1) || label.hasFlag(1024)) {
						fieldValueCached = false;
					}
				}

				if (insn == targetInsn) {
					remainingEntries--;
					StringEntry stringEntry = entryPair.getValue();
					additionalStackRequired = 0;
					switch (stringEntry.getLookupType()) {
						case LOCAL_ARRAY_AND_INDEX:
							ArrayList<AbstractInsnNode> localArrayInsns = new ArrayList<>();
							if (stringEntry.hasIndex()) {
								additionalStackRequired = 1;
								localArrayInsns.add(AbstractInsnNode.loadObject(stringEntry.getLocalArrayIndex(), this.localVariableTable, 1));
								this.pushIntConstant(stringEntry.getIndex(), localArrayInsns, constantPoolManager, typeWriters, entryCount);
								localArrayInsns.add(InsnNode.of(AALOAD));
							} else {
								localArrayInsns.add(AbstractInsnNode.loadObject(stringEntry.getLocalArrayIndex(), this.localVariableTable, 1));
							}

							instructionBlocks.add(new InstructionBlock(localArrayInsns, insnIndex - 1, 1, additionalStackRequired));
							break;
						case FIELD_ARRAY_AND_INDEX:
							ArrayList<AbstractInsnNode> fieldArrayInsns = new ArrayList<>();
							FieldTypeWriter arrayFieldWriter = stringEntry.getArrayFieldWriter();
							if (stringEntry.hasIndex()) {
								additionalStackRequired = 1;
								if (fieldValueCached) {
									fieldArrayInsns.add(AbstractInsnNode.loadObject(fieldArraySlotIndex, this.localVariableTable, 1));
								} else {
									fieldArrayInsns.add(new WideConstantPoolInsnNode(GETSTATIC, arrayFieldWriter));
									if (remainingEntries > 0 && (exceptionRanges.length == 0 || !this.isWithinExceptionRange(insnIndex, exceptionRanges))) {
										if (!slotAllocated) {
											slotAllocated = true;
											fieldArraySlotIndex = localSlotIndex.get();
											localSlotIndex.increaseAndGet();
										}

										fieldArrayInsns.add(AbstractInsnNode.storeObject(fieldArraySlotIndex, this.localVariableTable, 1));
										fieldArrayInsns.add(AbstractInsnNode.loadObject(fieldArraySlotIndex, this.localVariableTable, 1));
										fieldValueCached = true;
									}
								}

								AbstractInsnNode.encryptIntConstant(stringEntry.getIndex(), fieldArrayInsns, constantPoolManager, typeWriters);
								fieldArrayInsns.add(InsnNode.of(AALOAD));
							} else {
								fieldArrayInsns.add(new WideConstantPoolInsnNode(GETSTATIC, arrayFieldWriter));
							}

							instructionBlocks.add(new InstructionBlock(fieldArrayInsns, insnIndex - 1, 1, additionalStackRequired));
							break;
						case SEPARATE_METHODS:
							MethodCallWriter encoderMethod = stringEntry.getEncoderMethod();
							MethodCallWriter decoderMethod = stringEntry.getDecoderMethod();
							ArrayList<AbstractInsnNode> separateMethodInsns = new ArrayList<>();
							separateMethodInsns.add(new WideConstantPoolInsnNode(INVOKESTATIC, encoderMethod));
							separateMethodInsns.add(new WideConstantPoolInsnNode(INVOKESTATIC, decoderMethod));
							instructionBlocks.add(new InstructionBlock(separateMethodInsns, insnIndex - 1, 0, 0));
							break;
						case LABEL_JUMP:
							LabelNode jumpTargetLabel = stringEntry.getJumpTargetLabel();
							ArrayList<AbstractInsnNode> labelJumpInsns = new ArrayList<>();
							if (stringEntry.useJsrJump()) {
								labelJumpInsns.add(new JSRNode(jumpTargetLabel));
								additionalStackRequired = 1;
							} else {
								int labelCounter = labelCounters.get(jumpTargetLabel).increaseAndGet();
								AbstractInsnNode.encryptIntConstant(labelCounter, labelJumpInsns, constantPoolManager, typeWriters);
								labelJumpInsns.add(new GotoNode(jumpTargetLabel));
								LabelNode replacementLabel = new LabelNode(true, 1);
								labelJumpInsns.add(replacementLabel);
								labelReplacements.put(jumpTargetLabel, new PriorityEntry<>(labelCounter, replacementLabel));
							}

							instructionBlocks.add(new InstructionBlock(labelJumpInsns, insnIndex - 1, 0, additionalStackRequired));
							break;
						case LOOKUP_METHOD_AND_INDEX:
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							AbstractMethodCallWriter lookupMethodWriter = stringEntry.getMethodCallWriter();
							ArrayList<AbstractInsnNode> indexOperandInsns = new ArrayList<>();
							ArrayList<AbstractInsnNode> lookupKeyOperandInsns = new ArrayList<>();
							int entryIndex = stringEntry.getIndex();
							int lookupKey = stringEntry.getLookupKey();
							if (xorKey != null && xorKeyOperand != null) {
								int encryptedIndex = entryIndex ^ xorKey.intValue();
								this.pushIntConstant(encryptedIndex, indexOperandInsns, constantPoolManager, typeWriters, entryCount);
								int encryptedLookupKey = lookupKey ^ xorKey.intValue();
								this.pushIntConstant(encryptedLookupKey, lookupKeyOperandInsns, constantPoolManager, typeWriters, entryCount);
								lookupKeyOperandInsns.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 1));
								lookupKeyOperandInsns.add(InsnNode.of(L2I));
							} else {
								this.pushIntConstant(entryIndex, indexOperandInsns, constantPoolManager, typeWriters, entryCount);
								this.pushIntConstant(lookupKey, lookupKeyOperandInsns, constantPoolManager, typeWriters, entryCount);
							}

							WideConstantPoolInsnNode lookupInvokeInsn = new WideConstantPoolInsnNode(INVOKESTATIC, lookupMethodWriter);
							this.insertInstructionBlock(indexOperandInsns, lookupKeyOperandInsns, lookupInvokeInsn, instructionBlocks, methodAnalyzer, insnIndex, 1);
							break;
						case LOOKUP_METHOD_AND_INDEX_DES:
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							AbstractMethodCallWriter lookupDesMethodWriter = stringEntry.getMethodCallWriter();
							ArrayList<AbstractInsnNode> desIndexOperandInsns = new ArrayList<>();
							ArrayList<AbstractInsnNode> desKeyOperandInsns = new ArrayList<>();
							int desEntryIndex = stringEntry.getIndex();
							long entryEncryptionKey = stringEntry.getEncryptionKey();
							if (xorKey != null && xorKeyOperand != null) {
								int encryptedDesIndex = desEntryIndex ^ (int)(entryEncryptionKey & 32767L);
								this.pushIntConstant(encryptedDesIndex, desIndexOperandInsns, constantPoolManager, typeWriters, entryCount);
								AbstractInsnNode.appendLongConst(entryEncryptionKey ^ xorKey, desKeyOperandInsns, constantPoolManager, typeWriters);
								desKeyOperandInsns.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 1));
								desKeyOperandInsns.add(InsnNode.of(LXOR));
							} else {
								AbstractInsnNode.encryptIntConstant(desEntryIndex ^ (int)entryEncryptionKey, desIndexOperandInsns, constantPoolManager, typeWriters);
								AbstractInsnNode.appendLongConst(entryEncryptionKey, desKeyOperandInsns, constantPoolManager, typeWriters);
							}

							WideConstantPoolInsnNode lookupDesInvokeInsn = new WideConstantPoolInsnNode(INVOKESTATIC, lookupDesMethodWriter);
							this.insertInstructionBlock(desIndexOperandInsns, desKeyOperandInsns, lookupDesInvokeInsn, instructionBlocks, methodAnalyzer, insnIndex, 1);
							break;
						case INDY_ENTRY_AND_INDEX:
							InvokeDynamicCallWriter indyCallWriter = stringEntry.getIndyCallWriter();
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							ArrayList<AbstractInsnNode> indyIndexOperandInsns = new ArrayList<>();
							ArrayList<AbstractInsnNode> indyKeyOperandInsns = new ArrayList<>();
							int indyEntryIndex = stringEntry.getIndex();
							long indyEncryptionKey = stringEntry.getEncryptionKey();
							if (xorKey != null && xorKeyOperand != null) {
								int encryptedIndyIndex = indyEntryIndex ^ (int)(indyEncryptionKey & 32767L);
								this.pushIntConstant(encryptedIndyIndex, indyIndexOperandInsns, constantPoolManager, typeWriters, entryCount);
								long indyKeyXored = indyEncryptionKey ^ xorKey;
								AbstractInsnNode.appendLongConst(indyKeyXored, indyKeyOperandInsns, constantPoolManager, typeWriters);
								indyKeyOperandInsns.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 1));
								indyKeyOperandInsns.add(InsnNode.of(LXOR));
							} else {
								int indyIndexXored = indyEntryIndex ^ (int)indyEncryptionKey;
								this.pushIntConstant(indyIndexXored, indyIndexOperandInsns, constantPoolManager, typeWriters, entryCount);
								AbstractInsnNode.appendLongConst(indyEncryptionKey, indyKeyOperandInsns, constantPoolManager, typeWriters);
							}

							InvokeDynamicNode indyInsn = new InvokeDynamicNode(indyCallWriter);
							this.insertInstructionBlock(indyIndexOperandInsns, indyKeyOperandInsns, indyInsn, instructionBlocks, methodAnalyzer, insnIndex, 1);
					}

					if (!entryIterator.hasNext()) {
						break;
					}

					entryPair = entryIterator.next();
					targetInsn = entryPair.getKey().getValue();
				}
			}

			this.applyInstructionPatches(instructionBlocks, "String Encryption", isClinit);
			if (!isClinit) {
				CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
				int originalMaxStack = codeAttribute.getMaxStack();
				codeAttribute.setMaxStack(originalMaxStack + additionalStackRequired);
			}
		}
	}

	private void replaceCallsWithReflectionArray(
		List<InstructionBlock> instructionBlocks, List<AbstractMethodNode> reflectedMethods, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, AbstractWorkspace workspace, ClassProvider classProvider
	) {
		int instructionCount = this.instructions.size();
		ClassInfoWriter objectClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Object", typeWriters);

		for (int insnIndex = 0; insnIndex < instructionCount; insnIndex++) {
			AbstractInsnNode insn = this.instructions.get(insnIndex);
			if (insn.isInvoke()) {
				AbstractMethodNode targetMethod = (AbstractMethodNode)((AbstractMethodCallWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getTargetMember();
				if (targetMethod != null && reflectedMethods.contains(targetMethod)) {
					if (insn.getOpcode() == INVOKEINTERFACE) {
						((InvokeInterfaceNode)insn).setParameterStackSize(2);
					}

					List<String> parameterDescriptors = targetMethod.getParameterDescriptors();
					int parameterCount = parameterDescriptors.size();
					ArrayList<AbstractInsnNode> paramArrayInsns = new ArrayList<>();
					paramArrayInsns.add(AbstractInsnNode.ldcInt(parameterCount, constantPoolManager, typeWriters));
					paramArrayInsns.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClassInfo));

					for (int paramIndex = parameterCount - 1; paramIndex >= 0; paramIndex--) {
						String parameterDescriptor = parameterDescriptors.get(paramIndex);
						if (parameterDescriptor.length() == 1) {
							switch (parameterDescriptor.charAt(0)) {
								case 'B':
								case 'C':
								case 'I':
								case 'S':
									paramArrayInsns.add(InsnNode.of(DUP_X1));
									paramArrayInsns.add(InsnNode.of(SWAP));
									PrimitiveBoxingUtils.boxInt(paramArrayInsns, this.getClassNode().isJava5OrNewer(), this.localVariableTable, typeWriters, constantPoolManager, workspace, classProvider, 10);
									paramArrayInsns.add(AbstractInsnNode.ldcInt(paramIndex, constantPoolManager, typeWriters));
									paramArrayInsns.add(InsnNode.of(SWAP));
									paramArrayInsns.add(InsnNode.of(AASTORE));
									break;
								case 'D':
									paramArrayInsns.add(InsnNode.of(DUP_X2));
									paramArrayInsns.add(InsnNode.of(DUP_X2));
									paramArrayInsns.add(InsnNode.of(POP));
									PrimitiveBoxingUtils.boxDouble(paramArrayInsns, this.getClassNode().isJava5OrNewer(), this.localVariableTable, typeWriters, constantPoolManager, workspace, classProvider, 10);
									paramArrayInsns.add(AbstractInsnNode.ldcInt(paramIndex, constantPoolManager, typeWriters));
									paramArrayInsns.add(InsnNode.of(SWAP));
									paramArrayInsns.add(InsnNode.of(AASTORE));
								case 'E':
								case 'G':
								case 'H':
								case 'K':
								case 'L':
								case 'M':
								case 'N':
								case 'O':
								case 'P':
								case 'Q':
								case 'R':
								case 'T':
								case 'U':
								case 'V':
								case 'W':
								case 'X':
								case 'Y':
								default:
									break;
								case 'F':
									paramArrayInsns.add(InsnNode.of(DUP_X1));
									paramArrayInsns.add(InsnNode.of(SWAP));
									PrimitiveBoxingUtils.boxFloat(paramArrayInsns, this.getClassNode().isJava5OrNewer(), this.localVariableTable, typeWriters, constantPoolManager, workspace, classProvider, 10);
									paramArrayInsns.add(AbstractInsnNode.ldcInt(paramIndex, constantPoolManager, typeWriters));
									paramArrayInsns.add(InsnNode.of(SWAP));
									paramArrayInsns.add(InsnNode.of(AASTORE));
									break;
								case 'J':
									paramArrayInsns.add(InsnNode.of(DUP_X2));
									paramArrayInsns.add(InsnNode.of(DUP_X2));
									paramArrayInsns.add(InsnNode.of(POP));
									PrimitiveBoxingUtils.boxLong(paramArrayInsns, this.getClassNode().isJava5OrNewer(), this.localVariableTable, typeWriters, constantPoolManager, workspace, classProvider, 10);
									paramArrayInsns.add(AbstractInsnNode.ldcInt(paramIndex, constantPoolManager, typeWriters));
									paramArrayInsns.add(InsnNode.of(SWAP));
									paramArrayInsns.add(InsnNode.of(AASTORE));
									break;
								case 'Z':
									paramArrayInsns.add(InsnNode.of(DUP_X1));
									paramArrayInsns.add(InsnNode.of(SWAP));
									PrimitiveBoxingUtils.boxBoolean(paramArrayInsns, this.getClassNode().isJava5OrNewer(), this.localVariableTable, typeWriters, constantPoolManager, workspace, classProvider, 10);
									paramArrayInsns.add(AbstractInsnNode.ldcInt(paramIndex, constantPoolManager, typeWriters));
									paramArrayInsns.add(InsnNode.of(SWAP));
									paramArrayInsns.add(InsnNode.of(AASTORE));
							}
						} else {
							paramArrayInsns.add(InsnNode.of(DUP_X1));
							paramArrayInsns.add(InsnNode.of(SWAP));
							paramArrayInsns.add(AbstractInsnNode.ldcInt(paramIndex, constantPoolManager, typeWriters));
							paramArrayInsns.add(InsnNode.of(SWAP));
							paramArrayInsns.add(InsnNode.of(AASTORE));
						}
					}

					instructionBlocks.add(new InstructionBlock(paramArrayInsns, insnIndex - 1, 0, 2));
				}
			}
		}
	}

	private boolean resolveReflectionMethodInLibrary(
		ReflectionMethodPattern reflectionPattern,
		Reference classReference,
		LibraryClassNode libraryClassNode,
		Set<Reference> memberNameReferences,
		Set<Reference> memberTypeReferences,
		boolean hasReturnType,
		AtomicReference<String> errorMessage,
		AbstractWorkspace workspace,
		boolean resolved
	) {
		Iterator<Reference> nameReferenceIterator = memberNameReferences.iterator();

		while (nameReferenceIterator.hasNext()) {
			String typeName = nameReferenceIterator.next().getTypeName();
			if (typeName != null && typeName.length() > 0) {
				for (Reference memberTypeReference : memberTypeReferences) {
					Set<MethodParameterKey> parameterKeys = this.createMethodParameterKeys(typeName, hasReturnType, memberTypeReference);
					if (parameterKeys != null) {
						AbstractMethodNode matchedMethod = null;

						for (MethodParameterKey parameterKey : parameterKeys) {
							AbstractMethodNode[] candidateMethods = libraryClassNode.findMethods(parameterKey);
							if (candidateMethods != null && candidateMethods.length == 1) {
								if (matchedMethod != null) {
									matchedMethod = null;
									break;
								}

								matchedMethod = candidateMethods[0];
							}
						}

						if (matchedMethod == null) {
							resolved = false;
						}
					} else {
						resolved = false;
					}
				}
			} else {
				resolved = false;
			}
		}

		return resolved;
	}

	public void buildMethodReferenceGraph(ReferenceGraph<MethodNode, MethodNode> graph, Set<MethodNode> visitedMethods) {
		int instructionCount = this.instructions.size();
		MethodNode currentMethod = (MethodNode)this.getMethodNode();

		for (int insnIndex = 0; insnIndex < instructionCount; insnIndex++) {
			AbstractInsnNode insn = this.instructions.get(insnIndex);
			if (insn.isInvoke()) {
				AbstractMethodNode targetMethod = (AbstractMethodNode)((AbstractMethodCallWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getTargetMember();
				if (targetMethod != null && targetMethod.isWorkspaceNode() && !targetMethod.isClinit()) {
					graph.addReference((MethodNode)targetMethod, currentMethod);
					visitedMethods.add((MethodNode)targetMethod);
				}
			} else if (Options.ZKM_INDY_PARAM_CHANGING && insn.getOpcode() == INVOKEDYNAMIC) {
				AbstractMethodNode lambdaTargetMethod = ((InvokeDynamicNode)insn).getIndyCallWriter().getLambdaParamChangeTarget();
				if (lambdaTargetMethod != null && lambdaTargetMethod.isWorkspaceNode()) {
					graph.addReference((MethodNode)lambdaTargetMethod, currentMethod);
					visitedMethods.add((MethodNode)lambdaTargetMethod);
				}
			}
		}
	}

	public void injectLongXorDecryption(
		ChangedMethod changedMethod,
		Long xorKey,
		MethodNode methodNode,
		AtomicReference<LocalVariableSlot> localSlotRef,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager,
		String reason,
		int variableFlag
	) {
		ArrayList<AbstractInsnNode> decryptionInsns = new ArrayList<>();
		FieldTypeWriter saltFieldRef = changedMethod.getSaltFieldRef();
		long methodSalt = changedMethod.getMethodSalt();
		long maskedXorKey = xorKey ^ methodSalt;
		decryptionInsns.add(new WideConstantPoolInsnNode(GETSTATIC, saltFieldRef));
		decryptionInsns.add(AbstractInsnNode.ldcLong(maskedXorKey, constantPoolManager, typeWriters));
		decryptionInsns.add(InsnNode.of(LXOR));
		int slotIndex = this.getMaxLocals();
		decryptionInsns.add(AbstractInsnNode.storeLong(slotIndex, this.localVariableTable, variableFlag));
		localSlotRef.set(this.localVariableTable.getVariable(slotIndex));
		this.setMaxLocals(slotIndex + 2);
		if (this.getMaxStack() < 4) {
			this.setMaxStack(4);
		}

		InstructionBlock instructionBlock = new InstructionBlock(decryptionInsns, -1, 0, 4);
		this.applySingleInstructionBlock(instructionBlock, reason);
	}

	public void applyInstructionBlockPatches(List<InstructionBlock> instructionBlocks, String reason, boolean deferOffsetUpdate, boolean useControlFlowLabel) {
		int instructionCount = this.instructions.size();
		int controlFlowLabelIndex = -1;
		if (useControlFlowLabel) {
			controlFlowLabelIndex = this.findFirstControlFlowLabel();
		}

		int blockCount = instructionBlocks.size();
		if (blockCount > 0) {
			Collections.sort(instructionBlocks);
			ArrayList<AbstractInsnNode> mergedInsns = new ArrayList<>(this.instructions.size() + blockCount * 3);
			int maxAdditionalStack = 0;
			int nextInsnIndex = 0;
			AbstractInsnNode firstInsn = instructionCount > 0 ? this.instructions.get(0) : null;
			int headInsnCount = 0;

			for (int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
				InstructionBlock block = instructionBlocks.get(blockIndex);
				int additionalStackRequired = block.getAdditionalStackRequired();
				if (additionalStackRequired > maxAdditionalStack) {
					maxAdditionalStack = additionalStackRequired;
				}

				boolean headLabelAdded = false;
				int blockOffset = block.getOffset();
				if (controlFlowLabelIndex > -1 && (blockOffset == -1 || blockOffset == 0)) {
					blockOffset = controlFlowLabelIndex;
				}

				if (blockOffset == -1) {
					if (headInsnCount == 0 && firstInsn != null && firstInsn.isLabel() && ((LabelNode)firstInsn).isFixed()) {
						LabelNode firstLabel = (LabelNode)this.instructions.get(0);
						mergedInsns.add(firstLabel);
						headInsnCount++;
						headLabelAdded = true;
					}
				} else {
					for (int insnIndex = nextInsnIndex; insnIndex <= blockOffset; insnIndex++) {
						mergedInsns.add(this.instructions.get(insnIndex));
					}
				}

				List<AbstractInsnNode> newInsns = block.getNewInstructions();

				for (int newInsnIndex = 0; newInsnIndex < newInsns.size(); newInsnIndex++) {
					AbstractInsnNode newInsn = newInsns.get(newInsnIndex);
					mergedInsns.add(newInsn);
					if (newInsn.isString()) {
						this.stringConstantCount++;
					}

					if (newInsn.isIntegerConstant2()) {
						this.intConstantCount++;
					}

					if (newInsn.isLong()) {
						this.longConstantCount++;
					}
				}

				nextInsnIndex = blockOffset + 1 + block.getReplacedInsnCount() + (headLabelAdded ? headInsnCount : 0);
			}

			for (int remainingInsnIndex = nextInsnIndex; remainingInsnIndex < instructionCount; remainingInsnIndex++) {
				mergedInsns.add(this.instructions.get(remainingInsnIndex));
			}

			this.instructions = mergedInsns;
			CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
			int newMaxStack = codeAttribute.getMaxStack() + maxAdditionalStack;
			codeAttribute.setMaxStack(newMaxStack);
			int originalByteCodeLength = this.getByteCodeLength();
			if (!deferOffsetUpdate) {
				this.recalculateOffsets();
				this.setModified(true);
				if (this.getByteCodeLength() > 65535) {
					throw new ProcessingException(
						reason
							+ " : Bytecode length greater than "
							+ '\uffff'
							+ " in "
							+ this.getParentString()
							+ " in file '"
							+ this.getRawSourcePath()
							+ "' (4). (original="
							+ originalByteCodeLength
							+ ", new="
							+ this.getByteCodeLength()
							+ ") : '"
							+ reason
							+ "'"
					);
				}
			}
		}
	}

	public boolean handleAutoReflection(
		List<MethodInvocation> methodInvocations,
		String classInternalName,
		List<TypeWriter> typeWriters,
		boolean hasRenamedNames,
		boolean hasRenamedFields,
		boolean hasRenamedMethods,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		boolean verbose,
		ScriptContext scriptContext
	) {
		HashMap<AbstractInsnNode, MethodInvocation> invocationsByInsn = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(methodInvocations.size()));

		for (MethodInvocation methodInvocation : methodInvocations) {
			invocationsByInsn.put(methodInvocation.getInvokeNode(), methodInvocation);
		}

		boolean reflectionHandled = false;
		int instructionCount = this.instructions.size();
		ClassProvider classProvider = typeResolver.getClassProvider();
		ConstantPoolManager constantPoolManager = (ConstantPoolManager)this.getClassNode().getConstantPoolWriter();
		ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();

		for (int insnIndex = 0; insnIndex < instructionCount; insnIndex++) {
			AbstractInsnNode insn = this.instructions.get(insnIndex);
			if (insn.isInvoke()) {
				MethodInvocation invocation = invocationsByInsn.get(insn);
				if (invocation != null) {
					ReflectionMethodPattern pattern = invocation.getPattern();
					if (pattern.matchesScope(hasRenamedNames, hasRenamedFields, hasRenamedMethods)) {
						reflectionHandled = true;
						ReflectionRewriteSnippet snippet = pattern.getRewriteSnippet();
						MethodCallWriter handlerMethodRef = constantPoolManager.getOrCreateMethodRef(classInternalName, snippet.getHandlerMethodName(), snippet.getHandlerMethodDescriptor(), typeWriters, workspace, classProvider);
						MethodCallWriter[] helperMethodRefs = null;
						String[] requiredHelperClasses;
						if ((requiredHelperClasses = snippet.getRequiredHelperClasses()) != null) {
							helperMethodRefs = new MethodCallWriter[requiredHelperClasses.length];
							String[] helperMethodNames = snippet.getHelperMethodNames();
							String[] helperMethodDescriptors = snippet.getHelperMethodDescriptors();

							for (int helperIndex = 0; helperIndex < helperMethodNames.length; helperIndex++) {
								helperMethodRefs[helperIndex] = constantPoolManager.getOrCreateMethodRef(requiredHelperClasses[helperIndex], helperMethodNames[helperIndex], helperMethodDescriptors[helperIndex], typeWriters, workspace, classProvider);
							}
						}

						List<AbstractInsnNode> generatedInsns = snippet.generateBytecode(handlerMethodRef, helperMethodRefs);
						instructionBlocks.add(new InstructionBlock(generatedInsns, insnIndex - 1, 0, snippet.getAdditionalStackRequired()));
						if (verbose) {
							scriptContext.getWriter()
								.println(
									"\tSet up AutoReflection handling of method '"
										+ new MethodSignature(pattern.getMethodSignature()).toString()
										+ "' call in method '"
										+ invocation.getMethodLocation()
										+ "' in class '"
										+ invocation.getSourcePath()
										+ "'"
								);
						}
					}
				}
			}
		}

		if (reflectionHandled) {
			this.applyPatchesWithoutStackCheck(instructionBlocks, "Auto Reflection Handling");
		}

		return reflectionHandled;
	}

	private int findDummyTryBlockStart(int startIndex, int endIndex, AtomicInt anchorIndex, int controlFlowLabelIndex, boolean avoidMonitors, MethodAnalyzer methodAnalyzer) {
		int blockStartIndex = -1;
		int nonLabelInsnCount = 0;
		if (endIndex > controlFlowLabelIndex) {
			for (int insnIndex = endIndex; insnIndex >= startIndex; insnIndex--) {
				AbstractInsnNode insn = this.instructions.get(insnIndex);
				if (insn.isVarStore() || insn.isJSR() || avoidMonitors && (insn.isMonitor() || methodAnalyzer.hasActiveMonitorsAtInsn(insnIndex)) || insnIndex <= controlFlowLabelIndex) {
					break;
				}

				blockStartIndex = insnIndex;
				if (!insn.isLabel()) {
					nonLabelInsnCount++;
				}
			}
		}

		if (nonLabelInsnCount <= 2) {
			return -1;
		} else {
			if (blockStartIndex > startIndex) {
			}

			int labelScanIndex = endIndex;
			AbstractInsnNode scannedInsn = this.instructions.get(endIndex);

			while (scannedInsn.isLabel()) {
				scannedInsn = this.instructions.get(--labelScanIndex);
			}

			if (labelScanIndex < endIndex) {
				anchorIndex.set(labelScanIndex);
			}

			if (blockStartIndex > -1) {
				CompactFrame frame = (CompactFrame)methodAnalyzer.getAnalyzedFrameAtInsn(blockStartIndex, true);
				if (frame == null || frame.hasUnresolvedTypes()) {
					return -1;
				}
			}

			return blockStartIndex;
		}
	}

	private void resolveClassReference(
		String className,
		boolean checkResourceBundle,
		Reference reference,
		DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferences,
		MultiMap<String, Reference> stringResourcePathReferences,
		Set<StringNode> processedStringNodes,
		Set<StructureNode> resolvedNodes,
		ClassRepository classRepository,
		AbstractWorkspace workspace
	) {
		if (className != null
			&& className.length() > 0
			&& (Character.isJavaIdentifierStart(className.charAt(0)) || className.charAt(0) == '[' && className.charAt(className.length() - 1) == ';')
			&& !(reference instanceof InstructionTypeReference)
			&& !(reference instanceof PrimitiveTypeReference)) {
			int arrayDimensionCount = 0;

			while (className.charAt(arrayDimensionCount) == '[') {
				arrayDimensionCount++;
			}

			if (arrayDimensionCount > 0) {
				className = className.substring(arrayDimensionCount);
				if (className.length() <= 1 || !className.endsWith(";")) {
					return;
				}

				className = className.substring(1, className.length() - 1);
			}

			String internalName = GlobalUtils.binaryToInternalName(className);
			AbstractClassNode classNode = findClassFromRepository(internalName, classRepository);
			if (classNode != null && (!checkResourceBundle || this.isResourceBundleSubclass(internalName, workspace))) {
				stringClassReferences.put(classNode, reference, INTEGER_CACHE.valueOf(arrayDimensionCount));
				resolvedNodes.add(classNode);
				if (reference instanceof StringReference) {
					processedStringNodes.add(((StringReference)reference).getStringNode());
				}
			} else if (checkResourceBundle && arrayDimensionCount == 0) {
				stringResourcePathReferences.put(className, reference);
				if (reference instanceof StringReference) {
					processedStringNodes.add(((StringReference)reference).getStringNode());
				}
			}
		}
	}

	private LocalVariableSlot prepareParameterObfuscation(
		ChangedMethod changedMethod,
		ParameterPattern changedPattern,
		long xorKey,
		boolean isParameterTarget,
		Map<AbstractClassNode, ChangedMethod> changedMethodsByClass,
		Map<MethodNode, ParameterPattern> parameterPatterns,
		Map<MethodNode, Long> keyOperands,
		List<Integer> usedLocalSlots,
		List<InstructionBlock> instructionBlocks,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		List<TypeWriter> typeWriters,
		Condition condition,
		Random random,
		ConstantPoolManager constantPoolManager,
		Hierarchy hierarchy,
		Set<MethodNode> parameterChangedMethods,
		ScriptContext scriptContext
	) {
		ArrayList<AbstractInsnNode> blockInstructions = new ArrayList<>();
		if (changedMethod != null && changedMethod.getMethodNode() == this.getMethodNode()) {
			List<AbstractInsnNode> decryptionInstructions = changedMethod.getDecryptionInstructions();
			blockInstructions.addAll(decryptionInstructions);
		}

		int instructionCount = this.instructions.size();
		boolean parameterChangeFound = false;
		LocalVariableSlot keyLocalSlot = null;
		MethodAnalyzer methodAnalyzer = null;
		int additionalStackRequired = 0;
		ArrayList<Integer> paramChangeIndexes = new ArrayList<>();
		HashSet<MethodNode> changedTargetMethods = GlobalUtils.newHashSet();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode insn = this.instructions.get(i);
			if (insn.isInvoke() || insn.getOpcode() == INVOKEDYNAMIC && Options.ZKM_INDY_PARAM_CHANGING) {
				WideConstantPoolInsnNode invokeInsn = (WideConstantPoolInsnNode)insn;
				AbstractMethodNode invokedMethod;
				if (insn.isInvoke()) {
					invokedMethod = (AbstractMethodNode)((AbstractMethodCallWriter)invokeInsn.getTypeWriter()).getTargetMember();
				} else {
					invokedMethod = ((InvokeDynamicCallWriter)invokeInsn.getTypeWriter()).getLambdaParamChangeTarget();
				}

				if (invokedMethod != null) {
					ParameterPattern invokedPattern = parameterPatterns.get(invokedMethod);
					if (invokedPattern != null && !invokedPattern.hasNoExtraParameters()) {
						parameterChangeFound = true;
						paramChangeIndexes.add(INTEGER_CACHE.valueOf(i));
						changedTargetMethods.add((MethodNode)invokedMethod);
					}
				}
			}
		}

		if (isParameterTarget || parameterChangeFound) {
			AtomicReference<LocalVariableSlot> slotReference = new AtomicReference<>();
			additionalStackRequired = this.generateDecryptionCode(changedMethod, changedPattern, xorKey, changedMethodsByClass, slotReference, blockInstructions, parameterChangedMethods, usedLocalSlots, typeWriters, condition, random, constantPoolManager, hierarchy);
			keyLocalSlot = slotReference.get();

			try {
				methodAnalyzer = this.getMethodAnalyzer(typeResolver, classHierarchy);
			} catch (StackOverflowError stackOverflowError) {
				throw new ZKMUnexpectedException("StackOverflowError trapped in '" + this.getParentString() + "' (7)", stackOverflowError);
			}
		}

		if (parameterChangeFound) {
			HashMap<MethodNode, int[]> keyLocalSlotsByMethod = GlobalUtils.newHashMap();
			int parameterDecryptionStackSize = this.generateParameterDecryptionCode(xorKey, keyLocalSlot, changedTargetMethods, changedMethodsByClass, parameterPatterns, keyOperands, blockInstructions, usedLocalSlots, keyLocalSlotsByMethod, typeWriters, constantPoolManager, hierarchy);
			additionalStackRequired = Math.max(additionalStackRequired, parameterDecryptionStackSize);
			LabelNode blockEndLabel = new LabelNode(8192);
			blockInstructions.add(blockEndLabel);
			InstructionBlock paramChangeBlock = new InstructionBlock(blockInstructions, -1, 0, additionalStackRequired);
			instructionBlocks.add(paramChangeBlock);
			MultiMap<Integer, AbstractInsnNode> instructionsByOffset = new MultiMap<>();
			CodeAttribute codeAttribute = this.getCodeAttribute();
			int originalMaxLocals = codeAttribute.getMaxLocals();
			AtomicInt maxLocals = new AtomicInt(originalMaxLocals);

			for (int instructionIndex : paramChangeIndexes) {
				WideConstantPoolInsnNode callInsn = (WideConstantPoolInsnNode)this.instructions.get(instructionIndex);
				AbstractMethodNode targetMethod;
				String descriptor;
				if (callInsn.isInvoke()) {
					AbstractMethodCallWriter callWriter = (AbstractMethodCallWriter)callInsn.getTypeWriter();
					targetMethod = (AbstractMethodNode)callWriter.getTargetMember();
					descriptor = callWriter.getDescriptor();
				} else {
					InvokeDynamicCallWriter indyCallWriter = (InvokeDynamicCallWriter)callInsn.getTypeWriter();
					targetMethod = indyCallWriter.getLambdaParamChangeTarget();
					descriptor = indyCallWriter.getDescriptor();
				}

				ParameterPattern targetPattern = parameterPatterns.get(targetMethod);
				int[] extraParameterSlots = keyLocalSlotsByMethod.get(targetMethod);
				this.rewriteMethodParameters(changedPattern, xorKey, keyLocalSlot, methodAnalyzer, instructionIndex, descriptor, targetMethod, targetPattern, extraParameterSlots, instructionsByOffset, usedLocalSlots, originalMaxLocals, maxLocals, typeWriters);
			}

			if (methodAnalyzer != null) {
				methodAnalyzer.release();
			}

			if (maxLocals.get() > originalMaxLocals) {
				codeAttribute.setMaxLocals(maxLocals.get());
			}

			for (Entry<Integer, List<AbstractInsnNode>> offsetEntry : instructionsByOffset.entrySet()) {
				InstructionBlock offsetBlock = new InstructionBlock(offsetEntry.getValue(), offsetEntry.getKey(), 0, 1);
				instructionBlocks.add(offsetBlock);
			}
		} else if (blockInstructions.size() > 0) {
			LabelNode decryptionBlockEndLabel = new LabelNode(8192);
			blockInstructions.add(decryptionBlockEndLabel);
			InstructionBlock decryptionBlock = new InstructionBlock(blockInstructions, -1, 0, additionalStackRequired);
			instructionBlocks.add(decryptionBlock);
		}

		return keyLocalSlot;
	}

	private boolean isSubclassOrSuperclass(String classInternalName, String otherClassInternalName, PhantomClassManager phantomClassManager, ClassHierarchy classHierarchy) {
		try {
			return classInternalName.equals(otherClassInternalName) || classHierarchy.isClassInstanceOf(classInternalName, otherClassInternalName) || classHierarchy.isClassInstanceOf(otherClassInternalName, classInternalName);
		} catch (FileException fileException) {
			if (phantomClassManager != null && phantomClassManager.isClassIgnored(fileException.getClassName().replace('.', '/'), new AtomicReference<>())) {
				return true;
			} else {
				throw fileException;
			}
		}
	}

	public int getByteCodeLength() {
		return this.bytecodeLength;
	}

	private void moveTailVariables(List<Integer> tailVariableSlots) {
		int tailVariableCount = tailVariableSlots.size();
		int firstNonParamSlot = ((MethodNode)this.getParent().getParent()).getFirstNonParamLocalSlot();
		this.localVariableTable.moveTailVariablesTo(tailVariableCount, firstNonParamSlot);
	}

	private int findLastExitInstruction() {
		int athrowIndex = -1;
		int jumpIndex = -1;

		for (int instructionIndex = this.instructions.size() - 1; instructionIndex >= 0; instructionIndex--) {
			AbstractInsnNode insn = this.instructions.get(instructionIndex);
			if (insn.isReturn()) {
				return instructionIndex;
			}

			if (athrowIndex == -1 && insn.getOpcode() == ATHROW) {
				athrowIndex = instructionIndex;
			}

			if (jumpIndex == -1 && insn.isJump()) {
				jumpIndex = instructionIndex;
			}
		}

		return athrowIndex > -1 ? athrowIndex : jumpIndex;
	}

	public void collectMethodCalls(MultiMap<AbstractMethodNode, InstructionListNode> callSitesByMethod) {
		int instructionCount = this.instructions.size();
		HashSet<AbstractMethodNode> calledMethods = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(Math.min(Math.max(5, instructionCount / 2), 101)));

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode insn = this.instructions.get(i);
			if (insn.isInvoke()) {
				AbstractMethodNode invokedMethod = (AbstractMethodNode)((AbstractMethodCallWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getTargetMember();
				if (invokedMethod != null && !invokedMethod.isConstructor() && !invokedMethod.isClinit() && insn.getOpcode() != INVOKESPECIAL) {
					calledMethods.add(invokedMethod);
				}
			}
		}

		for (AbstractMethodNode calledMethod : calledMethods) {
			callSitesByMethod.put(calledMethod, this);
		}
	}

	private void collectMethodDependencies(AbstractMethodCallWriter methodCallWriter, Set<MethodNode> methods, Set<ClassNode> dependencyClasses, Set<ClassNode> unused) {
		String ownerName = methodCallWriter.getOwnerName();
		ClassNode ownerClassNode = null;
		if (!ownerName.startsWith("[")) {
			ownerClassNode = ClassHierarchyNode.getWorkspaceClassNode(ownerName);
			if (ownerClassNode != null) {
				dependencyClasses.add(this.getEquivalentClassNode(ownerClassNode));
			}
		}

		AbstractMethodNode targetMethod = (AbstractMethodNode)methodCallWriter.getTargetMember();
		if (targetMethod != null && targetMethod.isWorkspaceNode()) {
			methods.add((MethodNode)targetMethod);
			ClassNode targetClassNode = (ClassNode)targetMethod.getClassNode();
			if (ownerClassNode == null || targetClassNode != ownerClassNode) {
				dependencyClasses.add(this.getEquivalentClassNode(targetClassNode));
			}
		}
	}

	public int[] insertInstructionsAtOffset(List<AbstractInsnNode> instructions, int insertIndex, String reason, List<Integer> tailVariableSlots) {
		int adjustedInsertIndex = insertIndex;
		if (insertIndex == 0) {
			for (int i = 0; i < this.instructions.size(); i++) {
				AbstractInsnNode existingInsn = this.instructions.get(i);
				if (existingInsn.getOpcode() == 2147483646) {
					LabelNode label = (LabelNode)existingInsn;
					if (label.isFixed()) {
						adjustedInsertIndex = i + 1;
					} else if (label.hasFlag(8192)) {
						adjustedInsertIndex = i + 1;
						break;
					}
				}
			}
		}

		int insertedCount = instructions.size();
		this.instructions.size();
		if (insertedCount > 0) {
			this.setModified(true);
		}

		this.instructions.addAll(adjustedInsertIndex, instructions);

		for (AbstractInsnNode insertedInsn : instructions) {
			if (insertedInsn.isString()) {
				this.stringConstantCount++;
			}

			if (insertedInsn.isIntegerConstant2()) {
				this.intConstantCount++;
			}

			if (insertedInsn.isLong()) {
				this.longConstantCount++;
			}
		}

		if (tailVariableSlots != null && tailVariableSlots.size() > 0 && Options.ZKM_ADJUST_LOCALS) {
			this.moveTailVariables(tailVariableSlots);
		}

		int originalByteCodeLength = this.getByteCodeLength();
		this.recalculateOffsets();
		if (this.getByteCodeLength() > 65535) {
			throw new ProcessingException(
				reason
					+ " : Bytecode length greater than "
					+ '\uffff'
					+ " in "
					+ this.getParentString()
					+ " in file '"
					+ this.getRawSourcePath()
					+ "' (5). (original="
					+ originalByteCodeLength
					+ ", new="
					+ this.getByteCodeLength()
					+ ") : '"
					+ reason
					+ "'"
			);
		} else {
			return null;
		}
	}

	public boolean isFieldWritten(FieldNode field) {
		int instructionCount = this.instructions.size();
		int i = 0;

		while (i < instructionCount) {
			AbstractInsnNode insn = this.instructions.get(i);
			switch (insn.getOpcode()) {
				case PUTSTATIC:
				case PUTFIELD:
					if (((FieldTypeWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getTargetMember() == field) {
						return true;
					}
				default:
					i++;
			}
		}

		return false;
	}

	private void generateHandlerBlock(
		List<AbstractInsnNode> instructions,
		LabelNode startLabel,
		LabelNode endLabel,
		ObfuscateReferencesBuilder referencesBuilder,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode afterHandlerLabel = new LabelNode(true, 1);
		instructions.add(startLabel);
		instructions.add(new GotoNode(afterHandlerLabel));
		instructions.add(endLabel);
		MethodCallWriter exceptionCallWriter = referencesBuilder.genGetTargetExceptionCall(constantPoolManager, typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, exceptionCallWriter));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(afterHandlerLabel);
	}

	public CodeAttribute getCodeAttribute() {
		return (CodeAttribute)this.getParent();
	}

	public void encryptIntegerConstantsWithLong(
		List<KeyValuePair<PriorityEntry<WritableType>, IntegerEntry>> integerConstantEntries,
		boolean isClinit,
		Map<LabelNode, AtomicInt> labelCounters,
		int[][] exceptionRanges,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelReplacements,
		Long xorKey,
		LocalVariableOperand xorKeyOperand,
		Indexable localSlotIndex,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		Comparator<KeyValuePair<PriorityEntry<WritableType>, IntegerEntry>> entryComparator = new Comparator<KeyValuePair<PriorityEntry<WritableType>, IntegerEntry>>() {
			@Override
			public int compare(KeyValuePair<PriorityEntry<WritableType>, IntegerEntry> var1, KeyValuePair<PriorityEntry<WritableType>, IntegerEntry> var2x) {
				return var1.getKey().compareTo(var2x.getKey());
			}
		};
		Collections.sort(integerConstantEntries, entryComparator);
		int constantCount = integerConstantEntries.size();
		int cachedArraySlotIndex = -1;
		if (constantCount != 0) {
			MethodAnalyzer methodAnalyzer = null;
			Iterator<KeyValuePair<PriorityEntry<WritableType>, IntegerEntry>> entryIterator = integerConstantEntries.iterator();
			KeyValuePair<PriorityEntry<WritableType>, IntegerEntry> entry = entryIterator.next();
			WritableType targetInsn = entry.getKey().getValue();
			int remainingEntries = constantCount;
			this.setModified(true);
			int additionalStackRequired = 0;
			boolean arrayValueCached = false;
			boolean cacheSlotAllocated = false;
			ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
			int instructionCount = this.instructions.size();

			for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
				AbstractInsnNode insn = this.instructions.get(instructionIndex);
				if (insn.isLabel()) {
					LabelNode label = (LabelNode)insn;
					if (label.hasFlag(1) || label.hasFlag(1024)) {
						arrayValueCached = false;
					}
				}

				if (insn == targetInsn) {
					remainingEntries--;
					IntegerEntry integerEntry = entry.getValue();
					additionalStackRequired = 0;
					switch (integerEntry.getLookupType()) {
						case LOCAL_ARRAY_AND_INDEX:
							ArrayList<AbstractInsnNode> localArrayInstructions = new ArrayList<>();
							if (integerEntry.hasIndex()) {
								additionalStackRequired = 1;
								localArrayInstructions.add(AbstractInsnNode.loadObject(integerEntry.getLocalArrayIndex(), this.localVariableTable, 11));
								this.pushIntConstant(integerEntry.getIndex(), localArrayInstructions, constantPoolManager, typeWriters, constantCount);
								localArrayInstructions.add(InsnNode.of(LALOAD));
								localArrayInstructions.add(InsnNode.of(L2I));
							} else {
								additionalStackRequired = 2;
								localArrayInstructions.add(AbstractInsnNode.loadLong(integerEntry.getLocalArrayIndex(), this.localVariableTable, 11));
								localArrayInstructions.add(InsnNode.of(L2I));
							}

							instructionBlocks.add(new InstructionBlock(localArrayInstructions, instructionIndex - 1, 1, additionalStackRequired));
							break;
						case FIELD_ARRAY_AND_INDEX:
							ArrayList<AbstractInsnNode> fieldArrayInstructions = new ArrayList<>();
							FieldTypeWriter arrayFieldWriter = integerEntry.getArrayFieldWriter();
							if (integerEntry.hasIndex()) {
								additionalStackRequired = 1;
								if (arrayValueCached) {
									fieldArrayInstructions.add(AbstractInsnNode.loadObject(cachedArraySlotIndex, this.localVariableTable, 11));
								} else {
									fieldArrayInstructions.add(new WideConstantPoolInsnNode(GETSTATIC, arrayFieldWriter));
									if (remainingEntries > 0 && (exceptionRanges.length == 0 || !this.isWithinExceptionRange(instructionIndex, exceptionRanges))) {
										if (!cacheSlotAllocated) {
											cacheSlotAllocated = true;
											cachedArraySlotIndex = localSlotIndex.get();
											localSlotIndex.increaseAndGet();
										}

										fieldArrayInstructions.add(AbstractInsnNode.storeObject(cachedArraySlotIndex, this.localVariableTable, 11));
										fieldArrayInstructions.add(AbstractInsnNode.loadObject(cachedArraySlotIndex, this.localVariableTable, 11));
										arrayValueCached = true;
									}
								}

								AbstractInsnNode.encryptIntConstant(integerEntry.getIndex(), fieldArrayInstructions, constantPoolManager, typeWriters);
								fieldArrayInstructions.add(InsnNode.of(LALOAD));
								fieldArrayInstructions.add(InsnNode.of(L2I));
							} else {
								fieldArrayInstructions.add(new WideConstantPoolInsnNode(GETSTATIC, arrayFieldWriter));
								fieldArrayInstructions.add(InsnNode.of(L2I));
							}

							instructionBlocks.add(new InstructionBlock(fieldArrayInstructions, instructionIndex - 1, 1, additionalStackRequired));
							break;
						case LOOKUP_METHOD_AND_INDEX:
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							AbstractMethodCallWriter callWriter = integerEntry.getMethodCallWriter();
							ArrayList<AbstractInsnNode> indexInstructions = new ArrayList<>();
							ArrayList<AbstractInsnNode> keyInstructions = new ArrayList<>();
							int constantIndex = integerEntry.getIndex();
							long encryptionKey = integerEntry.getEncryptionKey();
							if (xorKey != null && xorKeyOperand != null) {
								int xoredIndex = constantIndex ^ (int)(encryptionKey & 32767L);
								this.pushIntConstant(xoredIndex, indexInstructions, constantPoolManager, typeWriters, constantCount);
								long xoredKey = encryptionKey ^ xorKey;
								AbstractInsnNode.appendLongConst(xoredKey, keyInstructions, constantPoolManager, typeWriters);
								keyInstructions.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 11));
								keyInstructions.add(InsnNode.of(LXOR));
							} else {
								int plainIndex = constantIndex ^ (int)(encryptionKey & 32767L);
								this.pushIntConstant(plainIndex, indexInstructions, constantPoolManager, typeWriters, constantCount);
								AbstractInsnNode.appendLongConst(encryptionKey, keyInstructions, constantPoolManager, typeWriters);
							}

							WideConstantPoolInsnNode invokeInstruction = new WideConstantPoolInsnNode(INVOKESTATIC, callWriter);
							this.insertInstructionBlock(indexInstructions, keyInstructions, invokeInstruction, instructionBlocks, methodAnalyzer, instructionIndex, 1);
							break;
						case LOOKUP_METHOD_AND_INDEX_DES:
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							AbstractMethodCallWriter desCallWriter = integerEntry.getMethodCallWriter();
							ArrayList<AbstractInsnNode> desIndexInstructions = new ArrayList<>();
							ArrayList<AbstractInsnNode> desKeyInstructions = new ArrayList<>();
							int desConstantIndex = integerEntry.getIndex();
							long desEncryptionKey = integerEntry.getEncryptionKey();
							if (xorKey != null && xorKeyOperand != null) {
								int desXoredIndex = desConstantIndex ^ (int)(desEncryptionKey & 32767L);
								this.pushIntConstant(desXoredIndex, desIndexInstructions, constantPoolManager, typeWriters, constantCount);
								AbstractInsnNode.appendLongConst(desEncryptionKey ^ xorKey, desKeyInstructions, constantPoolManager, typeWriters);
								desKeyInstructions.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 11));
								desKeyInstructions.add(InsnNode.of(LXOR));
							} else {
								AbstractInsnNode.encryptIntConstant(desConstantIndex ^ (int)desEncryptionKey, desIndexInstructions, constantPoolManager, typeWriters);
								AbstractInsnNode.appendLongConst(desEncryptionKey, desKeyInstructions, constantPoolManager, typeWriters);
							}

							WideConstantPoolInsnNode desInvokeInstruction = new WideConstantPoolInsnNode(INVOKESTATIC, desCallWriter);
							this.insertInstructionBlock(desIndexInstructions, desKeyInstructions, desInvokeInstruction, instructionBlocks, methodAnalyzer, instructionIndex, 1);
							break;
						case INDY_ENTRY_AND_INDEX:
							InvokeDynamicCallWriter indyCallWriter = integerEntry.getIndyCallWriter();
							methodAnalyzer = this.getOrCreateMethodAnalyzer(methodAnalyzer, typeResolver, classHierarchy);
							ArrayList<AbstractInsnNode> indyIndexInstructions = new ArrayList<>();
							ArrayList<AbstractInsnNode> indyKeyInstructions = new ArrayList<>();
							int indyConstantIndex = integerEntry.getIndex();
							long indyEncryptionKey = integerEntry.getEncryptionKey();
							if (xorKey != null && xorKeyOperand != null) {
								int indyXoredIndex = indyConstantIndex ^ (int)(indyEncryptionKey & 32767L);
								this.pushIntConstant(indyXoredIndex, indyIndexInstructions, constantPoolManager, typeWriters, constantCount);
								long indyXoredKey = indyEncryptionKey ^ xorKey;
								AbstractInsnNode.appendLongConst(indyXoredKey, indyKeyInstructions, constantPoolManager, typeWriters);
								indyKeyInstructions.add(AbstractInsnNode.loadLong(xorKeyOperand.getIndex(), this.localVariableTable, 11));
								indyKeyInstructions.add(InsnNode.of(LXOR));
							} else {
								int indyPlainIndex = indyConstantIndex ^ (int)indyEncryptionKey;
								this.pushIntConstant(indyPlainIndex, indyIndexInstructions, constantPoolManager, typeWriters, constantCount);
								AbstractInsnNode.appendLongConst(indyEncryptionKey, indyKeyInstructions, constantPoolManager, typeWriters);
							}

							InvokeDynamicNode indyInvokeInstruction = new InvokeDynamicNode(indyCallWriter);
							this.insertInstructionBlock(indyIndexInstructions, indyKeyInstructions, indyInvokeInstruction, instructionBlocks, methodAnalyzer, instructionIndex, 1);
					}

					if (!entryIterator.hasNext()) {
						break;
					}

					entry = entryIterator.next();
					targetInsn = entry.getKey().getValue();
				}
			}

			this.applyInstructionPatches(instructionBlocks, "Integer Constant Encryption", isClinit);
			if (!isClinit) {
				CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
				int originalMaxStack = codeAttribute.getMaxStack();
				codeAttribute.setMaxStack(originalMaxStack + additionalStackRequired);
			}
		}
	}

	public void setInstructions(ArrayList<AbstractInsnNode> instructions, String reason) {
		this.instructions = instructions;
		this.recalculateOffsets();
		if (this.getByteCodeLength() > 65535) {
			throw new ProcessingException(
				reason
					+ " : Bytecode length greater than "
					+ '\uffff'
					+ " in "
					+ this.getParentString()
					+ " in file '"
					+ this.getRawSourcePath()
					+ "' (2). (new="
					+ this.getByteCodeLength()
					+ ") : '"
					+ reason
					+ "'"
			);
		} else {
			this.setModified(true);
		}
	}

	private void replaceGetFieldWithReflection(
		AbstractInsnNode memberAccessInsn,
		int instructionIndex,
		FieldTypeWriter fieldTypeWriter,
		LongNode longConstant,
		AbstractInsnNode keyLoadInsn,
		List<InstructionBlock> instructionBlocks,
		AbstractMethodCallWriter fieldHelperWriter,
		MethodAnalyzer methodAnalyzer,
		Map<AbstractInsnNode, Boolean> invokeDynamicFlags,
		ObfuscateReferencesBuilder referencesBuilder,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> replacementInstructions = new ArrayList<>();
		replacementInstructions.add(new WideConstantPoolInsnNode(LDC2_W, longConstant));
		if (keyLoadInsn != null) {
			replacementInstructions.add(keyLoadInsn);
		}

		replacementInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, fieldHelperWriter));
		if (memberAccessInsn.getOpcode() == GETSTATIC) {
			replacementInstructions.add(InsnNode.of(ACONST_NULL));
		} else if (memberAccessInsn.getOpcode() == GETFIELD) {
			replacementInstructions.add(InsnNode.of(SWAP));
		}

		AtomicBool castRequired = new AtomicBool(false);
		MethodCallWriter fieldGetterCall = referencesBuilder.genFieldGetterCall(fieldTypeWriter, castRequired, constantPoolManager, typeWriters, workspace, classProvider);
		replacementInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, fieldGetterCall));
		if (castRequired.get() && this.isReturnValueUsed(instructionIndex, fieldTypeWriter, invokeDynamicFlags, methodAnalyzer)) {
			ClassInfoWriter castClassInfo = constantPoolManager.getOrCreateClassInfo(TypeWriter.getClassInternalName(fieldTypeWriter.getStackType()), typeWriters);
			replacementInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, castClassInfo));
		}

		InstructionBlock instructionBlock = new InstructionBlock(replacementInstructions, instructionIndex - 1, 1, 2);
		instructionBlocks.add(instructionBlock);
	}

	public MethodAnalyzer createMethodAnalyzer(TypeResolver typeResolver, ClassHierarchy classHierarchy, boolean unusedFlag) {
		CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
		MethodAnalyzer methodAnalyzer = new MethodAnalyzer(
			new InsnList<>(this.instructions),
			this.copyExceptionTableEntries(),
			codeAttribute.getMaxLocals(),
			codeAttribute.getMaxStack(),
			codeAttribute.getParameterTypeDescriptors(),
			codeAttribute.isStatic(),
			this.isConstructor(),
			typeResolver,
			classHierarchy,
			this,
			unusedFlag
		);
		this.hasControlFlow = methodAnalyzer.hasControlFlowNodes();
		return methodAnalyzer;
	}

	public List<InstructionBlock> applyReferenceObfuscation(
		List<MemberContext> memberContexts,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		Map<MemberContext, Integer> memberKeyIndexes,
		Map<TypeWriter, MemberContext> memberContextsByWriter,
		Map<Integer, Long> memberKeys,
		AbstractMethodCallWriter fieldHelperWriter,
		AbstractMethodCallWriter methodHelperWriter,
		Map<MemberContext, AbstractMethodCallWriter> memberHelperWriters,
		AbstractMethodCallWriter indyHelperWriter,
		int[] helperIndexes,
		Map<String, InvokeDynamicCallWriter> indyWritersByName,
		AtomicReference<BootstrapMethodsAttribute> bootstrapMethodsRef,
		Long xorKey,
		LocalVariableOperand keyLocalSlotOperand,
		ObfuscateReferencesBuilder referencesBuilder,
		ConstantPoolManager constantPoolManager,
		List<ExceptionEntryNode> exceptionEntries,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		TypeResolver typeResolver,
		boolean useInvokeDynamic
	) {
		HashMap<AbstractInsnNode, Boolean> invokeDynamicFlags = GlobalUtils.newHashMap();

		for (MemberContext memberContext : memberContexts) {
			Boolean indyEligible = useInvokeDynamic && ObfuscateReferencesBuilder.isMemberEligible(memberContext, workspace);
			List<WideConstantPoolInsnNode> obfuscationRefs = memberObfuscationRefs.getValues(memberContext, this);
			if (obfuscationRefs != null) {
				for (WideConstantPoolInsnNode memberRefInsn : obfuscationRefs) {
					invokeDynamicFlags.put(memberRefInsn, indyEligible);
				}
			}
		}

		MethodAnalyzer methodAnalyzer;
		try {
			methodAnalyzer = this.getMethodAnalyzer(typeResolver, workspace);
		} catch (StackOverflowError stackOverflowError) {
			throw new ZKMUnexpectedException("StackOverflowError trapped in '" + this.getParentString() + "' (3)", stackOverflowError);
		}

		ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
		int instructionCount = this.instructions.size();

		for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
			AbstractInsnNode memberAccessInsn = this.instructions.get(instructionIndex);
			if (invokeDynamicFlags.containsKey(memberAccessInsn)) {
				MemberTypeWriter memberTypeWriter = (MemberTypeWriter)((WideConstantPoolInsnNode)memberAccessInsn).getTypeWriter();
				MemberContext referencedMemberContext = memberContextsByWriter.get(memberTypeWriter);
				if (referencedMemberContext != null && memberKeyIndexes.containsKey(referencedMemberContext)) {
					Integer memberKeyIndex = memberKeyIndexes.get(referencedMemberContext);
					Long memberKey = memberKeys.get(memberKeyIndex);
					this.obfuscateMemberAccess(
						(WideConstantPoolInsnNode)memberAccessInsn,
						instructionIndex,
						memberTypeWriter,
						referencedMemberContext,
						memberKey,
						instructionBlocks,
						fieldHelperWriter,
						methodHelperWriter,
						memberHelperWriters,
						indyHelperWriter,
						helperIndexes,
						indyWritersByName,
						bootstrapMethodsRef,
						xorKey,
						keyLocalSlotOperand,
						referencesBuilder,
						methodAnalyzer,
						invokeDynamicFlags,
						constantPoolManager,
						exceptionEntries,
						typeWriters,
						workspace,
						classProvider
					);
				}
			}
		}

		return instructionBlocks;
	}

	public boolean hasIntegerConstants() {
		return this.intConstantCount > 0;
	}

	private void collectFieldDependenciesForHierarchy(FieldTypeWriter fieldWriter, Set<AbstractFieldNode> fields, Set<AbstractClassNode> dependencyClasses, Set<AbstractClassNode> unused) {
		AbstractClassNode ownerClassNode = ClassHierarchyNode.getClassNodeByInternalName(fieldWriter.getOwnerName());
		if (ownerClassNode != null) {
			dependencyClasses.add(ownerClassNode);
		}

		AbstractFieldNode targetField = (AbstractFieldNode)fieldWriter.getTargetMember();
		if (targetField != null && (targetField.isWorkspaceNode() || ClassHierarchyNode.isMissingClass(targetField.getInternalName()))) {
			fields.add(targetField);
			AbstractClassNode fieldClassNode = targetField.getClassNode();
			if ((ownerClassNode == null || fieldClassNode != ownerClassNode) && !ClassHierarchyNode.isMissingClass(fieldClassNode.getInternalName())) {
				dependencyClasses.add(fieldClassNode);
			}
		}
	}

	private void obfuscateMemberAccess(
		WideConstantPoolInsnNode memberAccessInsn,
		int instructionIndex,
		MemberTypeWriter memberTypeWriter,
		MemberContext memberContext,
		Long memberKey,
		List<InstructionBlock> instructionBlocks,
		AbstractMethodCallWriter fieldHelperWriter,
		AbstractMethodCallWriter methodHelperWriter,
		Map<MemberContext, AbstractMethodCallWriter> memberHelperWriters,
		AbstractMethodCallWriter indyHelperWriter,
		int[] helperIndexes,
		Map<String, InvokeDynamicCallWriter> indyWritersByName,
		AtomicReference<BootstrapMethodsAttribute> bootstrapMethodsRef,
		Long xorKey,
		LocalVariableOperand keyLocalSlotOperand,
		ObfuscateReferencesBuilder referencesBuilder,
		MethodAnalyzer methodAnalyzer,
		Map<AbstractInsnNode, Boolean> invokeDynamicFlags,
		ConstantPoolManager constantPoolManager,
		List<ExceptionEntryNode> exceptionEntries,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		AbstractInsnNode keyLoadInsn = null;
		long obfuscatedMemberKey;
		if (xorKey != null && keyLocalSlotOperand != null) {
			obfuscatedMemberKey = memberKey ^ (xorKey << 48 | xorKey);
			keyLoadInsn = AbstractInsnNode.loadLong(keyLocalSlotOperand.getIndex(), this.localVariableTable, 4);
		} else {
			obfuscatedMemberKey = memberKey;
		}

		LongNode longConstant = constantPoolManager.getOrCreateLong(obfuscatedMemberKey, typeWriters);
		if (invokeDynamicFlags.get(memberAccessInsn)) {
			MemberNode memberNode = memberContext.getMemberNode();
			String memberDescriptor = memberContext.getMemberDescriptor();
			String contextClassInternalName = memberContext.getContextClass().getInternalName();
			StringBuilder descriptorBuilder = new StringBuilder();
			String receiverInternalName;
			if (memberNode.isProtected() && !memberNode.getPackageName().equals(this.getPackageName())) {
				receiverInternalName = this.getInternalName();
			} else {
				receiverInternalName = contextClassInternalName;
			}

			String keyDescriptor = keyLoadInsn == null ? "J" : "JJ";
			int helperIndex;
			switch (memberAccessInsn.getOpcode()) {
				case GETSTATIC:
					descriptorBuilder.append('(');
					descriptorBuilder.append(keyDescriptor);
					descriptorBuilder.append(')');
					descriptorBuilder.append(memberDescriptor);
					helperIndex = helperIndexes[2];
					break;
				case PUTSTATIC:
					descriptorBuilder.append('(');
					descriptorBuilder.append(memberDescriptor);
					descriptorBuilder.append(keyDescriptor);
					descriptorBuilder.append(")V");
					helperIndex = helperIndexes[3];
					break;
				case GETFIELD:
					descriptorBuilder.append("(L");
					descriptorBuilder.append(Options.keepRealTypesInDescriptors ? receiverInternalName : "java/lang/Object");
					descriptorBuilder.append(';');
					descriptorBuilder.append(keyDescriptor);
					descriptorBuilder.append(')');
					descriptorBuilder.append(memberDescriptor);
					helperIndex = helperIndexes[0];
					break;
				case PUTFIELD:
					descriptorBuilder.append("(L");
					descriptorBuilder.append(Options.keepRealTypesInDescriptors ? receiverInternalName : "java/lang/Object");
					descriptorBuilder.append(";");
					descriptorBuilder.append(memberDescriptor);
					descriptorBuilder.append(keyDescriptor);
					descriptorBuilder.append(")V");
					helperIndex = helperIndexes[1];
					break;
				case INVOKEVIRTUAL:
				case INVOKEINTERFACE:
					descriptorBuilder.append(obfuscateTypeDescriptor(keyDescriptor, this.prependReceiverType(receiverInternalName, memberDescriptor)));
					helperIndex = helperIndexes[4];
					break;
				case INVOKESPECIAL:
					descriptorBuilder.append(obfuscateTypeDescriptor(keyDescriptor, this.prependReceiverType(contextClassInternalName, memberDescriptor)));
					helperIndex = helperIndexes[6];
					break;
				case INVOKESTATIC:
					descriptorBuilder.append(obfuscateTypeDescriptor(keyDescriptor, memberDescriptor));
					helperIndex = helperIndexes[5];
					break;
				default:
					helperIndex = helperIndexes[4];
			}

			this.replaceWithInvokeDynamic(instructionIndex, longConstant, obfuscatedMemberKey, keyLoadInsn, xorKey, memberContext, descriptorBuilder.toString(), helperIndex, bootstrapMethodsRef, indyHelperWriter, indyWritersByName, instructionBlocks, typeWriters, constantPoolManager);
		} else {
			switch (memberAccessInsn.getOpcode()) {
				case GETSTATIC:
					this.replaceGetFieldWithReflection(memberAccessInsn, instructionIndex, (FieldTypeWriter)memberTypeWriter, longConstant, keyLoadInsn, instructionBlocks, fieldHelperWriter, methodAnalyzer, invokeDynamicFlags, referencesBuilder, constantPoolManager, typeWriters, workspace, classProvider);
					break;
				case PUTSTATIC:
					ArrayList<AbstractInsnNode> staticFieldInsn = new ArrayList<>();
					FieldTypeWriter staticFieldTypeWriter = (FieldTypeWriter)memberTypeWriter;
					staticFieldInsn.add(new WideConstantPoolInsnNode(LDC2_W, longConstant));
					if (keyLoadInsn != null) {
						staticFieldInsn.add(keyLoadInsn);
					}

					staticFieldInsn.add(new WideConstantPoolInsnNode(INVOKESTATIC, fieldHelperWriter));
					staticFieldInsn.add(InsnNode.of(ACONST_NULL));
					boolean staticLongOrDouble = referencesBuilder.isLongOrDoubleType(staticFieldTypeWriter);
					if (staticLongOrDouble) {
						staticFieldInsn.add(InsnNode.of(DUP2_X2));
					} else {
						staticFieldInsn.add(InsnNode.of(DUP2_X1));
					}

					staticFieldInsn.add(InsnNode.of(POP2));
					MethodCallWriter staticFieldSetterCall = referencesBuilder.genFieldSetterCall(staticFieldTypeWriter, constantPoolManager, typeWriters, workspace, classProvider);
					staticFieldInsn.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, staticFieldSetterCall));
					int staticOffset = instructionIndex - 1;
					int staticAdditionalStack = staticLongOrDouble ? 6 : 5;
					InstructionBlock staticFieldBlock = new InstructionBlock(staticFieldInsn, staticOffset, 1, staticAdditionalStack);
					instructionBlocks.add(staticFieldBlock);
					break;
				case GETFIELD:
					this.replaceGetFieldWithReflection(memberAccessInsn, instructionIndex, (FieldTypeWriter)memberTypeWriter, longConstant, keyLoadInsn, instructionBlocks, fieldHelperWriter, methodAnalyzer, invokeDynamicFlags, referencesBuilder, constantPoolManager, typeWriters, workspace, classProvider);
					break;
				case PUTFIELD:
					ArrayList<AbstractInsnNode> fieldInsn = new ArrayList<>();
					FieldTypeWriter fieldTypeWriter = (FieldTypeWriter)memberTypeWriter;
					boolean fieldLongOrDouble = referencesBuilder.isLongOrDoubleType(fieldTypeWriter);
					if (fieldLongOrDouble) {
						fieldInsn.add(InsnNode.of(DUP2_X1));
						fieldInsn.add(InsnNode.of(POP2));
						fieldInsn.add(new WideConstantPoolInsnNode(LDC2_W, longConstant));
						if (keyLoadInsn != null) {
							fieldInsn.add(keyLoadInsn);
						}

						fieldInsn.add(new WideConstantPoolInsnNode(INVOKESTATIC, fieldHelperWriter));
						fieldInsn.add(InsnNode.of(SWAP));
						fieldInsn.add(InsnNode.of(DUP2_X2));
						fieldInsn.add(InsnNode.of(POP2));
					} else {
						fieldInsn.add(new WideConstantPoolInsnNode(LDC2_W, longConstant));
						if (keyLoadInsn != null) {
							fieldInsn.add(keyLoadInsn);
						}

						fieldInsn.add(new WideConstantPoolInsnNode(INVOKESTATIC, fieldHelperWriter));
						fieldInsn.add(InsnNode.of(DUP_X2));
						fieldInsn.add(InsnNode.of(POP));
					}

					MethodCallWriter fieldSetterCall = referencesBuilder.genFieldSetterCall(fieldTypeWriter, constantPoolManager, typeWriters, workspace, classProvider);
					fieldInsn.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, fieldSetterCall));
					int fieldOffset = instructionIndex - 1;
					int fieldAdditionalStack = fieldLongOrDouble ? 6 : 4;
					InstructionBlock fieldBlock = new InstructionBlock(fieldInsn, fieldOffset, 1, fieldAdditionalStack);
					instructionBlocks.add(fieldBlock);
					break;
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKEINTERFACE:
					this.replaceWithReflection(
						memberAccessInsn, instructionIndex, memberContext, (AbstractMethodCallWriter)memberTypeWriter, longConstant, keyLoadInsn, instructionBlocks, methodHelperWriter, memberHelperWriters, methodAnalyzer, invokeDynamicFlags, referencesBuilder, constantPoolManager, exceptionEntries, typeWriters, workspace, classProvider
					);
					break;
				case INVOKESTATIC:
					this.replaceWithReflection(
						memberAccessInsn, instructionIndex, memberContext, (AbstractMethodCallWriter)memberTypeWriter, longConstant, keyLoadInsn, instructionBlocks, methodHelperWriter, memberHelperWriters, methodAnalyzer, invokeDynamicFlags, referencesBuilder, constantPoolManager, exceptionEntries, typeWriters, workspace, classProvider
					);
			}
		}
	}

	public boolean hasStringConstants() {
		return this.stringConstantCount > 0;
	}

	public void mapMemberAccessInsns(
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadAccesses,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteAccesses,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodAccesses,
		IntegerCache integerCache
	) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode insn = this.instructions.get(i);
			switch (insn.getOpcode()) {
				case GETSTATIC:
				case GETFIELD:
					AbstractFieldNode readFieldNode = (AbstractFieldNode)((FieldTypeWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getTargetMember();
					if (readFieldNode != null && readFieldNode.isWorkspaceNode()) {
						FieldID readFieldId = readFieldNode.toFieldId();
						fieldReadAccesses.put(readFieldId, this, integerCache.valueOf(i));
					}
					break;
				case PUTSTATIC:
				case PUTFIELD:
					AbstractFieldNode writeFieldNode = (AbstractFieldNode)((FieldTypeWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getTargetMember();
					if (writeFieldNode != null && writeFieldNode.isWorkspaceNode()) {
						FieldID writeFieldId = writeFieldNode.toFieldId();
						fieldWriteAccesses.put(writeFieldId, this, integerCache.valueOf(i));
					}
					break;
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKESTATIC:
				case INVOKEINTERFACE:
					AbstractMethodNode methodNode = (AbstractMethodNode)((AbstractMethodCallWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getTargetMember();
					if (methodNode != null) {
						MethodID methodId = methodNode.toMethodId();
						methodAccesses.put(methodId, this, integerCache.valueOf(i));
					}
			}
		}
	}

	public void applyReflectionObfuscation(
		List<AbstractMethodNode> reflectedMethods,
		Map<String, AbstractMethodCallWriter> writersByName,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		LocalVariableOperand decryptionLocalVarOperand,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		String reason,
		boolean insertThisVar
	) {
		ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
		if (this.getMethodNode().isParameterObfuscationMarked()) {
			this.insertParameterUnpacking(instructionBlocks, decryptionLocalVarOperand, constantPoolManager, typeWriters, workspace, classProvider, insertThisVar);
		}

		if (reflectedMethods != null) {
			if (writersByName != null) {
				this.replaceCallsWithReflection(instructionBlocks, reflectedMethods, writersByName, constantPoolManager, typeWriters);
			} else {
				this.replaceCallsWithReflectionArray(instructionBlocks, reflectedMethods, constantPoolManager, typeWriters, workspace, classProvider);
			}
		}

		if (!instructionBlocks.isEmpty()) {
			this.applyInstructionBlockPatches(instructionBlocks, reason, false, false);
		}

		this.setMaxLocals(this.localVariableTable.getMaxSlotIndex() + 1);
	}

	public LocalVariableTable getLocalVariableTable() {
		return this.localVariableTable;
	}

	public void collectInnerClassMethodCalls(Set<AbstractMethodNode> collectedMethods, Set<AbstractClassNode> innerClasses) {
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode insn = this.instructions.get(i);
			switch (insn.getOpcode()) {
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKESTATIC:
				case INVOKEINTERFACE:
					AbstractMethodNode methodNode = (AbstractMethodNode)((AbstractMethodCallWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getTargetMember();
					if (methodNode != null) {
						AbstractClassNode methodClassNode = methodNode.getClassNode();
						if (methodClassNode.isInnerClass()) {
							methodClassNode = methodClassNode.getOuterClass();
						}

						if (innerClasses.contains(methodClassNode)) {
							methodNode.collectInnerClassMethodCalls(collectedMethods, innerClasses);
						}
					}
					break;
				case INVOKEDYNAMIC:
					AbstractMethodNode bootstrapMethodNode = ((InvokeDynamicCallWriter)((WideConstantPoolInsnNode)insn).getTypeWriter()).getResolvedBootstrapTarget();
					if (bootstrapMethodNode != null) {
						AbstractClassNode bootstrapClassNode = bootstrapMethodNode.getClassNode();
						if (bootstrapClassNode.isInnerClass()) {
							bootstrapClassNode = bootstrapClassNode.getOuterClass();
						}

						if (innerClasses.contains(bootstrapClassNode)) {
							bootstrapMethodNode.collectInnerClassMethodCalls(collectedMethods, innerClasses);
						}
					}
			}
		}
	}

	private boolean resolveReflectionParameters(
		ReflectionMethodPattern reflectionPattern,
		ReflectionMethodCallTracker callTracker,
		DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferences,
		MultiMap<String, Reference> stringResourcePathReferences,
		DualKeyMap<AbstractFieldNode, Reference, Reference> stringFieldNameReferences,
		DualKeyMap<AbstractMethodNode, Reference, Reference> stringMethodNameReferences,
		DualKeyMap<String, Reference, Reference> stringInternalNameReferences,
		AbstractWorkspace workspace,
		ClassRepository classRepository,
		Set<StringNode> processedStringNodes,
		Set<StructureNode> resolvedNodes,
		AtomicReference<String> errorMessage
	) {
		if (reflectionPattern.isObjectNameType()) {
			return true;
		} else if (reflectionPattern.isFunctionalInterfaceMethodType()) {
			if (callTracker.isResolved()) {
				boolean resolved = true;
				StringReference methodNameStringReference = (StringReference)callTracker.getParameterReferences(0).iterator().next();
				String methodDescriptor = ((StringReference)callTracker.getParameterReferences(1).iterator().next()).getValue();
				String classInternalName = ((StringReference)callTracker.getParameterReferences(2).iterator().next()).getValue();
				MethodSignature methodSignature = new MethodSignature(methodNameStringReference.getValue(), methodDescriptor);
				ClassNode declaringClassNode = ClassHierarchyNode.getWorkspaceClassNode(classInternalName);
				if (declaringClassNode != null) {
					MethodNode methodNode = declaringClassNode.findMethodNode(methodSignature);
					if (methodNode != null) {
						stringMethodNameReferences.put(methodNode, methodNameStringReference, methodNameStringReference);
						resolvedNodes.add(methodNode);
						processedStringNodes.add(methodNameStringReference.getStringNode());
					} else {
						errorMessage.set("'" + classInternalName + "' not found (A)");
						resolved = false;
					}
				} else {
					try {
						LibraryClassNode libraryClassNode = classRepository.loadClassOrThrow(classInternalName);
						if (libraryClassNode.findMethod(methodSignature) == null) {
							errorMessage.set("Method not found in unopened class");
							resolved = false;
						}
					} catch (FileException ignored) {
						errorMessage.set("'" + classInternalName + "' not found (B)");
						resolved = false;
					}
				}

				return resolved;
			} else {
				return false;
			}
		} else {
			int parameterSpecCount = reflectionPattern.getParameterSpecCount();
			int trackedParameterCount = callTracker.getTrackedParameterCount();
			List<ReflectionParameter> parameters = reflectionPattern.getParameters();

			for (int parameterIndex = 0; parameterIndex < trackedParameterCount; parameterIndex++) {
				ReflectionParameter parameterSpec;
				if (parameterIndex < parameterSpecCount) {
					parameterSpec = parameters.get(parameterIndex);
				} else {
					parameterSpec = null;
				}

				Set<Reference> parameterReferences = callTracker.getParameterReferences(parameterIndex);
				if ((parameterSpec == null || !parameterSpec.isClassParameter())
					&& (parameterSpec == null || !parameterSpec.isClassNameParameter())
					&& (parameterSpec == null || !parameterSpec.isClassOrPropertiesNameParameter())
					&& parameterSpec != null) {
					if (parameterSpec.isPackageNameParameter()) {
						for (Reference packageNameReference : parameterReferences) {
							if (packageNameReference.isResolved()) {
								if (packageNameReference instanceof ArrayReference) {
									ArrayList<Reference> packageNameReferences = ((ArrayReference)packageNameReference).getAllReferences();

									for (int packageNameIndex = 0; packageNameIndex < packageNameReferences.size(); packageNameIndex++) {
										Reference packageNameElementReference = packageNameReferences.get(packageNameIndex);
										this.putClassReference(packageNameElementReference.getTypeName(), packageNameElementReference, stringInternalNameReferences, processedStringNodes);
									}
								} else {
									String packageName = packageNameReference.getTypeName();
									this.putClassReference(packageName, packageNameReference, stringInternalNameReferences, processedStringNodes);
								}
							}
						}
					}
				} else {
					for (Reference classReference : parameterReferences) {
						if (classReference.isResolved()) {
							if (classReference instanceof ArrayReference) {
								ArrayList<Reference> classReferences = ((ArrayReference)classReference).getAllReferences();

								for (int classReferenceIndex = 0; classReferenceIndex < classReferences.size(); classReferenceIndex++) {
									Reference classElementReference = classReferences.get(classReferenceIndex);
									this.resolveClassReference(
										classElementReference.getValue(), parameterSpec != null && parameterSpec.isClassOrPropertiesNameParameter(), classElementReference, stringClassReferences, stringResourcePathReferences, processedStringNodes, resolvedNodes, classRepository, workspace
									);
								}
							} else {
								String className = classReference.getValue();
								this.resolveClassReference(className, parameterSpec != null && parameterSpec.isClassOrPropertiesNameParameter(), classReference, stringClassReferences, stringResourcePathReferences, processedStringNodes, resolvedNodes, classRepository, workspace);
							}
						}
					}
				}
			}

			if (!callTracker.isResolved()) {
				return false;
			} else {
				if (reflectionPattern.isFieldType()) {
					Set<Reference> fieldNameReferences = callTracker.getParameterReferences(ReflectionMethodPattern.getMemberNameParameterIndex(reflectionPattern));
					ReferenceGraph<String, Reference> fieldNameGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(fieldNameReferences.size()));

					for (Reference fieldNameReference : fieldNameReferences) {
						fieldNameGraph.addReference(fieldNameReference.getValue(), fieldNameReference);
					}

					Set<Reference> fieldTargetClassReferences = callTracker.getParameterReferences(ReflectionMethodPattern.getTargetClassParameterIndex(reflectionPattern));
					Set<Reference> fieldTypeClassReferences = null;
					if (ReflectionMethodPattern.getFieldTypeClassParamIndex(reflectionPattern) > -1) {
						fieldTypeClassReferences = callTracker.getParameterReferences(ReflectionMethodPattern.getFieldTypeClassParamIndex(reflectionPattern));
					} else if (ReflectionMethodPattern.isAtomicUpdaterPattern(reflectionPattern)) {
						fieldTypeClassReferences = GlobalUtils.newHashSet();
						fieldTypeClassReferences.add(new ConstructorReference(ReflectionMethodPattern.getAtomicUpdaterFieldType(reflectionPattern)));
					}

					ReferenceGraph<String, Reference> fieldTargetClassGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(fieldTargetClassReferences.size()));

					for (Reference fieldTargetClassReference : fieldTargetClassReferences) {
						fieldTargetClassGraph.addReference(fieldTargetClassReference.getTypeName(), fieldTargetClassReference);
					}

					if (fieldTargetClassGraph.size() == 0 || fieldNameGraph.size() == 0 || fieldTargetClassGraph.size() > 1 && fieldNameGraph.size() > 1) {
						errorMessage.set("UNRESOLVED : Field inadequately resolved (A)");
						return false;
					}

					boolean fieldsResolved = this.resolveReflectionFields(reflectionPattern, callTracker, fieldTargetClassReferences, fieldNameReferences, fieldTypeClassReferences, processedStringNodes, stringClassReferences, stringFieldNameReferences, resolvedNodes, errorMessage, classRepository, workspace);
					if (!fieldsResolved) {
						return fieldsResolved;
					}
				} else if (reflectionPattern.isMethodType()) {
					Set<Reference> methodTargetClassReferences = callTracker.getParameterReferences(2);
					ReferenceGraph<String, Reference> methodTargetClassGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(methodTargetClassReferences.size()));

					for (Reference methodTargetClassReference : methodTargetClassReferences) {
						methodTargetClassGraph.addReference(methodTargetClassReference.getTypeName(), methodTargetClassReference);
					}

					Set<Reference> methodNameReferences = callTracker.getParameterReferences(0);
					ReferenceGraph<String, Reference> methodNameGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(methodNameReferences.size()));

					for (Reference methodNameReference : methodNameReferences) {
						methodNameGraph.addReference(methodNameReference.getValue(), methodNameReference);
					}

					Set<Reference> parameterTypeReferences = callTracker.getParameterReferences(1);
					ReferenceGraph<String, Reference> parameterTypeGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(parameterTypeReferences.size()));

					for (Reference parameterTypeReference : parameterTypeReferences) {
						parameterTypeGraph.addReference(parameterTypeReference.getKey(), parameterTypeReference);
					}

					if (methodTargetClassGraph.size() == 0 || methodNameGraph.size() == 0 || parameterTypeGraph.size() == 0 || methodTargetClassGraph.size() > 1 && (methodNameGraph.size() > 1 || parameterTypeGraph.size() > 1)) {
						errorMessage.set("UNRESOLVED : Method inadequately resolved (A).");
						return false;
					}

					boolean methodsResolved = this.resolveReflectionMethods(reflectionPattern, callTracker, methodTargetClassReferences, methodNameReferences, parameterTypeReferences, false, processedStringNodes, stringClassReferences, stringMethodNameReferences, resolvedNodes, errorMessage, classRepository, workspace);
					if (!methodsResolved) {
						return methodsResolved;
					}
				} else if (reflectionPattern.isConstructorType()) {
					Set<Reference> constructorClassReferences = callTracker.getParameterReferences(callTracker.getTrackedParameterCount() - 1);
					if (reflectionPattern.isDefaultConstructorCallType()) {
						boolean defaultConstructorResolved = false;

						for (Reference defaultConstructorClassReference : constructorClassReferences) {
							if (!(defaultConstructorClassReference instanceof NullReference)) {
								String defaultConstructorInternalName = GlobalUtils.binaryToInternalName(defaultConstructorClassReference.getTypeName());
								ClassNode defaultConstructorClassNode = ClassHierarchyNode.getWorkspaceClassNode(defaultConstructorInternalName);
								if (defaultConstructorClassNode != null) {
									MethodParameterKey noArgConstructorKey = new MethodParameterKey("<init>", "()", "V");
									MethodNode[] constructorNodes = workspace.getMethodNodesByParameterKey(defaultConstructorClassNode, noArgConstructorKey);
									if (constructorNodes != null) {
										if (constructorNodes.length == 1) {
											resolvedNodes.add(constructorNodes[0]);
											defaultConstructorResolved = true;
										}
									} else if (defaultConstructorClassReference instanceof InstructionTypeReference) {
										HashSet<MethodParameterKey> hierarchyConstructorKeys = GlobalUtils.newHashSet(3);
										hierarchyConstructorKeys.add(noArgConstructorKey);
										defaultConstructorResolved = this.findConstructorsInHierarchy(defaultConstructorInternalName, hierarchyConstructorKeys, resolvedNodes, workspace);
									}
								}
							}
						}

						if (!defaultConstructorResolved) {
							errorMessage.set("UNRESOLVED : Constructor inadequately resolved (A).");
							return false;
						}
					} else if (reflectionPattern.isAllConstructorsCallType()) {
						for (Reference allConstructorsClassReference : constructorClassReferences) {
							if (!(allConstructorsClassReference instanceof NullReference)) {
								ClassNode allConstructorsClassNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(allConstructorsClassReference.getTypeName()));
								if (allConstructorsClassNode != null) {
									List<AbstractMethodNode> constructorMethodNodes = allConstructorsClassNode.findMethodsByName("<init>");

									for (int constructorIndex = 0; constructorIndex < constructorMethodNodes.size(); constructorIndex++) {
										MethodNode constructorNode = (MethodNode)constructorMethodNodes.get(constructorIndex);
										resolvedNodes.add(constructorNode);
									}
								}
							}
						}
					} else if (reflectionPattern.isSpecificConstructorCallType()) {
						Set<Reference> specificConstructorParameterReferences = callTracker.getParameterReferences(0);
						if (constructorClassReferences.size() != 1 && specificConstructorParameterReferences.size() != 1) {
							errorMessage.set("UNRESOLVED : Constructor inadequately resolved (C).");
							return false;
						}

						boolean specificConstructorResolved = false;

						for (Reference specificConstructorClassReference : constructorClassReferences) {
							if (!(specificConstructorClassReference instanceof NullReference)) {
								String specificConstructorInternalName = GlobalUtils.binaryToInternalName(specificConstructorClassReference.getTypeName());
								ClassNode specificConstructorClassNode = ClassHierarchyNode.getWorkspaceClassNode(specificConstructorInternalName);
								if (specificConstructorClassNode != null) {
									for (Reference specificConstructorParameterReference : specificConstructorParameterReferences) {
										Set<MethodParameterKey> constructorParameterKeys = this.createMethodParameterKeys("<init>", false, specificConstructorParameterReference);
										if (constructorParameterKeys != null) {
											MethodNode uniqueConstructor = null;

											for (MethodParameterKey constructorParameterKey : constructorParameterKeys) {
												MethodNode candidateConstructor = (MethodNode)AbstractMethodNode.resolveByCovariantReturn(workspace.getMethodNodesByParameterKey(specificConstructorClassNode, constructorParameterKey), workspace);
												if (candidateConstructor != null) {
													if (uniqueConstructor != null) {
														uniqueConstructor = null;
														break;
													}

													uniqueConstructor = candidateConstructor;
												}
											}

											if (uniqueConstructor != null) {
												specificConstructorResolved = true;
												resolvedNodes.add(uniqueConstructor);
											} else if (specificConstructorClassReference instanceof InstructionTypeReference) {
												specificConstructorResolved = this.findConstructorsInHierarchy(specificConstructorInternalName, constructorParameterKeys, resolvedNodes, workspace);
											}
										}
									}
								}
							}
						}

						if (!specificConstructorResolved) {
							errorMessage.set("UNRESOLVED : Constructor inadequately resolved (B).");
							return false;
						}
					}
				} else if (!reflectionPattern.isMethodTypeType() && reflectionPattern.isMethodHandleType()) {
					Set<Reference> methodHandleClassReferences = callTracker.getParameterReferences(0);
					ReferenceGraph<String, Reference> methodHandleClassGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(methodHandleClassReferences.size()));

					for (Reference methodHandleClassReference : methodHandleClassReferences) {
						methodHandleClassGraph.addReference(methodHandleClassReference.getTypeName(), methodHandleClassReference);
					}

					Set<Reference> memberNameReferences;
					Set<Reference> memberTypeReferences;
					if (reflectionPattern.isConstructorType()) {
						memberNameReferences = GlobalUtils.newHashSet(3);
						memberNameReferences.add(new ConstructorReference("<init>"));
						memberTypeReferences = callTracker.getParameterReferences(1);
					} else {
						memberNameReferences = callTracker.getParameterReferences(1);
						memberTypeReferences = callTracker.getParameterReferences(2);
					}

					ReferenceGraph<String, Reference> memberNameGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(memberNameReferences.size()));

					for (Reference memberNameReference : memberNameReferences) {
						memberNameGraph.addReference(memberNameReference.getValue(), memberNameReference);
					}

					ReferenceGraph<String, Reference> memberTypeGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(memberTypeReferences.size()));

					for (Reference memberTypeReference : memberTypeReferences) {
						memberTypeGraph.addReference(memberTypeReference.getKey(), memberTypeReference);
					}

					if (reflectionPattern.hasMethodNameParameter()) {
						if (methodHandleClassGraph.size() == 0 || memberNameGraph.size() == 0 || memberTypeGraph.size() == 0 || methodHandleClassGraph.size() > 1 && (memberNameGraph.size() > 1 || memberTypeGraph.size() > 1)) {
							errorMessage.set("UNRESOLVED : Method inadequately resolved (B).");
							return false;
						}

						boolean methodHandleMethodsResolved = this.resolveReflectionMethods(reflectionPattern, callTracker, methodHandleClassReferences, memberNameReferences, memberTypeReferences, true, processedStringNodes, stringClassReferences, stringMethodNameReferences, resolvedNodes, errorMessage, classRepository, workspace);
						if (!methodHandleMethodsResolved) {
							return methodHandleMethodsResolved;
						}
					} else if (reflectionPattern.hasFieldNameParameter()) {
						if (methodHandleClassGraph.size() == 0 || memberNameGraph.size() == 0 || methodHandleClassGraph.size() > 1 && memberNameGraph.size() > 1) {
							errorMessage.set("UNRESOLVED : Field inadequately resolved (B)");
							return false;
						}

						boolean methodHandleFieldsResolved = this.resolveReflectionFields(reflectionPattern, callTracker, methodHandleClassReferences, memberNameReferences, memberTypeReferences, processedStringNodes, stringClassReferences, stringFieldNameReferences, resolvedNodes, errorMessage, classRepository, workspace);
						if (!methodHandleFieldsResolved) {
							return methodHandleFieldsResolved;
						}
					}
				}

				return true;
			}
		}
	}

	private void obfuscateLongBitwise(Long xorKey, Long xorKeyOperand, ParameterPattern parameterPattern, List<InstructionBlock> instructionBlocks, List<TypeWriter> typeWriters, Random random) {
		int extraParameterCount = parameterPattern.getExtraParameterCount();
		VariableNode[] parameterVariables = parameterPattern.getParameterVariables(this);
		VariableKey[] variableKeys = new VariableKey[extraParameterCount];
		if (extraParameterCount == 1) {
			variableKeys[0] = new LongVariableKey(parameterVariables[0].getVariableSlot(), xorKey);
		} else {
			long obfuscatedValue;
			if (xorKeyOperand != null) {
				obfuscatedValue = xorKey ^ xorKeyOperand;
			} else {
				obfuscatedValue = xorKey;
			}

			for (int parameterIndex = 0; parameterIndex < extraParameterCount; parameterIndex++) {
				int cumulativeBitShift = parameterPattern.getCumulativeBitShift(parameterIndex);
				long parameterValue;
				if (cumulativeBitShift > 0) {
					parameterValue = obfuscatedValue << cumulativeBitShift;
				} else {
					parameterValue = obfuscatedValue;
				}

				int rightShiftAmount = parameterPattern.getRightShiftAmount(parameterIndex);
				parameterValue >>>= rightShiftAmount;
				if (parameterPattern.isLongExtraParameter(parameterIndex)) {
					variableKeys[parameterIndex] = new LongVariableKey(parameterVariables[parameterIndex].getVariableSlot(), parameterValue);
				} else {
					int narrowedValue = -1;
					switch (parameterPattern.getExtraParameterTypeChar(parameterIndex)) {
						case 'B':
							narrowedValue = (byte)parameterValue;
							break;
						case 'C':
							narrowedValue = (char)parameterValue;
							break;
						case 'I':
							narrowedValue = (int)parameterValue;
							break;
						case 'S':
							narrowedValue = (short)parameterValue;
					}

					variableKeys[parameterIndex] = new IntegerVariableKey(parameterVariables[parameterIndex].getVariableSlot(), narrowedValue);
				}
			}
		}

		for (InstructionBlock instructionBlock : instructionBlocks) {
			if (instructionBlock instanceof LabeledInstructionBlockPatch) {
				LabeledInstructionBlockPatch labeledBlock = (LabeledInstructionBlockPatch)instructionBlock;
				ArrayList<AbstractInsnNode> conditionInsns = new ArrayList<>();
				int randomParameterIndex = random.nextInt(extraParameterCount);
				VariableKey variableKey = variableKeys[randomParameterIndex];
				if (variableKey.hasLongValue()) {
					conditionInsns.add(AbstractInsnNode.loadLong(parameterVariables[randomParameterIndex].getVariableSlot()));
					conditionInsns.add(InsnNode.of(LCONST_0));
					conditionInsns.add(InsnNode.of(LCMP));
					long longKeyValue = ((LongVariableKey)variableKey).getLongValue();
					int compareResult = longKeyValue > 0L ? 1 : (longKeyValue == 0L ? 0 : -1);
					conditionInsns.add(this.createConditionalJump(labeledBlock.isFlagged(), compareResult, labeledBlock.getLabelNode(), random));
				} else {
					conditionInsns.add(AbstractInsnNode.loadInt(parameterVariables[randomParameterIndex].getVariableSlot()));
					conditionInsns.add(this.createConditionalJump(labeledBlock.isFlagged(), ((IntegerVariableKey)variableKey).getIntValue(), labeledBlock.getLabelNode(), random));
				}

				labeledBlock.prependInstructions(conditionInsns);
			}
		}
	}

	public MultiMap<AbstractMethodCallWriter, String> analyzeReflectionCalls(
		Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns,
		ClassRepository classRepository,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadAccesses,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteAccesses,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodAccesses,
		DualKeyMap<String, Reference, Reference> stringInternalNameReferences,
		TypeResolver typeResolver,
		Map<MethodID, MethodAnalysisResult> methodAnalysisResults,
		Map<FieldID, HashSet<Reference>> fieldReferences,
		Map<ExcludeElement, HashSet<Reference>> stackReferences,
		Map<ExcludeElement, HashSet<Reference>> localVariableReferences,
		AbstractWorkspace workspace,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferences,
		MultiMap<String, Reference> stringResourcePathReferences,
		DualKeyMap<AbstractFieldNode, Reference, Reference> stringFieldNameReferences,
		DualKeyMap<AbstractMethodNode, Reference, Reference> stringMethodNameReferences,
		Set<StringNode> processedStringNodes,
		List<StructureNode> resolvedNodes,
		List<MethodInvocation> methodInvocations,
		boolean verbose
	) {
		ArrayList<PriorityEntry<AbstractMethodCallWriter>> reflectionCallEntries = new ArrayList<>();
		HashMap<Integer, ReflectionMethodCallTracker> trackersByInstructionIndex = GlobalUtils.newHashMap();
		int instructionCount = this.instructions.size();

		for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
			AbstractInsnNode instruction = this.instructions.get(instructionIndex);
			if (instruction.isInvoke()) {
				AbstractMethodCallWriter invokeCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
				if (reflectionPatterns.containsKey(invokeCallWriter)) {
					reflectionCallEntries.add(new PriorityEntry<>(instructionIndex, invokeCallWriter));
					trackersByInstructionIndex.put(INTEGER_CACHE.valueOf(instructionIndex), null);
				}
			}
		}

		DualKeyMap<AbstractMethodCallWriter, PriorityEntry<AbstractMethodCallWriter>, String> messagesByCallAndEntry = new DualKeyMap<>(10, 10);
		if (reflectionCallEntries.size() > 0) {
			String processingErrorMessage = "";

			try {
				MethodAnalyzer methodAnalyzer = this.getOrCreateMethodAnalyzerFromCache(methodAnalyzerCache, typeResolver, workspace);

				for (PriorityEntry<AbstractMethodCallWriter> analysisEntry : reflectionCallEntries) {
					AbstractMethodCallWriter analyzedWriter = analysisEntry.getValue();
					ReflectionMethodPattern analyzedPattern = reflectionPatterns.get(analyzedWriter);
					ReflectionMethodCallTracker methodCallTracker = methodAnalyzer.analyzeReflectionMethodCall(
						analysisEntry.getPriority(), analyzedWriter, analyzedPattern, fieldReadAccesses, fieldWriteAccesses, methodAccesses, reflectionPatterns, methodAnalysisResults, fieldReferences, stackReferences, localVariableReferences, workspace, methodAnalyzerCache
					);
					trackersByInstructionIndex.put(INTEGER_CACHE.valueOf(analysisEntry.getPriority()), methodCallTracker);
				}
			} catch (StackOverflowError stackOverflowError) {
				throw new ZKMUnexpectedException("StackOverflowError trapped in '" + this.getParentString() + "' (2)", stackOverflowError);
			} catch (ProcessingException processingException) {
				processingErrorMessage = processingException.getMessage();
			}

			HashSet<StructureNode> resolvedStructureNodes = GlobalUtils.newHashSet(13);
			HashMap<AbstractInsnNode, MethodInvocation> invocationsByInstruction = GlobalUtils.newHashMap();

			for (PriorityEntry<AbstractMethodCallWriter> callEntry : reflectionCallEntries) {
				AbstractMethodCallWriter methodCallWriter = callEntry.getValue();
				ReflectionMethodPattern reflectionPattern = reflectionPatterns.get(methodCallWriter);
				ReflectionMethodCallTracker callTracker = trackersByInstructionIndex.get(INTEGER_CACHE.valueOf(callEntry.getPriority()));
				if (callTracker != null) {
					callTracker.resolveParameterReferences(trackersByInstructionIndex, INTEGER_CACHE, this);
					if (!callTracker.isResolved()) {
						if (reflectionPattern.isMemberAccessOrCreation()) {
							ArrayList<String> unresolvedTraceParameterTypes = new ArrayList<>();
							String unresolvedTrace = this.formatReflectionTrace(reflectionPattern, callTracker, "UNRESOLVED", unresolvedTraceParameterTypes);
							messagesByCallAndEntry.put(methodCallWriter, callEntry, unresolvedTrace);
							if (reflectionPattern.hasRewriteSnippet()) {
								WideConstantPoolInsnNode unresolvedInvokeInsn = (WideConstantPoolInsnNode)this.instructions.get(callEntry.getPriority());
								invocationsByInstruction.put(unresolvedInvokeInsn, new MethodInvocation(this, reflectionPattern, unresolvedInvokeInsn, this.getRawSourcePath(), this.getParentString(), unresolvedTraceParameterTypes, callTracker.getMemberName()));
							}
						}
					} else {
						if ((!callTracker.isPrivateCallChain() || Options.ZKM_FORCE_AUTOREFLECTION) && reflectionPattern.isMemberAccessOrCreation() && reflectionPattern.hasRewriteSnippet()) {
							ArrayList<String> resolvedTraceParameterTypes = new ArrayList<>();
							this.formatReflectionTrace(reflectionPattern, callTracker, "RESOLVED", resolvedTraceParameterTypes);
							WideConstantPoolInsnNode resolvedInvokeInsn = (WideConstantPoolInsnNode)this.instructions.get(callEntry.getPriority());
							invocationsByInstruction.put(resolvedInvokeInsn, new MethodInvocation(this, reflectionPattern, resolvedInvokeInsn, this.getRawSourcePath(), this.getParentString(), resolvedTraceParameterTypes, callTracker.getMemberName()));
						}

						if (verbose && reflectionPattern.isMemberAccessOrCreation()) {
							String resolvedTrace = this.formatResolvedReflection(reflectionPattern, callTracker, "RESOLVED");
							messagesByCallAndEntry.put(methodCallWriter, callEntry, resolvedTrace);
						}
					}

					AtomicReference<String> errorMessage = new AtomicReference<>();
					if (!this.resolveReflectionParameters(reflectionPattern, callTracker, stringClassReferences, stringResourcePathReferences, stringFieldNameReferences, stringMethodNameReferences, stringInternalNameReferences, workspace, classRepository, processedStringNodes, resolvedStructureNodes, errorMessage)
						&& reflectionPattern.isMemberAccessOrCreation()
						&& (!messagesByCallAndEntry.contains(methodCallWriter, callEntry) || messagesByCallAndEntry.get(methodCallWriter, callEntry).startsWith("RESOLVED"))) {
						ArrayList<String> fallbackTraceParameterTypes = new ArrayList<>();
						String fallbackTrace = this.formatReflectionTrace(reflectionPattern, callTracker, "UNRESOLVED", fallbackTraceParameterTypes);
						if (!errorMessage.isNull()) {
							fallbackTrace = fallbackTrace + " : " + errorMessage.get();
						}

						messagesByCallAndEntry.put(methodCallWriter, callEntry, fallbackTrace);
						if (reflectionPattern.hasRewriteSnippet()) {
							WideConstantPoolInsnNode fallbackInvokeInsn = (WideConstantPoolInsnNode)this.instructions.get(callEntry.getPriority());
							invocationsByInstruction.put(fallbackInvokeInsn, new MethodInvocation(this, reflectionPattern, fallbackInvokeInsn, this.getRawSourcePath(), this.getParentString(), fallbackTraceParameterTypes, callTracker.getMemberName()));
						}
					}
				} else if (reflectionPattern.isMemberAccessOrCreation()) {
					messagesByCallAndEntry.put(methodCallWriter, callEntry, "UNRESOLVED " + processingErrorMessage);
					if (reflectionPattern.hasRewriteSnippet()) {
						WideConstantPoolInsnNode untrackedInvokeInsn = (WideConstantPoolInsnNode)this.instructions.get(callEntry.getPriority());
						invocationsByInstruction.put(untrackedInvokeInsn, new MethodInvocation(this, reflectionPattern, untrackedInvokeInsn, this.getRawSourcePath(), this.getParentString(), null, null));
					}
				}
			}

			for (Entry<AbstractInsnNode, MethodInvocation> invocationEntry : invocationsByInstruction.entrySet()) {
				methodInvocations.add(invocationEntry.getValue());
			}

			if (resolvedStructureNodes.size() > 0) {
				for (StructureNode structureNode : resolvedStructureNodes) {
					resolvedNodes.add(structureNode);
					if (structureNode instanceof AbstractFieldNode) {
						AbstractClassNode fieldClassNode = ((AbstractFieldNode)structureNode).getClassNode();
						if (!resolvedStructureNodes.contains(fieldClassNode)) {
							resolvedNodes.add(fieldClassNode);
						}
					} else if (structureNode instanceof AbstractMethodNode) {
						AbstractClassNode methodClassNode = ((AbstractMethodNode)structureNode).getClassNode();
						if (!resolvedStructureNodes.contains(methodClassNode)) {
							resolvedNodes.add(methodClassNode);
						}
					}
				}
			}
		}

		MultiMap<AbstractMethodCallWriter, String> messagesByCall = new MultiMap<>();
		Enumeration<AbstractMethodCallWriter> callWriterEnumeration = messagesByCallAndEntry.getGroupKeys();

		while (callWriterEnumeration.hasMoreElements()) {
			AbstractMethodCallWriter callWriter = callWriterEnumeration.nextElement();

			for (String message : messagesByCallAndEntry.getGroup(callWriter).values()) {
				messagesByCall.put(callWriter, message);
			}
		}

		return messagesByCall;
	}

	private String formatResolvedReflection(ReflectionMethodPattern reflectionPattern, ReflectionMethodCallTracker callTracker, String status) {
		return this.formatReflectionTrace(reflectionPattern, callTracker, status, null);
	}

	@Override
	public String getRawSourcePath() {
		CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
		return codeAttribute != null ? codeAttribute.getRawSourcePath() : "<NO_PARENT_YET>";
	}

	private void putClassReference(String className, Reference reference, DualKeyMap<String, Reference, Reference> stringInternalNameReferences, Set<StringNode> processedStringNodes) {
		if (className != null && className.length() > 0 && reference instanceof StringReference) {
			className = GlobalUtils.binaryToInternalName(className);
			stringInternalNameReferences.put(className, reference, reference);
			if (reference instanceof StringReference) {
				processedStringNodes.add(((StringReference)reference).getStringNode());
			}
		}
	}

	public void findCommonStackFrame(
		AtomicInt anchorIndex,
		AtomicInt targetIndex,
		AtomicReference<AnalyzingNode> successorNodeRef,
		AnalyzingNode frameNode,
		AnalyzingNode successorNode,
		Set<StackFrameType> monitorStackTypes,
		FrameNode[] frames,
		TypeResolver typeResolver,
		ScriptContext scriptContext,
		boolean keepBalancedLocks,
		Random random,
		String analysisContext
	) {
		ArrayList<CommonStackFrameCandidate> candidates = new ArrayList<>();
		if (frameNode != successorNode) {
			int startIndex = frameNode.getStartIndex();
			if (this.instructions.get(startIndex).isLabel()) {
				startIndex++;
			}

			for (int instructionIndex = frameNode.getEndIndex() - 1; instructionIndex >= startIndex; instructionIndex--) {
				if (frames[instructionIndex].isFullyResolved()) {
					int compatibleFrameIndex = successorNode.findCompatibleFrameIndex(
						frames[instructionIndex], monitorStackTypes, frames, GlobalUtils.newHashSet(), successorNode, successorNode, null, new InsnList<>(this.instructions), typeResolver, scriptContext, successorNodeRef, keepBalancedLocks, analysisContext
					);
					if (compatibleFrameIndex != -1 && !this.instructions.get(instructionIndex).isLabel()) {
						candidates.add(new CommonStackFrameCandidate(instructionIndex, compatibleFrameIndex, successorNodeRef.get()));
						if (Options.dontRandomizeFlowObfuscation) {
							break;
						}
					}
				}
			}
		}

		this.selectCommonStackFrameCandidate(anchorIndex, targetIndex, successorNodeRef, candidates, random);
	}

	private void selectCommonStackFrameCandidate(
		AtomicInt anchorIndex, AtomicInt targetIndex, AtomicReference<AnalyzingNode> successorNodeRef, List<CommonStackFrameCandidate> candidates, Random random
	) {
		if (candidates.isEmpty()) {
			anchorIndex.set(-1);
			targetIndex.set(-1);
			successorNodeRef.set(null);
		} else {
			CommonStackFrameCandidate candidate;
			switch (candidates.size()) {
				case 1:
					candidate = candidates.get(0);
					break;
				case 2:
					candidate = candidates.get(1);
					break;
				default:
					candidate = candidates.get(random.nextInt(candidates.size() - 1) + 1);
			}

			anchorIndex.set(candidate.instructionIndex);
			targetIndex.set(candidate.frameIndex);
			successorNodeRef.set(candidate.analyzingNode);
		}
	}

	private String formatReflectionTrace(ReflectionMethodPattern reflectionPattern, ReflectionMethodCallTracker callTracker, String status, List<String> parameterTypes) {
		StringBuilder traceBuilder = new StringBuilder(32);
		traceBuilder.append(status);
		if (reflectionPattern.isFunctionalInterfaceMethodType()) {
			traceBuilder.append(' ');
			int trackedParameterCount = callTracker.getTrackedParameterCount();

			for (int i = 0; i < trackedParameterCount; i++) {
				traceBuilder.append(this.formatReflectionParameters(callTracker, i, false, parameterTypes));
			}
		} else {
			int parameterIndex = 0;

			for (ReflectionParameter parameter : reflectionPattern.getParameters()) {
				traceBuilder.append(
					" param_"
						+ parameter.getParameterIndex()
						+ "="
						+ this.formatReflectionParameters(callTracker, parameterIndex++, parameter.isClassParameter(), parameter.isTargetClass() ? parameterTypes : null)
				);
			}

			if (reflectionPattern.isInvokedOnClassInstance()) {
				traceBuilder.append(" referencedClass=" + this.formatReflectionParameters(callTracker, parameterIndex, true, parameterTypes));
			}
		}

		return traceBuilder.toString();
	}

	private boolean isMethodCallRemovable(int instructionIndex, AbstractInsnNode instruction, Set<AbstractMethodNode> reflectedMethods, AtomicInt popableCount) {
		AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
		AbstractMethodNode targetMethod = (AbstractMethodNode)methodCallWriter.getTargetMember();
		return targetMethod != null
			&& reflectedMethods.contains(targetMethod)
			&& methodCallWriter.getStackReturnType() != null
			&& !methodCallWriter.getOwnerName().equals("java/lang/StringBuffer")
			&& !methodCallWriter.getOwnerName().equals("java/lang/StringBuilder")
			&& this.isMethodCallPopable(instructionIndex - 1, targetMethod, popableCount);
	}

	public int getMinReturnStackDepth() {
		return this.minReturnStackDepth;
	}

	public void setMaxStack(int maxStack) {
		((CodeAttribute)this.getParent()).setMaxStack(maxStack);
	}

	public JumpInsnNode createConditionalJump(boolean flagged, int comparisonResult, LabelNode targetLabel, Random random) {
		JumpInsnNode jumpInsn;
		if (flagged) {
			if (comparisonResult >= 0) {
				if (comparisonResult != 0) {
					if (random.nextBoolean()) {
						jumpInsn = new JumpInsnNode(IFGE, targetLabel);
					} else {
						jumpInsn = new JumpInsnNode(IFGT, targetLabel);
					}
				} else {
					jumpInsn = new JumpInsnNode(IFGE, targetLabel);
				}
			} else if (random.nextBoolean()) {
				jumpInsn = new JumpInsnNode(IFLE, targetLabel);
			} else {
				jumpInsn = new JumpInsnNode(IFLT, targetLabel);
			}
		} else if (comparisonResult >= 0) {
			if (comparisonResult != 0) {
				if (random.nextBoolean()) {
					jumpInsn = new JumpInsnNode(IFLE, targetLabel);
				} else {
					jumpInsn = new JumpInsnNode(IFLT, targetLabel);
				}
			} else {
				jumpInsn = new JumpInsnNode(IFLT, targetLabel);
			}
		} else if (random.nextBoolean()) {
			jumpInsn = new JumpInsnNode(IFGE, targetLabel);
		} else {
			jumpInsn = new JumpInsnNode(IFGT, targetLabel);
		}

		return jumpInsn;
	}

	private void insertParameterUnpacking(
		List<InstructionBlock> instructionBlocks,
		LocalVariableOperand decryptionLocalVarOperand,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean shuffleLocals
	) {
		MethodNode methodNode = (MethodNode)this.getParent().getParent();
		List<String> parameterDescriptors = methodNode.getParameterDescriptors();
		boolean staticMethod = methodNode.isStatic();
		int firstParameterSlot = staticMethod ? 0 : 1;
		LocalVariableSlot parameterArraySlot = this.localVariableTable.insertVariableAt(firstParameterSlot, false, 10);
		this.localVariableTable.recalculateSlotIndices();
		if (!parameterDescriptors.isEmpty()) {
			int parameterCount = parameterDescriptors.size();
			int parameterEndSlot = parameterCount + firstParameterSlot;
			int shuffleStartSlot = firstParameterSlot + 1;
			int slotCursor = shuffleStartSlot;
			ArrayList<LocalVariableSlot> parameterSlots = new ArrayList<>(parameterCount);

			for (int parameterSlotIndex = firstParameterSlot; parameterSlotIndex < parameterEndSlot; parameterSlotIndex++) {
				LocalVariableSlot parameterSlot = this.localVariableTable.getVariable(slotCursor++);
				parameterSlots.add(parameterSlot);
				if (parameterSlot.isWideSlotStart()) {
					slotCursor++;
				}
			}

			int shuffleEndSlot = slotCursor - 1;
			ParameterUnpackingState[] parameterStates = new ParameterUnpackingState[parameterCount];

			for (int instructionIndex = 0; instructionIndex < this.instructions.size(); instructionIndex++) {
				AbstractInsnNode instruction = this.instructions.get(instructionIndex);
				if (instruction instanceof VarInsnNode) {
					VarInsnNode varInsn = (VarInsnNode)instruction;
					int parameterIndex = indexOfParameterSlot(parameterSlots, varInsn.getVariableIndex());
					if (parameterIndex > -1) {
						ParameterUnpackingState parameterState = parameterStates[parameterIndex];
						if (parameterState == null) {
							parameterState = new ParameterUnpackingState(this, parameterIndex, parameterDescriptors.get(parameterIndex));
							parameterStates[parameterIndex] = parameterState;
						}

						parameterState.addAccess(instructionIndex, varInsn.isVarStore());
					}
				}
			}

			if (decryptionLocalVarOperand != null) {
				int parameterIndex = indexOfParameterSlot(parameterSlots, decryptionLocalVarOperand.getIndex());
				if (parameterIndex > -1 && parameterStates[parameterIndex] == null) {
					parameterStates[parameterIndex] = new ParameterUnpackingState(this, parameterIndex, parameterDescriptors.get(parameterIndex));
				}
			}

			if (!Options.skipParameterUnpackingAnalysis) {
				this.analyzeParameterUnpackingTypes(parameterStates, methodNode);
			}

			VarInsnNode parameterArrayLoadInsn = new VarInsnNode(parameterArraySlot, LocalVarOperationType.OBJECT_LOAD);

			for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
				ParameterUnpackingState parameterState = parameterStates[parameterIndex];
				if (parameterState != null && !parameterState.needsHeadUnpacking()) {
					ArrayList<AbstractInsnNode> inlineUnpackingInsns = new ArrayList<>();
					inlineUnpackingInsns.add(parameterArrayLoadInsn);
					List<AbstractInsnNode> arrayLoadAndCastInsns = this.generateArrayLoadAndCast(
						parameterIndex, parameterState.getDescriptor(), false, parameterState.needsCast(), constantPoolManager, typeWriters, workspace, classProvider
					);
					inlineUnpackingInsns.addAll(arrayLoadAndCastInsns);
					instructionBlocks.add(new InstructionBlock(inlineUnpackingInsns, parameterState.getSingleAccessIndex() - 1, 1, 3));
				}
			}

			int unpackSlotCursor = shuffleStartSlot;
			ArrayList<List<AbstractInsnNode>> unpackingInsnsByParameter = new ArrayList<>(parameterCount);

			for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
				LocalVariableSlot parameterSlot = this.localVariableTable.getVariable(unpackSlotCursor);
				String parameterDescriptor = parameterDescriptors.get(parameterIndex);
				ParameterUnpackingState parameterState = parameterStates[parameterIndex];
				if (parameterState == null) {
					if (!methodNode.preservesUnusedParameterSlots() && !Options.useStaticBlockEligibility) {
						LocalVariableSlot removedSlot = this.localVariableTable.removeVariable(unpackSlotCursor);
						this.localVariableTable.recalculateSlotIndices();
						shuffleEndSlot--;
						if (removedSlot.isWideSlotStart()) {
							shuffleEndSlot--;
						}
					} else {
						unpackSlotCursor++;
						if (LocalVariableTable.isWideType(parameterDescriptor)) {
							unpackSlotCursor++;
						}
					}
				} else {
					if (parameterState.needsHeadUnpacking()) {
						List<AbstractInsnNode> parameterUnpackingInsns = this.generateArrayLoadAndCast(
							parameterIndex, parameterDescriptor, true, parameterState.needsCast(), constantPoolManager, typeWriters, workspace, classProvider
						);
						parameterUnpackingInsns.add(VarInsnNode.createByTypeDescriptor(parameterDescriptor, false, parameterSlot));
						unpackingInsnsByParameter.add(parameterUnpackingInsns);
					}

					unpackSlotCursor++;
					if (LocalVariableTable.isWideType(parameterDescriptor)) {
						unpackSlotCursor++;
					}
				}
			}

			if (shuffleLocals) {
				if (!Options.shuffleParameterBlocks) {
					Collections.shuffle(unpackingInsnsByParameter, GlobalUtils.newRandom(993));
				}

				if (!Options.dontShuffleLocalVariables && shuffleStartSlot < shuffleEndSlot) {
					this.localVariableTable.shuffleLocalVariables(shuffleStartSlot, shuffleEndSlot + 1, GlobalUtils.newRandom(994));
					methodNode.setParameterLocalsShuffled(true);
				}
			}

			if (!unpackingInsnsByParameter.isEmpty()) {
				ArrayList<AbstractInsnNode> headUnpackingInsns = new ArrayList<>();
				headUnpackingInsns.add(parameterArrayLoadInsn);

				for (List<AbstractInsnNode> parameterUnpackingInsns : unpackingInsnsByParameter) {
					headUnpackingInsns.addAll(parameterUnpackingInsns);
				}

				headUnpackingInsns.add(InsnNode.of(POP));
				InstructionBlock headUnpackingBlock = new InstructionBlock(headUnpackingInsns, -1, 0, 3);
				headUnpackingBlock.setPriority(-2);
				instructionBlocks.add(headUnpackingBlock);
			}
		}
	}

	private static int indexOfParameterSlot(List<LocalVariableSlot> parameterSlots, int localSlotIndex) {
		int parameterCount = parameterSlots.size();

		for (int i = 0; i < parameterCount; i++) {
			if (parameterSlots.get(i).getIndex() == localSlotIndex) {
				return i;
			}
		}

		return -1;
	}

	private void analyzeParameterUnpackingTypes(ParameterUnpackingState[] parameterStates, MethodNode methodNode) {
		for (int stateIndex = 0; stateIndex < parameterStates.length; stateIndex++) {
			ParameterUnpackingState parameterState = parameterStates[stateIndex];
			if (parameterState == null || !parameterState.hasSingleAccess() || parameterState.hasStoreAccess()) {
				continue;
			}

			int accessIndex = parameterState.getSingleAccessIndex();
			AbstractInsnNode accessInsn = this.instructions.get(accessIndex);
			if (accessInsn.getOpcode() == IINC) {
				parameterState.disableHeadUnpacking();
				continue;
			}

			int stackSize = accessInsn.getStackEffect();

			for (int instructionIndex = accessIndex + 1; instructionIndex < this.instructions.size(); instructionIndex++) {
				AbstractInsnNode instruction = this.instructions.get(instructionIndex);
				if (instruction.isPureStackOpcode()
					|| instruction.isJump()
					|| instruction.getOpcode() == INVOKEDYNAMIC
					|| instruction.isLabel() && (((LabelNode)instruction).hasFlag(1) || ((LabelNode)instruction).hasFlag(1024))
					|| instruction.isReturn()) {
					break;
				}

				stackSize += instruction.getStackEffect();
				if (!instruction.consumesStackFromIndex(0, stackSize)) {
					continue;
				}

				if (instruction.getOpcode() == CHECKCAST) {
					parameterState.disableHeadUnpacking();
					parameterState.clearNeedsCast();
					break;
				}

				if (instruction.isPutField()) {
					FieldTypeWriter fieldTypeWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
					parameterState.disableHeadUnpacking();
					if (fieldTypeWriter.getStackType().equals("Ljava/lang/Object;")) {
						parameterState.clearNeedsCast();
					}
					break;
				}

				if (!instruction.isInvoke()) {
					break;
				}

				AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
				AbstractMethodNode targetMethod = (AbstractMethodNode)methodCallWriter.getTargetMember();
				if (targetMethod == null) {
					break;
				}

				if (targetMethod.isParameterObfuscationMarked()) {
					if (ObfuscateParameterBuilder.supportsInterfaceCallPacking(this.getClassNode())) {
						parameterState.disableHeadUnpacking();
					}
					break;
				}

				parameterState.disableHeadUnpacking();
				int consumedSlotCount = 0;
				if (!targetMethod.isStatic()) {
					consumedSlotCount--;
				}

				String returnDescriptor = targetMethod.getReturnDescriptor();
				if (!returnDescriptor.equals("V")) {
					if (TypeWriter.isDoubleSlotType(returnDescriptor)) {
						consumedSlotCount += 2;
					} else {
						consumedSlotCount++;
					}
				}

				if (stackSize > consumedSlotCount) {
					break;
				}

				List<String> targetParameterDescriptors = targetMethod.getParameterDescriptors();
				int targetStackSize = stackSize;
				int targetParameterIndex = 0;
				targetParameterIndex++;
				String targetParameterDescriptor = targetParameterDescriptors.get(0);

				while (targetStackSize != consumedSlotCount) {
					if (TypeWriter.isDoubleSlotType(targetParameterDescriptor)) {
						targetStackSize += 2;
					} else {
						targetStackSize++;
					}

					targetParameterDescriptor = targetParameterDescriptors.get(targetParameterIndex++);
				}

				if (targetParameterDescriptor.equals("Ljava/lang/Object;")) {
					parameterState.clearNeedsCast();
				}
				break;
			}
		}
	}

	private int findFirstControlFlowLabel() {
		int controlFlowLabelIndex = -1;
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode instruction = this.instructions.get(i);
			if (instruction.isLabel()) {
				LabelNode labelNode = (LabelNode)instruction;
				if (labelNode.hasFlag(8192)) {
					controlFlowLabelIndex = i;
					break;
				}

				if (labelNode.hasFlag(256)) {
					break;
				}
			}
		}

		return controlFlowLabelIndex;
	}

	public MethodAnalyzer getOrCreateMethodAnalyzerFromCache(Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache, TypeResolver typeResolver, ClassHierarchy classHierarchy) {
		MethodAnalyzer methodAnalyzer = methodAnalyzerCache.get(this);
		if (methodAnalyzer != null) {
			return methodAnalyzer;
		} else {
			methodAnalyzer = this.getMethodAnalyzer(typeResolver, classHierarchy);
			methodAnalyzerCache.put(this, methodAnalyzer);
			return methodAnalyzer;
		}
	}

	private void insertDummyExceptionHandler(
		int startIndex, int handlerIndex, String exceptionType, boolean rethrowInHandler, List<ExceptionEntryNode> exceptionEntries, List<InstructionBlock> instructionBlocks, List<TypeWriter> typeWriters
	) {
		ClassInfoWriter exceptionClassInfo = ((ConstantPoolManager)this.getClassNode().getConstantPoolWriter()).getOrCreateClassInfo(exceptionType, typeWriters);
		LabelNode startLabel = new LabelNode(true, 128);
		LabelNode endLabel = new LabelNode(true, 128);
		LabelNode handlerLabel = new LabelNode(true, 128);
		ExceptionEntryNode exceptionEntry = new ExceptionEntryNode((CodeAttribute)this.getParent(), exceptionClassInfo, startLabel, endLabel, handlerLabel);
		exceptionEntries.add(exceptionEntry);
		int labelOffset = -1;
		AbstractInsnNode instruction = this.instructions.get(startIndex);

		while (instruction.isLabel()) {
			instruction = this.instructions.get(startIndex + ++labelOffset + 1);
		}

		InstructionBlock tryBlock = new InstructionBlock(startLabel, startIndex + labelOffset, 0, 0);
		ArrayList<AbstractInsnNode> handlerInsns = new ArrayList<>();
		if (rethrowInHandler) {
			handlerInsns.add(endLabel);
			LabelNode rethrowLabel = new LabelNode(true, 1);
			handlerInsns.add(new GotoNode(rethrowLabel));
			handlerInsns.add(handlerLabel);
			handlerInsns.add(InsnNode.of(ATHROW));
			handlerInsns.add(rethrowLabel);
		} else {
			handlerInsns.add(handlerLabel);
			handlerInsns.add(endLabel);
			handlerInsns.add(InsnNode.of(ATHROW));
		}

		InstructionBlock handlerBlock = new InstructionBlock(handlerInsns, handlerIndex, 0, 0);
		instructionBlocks.add(tryBlock);
		instructionBlocks.add(handlerBlock);
	}

	public int indexOfInstruction(AbstractInsnNode instruction) {
		return this.instructions.indexOf(instruction);
	}

	private void collectIndyDependenciesForRenaming(
		InvokeDynamicCallWriter indyCallWriter, Set<FieldNode> renamedFields, Set<MethodNode> renamedMethods, Set<ClassNode> signatureClasses, Set<ClassNode> renamedClasses
	) {
		indyCallWriter.collectWorkspaceDependencies(renamedClasses, signatureClasses, renamedFields, renamedMethods);
	}

	private String buildParameterDescriptor(String[] parameterTypes) {
		StringBuilder descriptorBuilder = new StringBuilder();
		int parameterCount = parameterTypes.length;
		descriptorBuilder.append("(");

		for (int i = 0; i < parameterCount; i++) {
			String parameterType = parameterTypes[i];
			if (parameterType != null) {
				parameterType.length();
				descriptorBuilder.append(MethodSignature.fromInternalName(parameterType));
			}
		}

		descriptorBuilder.append(")");
		return descriptorBuilder.toString();
	}

	private void pushIntConstant(int intValue, List<AbstractInsnNode> instructions, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, int constantCount) {
		if (Options.dontEncryptIntConstants || this.instructions.size() > 10000 && constantCount > 1000 && typeWriters.size() < 24000 && !Options.forceEncryptIntConstants) {
			AbstractInsnNode ldcInsn = AbstractInsnNode.ldcInt(intValue, constantPoolManager, typeWriters);
			instructions.add(ldcInsn);
		} else {
			AbstractInsnNode.encryptIntConstant(intValue, instructions, constantPoolManager, typeWriters);
		}
	}

	public boolean isModified() {
		return this.modified;
	}

	private int generateDecryptionCode(
		ChangedMethod changedMethod,
		ParameterPattern parameterPattern,
		long xorKey,
		Map<AbstractClassNode, ChangedMethod> changedMethodsByClass,
		AtomicReference<LocalVariableSlot> keyLocalSlotRef,
		List<AbstractInsnNode> instructions,
		Set<MethodNode> parameterChangedMethods,
		List<Integer> usedLocalSlots,
		List<TypeWriter> typeWriters,
		Condition condition,
		Random random,
		ConstantPoolManager constantPoolManager,
		Hierarchy hierarchy
	) {
		keyLocalSlotRef.setNull();
		byte additionalStackRequired = 0;
		if (parameterPattern != null && !parameterPattern.hasNoExtraParameters()) {
			ChangedMethod changedMethodForClass = changedMethodsByClass.get(this.getClassNode());
			VariableNode[] parameterVariables = parameterPattern.getParameterVariables(this);
			if (parameterPattern.hasMultipleExtraParameters()) {
				if (parameterPattern.isLongExtraParameter(0)) {
					instructions.add(AbstractInsnNode.loadLong(parameterVariables[0].getVariableSlot()));
				} else {
					instructions.add(AbstractInsnNode.loadInt(parameterVariables[0].getVariableSlot()));
					instructions.add(InsnNode.of(I2L));
				}

				int leftShiftAmount = parameterPattern.getLeftShiftAmount(0);
				instructions.add(AbstractInsnNode.newInt(leftShiftAmount));
				instructions.add(InsnNode.of(LSHL));

				for (int parameterIndex = 1; parameterIndex < parameterVariables.length; parameterIndex++) {
					if (parameterPattern.isLongExtraParameter(parameterIndex)) {
						instructions.add(AbstractInsnNode.loadLong(parameterVariables[parameterIndex].getVariableSlot()));
					} else {
						instructions.add(AbstractInsnNode.loadInt(parameterVariables[parameterIndex].getVariableSlot()));
						instructions.add(InsnNode.of(I2L));
					}

					leftShiftAmount = parameterPattern.getLeftShiftAmount(parameterIndex);
					instructions.add(AbstractInsnNode.newInt(leftShiftAmount));
					instructions.add(InsnNode.of(LSHL));
					int precedingBitShift = parameterPattern.getPrecedingBitShift(parameterIndex);
					instructions.add(AbstractInsnNode.newInt(precedingBitShift));
					instructions.add(InsnNode.of(LUSHR));
					instructions.add(InsnNode.of(LOR));
				}

				int keySlotMultiParam = this.getMaxLocals();
				if (isMethodParameterChanged(this.getMethodNode(), changedMethodForClass, hierarchy)) {
					parameterChangedMethods.add((MethodNode)this.getMethodNode());
					FieldTypeWriter multiParamSaltFieldRef = changedMethodForClass.getSaltFieldRef();
					instructions.add(new WideConstantPoolInsnNode(GETSTATIC, multiParamSaltFieldRef));
					instructions.add(InsnNode.of(LXOR));
				}

				instructions.add(AbstractInsnNode.storeLong(keySlotMultiParam, this.localVariableTable, 5));
				additionalStackRequired = 5;
				usedLocalSlots.add(INTEGER_CACHE.valueOf(keySlotMultiParam));
				usedLocalSlots.add(INTEGER_CACHE.valueOf(keySlotMultiParam + 1));
				keyLocalSlotRef.set(this.localVariableTable.getPenultimateVariable());
				this.setMaxLocals(keySlotMultiParam + 2);
			} else if (isMethodParameterChanged(this.getMethodNode(), changedMethodForClass, hierarchy)) {
				parameterChangedMethods.add((MethodNode)this.getMethodNode());
				FieldTypeWriter singleParamSaltFieldRef = changedMethodForClass.getSaltFieldRef();
				instructions.add(new WideConstantPoolInsnNode(GETSTATIC, singleParamSaltFieldRef));
				instructions.add(AbstractInsnNode.loadLong(parameterVariables[0].getVariableSlot()));
				instructions.add(InsnNode.of(LXOR));
				instructions.add(AbstractInsnNode.storeLong(parameterVariables[0].getVariableSlot()));
				keyLocalSlotRef.set(parameterVariables[0].getVariableSlot());
			} else {
				keyLocalSlotRef.set(parameterVariables[0].getVariableSlot());
			}
		} else {
			if (changedMethod != null) {
				FieldTypeWriter saltFieldRef = changedMethod.getSaltFieldRef();
				long methodSalt = changedMethod.getMethodSalt();
				long combinedKey = xorKey ^ methodSalt;
				if (saltFieldRef != null) {
					instructions.add(new WideConstantPoolInsnNode(GETSTATIC, saltFieldRef));
				}

				instructions.add(AbstractInsnNode.ldcLong(combinedKey, constantPoolManager, typeWriters));
				instructions.add(InsnNode.of(LXOR));
			} else {
				instructions.add(AbstractInsnNode.ldcLong(xorKey, constantPoolManager, typeWriters));
			}

			int keySlot = this.getMaxLocals();
			instructions.add(AbstractInsnNode.storeLong(keySlot, this.localVariableTable, 5));
			additionalStackRequired = 1;
			usedLocalSlots.add(INTEGER_CACHE.valueOf(keySlot));
			usedLocalSlots.add(INTEGER_CACHE.valueOf(keySlot + 1));
			keyLocalSlotRef.set(this.localVariableTable.getVariable(keySlot));
			this.setMaxLocals(keySlot + 2);
		}

		return additionalStackRequired;
	}

	public void writeInstructions(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeInt(this.getByteCodeLength());

		for (int i = 0; i < this.instructions.size(); i++) {
			this.instructions.get(i).writeToStream(dataOutputStream);
		}
	}

	public void updateClassMetrics(ClassMetrics classMetrics) {
		int instructionCount = 0;
		StringBuilder opcodeBuffer = new StringBuilder(this.instructions.size());

		for (AbstractInsnNode instruction : this.instructions) {
			if (!instruction.isLabel()) {
				instructionCount++;
				opcodeBuffer.append((char)instruction.getOpcode());
			}
		}

		classMetrics.incrementCodeBlockCount();
		classMetrics.addCodeHash(opcodeBuffer.toString().hashCode());
		classMetrics.addInstructionCount(instructionCount);
	}

	public void encryptStringConstantsInPlace(
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries, MethodNode methodNode, Set<StringNode> excludedStrings
	) {
		if (this.stringConstantCount != 0) {
			int instructionCount = this.instructions.size();

			for (int i = 0; i < instructionCount; i++) {
				this.instructions.get(i).collectStringObfCandidate(stringConstantEntries, excludedStrings, methodNode, i);
			}
		}
	}

	public void writeInstructionsRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> typeWriterMapping) throws IOException {
		dataOutputStream.writeInt(this.getByteCodeLength());

		for (int i = 0; i < this.instructions.size(); i++) {
			this.instructions.get(i).writeWithConstantMap(dataOutputStream, typeWriterMapping);
		}
	}

	private boolean isWithinExceptionRange(int instructionIndex, int[][] exceptionRanges) {
		for (int[] exceptionRange : exceptionRanges) {
			if (instructionIndex >= exceptionRange[0] && instructionIndex < exceptionRange[1]) {
				return true;
			}
		}

		return false;
	}

	private List<AbstractInsnNode> generateArrayLoadAndCast(
		int parameterIndex,
		String parameterType,
		boolean duplicateArrayReference,
		boolean castToParameterType,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> insns = new ArrayList<>(6);
		if (duplicateArrayReference) {
			insns.add(InsnNode.of(DUP));
		}

		insns.add(AbstractInsnNode.newInt(parameterIndex));
		insns.add(InsnNode.of(AALOAD));
		if (castToParameterType) {
			if (parameterType.length() == 1) {
				String wrapperName = TypeWriter.getStackWrapperName(parameterType);
				ClassInfoWriter wrapperClassInfo = constantPoolManager.getOrCreateClassInfo(wrapperName, typeWriters);
				insns.add(new WideConstantPoolInsnNode(CHECKCAST, wrapperClassInfo));
				MethodCallWriter unboxingMethod = constantPoolManager.getOrCreateUnboxingMethod(parameterType, typeWriters, workspace, classProvider);
				insns.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, unboxingMethod));
			} else {
				String className;
				if (parameterType.startsWith("L") && parameterType.endsWith(";")) {
					className = TypeWriter.getClassInternalName(parameterType);
				} else {
					className = parameterType;
				}

				ClassInfoWriter classInfo = constantPoolManager.getOrCreateClassInfo(className, typeWriters);
				insns.add(new WideConstantPoolInsnNode(CHECKCAST, classInfo));
			}
		}

		return insns;
	}

	public void applySingleInstructionBlock(InstructionBlock instructionBlock, String reason) {
		ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>(1);
		instructionBlocks.add(instructionBlock);
		this.applyInstructionBlockPatches(instructionBlocks, reason, false, true);
	}

	private void replaceWithInvokeDynamic(
		int instructionIndex,
		LongNode longConstant,
		Long xorKey,
		AbstractInsnNode keyLoadInsn,
		Long memberKey,
		MemberContext memberContext,
		String descriptor,
		int helperIndex,
		AtomicReference<BootstrapMethodsAttribute> bootstrapMethodsRef,
		AbstractMethodCallWriter indyHelperWriter,
		Map<String, InvokeDynamicCallWriter> indyWritersByName,
		List<InstructionBlock> instructionBlocks,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		BootstrapMethodsAttribute bootstrapMethods;
		if (bootstrapMethodsRef.isNull()) {
			bootstrapMethods = ((ClassNode)this.getClassNode()).getOrCreateBootstrapMethods(typeWriters);
		} else {
			bootstrapMethods = bootstrapMethodsRef.get();
		}

		ArrayList<AbstractInsnNode> indyInsns = new ArrayList<>(keyLoadInsn == null ? 2 : 3);
		indyInsns.add(new WideConstantPoolInsnNode(LDC2_W, longConstant));
		if (keyLoadInsn != null) {
			indyInsns.add(keyLoadInsn);
			if (Options.xorKeyWithLowBits) {
				helperIndex ^= memberKey.intValue() & 7;
			}
		}

		InvokeDynamicCallWriter indyWriter = this.getOrCreateIndyWriter(descriptor, helperIndex, indyHelperWriter, bootstrapMethods, indyWritersByName, typeWriters, constantPoolManager);
		indyInsns.add(new InvokeDynamicNode(indyWriter));
		InstructionBlock indyBlock = new InstructionBlock(indyInsns, instructionIndex - 1, 1, 2);
		instructionBlocks.add(indyBlock);
	}

	public boolean recalculateOffsets() {
		boolean offsetsChanged = false;

		boolean expansionOccurred;
		do {
			expansionOccurred = false;
			int bytecodeOffset = 0;
			int instructionCount = this.instructions.size();

			for (int i = 0; i < instructionCount; i++) {
				AbstractInsnNode instruction = this.instructions.get(i);
				instruction.setOffset(bytecodeOffset);
				if (instruction instanceof LabelNode) {
					((LabelNode)instruction).setLabelID(i);
				}

				bytecodeOffset += instruction.getBytecodeSize();
			}

			this.bytecodeLength = bytecodeOffset;
			int currentInstructionCount = this.instructions.size();

			for (int instructionIndex = 0; instructionIndex < currentInstructionCount; instructionIndex++) {
				List<AbstractInsnNode> expandedInsns = this.instructions.get(instructionIndex).expandToWideIfNeeded();
				if (expandedInsns != null) {
					for (int expandedIndex = 0; expandedIndex < expandedInsns.size(); expandedIndex++) {
						AbstractInsnNode expandedInsn = expandedInsns.get(expandedIndex);
						if (expandedIndex == 0) {
							this.instructions.set(instructionIndex, expandedInsn);
						} else {
							this.instructions.add(++instructionIndex, expandedInsn);
							currentInstructionCount = this.instructions.size();
						}
					}

					expansionOccurred = true;
					offsetsChanged = true;
				}
			}
		} while (expansionOccurred);

		if (offsetsChanged) {
			this.setModified(true);
		}

		return offsetsChanged;
	}

	public void printInstructions(PrintWriter writer, int indent) {
		StringBuilder indentBuilder = new StringBuilder(indent);

		for (int i = 0; i < indent; i++) {
			indentBuilder.append("   ");
		}

		int totalBytecodeSize = 0;

		for (int instructionIndex = 0; instructionIndex < this.instructions.size(); instructionIndex++) {
			AbstractInsnNode instruction = this.instructions.get(instructionIndex);

			try {
				instruction.printInsn(writer, indentBuilder);
			} catch (UnknownOpcodeException unknownOpcodeException) {
				throw new ProcessingException(unknownOpcodeException.getMessage());
			}

			totalBytecodeSize += instruction.getBytecodeSize();
		}
	}

	private String findCoveringExceptionType(
		int startOffset,
		int endOffset,
		ClassHierarchy classHierarchy,
		List<String> exceptionTypes,
		List<ClassInfoWriter> configuredExceptionClasses,
		List<ClassInfoWriter> runtimeExceptionClasses,
		List<ExceptionEntryNode> exceptionEntries,
		boolean useConfiguredTypes,
		PhantomClassManager phantomClassManager
	) {
		for (ExceptionEntryNode overlappingEntry : exceptionEntries) {
			if (overlappingEntry.partiallyOverlapsRange(startOffset, endOffset) || startOffset < overlappingEntry.getOffset(0) && endOffset == overlappingEntry.getOffset(1)) {
				return null;
			}
		}

		label114:
		for (ExceptionEntryNode targetExceptionEntry : exceptionEntries) {
			ClassInfoWriter targetException = targetExceptionEntry.getTargetException();
			if (targetException != null) {
				for (ExceptionEntryNode coveringEntry : exceptionEntries) {
					String targetExceptionName = targetExceptionEntry.getExceptionName();
					String coveringExceptionName = coveringEntry.getExceptionName();
					if (coveringEntry.isInsideRange(startOffset, endOffset) && this.isSubclassOrSuperclass(targetExceptionName, coveringExceptionName, phantomClassManager, classHierarchy)) {
						continue label114;
					}
				}

				return targetException.getInternalName();
			}
		}

		if (runtimeExceptionClasses != null) {
			label97:
			for (ClassInfoWriter declaredExceptionClass : runtimeExceptionClasses) {
				String declaredExceptionName = declaredExceptionClass.getInternalName();

				for (ExceptionEntryNode declaredHandlerEntry : exceptionEntries) {
					if (declaredHandlerEntry.isInsideRange(startOffset, endOffset)) {
						String declaredHandlerName = declaredHandlerEntry.getExceptionName();
						if (this.isSubclassOrSuperclass(declaredExceptionName, declaredHandlerName, phantomClassManager, classHierarchy)) {
							continue label97;
						}
					}
				}

				return declaredExceptionClass.getInternalName();
			}
		}

		if (configuredExceptionClasses != null) {
			label83:
			for (ClassInfoWriter runtimeExceptionClass : configuredExceptionClasses) {
				String runtimeExceptionName = runtimeExceptionClass.getInternalName();

				for (ExceptionEntryNode runtimeHandlerEntry : exceptionEntries) {
					if (runtimeHandlerEntry.isInsideRange(startOffset, endOffset)) {
						String runtimeHandlerName = runtimeHandlerEntry.getExceptionName();
						if (this.isSubclassOrSuperclass(runtimeExceptionName, runtimeHandlerName, phantomClassManager, classHierarchy)) {
							continue label83;
						}
					}
				}

				return runtimeExceptionClass.getInternalName();
			}
		}

		if (useConfiguredTypes && exceptionTypes != null) {
			label68:
			for (String exceptionType : exceptionTypes) {
				for (ExceptionEntryNode typeHandlerEntry : exceptionEntries) {
					if (typeHandlerEntry.isInsideRange(startOffset, endOffset)) {
						String typeHandlerName = typeHandlerEntry.getExceptionName();
						if (this.isSubclassOrSuperclass(exceptionType, typeHandlerName, phantomClassManager, classHierarchy)) {
							continue label68;
						}
					}
				}

				return exceptionType;
			}
		}

		return null;
	}

	private void collectMethodDependenciesForHierarchy(
		AbstractMethodCallWriter methodCallWriter, Set<AbstractMethodNode> methods, Set<AbstractClassNode> dependencyClasses, Set<AbstractClassNode> unused
	) {
		String ownerName = methodCallWriter.getOwnerName();
		AbstractClassNode ownerClassNode = null;
		if (!ownerName.startsWith("[")) {
			ownerClassNode = ClassHierarchyNode.getClassNodeByInternalName(ownerName);
			if (ownerClassNode != null) {
				if (ownerClassNode.hasInnerClassInfo()) {
					dependencyClasses.addAll(ownerClassNode.getSelfAndAllInnerClasses());
				} else {
					dependencyClasses.add(ownerClassNode);
				}
			}
		}

		AbstractMethodNode targetMethod = (AbstractMethodNode)methodCallWriter.getTargetMember();
		if (targetMethod != null && (targetMethod.isWorkspaceNode() || !ClassHierarchyNode.isMissingClass(targetMethod.getInternalName()))) {
			methods.add(targetMethod);
			AbstractClassNode targetClassNode = targetMethod.getClassNode();
			if ((ownerClassNode == null || targetClassNode != ownerClassNode) && !ClassHierarchyNode.isMissingClass(targetClassNode.getInternalName())) {
				if (targetClassNode.hasInnerClassInfo()) {
					dependencyClasses.addAll(targetClassNode.getSelfAndAllInnerClasses());
				} else {
					dependencyClasses.add(targetClassNode);
				}
			}
		}
	}

	public boolean isClinit() {
		return ((CodeAttribute)this.getParent()).isClinit();
	}

	public void obfuscateMethodParameters(
		ChangedMethod changedMethod,
		Map<AbstractClassNode, ChangedMethod> changedMethodsByClass,
		Map<MethodNode, ParameterPattern> parameterPatterns,
		Map<MethodNode, Long> keyOperands,
		boolean isParameterTarget,
		Map<MethodNode, LocalVariableOperand> keyLocalSlots,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		List<TypeWriter> typeWriters,
		Condition condition,
		Random random,
		ConstantPoolManager constantPoolManager,
		Hierarchy hierarchy,
		Set<MethodNode> parameterChangedMethods,
		ScriptContext scriptContext
	) {
		MethodNode methodNode = (MethodNode)this.getMethodNode();
		ParameterPattern parameterPattern = parameterPatterns.get(methodNode);
		Long keyOperand = keyOperands.get(methodNode);
		if (keyOperand == null) {
			if (changedMethod != null && changedMethod.getMethodNode() == methodNode) {
				ArrayList<AbstractInsnNode> decryptionInstructions = new ArrayList<>(changedMethod.getDecryptionInstructions());
				LabelNode markerLabel = new LabelNode(8192);
				decryptionInstructions.add(markerLabel);
				ArrayList<Integer> newVariableSlots = new ArrayList<>();
				this.setMaxStack(changedMethod.getMaxStack());
				this.insertInstructionsAtOffset(decryptionInstructions, 0, "Method Parameter List Changing", newVariableSlots);
			}
		} else {
			long keyValue = keyOperand;
			CodeAttribute codeAttribute = this.getCodeAttribute();
			boolean parametersModified = false;
			if (parameterPattern != null) {
				int[] extraParameterSlots = ParameterPattern.computeExtraParameterSlots(parameterPattern, this.isStaticMethod());
				if (extraParameterSlots.length > 0) {
					for (int extraParameterIndex = 0; extraParameterIndex < extraParameterSlots.length; extraParameterIndex++) {
						LocalVariableSlot extraParameterSlot = this.localVariableTable.insertVariableAt(extraParameterSlots[extraParameterIndex], parameterPattern.isLongExtraParameter(extraParameterIndex), 5);
						parameterPattern.setParameterVariable(this, extraParameterIndex, extraParameterSlot);
					}

					int localSlotCount = this.localVariableTable.recalculateSlotIndices();
					codeAttribute.setMaxLocals(localSlotCount + 1);
					parametersModified = true;
				}
			}

			ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
			ArrayList<Integer> movedVariableSlots = new ArrayList<>();
			LocalVariableSlot keySlot = this.prepareParameterObfuscation(
				changedMethod, parameterPattern, keyValue, isParameterTarget, changedMethodsByClass, parameterPatterns, keyOperands, movedVariableSlots, instructionBlocks, typeResolver, classHierarchy, typeWriters, condition, random, constantPoolManager, hierarchy, parameterChangedMethods, scriptContext
			);
			if (!instructionBlocks.isEmpty()) {
				Collections.sort(instructionBlocks);
				boolean offsetRecalculationDeferred = !movedVariableSlots.isEmpty();
				this.applyInstructionPatches(instructionBlocks, "Method Parameter List Changing", offsetRecalculationDeferred);
				parametersModified = true;
			}

			if (!movedVariableSlots.isEmpty() && Options.ZKM_ADJUST_LOCALS) {
				this.moveTailVariables(movedVariableSlots);
				parametersModified = true;
			}

			if (keySlot != null) {
				keyLocalSlots.put(methodNode, keySlot);
			}

			if (parametersModified) {
				this.recalculateOffsets();
				this.setModified(true);
			}
		}
	}

	public TypeWriter getFirstLdcConstant() {
		AbstractInsnNode firstInstruction = this.instructions.get(0);
		switch (firstInstruction.getOpcode()) {
			case LDC:
				return ((LdcNode)firstInstruction).getTypeWriter();
			case LDC_W:
			case LDC2_W:
				return ((WideConstantPoolInsnNode)firstInstruction).getTypeWriter();
			default:
				return null;
		}
	}

	public void insertDummyBranches(
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		List<String> exceptionTypes,
		List<ClassInfoWriter> runtimeExceptionClasses,
		List<ClassInfoWriter> configuredExceptionClasses,
		List<ExceptionEntryNode> exceptionEntries,
		List<TypeWriter> typeWriters,
		List<ExceptionEntryNode> addedExceptionEntries,
		List<InstructionBlock> instructionBlocks,
		boolean exceptionObfuscation,
		boolean avoidMonitors,
		PhantomClassManager phantomClassManager
	) {
		MethodAnalyzer methodAnalyzer;
		try {
			methodAnalyzer = this.getMethodAnalyzer(typeResolver, classHierarchy);
		} catch (StackOverflowError stackOverflowError) {
			throw new ZKMUnexpectedException("StackOverflowError trapped in '" + this.getParentString() + "' (5)", stackOverflowError);
		}

		int firstControlFlowLabel = this.findFirstControlFlowLabel();
		int[] instructionOffsets = this.getInstructionOffsets();
		List<ControlFlowNode> controlFlowNodes = methodAnalyzer.getControlFlowNodes();
		HashMap<AnalyzingNode, AnalyzingNode> processedNodes = GlobalUtils.newHashMap();

		for (int i = 0; i < controlFlowNodes.size(); i++) {
			ControlFlowNode controlFlowNode = controlFlowNodes.get(i);
			AnalyzingNode anchorNode = controlFlowNode.getAnchorNode();
			AnalyzingNode firstSourceNode = controlFlowNode.getFirstSourceNode();
			AnalyzingNode alternateNode = controlFlowNode.getAlternateNode();
			AnalyzingNode fallThroughNode = controlFlowNode.getFallThroughNode();
			processedNodes.put(anchorNode, anchorNode);
			processedNodes.put(firstSourceNode, firstSourceNode);
			if (fallThroughNode != null && alternateNode != null && anchorNode.getEndIndex() == fallThroughNode.getStartIndex() - 1 && fallThroughNode.getEndIndex() < this.instructions.size() - 1) {
				int anchorStartIndex = anchorNode.getStartIndex();
				int fallThroughEndIndex = fallThroughNode.getEndIndex();
				AtomicInt tryBlockEndHolder = new AtomicInt(fallThroughEndIndex);
				int tryBlockStartOffset = this.findDummyTryBlockStart(anchorStartIndex, fallThroughEndIndex, tryBlockEndHolder, firstControlFlowLabel, avoidMonitors, methodAnalyzer);
				if (tryBlockStartOffset > -1) {
					int tryBlockEndOffset = tryBlockEndHolder.get();
					if (tryBlockStartOffset <= anchorNode.getEndIndex() && tryBlockEndOffset >= fallThroughNode.getStartIndex()) {
						String coveringExceptionType = this.findCoveringExceptionType(instructionOffsets[tryBlockStartOffset], instructionOffsets[tryBlockEndOffset + 1], classHierarchy, exceptionTypes, runtimeExceptionClasses, configuredExceptionClasses, exceptionEntries, exceptionObfuscation, phantomClassManager);
						if (coveringExceptionType != null) {
							this.insertDummyExceptionHandler(tryBlockStartOffset, tryBlockEndOffset, coveringExceptionType, fallThroughNode.endsWithFallThroughOrJsr(new InsnList<>(this.instructions)), addedExceptionEntries, instructionBlocks, typeWriters);
						}
					}
				}
			}
		}

		List<AnalyzingNode> analyzingNodes = methodAnalyzer.getAnalyzingNodes();
		Collections.sort(analyzingNodes);

		for (AnalyzingNode analyzingNode : analyzingNodes) {
			if (!processedNodes.containsKey(analyzingNode)) {
				List<AnalyzingNode> successors = analyzingNode.getSuccessors();
				if (successors != null && successors.size() > 1) {
					Collections.sort(successors);
					AnalyzingNode firstSuccessor = successors.get(0);
					if (analyzingNode.getEndIndex() == firstSuccessor.getStartIndex() - 1 && firstSuccessor.getEndIndex() < this.instructions.size() - 1) {
						int nodeStartIndex = analyzingNode.getStartIndex();
						int successorEndIndex = firstSuccessor.getEndIndex();
						AtomicInt successorTryBlockEndHolder = new AtomicInt(successorEndIndex);
						int successorTryBlockStartOffset = this.findDummyTryBlockStart(nodeStartIndex, successorEndIndex, successorTryBlockEndHolder, firstControlFlowLabel, avoidMonitors, methodAnalyzer);
						if (successorTryBlockStartOffset > -1) {
							int successorTryBlockEndOffset = successorTryBlockEndHolder.get();
							if (successorTryBlockStartOffset <= analyzingNode.getEndIndex() && successorTryBlockEndOffset >= firstSuccessor.getStartIndex()) {
								String successorCoveringExceptionType = this.findCoveringExceptionType(instructionOffsets[successorTryBlockStartOffset], instructionOffsets[successorTryBlockEndOffset + 1], classHierarchy, exceptionTypes, runtimeExceptionClasses, configuredExceptionClasses, exceptionEntries, exceptionObfuscation, phantomClassManager);
								if (successorCoveringExceptionType != null) {
									this.insertDummyExceptionHandler(successorTryBlockStartOffset, successorTryBlockEndOffset, successorCoveringExceptionType, firstSuccessor.endsWithFallThroughOrJsr(new InsnList<>(this.instructions)), addedExceptionEntries, instructionBlocks, typeWriters);
								}
							}
						}
					}
				}
			}
		}

		if (methodAnalyzer != null) {
			methodAnalyzer.release();
		}
	}

	public boolean hasConditionalJump() {
		for (AbstractInsnNode instruction : this.instructions) {
			if (instruction.isJump()) {
				switch (instruction.getOpcode()) {
					case GOTO:
					case JSR:
					case GOTO_W:
					case JSR_W:
						return false;
					default:
						return true;
				}
			}
		}

		return false;
	}

	public void insertDummyCondition(
		Condition condition,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		StaticBlockAnalyzer staticBlockAnalyzer,
		Random random
	) {
		MethodAnalyzer methodAnalyzer;
		try {
			methodAnalyzer = this.getMethodAnalyzer(typeResolver, workspace);
		} catch (InvalidBytecodeException invalidBytecodeException) {
			throw new ProcessingException("Corrupt bytecode in '" + this.getSourcePath() + "' <clinit> : " + invalidBytecodeException.getMessage());
		}

		AtomicInt insertionLimitIndex = new AtomicInt();
		OrderedSet<Integer> safeInsertionPoints;
		if (staticBlockAnalyzer.hasOnlyLeafDependencies(this.getClassNode())) {
			safeInsertionPoints = this.findSafeInsertionPoints(methodAnalyzer, Integer.MAX_VALUE, insertionLimitIndex);
		} else {
			safeInsertionPoints = new OrderedSet<>(13);
		}
		ConstantPoolManager constantPoolManager = (ConstantPoolManager)this.getConstantPoolWriter();
		ArrayList<AbstractInsnNode> conditionInstructions = new ArrayList<>();
		LabelNode endLabel = null;
		if (safeInsertionPoints.size() <= 1) {
			WideConstantPoolInsnNode fieldReadInsn;
			boolean booleanGetterUsed;
			if (condition.canCallBooleanGetter() && random.nextBoolean()) {
				booleanGetterUsed = true;
				fieldReadInsn = condition.createBooleanGetterInsn(constantPoolManager, typeWriters);
			} else {
				booleanGetterUsed = false;
				fieldReadInsn = condition.createFieldReadInsn(constantPoolManager, typeWriters);
			}

			this.trackMemberForObfuscation(fieldReadInsn, memberObfuscationRefs);
			conditionInstructions.add(fieldReadInsn);
			endLabel = new LabelNode(1);
			JumpInsnNode conditionalJump;
			if (!booleanGetterUsed) {
				conditionalJump = condition.createJumpIfTrue(endLabel);
			} else {
				conditionalJump = condition.createJumpIfFalse(endLabel);
			}

			conditionInstructions.add(conditionalJump);
			if (condition.isReferenceType()) {
				conditionInstructions.addAll(ControlFlowBuilder.createDummyValueInstructions(condition, typeWriters, constantPoolManager, true, workspace, typeResolver.getClassProvider(), random));
			} else if (condition.isBooleanField()) {
				conditionInstructions.add(InsnNode.of(ICONST_1));
			} else {
				int dummyIntValue = random.nextInt(126) + 1;
				conditionInstructions.add(AbstractInsnNode.newInt(dummyIntValue));
			}
		} else if (condition.isReferenceType()) {
			if (condition.isPositiveCondition()) {
				conditionInstructions.addAll(ControlFlowBuilder.createDummyValueInstructions(condition, typeWriters, constantPoolManager, true, workspace, typeResolver.getClassProvider(), random));
			} else {
				conditionInstructions.add(InsnNode.of(ACONST_NULL));
			}
		} else if (condition.isBooleanField()) {
			if (condition.isPositiveCondition()) {
				conditionInstructions.add(InsnNode.of(ICONST_1));
			} else {
				conditionInstructions.add(InsnNode.of(ICONST_0));
			}
		} else if (condition.isPositiveCondition()) {
			int randomIntValue = random.nextInt(126) + 1;
			conditionInstructions.add(AbstractInsnNode.newInt(randomIntValue));
		} else {
			conditionInstructions.add(InsnNode.of(ICONST_0));
		}

		WideConstantPoolInsnNode fieldWriteInsn = condition.createFieldWriteInsn(constantPoolManager, typeWriters);
		this.trackMemberForObfuscation(fieldWriteInsn, memberObfuscationRefs);
		if (safeInsertionPoints.size() <= 1) {
			conditionInstructions.add(fieldWriteInsn);
			conditionInstructions.add(endLabel);
			fieldWriteInsn = null;
		}

		ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
		this.insertObfuscationBlock(conditionInstructions, fieldWriteInsn, instructionBlocks, methodAnalyzer, Integer.MAX_VALUE, null, insertionLimitIndex.get(), safeInsertionPoints, random);
		this.applyInstructionBlockPatches(instructionBlocks, "Flow Obfuscation", false, true);
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	private boolean isResourceBundleSubclass(String classInternalName, AbstractWorkspace workspace) {
		try {
			return workspace.isClassInstanceOf(classInternalName, "java/util/ResourceBundle");
		} catch (FileException ignored) {
			return true;
		}
	}

	public MethodAnalyzer getOrCreateMethodAnalyzerWithFlag(MethodAnalyzer methodAnalyzer, TypeResolver typeResolver, ClassHierarchy classHierarchy, boolean unusedFlag) {
		if (methodAnalyzer == null) {
			try {
				methodAnalyzer = this.createMethodAnalyzer(typeResolver, classHierarchy, unusedFlag);
			} catch (MethodAnalyzerException ignoredMethodAnalyzer) {
			} catch (StackOverflowError stackOverflowError) {
				throw new ZKMUnexpectedException("StackOverflowError trapped in '" + this.getParentString() + "' (6)", stackOverflowError);
			}
		}

		return methodAnalyzer;
	}

	private ExtraParameter[] adjustParameterListForIndy(
		List<String> currentParameterTypes, ParameterPattern parameterPattern, AbstractMethodNode lambdaTargetMethod, List<String> newParameterTypes, InvokeDynamicCallWriter indyCallWriter
	) {
		TypeWriter.parseParameterDescriptors(parameterPattern.getDescriptor());
		ExtraParameter[] extraParameters = parameterPattern.getExtraParameters();
		newParameterTypes.addAll(currentParameterTypes);
		String receiverParameter = null;
		if (lambdaTargetMethod.isInstanceMethod()) {
			receiverParameter = newParameterTypes.remove(0);
		}

		for (int extraParameterIndex = 0; extraParameterIndex < extraParameters.length; extraParameterIndex++) {
			ExtraParameter extraParameter = extraParameters[extraParameterIndex];
			DebugUtils.assertTrue(
				extraParameter.getParameterIndex() <= newParameterTypes.size(),
				new String[]{
					"Inconsistent parameters in method '" + lambdaTargetMethod.getSignature() + "' in class '" + lambdaTargetMethod.getSourcePath() + "' : " + extraParameter.getParameterIndex() + " : " + newParameterTypes.size()
				}
			);
			newParameterTypes.add(extraParameter.getParameterIndex(), String.valueOf(extraParameter.getTypeChar()));
		}

		ExtraParameter[] adjustedParameters;
		if (receiverParameter != null) {
			newParameterTypes.add(0, receiverParameter);
			adjustedParameters = new ExtraParameter[extraParameters.length];

			for (int adjustedParameterIndex = 0; adjustedParameterIndex < extraParameters.length; adjustedParameterIndex++) {
				ExtraParameter shiftedParameter = extraParameters[adjustedParameterIndex];
				adjustedParameters[adjustedParameterIndex] = new ExtraParameter(shiftedParameter.getParameterIndex() + 1, shiftedParameter.getTypeChar());
			}
		} else {
			adjustedParameters = extraParameters;
		}

		return adjustedParameters;
	}

	public void encryptLowValueLongs(
		NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longConstantEntries,
		MethodNode methodNode,
		Set<LongNode> excludedLongs,
		LongEncryptionExcludes encryptionExcludes,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		ClassNode ownerClass = methodNode.getMethodOwner();
		boolean longConstantsEncrypted = false;
		int instructionCount = this.instructions.size();
		int markerLabelIndex = 0;

		for (int markerSearchIndex = 0; markerSearchIndex < instructionCount; markerSearchIndex++) {
			AbstractInsnNode scannedInstruction = this.instructions.get(markerSearchIndex);
			if (scannedInstruction.isLabel() && ((LabelNode)scannedInstruction).isMarker()) {
				markerLabelIndex = markerSearchIndex;
				break;
			}
		}

		for (int instructionIndex = markerLabelIndex; instructionIndex < instructionCount; instructionIndex++) {
			AbstractInsnNode instruction = this.instructions.get(instructionIndex);
			if (instruction.isLong() || instruction.isLConst()) {
				boolean fieldStoreHandled = false;
				if (instructionIndex < instructionCount - 1) {
					AbstractInsnNode nextInstruction = this.instructions.get(instructionIndex + 1);
					if (nextInstruction.isPutField()) {
						AbstractFieldNode writtenField = (AbstractFieldNode)((FieldTypeWriter)((WideConstantPoolInsnNode)nextInstruction).getTypeWriter()).getTargetMember();
						if (writtenField != null && writtenField.isWorkspaceNode()) {
							fieldStoreHandled = true;
							if (encryptionExcludes == null || !encryptionExcludes.isFieldExcluded((FieldNode)writtenField)) {
								int opcode = instruction.getOpcode();
								if (opcode != LCONST_0 && opcode != LCONST_1) {
									instruction.collectLongObfCandidate(longConstantEntries, excludedLongs, methodNode, instructionIndex);
								} else {
									long constantValue = -1L;
									if (opcode == LCONST_0) {
										if (!Options.ZKM_LONG_ENCRYPT_LOW_VALUE_FIELDS) {
											continue;
										}

										constantValue = 0L;
									} else if (opcode == LCONST_1) {
										if (!Options.ZKM_LONG_ENCRYPT_LOW_VALUE_FIELDS) {
											continue;
										}

										constantValue = 1L;
									}

									LongNode longNode = constantPoolManager.getOrCreateLongInternal(constantValue, typeWriters, false, true);
									WideConstantPoolInsnNode ldcInstruction = new WideConstantPoolInsnNode(LDC2_W, longNode);
									this.instructions.set(instructionIndex, ldcInstruction);
									this.longConstantCount++;
									longConstantEntries.put(ownerClass, methodNode, longNode, new PriorityEntry<>(instructionIndex, ldcInstruction));
									longConstantsEncrypted = true;
								}
							}
						}
					}
				}

				if (!fieldStoreHandled && !instruction.isLConst()) {
					instruction.collectLongObfCandidate(longConstantEntries, excludedLongs, methodNode, instructionIndex);
				}
			}
		}

		if (longConstantsEncrypted) {
			this.setModified(true);
			this.recalculateOffsets();
		}
	}

	public boolean isStaticMethod() {
		return ((CodeAttribute)this.getParent()).isStatic();
	}

	public void collectExcludedStringConstants(Set<StringNode> excludedStrings, StringEncryptionExcludes encryptionTargetManager) {
		ClassNode classNode = (ClassNode)this.getClassNode();
		int instructionCount = this.instructions.size();

		for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
			AbstractInsnNode instruction = this.instructions.get(instructionIndex);
			int opcode = instruction.getOpcode();
			if (opcode == PUTSTATIC || opcode == PUTFIELD) {
				AbstractFieldNode writtenField = (AbstractFieldNode)((FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter()).getTargetMember();
				if (writtenField != null && writtenField.isWorkspaceNode() && writtenField.getParent() == classNode && encryptionTargetManager.isFieldExcluded((FieldNode)writtenField)) {
					AbstractInsnNode previousInstruction = this.instructions.get(instructionIndex - 1);
					int previousOpcode = previousInstruction.getOpcode();
					if (previousOpcode == LDC || previousOpcode == LDC_W) {
						TypeWriter constantWriter;
						if (previousOpcode == LDC) {
							constantWriter = ((LdcNode)previousInstruction).getTypeWriter();
						} else {
							constantWriter = ((WideConstantPoolInsnNode)previousInstruction).getTypeWriter();
						}

						if (constantWriter instanceof StringNode) {
							excludedStrings.add((StringNode)constantWriter);
						}
					}
				}
			}
		}
	}

	public String getMethodSignature() {
		CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
		return codeAttribute != null ? codeAttribute.getMethodSignature() : "<NO_PARENT_YET>";
	}

	public void collectExcludedLongConstants(Set<LongNode> excludedLongs, LongEncryptionExcludes encryptionExcludes) {
		ClassNode classNode = (ClassNode)this.getClassNode();
		int instructionCount = this.instructions.size();

		for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
			AbstractInsnNode instruction = this.instructions.get(instructionIndex);
			int opcode = instruction.getOpcode();
			if (opcode == PUTSTATIC || opcode == PUTFIELD) {
				AbstractFieldNode writtenField = (AbstractFieldNode)((FieldTypeWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter()).getTargetMember();
				if (writtenField != null && writtenField.isWorkspaceNode() && writtenField.getParent() == classNode && encryptionExcludes.isFieldExcluded((FieldNode)writtenField)) {
					AbstractInsnNode previousInstruction = this.instructions.get(instructionIndex - 1);
					if (previousInstruction.getOpcode() == LDC2_W) {
						TypeWriter constantWriter = ((WideConstantPoolInsnNode)previousInstruction).getTypeWriter();
						if (constantWriter instanceof LongNode) {
							excludedLongs.add((LongNode)constantWriter);
						}
					}
				}
			}
		}
	}

	public void findCommonStackFrameWithSuccessor(
		AtomicInt anchorIndex,
		AtomicInt targetIndex,
		AtomicReference<AnalyzingNode> successorNodeRef,
		AnalyzingNode scanNode,
		AnalyzingNode stackFrameNode,
		AnalyzingNode anchorNode,
		AnalyzingNode successorNode,
		Set<StackFrameType> monitorStackTypes,
		FrameNode[] frames,
		TypeResolver typeResolver,
		ScriptContext scriptContext,
		boolean keepBalancedLocks,
		Random random,
		String analysisContext
	) {
		int startIndex = scanNode.getStartIndex();
		int endIndex = scanNode.getEndIndex();
		ArrayList<CommonStackFrameCandidate> candidates = new ArrayList<>();

		for (int instructionIndex = endIndex - 1; instructionIndex >= startIndex; instructionIndex--) {
			if (frames[instructionIndex].isFullyResolved()) {
				int compatibleFrameIndex = stackFrameNode.findCompatibleFrameIndex(
					frames[instructionIndex], monitorStackTypes, frames, GlobalUtils.newHashSet(), stackFrameNode, successorNode, scanNode, new InsnList<>(this.instructions), typeResolver, scriptContext, successorNodeRef, keepBalancedLocks, analysisContext
				);
				if (compatibleFrameIndex != -1) {
					candidates.add(new CommonStackFrameCandidate(instructionIndex, compatibleFrameIndex, successorNodeRef.get()));
					if (Options.dontRandomizeFlowObfuscationWithSuccessor) {
						break;
					}
				}
			}
		}

		this.selectCommonStackFrameCandidate(anchorIndex, targetIndex, successorNodeRef, candidates, random);
	}

	private boolean isMethodCallPopable(int instructionIndex, AbstractMethodNode method, AtomicInt popCount) {
		int parameterCount = method.getNormalizedParameterDescriptors().size();
		int stackValueCount = 1;

		for (int parameterIndex = parameterCount - 1; parameterIndex >= 0; parameterIndex--) {
			int argumentInstructionIndex = instructionIndex - parameterCount + parameterIndex + 1;
			if (!this.instructions.get(argumentInstructionIndex).isStandaloneStackProducer()) {
				return false;
			}

			instructionIndex--;
			stackValueCount++;
		}

		if (!method.isStatic()) {
			if (!this.instructions.get(instructionIndex).isStandaloneStackProducer()) {
				return false;
			}

			stackValueCount++;
		}

		popCount.set(stackValueCount);
		return true;
	}

	private int generateParameterDecryptionCode(
		long xorKey,
		LocalVariableSlot keyLocalSlot,
		Set<MethodNode> parameterChangedMethods,
		Map<AbstractClassNode, ChangedMethod> changedMethodsByClass,
		Map<MethodNode, ParameterPattern> parameterPatterns,
		Map<MethodNode, Long> keyOperands,
		List<AbstractInsnNode> instructions,
		List<Integer> usedLocalSlots,
		Map<MethodNode, int[]> keyLocalSlotsByMethod,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager,
		Hierarchy hierarchy
	) {
		CodeAttribute codeAttribute = this.getCodeAttribute();
		byte requiredStackDepth = 4;
		int nextFreeSlot = codeAttribute.getMaxLocals();
		instructions.add(AbstractInsnNode.loadLong(keyLocalSlot));

		for (MethodNode parameterChangedMethod : parameterChangedMethods) {
			ParameterPattern parameterPattern = parameterPatterns.get(parameterChangedMethod);
			if (!parameterPattern.hasNoExtraParameters()) {
				long keyOperand = keyOperands.get(parameterChangedMethod);
				ChangedMethod changedMethod = changedMethodsByClass.get(parameterChangedMethod.getMethodOwner());
				long decryptionKey;
				if (isMethodParameterChanged(parameterChangedMethod, changedMethod, hierarchy)) {
					decryptionKey = xorKey ^ keyOperand ^ changedMethod.getMethodSalt();
				} else {
					decryptionKey = xorKey ^ keyOperand;
				}

				instructions.add(InsnNode.of(DUP2));
				instructions.add(AbstractInsnNode.ldcLong(decryptionKey, constantPoolManager, typeWriters));
				instructions.add(InsnNode.of(LXOR));
				if (parameterPattern.hasMultipleExtraParameters()) {
					requiredStackDepth = 4;
					int[] extraParameterSlots = new int[parameterPattern.getExtraParameterCount()];

					for (int extraParameterIndex = 0; extraParameterIndex < extraParameterSlots.length; extraParameterIndex++) {
						instructions.add(InsnNode.of(DUP2));
						int bitShiftAmount = parameterPattern.getCumulativeBitShift(extraParameterIndex);
						if (bitShiftAmount > 0) {
							instructions.add(AbstractInsnNode.newInt(bitShiftAmount));
							instructions.add(InsnNode.of(LSHL));
						}

						int rightShiftAmount = parameterPattern.getRightShiftAmount(extraParameterIndex);
						instructions.add(AbstractInsnNode.newInt(rightShiftAmount));
						instructions.add(InsnNode.of(LUSHR));
						if (parameterPattern.isLongExtraParameter(extraParameterIndex)) {
							instructions.add(AbstractInsnNode.storeLong(nextFreeSlot, this.localVariableTable, 5));
							extraParameterSlots[extraParameterIndex] = nextFreeSlot;
							usedLocalSlots.add(INTEGER_CACHE.valueOf(nextFreeSlot));
							usedLocalSlots.add(INTEGER_CACHE.valueOf(nextFreeSlot + 1));
							nextFreeSlot += 2;
						} else {
							instructions.add(InsnNode.of(L2I));
							instructions.add(AbstractInsnNode.storeInt(nextFreeSlot, this.localVariableTable, 5));
							extraParameterSlots[extraParameterIndex] = nextFreeSlot;
							usedLocalSlots.add(INTEGER_CACHE.valueOf(nextFreeSlot));
							nextFreeSlot++;
						}
					}

					instructions.add(InsnNode.of(POP2));
					keyLocalSlotsByMethod.put(parameterChangedMethod, extraParameterSlots);
				} else {
					instructions.add(AbstractInsnNode.storeLong(nextFreeSlot, this.localVariableTable, 5));
					int[] parameterSlots = new int[]{nextFreeSlot};
					usedLocalSlots.add(INTEGER_CACHE.valueOf(nextFreeSlot));
					usedLocalSlots.add(INTEGER_CACHE.valueOf(nextFreeSlot + 1));
					nextFreeSlot += 2;
					keyLocalSlotsByMethod.put(parameterChangedMethod, parameterSlots);
				}
			}
		}

		instructions.add(InsnNode.of(POP2));
		codeAttribute.setMaxLocals(nextFreeSlot);
		return requiredStackDepth;
	}

	public AbstractMethodNode getMethodNode() {
		return (AbstractMethodNode)((CodeAttribute)this.getParent()).getParent();
	}

	private InvokeDynamicCallWriter getOrCreateIndyWriter(
		String descriptor,
		int helperIndex,
		AbstractMethodCallWriter helperWriter,
		BootstrapMethodsAttribute bootstrapMethods,
		Map<String, InvokeDynamicCallWriter> indyWritersByName,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		String indyKey = (char)helperIndex + descriptor;
		InvokeDynamicCallWriter indyCallWriter = indyWritersByName.get(indyKey);
		if (indyCallWriter == null) {
			NameAndTypeWriter nameAndType = constantPoolManager.createNameAndType(String.valueOf((char)helperIndex), descriptor, typeWriters);
			ResolvedMethodHandleWriter methodHandle = constantPoolManager.getOrCreateMethodHandle(MethodHandleKind.REF_INVOKE_STATIC, helperWriter, typeWriters);
			TypeWriter[] bootstrapArguments = new TypeWriter[0];
			BootstrapMethodEntry bootstrapMethodEntry = bootstrapMethods.addEntry(methodHandle, bootstrapArguments);
			indyCallWriter = new InvokeDynamicCallWriter(0, constantPoolManager, nameAndType, bootstrapMethodEntry);
			typeWriters.add(indyCallWriter);
			indyWritersByName.put(indyKey, indyCallWriter);
		}

		return indyCallWriter;
	}

	public InsnList<AbstractInsnNode> getInstructionListCopy() {
		return new InsnList<>(this.instructions);
	}

	public List<AbstractInsnNode> generateDummyJump(
		Condition condition,
		WideConstantPoolInsnNode memberAccessInsn,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager,
		List<Integer> tailVariableSlots,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		Random random
	) {
		ArrayList<AbstractInsnNode> generatedInstructions = new ArrayList<>(8);
		LabelNode jumpTargetLabel = new LabelNode(true, 1);
		LabelNode mergeLabel = new LabelNode(true, 1);
		WideConstantPoolInsnNode conditionWriteInsn = condition.createFieldWriteInsn(constantPoolManager, typeWriters);
		this.trackMemberForObfuscation(conditionWriteInsn, memberObfuscationRefs);
		if (condition.isBooleanField()) {
			generatedInstructions.add(memberAccessInsn);
			generatedInstructions.add(condition.createJumpIfTrue(jumpTargetLabel));
			generatedInstructions.add(InsnNode.of(ICONST_0));
			generatedInstructions.add(new GotoNode(mergeLabel));
			generatedInstructions.add(jumpTargetLabel);
			generatedInstructions.add(InsnNode.of(ICONST_1));
			generatedInstructions.add(mergeLabel);
			generatedInstructions.add(conditionWriteInsn);
		} else if (condition.isReferenceType()) {
			generatedInstructions.addAll(ControlFlowBuilder.createDummyValueInstructions(condition, typeWriters, constantPoolManager, false, workspace, classProvider, random));
			generatedInstructions.add(conditionWriteInsn);
		} else {
			CodeAttribute codeAttribute = (CodeAttribute)this.getParent();
			int newMaxLocals = codeAttribute.getMaxLocals() + 1;
			codeAttribute.setMaxLocals(newMaxLocals);
			int newLocalIndex = newMaxLocals - 1;
			tailVariableSlots.add(INTEGER_CACHE.valueOf(newLocalIndex));
			AbstractInsnNode storeLocalInsn = AbstractInsnNode.storeInt(newLocalIndex, this.localVariableTable, 2);
			AbstractInsnNode loadLocalInsn = AbstractInsnNode.loadInt(newLocalIndex, this.localVariableTable, 2);
			generatedInstructions.add(memberAccessInsn);
			generatedInstructions.add(storeLocalInsn);
			generatedInstructions.add(AbstractInsnNode.IINC(newLocalIndex, 1, this.localVariableTable, 2));
			generatedInstructions.add(loadLocalInsn);
			generatedInstructions.add(conditionWriteInsn);
		}

		return generatedInstructions;
	}

	public boolean optimizeLdcInstructions(Map<TypeWriter, TypeWriter> typeWriterMapping) {
		boolean optimized = false;
		int instructionCount = this.instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode optimizedInstruction = this.instructions.get(i).optimizeToShortLdc(typeWriterMapping);
			if (optimizedInstruction != null) {
				this.instructions.set(i, optimizedInstruction);
				optimized = true;
			}
		}

		if (optimized) {
			this.setModified(true);
		}

		return optimized;
	}

	private List<AbstractInsnNode> generateConditionalBlock(
		int localSlotIndex,
		WideConstantPoolInsnNode groupInsn,
		AbstractInsnNode loadInsn,
		Condition groupCondition,
		Condition packageCondition,
		boolean flagged,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		Random random
	) {
		ArrayList<AbstractInsnNode> generatedInstructions = new ArrayList<>(11);
		LabelNode endLabel = new LabelNode(true, 1);
		generatedInstructions.add(groupInsn);
		if (flagged) {
			generatedInstructions.add(groupCondition.createJumpIfFalse(endLabel));
		} else {
			generatedInstructions.add(groupCondition.createJumpIfTrue(endLabel));
		}

		WideConstantPoolInsnNode conditionWriteInsn = packageCondition.createFieldWriteInsn(constantPoolManager, typeWriters);
		this.trackMemberForObfuscation(conditionWriteInsn, memberObfuscationRefs);
		if (packageCondition.isBooleanField()) {
			LabelNode jumpTargetLabel = new LabelNode(true, 1);
			LabelNode mergeLabel = new LabelNode(true, 1);
			generatedInstructions.add(loadInsn);
			generatedInstructions.add(new JumpInsnNode(IFEQ, jumpTargetLabel));
			generatedInstructions.add(InsnNode.of(ICONST_0));
			generatedInstructions.add(new GotoNode(mergeLabel));
			generatedInstructions.add(jumpTargetLabel);
			generatedInstructions.add(InsnNode.of(ICONST_1));
			generatedInstructions.add(mergeLabel);
			generatedInstructions.add(conditionWriteInsn);
		} else if (packageCondition.isReferenceType()) {
			generatedInstructions.addAll(ControlFlowBuilder.createDummyValueInstructions(packageCondition, typeWriters, constantPoolManager, false, workspace, classProvider, random));
			generatedInstructions.add(conditionWriteInsn);
		} else {
			generatedInstructions.add(AbstractInsnNode.IINC(localSlotIndex, 1, this.localVariableTable, 2));
			generatedInstructions.add(loadInsn);
			generatedInstructions.add(conditionWriteInsn);
		}

		generatedInstructions.add(endLabel);
		return generatedInstructions;
	}

	private Set<MethodParameterKey> createMethodParameterKeys(String memberName, boolean hasReturnType, Reference reference) {
		HashSet<MethodParameterKey> parameterKeys = GlobalUtils.newHashSet();
		if (reference instanceof NullReference) {
			parameterKeys.add(new MethodParameterKey(memberName, "()", null));
		} else if (reference instanceof ArrayReference) {
			ArrayReference arrayReference = (ArrayReference)reference;
			String[][] typeCombinations = arrayReference.getTypeCombinations();
			if (arrayReference.getElementCount() != 0 && typeCombinations != null && typeCombinations.length != 0) {
				for (int i = 0; i < typeCombinations.length; i++) {
					String[] typeCombination = typeCombinations[i];
					if (hasReturnType) {
						String[] parameterTypeNames = new String[typeCombination.length - 1];
						System.arraycopy(typeCombination, 1, parameterTypeNames, 0, parameterTypeNames.length);
						parameterKeys.add(new MethodParameterKey(memberName, this.buildParameterDescriptor(parameterTypeNames), MethodSignature.fromInternalName(typeCombination[0])));
					} else {
						parameterKeys.add(new MethodParameterKey(memberName, this.buildParameterDescriptor(typeCombination), null));
					}
				}
			} else {
				parameterKeys.add(new MethodParameterKey(memberName, "()", null));
			}
		} else if (!(reference instanceof ClassReference) && !(reference instanceof InstructionTypeReference)) {
			if (reference instanceof MethodTypeReference) {
				parameterKeys.add(new MethodParameterKey(memberName, reference.getValue()));
			} else if (reference instanceof PrimitiveTypeReference) {
				if (hasReturnType) {
					parameterKeys.add(new MethodParameterKey(memberName, this.buildParameterDescriptor(new String[0]), reference.getValue()));
				} else {
					String[] primitiveTypeNames = new String[]{reference.getValue()};
					parameterKeys.add(new MethodParameterKey(memberName, this.buildParameterDescriptor(primitiveTypeNames), null));
				}
			}
		} else if (hasReturnType) {
			parameterKeys.add(new MethodParameterKey(memberName, this.buildParameterDescriptor(new String[0]), MethodSignature.fromInternalName(reference.getValue())));
		} else {
			String[] classTypeNames = new String[]{MethodSignature.fromInternalName(reference.getValue())};
			parameterKeys.add(new MethodParameterKey(memberName, this.buildParameterDescriptor(classTypeNames), null));
		}

		return parameterKeys;
	}

	public static class LongEntryPairComparator implements Comparator<KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry>> {
		public final InstructionListNode ownerNode;

		public LongEntryPairComparator(InstructionListNode ownerNode) {
			this.ownerNode = ownerNode;
		}

		@Override
		public int compare(
			KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry> o1, KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry> o2
		) {
			return this.compareByPriority(o1, o2);
		}

		public int compareByPriority(
			KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry> pair1, KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry> pair2
		) {
			return pair1.getKey().compareTo(pair2.getKey());
		}
	}

	public static class GraphElementComparator implements Comparator<IGraphElement> {
		public int compareByLocalVariableTable(IGraphElement element1, IGraphElement element2) {
			if (element1 instanceof LocalVariableEntry) {
				return element2 instanceof LocalVariableEntry ? 0 : -1;
			} else {
				return element2 instanceof LocalVariableEntry ? 1 : 0;
			}
		}

		@Override
		public int compare(IGraphElement o1, IGraphElement o2) {
			return this.compareByLocalVariableTable(o1, o2);
		}
	}

	public static class StringEntryPairComparator implements Comparator<KeyValuePair<PriorityEntry<WritableType>, StringEntry>> {
		public final InstructionListNode ownerNode;

		public StringEntryPairComparator(InstructionListNode ownerNode) {
			this.ownerNode = ownerNode;
		}

		public int compareByPriority(KeyValuePair<PriorityEntry<WritableType>, StringEntry> pair1, KeyValuePair<PriorityEntry<WritableType>, StringEntry> pair2) {
			return pair1.getKey().compareTo(pair2.getKey());
		}

		@Override
		public int compare(KeyValuePair<PriorityEntry<WritableType>, StringEntry> o1, KeyValuePair<PriorityEntry<WritableType>, StringEntry> o2) {
			return this.compareByPriority(o1, o2);
		}
	}
}

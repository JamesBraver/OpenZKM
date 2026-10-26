package com.zelix.tools.references;

import com.zelix.enums.MixCaseType;
import com.zelix.event.EventRegistry;
import com.zelix.exceptions.ObfuscateReferencesFailedException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.*;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.*;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.utils.PrimitiveBoxingUtils;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.tools.AuxiliaryClassGenerator;
import com.zelix.tools.AuxiliaryWiringStrategy;
import com.zelix.tools.InjectedClassManager;
import com.zelix.tools.parameterChanger.dataset.ParameterRotateEntry;
import com.zelix.tools.string.StringEncryptionBuilder;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.collections.UnmodifiableMultiMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.dataset.MemberContext;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import com.zelix.workspace.Workspace;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class ObfuscateReferencesBuilder implements AuxiliaryClassGenerator {
	private static final int ENCRYPTION_KEY_MASK = (int)(Math.pow(2.0, 7.0) - 1.0);
	private static final int ENCRYPTION_SHIFT = (int)Math.pow(2.0, 6.0);
	private static final int MAX_CONSTANT_POOL_SIZE = 65535 - (Options.analyzeReferenceDataClass ? '릌' : 32);
	public static final byte[] LEGACY_HELPER_CLASS_BYTES = GlobalUtils.baseNStringToByteArray(
		"5m72565a00030019000a0700080700090a000200040c0006000501000314152e0100061o2x322x381q0100041v332s2t0100051x3134383d01000g2y2p3a2p1b302p322v1b272q2y2t2r38000100010002000000000001000100060005000100070000000h0001000100000005165300034x000000000000",
		36
	);
	public static final byte[] MODERN_HELPER_CLASS_BYTES = GlobalUtils.baseNStringToByteArray(
		"5m72565a0000001d000a0a000300070700080700090100061o2x322x381q01000314152e0100041v332s2t0c000400050100051x3134383d01000g2y2p3a2p1b302p322v1b272q2y2t2r38000x00020003000000000001000100040005000100060000000h0001000100000005165300014x000000000000",
		36
	);
	public static IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
	private final DualKeyMap<MemberNode, AbstractClassNode, MemberContext> memberEntryCache = new DualKeyMap<>();
	private final Random random = GlobalUtils.newRandom(65536);
	private final boolean randomizeObfuscation;
	private final AtomicBool useEnhanced;
	private final List<String> userConfiguredNames;

	public ObfuscateReferencesBuilder(AtomicBool useEnhanced, List<String> userConfiguredNames, boolean randomizeObfuscation) {
		this.randomizeObfuscation = randomizeObfuscation;
		this.useEnhanced = useEnhanced;
		this.userConfiguredNames = userConfiguredNames;
	}

	public static boolean isMemberEligible(MemberContext memberContext, ClassHierarchy classHierarchy) {
		boolean eligible;
		if (memberContext.isWorkspaceNode()) {
			eligible = true;
		} else if (Options.ZKM_OBFUSCATE_EXTERNAL_REFERENCES_INDY) {
			String ownerPackageName = memberContext.getOwnerPackageName();
			MemberNode memberNode = memberContext.getMemberNode();
			AbstractClassNode contextClass = memberContext.getContextClass();
			if (ownerPackageName.equals("java/lang/invoke")
				|| memberNode.isMethod() && memberNode.isNative() && memberNode.getInternalName().equals("java/lang/Object")
				|| contextClass.isInterface() && memberNode.isMethod() && memberNode.isAbstract() && classHierarchy.isObjectMethodSignature(((AbstractMethodNode)memberNode).getMethodSignature())) {
				eligible = false;
			} else {
				eligible = true;
			}
		} else {
			eligible = false;
		}

		return eligible;
	}

	public static boolean isIndySupported(AbstractClassNode classNode) {
		return Options.ZKM_OBFUSCATE_REFERENCES_INDY && indySupported(classNode);
	}

	public static boolean needsArgumentArrayResolver(ClassNode invokerClass, ClassNode referencingClass, MemberContext memberContext, ClassHierarchy hierarchy) {
		if (memberContext.isField()) {
			return false;
		} else if (!Options.ZKM_OBFUSCATE_REFERENCES_INDY) {
			return true;
		} else {
			return !isMemberEligible(memberContext, hierarchy) ? true : !shouldUseIndy(invokerClass, referencingClass);
		}
	}

	private static void applyKeyShift(int tableIndex, int[] keys, int[] digitTable) {
		int xorKey = digitTable[tableIndex];

		for (int i = 0; i < keys.length; i++) {
			keys[i] = (keys[i] + xorKey) % (ENCRYPTION_KEY_MASK + 1);
		}
	}

	public static boolean indySupported(AbstractClassNode classNode) {
		return classNode.isJava8OrNewer() || !Options.dontUseIndyOnJava7 && classNode.isJava7OrNewer() && !classNode.isInterface();
	}

	public static boolean shouldUseIndy(ClassNode invokerClass, ClassNode referencingClass) {
		return Options.ZKM_OBFUSCATE_REFERENCES_INDY && isIndySupported(invokerClass) && (referencingClass == null || isIndySupported(referencingClass));
	}

//   public static void main(String[] args)throws  Throwable{
//      java.nio.file.Files.write(java.nio.file.Paths.get("referencesObf_q.class"), q);
//      java.nio.file.Files.write(java.nio.file.Paths.get("referencesObf_u.class"), u);
//   }

	@Override
	public ClassNode generate(String className, int majorVersion, int minorVersion, AuxiliaryWiringStrategy wiringStrategy) {
		try {
			byte[] classBytes;
			if (majorVersion < 46) {
				classBytes = LEGACY_HELPER_CLASS_BYTES;
			} else {
				classBytes = MODERN_HELPER_CLASS_BYTES;
			}

			ByteDataBuffer classDataBuffer = ByteDataBuffer.fromBytes(classBytes, false);
			ClasspathResource classResource = ClasspathResource.createSynthetic("ReferenceObfuscation." + className);
			AtomicReference<String> progressMessageRef = new AtomicReference<>();
			EventRegistry eventRegistry = new EventRegistry();
			PrintWriter messageWriter = new PrintWriter(new StringWriter());
			PrintWriter unusedWriter = new PrintWriter(new StringWriter());
			ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes = new ThreeLevelMultiMap<>();
			ClassNode helperClassNode = new ClassNode(classDataBuffer, classResource, progressMessageRef, eventRegistry, messageWriter, unusedWriter, unknownAttributes, 4);
			helperClassNode.renameClassInternal(className, GlobalUtils.newHashMap(13));
			helperClassNode.setMajorVersion(majorVersion);
			helperClassNode.setMinorVersion(minorVersion);
			return helperClassNode;
		} catch (IOException ioException) {
			throw new ObfuscateReferencesFailedException("ObfuscateReferences (A) : " + ioException.getMessage(), ioException);
		} catch (ProcessingException processingException) {
			throw new ObfuscateReferencesFailedException("ObfuscateReferences (B) : " + processingException.getMessage(), processingException);
		}
	}

	public Map<MemberContext, Integer> assignIdsToMembers(
		boolean useIndy,
		AtomicBool hasIndyMembers,
		AtomicBool hasNonIndyMembers,
		MultiMap<InstructionListNode, MemberContext> memberReferences,
		int startId,
		Set<MemberContext> nonIndyMembers,
		AtomicBool hasFields,
		AtomicBool hasMethods,
		ClassHierarchy classHierarchy
	) {
		int nextMemberId = startId;
		HashSet<MemberContext> distinctMemberContexts = GlobalUtils.newHashSet();
		LinkedHashMap<MemberContext, Integer> idByMemberContext = new LinkedHashMap<>();
		if (memberReferences != null) {
			for (Entry<InstructionListNode, List<MemberContext>> instructionEntry : memberReferences.entrySet()) {
				distinctMemberContexts.addAll(instructionEntry.getValue());
				if (!isIndySupported(instructionEntry.getKey().getClassNode())) {
					for (MemberContext entryMemberContext : instructionEntry.getValue()) {
						if (!entryMemberContext.isField()) {
							nonIndyMembers.add(entryMemberContext);
						}
					}
				}
			}

			for (MemberContext memberContext : distinctMemberContexts) {
				if (!idByMemberContext.containsKey(memberContext)) {
					idByMemberContext.put(memberContext, INTEGER_CACHE.valueOf(nextMemberId++));
					if (memberContext.isField()) {
						hasFields.set(true);
					} else {
						hasMethods.set(true);
					}
				}

				if (useIndy && !nonIndyMembers.contains(memberContext) && isMemberEligible(memberContext, classHierarchy)) {
					hasIndyMembers.set(true);
				} else {
					hasNonIndyMembers.set(true);
				}
			}
		}

		return idByMemberContext;
	}

	private Long scrambleId(int typeId) {
		long idHighBits = (long)typeId << 46;
		long randomBits = this.random.nextLong() & 70368744177663L;
		return idHighBits | randomBits;
	}

	public AbstractMethodCallWriter genInterfaceFieldResolver(
		ClassNode classNode, AbstractMethodCallWriter declaredFieldFinderWriter, Inheritance inheritance, Workspace workspace, ClassProvider classProvider, List<TypeWriter> typeWriters, ConstantPoolManager constantPoolManager
	) {
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		AtomicInt maxLocals = new AtomicInt(6);
		AtomicInt maxStack = new AtomicInt(3);
		AtomicReference<InstructionListNode> instructionListRef = new AtomicReference<>();
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/reflect/Field;", 5);
		AbstractMethodCallWriter generatedMethodWriter = this.createHelperMethod(
			classNode,
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/reflect/Field;",
			new ArrayList<>(),
			maxStack.get(),
			maxLocals.get(),
			localVariableTable,
			tryCatchBlocks,
			false,
			typeWriters,
			instructionListRef,
			inheritance,
			workspace
		);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.genFieldLookupCode(instructions, localVariableTable, generatedMethodWriter, declaredFieldFinderWriter, typeWriters, constantPoolManager, workspace, classProvider);
		instructionListRef.get().setInstructions(instructions, "Reference Obfuscation");
		return generatedMethodWriter;
	}

	public AbstractMethodCallWriter genClassResolverMethod(
		ClassNode classNode,
		AbstractMethodCallWriter idDecoder,
		FieldTypeWriter classNameField,
		FieldTypeWriter classCacheField,
		Inheritance inheritance,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> cpEntries,
		ConstantPoolManager cpManager
	) {
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[1];
		LocalVariableTable localVariableTable = new LocalVariableTable(true, this.useEnhanced.get() ? "(JJ)Ljava/lang/Class;" : "(J)Ljava/lang/Class;", 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		AtomicInt maxLocals = new AtomicInt(0);
		AtomicInt maxStack = new AtomicInt(0);
		this.genClassResolverMethodCode(instructions, localVariableTable, idDecoder, classNameField, classCacheField, cpEntries, tryCatchBlocks, maxStack, maxLocals, cpManager, workspace, classProvider);
		return this.createGeneratedMethod(
			classNode, this.useEnhanced.get() ? "(JJ)Ljava/lang/Class;" : "(J)Ljava/lang/Class;", instructions, maxStack.get(), maxLocals.get(), localVariableTable, tryCatchBlocks, false, cpEntries, inheritance, workspace
		);
	}

	public AbstractMethodCallWriter genMethodResolverHelper(
		ClassNode classNode, Inheritance inheritance, Workspace workspace, ClassProvider classProvider, List<TypeWriter> typeWriters, ConstantPoolManager constantPoolManager
	) {
		LocalVariableTable localVariableTable = new LocalVariableTable(
			true, "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;I[Ljava/lang/Class;)Ljava/lang/reflect/Method;", 11
		);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		AtomicInt maxLocals = new AtomicInt(0);
		AtomicInt maxStack = new AtomicInt(0);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		this.genMethodResolverHelperCode(instructions, localVariableTable, typeWriters, maxStack, maxLocals, constantPoolManager, workspace, classProvider);
		return this.createGeneratedMethod(
			classNode,
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;I[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
			instructions,
			maxStack.get(),
			maxLocals.get(),
			localVariableTable,
			tryCatchBlocks,
			false,
			typeWriters,
			inheritance,
			workspace
		);
	}

	public MethodCallWriter genFieldGetterCall(
		FieldTypeWriter fieldRef, AtomicBool needsCast, ConstantPoolManager cpManager, List<TypeWriter> cpEntries, AbstractWorkspace workspace, ClassProvider classProvider
	) {
		String fieldDescriptor = fieldRef.getDescriptor();
		if (!fieldDescriptor.startsWith("[") && (!fieldDescriptor.startsWith("L") || !fieldDescriptor.endsWith(";"))) {
			if (fieldDescriptor.equals("Z")) {
				return cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "getBoolean", "(Ljava/lang/Object;)Z", cpEntries, workspace, classProvider);
			} else if (fieldDescriptor.equals("B")) {
				return cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "getByte", "(Ljava/lang/Object;)B", cpEntries, workspace, classProvider);
			} else if (fieldDescriptor.equals("S")) {
				return cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "getShort", "(Ljava/lang/Object;)S", cpEntries, workspace, classProvider);
			} else if (fieldDescriptor.equals("C")) {
				return cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "getChar", "(Ljava/lang/Object;)C", cpEntries, workspace, classProvider);
			} else if (fieldDescriptor.equals("I")) {
				return cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "getInt", "(Ljava/lang/Object;)I", cpEntries, workspace, classProvider);
			} else if (fieldDescriptor.equals("J")) {
				return cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "getLong", "(Ljava/lang/Object;)J", cpEntries, workspace, classProvider);
			} else if (fieldDescriptor.equals("F")) {
				return cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "getFloat", "(Ljava/lang/Object;)F", cpEntries, workspace, classProvider);
			} else {
				return fieldDescriptor.equals("D")
					? cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "getDouble", "(Ljava/lang/Object;)D", cpEntries, workspace, classProvider)
					: cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", cpEntries, workspace, classProvider);
			}
		} else {
			needsCast.set(true);
			return cpManager.getOrCreateMethodRef("java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", cpEntries, workspace, classProvider);
		}
	}

	private void genInterfaceMethodResolverCode(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariables,
		AbstractMethodCallWriter interfaceResolverWriter,
		AbstractMethodCallWriter declaredMethodFinderWriter,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode interfacesSearchLabel = new LabelNode(true, 1);
		LabelNode loopStartLabel = new LabelNode(true, 1);
		LabelNode nextInterfaceLabel = new LabelNode(true, 1);
		LabelNode returnNullLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(1, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(2, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(3, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(4, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, declaredMethodFinderWriter));
		instructions.add(AbstractInsnNode.storeObject(5, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(5, localVariables, 4));
		instructions.add(new JumpInsnNode(IFNULL, interfacesSearchLabel));
		instructions.add(AbstractInsnNode.loadObject(5, localVariables, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(interfacesSearchLabel);
		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 4));
		MethodCallWriter getInterfacesMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getInterfaces", "()[Ljava/lang/Class;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getInterfacesMethod));
		instructions.add(AbstractInsnNode.storeObject(6, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(6, localVariables, 4));
		instructions.add(new JumpInsnNode(IFNULL, returnNullLabel));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(7, localVariables, 4));
		instructions.add(loopStartLabel);
		instructions.add(AbstractInsnNode.loadInt(7, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(6, localVariables, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(new JumpInsnNode(IF_ICMPGE, returnNullLabel));
		instructions.add(AbstractInsnNode.loadObject(6, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(7, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.loadObject(1, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(2, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(3, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(4, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, interfaceResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(5, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(5, localVariables, 4));
		instructions.add(new JumpInsnNode(IFNULL, nextInterfaceLabel));
		instructions.add(AbstractInsnNode.loadObject(5, localVariables, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(nextInterfaceLabel);
		instructions.add(AbstractInsnNode.IINC(7, 1, localVariables, 4));
		instructions.add(new GotoNode(loopStartLabel));
		instructions.add(returnNullLabel);
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(InsnNode.of(ARETURN));
	}

	public int[] getAccessKindCodes() {
		int[] defaultKindCodeTable = new int[]{
			97,
			98,
			99,
			100,
			101,
			102,
			103,
			104,
			105,
			106,
			107,
			108,
			109,
			110,
			111,
			112,
			113,
			114,
			115,
			116,
			117,
			118,
			119,
			120,
			121,
			122,
			65,
			66,
			67,
			68,
			69,
			70,
			71,
			72,
			73,
			74,
			75,
			76,
			77,
			78,
			79,
			80,
			81,
			82,
			83,
			84,
			85,
			86,
			87,
			88,
			89,
			90,
			36,
			162,
			163,
			164,
			165,
			170,
			181,
			186,
			192,
			193,
			194,
			195,
			196,
			197,
			198,
			199,
			200,
			201,
			202,
			203,
			204,
			205,
			206,
			207,
			208,
			209,
			210,
			211,
			212,
			213,
			214,
			216,
			217,
			218,
			219,
			220,
			221,
			222,
			223,
			224,
			225,
			226,
			227,
			228,
			229,
			230,
			231,
			232,
			233,
			234,
			235,
			236,
			237,
			238,
			239,
			240,
			241,
			242,
			243,
			244,
			245,
			246,
			248,
			249,
			250,
			251,
			252,
			253,
			254,
			255
		};
		int[] enhancedKindCodeTable = new int[]{104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115};
		int[] selectedKindCodeTable;
		if (this.isEnhanced() && Options.xorKeyWithLowBits) {
			selectedKindCodeTable = enhancedKindCodeTable;
		} else {
			selectedKindCodeTable = defaultKindCodeTable;
		}

		if (!Options.dontShuffleReferenceOpCodes) {
			GlobalUtils.shuffle(selectedKindCodeTable, this.random);
		}

		int[] referenceKindCodes = new int[7];
		System.arraycopy(selectedKindCodeTable, 0, referenceKindCodes, 0, referenceKindCodes.length);
		return referenceKindCodes;
	}

	private void genMethodResolverHelperCode(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariables,
		List<TypeWriter> typeWriters,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		maxLocals.set(11);
		maxStack.set(3);
		LabelNode methodLoopStartLabel = new LabelNode(true, 1);
		LabelNode paramLoopStartLabel = new LabelNode(true, 1);
		LabelNode paramMatchedLabel = new LabelNode(true, 1);
		LabelNode returnMethodLabel = new LabelNode(true, 1);
		LabelNode nextMethodLabel = new LabelNode(true, 1);
		LabelNode returnNullLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 4));
		MethodCallWriter getDeclaredMethodsMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getDeclaredMethods", "()[Ljava/lang/reflect/Method;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getDeclaredMethodsMethod));
		instructions.add(AbstractInsnNode.storeObject(5, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(5, localVariables, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(AbstractInsnNode.storeInt(6, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(7, localVariables, 4));
		instructions.add(methodLoopStartLabel);
		instructions.add(AbstractInsnNode.loadInt(7, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(6, localVariables, 4));
		instructions.add(new JumpInsnNode(IF_ICMPGE, returnNullLabel));
		instructions.add(AbstractInsnNode.loadObject(5, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(7, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.storeObject(8, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(8, localVariables, 4));
		MethodCallWriter getNameMethod = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "getName", "()Ljava/lang/String;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameMethod));
		instructions.add(AbstractInsnNode.loadObject(1, localVariables, 4));
		MethodCallWriter equalsMethod = constantPoolManager.getOrCreateMethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, equalsMethod));
		instructions.add(new JumpInsnNode(IFEQ, nextMethodLabel));
		instructions.add(AbstractInsnNode.loadObject(8, localVariables, 4));
		MethodCallWriter getReturnTypeMethod = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "getReturnType", "()Ljava/lang/Class;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getReturnTypeMethod));
		instructions.add(AbstractInsnNode.loadObject(2, localVariables, 4));
		instructions.add(new JumpInsnNode(IF_ACMPNE, nextMethodLabel));
		instructions.add(AbstractInsnNode.loadObject(8, localVariables, 4));
		MethodCallWriter getParameterTypesMethod = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "getParameterTypes", "()[Ljava/lang/Class;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getParameterTypesMethod));
		instructions.add(AbstractInsnNode.storeObject(9, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(9, localVariables, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(AbstractInsnNode.loadInt(3, localVariables, 4));
		instructions.add(new JumpInsnNode(IF_ICMPNE, nextMethodLabel));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(10, localVariables, 4));
		instructions.add(paramLoopStartLabel);
		instructions.add(AbstractInsnNode.loadInt(10, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(3, localVariables, 4));
		instructions.add(new JumpInsnNode(IF_ICMPGE, returnMethodLabel));
		instructions.add(AbstractInsnNode.loadObject(9, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(10, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.loadObject(4, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(10, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new JumpInsnNode(IF_ACMPEQ, paramMatchedLabel));
		instructions.add(new GotoNode(nextMethodLabel));
		instructions.add(paramMatchedLabel);
		instructions.add(AbstractInsnNode.IINC(10, 1, localVariables, 4));
		instructions.add(new GotoNode(paramLoopStartLabel));
		instructions.add(returnMethodLabel);
		instructions.add(AbstractInsnNode.loadObject(8, localVariables, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(nextMethodLabel);
		instructions.add(AbstractInsnNode.IINC(7, 1, localVariables, 4));
		instructions.add(new GotoNode(methodLoopStartLabel));
		instructions.add(returnNullLabel);
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(InsnNode.of(ARETURN));
	}

	private String encodeMemberSignature(MemberContext memberContext, Map<String, Integer> classIndexes, Map<Integer, Long> encryptedKeys) {
		StringBuilder signatureBuilder = new StringBuilder();
		String classInternalName = memberContext.getContextClass().getInternalName();
		Integer classIndex = classIndexes.get(classInternalName.replace('/', '.'));
		if (classIndex != null) {
			signatureBuilder.append(String.valueOf(Long.toString(encryptedKeys.get(classIndex), 36)));
		} else {
			signatureBuilder.append(classInternalName);
		}

		signatureBuilder.append('\b');
		signatureBuilder.append(memberContext.getMemberNameDisplay());
		if (!memberContext.isField()) {
			signatureBuilder.append('\b');
			String methodDescriptor = memberContext.getMemberDescriptor();

			for (String parameterTypeName : TypeWriter.getParameterTypeNames(methodDescriptor)) {
				this.appendEncodedTypeRef(parameterTypeName, signatureBuilder, classIndexes, encryptedKeys, true);
			}

			this.appendEncodedTypeRef(MethodSignature.extractReturnDescriptor(methodDescriptor), signatureBuilder, classIndexes, encryptedKeys, true);
		} else {
			signatureBuilder.append('\b');
			String fieldDescriptor = memberContext.getMemberDescriptor();
			this.appendEncodedTypeRef(fieldDescriptor, signatureBuilder, classIndexes, encryptedKeys, false);
		}

		return signatureBuilder.toString();
	}

	public AbstractMethodCallWriter genIndyBootstrapMethod(
		boolean makePublic,
		ClassNode classNode,
		AbstractMethodCallWriter callsiteTargetWriter,
		Inheritance inheritance,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[1];
		LocalVariableTable localVariableTable = new LocalVariableTable(
			true, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", 5
		);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		AtomicInt maxLocals = new AtomicInt(0);
		AtomicInt maxStack = new AtomicInt(0);
		this.genIndyBootstrapCode(instructions, localVariableTable, callsiteTargetWriter, tryCatchBlocks, typeWriters, maxStack, maxLocals, constantPoolManager, workspace, classProvider);
		return this.createGeneratedMethod(
			classNode,
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
			instructions,
			maxStack.get(),
			maxLocals.get(),
			localVariableTable,
			tryCatchBlocks,
			makePublic,
			typeWriters,
			inheritance,
			workspace
		);
	}

	public MethodCallWriter genMethodInvokeCall(ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, AbstractWorkspace workspace, ClassProvider classProvider) {
		return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", typeWriters, workspace, classProvider);
	}

	public Map<Types, Integer> assignIdsToTypes(MultiMap<InstructionListNode, MemberContext> referencesByInstruction, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		int nextTypeId = 0;
		LinkedHashMap<Types, Integer> idByType = new LinkedHashMap<>();
		if (referencesByInstruction != null) {
			HashSet<MemberContext> seenMemberContexts = GlobalUtils.newHashSet();

			for (Entry<InstructionListNode, List<MemberContext>> instructionEntry : referencesByInstruction.entrySet()) {
				InstructionListNode instructionNode = instructionEntry.getKey();

				for (MemberContext memberContext : instructionEntry.getValue()) {
					if (seenMemberContexts.add(memberContext)) {
						AbstractClassNode contextClass = memberContext.getContextClass();
						if (!idByType.containsKey(contextClass)) {
							idByType.put(contextClass, INTEGER_CACHE.valueOf(nextTypeId++));
						}

						AbstractClassNode instructionClass = instructionNode.getClassNode();
						Integer innerClassOrdinal = instructionClass.hasInnerClassOrdinal() ? instructionClass.getInnerClassOrdinal() : null;
						if (!memberContext.isField()) {
							String memberDescriptor = memberContext.getMemberDescriptor();

							for (String parameterTypeName : TypeWriter.getParameterTypeNames(memberDescriptor)) {
								Types parameterType = this.getTypeFromDescriptor(parameterTypeName, innerClassOrdinal, classProvider, phantomClassManager);
								if (!idByType.containsKey(parameterType)) {
									idByType.put(parameterType, nextTypeId++);
								}
							}

							String returnDescriptor = MethodSignature.extractReturnDescriptor(memberDescriptor);
							Types returnType = this.getTypeFromDescriptor(returnDescriptor, innerClassOrdinal, classProvider, phantomClassManager);
							if (!idByType.containsKey(returnType)) {
								idByType.put(returnType, nextTypeId++);
							}
						} else {
							String fieldDescriptor = memberContext.getMemberDescriptor();
							Types fieldType = this.getTypeFromDescriptor(fieldDescriptor, innerClassOrdinal, classProvider, phantomClassManager);
							if (!idByType.containsKey(fieldType)) {
								idByType.put(fieldType, nextTypeId++);
							}
						}
					}
				}
			}
		}

		AbstractClassNode objectClass = classProvider.getClassNode("java/lang/Object");
		if (!idByType.containsKey(objectClass)) {
			idByType.put(objectClass, nextTypeId++);
		}

		return idByType;
	}

	private void genClassResolverMethodCode(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariables,
		AbstractMethodCallWriter idDecryptorWriter,
		FieldTypeWriter decodedNameField,
		FieldTypeWriter encodedValueField,
		List<TypeWriter> typeWriters,
		TryCatchBlockInfo[] tryCatchBlocks,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		maxLocals.set(this.useEnhanced.get() ? 8 : 6);
		maxStack.set(4);
		int secondLongSlot = this.useEnhanced.get() ? 2 : 0;
		int indexSlot = secondLongSlot + 2;
		int resolvedClassSlot = indexSlot + 1;
		int arrayElementSlot = resolvedClassSlot + 1;
		int exceptionSlot = arrayElementSlot + 1;
		LabelNode tryStartLabel = new LabelNode(true, 128);
		LabelNode tryEndLabel = new LabelNode(true, 128);
		LabelNode classElementLabel = new LabelNode(true, 1);
		LabelNode returnLabel = new LabelNode(true, 1);
		LabelNode catchHandlerLabel = new LabelNode(true, 128);
		ClassInfoWriter exceptionClass = constantPoolManager.getOrCreateClassInfo("java/lang/Exception", typeWriters);
		tryCatchBlocks[0] = new TryCatchBlockInfo(exceptionClass, tryStartLabel, tryEndLabel, catchHandlerLabel);
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(resolvedClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadLong(0, localVariables, 4));
		if (this.useEnhanced.get()) {
			instructions.add(AbstractInsnNode.loadLong(secondLongSlot, localVariables, 4));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, idDecryptorWriter));
		instructions.add(AbstractInsnNode.storeInt(indexSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encodedValueField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.storeObject(arrayElementSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(arrayElementSlot, localVariables, 4));
		instructions.add(tryStartLabel);
		ClassInfoWriter stringClass = constantPoolManager.getOrCreateClassInfo("java/lang/String", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(INSTANCEOF, stringClass));
		instructions.add(new JumpInsnNode(IFEQ, classElementLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, decodedNameField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		MethodCallWriter forNameMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, forNameMethod));
		instructions.add(AbstractInsnNode.storeObject(resolvedClassSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encodedValueField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(resolvedClassSlot, localVariables, 4));
		instructions.add(InsnNode.of(AASTORE));
		if (Options.ZKM_OBFUSCATE_REFERENCES_OBFUSCATE_EXCEPTIONS) {
			instructions.add(tryEndLabel);
		}

		instructions.add(new GotoNode(returnLabel));
		instructions.add(classElementLabel);
		instructions.add(AbstractInsnNode.loadObject(arrayElementSlot, localVariables, 4));
		ClassInfoWriter classClass = constantPoolManager.getOrCreateClassInfo("java/lang/Class", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, classClass));
		instructions.add(AbstractInsnNode.storeObject(resolvedClassSlot, localVariables, 4));
		if (!Options.ZKM_OBFUSCATE_REFERENCES_OBFUSCATE_EXCEPTIONS) {
			instructions.add(tryEndLabel);
		}

		instructions.add(new GotoNode(returnLabel));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(exceptionSlot, localVariables, 4));
		ClassInfoWriter runtimeExceptionClass = constantPoolManager.getOrCreateClassInfo("java/lang/RuntimeException", typeWriters);
		instructions.add(new NewNode(runtimeExceptionClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(exceptionSlot, localVariables, 4));
		MethodCallWriter toStringMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Exception", "toString", "()Ljava/lang/String;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, toStringMethod));
		MethodCallWriter runtimeExceptionConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionConstructorMethod));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(returnLabel);
		instructions.add(AbstractInsnNode.loadObject(resolvedClassSlot, localVariables, 4));
		instructions.add(InsnNode.of(ARETURN));
	}

	private void genIdDecryptorCode(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariables,
		FieldTypeWriter encodedValueField,
		FieldTypeWriter decodedNameField,
		int[] digitTable,
		List<TypeWriter> typeWriters,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		maxLocals.set(this.useEnhanced.get() ? 11 : 9);
		maxStack.set(6);
		int secondKeySlot = this.useEnhanced.get() ? 2 : 0;
		int indexSlot = secondKeySlot + 2;
		int elementSlot = indexSlot + 1;
		int offsetSlot = elementSlot + 1;
		int keyBytesSlot = offsetSlot + 1;
		int charBufferSlot = keyBytesSlot + 1;
		int charIndexSlot = charBufferSlot + 1;
		int keyByteSlot = charIndexSlot + 1;
		LabelNode uncachedLabel = new LabelNode(true, 1);
		LabelNode notStringLabel = new LabelNode(true, 1);
		LabelNode stringElementLabel = new LabelNode(true, 1);
		LabelNode caseLabel0 = new LabelNode(true, 1);
		LabelNode caseLabel1 = new LabelNode(true, 1);
		LabelNode caseLabel2 = new LabelNode(true, 1);
		LabelNode caseLabel3 = new LabelNode(true, 1);
		LabelNode caseLabel4 = new LabelNode(true, 1);
		LabelNode caseLabel5 = new LabelNode(true, 1);
		LabelNode caseLabel6 = new LabelNode(true, 1);
		LabelNode caseLabel7 = new LabelNode(true, 1);
		LabelNode caseLabel8 = new LabelNode(true, 1);
		LabelNode caseLabel9 = new LabelNode(true, 1);
		LabelNode caseLabel10 = new LabelNode(true, 1);
		LabelNode caseLabel11 = new LabelNode(true, 1);
		LabelNode caseLabel12 = new LabelNode(true, 1);
		LabelNode caseLabel13 = new LabelNode(true, 1);
		LabelNode caseLabel14 = new LabelNode(true, 1);
		LabelNode caseLabel15 = new LabelNode(true, 1);
		LabelNode caseLabel16 = new LabelNode(true, 1);
		LabelNode caseLabel17 = new LabelNode(true, 1);
		LabelNode caseLabel18 = new LabelNode(true, 1);
		LabelNode caseLabel19 = new LabelNode(true, 1);
		LabelNode caseLabel20 = new LabelNode(true, 1);
		LabelNode caseLabel21 = new LabelNode(true, 1);
		LabelNode caseLabel22 = new LabelNode(true, 1);
		LabelNode caseLabel23 = new LabelNode(true, 1);
		LabelNode caseLabel24 = new LabelNode(true, 1);
		LabelNode caseLabel25 = new LabelNode(true, 1);
		LabelNode caseLabel26 = new LabelNode(true, 1);
		LabelNode caseLabel27 = new LabelNode(true, 1);
		LabelNode caseLabel28 = new LabelNode(true, 1);
		LabelNode caseLabel29 = new LabelNode(true, 1);
		LabelNode caseLabel30 = new LabelNode(true, 1);
		LabelNode caseLabel31 = new LabelNode(true, 1);
		LabelNode caseLabel32 = new LabelNode(true, 1);
		LabelNode caseLabel33 = new LabelNode(true, 1);
		LabelNode caseLabel34 = new LabelNode(true, 1);
		LabelNode caseLabel35 = new LabelNode(true, 1);
		LabelNode caseLabel36 = new LabelNode(true, 1);
		LabelNode caseLabel37 = new LabelNode(true, 1);
		LabelNode caseLabel38 = new LabelNode(true, 1);
		LabelNode caseLabel39 = new LabelNode(true, 1);
		LabelNode caseLabel40 = new LabelNode(true, 1);
		LabelNode caseLabel41 = new LabelNode(true, 1);
		LabelNode caseLabel42 = new LabelNode(true, 1);
		LabelNode caseLabel43 = new LabelNode(true, 1);
		LabelNode caseLabel44 = new LabelNode(true, 1);
		LabelNode caseLabel45 = new LabelNode(true, 1);
		LabelNode caseLabel46 = new LabelNode(true, 1);
		LabelNode caseLabel47 = new LabelNode(true, 1);
		LabelNode caseLabel48 = new LabelNode(true, 1);
		LabelNode caseLabel49 = new LabelNode(true, 1);
		LabelNode caseLabel50 = new LabelNode(true, 1);
		LabelNode caseLabel51 = new LabelNode(true, 1);
		LabelNode caseLabel52 = new LabelNode(true, 1);
		LabelNode caseLabel53 = new LabelNode(true, 1);
		LabelNode caseLabel54 = new LabelNode(true, 1);
		LabelNode caseLabel55 = new LabelNode(true, 1);
		LabelNode caseLabel56 = new LabelNode(true, 1);
		LabelNode caseLabel57 = new LabelNode(true, 1);
		LabelNode caseLabel58 = new LabelNode(true, 1);
		LabelNode caseLabel59 = new LabelNode(true, 1);
		LabelNode caseLabel60 = new LabelNode(true, 1);
		LabelNode caseLabel61 = new LabelNode(true, 1);
		LabelNode caseLabel62 = new LabelNode(true, 1);
		LabelNode defaultCaseLabel = new LabelNode(true, 1);
		LabelNode caseMergeLabel = new LabelNode(true, 1);
		LabelNode keyByteLoopBodyLabel = new LabelNode(true, 1);
		LabelNode keyByteLoopContinueLabel = new LabelNode(true, 1);
		LabelNode keyByteLoopConditionLabel = new LabelNode(true, 1);
		LabelNode charBufferSetupLabel = new LabelNode(true, 1);
		LabelNode charLoopBodyLabel = new LabelNode(true, 1);
		LabelNode charLoopConditionLabel = new LabelNode(true, 1);
		LabelNode zeroKeyLabel = new LabelNode(true, 1);
		LabelNode charLoopEntryLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadLong(0, localVariables, 4));
		if (this.useEnhanced.get()) {
			instructions.add(AbstractInsnNode.loadLong(secondKeySlot, localVariables, 4));
			instructions.add(AbstractInsnNode.newInt(48));
			instructions.add(InsnNode.of(LSHL));
			instructions.add(AbstractInsnNode.loadLong(secondKeySlot, localVariables, 4));
			instructions.add(InsnNode.of(LOR));
			instructions.add(InsnNode.of(LXOR));
			instructions.add(AbstractInsnNode.storeLong(0, localVariables, 4));
			instructions.add(AbstractInsnNode.loadLong(0, localVariables, 4));
		}

		instructions.add(AbstractInsnNode.newInt(46));
		instructions.add(InsnNode.of(LUSHR));
		instructions.add(InsnNode.of(L2I));
		instructions.add(AbstractInsnNode.storeInt(indexSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, decodedNameField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new JumpInsnNode(IFNULL, uncachedLabel));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(IRETURN));
		instructions.add(uncachedLabel);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, encodedValueField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.storeObject(elementSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(elementSlot, localVariables, 4));
		ClassInfoWriter instanceOfStringClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/String", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(INSTANCEOF, instanceOfStringClassInfo));
		instructions.add(new JumpInsnNode(IFNE, stringElementLabel));
		instructions.add(notStringLabel);
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(IRETURN));
		instructions.add(stringElementLabel);
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(offsetSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadLong(0, localVariables, 4));
		instructions.add(AbstractInsnNode.newInt(42));
		instructions.add(InsnNode.of(LUSHR));
		LongNode sixBitMaskConstant = constantPoolManager.getOrCreateLong(63L, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(LDC2_W, sixBitMaskConstant));
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(L2I));
		LabelNode[] caseLabels = new LabelNode[]{
			caseLabel0,
			caseLabel1,
			caseLabel2,
			caseLabel3,
			caseLabel4,
			caseLabel5,
			caseLabel6,
			caseLabel7,
			caseLabel8,
			caseLabel9,
			caseLabel10,
			caseLabel11,
			caseLabel12,
			caseLabel13,
			caseLabel14,
			caseLabel15,
			caseLabel16,
			caseLabel17,
			caseLabel18,
			caseLabel19,
			caseLabel20,
			caseLabel21,
			caseLabel22,
			caseLabel23,
			caseLabel24,
			caseLabel25,
			caseLabel26,
			caseLabel27,
			caseLabel28,
			caseLabel29,
			caseLabel30,
			caseLabel31,
			caseLabel32,
			caseLabel33,
			caseLabel34,
			caseLabel35,
			caseLabel36,
			caseLabel37,
			caseLabel38,
			caseLabel39,
			caseLabel40,
			caseLabel41,
			caseLabel42,
			caseLabel43,
			caseLabel44,
			caseLabel45,
			caseLabel46,
			caseLabel47,
			caseLabel48,
			caseLabel49,
			caseLabel50,
			caseLabel51,
			caseLabel52,
			caseLabel53,
			caseLabel54,
			caseLabel55,
			caseLabel56,
			caseLabel57,
			caseLabel58,
			caseLabel59,
			caseLabel60,
			caseLabel61,
			caseLabel62
		};
		instructions.add(new TableSwitchNode(defaultCaseLabel, 0, caseLabels.length - 1, caseLabels));

		for (int caseIndex = 0; caseIndex < caseLabels.length; caseIndex++) {
			instructions.add(caseLabels[caseIndex]);
			instructions.add(AbstractInsnNode.newInt(digitTable[caseIndex]));
			instructions.add(new GotoNode(caseMergeLabel));
		}

		instructions.add(defaultCaseLabel);
		instructions.add(AbstractInsnNode.newInt(digitTable[63]));
		instructions.add(caseMergeLabel);
		instructions.add(AbstractInsnNode.storeInt(offsetSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(new NewArrayNode(10));
		instructions.add(AbstractInsnNode.storeObject(keyBytesSlot, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(charBufferSlot, localVariables, 4));
		instructions.add(new GotoNode(keyByteLoopConditionLabel));
		instructions.add(keyByteLoopBodyLabel);
		instructions.add(AbstractInsnNode.newInt(7));
		instructions.add(InsnNode.of(ICONST_5));
		instructions.add(AbstractInsnNode.loadInt(charBufferSlot, localVariables, 4));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(InsnNode.of(IMUL));
		instructions.add(AbstractInsnNode.storeInt(charIndexSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadLong(0, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(charIndexSlot, localVariables, 4));
		instructions.add(InsnNode.of(LUSHR));
		LongNode sevenBitMaskConstant = constantPoolManager.getOrCreateLong(127L, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(LDC2_W, sevenBitMaskConstant));
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(L2I));
		instructions.add(AbstractInsnNode.storeInt(keyByteSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(keyByteSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(offsetSlot, localVariables, 4));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(AbstractInsnNode.storeInt(keyByteSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(keyByteSlot, localVariables, 4));
		instructions.add(new JumpInsnNode(IFGE, keyByteLoopContinueLabel));
		instructions.add(AbstractInsnNode.IINC(keyByteSlot, 128, localVariables, 4));
		instructions.add(keyByteLoopContinueLabel);
		instructions.add(AbstractInsnNode.loadObject(keyBytesSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(charBufferSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(keyByteSlot, localVariables, 4));
		instructions.add(InsnNode.of(IASTORE));
		instructions.add(AbstractInsnNode.IINC(charBufferSlot, 1, localVariables, 4));
		instructions.add(keyByteLoopConditionLabel);
		instructions.add(AbstractInsnNode.loadInt(charBufferSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.newInt(6));
		instructions.add(new JumpInsnNode(IF_ICMPLT, keyByteLoopBodyLabel));
		instructions.add(charBufferSetupLabel);
		instructions.add(AbstractInsnNode.loadObject(elementSlot, localVariables, 4));
		ClassInfoWriter checkCastStringClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/String", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, checkCastStringClassInfo));
		MethodCallWriter toCharArrayMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/String", "toCharArray", "()[C", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, toCharArrayMethodRef));
		instructions.add(AbstractInsnNode.storeObject(charBufferSlot, localVariables, 4));
		instructions.add(charLoopEntryLabel);
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(charIndexSlot, localVariables, 4));
		instructions.add(new GotoNode(charLoopConditionLabel));
		instructions.add(charLoopBodyLabel);
		instructions.add(AbstractInsnNode.loadObject(keyBytesSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(charIndexSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(keyBytesSlot, localVariables, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(InsnNode.of(IREM));
		instructions.add(InsnNode.of(IALOAD));
		instructions.add(AbstractInsnNode.storeInt(keyByteSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(keyByteSlot, localVariables, 4));
		instructions.add(new JumpInsnNode(IFEQ, zeroKeyLabel));
		instructions.add(AbstractInsnNode.loadObject(charBufferSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(charIndexSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(charBufferSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(charIndexSlot, localVariables, 4));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(AbstractInsnNode.loadInt(keyByteSlot, localVariables, 4));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(AbstractInsnNode.IINC(charIndexSlot, 1, localVariables, 4));
		instructions.add(charLoopConditionLabel);
		instructions.add(AbstractInsnNode.loadInt(charIndexSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(charBufferSlot, localVariables, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(new JumpInsnNode(IF_ICMPLT, charLoopBodyLabel));
		instructions.add(zeroKeyLabel);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, decodedNameField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		ClassInfoWriter newStringClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/String", typeWriters);
		instructions.add(new NewNode(newStringClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(charBufferSlot, localVariables, 4));
		MethodCallWriter stringConstructorMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/String", "<init>", "([C)V", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringConstructorMethodRef));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(IRETURN));
	}

	public AbstractMethodCallWriter genIndyInvokeTargetMethod(
		boolean makePublic,
		ClassNode classNode,
		AbstractMethodCallWriter methodHandleResolverWriter,
		Inheritance inheritance,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		int localSlotCount = this.isEnhanced() ? 11 : 9;
		LocalVariableTable localVariableTable = new LocalVariableTable(
			true,
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/Object;",
			localSlotCount
		);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		AtomicInt maxLocals = new AtomicInt(0);
		AtomicInt maxStack = new AtomicInt(0);
		this.genIndyInvokeTargetCode(instructions, localVariableTable, tryCatchBlocks, methodHandleResolverWriter, typeWriters, maxStack, maxLocals, constantPoolManager, workspace, classProvider);
		return this.createHelperMethod(
			classNode,
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/Object;",
			instructions,
			maxStack.get(),
			maxLocals.get(),
			localVariableTable,
			tryCatchBlocks,
			makePublic,
			typeWriters,
			null,
			inheritance,
			workspace
		);
	}

	public MethodCallWriter genFieldSetterCall(FieldTypeWriter fieldTypeWriter, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, AbstractWorkspace workspace, ClassProvider classProvider) {
		String fieldDescriptor = fieldTypeWriter.getDescriptor();
		if (!fieldDescriptor.startsWith("[") && (!fieldDescriptor.startsWith("L") || !fieldDescriptor.endsWith(";"))) {
			if (fieldDescriptor.equals("Z")) {
				return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "setBoolean", "(Ljava/lang/Object;Z)V", typeWriters, workspace, classProvider);
			} else if (fieldDescriptor.equals("B")) {
				return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "setByte", "(Ljava/lang/Object;B)V", typeWriters, workspace, classProvider);
			} else if (fieldDescriptor.equals("S")) {
				return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "setShort", "(Ljava/lang/Object;S)V", typeWriters, workspace, classProvider);
			} else if (fieldDescriptor.equals("C")) {
				return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "setChar", "(Ljava/lang/Object;C)V", typeWriters, workspace, classProvider);
			} else if (fieldDescriptor.equals("I")) {
				return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "setInt", "(Ljava/lang/Object;I)V", typeWriters, workspace, classProvider);
			} else if (fieldDescriptor.equals("J")) {
				return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "setLong", "(Ljava/lang/Object;J)V", typeWriters, workspace, classProvider);
			} else if (fieldDescriptor.equals("F")) {
				return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "setFloat", "(Ljava/lang/Object;F)V", typeWriters, workspace, classProvider);
			} else {
				return fieldDescriptor.equals("D")
					? constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "setDouble", "(Ljava/lang/Object;D)V", typeWriters, workspace, classProvider)
					: constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", typeWriters, workspace, classProvider);
			}
		} else {
			return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", typeWriters, workspace, classProvider);
		}
	}

	private void genDataPopulationCode(
		String descriptor,
		List<ObfuscateReferencesBuilder.DataPopulationMethodBody> methodBodies,
		Map<Types, Integer> typeIds,
		Map<MemberContext, Integer> memberIds,
		int[] saltTable,
		Map<Integer, Long> encryptedKeys,
		FieldTypeWriter objectArrayField,
		FieldTypeWriter stringArrayField,
		ParameterRotateEntry parameterRotateEntry,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		int totalEntryCount = typeIds.size() + memberIds.size();
		int processedEntryCount = 0;
		int codeSize = 0;
		ObfuscateReferencesBuilder.DataPopulationMethodBody methodBody = new ObfuscateReferencesBuilder.DataPopulationMethodBody(totalEntryCount, descriptor, true);
		ArrayList<AbstractInsnNode> instructions = methodBody.getInstructions();
		methodBodies.add(methodBody);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
		codeSize += 3;
		HashMap<String, Integer> classIdsByClassName = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(typeIds.size()));
		AtomicReference<Long> scrambledIdReference = new AtomicReference<>();

		for (Entry<Types, Integer> typeEntry : typeIds.entrySet()) {
			Types type = typeEntry.getKey();
			String className = type.getInternalName().replace('/', '.');
			Integer typeId = typeEntry.getValue();
			classIdsByClassName.put(className, typeId);
			if (parameterRotateEntry != null && instructions.size() == 1) {
				AtomicInt typeRotationCodeSize = new AtomicInt();
				instructions.addAll(0, parameterRotateEntry.buildSaltXorPrologue(methodBody.getLocalVariableTable(), methodBodies.size() - 1, typeRotationCodeSize, typeWriters, constantPoolManager, 4));
				codeSize += typeRotationCodeSize.get();
			}

			instructions.add(InsnNode.of(DUP));
			codeSize = ++codeSize + AbstractInsnNode.encryptIntConstant(typeId, instructions, constantPoolManager, typeWriters);
			if (type.isPrimitive()) {
				encryptedKeys.put(typeId, this.scrambleId(typeId));
				PrimitiveNode primitiveNode = (PrimitiveNode)type;
				FieldTypeWriter typeFieldRef = constantPoolManager.getOrCreateFieldRef(primitiveNode.getWrapperName(), PrimitiveNode.getTypeFieldName(), PrimitiveNode.getClassDescriptor(), typeWriters, workspace, classProvider, true);
				instructions.add(new WideConstantPoolInsnNode(GETSTATIC, typeFieldRef));
				instructions.add(InsnNode.of(AASTORE));
				instructions.add(new WideConstantPoolInsnNode(GETSTATIC, stringArrayField));
				codeSize += 7;
				codeSize += AbstractInsnNode.encryptIntConstant(typeId, instructions, constantPoolManager, typeWriters);
				StringNode wrapperNameConstant = constantPoolManager.createString(primitiveNode.getWrapperName(), typeWriters);
				instructions.add(new WideConstantPoolInsnNode(LDC_W, wrapperNameConstant));
				instructions.add(InsnNode.of(AASTORE));
				codeSize += 4;
			} else {
				String encryptedClassName = this.encryptStringAndGenKey(className, typeId, scrambledIdReference, saltTable);
				encryptedKeys.put(typeId, scrambledIdReference.get());
				StringNode encryptedClassNameConstant = constantPoolManager.createString(encryptedClassName, typeWriters);
				instructions.add(new WideConstantPoolInsnNode(LDC_W, encryptedClassNameConstant));
				codeSize += 3;
				instructions.add(InsnNode.of(AASTORE));
				codeSize++;
			}

			processedEntryCount++;
			if (codeSize > MAX_CONSTANT_POOL_SIZE) {
				instructions.add(InsnNode.of(RETURN));
				if (processedEntryCount < totalEntryCount) {
					methodBody = new ObfuscateReferencesBuilder.DataPopulationMethodBody(totalEntryCount, descriptor, true);
					instructions = methodBody.getInstructions();
					methodBodies.add(methodBody);
					instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
					codeSize = 3;
				} else {
					instructions = null;
					codeSize = 0;
				}
			}
		}

		for (Entry<MemberContext, Integer> memberEntry : memberIds.entrySet()) {
			MemberContext memberContext = memberEntry.getKey();
			int memberId = memberEntry.getValue();
			String encodedSignature = this.encodeMemberSignature(memberContext, classIdsByClassName, encryptedKeys);
			String encryptedSignature = this.encryptStringAndGenKey(encodedSignature, memberId, scrambledIdReference, saltTable);
			encryptedKeys.put(memberId, scrambledIdReference.get());
			StringNode encryptedSignatureConstant = constantPoolManager.createString(encryptedSignature, typeWriters);
			if (parameterRotateEntry != null && instructions.size() == 1) {
				AtomicInt memberRotationCodeSize = new AtomicInt();
				instructions.addAll(0, parameterRotateEntry.buildSaltXorPrologue(methodBody.getLocalVariableTable(), methodBodies.size() - 1, memberRotationCodeSize, typeWriters, constantPoolManager, 4));
				codeSize += memberRotationCodeSize.get();
			}

			instructions.add(InsnNode.of(DUP));
			codeSize += AbstractInsnNode.encryptIntConstant(memberId, instructions, constantPoolManager, typeWriters);
			instructions.add(new WideConstantPoolInsnNode(LDC_W, encryptedSignatureConstant));
			instructions.add(InsnNode.of(AASTORE));
			codeSize += 5;
			processedEntryCount++;
			if (codeSize > MAX_CONSTANT_POOL_SIZE) {
				instructions.add(InsnNode.of(RETURN));
				if (processedEntryCount < totalEntryCount) {
					methodBody = new ObfuscateReferencesBuilder.DataPopulationMethodBody(totalEntryCount, descriptor, true);
					instructions = methodBody.getInstructions();
					methodBodies.add(methodBody);
					instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
					codeSize = 3;
				} else {
					instructions = null;
					codeSize = 0;
				}
			}
		}

		if (instructions != null) {
			instructions.add(InsnNode.of(RETURN));
		}
	}

	private void genIndyInvokeTargetCode(
		ArrayList<AbstractInsnNode> instructions,
		LocalVariableTable localVariables,
		TryCatchBlockInfo[] tryCatchBlocks,
		AbstractMethodCallWriter methodHandleResolverWriter,
		List<TypeWriter> typeWriters,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		Workspace workspace,
		ClassProvider classProvider
	) {
		maxLocals.set(this.isEnhanced() ? 11 : 9);
		maxStack.set(6);
		int secondKeySlot = this.isEnhanced() ? 9 : 8;
		instructions.add(AbstractInsnNode.loadObject(4, localVariables, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(AbstractInsnNode.newInt(this.isEnhanced() ? 2 : 1));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(AbstractInsnNode.storeInt(5, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(4, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(5, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		ClassInfoWriter longClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Long", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, longClassInfo));
		MethodCallWriter longValueMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Long", "longValue", "()J", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, longValueMethodRef));
		instructions.add(AbstractInsnNode.storeLong(6, localVariables, 4));
		if (this.isEnhanced()) {
			instructions.add(AbstractInsnNode.loadObject(4, localVariables, 4));
			instructions.add(AbstractInsnNode.IINC(5, 1, localVariables, 4));
			instructions.add(AbstractInsnNode.loadInt(5, localVariables, 4));
			instructions.add(InsnNode.of(AALOAD));
			instructions.add(new WideConstantPoolInsnNode(CHECKCAST, longClassInfo));
			MethodCallWriter secondLongValueMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Long", "longValue", "()J", typeWriters, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, secondLongValueMethodRef));
			instructions.add(AbstractInsnNode.storeLong(secondKeySlot, localVariables, 4));
		}

		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(1, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(2, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(3, localVariables, 4));
		instructions.add(AbstractInsnNode.loadLong(6, localVariables, 4));
		if (this.isEnhanced()) {
			instructions.add(AbstractInsnNode.loadLong(secondKeySlot, localVariables, 4));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, methodHandleResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(8, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(1, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(8, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(3, localVariables, 4));
		MethodCallWriter explicitCastArgumentsMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles",
			"explicitCastArguments",
			"(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			typeWriters,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, explicitCastArgumentsMethodRef));
		MethodCallWriter setTargetMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, setTargetMethodRef));
		instructions.add(AbstractInsnNode.loadObject(8, localVariables, 4));
		instructions.add(AbstractInsnNode.ldcClass("[Ljava/lang/Object;", constantPoolManager, typeWriters));
		instructions.add(AbstractInsnNode.loadObject(4, localVariables, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		MethodCallWriter asSpreaderMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandle", "asSpreader", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", typeWriters, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, asSpreaderMethodRef));
		instructions.add(AbstractInsnNode.loadObject(4, localVariables, 4));
		MethodCallWriter invokeMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodHandle", "invoke", "([Ljava/lang/Object;)Ljava/lang/Object;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, invokeMethodRef));
		instructions.add(InsnNode.of(ARETURN));
	}

	public AbstractMethodCallWriter createGeneratedMethod(
		ClassNode classNode,
		String descriptor,
		ArrayList<AbstractInsnNode> instructions,
		int maxStack,
		int maxLocals,
		LocalVariableTable localVariables,
		TryCatchBlockInfo[] tryCatchBlocks,
		boolean makePublic,
		List<TypeWriter> typeWriters,
		Inheritance inheritance,
		Workspace workspace
	) {
		return this.createHelperMethod(classNode, descriptor, instructions, maxStack, maxLocals, localVariables, tryCatchBlocks, makePublic, typeWriters, null, inheritance, workspace);
	}

	public void appendEncodedTypeRef(String typeRef, StringBuilder signature, Map<String, Integer> classIds, Map<Integer, Long> scrambledIds, boolean appendSeparator) {
		String className = TypeWriter.getClassInternalName(typeRef).replace('/', '.');
		Integer classId = classIds.get(className);
		if (classId != null) {
			Long scrambledId = scrambledIds.get(classId);
			signature.append(String.valueOf(Long.toString(scrambledId, 36)));
		} else {
			signature.append(className);
		}

		if (appendSeparator) {
			signature.append('\b');
		}
	}

	public MethodCallWriter genGetTargetExceptionCall(ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, AbstractWorkspace workspace, ClassProvider classProvider) {
		return constantPoolManager.getOrCreateMethodRef("java/lang/reflect/InvocationTargetException", "getTargetException", "()Ljava/lang/Throwable;", typeWriters, workspace, classProvider);
	}

	public AbstractMethodCallWriter genFieldResolverHelper(
		ClassNode classNode, Inheritance inheritance, Workspace workspace, ClassProvider classProvider, List<TypeWriter> typeWriters, ConstantPoolManager constantPoolManager
	) {
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		LocalVariableTable localVariableTable = new LocalVariableTable(true, "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/reflect/Field;", 7);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		AtomicInt maxLocals = new AtomicInt(0);
		AtomicInt maxStack = new AtomicInt(0);
		this.genDeclaredFieldLookupCode(instructions, localVariableTable, typeWriters, maxStack, maxLocals, constantPoolManager, workspace, classProvider);
		return this.createGeneratedMethod(
			classNode,
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/reflect/Field;",
			instructions,
			maxStack.get(),
			maxLocals.get(),
			localVariableTable,
			tryCatchBlocks,
			false,
			typeWriters,
			inheritance,
			workspace
		);
	}

	public AbstractMethodCallWriter genInterfaceMethodResolver(
		ClassNode classNode, AbstractMethodCallWriter declaredMethodFinderWriter, Inheritance inheritance, Workspace workspace, ClassProvider classProvider, List<TypeWriter> typeWriters, ConstantPoolManager constantPoolManager
	) {
		AtomicInt maxLocals = new AtomicInt(8);
		AtomicInt maxStack = new AtomicInt(5);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		LocalVariableTable localVariableTable = new LocalVariableTable(
			true, "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;I[Ljava/lang/Class;)Ljava/lang/reflect/Method;", 10
		);
		AtomicReference<InstructionListNode> instructionListReference = new AtomicReference<>();
		AbstractMethodCallWriter methodReference = this.createHelperMethod(
			classNode,
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;I[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
			new ArrayList<>(),
			maxStack.get(),
			maxLocals.get(),
			localVariableTable,
			tryCatchBlocks,
			false,
			typeWriters,
			instructionListReference,
			inheritance,
			workspace
		);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.genInterfaceMethodResolverCode(instructions, localVariableTable, methodReference, declaredMethodFinderWriter, typeWriters, constantPoolManager, workspace, classProvider);
		instructionListReference.get().setInstructions(instructions, "Reference Obfuscation");
		return methodReference;
	}

	public boolean isEnhanced() {
		return this.useEnhanced.get();
	}

	public AbstractMethodCallWriter genMethodHandleResolver(
		boolean isPublic,
		ClassNode classNode,
		int[] kindCodes,
		AbstractMethodCallWriter fieldResolverWriter,
		AbstractMethodCallWriter methodResolverWriter,
		Inheritance inheritance,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> types,
		ConstantPoolManager constantPoolManager
	) {
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[1];
		String methodDescriptor = this.isEnhanced()
			? "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;JJ)Ljava/lang/invoke/MethodHandle;"
			: "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/MethodHandle;";
		int localSlotCount = this.isEnhanced() ? 15 : 13;
		LocalVariableTable localVariableTable = new LocalVariableTable(true, methodDescriptor, localSlotCount);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		AtomicInt maxLocals = new AtomicInt(0);
		AtomicInt maxStack = new AtomicInt(0);
		this.genMethodHandleResolverCode(instructions, localVariableTable, tryCatchBlocks, kindCodes, fieldResolverWriter, methodResolverWriter, types, maxStack, maxLocals, constantPoolManager, workspace, classProvider);
		return this.createGeneratedMethod(
			classNode,
			this.isEnhanced()
				? "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;JJ)Ljava/lang/invoke/MethodHandle;"
				: "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;J)Ljava/lang/invoke/MethodHandle;",
			instructions,
			maxStack.get(),
			maxLocals.get(),
			localVariableTable,
			tryCatchBlocks,
			isPublic,
			types,
			inheritance,
			workspace
		);
	}

	public MemberContext getOrCreateMemberContext(MemberNode memberNode, AbstractClassNode contextClass) {
		MemberContext memberContext = this.memberEntryCache.get(memberNode, contextClass);
		if (memberContext == null) {
			memberContext = new MemberContext(memberNode, contextClass);
		}

		return memberContext;
	}

	private void genIndyBootstrapCode(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariableTable,
		AbstractMethodCallWriter callsiteTargetWriter,
		TryCatchBlockInfo[] tryCatchBlocks,
		List<TypeWriter> types,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		maxLocals.set(5);
		maxStack.set(7);
		LabelNode tryStartLabel = new LabelNode(true, 128);
		LabelNode tryEndLabel = new LabelNode(true, 128);
		LabelNode catchHandlerLabel = new LabelNode(true, 128);
		LabelNode returnLabel = new LabelNode(true, 1);
		ClassInfoWriter exceptionClass = constantPoolManager.getOrCreateClassInfo("java/lang/Exception", types);
		tryCatchBlocks[0] = new TryCatchBlockInfo(exceptionClass, tryStartLabel, tryEndLabel, catchHandlerLabel);
		ClassInfoWriter mutableCallSiteClass = constantPoolManager.getOrCreateClassInfo("java/lang/invoke/MutableCallSite", types);
		instructions.add(new NewNode(mutableCallSiteClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		MethodCallWriter callSiteConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodType;)V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, callSiteConstructorMethod));
		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 4));
		instructions.add(tryStartLabel);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		ResolvedMethodHandleWriter targetMethodHandle = constantPoolManager.getOrCreateMethodHandle(MethodHandleKind.REF_INVOKE_STATIC, callsiteTargetWriter, types);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, targetMethodHandle));
		instructions.add(AbstractInsnNode.ldcClass("[Ljava/lang/Object;", constantPoolManager, types));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		MethodCallWriter parameterCountMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "parameterCount", "()I", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, parameterCountMethod));
		MethodCallWriter asCollectorMethod = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandle", "asCollector", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;", types, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, asCollectorMethod));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(ICONST_4));
		ClassInfoWriter objectClass = constantPoolManager.getOrCreateClassInfo("java/lang/Object", types);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClass));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(0));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(1));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(2));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 4));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(3));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		instructions.add(InsnNode.of(AASTORE));
		MethodCallWriter insertArgumentsMethod = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles",
			"insertArguments",
			"(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, insertArgumentsMethod));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		MethodCallWriter explicitCastArgumentsMethod = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles",
			"explicitCastArguments",
			"(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, explicitCastArgumentsMethod));
		MethodCallWriter setTargetMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, setTargetMethod));
		instructions.add(tryEndLabel);
		instructions.add(new GotoNode(returnLabel));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 4));
		ClassInfoWriter runtimeExceptionClass = constantPoolManager.getOrCreateClassInfo("java/lang/RuntimeException", types);
		instructions.add(new NewNode(runtimeExceptionClass));
		instructions.add(InsnNode.of(DUP));
		ClassInfoWriter stringBuilderClass = constantPoolManager.getOrCreateClassInfo("java/lang/StringBuilder", types);
		instructions.add(new NewNode(stringBuilderClass));
		instructions.add(InsnNode.of(DUP));
		MethodCallWriter stringBuilderConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "<init>", "()V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringBuilderConstructorMethod));
		instructions.add(AbstractInsnNode.ldcString(constantPoolManager.getOwnerInternalName(), constantPoolManager, types, false));
		MethodCallWriter appendMethod = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendMethod));
		StringNode ownerSeparator = constantPoolManager.createStringNode(" : ", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, ownerSeparator));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendMethod));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendMethod));
		StringNode typeSeparator = constantPoolManager.createStringNode(" : ", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, typeSeparator));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendMethod));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		MethodCallWriter methodTypeToStringMethod = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "toString", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, methodTypeToStringMethod));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendMethod));
		MethodCallWriter stringBuilderToStringMethod = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderToStringMethod));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 4));
		MethodCallWriter runtimeExceptionConstructorMethod = constantPoolManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionConstructorMethod));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(returnLabel);
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		instructions.add(InsnNode.of(ARETURN));
	}

	public AbstractMethodCallWriter genMethodResolver(
		boolean isPublic,
		ClassNode classNode,
		AbstractMethodCallWriter indexResolverWriter,
		FieldTypeWriter stringArrayField,
		FieldTypeWriter objectArrayField,
		AbstractMethodCallWriter classResolverWriter,
		AbstractMethodCallWriter declaredMethodLookupWriter,
		AbstractMethodCallWriter methodLookupWriter,
		Inheritance inheritance,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> types,
		LongNode objectClassKey,
		ConstantPoolManager constantPoolManager
	) {
		String methodDescriptor = this.useEnhanced.get() ? "(JJ)Ljava/lang/reflect/Method;" : "(J)Ljava/lang/reflect/Method;";
		int localSlotCount = this.useEnhanced.get() ? 20 : 18;
		LocalVariableTable localVariableTable = new LocalVariableTable(true, methodDescriptor, localSlotCount);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		AtomicInt maxLocals = new AtomicInt(0);
		AtomicInt maxStack = new AtomicInt(0);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		this.genMethodResolverMethod(instructions, localVariableTable, indexResolverWriter, stringArrayField, objectArrayField, classResolverWriter, declaredMethodLookupWriter, methodLookupWriter, types, objectClassKey, maxStack, maxLocals, constantPoolManager, workspace, classProvider, true);
		return this.createGeneratedMethod(
			classNode,
			this.useEnhanced.get() ? "(JJ)Ljava/lang/reflect/Method;" : "(J)Ljava/lang/reflect/Method;",
			instructions,
			maxStack.get(),
			maxLocals.get(),
			localVariableTable,
			tryCatchBlocks,
			isPublic,
			types,
			inheritance,
			workspace
		);
	}

	public void injectLoaderToClinit(
		ClassNode classNode,
		FieldTypeWriter objectArrayField,
		FieldTypeWriter stringArrayField,
		MethodNode clinitMethodNode,
		Map<Types, Integer> idsByType,
		Map<MemberContext, Integer> memberIds,
		int[] saltTable,
		Map<Integer, Long> encryptionKeysById,
		Inheritance inheritance,
		ParameterRotateEntry parameterRotateEntry,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		int referenceCount = idsByType.size() + memberIds.size();
		int maxReferenceCount = 65535 - constantPoolManager.getConstantPoolSize() - typeWriters.size() - 100;
		if (!Options.analyzeReferenceDataClass && referenceCount > maxReferenceCount) {
			throw new ProcessingException(
				"Reference Obfuscation : ConstantPool length too large in class '"
					+ classNode.getRawSourcePath()
					+ "' : "
					+ referenceCount
					+ " > "
					+ maxReferenceCount
					+ " : "
					+ constantPoolManager.getConstantPoolSize()
					+ " : Consider limiting the number of references being obscured or using 'inReferencingClasses' setting. (A)"
			);
		} else {
			if (parameterRotateEntry != null) {
				parameterRotateEntry.initChangedMethod(clinitMethodNode, 1);
			}

			String dataPopulationDescriptor = parameterRotateEntry == null ? "()V" : "(J)V";
			ArrayList<ObfuscateReferencesBuilder.DataPopulationMethodBody> methodBodies = new ArrayList<>();
			this.genDataPopulationCode(dataPopulationDescriptor, methodBodies, idsByType, memberIds, saltTable, encryptionKeysById, objectArrayField, stringArrayField, parameterRotateEntry, workspace, classProvider, typeWriters, constantPoolManager);
			ArrayList<AbstractInsnNode> clinitInstructions = new ArrayList<>();
			if (parameterRotateEntry != null) {
				clinitInstructions.addAll(parameterRotateEntry.buildDecryptionPrologue(clinitMethodNode, typeWriters, inheritance, 4));
			}

			AbstractInsnNode objectArraySizeInsn = AbstractInsnNode.ldcInt(referenceCount, constantPoolManager, typeWriters);
			clinitInstructions.add(objectArraySizeInsn);
			ClassInfoWriter objectClassWriter = constantPoolManager.getOrCreateClassInfo("java/lang/Object", typeWriters);
			clinitInstructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClassWriter));
			clinitInstructions.add(new WideConstantPoolInsnNode(PUTSTATIC, objectArrayField));
			AbstractInsnNode stringArraySizeInsn = AbstractInsnNode.ldcInt(referenceCount, constantPoolManager, typeWriters);
			clinitInstructions.add(stringArraySizeInsn);
			ClassInfoWriter stringClassWriter = constantPoolManager.getOrCreateClassInfo("java/lang/String", typeWriters);
			clinitInstructions.add(new WideConstantPoolInsnNode(ANEWARRAY, stringClassWriter));
			clinitInstructions.add(new WideConstantPoolInsnNode(PUTSTATIC, stringArrayField));
			int maxLocals = parameterRotateEntry == null ? 0 : 2;
			LocalVariableTable localVariableTable = clinitMethodNode.getLocalVariableTable();

			for (int bodyIndex = 0; bodyIndex < methodBodies.size(); bodyIndex++) {
				ObfuscateReferencesBuilder.DataPopulationMethodBody methodBody = methodBodies.get(bodyIndex);
				MethodNode dataPopulationMethod = classNode.createUniqueMethod(
					dataPopulationDescriptor, methodBody.getInstructions(), 4, maxLocals, 1, methodBody.getLocalVariableTable(), new TryCatchBlockInfo[0], "Reference Obfuscation", typeWriters, inheritance, workspace, 4
				);
				if (parameterRotateEntry != null) {
					Long methodEncryptionKey = parameterRotateEntry.createEncryptionKey(dataPopulationMethod);
					parameterRotateEntry.setOperand(dataPopulationMethod, methodBody.getLocalVariableTable().getVariable(0));
					long clinitEncryptionKey = parameterRotateEntry.getEncryptionKey(clinitMethodNode);
					clinitInstructions.add(AbstractInsnNode.loadLong(parameterRotateEntry.getOperand(clinitMethodNode).getIndex(), localVariableTable, 4));
					long saltValue = parameterRotateEntry.getSaltValue(bodyIndex);
					long xorKey = methodEncryptionKey ^ clinitEncryptionKey ^ saltValue;
					clinitInstructions.add(AbstractInsnNode.ldcLong(xorKey, constantPoolManager, typeWriters));
					clinitInstructions.add(InsnNode.of(LXOR));
				}

				AbstractMethodCallWriter dataPopulationMethodWriter = classNode.getOrCreateAnyMethodRef(dataPopulationMethod, typeWriters);
				clinitInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, dataPopulationMethodWriter));
			}

			clinitMethodNode.insertInstructionsAtOffsetSimple(clinitInstructions, 0, 0, 0, "Reference Obfuscation");
		}
	}

	private void genFieldLookupCode(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariableTable,
		AbstractMethodCallWriter lookupMethodWriter,
		AbstractMethodCallWriter declaredFieldLookupWriter,
		List<TypeWriter> types,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		LabelNode declaredLookupMissLabel = new LabelNode(true, 1);
		LabelNode interfaceLoopLabel = new LabelNode(true, 1);
		LabelNode nextInterfaceLabel = new LabelNode(true, 1);
		LabelNode interfacesExhaustedLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, declaredFieldLookupWriter));
		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNULL, declaredLookupMissLabel));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(declaredLookupMissLabel);
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		MethodCallWriter getInterfacesWriter = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getInterfaces", "()[Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getInterfacesWriter));
		instructions.add(AbstractInsnNode.storeObject(4, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNULL, interfacesExhaustedLabel));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(5, localVariableTable, 4));
		instructions.add(interfaceLoopLabel);
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(new JumpInsnNode(IF_ICMPGE, interfacesExhaustedLabel));
		instructions.add(AbstractInsnNode.loadObject(4, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, lookupMethodWriter));
		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNULL, nextInterfaceLabel));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(nextInterfaceLabel);
		instructions.add(AbstractInsnNode.IINC(5, 1, localVariableTable, 4));
		instructions.add(new GotoNode(interfaceLoopLabel));
		instructions.add(interfacesExhaustedLabel);
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(InsnNode.of(ARETURN));
	}

	private void genDeclaredFieldLookupCode(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariableTable,
		List<TypeWriter> types,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		maxLocals.set(7);
		maxStack.set(2);
		LabelNode fieldLoopLabel = new LabelNode(true, 1);
		LabelNode nextFieldLabel = new LabelNode(true, 1);
		LabelNode fieldsExhaustedLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		MethodCallWriter getDeclaredFieldsWriter = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getDeclaredFields", "()[Ljava/lang/reflect/Field;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getDeclaredFieldsWriter));
		instructions.add(AbstractInsnNode.storeObject(3, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(AbstractInsnNode.storeInt(4, localVariableTable, 4));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(5, localVariableTable, 4));
		instructions.add(fieldLoopLabel);
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(4, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IF_ICMPGE, fieldsExhaustedLabel));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(5, localVariableTable, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.storeObject(6, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(6, localVariableTable, 4));
		MethodCallWriter getNameWriter = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "getName", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameWriter));
		instructions.add(AbstractInsnNode.loadObject(1, localVariableTable, 4));
		MethodCallWriter equalsWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, equalsWriter));
		instructions.add(new JumpInsnNode(IFEQ, nextFieldLabel));
		instructions.add(AbstractInsnNode.loadObject(6, localVariableTable, 4));
		MethodCallWriter getTypeWriter = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "getType", "()Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getTypeWriter));
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IF_ACMPNE, nextFieldLabel));
		instructions.add(AbstractInsnNode.loadObject(6, localVariableTable, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(nextFieldLabel);
		instructions.add(AbstractInsnNode.IINC(5, 1, localVariableTable, 4));
		instructions.add(new GotoNode(fieldLoopLabel));
		instructions.add(fieldsExhaustedLabel);
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(InsnNode.of(ARETURN));
	}

	public List<AbstractInsnNode> genReturnValueConversion(
		AbstractMethodCallWriter invokedMethodWriter, ConstantPoolManager constantPoolManager, List<TypeWriter> types, AbstractWorkspace workspace, ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> conversionInstructions = new ArrayList<>(2);
		String returnDescriptor = TypeWriter.getReturnDescriptor(invokedMethodWriter.getDescriptor());
		if (returnDescriptor.equals("V")) {
			conversionInstructions.add(InsnNode.of(POP));
			return conversionInstructions;
		} else if (!returnDescriptor.startsWith("[") && (!returnDescriptor.startsWith("L") || !returnDescriptor.endsWith(";"))) {
			if (returnDescriptor.equals("Z")) {
				ClassInfoWriter booleanWrapperClass = constantPoolManager.getOrCreateClassInfo("java/lang/Boolean", types);
				conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, booleanWrapperClass));
				MethodCallWriter booleanValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Boolean", "booleanValue", "()Z", types, workspace, classProvider);
				conversionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, booleanValueMethod));
				return conversionInstructions;
			} else if (returnDescriptor.equals("B")) {
				ClassInfoWriter byteWrapperClass = constantPoolManager.getOrCreateClassInfo("java/lang/Byte", types);
				conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, byteWrapperClass));
				MethodCallWriter byteValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Byte", "byteValue", "()B", types, workspace, classProvider);
				conversionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, byteValueMethod));
				return conversionInstructions;
			} else if (returnDescriptor.equals("S")) {
				ClassInfoWriter shortWrapperClass = constantPoolManager.getOrCreateClassInfo("java/lang/Short", types);
				conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, shortWrapperClass));
				MethodCallWriter shortValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Short", "shortValue", "()S", types, workspace, classProvider);
				conversionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, shortValueMethod));
				return conversionInstructions;
			} else if (returnDescriptor.equals("C")) {
				ClassInfoWriter characterWrapperClass = constantPoolManager.getOrCreateClassInfo("java/lang/Character", types);
				conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, characterWrapperClass));
				MethodCallWriter charValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Character", "charValue", "()C", types, workspace, classProvider);
				conversionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, charValueMethod));
				return conversionInstructions;
			} else if (returnDescriptor.equals("I")) {
				ClassInfoWriter integerWrapperClass = constantPoolManager.getOrCreateClassInfo("java/lang/Integer", types);
				conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, integerWrapperClass));
				MethodCallWriter intValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Integer", "intValue", "()I", types, workspace, classProvider);
				conversionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, intValueMethod));
				return conversionInstructions;
			} else if (returnDescriptor.equals("J")) {
				ClassInfoWriter longWrapperClass = constantPoolManager.getOrCreateClassInfo("java/lang/Long", types);
				conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, longWrapperClass));
				MethodCallWriter longValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Long", "longValue", "()J", types, workspace, classProvider);
				conversionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, longValueMethod));
				return conversionInstructions;
			} else if (returnDescriptor.equals("F")) {
				ClassInfoWriter floatWrapperClass = constantPoolManager.getOrCreateClassInfo("java/lang/Float", types);
				conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, floatWrapperClass));
				MethodCallWriter floatValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Float", "floatValue", "()F", types, workspace, classProvider);
				conversionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, floatValueMethod));
				return conversionInstructions;
			} else if (returnDescriptor.equals("D")) {
				ClassInfoWriter doubleWrapperClass = constantPoolManager.getOrCreateClassInfo("java/lang/Double", types);
				conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, doubleWrapperClass));
				MethodCallWriter doubleValueMethod = constantPoolManager.getOrCreateMethodRef("java/lang/Double", "doubleValue", "()D", types, workspace, classProvider);
				conversionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, doubleValueMethod));
				return conversionInstructions;
			} else {
				return null;
			}
		} else {
			ClassInfoWriter returnTypeClass = constantPoolManager.getOrCreateClassInfo(TypeWriter.getClassInternalName(invokedMethodWriter.getStackReturnType()), types);
			conversionInstructions.add(new WideConstantPoolInsnNode(CHECKCAST, returnTypeClass));
			return conversionInstructions;
		}
	}

	private String encryptStringAndGenKey(String stringToEncrypt, int entryId, AtomicReference<Long> generatedKey, int[] digitTable) {
		long packedKey = (long)entryId << 46;
		int[] encryptionKeys = StringEncryptionBuilder.generateRandomIntArray(this.random, 6, ENCRYPTION_KEY_MASK);
		long randomNibble = Math.abs(this.random.nextInt() % 16);
		int tableIndex = (int)((entryId & 3) << 4 | randomNibble);
		packedKey |= randomNibble << 42;
		String encodedString = StringEncryptionBuilder.simpleXorEncode(stringToEncrypt, encryptionKeys);
		applyKeyShift(tableIndex, encryptionKeys, digitTable);

		for (int keyIndex = 0; keyIndex < encryptionKeys.length; keyIndex++) {
			long keyPart = encryptionKeys[keyIndex];
			int shiftAmount = 7 * (6 - keyIndex - 1);
			long shiftedKeyPart = keyPart << shiftAmount;
			packedKey |= shiftedKeyPart;
		}

		generatedKey.set(packedKey);
		return encodedString;
	}

	public AbstractMethodCallWriter genFieldResolver(
		boolean isPublic,
		ClassNode classNode,
		AbstractMethodCallWriter indexResolverWriter,
		FieldTypeWriter stringArrayField,
		FieldTypeWriter objectArrayField,
		AbstractMethodCallWriter classResolverWriter,
		AbstractMethodCallWriter declaredFieldLookupWriter,
		AbstractMethodCallWriter fieldLookupWriter,
		Inheritance inheritance,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> types,
		LongNode objectClassKey,
		ConstantPoolManager constantPoolManager
	) {
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		String resolverDescriptor = this.useEnhanced.get() ? "(JJ)Ljava/lang/reflect/Field;" : "(J)Ljava/lang/reflect/Field;";
		int maxLocals = this.useEnhanced.get() ? 16 : 14;
		LocalVariableTable localVariableTable = new LocalVariableTable(true, resolverDescriptor, maxLocals);
		ArrayList<AbstractInsnNode> resolverInstructions = new ArrayList<>();
		AtomicInt maxLocalsAccumulator = new AtomicInt(0);
		AtomicInt maxStackAccumulator = new AtomicInt(0);
		this.genFieldResolverMethod(resolverInstructions, localVariableTable, indexResolverWriter, stringArrayField, objectArrayField, classResolverWriter, declaredFieldLookupWriter, fieldLookupWriter, types, objectClassKey, maxStackAccumulator, maxLocalsAccumulator, constantPoolManager, workspace, classProvider);
		return this.createGeneratedMethod(
			classNode,
			this.useEnhanced.get() ? "(JJ)Ljava/lang/reflect/Field;" : "(J)Ljava/lang/reflect/Field;",
			resolverInstructions,
			maxStackAccumulator.get(),
			maxLocalsAccumulator.get(),
			localVariableTable,
			tryCatchBlocks,
			isPublic,
			types,
			inheritance,
			workspace
		);
	}

	public void genArgumentArrayResolvers(
		boolean isPublic,
		ClassNode classNode,
		DualKeyMap<ClassNode, MemberContext, AbstractMethodCallWriter> argArrayResolverMap,
		Map<MemberContext, Integer> memberIds,
		Inheritance inheritance,
		MultiMap<ClassNode, MemberContext> classMemberReferences,
		Set<MemberContext> nonIndyMembers,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> types,
		ConstantPoolManager constantPoolManager
	) {
		ReferenceGraph<String, MemberContext> membersBySignature = new ReferenceGraph<>();
		Iterator<Entry<MemberContext, Integer>> memberIdIterator = memberIds.entrySet().iterator();

		while (memberIdIterator.hasNext()) {
			MemberContext eligibleMember = memberIdIterator.next().getKey();
			if (nonIndyMembers.contains(eligibleMember) || needsArgumentArrayResolver(classNode, classNode, eligibleMember, workspace)) {
				StringBuilder signatureBuffer = new StringBuilder();

				for (String parameterDescriptor : TypeWriter.parseParameterDescriptors(eligibleMember.getMemberDescriptor())) {
					if (parameterDescriptor.length() == 1) {
						signatureBuffer.append(parameterDescriptor);
					} else {
						signatureBuffer.append("Ljava/lang/Object;");
					}
				}

				membersBySignature.addReference(signatureBuffer.toString(), eligibleMember);
			}
		}

		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];

		for (Entry<String, Set<MemberContext>> signatureEntry : membersBySignature.getAdjacencyEntries()) {
			String parameterSignature = signatureEntry.getKey();
			String resolverDescriptor = "(" + parameterSignature + ")[Ljava/lang/Object;";
			LocalVariableTable localVariableTable = new LocalVariableTable(true, resolverDescriptor, 5);
			ArrayList<AbstractInsnNode> resolverInstructions = new ArrayList<>();
			AtomicInt maxStackAccumulator = new AtomicInt();
			int maxLocals = this.genArgumentArrayCode(resolverDescriptor, resolverInstructions, localVariableTable, maxStackAccumulator, types, constantPoolManager, workspace, classProvider, classNode.isJava5OrNewer());
			AbstractMethodCallWriter resolverMethodWriter = this.createGeneratedMethod(classNode, resolverDescriptor, resolverInstructions, maxStackAccumulator.get(), maxLocals, localVariableTable, tryCatchBlocks, isPublic, types, inheritance, workspace);

			for (MemberContext member : signatureEntry.getValue()) {
				argArrayResolverMap.put(classNode, member, resolverMethodWriter);
			}
		}
	}

	public AbstractMethodCallWriter createHelperMethod(
		ClassNode classNode,
		String descriptor,
		ArrayList<AbstractInsnNode> instructions,
		int maxStack,
		int maxLocals,
		LocalVariableTable localVariableTable,
		TryCatchBlockInfo[] tryCatchBlocks,
		boolean isPublic,
		List<TypeWriter> types,
		AtomicReference<InstructionListNode> instructionListRef,
		Inheritance inheritance,
		AbstractWorkspace workspace
	) {
		byte accessFlags;
		if (isPublic) {
			accessFlags = 4;
		} else {
			accessFlags = 1;
		}

		MethodNode helperMethod = classNode.createUniqueMethod(descriptor, instructions, maxStack, maxLocals, accessFlags, localVariableTable, tryCatchBlocks, "Reference Obfuscation", types, inheritance, workspace, 4);
		if (instructionListRef != null) {
			instructionListRef.set(helperMethod.getInstructionList());
		}

		return classNode.getOrCreateAnyMethodRef(helperMethod, types);
	}

	private void genFieldResolverMethod(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariableTable,
		AbstractMethodCallWriter indexResolverWriter,
		FieldTypeWriter stringArrayField,
		FieldTypeWriter objectArrayField,
		AbstractMethodCallWriter classResolverWriter,
		AbstractMethodCallWriter declaredFieldLookupWriter,
		AbstractMethodCallWriter fieldLookupWriter,
		List<TypeWriter> types,
		LongNode objectClassKey,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		int secondKeySlot = this.useEnhanced.get() ? 2 : 0;
		int referenceIndexSlot = secondKeySlot + 2;
		int cachedEntrySlot = referenceIndexSlot + 1;
		int encodedInfoSlot = cachedEntrySlot + 1;
		int separatorIndexSlot = encodedInfoSlot + 1;
		int declaringClassSlot = separatorIndexSlot + 1;
		int nextSeparatorIndexSlot = declaringClassSlot + 1;
		int fieldNameSlot = nextSeparatorIndexSlot + 1;
		int fieldTypeSlot = fieldNameSlot + 1;
		int currentClassSlot = fieldTypeSlot + 1;
		int matchedFieldSlot = currentClassSlot + 1;
		int interfacesSlot = matchedFieldSlot + 1;
		int interfaceIndexSlot = interfacesSlot + 1;
		maxLocals.set(this.useEnhanced.get() ? 16 : 14);
		maxStack.set(3);
		LabelNode classSearchLoopLabel = new LabelNode(true, 1);
		LabelNode declaredLookupMissLabel = new LabelNode(true, 1);
		LabelNode interfaceLoopLabel = new LabelNode(true, 1);
		LabelNode nextInterfaceLabel = new LabelNode(true, 1);
		LabelNode interfacesExhaustedLabel = new LabelNode(true, 1);
		LabelNode fieldNotFoundLabel = new LabelNode(true, 1);
		LabelNode cachedFieldLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadLong(0, localVariableTable, 4));
		if (this.useEnhanced.get()) {
			instructions.add(AbstractInsnNode.loadLong(secondKeySlot, localVariableTable, 4));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, indexResolverWriter));
		instructions.add(AbstractInsnNode.storeInt(referenceIndexSlot, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
		instructions.add(AbstractInsnNode.loadInt(referenceIndexSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.storeObject(cachedEntrySlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(cachedEntrySlot, localVariableTable, 4));
		ClassInfoWriter stringClassWriter = constantPoolManager.getOrCreateClassInfo("java/lang/String", types);
		instructions.add(new WideConstantPoolInsnNode(INSTANCEOF, stringClassWriter));
		instructions.add(new JumpInsnNode(IFEQ, cachedFieldLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, stringArrayField));
		instructions.add(AbstractInsnNode.loadInt(referenceIndexSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.storeObject(encodedInfoSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(encodedInfoSlot, localVariableTable, 4));
		instructions.add(new BiPushNode(8));
		MethodCallWriter indexOfCharWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "indexOf", "(I)I", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, indexOfCharWriter));
		instructions.add(AbstractInsnNode.storeInt(separatorIndexSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(encodedInfoSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadInt(separatorIndexSlot, localVariableTable, 4));
		MethodCallWriter substringRangeWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "substring", "(II)Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, substringRangeWriter));
		MethodCallWriter parseLongWriter = constantPoolManager.getOrCreateMethodRef("java/lang/Long", "parseLong", "(Ljava/lang/String;I)J", types, workspace, classProvider);
		instructions.add(new BiPushNode(36));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, parseLongWriter));
		if (this.useEnhanced.get()) {
			instructions.add(InsnNode.of(LCONST_0));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, classResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.IINC(separatorIndexSlot, 1, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(encodedInfoSlot, localVariableTable, 4));
		instructions.add(new BiPushNode(8));
		instructions.add(AbstractInsnNode.loadInt(separatorIndexSlot, localVariableTable, 4));
		MethodCallWriter indexOfCharFromWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "indexOf", "(II)I", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, indexOfCharFromWriter));
		instructions.add(AbstractInsnNode.storeInt(nextSeparatorIndexSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(encodedInfoSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(separatorIndexSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(nextSeparatorIndexSlot, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, substringRangeWriter));
		instructions.add(AbstractInsnNode.storeObject(fieldNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.IINC(nextSeparatorIndexSlot, 1, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(encodedInfoSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(nextSeparatorIndexSlot, localVariableTable, 4));
		MethodCallWriter substringFromWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "substring", "(I)Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, substringFromWriter));
		instructions.add(new BiPushNode(36));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, parseLongWriter));
		if (this.useEnhanced.get()) {
			instructions.add(InsnNode.of(LCONST_0));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, classResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(fieldTypeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariableTable, 4));
		instructions.add(classSearchLoopLabel);
		instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(fieldNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(fieldTypeSlot, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, declaredFieldLookupWriter));
		instructions.add(AbstractInsnNode.storeObject(matchedFieldSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(matchedFieldSlot, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNULL, declaredLookupMissLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
		instructions.add(AbstractInsnNode.loadInt(referenceIndexSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(matchedFieldSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(matchedFieldSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(declaredLookupMissLabel);
		instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariableTable, 4));
		MethodCallWriter getInterfacesWriter = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getInterfaces", "()[Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getInterfacesWriter));
		instructions.add(AbstractInsnNode.storeObject(interfacesSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(interfacesSlot, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNULL, interfacesExhaustedLabel));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(interfaceIndexSlot, localVariableTable, 4));
		instructions.add(interfaceLoopLabel);
		instructions.add(AbstractInsnNode.loadInt(interfaceIndexSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(interfacesSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(new JumpInsnNode(IF_ICMPGE, interfacesExhaustedLabel));
		instructions.add(AbstractInsnNode.loadObject(interfacesSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(interfaceIndexSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.loadObject(fieldNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(fieldTypeSlot, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, fieldLookupWriter));
		instructions.add(AbstractInsnNode.storeObject(matchedFieldSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(matchedFieldSlot, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNULL, nextInterfaceLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
		instructions.add(AbstractInsnNode.loadInt(referenceIndexSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(matchedFieldSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(matchedFieldSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(nextInterfaceLabel);
		instructions.add(AbstractInsnNode.IINC(interfaceIndexSlot, 1, localVariableTable, 4));
		instructions.add(new GotoNode(interfaceLoopLabel));
		instructions.add(interfacesExhaustedLabel);
		instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariableTable, 4));
		MethodCallWriter getNameWriter = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getName", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameWriter));
		StringNode objectClassNameNode = constantPoolManager.createStringNode("java.lang.Object", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, objectClassNameNode));
		MethodCallWriter equalsWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, equalsWriter));
		instructions.add(new JumpInsnNode(IFNE, fieldNotFoundLabel));
		instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariableTable, 4));
		MethodCallWriter getSuperclassWriter = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getSuperclassWriter));
		instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNONNULL, classSearchLoopLabel));
		instructions.add(new WideConstantPoolInsnNode(LDC2_W, objectClassKey));
		if (this.useEnhanced.get()) {
			instructions.add(InsnNode.of(LCONST_0));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, classResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariableTable, 4));
		instructions.add(new GotoNode(classSearchLoopLabel));
		instructions.add(fieldNotFoundLabel);
		ClassInfoWriter stringBufferClassWriter = constantPoolManager.getOrCreateClassInfo("java/lang/StringBuffer", types);
		instructions.add(new NewNode(stringBufferClassWriter));
		instructions.add(InsnNode.of(DUP));
		MethodCallWriter stringBufferInitWriter = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuffer", "<init>", "()V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringBufferInitWriter));
		instructions.add(AbstractInsnNode.storeObject(interfaceIndexSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(interfaceIndexSlot, localVariableTable, 4));
		StringNode noSuchFieldMessageNode = constantPoolManager.createStringNode("NoSuchFieldException in ", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, noSuchFieldMessageNode));
		MethodCallWriter appendStringWriter = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringWriter));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameWriter));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringWriter));
		instructions.add(new BiPushNode(32));
		MethodCallWriter appendCharWriter = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuffer", "append", "(C)Ljava/lang/StringBuffer;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendCharWriter));
		instructions.add(AbstractInsnNode.loadObject(fieldTypeSlot, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameWriter));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringWriter));
		instructions.add(new BiPushNode(32));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendCharWriter));
		instructions.add(AbstractInsnNode.loadObject(fieldNameSlot, localVariableTable, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringWriter));
		instructions.add(InsnNode.of(POP));
		ClassInfoWriter runtimeExceptionClassWriter = constantPoolManager.getOrCreateClassInfo("java/lang/RuntimeException", types);
		instructions.add(new NewNode(runtimeExceptionClassWriter));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(interfaceIndexSlot, localVariableTable, 4));
		MethodCallWriter toStringWriter = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuffer", "toString", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, toStringWriter));
		MethodCallWriter runtimeExceptionInitWriter = constantPoolManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionInitWriter));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(cachedFieldLabel);
		instructions.add(AbstractInsnNode.loadObject(cachedEntrySlot, localVariableTable, 4));
		ClassInfoWriter fieldClassWriter = constantPoolManager.getOrCreateClassInfo("java/lang/reflect/Field", types);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, fieldClassWriter));
		instructions.add(InsnNode.of(ARETURN));
	}

	private void genMethodResolverMethod(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariables,
		AbstractMethodCallWriter indexResolverWriter,
		FieldTypeWriter stringArrayField,
		FieldTypeWriter objectArrayField,
		AbstractMethodCallWriter classResolverWriter,
		AbstractMethodCallWriter declaredMethodLookupWriter,
		AbstractMethodCallWriter methodLookupWriter,
		List<TypeWriter> types,
		LongNode objectClassKey,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean includeInterfaces
	) {
		maxLocals.set(this.useEnhanced.get() ? 20 : 18);
		maxStack.set(5);
		int secondKeySlot = this.useEnhanced.get() ? 2 : 0;
		int indexSlot = secondKeySlot + 2;
		int elementSlot = indexSlot + 1;
		int signatureSlot = elementSlot + 1;
		int idSeparatorSlot = signatureSlot + 1;
		int declaringClassSlot = idSeparatorSlot + 1;
		int nameSeparatorSlot = declaringClassSlot + 1;
		int methodNameSlot = nameSeparatorSlot + 1;
		int tokenCountSlot = methodNameSlot + 1;
		int cursorSlot = tokenCountSlot + 1;
		int parameterCountSlot = cursorSlot + 1;
		int parameterTypesSlot = parameterCountSlot + 1;
		int tokenClassSlot = parameterTypesSlot + 1;
		int currentClassSlot = tokenClassSlot + 1;
		int lookupResultSlot = currentClassSlot + 1;
		int messageBufferSlot = lookupResultSlot + 1;
		int messageIndexSlot = messageBufferSlot + 1;
		LabelNode cachedMethodLabel = new LabelNode(true, 1);
		LabelNode separatorScanLabel = new LabelNode(true, 1);
		LabelNode parameterLoopLabel = new LabelNode(true, 1);
		LabelNode arrayStoreEndLabel = new LabelNode(true, 1);
		LabelNode parameterLoopEndLabel = new LabelNode(true, 1);
		LabelNode classHierarchyLoopLabel = new LabelNode(true, 1);
		LabelNode classLookupFailedLabel = new LabelNode(true, 1);
		LabelNode interfacesScanStartLabel = new LabelNode(true, 1);
		LabelNode interfaceScanLoopLabel = new LabelNode(true, 1);
		LabelNode interfaceLoopLabel = new LabelNode(true, 1);
		LabelNode interfaceLookupFailedLabel = new LabelNode(true, 1);
		LabelNode interfacesScanEndLabel = new LabelNode(true, 1);
		LabelNode methodNotFoundErrorLabel = new LabelNode(true, 1);
		LabelNode parameterMessageLoopLabel = new LabelNode(true, 1);
		LabelNode messageLoopEndLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.loadLong(0, localVariables, 4));
		if (this.useEnhanced.get()) {
			instructions.add(AbstractInsnNode.loadLong(secondKeySlot, localVariables, 4));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, indexResolverWriter));
		instructions.add(AbstractInsnNode.storeInt(indexSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.storeObject(elementSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(elementSlot, localVariables, 4));
		ClassInfoWriter stringClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/String", types);
		instructions.add(new WideConstantPoolInsnNode(INSTANCEOF, stringClassInfo));
		instructions.add(new JumpInsnNode(IFEQ, messageLoopEndLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, stringArrayField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(AbstractInsnNode.storeObject(signatureSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(signatureSlot, localVariables, 4));
		instructions.add(new BiPushNode(8));
		MethodCallWriter indexOfCharMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/String", "indexOf", "(I)I", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, indexOfCharMethodRef));
		instructions.add(AbstractInsnNode.storeInt(idSeparatorSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(signatureSlot, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadInt(idSeparatorSlot, localVariables, 4));
		MethodCallWriter substringMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/String", "substring", "(II)Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, substringMethodRef));
		instructions.add(new BiPushNode(36));
		MethodCallWriter parseLongMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Long", "parseLong", "(Ljava/lang/String;I)J", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, parseLongMethodRef));
		if (this.useEnhanced.get()) {
			instructions.add(InsnNode.of(LCONST_0));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, classResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(declaringClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.IINC(idSeparatorSlot, 1, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(signatureSlot, localVariables, 4));
		instructions.add(new BiPushNode(8));
		instructions.add(AbstractInsnNode.loadInt(idSeparatorSlot, localVariables, 4));
		MethodCallWriter indexOfCharFromIndexMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/String", "indexOf", "(II)I", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, indexOfCharFromIndexMethodRef));
		instructions.add(AbstractInsnNode.storeInt(nameSeparatorSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(signatureSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(idSeparatorSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(nameSeparatorSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, substringMethodRef));
		instructions.add(AbstractInsnNode.storeObject(methodNameSlot, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_M1));
		instructions.add(AbstractInsnNode.storeInt(tokenCountSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(nameSeparatorSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.storeInt(cursorSlot, localVariables, 4));
		instructions.add(cachedMethodLabel);
		instructions.add(AbstractInsnNode.IINC(tokenCountSlot, 1, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(signatureSlot, localVariables, 4));
		instructions.add(new BiPushNode(8));
		instructions.add(AbstractInsnNode.IINC(cursorSlot, 1, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(cursorSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, indexOfCharFromIndexMethodRef));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.storeInt(cursorSlot, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_M1));
		instructions.add(new JumpInsnNode(IF_ICMPGT, cachedMethodLabel));
		instructions.add(AbstractInsnNode.loadInt(tokenCountSlot, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.storeInt(parameterCountSlot, localVariables, 4));
		ClassInfoWriter classArrayClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Class", types);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, classArrayClassInfo));
		instructions.add(AbstractInsnNode.storeObject(parameterTypesSlot, localVariables, 4));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(tokenClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(nameSeparatorSlot, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(IADD));
		instructions.add(AbstractInsnNode.storeInt(cursorSlot, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(currentClassSlot, localVariables, 4));
		instructions.add(separatorScanLabel);
		instructions.add(AbstractInsnNode.loadInt(currentClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(tokenCountSlot, localVariables, 4));
		instructions.add(new JumpInsnNode(IF_ICMPGE, arrayStoreEndLabel));
		instructions.add(AbstractInsnNode.loadObject(signatureSlot, localVariables, 4));
		instructions.add(new BiPushNode(8));
		instructions.add(AbstractInsnNode.loadInt(cursorSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, indexOfCharFromIndexMethodRef));
		instructions.add(AbstractInsnNode.storeInt(lookupResultSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(signatureSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(cursorSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(lookupResultSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, substringMethodRef));
		instructions.add(new BiPushNode(36));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, parseLongMethodRef));
		if (this.useEnhanced.get()) {
			instructions.add(InsnNode.of(LCONST_0));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, classResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(tokenClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(currentClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(parameterCountSlot, localVariables, 4));
		instructions.add(new JumpInsnNode(IF_ICMPGE, parameterLoopLabel));
		instructions.add(AbstractInsnNode.loadObject(parameterTypesSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(currentClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(tokenClassSlot, localVariables, 4));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(parameterLoopLabel);
		instructions.add(AbstractInsnNode.loadInt(lookupResultSlot, localVariables, 4));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(IADD));
		instructions.add(AbstractInsnNode.storeInt(cursorSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.IINC(currentClassSlot, 1, localVariables, 4));
		instructions.add(new GotoNode(separatorScanLabel));
		instructions.add(arrayStoreEndLabel);
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariables, 4));
		instructions.add(parameterLoopEndLabel);
		instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(methodNameSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(tokenClassSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(parameterCountSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(parameterTypesSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, declaredMethodLookupWriter));
		instructions.add(AbstractInsnNode.storeObject(lookupResultSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(lookupResultSlot, localVariables, 4));
		instructions.add(new JumpInsnNode(IFNULL, classHierarchyLoopLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
		instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(lookupResultSlot, localVariables, 4));
		instructions.add(InsnNode.of(AASTORE));
		instructions.add(AbstractInsnNode.loadObject(lookupResultSlot, localVariables, 4));
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(classHierarchyLoopLabel);
		instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariables, 4));
		MethodCallWriter getNameMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getName", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameMethodRef));
		StringNode objectClassNameConstant = constantPoolManager.createStringNode("java.lang.Object", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, objectClassNameConstant));
		MethodCallWriter equalsMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, equalsMethodRef));
		if (includeInterfaces) {
			instructions.add(new JumpInsnNode(IFNE, classLookupFailedLabel));
		} else {
			instructions.add(new JumpInsnNode(IFNE, interfacesScanEndLabel));
		}

		instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariables, 4));
		MethodCallWriter getSuperclassMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getSuperclassMethodRef));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariables, 4));
		instructions.add(new JumpInsnNode(IFNONNULL, parameterLoopEndLabel));
		instructions.add(new WideConstantPoolInsnNode(LDC2_W, objectClassKey));
		if (this.useEnhanced.get()) {
			instructions.add(InsnNode.of(LCONST_0));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, classResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariables, 4));
		if (includeInterfaces) {
			instructions.add(classLookupFailedLabel);
			instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariables, 4));
			instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariables, 4));
			instructions.add(interfacesScanStartLabel);
			instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariables, 4));
			MethodCallWriter getInterfacesMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getInterfaces", "()[Ljava/lang/Class;", types, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getInterfacesMethodRef));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.storeObject(lookupResultSlot, localVariables, 4));
			instructions.add(new JumpInsnNode(IFNULL, interfaceLookupFailedLabel));
			instructions.add(InsnNode.of(ICONST_0));
			instructions.add(AbstractInsnNode.storeInt(messageBufferSlot, localVariables, 4));
			instructions.add(interfaceScanLoopLabel);
			instructions.add(AbstractInsnNode.loadInt(messageBufferSlot, localVariables, 4));
			instructions.add(AbstractInsnNode.loadObject(lookupResultSlot, localVariables, 4));
			instructions.add(InsnNode.of(ARRAYLENGTH));
			instructions.add(new JumpInsnNode(IF_ICMPGE, interfaceLookupFailedLabel));
			instructions.add(AbstractInsnNode.loadObject(lookupResultSlot, localVariables, 4));
			instructions.add(AbstractInsnNode.loadInt(messageBufferSlot, localVariables, 4));
			instructions.add(InsnNode.of(AALOAD));
			instructions.add(AbstractInsnNode.loadObject(methodNameSlot, localVariables, 4));
			instructions.add(AbstractInsnNode.loadObject(tokenClassSlot, localVariables, 4));
			instructions.add(AbstractInsnNode.loadInt(parameterCountSlot, localVariables, 4));
			instructions.add(AbstractInsnNode.loadObject(parameterTypesSlot, localVariables, 4));
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, methodLookupWriter));
			instructions.add(AbstractInsnNode.storeObject(messageIndexSlot, localVariables, 4));
			instructions.add(AbstractInsnNode.loadObject(messageIndexSlot, localVariables, 4));
			instructions.add(new JumpInsnNode(IFNULL, interfaceLoopLabel));
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, objectArrayField));
			instructions.add(AbstractInsnNode.loadInt(indexSlot, localVariables, 4));
			instructions.add(AbstractInsnNode.loadObject(messageIndexSlot, localVariables, 4));
			instructions.add(InsnNode.of(AASTORE));
			instructions.add(AbstractInsnNode.loadObject(messageIndexSlot, localVariables, 4));
			instructions.add(InsnNode.of(ARETURN));
			instructions.add(interfaceLoopLabel);
			instructions.add(AbstractInsnNode.IINC(messageBufferSlot, 1, localVariables, 4));
			instructions.add(new GotoNode(interfaceScanLoopLabel));
			instructions.add(interfaceLookupFailedLabel);
			instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariables, 4));
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameMethodRef));
			StringNode secondObjectClassNameConstant = constantPoolManager.createStringNode("java.lang.Object", types, false);
			instructions.add(new WideConstantPoolInsnNode(LDC_W, secondObjectClassNameConstant));
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, equalsMethodRef));
			instructions.add(new JumpInsnNode(IFNE, interfacesScanEndLabel));
			instructions.add(AbstractInsnNode.loadObject(currentClassSlot, localVariables, 4));
			instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getSuperclassMethodRef));
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariables, 4));
			instructions.add(new JumpInsnNode(IFNONNULL, interfacesScanStartLabel));
			instructions.add(new WideConstantPoolInsnNode(LDC2_W, objectClassKey));
			if (this.useEnhanced.get()) {
				instructions.add(InsnNode.of(LCONST_0));
			}

			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, classResolverWriter));
			instructions.add(AbstractInsnNode.storeObject(currentClassSlot, localVariables, 4));
			instructions.add(new GotoNode(interfacesScanStartLabel));
		}

		instructions.add(interfacesScanEndLabel);
		ClassInfoWriter stringBufferClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/StringBuffer", types);
		instructions.add(new NewNode(stringBufferClassInfo));
		instructions.add(InsnNode.of(DUP));
		MethodCallWriter stringBufferConstructorMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuffer", "<init>", "()V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringBufferConstructorMethodRef));
		instructions.add(AbstractInsnNode.storeObject(messageBufferSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(messageBufferSlot, localVariables, 4));
		StringNode messagePrefixConstant = constantPoolManager.createStringNode("NoSuchMethodException in ", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, messagePrefixConstant));
		MethodCallWriter appendStringMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameMethodRef));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(new BiPushNode(32));
		MethodCallWriter appendCharMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuffer", "append", "(C)Ljava/lang/StringBuffer;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendCharMethodRef));
		instructions.add(AbstractInsnNode.loadObject(tokenClassSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameMethodRef));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(new BiPushNode(32));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendCharMethodRef));
		instructions.add(AbstractInsnNode.loadObject(methodNameSlot, localVariables, 4));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(new BiPushNode(40));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendCharMethodRef));
		instructions.add(InsnNode.of(POP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(messageIndexSlot, localVariables, 4));
		instructions.add(methodNotFoundErrorLabel);
		instructions.add(AbstractInsnNode.loadInt(messageIndexSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(parameterCountSlot, localVariables, 4));
		instructions.add(new JumpInsnNode(IF_ICMPGE, parameterMessageLoopLabel));
		instructions.add(AbstractInsnNode.loadObject(messageBufferSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadObject(parameterTypesSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(messageIndexSlot, localVariables, 4));
		instructions.add(InsnNode.of(AALOAD));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameMethodRef));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(InsnNode.of(POP));
		instructions.add(AbstractInsnNode.IINC(messageIndexSlot, 1, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(messageIndexSlot, localVariables, 4));
		instructions.add(AbstractInsnNode.loadInt(parameterCountSlot, localVariables, 4));
		instructions.add(new JumpInsnNode(IF_ICMPGE, methodNotFoundErrorLabel));
		instructions.add(AbstractInsnNode.loadObject(messageBufferSlot, localVariables, 4));
		StringNode commaSeparatorConstant = constantPoolManager.createStringNode(", ", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, commaSeparatorConstant));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(InsnNode.of(POP));
		instructions.add(new GotoNode(methodNotFoundErrorLabel));
		instructions.add(parameterMessageLoopLabel);
		instructions.add(AbstractInsnNode.loadObject(messageBufferSlot, localVariables, 4));
		instructions.add(new BiPushNode(41));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendCharMethodRef));
		instructions.add(InsnNode.of(POP));
		ClassInfoWriter runtimeExceptionClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/RuntimeException", types);
		instructions.add(new NewNode(runtimeExceptionClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(messageBufferSlot, localVariables, 4));
		MethodCallWriter stringBufferToStringMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuffer", "toString", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBufferToStringMethodRef));
		MethodCallWriter runtimeExceptionConstructorMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionConstructorMethodRef));
		instructions.add(InsnNode.of(ATHROW));
		instructions.add(messageLoopEndLabel);
		instructions.add(AbstractInsnNode.loadObject(elementSlot, localVariables, 4));
		ClassInfoWriter methodClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/reflect/Method", types);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, methodClassInfo));
		instructions.add(InsnNode.of(ARETURN));
	}

	private void genMethodHandleResolverCode(
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariableTable,
		TryCatchBlockInfo[] tryCatchBlocks,
		int[] kindCodes,
		AbstractMethodCallWriter fieldResolverWriter,
		AbstractMethodCallWriter methodResolverWriter,
		List<TypeWriter> types,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		maxLocals.set(this.isEnhanced() ? 15 : 13);
		maxStack.set(6);
		LabelNode tryStartLabel = new LabelNode(true, 128);
		LabelNode tryEndLabel = new LabelNode(true, 128);
		LabelNode catchHandlerLabel = new LabelNode(true, 128);
		LabelNode notGetterSetterLabel = new LabelNode(true, 1);
		LabelNode fieldPathLabel = new LabelNode(true, 1);
		LabelNode setterBranchLabel = new LabelNode(true, 1);
		LabelNode staticGetterBranchLabel = new LabelNode(true, 1);
		LabelNode staticSetterBranchLabel = new LabelNode(true, 1);
		LabelNode methodPathLabel = new LabelNode(true, 1);
		LabelNode findVirtualBranchLabel = new LabelNode(true, 1);
		LabelNode findStaticBranchLabel = new LabelNode(true, 1);
		LabelNode handleResolvedLabel = new LabelNode(true, 1);
		LabelNode nullFieldLabel = new LabelNode(true, 1);
		LabelNode nullMethodLabel = new LabelNode(true, 1);
		LabelNode memberTextLabel = new LabelNode(true, 1);
		ClassInfoWriter exceptionClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Exception", types);
		tryCatchBlocks[0] = new TryCatchBlockInfo(exceptionClassInfo, tryStartLabel, tryEndLabel, catchHandlerLabel);
		int secondKeySlot = this.isEnhanced() ? 6 : 4;
		int kindCodeSlot = this.isEnhanced() ? secondKeySlot + 2 : 6;
		int methodHandleSlot = kindCodeSlot + 1;
		int fieldSlot = methodHandleSlot + 1;
		int methodSlot = fieldSlot + 1;
		int declaringClassSlot = methodSlot + 1;
		int memberNameSlot = declaringClassSlot + 1;
		int memberTypeSlot = memberNameSlot + 1;
		instructions.add(AbstractInsnNode.loadObject(2, localVariableTable, 4));
		instructions.add(InsnNode.of(ICONST_0));
		MethodCallWriter charAtMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/String", "charAt", "(I)C", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, charAtMethodRef));
		if (this.isEnhanced() && Options.xorKeyWithLowBits) {
			instructions.add(AbstractInsnNode.loadLong(secondKeySlot, localVariableTable, 4));
			instructions.add(InsnNode.of(L2I));
			instructions.add(AbstractInsnNode.newInt(7));
			instructions.add(InsnNode.of(IAND));
			instructions.add(InsnNode.of(IXOR));
		}

		instructions.add(AbstractInsnNode.storeInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(fieldSlot, localVariableTable, 4));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(methodSlot, localVariableTable, 4));
		instructions.add(tryStartLabel);
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[0]));
		instructions.add(new JumpInsnNode(IF_ICMPEQ, fieldPathLabel));
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[1]));
		instructions.add(new JumpInsnNode(IF_ICMPEQ, fieldPathLabel));
		instructions.add(notGetterSetterLabel);
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[2]));
		instructions.add(new JumpInsnNode(IF_ICMPEQ, fieldPathLabel));
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[3]));
		instructions.add(new JumpInsnNode(IF_ICMPNE, methodPathLabel));
		instructions.add(fieldPathLabel);
		instructions.add(AbstractInsnNode.loadLong(4, localVariableTable, 4));
		if (this.isEnhanced()) {
			instructions.add(AbstractInsnNode.loadLong(secondKeySlot, localVariableTable, 4));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, fieldResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(fieldSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(fieldSlot, localVariableTable, 4));
		MethodCallWriter fieldGetDeclaringClassMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "getDeclaringClass", "()Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, fieldGetDeclaringClassMethodRef));
		instructions.add(AbstractInsnNode.storeObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(fieldSlot, localVariableTable, 4));
		MethodCallWriter fieldGetNameMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "getName", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, fieldGetNameMethodRef));
		instructions.add(AbstractInsnNode.storeObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(fieldSlot, localVariableTable, 4));
		MethodCallWriter fieldGetTypeMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "getType", "()Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, fieldGetTypeMethodRef));
		instructions.add(AbstractInsnNode.storeObject(memberTypeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[0]));
		instructions.add(new JumpInsnNode(IF_ICMPNE, setterBranchLabel));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberTypeSlot, localVariableTable, 4));
		MethodCallWriter findGetterMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles$Lookup",
			"findGetter",
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, findGetterMethodRef));
		instructions.add(AbstractInsnNode.storeObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(new GotoNode(handleResolvedLabel));
		instructions.add(setterBranchLabel);
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[1]));
		instructions.add(new JumpInsnNode(IF_ICMPNE, staticGetterBranchLabel));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberTypeSlot, localVariableTable, 4));
		MethodCallWriter findSetterMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles$Lookup",
			"findSetter",
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, findSetterMethodRef));
		instructions.add(AbstractInsnNode.storeObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(new GotoNode(handleResolvedLabel));
		instructions.add(staticGetterBranchLabel);
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[2]));
		instructions.add(new JumpInsnNode(IF_ICMPNE, staticSetterBranchLabel));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberTypeSlot, localVariableTable, 4));
		MethodCallWriter findStaticGetterMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles$Lookup",
			"findStaticGetter",
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, findStaticGetterMethodRef));
		instructions.add(AbstractInsnNode.storeObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(new GotoNode(handleResolvedLabel));
		instructions.add(staticSetterBranchLabel);
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberTypeSlot, localVariableTable, 4));
		MethodCallWriter findStaticSetterMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles$Lookup",
			"findStaticSetter",
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, findStaticSetterMethodRef));
		instructions.add(AbstractInsnNode.storeObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(new GotoNode(handleResolvedLabel));
		instructions.add(methodPathLabel);
		instructions.add(AbstractInsnNode.loadLong(4, localVariableTable, 4));
		if (this.isEnhanced()) {
			instructions.add(AbstractInsnNode.loadLong(secondKeySlot, localVariableTable, 4));
		}

		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, methodResolverWriter));
		instructions.add(AbstractInsnNode.storeObject(methodSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(methodSlot, localVariableTable, 4));
		MethodCallWriter methodGetDeclaringClassMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "getDeclaringClass", "()Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, methodGetDeclaringClassMethodRef));
		instructions.add(AbstractInsnNode.storeObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(methodSlot, localVariableTable, 4));
		MethodCallWriter methodGetNameMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "getName", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, methodGetNameMethodRef));
		instructions.add(AbstractInsnNode.storeObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(methodSlot, localVariableTable, 4));
		MethodCallWriter getReturnTypeMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "getReturnType", "()Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getReturnTypeMethodRef));
		instructions.add(AbstractInsnNode.loadObject(methodSlot, localVariableTable, 4));
		MethodCallWriter getParameterTypesMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "getParameterTypes", "()[Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getParameterTypesMethodRef));
		MethodCallWriter methodTypeMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", types, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, methodTypeMethodRef));
		instructions.add(AbstractInsnNode.storeObject(memberTypeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[4]));
		instructions.add(new JumpInsnNode(IF_ICMPNE, findVirtualBranchLabel));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberTypeSlot, localVariableTable, 4));
		MethodCallWriter findVirtualMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles$Lookup",
			"findVirtual",
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, findVirtualMethodRef));
		instructions.add(AbstractInsnNode.storeObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(new GotoNode(handleResolvedLabel));
		instructions.add(findVirtualBranchLabel);
		instructions.add(AbstractInsnNode.loadInt(kindCodeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.newInt(kindCodes[5]));
		instructions.add(new JumpInsnNode(IF_ICMPNE, findStaticBranchLabel));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberTypeSlot, localVariableTable, 4));
		MethodCallWriter findStaticMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles$Lookup",
			"findStatic",
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, findStaticMethodRef));
		instructions.add(AbstractInsnNode.storeObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(new GotoNode(handleResolvedLabel));
		instructions.add(findStaticBranchLabel);
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberTypeSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		MethodCallWriter findSpecialMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles$Lookup",
			"findSpecial",
			"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;",
			types,
			workspace,
			classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, findSpecialMethodRef));
		instructions.add(AbstractInsnNode.storeObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(handleResolvedLabel);
		instructions.add(AbstractInsnNode.loadObject(methodHandleSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(3, localVariableTable, 4));
		MethodCallWriter parameterCountMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodType", "parameterCount", "()I", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, parameterCountMethodRef));
		int keyCount = this.isEnhanced() ? 2 : 1;
		instructions.add(AbstractInsnNode.newInt(keyCount));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(AbstractInsnNode.newInt(keyCount));
		ClassInfoWriter classArrayClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Class", types);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, classArrayClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.newInt(0));
		AbstractFieldNode longTypeField = classProvider.getClassNode("java/lang/Long").findField("TYPE", "Ljava/lang/Class;");
		FieldTypeWriter longTypeFieldRef = constantPoolManager.getOrCreateFieldRef("java/lang/Long", "TYPE", "Ljava/lang/Class;", types, longTypeField);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, longTypeFieldRef));
		instructions.add(InsnNode.of(AASTORE));
		if (this.isEnhanced()) {
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.newInt(1));
			instructions.add(new WideConstantPoolInsnNode(GETSTATIC, longTypeFieldRef));
			instructions.add(InsnNode.of(AASTORE));
		}

		MethodCallWriter dropArgumentsMethodRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/invoke/MethodHandles", "dropArguments", "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", types, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, dropArgumentsMethodRef));
		instructions.add(tryEndLabel);
		instructions.add(InsnNode.of(ARETURN));
		instructions.add(catchHandlerLabel);
		instructions.add(AbstractInsnNode.storeObject(declaringClassSlot, localVariableTable, 4));
		ClassInfoWriter stringBuilderClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/StringBuilder", types);
		instructions.add(new NewNode(stringBuilderClassInfo));
		instructions.add(InsnNode.of(DUP));
		MethodCallWriter stringBuilderConstructorMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "<init>", "()V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringBuilderConstructorMethodRef));
		instructions.add(AbstractInsnNode.storeObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		MethodCallWriter getClassMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Object", "getClass", "()Ljava/lang/Class;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getClassMethodRef));
		MethodCallWriter getNameMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "getName", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getNameMethodRef));
		MethodCallWriter appendStringMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		StringNode classSeparatorConstant = constantPoolManager.createStringNode(" : ", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, classSeparatorConstant));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(AbstractInsnNode.loadObject(fieldSlot, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNULL, nullFieldLabel));
		instructions.add(AbstractInsnNode.loadObject(fieldSlot, localVariableTable, 4));
		MethodCallWriter fieldToStringMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Field", "toString", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, fieldToStringMethodRef));
		instructions.add(new GotoNode(memberTextLabel));
		instructions.add(nullFieldLabel);
		instructions.add(AbstractInsnNode.loadObject(methodSlot, localVariableTable, 4));
		instructions.add(new JumpInsnNode(IFNULL, nullMethodLabel));
		instructions.add(AbstractInsnNode.loadObject(methodSlot, localVariableTable, 4));
		MethodCallWriter methodToStringMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/reflect/Method", "toString", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, methodToStringMethodRef));
		instructions.add(new GotoNode(memberTextLabel));
		instructions.add(nullMethodLabel);
		StringNode nullMemberConstant = constantPoolManager.createStringNode(" null ", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, nullMemberConstant));
		instructions.add(memberTextLabel);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		StringNode memberSeparatorConstant = constantPoolManager.createStringNode(" : ", types, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, memberSeparatorConstant));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(AbstractInsnNode.loadObject(declaringClassSlot, localVariableTable, 4));
		MethodCallWriter exceptionToStringMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Exception", "toString", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, exceptionToStringMethodRef));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, appendStringMethodRef));
		instructions.add(InsnNode.of(POP));
		ClassInfoWriter runtimeExceptionClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/RuntimeException", types);
		instructions.add(new NewNode(runtimeExceptionClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(memberNameSlot, localVariableTable, 4));
		MethodCallWriter stringBuilderToStringMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringBuilderToStringMethodRef));
		MethodCallWriter runtimeExceptionConstructorMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", types, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, runtimeExceptionConstructorMethodRef));
		instructions.add(InsnNode.of(ATHROW));
	}

	public AbstractMethodCallWriter genIndexResolver(
		ClassNode classNode,
		FieldTypeWriter objectArrayField,
		FieldTypeWriter stringArrayField,
		int[] saltTable,
		Inheritance inheritance,
		Workspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> types,
		ConstantPoolManager constantPoolManager
	) {
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		LocalVariableTable localVariableTable = new LocalVariableTable(true, this.useEnhanced.get() ? "(JJ)I" : "(J)I", 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		AtomicInt maxLocals = new AtomicInt(0);
		AtomicInt maxStack = new AtomicInt(0);
		this.genIdDecryptorCode(instructions, localVariableTable, objectArrayField, stringArrayField, saltTable, types, maxStack, maxLocals, constantPoolManager, workspace, classProvider);
		return this.createGeneratedMethod(classNode, this.useEnhanced.get() ? "(JJ)I" : "(J)I", instructions, maxStack.get(), maxLocals.get(), localVariableTable, tryCatchBlocks, false, types, inheritance, workspace);
	}

	public boolean isLongOrDoubleType(FieldTypeWriter fieldWriter) {
		String descriptor = fieldWriter.getDescriptor();
		if (descriptor.length() == 1) {
			switch (descriptor.charAt(0)) {
				case 'B':
				case 'C':
				case 'F':
				case 'I':
				case 'S':
				case 'Z':
					return false;
				case 'D':
				case 'J':
					return true;
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
					return true;
			}
		} else {
			return false;
		}
	}

	public Set<String> planAuxiliaryClassName(
		Set<ClassNode> allWorkspaceClasses,
		AtomicReference<String> referenceObfuscationDataClassName,
		Map<ArchiveNode, String> archiveToDataClassNameMap,
		Map<ClassNode, String> classToDataClassNameMap,
		ChangeLogContext changelogIn,
		String configuredPackageName,
		ReferenceGraph<ArchiveNode, ArchiveNode> moduleReferenceGraph,
		ReferenceGraph<ArchiveNode, String> modulePackageGraph,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> classesByModule,
		ReadOnlyMap<String, String> packageRenameMap,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyMap<String, String> internalToBinaryNameMap,
		String namePrefix,
		MixCaseType mixedCaseType,
		boolean avoidNameClash,
		Workspace workspace,
		ClassRepository classRepository,
		ScriptContext scriptContext
	) {
		HashSet<String> filesystemPackageNames = GlobalUtils.newHashSet();
		HashSet<ClassNode> filesystemClasses = GlobalUtils.newHashSet();
		HashSet<ClassNode> nonModularClasses = GlobalUtils.newHashSet();
		allWorkspaceClasses = InjectedClassManager.collectEligibleClasses(allWorkspaceClasses, filesystemClasses, nonModularClasses, filesystemPackageNames);
		int nonModularClassCount = nonModularClasses.size();
		HashSet<String> dataClassNames = GlobalUtils.newHashSet();
		HashSet<String> usedInternalNames = GlobalUtils.newHashSet();
		if (changelogIn != null) {
			String changelogClassName = changelogIn.getObfuscateReferencesClassName();
			if (changelogClassName != null) {
				String validatedClassName = InjectedClassManager.validateUserConfiguredName(
					"ObfuscateReferencesClass:", changelogClassName, "obfuscateReferencesPackage", configuredPackageName, "Reference Obfuscation", filesystemPackageNames, changelogIn, internalToBinaryNameMap
				);
				if (validatedClassName != null) {
					referenceObfuscationDataClassName.set(validatedClassName);
					dataClassNames.add(validatedClassName);
					usedInternalNames.add(GlobalUtils.binaryToInternalName(validatedClassName));

					for (ClassNode classNode : filesystemClasses) {
						classToDataClassNameMap.put(classNode, validatedClassName);
					}
				}
			}

			for (ArchiveNode archiveNode : classesByModule.keySet()) {
				String moduleName = archiveNode.getModuleName();
				String changelogModuleClassName = changelogIn.getModuleObfuscateReferencesClass(moduleName);
				if (changelogModuleClassName != null) {
					String validatedModuleClassName = InjectedClassManager.validateUserConfiguredName(
						"ObfuscateReferencesClass:", changelogModuleClassName, "obfuscateReferencesPackage", configuredPackageName, "Reference Obfuscation", modulePackageGraph.getReferences(archiveNode), changelogIn, internalToBinaryNameMap
					);
					if (validatedModuleClassName != null) {
						archiveToDataClassNameMap.put(archiveNode, validatedModuleClassName);
						dataClassNames.add(validatedModuleClassName);
						usedInternalNames.add(GlobalUtils.binaryToInternalName(validatedModuleClassName));
						List<ClassNode> moduleClasses = classesByModule.get(archiveNode);
						if (moduleClasses != null) {
							for (ClassNode moduleClassNode : moduleClasses) {
								classToDataClassNameMap.put(moduleClassNode, validatedModuleClassName);
							}
						}
					}
				}
			}

			if ((nonModularClassCount == 0 || !referenceObfuscationDataClassName.isNull()) && classesByModule.size() == archiveToDataClassNameMap.size()) {
				InjectedClassManager.propagateArchiveMappings(
					allWorkspaceClasses, referenceObfuscationDataClassName, archiveToDataClassNameMap, nonModularClasses, classToDataClassNameMap, classesByModule
				);
				return dataClassNames;
			}
		}

		return InjectedClassManager.planAuxiliaryClassNames(
			allWorkspaceClasses,
			null,
			referenceObfuscationDataClassName,
			archiveToDataClassNameMap,
			classToDataClassNameMap,
			"obfuscateReferencesPackage",
			configuredPackageName,
			filesystemClasses,
			nonModularClasses,
			classesByModule,
			moduleReferenceGraph,
			modulePackageGraph,
			packageRenameMap,
			classNameToInternalNameMap,
			internalToBinaryNameMap,
			usedInternalNames,
			namePrefix,
			mixedCaseType,
			avoidNameClash,
			this.randomizeObfuscation,
			"Reference Obfuscation",
			this.userConfiguredNames,
			classRepository,
			scriptContext
		);
	}

	private Types getTypeFromDescriptor(String descriptor, Integer innerClassOrdinal, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		Object type;
		if (TypeWriter.isPrimitiveDescriptor(descriptor)) {
			type = PrimitiveNode.forDescriptor(descriptor);
		} else if (descriptor.startsWith("[")) {
			type = new ObfuscateReferencesBuilder.ArrayType(descriptor, innerClassOrdinal, classProvider, phantomClassManager);
		} else {
			String internalName = descriptor.substring(1, descriptor.length() - 1);
			type = classProvider.getClassNode(internalName, innerClassOrdinal, "Looking for type '" + descriptor + "' during Reference Obfuscation");
		}

		return (Types)type;
	}

	private int genArgumentArrayCode(
		String descriptor,
		List<AbstractInsnNode> instructions,
		LocalVariableTable localVariableTable,
		AtomicInt maxStack,
		List<TypeWriter> types,
		ConstantPoolManager constantPoolManager,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean useValueOf
	) {
		int argumentSlot = 0;
		maxStack.set(4);
		boolean wideArgumentBoxed = false;
		boolean narrowBoxedWithoutValueOf = false;
		boolean wideBoxedWithoutValueOf = false;
		List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(descriptor);
		instructions.add(AbstractInsnNode.newInt(parameterDescriptors.size()));
		ClassInfoWriter objectClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Object", types);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClassInfo));

		for (int i = 0; i < parameterDescriptors.size(); i++) {
			instructions.add(InsnNode.of(DUP));
			instructions.add(AbstractInsnNode.newInt(i));
			String parameterDescriptor = parameterDescriptors.get(i);
			if (parameterDescriptor.length() > 1) {
				instructions.add(AbstractInsnNode.loadObject(argumentSlot, localVariableTable, 4));
			} else {
				narrowBoxedWithoutValueOf = narrowBoxedWithoutValueOf || !useValueOf;
				if (parameterDescriptor.equals("I")) {
					PrimitiveBoxingUtils.loadAndBoxInt(argumentSlot, instructions, useValueOf, localVariableTable, types, constantPoolManager, workspace, classProvider, 4);
				} else if (parameterDescriptor.equals("B")) {
					PrimitiveBoxingUtils.loadAndBoxByte(argumentSlot, instructions, useValueOf, localVariableTable, types, constantPoolManager, workspace, classProvider, 4);
				} else if (parameterDescriptor.equals("Z")) {
					PrimitiveBoxingUtils.loadAndBoxBoolean(argumentSlot, instructions, useValueOf, localVariableTable, types, constantPoolManager, workspace, classProvider, 4);
				} else if (parameterDescriptor.equals("S")) {
					PrimitiveBoxingUtils.loadAndBoxShort(argumentSlot, instructions, useValueOf, localVariableTable, types, constantPoolManager, workspace, classProvider, 4);
				} else if (parameterDescriptor.equals("C")) {
					PrimitiveBoxingUtils.loadAndBoxChar(argumentSlot, instructions, useValueOf, localVariableTable, types, constantPoolManager, workspace, classProvider, 4);
				} else if (parameterDescriptor.equals("J")) {
					wideArgumentBoxed = true;
					wideBoxedWithoutValueOf = !useValueOf;
					PrimitiveBoxingUtils.loadAndBoxLong(argumentSlot, instructions, useValueOf, localVariableTable, types, constantPoolManager, workspace, classProvider, 4);
					argumentSlot++;
				} else if (parameterDescriptor.equals("F")) {
					PrimitiveBoxingUtils.loadAndBoxFloat(argumentSlot, instructions, useValueOf, localVariableTable, types, constantPoolManager, workspace, classProvider, 4);
				} else if (parameterDescriptor.equals("D")) {
					wideArgumentBoxed = true;
					wideBoxedWithoutValueOf = !useValueOf;
					PrimitiveBoxingUtils.loadAndBoxDouble(argumentSlot, instructions, useValueOf, localVariableTable, types, constantPoolManager, workspace, classProvider, 4);
					argumentSlot++;
				}
			}

			instructions.add(InsnNode.of(AASTORE));
			argumentSlot++;
		}

		instructions.add(InsnNode.of(ARETURN));
		if (wideArgumentBoxed) {
			maxStack.increaseAndGet();
		}

		if (narrowBoxedWithoutValueOf) {
			maxStack.add(2);
		}

		if (wideBoxedWithoutValueOf) {
			maxStack.increaseAndGet();
		}

		return argumentSlot;
	}

	public int[] createSaltTable() {
		int[] saltTable = new int[]{
			0,
			1,
			2,
			3,
			4,
			5,
			6,
			7,
			8,
			9,
			10,
			11,
			12,
			13,
			14,
			15,
			16,
			17,
			18,
			19,
			20,
			21,
			22,
			23,
			24,
			25,
			26,
			27,
			28,
			29,
			30,
			31,
			32,
			33,
			34,
			35,
			36,
			37,
			38,
			39,
			40,
			41,
			42,
			43,
			44,
			45,
			46,
			47,
			48,
			49,
			50,
			51,
			52,
			53,
			54,
			55,
			56,
			57,
			58,
			59,
			60,
			61,
			62,
			63
		};
		GlobalUtils.shuffle(saltTable, this.random);
		return saltTable;
	}

	public static class DataPopulationMethodBody {
		private final ArrayList<AbstractInsnNode> instructions;
		private final LocalVariableTable localVariableTable;

		public DataPopulationMethodBody(int initialCapacity, String methodSignature, boolean isStaticMethod) {
			this.instructions = new ArrayList<>(initialCapacity);
			this.localVariableTable = new LocalVariableTable(isStaticMethod, methodSignature, 5);
		}

		public LocalVariableTable getLocalVariableTable() {
			return this.localVariableTable;
		}

		public ArrayList<AbstractInsnNode> getInstructions() {
			return this.instructions;
		}
	}

	public static class ArrayType implements Types {
		private final String descriptor;
		private final int arrayDimension;
		private AbstractClassNode elementClass;

		public ArrayType(String descriptor, Integer innerClassOrdinal, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
			this.descriptor = descriptor;
			this.arrayDimension = descriptor.lastIndexOf("[") + 1;
			String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.descriptor);
			if (elementInternalName != null) {
				this.elementClass = classProvider.getClassNode(elementInternalName, innerClassOrdinal, "looking for array type", phantomClassManager);
			}
		}

		@Override
		public boolean isPrimitive() {
			return false;
		}

		@Override
		public String getInternalName() {
			return this.elementClass == null ? this.descriptor : AbstractClassNode.createClassDescriptor(this.elementClass.getInternalName(), this.arrayDimension).replace('/', '.');
		}
	}
}

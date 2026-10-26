package com.zelix.tools.parameterChanger;

import com.zelix.enums.MixCaseType;
import com.zelix.event.EventRegistry;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.analyzer.StaticBlockAnalyzer;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.code.InstructionBlock;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.*;
import com.zelix.framework.reference.IReferenceGraph;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.variable.LocalVariableIndexCounter;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableSlot;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.InterfaceMethodCallWriter;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.ApplicationEntryPoints;
import com.zelix.klassmaster.constant.LicenseCheckConstants2;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.command.ClassInitializationOrderStatement;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.AuxiliaryClassGenerator;
import com.zelix.tools.AuxiliaryWiringStrategy;
import com.zelix.tools.InjectedClassManager;
import com.zelix.tools.initOrder.ClassInitializationOrderBuilder;
import com.zelix.tools.parameterChanger.calls.ExceptionCallEntry;
import com.zelix.tools.parameterChanger.calls.ExtraCallEntry;
import com.zelix.tools.parameterChanger.dataset.ChangedMethod;
import com.zelix.tools.parameterChanger.salt.SaltManager;
import com.zelix.tools.parameterChanger.salt.SaltOrder;
import com.zelix.tools.trim.TrimExcludedMemberSet;
import com.zelix.utils.EnvironmentUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.OrderedSet;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.collections.UnmodifiableMultiMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.dataset.*;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.Workspace;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class ParameterChangerBuilder extends LicenseCheckConstants2 implements AuxiliaryClassGenerator {
	private static final IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
	private static final long[] bitMasks = new long[64];
	private static Iterator<Long> randomLongIterator;
	private static Random random;
	private final Map<Triplet, Triplet> resolvedMethodsByTriplet = GlobalUtils.newHashMap();
	private final Set<MethodNode> relationshipFileMethods = GlobalUtils.newHashSet();
	private final String[] decrypterPlaceholders = new String[]{"IiIiI", "LlLlL", "HhHhH"};
	private final Map<MethodNode, Long> methodEncryptionKeys;
	private final ReferenceGraph<ClassNode, ClassNode> innerClassReferenceGraph;
	private final Set<ClassNode> stringEncryptionClasses;
	private final Set<ClassNode> integerEncryptionClasses;
	private final Set<ClassNode> longEncryptionClasses;
	private final UnmodifiableMultiMap<ArchiveNode, ClassNode> classesByModule;
	private final String newNamesPrefix;
	private final MixCaseType mixedCaseClassNames;
	private final boolean uniqueClassNames;
	private final int injectClassMajorVersion;
	private final int injectClassMinorVersion;
	private final boolean veryLargeApplication;
	private final Workspace workspace;
	private final ReadOnlyMap<String, String> classNameToInternalNameMap;
	private final ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodRegistryView;
	private final ChangeLogContext changeLogContext;
	private final Set<AbstractClassNode> flowGroupingClasses;
	private final StaticBlockAnalyzer staticBlockAnalyzer;
	private final ClassInitializationOrderBuilder classInitOrderBuilder;
	private final List<String> overrideNames;
	private final ClassRepository classRepository;
	private final ScriptContext scriptContext;
	private final boolean randomizeObfuscation;
	private final Map<ClassNode, Triplet> classToMpcClassTripletMap;
	private List<SaltOrder> saltOrders;
	private Map<MethodNode, ChangedMethod> changedMethodByMethod;
	private Map<AbstractClassNode, Long> classEncryptionKeys;
	private int[] bitPermutation;

	public ParameterChangerBuilder(
		Map<MethodNode, Long> methodEncryptionKeys,
		Enumeration<ClassNode> methodStringConstantClasses,
		Enumeration<ClassNode> fieldStringConstantClasses,
		Enumeration<ClassNode> methodIntegerConstantClasses,
		Enumeration<ClassNode> fieldIntegerConstantClasses,
		Enumeration<ClassNode> methodLongConstantClasses,
		Enumeration<ClassNode> fieldLongConstantClasses,
		ReferenceGraph<ClassNode, ClassNode> innerClassReferenceGraph,
		int classCount,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> classesByModule,
		String newNamesPrefix,
		MixCaseType mixedCaseClassNames,
		boolean uniqueClassNames,
		int injectClassMajorVersion,
		int injectClassMinorVersion,
		boolean veryLargeApplication,
		ChangeLogContext changeLogContext,
		Workspace workspace,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodRegistryView,
		Set<AbstractClassNode> flowGroupingClasses,
		StaticBlockAnalyzer staticBlockAnalyzer,
		ClassInitializationOrderBuilder classInitOrderBuilder,
		List<String> overrideNames,
		ClassRepository classRepository,
		boolean randomizeObfuscation,
		ScriptContext scriptContext
	) {
		this.methodEncryptionKeys = methodEncryptionKeys;
		this.innerClassReferenceGraph = innerClassReferenceGraph;
		this.stringEncryptionClasses = GlobalUtils.newHashSet();

		while (methodStringConstantClasses.hasMoreElements()) {
			this.stringEncryptionClasses.add(methodStringConstantClasses.nextElement());
		}

		while (fieldStringConstantClasses.hasMoreElements()) {
			ClassNode stringConstantClass = fieldStringConstantClasses.nextElement();
			this.stringEncryptionClasses.add(stringConstantClass);
		}

		this.integerEncryptionClasses = GlobalUtils.newHashSet();

		while (methodIntegerConstantClasses.hasMoreElements()) {
			this.integerEncryptionClasses.add(methodIntegerConstantClasses.nextElement());
		}

		while (fieldIntegerConstantClasses.hasMoreElements()) {
			ClassNode integerConstantClass = fieldIntegerConstantClasses.nextElement();
			this.integerEncryptionClasses.add(integerConstantClass);
		}

		this.longEncryptionClasses = GlobalUtils.newHashSet();

		while (methodLongConstantClasses.hasMoreElements()) {
			this.longEncryptionClasses.add(methodLongConstantClasses.nextElement());
		}

		while (fieldLongConstantClasses.hasMoreElements()) {
			ClassNode longConstantClass = fieldLongConstantClasses.nextElement();
			this.longEncryptionClasses.add(longConstantClass);
		}

		this.classesByModule = classesByModule;
		this.newNamesPrefix = newNamesPrefix;
		this.mixedCaseClassNames = mixedCaseClassNames;
		this.uniqueClassNames = uniqueClassNames;
		this.injectClassMajorVersion = injectClassMajorVersion;
		this.injectClassMinorVersion = injectClassMinorVersion;
		this.veryLargeApplication = veryLargeApplication;
		this.workspace = workspace;
		this.classNameToInternalNameMap = classNameToInternalNameMap;
		this.methodRegistryView = methodRegistryView;
		this.changeLogContext = changeLogContext;
		this.flowGroupingClasses = flowGroupingClasses;
		this.staticBlockAnalyzer = staticBlockAnalyzer;
		this.classInitOrderBuilder = classInitOrderBuilder;
		this.overrideNames = overrideNames;
		this.classRepository = classRepository;
		this.scriptContext = scriptContext;
		this.randomizeObfuscation = randomizeObfuscation;
		this.classToMpcClassTripletMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		this.readRelationshipFileMethods();

		try {
			Class.forName(LicenseCheckConstants2.licenseCheckClassNames[LicenseCheckConstants2.licenseCheckClassNames.length - 1]).newInstance();
		} catch (Throwable ignored) {
		}
	}

	public static long permuteWithRandomIndex(long value, int[] bitPermutation) {
		long permutedBits = permuteAllBits(value, bitPermutation);
		AtomicInt highByte = new AtomicInt();
		AtomicLong middleBits = new AtomicLong();
		AtomicInt lowByte = new AtomicInt();
		splitLong(permutedBits, highByte, middleBits, lowByte);

		int randomIndex;
		do {
			randomIndex = random.nextInt(256);
		} while (randomIndex == highByte.get());

		return packAndPermuteLongParts(randomIndex, middleBits.get(), lowByte.get(), bitPermutation);
	}

	public static long extractLowBits(long value, int bitCount, int[] bitPermutation) {
		return extractPermutedBitRange(value, 0, bitCount - 1, bitPermutation);
	}

	public static boolean canChangeParameters(AbstractMethodNode method, Hierarchy hierarchy) {
		return (method.isPrivate() || method.isStatic() || method.isConstructor() || !hierarchy.isMethodRedirected(method) && !hierarchy.isMethodInOverrideGraph(method)) && !Options.dontChangeMethodParams;
	}

	public static void shuffleBitPermutation(int[] bitPermutation, Random random) {
		ArrayList<Integer> candidateIndices = new ArrayList<>(56);

		for (int bitIndex = 8; bitIndex < 64; bitIndex++) {
			if (bitIndex - bitPermutation[bitIndex] > 7) {
				candidateIndices.add(INTEGER_CACHE.valueOf(bitIndex));
			}
		}

		GlobalUtils.shuffle(candidateIndices, random);
		int candidateCount = candidateIndices.size();

		for (int pairIndex = 0; pairIndex < candidateCount; pairIndex++) {
			int firstBitIndex = candidateIndices.get(pairIndex);
			int secondBitIndex = candidateIndices.get(pairIndex + 1);
			int bitDelta = firstBitIndex - secondBitIndex;
			bitPermutation[firstBitIndex] = bitDelta;
			bitPermutation[secondBitIndex] = -bitDelta;
			pairIndex++;
		}
	}

	private static long permutePackedBits(long value, int[] bitPermutation) {
		return permuteBits(value, bitPermutation);
	}

	public static long permuteBits(long value, int[] bitPermutation) {
		long permutedBits = 0L;
		int bitCount = bitPermutation.length;

		for (int bitIndex = 0; bitIndex < bitCount; bitIndex++) {
			long bitValue = value & bitMasks[bitIndex];
			long bitShift = bitPermutation[bitIndex];
			if (bitValue != 0L) {
				if (bitShift > 0L) {
					bitValue >>>= (int)bitShift;
				} else if (bitShift < 0L) {
					bitValue <<= (int)(~bitShift + 1L);
				}

				permutedBits |= bitValue;
			}
		}

		return permutedBits;
	}

	public static long extractBitRange(long value, int fromBit, int toBit) {
		long extractedValue = value;
		int leftShift = 63 - toBit;
		if (leftShift > 0) {
			extractedValue = value << leftShift;
		}

		long rightShift = fromBit + 64 - 1 - toBit;
		if (rightShift > 0L) {
			extractedValue >>>= (int)rightShift;
		}

		return extractedValue;
	}

	public static long packBits(int highValue, int highBitCount, long lowValue) {
		new StringBuilder().append("%1$").append(highBitCount).append("s").toString();
		new StringBuilder().append("%1$").append(64 - highBitCount).append("s").toString();
		return 0L | (long)highValue << 64 - highBitCount | lowValue;
	}

	public static long packAndPermuteBits(int highValue, int highBitCount, long lowValue, int[] bitPermutation) {
		return permutePackedBits(packBits(highValue, highBitCount, lowValue), bitPermutation);
	}

	private static String getHashTableDescriptor(boolean java5OrNewer) {
		String descriptor;
		if (Options.ZKM_METHOD_PARAMETER_MAP_CLASS != null) {
			descriptor = "L" + Options.ZKM_METHOD_PARAMETER_MAP_CLASS.replace('.', '/') + ";";
		} else if (java5OrNewer) {
			descriptor = "Ljava/util/concurrent/ConcurrentHashMap;";
		} else {
			descriptor = "Ljava/util/Hashtable;";
		}

		return descriptor;
	}

	public static long nextRandomLong() {
		return randomLongIterator.next();
	}

	public static Map<MethodNode, Long> buildMethodEncryptionKeys(Set<MethodNode> methodsToChange, Map<MethodNode, Long> existingKeys, boolean randomizeObfuscation, int classCount, ChangeLogContext changeLogContext, Hierarchy hierarchy) {
		if (!randomizeObfuscation && !Options.ZKM_RANDOMIZE_OBFUSCATION) {
			random = GlobalUtils.newRandomWithSeed(classCount);
		} else {
			random = GlobalUtils.newRandom(69);
		}

		randomLongIterator = random.longs(1L, 140737488355328L).iterator();
		HashSet<Long> seenKeys = GlobalUtils.newHashSet();
		HashMap<MethodNode, Long> keysByMethod = GlobalUtils.newHashMap(GlobalUtils.newHashMap(Math.max(classCount * 5, existingKeys.size())));

		for (Entry<MethodNode, Long> existingEntry : existingKeys.entrySet()) {
			MethodNode existingMethod = existingEntry.getKey();
			Long existingKey = existingEntry.getValue();
			AbstractMethodNode existingBaseMethod = hierarchy.getBaseMethod(existingMethod);
			if (existingBaseMethod != null) {
				Long existingBaseMethodKey = existingKeys.get(existingBaseMethod);
				if (existingBaseMethodKey == null) {
					existingBaseMethodKey = keysByMethod.get(existingBaseMethod);
				}

				if (existingBaseMethodKey != null) {
					if (!existingBaseMethodKey.equals(existingKey)) {
						changeLogContext.logError(
							"Method '"
								+ existingMethod.getOriginalNameWithParams()
								+ "' in class '"
								+ existingMethod.getClassNameBinary()
								+ "' has a different 'Method Parameter List Changing' mapping than method '"
								+ existingBaseMethod.getOriginalNameWithParams()
								+ "' in class '"
								+ existingBaseMethod.getClassNameBinary()
								+ "'. Will use the mapping of the method in class '"
								+ existingBaseMethod.getClassNameBinary()
								+ "'. You must distribute this application as a whole."
						);
					}

					keysByMethod.put((MethodNode)existingBaseMethod, existingBaseMethodKey);
					keysByMethod.put(existingMethod, existingBaseMethodKey);
					seenKeys.add(existingBaseMethodKey);
				} else {
					if (existingBaseMethod.isWorkspaceNode()) {
						keysByMethod.put((MethodNode)existingBaseMethod, existingKey);
					}

					keysByMethod.put(existingMethod, existingKey);
					seenKeys.add(existingKey);
				}
			} else {
				keysByMethod.put(existingMethod, existingKey);
				seenKeys.add(existingKey);
			}
		}

		HashSet<ClassNode> classesWithInnerClasses = GlobalUtils.newHashSet();

		for (MethodNode methodToChange : methodsToChange) {
			if (methodToChange.getMethodOwner().hasInnerClasses()) {
				classesWithInnerClasses.add(methodToChange.getMethodOwner());
			}

			if (!keysByMethod.containsKey(methodToChange) && (!methodToChange.isGeneratedMember() || methodToChange.isClinit() && methodToChange.isGeneratedByControlFlow())) {
				AbstractMethodNode baseMethod = hierarchy.getBaseMethod(methodToChange);
				if (baseMethod != null) {
					Long baseMethodKey = keysByMethod.get(baseMethod);
					if (baseMethodKey != null) {
						keysByMethod.put(methodToChange, baseMethodKey);
						continue;
					}
				}

				Long newKey;
				if (Options.useFixedParamSalt) {
					newKey = 255L;
				} else {
					int retryCount = 0;

					do {
						newKey = newRandomLong();
					} while (!seenKeys.add(newKey) || retryCount++ > 2);
				}

				keysByMethod.put(methodToChange, newKey);
				if (baseMethod != null && baseMethod.isWorkspaceNode()) {
					keysByMethod.put((MethodNode)baseMethod, newKey);
				}
			}
		}

		for (ClassNode ownerClass : classesWithInnerClasses) {
			for (MethodNode ownerMethod : ownerClass.getMethodNodes()) {
				Long methodKey = keysByMethod.get(ownerMethod);
				if (methodKey != null) {
					MethodSignature ownerSignature = ownerMethod.getMethodSignature();
					Iterator<AbstractClassNode> signatureLookupIterator = ownerClass.getInnerClasses().iterator();

					while (signatureLookupIterator.hasNext()) {
						MethodNode matchedMethod = (MethodNode)signatureLookupIterator.next().findMethod(ownerSignature);
						if (matchedMethod != null) {
							keysByMethod.put(matchedMethod, methodKey);
						}
					}
				}
			}

			HashMap<MethodSignature, Long> keysBySignature = GlobalUtils.newHashMap();
			Iterator<AbstractClassNode> innerClassIterator = ownerClass.getInnerClasses().iterator();

			while (innerClassIterator.hasNext()) {
				for (AbstractMethodNode innerMethod : innerClassIterator.next().getMethods()) {
					if (!keysByMethod.containsKey(innerMethod)) {
						MethodSignature innerSignature = innerMethod.getMethodSignature();
						Long signatureKey = keysBySignature.get(innerSignature);
						if (signatureKey != null) {
							keysByMethod.put((MethodNode)innerMethod, signatureKey);
						} else {
							signatureKey = newRandomLong();
							keysByMethod.put((MethodNode)innerMethod, signatureKey);
							keysBySignature.put(innerSignature, signatureKey);
						}
					}
				}
			}
		}

		return keysByMethod;
	}

	public static void splitLong(long value, AtomicInt highByte, AtomicLong middleBits, AtomicInt lowByte) {
		long _8H = value >>> 56;
		highByte.set((int)_8H);
		long _48M = value << 8 >>> 16;
		middleBits.set(_48M);
		long _8L = value << 56 >>> 56;
		lowByte.set((int)_8L);
	}

	public static int[] createBitPermutation(Random random) {
		ArrayList<Integer> candidateIndices = new ArrayList<>();

		for (int bitIndex = 0; bitIndex < 64; bitIndex++) {
			candidateIndices.add(INTEGER_CACHE.valueOf(bitIndex));
		}

		GlobalUtils.shuffle(candidateIndices, random);
		int[] bitPermutation = new int[64];

		for (int pairIndex = 0; pairIndex < 64; pairIndex++) {
			int firstBitIndex = candidateIndices.get(pairIndex);
			int secondBitIndex = candidateIndices.get(pairIndex + 1);
			int bitDelta = firstBitIndex - secondBitIndex;
			bitPermutation[firstBitIndex] = bitDelta;
			bitPermutation[secondBitIndex] = -bitDelta;
			pairIndex++;
		}

		return bitPermutation;
	}

	public static long extractPermutedBitRange(long value, int fromBit, int toBit, int[] bitPermutation) {
		return extractBitRange(permuteBits(value, bitPermutation), fromBit, toBit);
	}

	public static long newRandomLong() {
		return nextRandomLong();
	}

	public static ReferenceGraph<ClassNode, ClassNode> buildInnerClassReferenceGraph(ClassTreeManager classTreeManager) {
		ReferenceGraph<ClassNode, ClassNode> referenceGraph = new ReferenceGraph<>();
		Enumeration<ClassHierarchyNode> hierarchyNodes = classTreeManager.getValidInnerClassNodes();

		while (hierarchyNodes.hasMoreElements()) {
			ClassHierarchyNode hierarchyNode = hierarchyNodes.nextElement();
			if (hierarchyNode.isWorkspaceClass()) {
				ClassNode classNode = (ClassNode)hierarchyNode.getClassNode();
				if (classNode.isLocalOrAnonymous() && !classNode.isStaticInnerClass()) {
					ClassHierarchyNode superHierarchyNode = hierarchyNode.getSuperClassNode();
					if (superHierarchyNode.isWorkspaceClass()) {
						ClassNode superClassNode = (ClassNode)superHierarchyNode.getClassNode();
						referenceGraph.addReference(superClassNode, classNode);
					}
				}
			}
		}

		return referenceGraph;
	}

	private static String getHashTableInternalName(boolean java5OrNewer) {
		String internalName;
		if (Options.ZKM_METHOD_PARAMETER_MAP_CLASS != null) {
			internalName = Options.ZKM_METHOD_PARAMETER_MAP_CLASS.replace('.', '/');
		} else if (java5OrNewer) {
			internalName = "java/util/concurrent/ConcurrentHashMap";
		} else {
			internalName = "java/util/Hashtable";
		}

		return internalName;
	}

	public static long permuteAllBits(long value, int[] bitPermutation) {
		return extractPermutedBitRange(value, 0, 63, bitPermutation);
	}

	public static long packAndPermuteLongParts(int indexBits, long midBits, int lowBits, int[] bitPermutation) {
		return permutePackedBits(packLongParts(indexBits, midBits, lowBits), bitPermutation);
	}

	private static long packLongParts(int indexBits, long midBits, int lowBits) {
		return 0L | (long)indexBits << 56 | (midBits & 281474976710655L) << 8 | lowBits;
	}

	public boolean isVeryLargeApplication() {
		return this.veryLargeApplication;
	}

	@Override
	public ClassNode generate(String className, int majorVersion, int minorVersion, AuxiliaryWiringStrategy wiringStrategy) {
		MethodParamObfuscationContext obfuscationContext = (MethodParamObfuscationContext)wiringStrategy;
		int decrypterRoleIndex = obfuscationContext.getDecrypterRoleIndex(className);

		try {
			byte[] classBytes;
			switch (decrypterRoleIndex) {
				case 0:
					if (majorVersion < 49) {
						classBytes = GlobalUtils.baseNStringToByteArray(
							"A0A'A)A~zzz7zzzfzzz5zyzzz~zyzzzaz_zzz)z8z*_zzUz_zzz)z8z/z*z/z_zzz)z8z/z*z!z_zzzmz8zXzU_OzU_OzUz=z*z!z_zzzmz8zXzU_OzU_OzUz=z*z|z_zzzhzU_OzU_OzUz_zzz__yz_zzz__Fz_zzzG_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_ZzAz_zzz_zzzAzzzzzzzzzzzhz)z_zzzmzzzFzzzzz)z_zzz+zzz7zzzzz)z_zzzmzzz)zzzzz)z_zzz+zzzhzzzzz)z_zzz+zzzyzzzzzzzz",
							"z_A7)hFy~+ma5RO2G`lj0?EBTZ{1s4oWd}-t3w(L8*v,xfDPHS@Vu&Y#6]$= <[.qKiJbn>M'U/cXkC;IQgrN:!ep9|"
						);
					} else {
						classBytes = GlobalUtils.baseNStringToByteArray(
							"A0A'A)A~zzzzzzzSzzz5zyzzzmzyzzzaz_zzz__Fz_zzzmz8zXzU_OzU_OzUz=z*z!z_zzz__yz_zzz)z8z*_zzUz_zzz)z8z/z*z/z_zzz)z8z/z*z!z_zzzmz8zXzU_OzU_OzUz=z*z|z_zzzhzU_OzU_OzUz_zzzG_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_ZzFz_zzz_zzzAzzzzzzzzzzzhz)z_zzz7zzz)zzzzz)z_zzzhzzzFzzzzz)z_zzz7zzzyzzzzz)z_zzzhzzz~zzzzz)z_zzzhzzz+zzzzzzzz",
							"z_A7)hFy~+ma5RO2G`lj0?EBTZ{1s4oWd}-t3w(L8*v,xfDPHS@Vu&Y#6]$= <[.qKiJbn>M'U/cXkC;IQgrN:!ep9|"
						);
					}
					break;
				case 1:
					if (majorVersion < 49) {
						classBytes = GlobalUtils.baseNStringToByteArray(
							"A0A'A)A~zzz7zzzfzzz[zyzzz*zyzzzDzyzzzYzyzzz#zyzzz6zmzzz7zzz`zmzzzhzzzlzmzzzAzzzjzmzzz)zzz0z+zzzAzzz?z+zzzAzzzEz+zzzAzzzBz+zzzAzzzTzmzzzhzzzZzazzz_zzz{z+zzzAzzz1z5zzzLzzzsz5zzzSzzz3z5zzz@zzz-z5zzzuzzztz5zzz&zzzPz5zzz@zzzfz5zzzVzzzxz5zzz]zzz,z5zzz$zzzwz5zzzVzzzdz5zzz zzzvz_zzz7z8z*z!z_zzz)z8z*_zzUz_zzz)z8z/z*z/z_zzz)z8z/z*z!z_zzzmz8zXzU_OzU_OzUz=z*z!z_zzzmz8zXzU_OzU_OzUz=z*z|z_zzzBz8zXzU_OzU_OzUz=zXzU_OzU_OzUz=z*zXzU_OzU_OzUz=z_zzz?z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zUz_zzz(z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzz6z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzz)z8z|z*z!z_zzzFz _O_j_O_Zz[z_zzz)zJ_0_+_mz_zzzhzU_OzU_OzUz_zzz_z/z_zzzyzXzU_OzU_OzUz=z_zzzyzXzX_`zX_`zXz=z_zzz?zX_2_F_1_FzP_{_Z_O_`zPz'_F_T_R_Z_F_y_`_mz=z_zzzhzX_`zX_`zXz_zzz_z|z_zzzA_zzUz_zzz7_5_m_Zz_zzz__yz_zzz__Fz_zzzG_O_+_m_j_Z_O_Z_oz'_F_T_RzJ_0_+_mz_zzz__mz_zzzG_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz_zzzG_2_F_1_FzP_`_F_j_5zPzr_o_T_Z_m_lz_zzzj_2_F_1_FzP_{_Z_O_`zPz'_F_T_R_Z_F_y_`_mz_zzz__~z_zzz7_?_{_Zz_zzz__+z_zzz__az_zzz__5zzz_zzzAzzz7zzz_zzz_zzzFzzzmzzz&zzzPzzzzzzzmzzzVzzzxzzzzzzzAzzz@zzzfzzzzzzzAzzz]zzz,zzzzzzzAzzz<zzzHzzzzzzzAzzz zzzvzzzzzzzazzz~zzzVzzz(zzz_zzz8zzzzzzz`zzz_zzz_zzzzzzzhz{_pzzzm_!zzzzzzzzzzz~zzzVzzz-zzz_zzz8zzzzzzzBzzz7zzz7zzzzzzza_ezzz5zvz,A_zzz~zkzx_:zzzzzzzzzzzAzzz@zzz-zzz_zzz8zzzzzzzvzzz7zzz)zzzzzzzozv_9zzzaz,AzzzzyAmzzz_zCzfA`zzzhzvzCzv_9zzzazxz,AzzzzOzezf_:zzzzzzzzzzzzzzz=zzzszzz_zzz8zzzzzzzRzzzzzzz_zzzzzzz__!zzzzzzzzzzzzzzz]zzzszzz_zzz8zzzzzzzRzzzzzzz_zzzzzzz__!zzzzzzzzzzz_zzzVzzzdzzz_zzz8zzzzzzz*zzzAzzzAzzzzzzz4zvz,_/zzz{zv_9zzzRA`zzz+zvz,_|zzzR_!zv_9zzzRz,A7zzz2zAzz_!zzzzzzzzzzz_zzzVzzzozzz_zzz8zzzzzzzOzzzAzzz7zzzzzzzAz+_gzzzzzzzzzzz_zzz@zzz4zzz_zzz8zzzzzzzOzzz_zzz_zzzzzzzAz__:zzzzzzzzzzz_zzz@zzz}zzz_zzz8zzzzzzzxzzzAzzzAzzzzzzzdzvz,_czzzhz)_Qz,AazzzA_[zzzjzvAAzzz+z,AAzzz+_+_Jzzzhz7_Qz)_Qz7_Qzzzzzzzzzzz_zzz@zzzWzzz_zzz8zzzzzzzlzzz7zzz7zzzzzzzFzvzW_|zzzG_!zzzzzzzzzzz_zzzLzzzszzz_zzz8zzzzzzz`zzz_zzz_zzzzzzzhzvA_zzzF_!zzzzzzzzzzzz",
							"z_A7)hFy~+ma5RO2G`lj0?EBTZ{1s4oWd}-t3w(L8*v,xfDPHS@Vu&Y#6]$= <[.qKiJbn>M'U/cXkC;IQgrN:!ep9|"
						);
					} else {
						classBytes = GlobalUtils.baseNStringToByteArray(
							"A0A'A)A~zzzzzzzSzzz[zmzzzOzzz(z+zzzazzzLz+zzzazzz8zmzzzazzz*z+zzzazzzvzmzzz,zzzxzyzzzfzmzzz,zzzDz+zzzazzzPzazzzyzzzHzyzzzSzmzzz@zzzVz+zzzazzzuzyzzz&z_zzz__mz_zzz_z|z_zzz__Fz_zzzyzXzX_`zX_`zXz=z_zzz__yz_zzz8zX_2_F_1_FzP_{_Z_O_`zP_~_0_j_~_{_B_B_m_j_ZzPzJ_0_j_~_{_B_B_m_j_Zz'_F_T_Rzk_F_?z=z_zzz__~z_zzzyzXzU_OzU_OzUz=z_zzz__5z_zzzA_zzUz_zzz__az_zzz_z/z_zzzFz _O_j_O_Zz[z_zzz7z8z*z!z_zzz)zJ_0_+_mz_zzz)z8z|z*z!z_zzzBz8zXzU_OzU_OzUz=zXzU_OzU_OzUz=z*zXzU_OzU_OzUz=z_zzz__+z_zzzmz8zXzU_OzU_OzUz=z*z!z_zzz)z8z/z*z/z_zzz)z8z*_zzUz_zzzmz8zXzU_OzU_OzUz=z*z|z_zzz)z8z/z*z!z5zzz1zzzsz5zzz2zzzGz5zzz`zzzlz5zzzjzzzWz5zzzjzzz0zyzzzYz5zzz#zzz6z_zzzhzU_OzU_OzUz5zzz]zzz$z5zzz?zzzEz5zzz`zzz}z_zzzhzX_`zX_`zXzyzzz=z5zzz zzz<z5zzzZzzz{z_zzzG_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz_zzz(_2_F_1_FzP_{_Z_O_`zP_~_0_j_~_{_B_B_m_j_ZzPzJ_0_j_~_{_B_B_m_j_Zz'_F_T_Rzk_F_?z_zzz7_5_m_Zz_zzz(z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzz7_?_{_Zz_zzz6z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzzG_2_F_1_FzP_`_F_j_5zPzr_o_T_Z_m_lz_zzzG_O_+_m_j_Z_O_Z_oz'_F_T_RzJ_0_+_mz_zzz?z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zUzzz}zzzazzzOzzz_zzzyzzzFzzzmzzz2zzzGzzzzzzzmzzz`zzzlzzzzzzzAzzzjzzz0zzzzzzzAzzz?zzzEzzzzzzzAzzzBzzzTzzzzzzzAzzzZzzz{zzzzzzzazzz_zzz1zzzszzz_zzz4zzzzzzz`zzz_zzz_zzzzzzzhzvA_zzz__!zzzzzzzzzzz~zzz`zzzozzz_zzz4zzzzzzz`zzz_zzz_zzzzzzzhz{_pzzzA_!zzzzzzzzzzz~zzz`zzzWzzz_zzz4zzzzzzzBzzz7zzz7zzzzzzza_ezzz7zvz,A_zzz)zkzx_:zzzzzzzzzzzAzzzjzzzWzzz_zzz4zzzzzzz,zzz7zzzhzzzzzzzWzv_9zzzhz,AzzzzFAmzzzyzCzfA`zzzhzvzCzv_9zzzhzxz,Azzzz~z$z)zf_:zzzzzzzzzzzzzzzdzzzszzz_zzz4zzzzzzzRzzzzzzz_zzzzzzz__!zzzzzzzzzzzzzzz?zzzszzz_zzz4zzzzzzzRzzzzzzz_zzzzzzz__!zzzzzzzzzzz_zzz`zzz}zzz_zzz4zzzzzzz,zzzAzzzAzzzzzzzWzvz,_/zzzszv_9zzz+A`zzzazvz,_|zzz+_XzzzRzv_9zzz+z,A7zzzmzAzz_!zzzzzzzzzzz_zzz`zzz-zzz_zzz4zzzzzzzOzzzAzzz7zzzzzzzAz+_gzzzzzzzzzzz_zzzjzzztzzz_zzz4zzzzzzzOzzz_zzz_zzzzzzzAz__:zzzzzzzzzzz_zzzjzzz3zzz_zzz4zzzzzzzDzzzAzzzAzzzzzzz-zvz,_czzzhz)_Qz,Aazzza_[zzz?zvAAzzz5z,AAzzz5_+_izzzyz)_Xzzz)z7_Qz7_Qzzzzzzzzzzz_zzzjzzzwzzz_zzz4zzzzzzzlzzz7zzz7zzzzzzzFzvzW_|zzzR_!zzzzzzzzzzzz",
							"z_A7)hFy~+ma5RO2G`lj0?EBTZ{1s4oWd}-t3w(L8*v,xfDPHS@Vu&Y#6]$= <[.qKiJbn>M'U/cXkC;IQgrN:!ep9|"
						);
					}
					break;
				default:
					if (majorVersion < 49) {
						classBytes = GlobalUtils.baseNStringToByteArray(
							"A0A'A)A~zzz7zzzfzz_wzyzz_7zyzz_Fzyzz_azyzz_Ozyzz_ozyzz_Wzmzzzhzzz(zmzzzFzzzLzmzzz_zzz8z+zzz_zzz*z+zzz_zzzvz+zzz_zzz,zmzzzFzzzxz+zzz_zzzfz+zzz_zzzDz+zzz_zzzPzmzzzhzzzHzmzzz_zzzSzmzzzFzzz@z+zzz_zzzVzmzzz7zzzuzmzzz_zzz&zazzzAzzzYz+zzz_zzz#zazzzAzzz6z+zzz_zzz]z+zzz_zzz$zmzzz_zzz=zmzzz_zzz z+zzz_zzz<zmzzzFzzz[zazzzAzzz.zmzzzFzzzqz+zzz_zzzKz+zzz_zzzizmzzz7zzzJz+zzz_zzzbz5zz__zzzMz5zz__zzzcz5zz__zzzQz5zz_hzz_)z5zz_5zz_)z5zz_Rzz_)z5zz_Gzzzpz5zz_`zz_2z5zz_lzz_2z5zz_jzz_mz5zz_0zzz>z5zz_?zzzgz5zz_Ezzz/z5zz_?zz_yz5zz_?zzz!z5zz_{zzzIz5zz_?zzz;z5zz_4zz_mz5zz_1zzz:z5zz_dzz_+z5zz_}zz_Oz5zz_?zzzkz5zz_?zzzCz5zz_Zzz_~z5zz_-zzz|z5zz_?zzzNz5zz_tzzznz5zz_{zz_yz5zz_1zz_Oz5zz_?zz_zz5zz_3zz_)z_zzz7z8z*zUz_zzz0z8z*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzz7z8z*z!z_zzz)z8z*_zzUz_zzz)z8zUz*z/z_zzz?z8zUz*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzz)z8zUz*z!z_zzzhz8zUzUz*z/z_zzzvz8zUzUzUzX_2_F_1_FzP_{_Z_O_`zPz!_m_~_Z_0_Bz=zX_2_F_1_FzP_{_Z_O_`zPz!_m_~_Z_0_Bz=z*z!z_zzzvz8zUzUzX_2_F_1_FzP_{_Z_O_`zPz!_m_~_Z_0_Bz=zX_2_F_1_FzP_{_Z_O_`zPz!_m_~_Z_0_Bz=zUz*z!z_zzz)z8z/z*z/z_zzzmz8z/z*zXzU_OzU_OzUz=z_zzz)z8z/z*z!z_zzzmz8z/zUzU_zzU_zz/z*z/z_zzz4z8z/z/zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zXzU_OzU_OzUz=z_zzzmz8zXzU_OzU_OzUz=z*z!z_zzzmz8zXzU_OzU_OzUz=z*z|z_zzzBz8zXzU_OzU_OzUz=zXzU_OzU_OzUz=z*zXzU_OzU_OzUz=z_zzzmz8zXzX_`zX_`zXz=z*z!z_zzz?z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*z!z_zzz?z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*z|z_zzzEz8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=zUz*z!z_zzz)z8z|z*z!z_zzzFz _O_j_O_Zz[z_zzz)zJ_0_+_mz_zzzhz'_Rz'_Rz'z_zzz_zUz_zzz__Rz_zzzhzU_OzU_OzUz_zzz_z/z_zzzyzXzU_OzU_OzUz=z_zzzlzX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzzlzX_2_F_1_FzP_{_Z_O_`zPz!_m_~_Z_0_Bz=z_zzzhzX_`zX_`zXz_zzz__jz_zzz__`z_zzzA_zzUz_zzzA_zz/z_zzzm_F_+_+zn_`_m_l_m_j_Zz_zzz__Gz_zzz__2z_zzz__lz_zzzh_~_`_0_j_mz_zzz__Fz_zzz+_m_`_m_l_m_j_ZzK_Zz_zzzF_m_E_{_F_`_Tz_zzz__5z_zzz__~z_zzz__+z_zzz__yz_zzz~_R_F_T_RzJ_0_+_mz_zzz__az_zzzG_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz_zzzG_2_F_1_FzP_{_Z_O_`zPz!_m_~_Z_0_Bz_zzz__0z_zzz__mz_zzz5_T_m_Zzn_`_m_l_m_j_ZzK_Zz_zzz)_T_O_W_mz_zzz__Ozzz_zzz_zzzhzzz_zzzAzzzOzzzAzz_?zz_yzzzzzzzAzz_1zz_OzzzzzzzAzz_Zzz_~zzzzzzzAzz_{zz_yzzzzzzzAzz_lzz_2zzzzzzzmzz_`zz_2zzzzzzz~zz_}zz_Ozzzzzzzmzz_4zz_mzzzzzzzmzz_jzz_mzzzzzzzmzz_3zz_)zzzzzzzmzz_dzz_+zzzzzzzmzz_hzz_)zzzzzzzmzz_Rzz_)zzzzzzzmzz_5zz_)zzzzzzz0zzz+zz_Tzzz>zzz_zz_AzzzzzzzGzzz_zzzzzzzzzzz)_ezzz{_:zzzzzzzzzzz+zz_?zzzrzzz_zz_Azzzzzzz.zzz)zzz~zzzzzzzVzoz+_]_izzzyz7_Xzzz)z)AAzzz3zoAAzzzEz$zhzdAAzzzEz$zFzZzhzZzFAAzzz?z$zyzZz)AGzzza_ezzzGzZz)AzzzzRzZzy_:zzzzzzzzzzz~zz_ZzzzIzzz_zz_Azzzzzz__zzzFzzz)zzzzzzzIzo_ezzzmzGz._ezzz1_ezzzOAAzzzl_fz<zs_ezzzw_MzzzG_ezzzTzsAzzzzjAmzzzAzCzf_:_ezzzTAzzzz}_ezzz5_?_.zzz2_ezzz1Azzzz`Amzzz)_pzzz1Ahzzz_z9zoA_zzz+zC_ezzzTzfAzzzzRzf_:zzzzzzzzzzzmzz_{zzzIzzz_zz_AzzzzzzzBzzz)zzz7zzzzzzzaAhzzz_z9zoA_zzz+zkzx_:zzzzzzzzzzz~zz_?zzzezzz_zz_AzzzzzzzRzzzzzzz_zzzzzzz__!zzzzzzzzzzz~zz_1zzzezzz_zz_AzzzzzzzRzzzzzzz_zzzzzzz__!zzzzzzzzzzzAzz__zzzQzzz_zz_Azzzzzzz3zzz7zzz7zzzzzzzTzvA_zzzyzvzW_|zzz0zv_ezzz1_|zzztzv_ezzzO_|zzz2_!zzzzzzzzzzz_zz_?zzz;zzz_zz_AzzzzzzzMzzzFzzzyzzzzzzz=zv_9zzz0zGz~zGz#zv_9zzztzv_9zzz2AAzzzlzizv_9zzz0zWzv_9zzz-_8_8z#zhzvzEzh_|zzz0zv_9zzzoAGzzzOzv_9zzzozWA7zzzBz7zzzpz}_gzzzzzzzzzzz_zz_1zzzQzzz_zz_Azzzzzzzlzzz7zzz7zzzzzzzFzvzW_|zzz-_!zzzzzzzzzzz_zz_?zzzNzzz_zz_Azzzzzzz*zzzAzzzAzzzzzzz4zvz,_/zzz{zv_9zzzoA`zzz+zvz,_|zzzo_!zv_9zzzoz,A7zzzdzAzz_!zzzzzzzzzzz_zz_szzznzzz_zz_AzzzzzzzdzzzFzzz_zzzzzzz0zv_9zzz0z7zGzyzv_9zzztzv_9zzz2AAzzzl_f_Qzzzzzzzzzzz_zz_Bzzz9zzz_zz_Azzzzzzzkzzz~zzz7zzzzzzzKzvz,_czzzhz)_Qz,Aazzz__[zzzuzv_9zzz0z7zGz#zv_9zzztzv_9zzz2AAzzzlz,Amzzz_zkzx_9zzz0z7zGz#zx_9zzztzx_9zzz2AAzzzl_]_[zzzhz7_Qz)_Qz7_Qzzzzzzzzzzz_zz_1zzz:zzz_zz_AzzzzzzzQzzz~zzz7zzzzzzznzvz,_czzzhz)_Qz,Aazzz__[zzz6zv_9zzz0zGz6zGz.zv_9zzztzv_9zzz2AAzzzlz,Amzzz_zkzx_9zzz0zGz6zGz.zx_9zzztzx_9zzz2AAzzzl_mz+_]_Jzzzhz7_Qz)_Qz)_Qzzzzzzzzzzz_zz_1zzz'zzz_zz_Azzzzzzz`zzz_zzz_zzzzzzzhzv_9zzzt_:zzzzzzzzzzzAzz_?zzzUzzz_zz_Azzzzzzz-zzzFzzz7zzzzzzzEz1z)_+z<zv_9zzz0z7zszv_9zzztzv_9zzz2AAzzzl_gzzzzzzzzzzzAzz_?zzzXzzz_zz_AzzzzzzzozzzFzzz7zzzzzzzlzv_9zzz0z1zszv_9zzztzv_9zzz2AAzzzl_gzzzzzzzzzzzmzz_?zzzgzzz_zz_Azzzzzz_ zzz)zzzOzzzzzz_Hz+z#zFzZz)A~zYz~z7zYz+_Xzzzbzo_ezzzOz?z+zP_3z#zmzZz)z?z+zDzYz5zEzmz+_]_[zzz*z?z5_JzzzRzEzmz?z5_-z#zm_Xzzzjz?z5_KzzzOzEzmz?z5zA_Lz)_h_oz#zmzEzFzEzm_(z#zF_*z+z_z?z+z?z~_>AUAhzGzqzYz~zEzFz#zmz?z~z)_+z4_+zYz5z?z5_JzzzmzEzmz?z5_oz#zmzsz?z~_hz)_+z4_+zYzRz?zR_JzzzmzEzmz?zR_-z#zmzEzm_gzzzzzzzzzzzmzz_ZzzzMzzz_zz_Azzzzzzz>zzzhzzz7zzzzzzz$_ezzzTAzzzz}z=AhzzzFz9z{A_zzz~zXz7z<_Xzzz`z,_ezzzTzsAzzzzjAzzzzR_*zAz_zsz{_>AUA$z7_ezzzTAzzzz}z)_+_ezzzTz,z7AAzzz4_!zzzzzzzzzzzmzz_?zzzCzzz_zz_AzzzzzzznzzzhzzzFzzzzzzz]z{z1_MzzzYz{z1z{_+zh_`_hzYzh_*z)z_z?z)_ezzza_MzzzZz{z?zhzxzfz?z)AAzzz4z?zhz)_hz1zxzfz?z)AAzzz4z{z?zhz1zxzfAAzzzs_!zzzzzzzzzzzmzz_?zzzkzzz_zz_Azzzzzz_Mzzz7zzz+zzzzzz_=z{zYzhz1z)_hzYzFz{zYzy_XzzzjzZz)zfz?zyAzzzzjz?zyAzzzzW_*zyz_z?zyzs_UAUA#_Xzzz/zZz)z?zhAzzzzjAmzzzAzZz)z?zFAzzzzjAmzzzAA7zzzZzAzz_[zzz?zZz)z?zh_*zhz_AzzzzjAmzzzAz$z~_XzzzlzZz)z?zF_*zFz_AzzzzjAmzzzAz$z~zfzZz~z{AzzzzW_*zzz_z?zhz1_'zzzoz?zFzs_UAU_:_Xzzz?zfzZz)z?zhAzzzzjz{AzzzzW_*zzz__*zhz_z?zhz1_UAUA&_!zzzzzzzzzzzz",
							"z_A7)hFy~+ma5RO2G`lj0?EBTZ{1s4oWd}-t3w(L8*v,xfDPHS@Vu&Y#6]$= <[.qKiJbn>M'U/cXkC;IQgrN:!ep9|"
						);
					} else {
						classBytes = GlobalUtils.baseNStringToByteArray(
							"A0A'A)A~zzzzzzzSzz_xz+zzzjzzzNzmzzz:zzz!zmzzzjzzzezmzzz:zzzpz+zzzjzzz9zmzzz|zz_zz+zzzjzz__z+zzzjzz_Az+zzzjzz_7zmzzzjzz_)z+zzzjzz_hz+zzzjzz_Fzmzzzdzz_yzyzz_~zmzzzdzz_+z+zzzjzz_mzmzzzlzz_azyzzz,zyzz_5zmzzzjzz_Rzmzzzdzz_zzmzzzLzz_Oz+zzzjzz_2z+zzzjzz_Gz+zzzjzz_`zmzzzjzz_lz+zzzjzz_jz+zzzjzz_0zazzzOzz_?zazzzOzz_Ezmzzzjzz_Bzyzz_Tzmzzzdzz_Zzmzzzjzz_{z+zzzjzz_1zmzzzjzz_szmzzzdzz_4zazzzOzz_ozyzz_Wz_zzz__Fz_zzz_z/z_zzz__yz_zzzA_zzUz_zzz__~z_zzzyzXzU_OzU_OzUz=z_zzz__+z_zzz__2z_zzzA_zz/z_zzz__Gz_zzz__mz_zzz__az_zzz?zX_2_F_1_FzP_{_Z_O_`zPzK_B_B_F_ozX_O_T_Zz=z_zzz__lz_zzzlzX_2_F_1_FzP_{_Z_O_`zPz!_m_~_Z_0_Bz=z_zzz__Oz_zzz_zUz_zzz__0z_zzzlzX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzz__Rz_zzz__`z_zzz__jz_zzz__5z_zzz0z8z*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzz)zJ_0_+_mz_zzz4z8z/z/zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zXzU_OzU_OzUz=z_zzzmz8z/z*zXzU_OzU_OzUz=z_zzzmz8zXzX_`zX_`zXz=z*z!z_zzzFz _O_j_O_Zz[z_zzz)z8z/z*z!z_zzz)z8z/z*z/z_zzzmz8zXzU_OzU_OzUz=z*z!z_zzz~_R_F_T_RzJ_0_+_mz_zzz7z8z*zUz_zzzF_m_E_{_F_`_Tz_zzz?z8zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*z|z_zzzmz8zXzU_OzU_OzUz=z*z|z_zzz)z8z*_zzUz_zzz)z8zUz*z/z_zzzhz8zUzUz*z/z_zzzmz8z/zUzU_zzU_zz/z*z/z_zzz7z8z*z!z_zzzHz8zUzUzX_2_F_1_FzP_{_Z_O_`zPzK_B_B_F_ozX_O_T_Zz=zX_2_F_1_FzP_{_Z_O_`zPzK_B_B_F_ozX_O_T_Zz=zUz*z!z_zzzHz8zUzUzUzX_2_F_1_FzP_{_Z_O_`zPzK_B_B_F_ozX_O_T_Zz=zX_2_F_1_FzP_{_Z_O_`zPzK_B_B_F_ozX_O_T_Zz=z*z!z5zzz]zzz$zyzz_dz5zzz8zz_}z5zzzDzzziz5zzz8zz_-z5zzz&zzzYzyzz_tz5zz_3zzzcz5zzz=zzz6z5zzz@zzz,z5zzzSzzzHz5zzz8zzzIz5zzz#zzz6z5zzzVzzzuz5zz_wzz_(z_zzzhzU_OzU_OzUz5zz_LzzzUz5zzz zzz6z5zz_8zzz.z_zzzhz'_Rz'_Rz'z5zzzbzzznz5zzzbzzzQz5zzz8zzz*z5zzzvzzz,z5zzzPzzzHz5zzz8zzz;z5zzzDzzz*z5zzzxzzzfz5zzz8zzz>z5zzz8zzzMz5zzz8zzzCz_zzzj_2_F_1_FzP_{_Z_O_`zPzK_B_B_F_ozX_O_T_Zz5zzzbzz_*z5zzz8zzzgz5zzz<zzz6z5zzz8zzzrz5zz_vzz_,z5zzzvzzzXz_zzzG_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz_zzzhzX_`zX_`zXz_zzz)z8z|z*z!z_zzzBz8zXzU_OzU_OzUz=zXzU_OzU_OzUz=z*zXzU_OzU_OzUz=z_zzzG_2_F_1_FzP_{_Z_O_`zPz!_m_~_Z_0_Bz_zzz7_F_+_+z_zzz7_5_m_Zz_zzz?z8zUz*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z_zzz)_T_O_W_mz_zzzh_~_`_0_j_mz_zzzZz8zX_2_F_1_FzP_{_Z_O_`zPzJ_0_`_`_m_~_Z_O_0_jz=z*z!z_zzz7_T_m_Zz_zzzLz8zUzX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=z*zX_2_F_1_FzP_`_F_j_5zPz;_y_2_m_~_Zz=zzz}zzzjzzzLzzz_zzzOzzzOzzzAzzz8zzz*zzzzzzzAzzzvzzz,zzzzzzzAzzzxzzzfzzzzzzzAzzzDzzz*zzzzzzzAzzzPzzzHzzzzzzzmzzzSzzzHzzzzzzz~zzz@zzz,zzzzzzzmzzzVzzzuzzzzzzzmzzz&zzzYzzzzzzzmzzz#zzz6zzzzzzzmzzz]zzz$zzzzzzzmzzz=zzz6zzzzzzzmzzz zzz6zzzzzzzmzzz<zzz6zzzzzzz0zzz+zzz[zzz.zzz_zzzqzzzzzzzGzzz_zzzzzzzzzzz)_ezzz__:zzzzzzzzzzz+zzz8zzzKzzz_zzzqzzzzzzzqzzz)zzz~zzzzzzzuzoz+_]_Jzzzyz)_Xzzz)z7AAzzzAzoAAzzz7z$zhzdAAzzz7z$zFzZzhzZzFAAzzz)z$zyzZz)AGzzz5_ezzzhzZz)AzzzzFzezZzy_:zzzzzzzzzzz~zzzxzzzizzz_zzzqzzzzzz_hzzzFzzz)zzzzzzzNzo_ezzzyzGz._ezzz~_ezzz+AAzzzm_fz<zs_ezzza_MzzzG_ezzz5zsAzzzzRAmzzzOzCzf_:_ezzz5Azzzz2_ezzzG_?_.zzzl_ezzz~Azzzz`AmzzzlAmzzzl_pzzz~Ahzzzjz9zoA_zzz0zC_ezzz5zfAzzzz?zezf_:zzzzzzzzzzzmzzzDzzzizzz_zzzqzzzzzzzBzzz)zzz7zzzzzzzaAhzzzjz9zoA_zzz0zkzx_:zzzzzzzzzzz~zzz8zzzJzzz_zzzqzzzzzzzRzzzzzzz_zzzzzzz__!zzzzzzzzzzz~zzzvzzzJzzz_zzzqzzzzzzzRzzzzzzz_zzzzzzz__!zzzzzzzzzzzAzzzbzzznzzz_zzzqzzzzzzz3zzz7zzz7zzzzzzzTzvA_zzzEzvzW_|zzzBzv_ezzz~_|zzzTzv_ezzz+_|zzzZ_!zzzzzzzzzzz_zzz8zzz>zzz_zzzqzzzzzzz zzzFzzzyzzzzzzzHzvzGz~zGz#A_zzz{zizv_9zzzBzWzv_9zzz1_8_8z#zhzvzEzh_|zzzBzv_9zzzsAGzzzOzv_9zzzszWA7zzz4z7zzzpz}_gzzzzzzzzzzz_zzzvzzznzzz_zzzqzzzzzzzlzzz7zzz7zzzzzzzFzvzW_|zzz1_!zzzzzzzzzzz_zzz8zzzMzzz_zzzqzzzzzzz,zzzAzzzAzzzzzzzWzvz,_/zzzszv_9zzzsA`zzzazvz,_|zzzs_XzzzRzv_9zzzsz,A7zzzozAzz_!zzzzzzzzzzz_zzz'zzzUzzz_zzzqzzzzzzz0zzzAzzz_zzzzzzz~zvzGz~A_zzzW_f_Qzzzzzzzzzzz_zzz/zzzczzz_zzzqzzzzzzz&zzz)zzzAzzzzzzz*zvz,_czzzhz)_Qz,Aazzzj_[zzzszvzGz6A_zzzWz,AmzzzjzGz6A_zzzW_]_.zzzyz)_Xzzz)z7_Qz7_Qzzzzzzzzzzz_zzzvzzzXzzz_zzzqzzzzzzz=zzzhzzzAzzzzzzzPzvz,_czzzhz)_Qz,Aazzzj_[zzz-zvzGz6zGz.A_zzz{z,AmzzzjzGz6zGz.A_zzz{_mz+_]_izzzyz)_Xzzz)z7_Qz)_Qzzzzzzzzzzz_zzzvzzzkzzz_zzzqzzzzzzz`zzz_zzz_zzzzzzzhzv_9zzzT_:zzzzzzzzzzzAzzz8zzzCzzz_zzzqzzzzzzz?zzz)zzzAzzzzzzz+zvz7z1z)_+A_zzz{_gzzzzzzzzzzzAzzz8zzz;zzz_zzzqzzzzzzzozzzFzzz7zzzzzzzlzv_9zzzBz1zszv_9zzzTzv_9zzzZAAzzzm_gzzzzzzzzzzzmzzz8zzzIzzz_zzzqzzzzzz_ zzz)zzzRzzzzzz_Hz+z#zFzZz)A~zYz~z7zYz+z?z+z?z~_MzzzMzo_ezzz+z?z+zP_3z#zmzZz)z?z+zDzYz5zEzmz+_]_[zzz*z?z5_JzzzRzEzmz?z5_-z#zm_Xzzzjz?z5_KzzzOzEzmz?z5zA_Lz)_h_oz#zmzEzFzEzm_(z#zF_*z+z__XAUAAzGzqzYz~zEzFz#z+z?z~z)_+z4_+zYzaz?za_JzzzmzEz+z?za_oz#z+zsz?z~_hz)_+z4_+zYz5z?z5_JzzzmzEz+z?z5_-z#z+zEz+_gzzzzzzzzzzzmzzzxzzzQzzz_zzzqzzzzzzz*zzzFzzz_zzzzzzz4z7z=z7_ezzz5Azzzz2z)_+_ezzz5Ahzzzdz9_ezzz5A_zzz}z{AAzzz-_!zzzzzzzzzzzmzzz8zzzgzzz_zzzqzzzzzzznzzzhzzzFzzzzzzz]z{z1_MzzzYz{z1z{_+zh_`_hzYzh_*z)z_z?z)_ezzzt_MzzzZz{z?zhzxzfz?z)AAzzz-z?zhz)_hz1zxzfz?z)AAzzz-z{z?zhz1zxzfAAzzz3_!zzzzzzzzzzzmzzz8zzzrzzz_zzzqzzzzzz_/zzz)zzz~zzzzzz_[z{zYzhz1z)_hzYzFz{zYzyz?zyzs_'zzzBzZz)z?zyzfz?zyAzzzzRAzzzzwze_*zyz__XAUAVz?zhz1_'zzzNz?zFzs_'zzzCzZz)z?zhAzzzzRAmzzzOzZz)z?zFAzzzzRAmzzzOA7zzz(zAzz_[zzz?zZz)z?zh_*zhz_AzzzzRAmzzzOz$zy_XzzzlzZz)z?zF_*zFz_AzzzzRAmzzzOz$zyzfz{zZzyAzzzzwze_*zzz__XAU_Qz?zhz1_'zzzZzfz{zZz)z?zhAzzzzRAzzzzwze_*zzz__*zhz__XAUAS_!zzzzzzzzzzzz",
							"z_A7)hFy~+ma5RO2G`lj0?EBTZ{1s4oWd}-t3w(L8*v,xfDPHS@Vu&Y#6]$= <[.qKiJbn>M'U/cXkC;IQgrN:!ep9|"
						);
					}
			}

			ByteDataBuffer classBuffer = ByteDataBuffer.fromBytes(classBytes, false);
			ClasspathResource classpathResource = ClasspathResource.createSynthetic("MethodParameterListChanging." + className);
			AtomicReference<String> progressMessage = new AtomicReference<>();
			EventRegistry eventRegistry = new EventRegistry();
			PrintWriter logWriter = new PrintWriter(new StringWriter());
			PrintWriter warningLogWriter = new PrintWriter(new StringWriter());
			ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes = new ThreeLevelMultiMap<>();
			ClassNode classNode = new ClassNode(classBuffer, classpathResource, progressMessage, eventRegistry, logWriter, warningLogWriter, unknownAttributes, 5);
			obfuscationContext.registerClassNode(classNode, decrypterRoleIndex);
			classNode.renameClassInternal(className, GlobalUtils.newHashMap(13));
			this.remapDecrypterReferences(classNode, obfuscationContext);
			classNode.setMajorVersion(majorVersion);
			classNode.setMinorVersion(minorVersion);
			return classNode;
		} catch (IOException ioException) {
			throw new ProcessingException("Method Parameter List Changing (A) : " + ioException.getMessage(), ioException);
		}
	}

	private InstructionBlock buildFallbackDecryptBlock(
		ClassNode fallbackClass,
		ClassNode decrypterInterface,
		ClassNode simpleLongClass,
		LocalVariableTable localVariables,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AtomicInt maxStack,
		AtomicInt maxLocals,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		maxStack.set(5);
		maxLocals.set(7);
		ArrayList<AbstractInsnNode> insnList = new ArrayList<>();
		LabelNode _Start = new LabelNode(true, 1);
		String decrypterInterfaceDescriptor = "L" + decrypterInterface.getInternalName() + ";";
		String decryptMethodDescriptor = "(J)" + decrypterInterfaceDescriptor;
		insnList.add(AbstractInsnNode.loadLong(1, localVariables, 5));
		MethodNode createDecrypterMethod = simpleLongClass.findMethodNode(new MethodSignature("c", decryptMethodDescriptor));
		MethodCallWriter createDecrypterRef = constantPoolManager.getOrCreateClassMethodRef(createDecrypterMethod, typeWriters);
		insnList.add(new WideConstantPoolInsnNode(INVOKESTATIC, createDecrypterRef));
		insnList.add(InsnNode.of(DUP));
		insnList.add(AbstractInsnNode.storeObject(3, localVariables, 5));
		insnList.add(AbstractInsnNode.loadLong(1, localVariables, 5));
		MethodNode decryptLongMethod = decrypterInterface.findMethodNode(new MethodSignature("a", "(J)J"));
		InterfaceMethodCallWriter decryptLongRef = constantPoolManager.getOrCreateInterfaceMethodRef(decryptLongMethod, typeWriters);
		insnList.add(new InvokeInterfaceNode(decryptLongRef));
		insnList.add(AbstractInsnNode.storeLong(4, localVariables, 5));
		insnList.add(AbstractInsnNode.loadObject(3, localVariables, 5));
		MethodNode getBitPermutationMethod = decrypterInterface.findMethodNode(new MethodSignature("b", "()[I"));
		InterfaceMethodCallWriter getBitPermutationRef = constantPoolManager.getOrCreateInterfaceMethodRef(getBitPermutationMethod, typeWriters);
		insnList.add(new InvokeInterfaceNode(getBitPermutationRef));
		insnList.add(AbstractInsnNode.storeObject(6, localVariables, 5));
		insnList.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		AbstractFieldNode bitPermutationField = fallbackClass.findField("g", "[I");
		FieldTypeWriter bitPermutationFieldRef = constantPoolManager.getOrCreateFieldRef(bitPermutationField, typeWriters);
		insnList.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		insnList.add(new WideConstantPoolInsnNode(GETFIELD, bitPermutationFieldRef));
		insnList.add(InsnNode.of(ICONST_0));
		insnList.add(AbstractInsnNode.loadObject(6, localVariables, 5));
		insnList.add(InsnNode.of(ICONST_0));
		insnList.add(AbstractInsnNode.newInt(64));
		MethodCallWriter arraycopyRef = constantPoolManager.getOrCreateMethodRef(
			"java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", typeWriters, workspace, classProvider
		);
		insnList.add(new WideConstantPoolInsnNode(INVOKESTATIC, arraycopyRef));
		insnList.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		AbstractFieldNode decrypterField = fallbackClass.findField("c", decrypterInterfaceDescriptor);
		FieldTypeWriter decrypterFieldRef = constantPoolManager.getOrCreateFieldRef(decrypterField, typeWriters);
		insnList.add(new WideConstantPoolInsnNode(GETFIELD, decrypterFieldRef));
		insnList.add(new JumpInsnNode(IFNULL, _Start));
		insnList.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		insnList.add(new WideConstantPoolInsnNode(GETFIELD, decrypterFieldRef));
		insnList.add(AbstractInsnNode.loadLong(1, localVariables, 5));
		insnList.add(new InvokeInterfaceNode(decryptLongRef));
		insnList.add(InsnNode.of(POP2));
		insnList.add(_Start);
		insnList.add(AbstractInsnNode.loadLong(4, localVariables, 5));
		return new InstructionBlock(insnList, -1, 1, 2);
	}

	private void emitEncryptedString(
		String stringValue, int caseIndex, LabelNode caseLabel, LabelNode decoderLabel, List<AbstractInsnNode> instructions, char[] xorKey, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters
	) {
		instructions.add(AbstractInsnNode.newInt(caseIndex));
		String encryptedString = this.xorString(stringValue, xorKey);
		StringNode stringNode = constantPoolManager.createStringNode(encryptedString, typeWriters, false);
		instructions.add(new WideConstantPoolInsnNode(LDC_W, stringNode));
		instructions.add(new GotoNode(decoderLabel));
		instructions.add(caseLabel);
	}

	private void buildFallbackMethods(
		ClassNode fallbackClass,
		ClassNode decrypterInterface,
		ClassNode simpleLongClass,
		List<ChangedMethod> changedMethods,
		List<Object> saltValueList,
		List<Object> tableVariantList,
		List<Pair> saltLinkPairs,
		List<Pair> saltValuePairs,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		ArrayList<TypeWriter> typeWriters = new ArrayList<>();
		MethodNode staticInitializer = fallbackClass.getOrCreateStaticInitializer(typeWriters, workspace, "Method Parameter List Changing", 5);
		boolean java5OrNewer = fallbackClass.isJava5OrNewer();
		ConstantPoolManager constantPoolManager = fallbackClass.getConstantPoolManager();
		AtomicReference<TryCatchBlockInfo[]> exceptionHandlers = new AtomicReference<>();
		AtomicReference<LongNode> longConstantRef = new AtomicReference<>();
		List<ExtraCallEntry> decrypterTableEntries = this.buildDecrypterTableEntries(saltValueList, fallbackClass, decrypterInterface, simpleLongClass, constantPoolManager, typeWriters, longConstantRef, workspace, classProvider, java5OrNewer);
		List<MethodNode> tableInitMethods = this.createExtraCallMethods(decrypterTableEntries, fallbackClass, constantPoolManager, typeWriters, workspace);
		AtomicInt ctorAdditionalLocals = new AtomicInt();
		AtomicInt ctorRequiredStack = new AtomicInt();
		MethodNode defaultConstructor = fallbackClass.findMethodNode(MethodSignature.DEFAULT_CONSTRUCTOR);
		LocalVariableTable ctorVariableTable = defaultConstructor.getLocalVariableTable();
		ArrayList<AbstractInsnNode> ctorInstructions = new ArrayList<>();
		this.buildFallbackCtorBlock(fallbackClass, decrypterInterface, simpleLongClass, ctorVariableTable, tableInitMethods, ctorInstructions, constantPoolManager, typeWriters, ctorAdditionalLocals, ctorRequiredStack, exceptionHandlers, workspace, classProvider, java5OrNewer);
		AtomicInt clinitAdditionalLocals = new AtomicInt();
		AtomicInt clinitRequiredStack = new AtomicInt();
		ArrayList<AbstractInsnNode> clinitInstructions = new ArrayList<>();
		this.emitSingletonInit(fallbackClass, clinitInstructions, constantPoolManager, typeWriters, clinitAdditionalLocals, clinitRequiredStack, exceptionHandlers, workspace, classProvider, java5OrNewer);
		AtomicInt decryptBlockMaxLocals = new AtomicInt();
		AtomicInt decryptBlockMaxStack = new AtomicInt();
		MethodNode decryptMethod = fallbackClass.findMethodNode(new MethodSignature("a", "(J)J"));
		LocalVariableTable decryptVariableTable = decryptMethod.getLocalVariableTable();
		InstructionBlock decryptBlock = this.buildFallbackDecryptBlock(fallbackClass, decrypterInterface, simpleLongClass, decryptVariableTable, constantPoolManager, typeWriters, decryptBlockMaxStack, decryptBlockMaxLocals, workspace, classProvider);
		AtomicInt permutationBlockMaxLocals = new AtomicInt();
		AtomicInt permutationBlockMaxStack = new AtomicInt();
		MethodNode permutationMethod = fallbackClass.findMethodNode(new MethodSignature("b", "()[I"));
		LocalVariableTable permutationVariableTable = permutationMethod.getLocalVariableTable();
		InstructionBlock permutationBlock = this.buildBitPermutationLoadBlock(fallbackClass, simpleLongClass, permutationVariableTable, constantPoolManager, typeWriters, permutationBlockMaxLocals, permutationBlockMaxStack);
		AtomicInt saltPermutationMaxLocals = new AtomicInt();
		AtomicInt saltPermutationMaxStack = new AtomicInt();
		MethodNode saltPermutationMethod = fallbackClass.findMethodNode(new MethodSignature("d", "()V"));
		List<ExtraCallEntry> saltPermutationEntries = this.buildSaltPermutationEntries(tableVariantList, fallbackClass, decrypterInterface, simpleLongClass, constantPoolManager, typeWriters, saltPermutationMaxLocals, saltPermutationMaxStack, longConstantRef, workspace, classProvider);
		List<MethodNode> saltPermutationInitMethods = this.createMaskVariantMethods(saltPermutationEntries, fallbackClass, constantPoolManager, typeWriters, saltPermutationMaxStack.get(), saltPermutationMaxLocals.get(), workspace);
		LocalVariableTable saltPermutationVariableTable = saltPermutationMethod.getLocalVariableTable();
		ArrayList<AbstractInsnNode> saltPermutationInstructions = new ArrayList<>();
		this.appendDecrypterInitCalls(fallbackClass, decrypterInterface, simpleLongClass, saltPermutationVariableTable, saltPermutationInitMethods, saltPermutationInstructions, constantPoolManager, typeWriters, workspace, classProvider);
		AtomicInt saltPairMaxLocals = new AtomicInt();
		AtomicInt saltPairMaxStack = new AtomicInt();
		MethodNode saltPairMethod = fallbackClass.findMethodNode(new MethodSignature("c", "()V"));
		List<ExtraCallEntry> saltPairEntries = this.buildSaltPairEntries(saltLinkPairs, saltValuePairs, fallbackClass, decrypterInterface, simpleLongClass, constantPoolManager, typeWriters, longConstantRef, saltPairMaxLocals, saltPairMaxStack);
		List<MethodNode> saltPairInitMethods = this.createSizedExtraCallMethods(saltPairEntries, fallbackClass, constantPoolManager, typeWriters, saltPairMaxStack.get(), saltPairMaxLocals.get(), workspace);
		LocalVariableTable saltPairVariableTable = saltPairMethod.getLocalVariableTable();
		ArrayList<AbstractInsnNode> saltPairInstructions = new ArrayList<>();
		this.appendGeneratedMethodCalls(fallbackClass, decrypterInterface, simpleLongClass, saltPairVariableTable, saltPairInitMethods, saltPairInstructions, constantPoolManager, typeWriters, workspace, classProvider);
		fallbackClass.appendEntriesToConstantPool(typeWriters);
		defaultConstructor.insertInstructionsAtOffset(ctorInstructions, ctorAdditionalLocals.get(), ctorRequiredStack.get(), 2, "Method Parameter List Changing", new ArrayList<>());
		if (!exceptionHandlers.isNull()) {
			defaultConstructor.setExceptionEntries(exceptionHandlers.get());
		}

		staticInitializer.insertInstructionsAtOffset(clinitInstructions, clinitAdditionalLocals.get(), clinitRequiredStack.get(), 0, "Method Parameter List Changing", new ArrayList<>());
		if (!exceptionHandlers.isNull()) {
			staticInitializer.setExceptionEntries(exceptionHandlers.get());
		}

		decryptMethod.applyInstructionBlock(decryptBlock, decryptBlockMaxStack.get(), decryptBlockMaxLocals.get(), "Method Parameter List Changing");
		permutationMethod.applyInstructionBlock(permutationBlock, permutationBlockMaxStack.get(), permutationBlockMaxLocals.get(), "Method Parameter List Changing");
		saltPermutationMethod.insertInstructionsAtOffset(saltPermutationInstructions, 1, 1, 0, "Method Parameter List Changing", new ArrayList<>());
		saltPairMethod.insertInstructionsAtOffset(saltPairInstructions, 1, 1, 0, "Method Parameter List Changing", new ArrayList<>());
	}

	public boolean hasMpcClassTriplets() {
		return this.classToMpcClassTripletMap != null && !this.classToMpcClassTripletMap.isEmpty();
	}

	public ScriptContext getScriptContext() {
		return this.scriptContext;
	}

	private void buildDecrypterClasses(
		Set<Triplet> decrypterClassTriplets,
		List<ChangedMethod> changedMethods,
		int indexBitOffset,
		int maxSortDepth,
		int tableBatchSize,
		List<Object> decrypterConstants,
		List<Object> saltValueList,
		AtomicReference<int[]> bitPermutationRef,
		List<Object> tableVariantList,
		List<Pair> saltLinkPairs,
		List<Pair> saltValuePairs,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		Inheritance inheritance
	) {
		for (Triplet decrypterClassTriplet : decrypterClassTriplets) {
			ClassNode decrypterInterface = (ClassNode)decrypterClassTriplet.getByIndex(0);
			ClassNode fallbackClass = (ClassNode)decrypterClassTriplet.getByIndex(1);
			ClassNode simpleLongClass = (ClassNode)decrypterClassTriplet.getByIndex(2);
			this.buildSimpleLongMethods(simpleLongClass, fallbackClass, indexBitOffset, maxSortDepth, tableBatchSize, changedMethods, decrypterConstants, bitPermutationRef, workspace, classProvider, inheritance);
			this.buildFallbackMethods(fallbackClass, decrypterInterface, simpleLongClass, changedMethods, saltValueList, tableVariantList, saltLinkPairs, saltValuePairs, workspace, classProvider);
		}
	}

	private List<MethodNode> createStaticDecryptMethods(
		List<ExceptionCallEntry> callEntries, ClassNode simpleLongClass, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, Inheritance inheritance, AbstractWorkspace workspace
	) {
		ArrayList<MethodNode> methods = new ArrayList<>(callEntries.size());
		int methodIndex = 0;

		for (int i = 0; i < callEntries.size(); i++) {
			ExceptionCallEntry callEntry = callEntries.get(i);
			MethodNode method = simpleLongClass.createStaticMethod(
				"a" + methodIndex++,
				"()V",
				callEntry.getInstructions(),
				4,
				callEntry.getMaxLocals(),
				1,
				callEntry.getLocalVariableTable(),
				callEntry.getTryCatchBlocks(),
				"MethodParameterListChanging",
				typeWriters,
				null,
				workspace,
				5
			);
			methods.add(method);
		}

		return methods;
	}

	private String xorString(String value, char[] key) {
		char[] chars = value.toCharArray();

		for (int i = 0; i < chars.length; i++) {
			chars[i] ^= key[i];
		}

		return new String(chars);
	}

	private List<ExtraCallEntry> buildDecrypterTableEntries(
		List<Object> saltValueList,
		ClassNode fallbackClass,
		ClassNode decrypterInterface,
		ClassNode simpleLongClass,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AtomicReference<LongNode> longConstantRef,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean java5OrNewer
	) {
		ArrayList<ExtraCallEntry> callEntries = new ArrayList<>();
		ExtraCallEntry callEntry = new ExtraCallEntry(31000, "()V", false);
		ArrayList<AbstractInsnNode> instructions = callEntry.getInstructions();
		callEntries.add(callEntry);
		String hashTableInternalName = getHashTableInternalName(java5OrNewer);
		String hashTableDescriptor = getHashTableDescriptor(java5OrNewer);
		FieldNode hashTableField = (FieldNode)fallbackClass.findField("b", hashTableDescriptor);
		FieldTypeWriter hashTableFieldRef = constantPoolManager.getOrCreateFieldRef(hashTableField, typeWriters);
		String decryptMethodDescriptor = "(J)L" + decrypterInterface.getInternalName() + ';';
		MethodNode decryptMethod = simpleLongClass.findMethodNode(new MethodSignature("c", decryptMethodDescriptor));
		MethodCallWriter decryptMethodRef = constantPoolManager.getOrCreateClassMethodRef(decryptMethod, typeWriters);
		MethodCallWriter putMethodRef = constantPoolManager.getOrCreateMethodRef(hashTableInternalName, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", typeWriters, workspace, classProvider);
		int bytecodeSize = 0;
		bytecodeSize++;
		instructions.add(AbstractInsnNode.loadObject(0, callEntry.getLocalVariableTable(), 5));
		WideConstantPoolInsnNode wideInsnNode = new WideConstantPoolInsnNode(GETFIELD, hashTableFieldRef);
		bytecodeSize += wideInsnNode.getBytecodeSize();
		instructions.add(wideInsnNode);
		int saltValueCount = saltValueList.size();

		for (int saltValueIndex = 0; saltValueIndex < saltValueCount; saltValueIndex++) {
			Object saltValue = saltValueList.get(saltValueIndex++);
			bytecodeSize++;
			instructions.add(InsnNode.of(DUP));
			if (typeWriters.size() > 32000) {
				int saltValueEncryptedSize = AbstractInsnNode.encryptLongConstant((Long)saltValue, instructions, constantPoolManager, typeWriters, longConstantRef);
				bytecodeSize += saltValueEncryptedSize;
			} else {
				AbstractInsnNode saltValueInstruction = AbstractInsnNode.ldcLong((Long)saltValue, constantPoolManager, typeWriters);
				bytecodeSize += saltValueInstruction.getBytecodeSize();
				instructions.add(saltValueInstruction);
			}

			wideInsnNode = new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethodRef);
			bytecodeSize += wideInsnNode.getBytecodeSize();
			instructions.add(wideInsnNode);
			Object pairedSaltValue = saltValueList.get(saltValueIndex);
			if (pairedSaltValue instanceof Long) {
				if (typeWriters.size() > 32000) {
					int pairedSaltValueEncryptedSize = AbstractInsnNode.encryptLongConstant((Long)pairedSaltValue, instructions, constantPoolManager, typeWriters, longConstantRef);
					bytecodeSize += pairedSaltValueEncryptedSize;
				} else {
					AbstractInsnNode pairedSaltValueInstruction = AbstractInsnNode.ldcLong((Long)pairedSaltValue, constantPoolManager, typeWriters);
					bytecodeSize += pairedSaltValueInstruction.getBytecodeSize();
					instructions.add(pairedSaltValueInstruction);
				}

				wideInsnNode = new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethodRef);
				bytecodeSize += wideInsnNode.getBytecodeSize();
				instructions.add(wideInsnNode);
			} else {
				bytecodeSize++;
				instructions.add(AbstractInsnNode.loadObject(0, callEntry.getLocalVariableTable(), 5));
			}

			wideInsnNode = new WideConstantPoolInsnNode(INVOKEVIRTUAL, putMethodRef);
			bytecodeSize += wideInsnNode.getBytecodeSize();
			instructions.add(wideInsnNode);
			bytecodeSize++;
			instructions.add(InsnNode.of(POP));
			if (bytecodeSize > 65035 && saltValueIndex < saltValueCount - 1) {
				instructions.add(InsnNode.of(RETURN));
				callEntry = new ExtraCallEntry(instructions.size() + 100, "()V", false);
				instructions = callEntry.getInstructions();
				callEntries.add(callEntry);
				instructions.add(AbstractInsnNode.loadObject(0, callEntry.getLocalVariableTable(), 5));
				instructions.add(new WideConstantPoolInsnNode(GETFIELD, hashTableFieldRef));
				bytecodeSize = 4;
			}
		}

		instructions.add(InsnNode.of(RETURN));
		instructions.trimToSize();
		return callEntries;
	}

	private void resolveMethodTriplet(Triplet decrypterClasses) {
		ClassNode simpleLongClass = (ClassNode)decrypterClasses.getByIndex(2);
		MethodSignature lockObjectSignature = new MethodSignature("g", "()Ljava/lang/Object;");
		MethodNode lockObjectMethod = simpleLongClass.findMethodNode(lockObjectSignature);
		ClassNode decrypterInterface = (ClassNode)decrypterClasses.getByIndex(0);
		String createDecrypterMethodDescriptor = "(JJLjava/lang/Object;)" + decrypterInterface.getDescriptor();
		MethodSignature createDecrypterSignature = new MethodSignature("a", createDecrypterMethodDescriptor);
		MethodNode createDecrypterMethod = simpleLongClass.findMethodNode(createDecrypterSignature);
		MethodSignature decryptLongSignature = new MethodSignature("a", "(J)J");
		MethodNode decryptLongMethod = decrypterInterface.findMethodNode(decryptLongSignature);
		this.resolvedMethodsByTriplet.put(decrypterClasses, new Triplet(lockObjectMethod, createDecrypterMethod, decryptLongMethod));
	}

	private List<MethodNode> createMaskVariantMethods(
		List<ExtraCallEntry> callEntries, ClassNode fallbackClass, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, int maxStack, int maxLocals, AbstractWorkspace workspace
	) {
		ArrayList<MethodNode> maskVariantMethods = new ArrayList<>(callEntries.size());
		int methodIndex = 0;

		for (ExtraCallEntry callEntry : callEntries) {
			MethodNode maskVariantMethod = fallbackClass.createMethodWithCode(
				"c" + methodIndex++,
				"()V",
				callEntry.getInstructions(),
				maxStack,
				maxLocals,
				1,
				false,
				callEntry.getLocalVariableTable(),
				new TryCatchBlockInfo[0],
				"MethodParameterListChanging",
				typeWriters,
				null,
				workspace,
				5
			);
			maskVariantMethods.add(maskVariantMethod);
		}

		return maskVariantMethods;
	}

	private void propagateChangedMethods(ClassHierarchyNode hierarchyNode, Map<ClassNode, ChangedMethod> changedMethodByClass) {
		ClassNode classNode = (ClassNode)hierarchyNode.getClassNode();
		ChangedMethod changedMethod = changedMethodByClass.get(classNode);
		if (!hierarchyNode.isInterface()) {
			List<ClassHierarchyNode> subclassNodes = hierarchyNode.getDirectSubclassesList();
			if (subclassNodes != null) {
				if (changedMethod != null) {
					Set<ClassNode> referencedSubclasses = this.staticBlockAnalyzer.getReferencedSubclasses(changedMethod.getMethodNode());

					for (ClassHierarchyNode subclassNode : subclassNodes) {
						ClassNode subclassClassNode = (ClassNode)subclassNode.getClassNode();
						if (!referencedSubclasses.contains(subclassClassNode)) {
							AbstractClassNode subclassAbstractNode = subclassNode.getClassNode();
							ChangedMethod subclassChangedMethod = changedMethodByClass.get(subclassAbstractNode);
							if (subclassChangedMethod != null) {
								changedMethod.addDependentMethod(subclassChangedMethod);
							}
						}
					}
				}

				for (ClassHierarchyNode descendantNode : subclassNodes) {
					this.propagateChangedMethods(descendantNode, changedMethodByClass);
				}
			}
		}
	}

	private void buildFallbackCtorBlock(
		ClassNode fallbackClass,
		ClassNode decrypterInterface,
		ClassNode simpleLongClass,
		LocalVariableTable localVariables,
		List<MethodNode> tableInitMethods,
		ArrayList<AbstractInsnNode> instructions,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AtomicInt additionalLocals,
		AtomicInt requiredStack,
		AtomicReference<TryCatchBlockInfo[]> exceptionHandlers,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean java5OrNewer
	) {
		exceptionHandlers.setNull();
		additionalLocals.set(1);
		requiredStack.set(4);
		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		String hashTableInternalName = getHashTableInternalName(java5OrNewer);
		String hashTableDescriptor = getHashTableDescriptor(java5OrNewer);
		ClassInfoWriter hashTableClassInfo = constantPoolManager.getOrCreateClassInfo(hashTableInternalName, typeWriters);
		instructions.add(new NewNode(hashTableClassInfo));
		instructions.add(InsnNode.of(DUP));
		MethodCallWriter hashTableCtorRef = constantPoolManager.getOrCreateMethodRef(hashTableInternalName, "<init>", "()V", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, hashTableCtorRef));
		FieldNode hashTableField = (FieldNode)fallbackClass.findField("b", hashTableDescriptor);
		FieldTypeWriter hashTableFieldRef = constantPoolManager.getOrCreateFieldRef(hashTableField, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(PUTFIELD, hashTableFieldRef));

		for (MethodNode tableInitMethod : tableInitMethods) {
			MethodCallWriter tableInitMethodRef = constantPoolManager.getOrCreateClassMethodRef(tableInitMethod, typeWriters);
			instructions.add(AbstractInsnNode.loadObject(0, localVariables, 5));
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, tableInitMethodRef));
		}

		FieldNode bitPermutationField = (FieldNode)fallbackClass.findField("g", "[I");
		FieldTypeWriter bitPermutationFieldRef = constantPoolManager.getOrCreateFieldRef(bitPermutationField, typeWriters);
		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		instructions.add(AbstractInsnNode.newInt(64));
		instructions.add(new NewArrayNode(10));
		instructions.add(new WideConstantPoolInsnNode(PUTFIELD, bitPermutationFieldRef));
		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		instructions.add(new WideConstantPoolInsnNode(GETFIELD, bitPermutationFieldRef));
		int[] bitPermutation = createBitPermutation(random);

		for (int i = 0; i < 64; i++) {
			if (i < 63) {
				instructions.add(InsnNode.of(DUP));
			}

			instructions.add(AbstractInsnNode.newInt(i));
			instructions.add(AbstractInsnNode.newInt(bitPermutation[i]));
			instructions.add(InsnNode.of(IASTORE));
		}

		String extraCallInitEntryMethodDescriptor = "(L" + fallbackClass.getInternalName() + ";)V";
		MethodNode extraCallInitEntryMethod = simpleLongClass.findMethodNode(new MethodSignature("a", extraCallInitEntryMethodDescriptor));
		MethodCallWriter extraCallInitEntryMethodRef = constantPoolManager.getOrCreateClassMethodRef(extraCallInitEntryMethod, typeWriters);
		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, extraCallInitEntryMethodRef));
	}

	private void buildSimpleLongMethods(
		ClassNode simpleLongClass,
		ClassNode fallbackClass,
		int indexBitOffset,
		int maxSortDepth,
		int tableBatchSize,
		List<ChangedMethod> changedMethods,
		List<Object> decrypterConstants,
		AtomicReference<int[]> bitPermutationRef,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		Inheritance inheritance
	) {
		ArrayList<TypeWriter> typeWriters = new ArrayList<>();
		MethodNode staticInitializer = simpleLongClass.getOrCreateStaticInitializer(typeWriters, workspace, "Method Parameter List Changing", 5);
		boolean java5OrNewer = simpleLongClass.isJava5OrNewer();
		ConstantPoolManager constantPoolManager = simpleLongClass.getConstantPoolManager();
		List<ExceptionCallEntry> simpleLongInitEntries = this.buildSimpleLongInitEntries(decrypterConstants, simpleLongClass, constantPoolManager, typeWriters, workspace, classProvider, java5OrNewer);
		List<MethodNode> staticDecryptMethods = this.createStaticDecryptMethods(simpleLongInitEntries, simpleLongClass, constantPoolManager, typeWriters, inheritance, workspace);
		LocalVariableTable clinitVariableTable = staticInitializer.getLocalVariableTable();
		ArrayList<AbstractInsnNode> clinitInstructions = new ArrayList<>();
		AtomicInt clinitAdditionalLocals = new AtomicInt();
		AtomicInt clinitRequiredStack = new AtomicInt();
		AtomicReference<TryCatchBlockInfo[]> exceptionHandlers = new AtomicReference<>();
		this.buildDecrypterClinit(simpleLongClass, clinitVariableTable, staticDecryptMethods, indexBitOffset, maxSortDepth, tableBatchSize, clinitInstructions, constantPoolManager, typeWriters, clinitAdditionalLocals, clinitRequiredStack, exceptionHandlers, workspace, classProvider, java5OrNewer);
		String extraCallInitEntryMethodDescriptor = "(L" + fallbackClass.getInternalName() + ";)V";
		MethodNode extraCallInitEntryMethod = simpleLongClass.findMethodNode(new MethodSignature("a", extraCallInitEntryMethodDescriptor));
		ArrayList<AbstractInsnNode> extraCallInitInstructions = new ArrayList<>();
		AtomicInt extraCallInitAdditionalLocals = new AtomicInt();
		AtomicInt extraCallInitRequiredStack = new AtomicInt();
		LocalVariableTable extraCallInitVariableTable = extraCallInitEntryMethod.getLocalVariableTable();
		this.emitExtraCallInitCode(simpleLongClass, fallbackClass, extraCallInitVariableTable, extraCallInitInstructions, constantPoolManager, typeWriters, extraCallInitAdditionalLocals, extraCallInitRequiredStack, workspace, classProvider, java5OrNewer);
		String initBlockMethodDescriptor = "(L" + fallbackClass.getInternalName() + ";)V";
		MethodNode initBlockMethod = simpleLongClass.findMethodNode(new MethodSignature("b", initBlockMethodDescriptor));
		LocalVariableTable initBlockVariableTable = initBlockMethod.getLocalVariableTable();
		ArrayList<AbstractInsnNode> initBlockInstructions = new ArrayList<>();
		AtomicInt initBlockAdditionalLocals = new AtomicInt();
		AtomicInt initBlockRequiredStack = new AtomicInt();
		this.buildSimpleLongInitBlock(simpleLongClass, fallbackClass, initBlockVariableTable, initBlockInstructions, bitPermutationRef, constantPoolManager, typeWriters, extraCallInitAdditionalLocals, extraCallInitRequiredStack, workspace, classProvider, java5OrNewer);
		simpleLongClass.appendEntriesToConstantPool(typeWriters);
		staticInitializer.insertInstructionsAtOffset(clinitInstructions, clinitAdditionalLocals.get(), clinitRequiredStack.get(), 0, "Method Parameter List Changing", new ArrayList<>());
		if (!exceptionHandlers.isNull()) {
			staticInitializer.setExceptionEntries(exceptionHandlers.get());
		}

		extraCallInitEntryMethod.insertInstructionsAtOffset(extraCallInitInstructions, extraCallInitAdditionalLocals.get(), extraCallInitRequiredStack.get(), 0, "Method Parameter List Changing", new ArrayList<>());
		initBlockMethod.insertInstructionsAtOffset(initBlockInstructions, initBlockAdditionalLocals.get(), initBlockRequiredStack.get(), 0, "Method Parameter List Changing", new ArrayList<>());
	}

	public boolean shouldEncryptStringsInMethod(MethodNode method) {
		if (method == null) {
			return false;
		} else {
			ClassNode methodOwner = method.getMethodOwner();
			return (methodOwner.isJava8OrNewer() || methodOwner.isJava1_4OrNewer() && !methodOwner.isInterface())
				&& this.isChangedMethod(method)
				&& this.stringEncryptionClasses.contains(methodOwner)
				&& Options.ZKM_STRING_ENCRYPT_DES
				&& GlobalUtils.isClassPresent("javax.crypto.Cipher");
		}
	}

	private void buildSimpleLongInitBlock(
		ClassNode simpleLongClass,
		ClassNode fallbackClass,
		LocalVariableTable localVariables,
		ArrayList<AbstractInsnNode> instructions,
		AtomicReference<int[]> bitPermutationRef,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AtomicInt additionalLocals,
		AtomicInt requiredStack,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean java5OrNewer
	) {
		additionalLocals.set(1);
		requiredStack.set(3);
		MethodNode extraCallInitMethod = simpleLongClass.findMethodNode(new MethodSignature("c", "()V"));
		MethodCallWriter extraCallInitMethodRef = constantPoolManager.getOrCreateClassMethodRef(extraCallInitMethod, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, extraCallInitMethodRef));
		FieldNode bitPermutationField = (FieldNode)simpleLongClass.findField("e", "[I");
		FieldTypeWriter bitPermutationFieldRef = constantPoolManager.getOrCreateFieldRef(bitPermutationField, typeWriters);
		instructions.add(AbstractInsnNode.newInt(64));
		instructions.add(new NewArrayNode(10));
		instructions.add(InsnNode.of(DUP));
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, bitPermutationFieldRef));
		int[] bitPermutation = bitPermutationRef.get();

		for (int i = 0; i < 64; i++) {
			if (i < 63) {
				instructions.add(InsnNode.of(DUP));
			}

			instructions.add(AbstractInsnNode.newInt(i));
			instructions.add(AbstractInsnNode.newInt(bitPermutation[i]));
			instructions.add(InsnNode.of(IASTORE));
		}

		instructions.add(AbstractInsnNode.loadObject(0, localVariables, 5));
		MethodNode saltPairMethod = fallbackClass.findMethodNode(new MethodSignature("c", "()V"));
		MethodCallWriter saltPairMethodRef = constantPoolManager.getOrCreateClassMethodRef(saltPairMethod, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, saltPairMethodRef));
	}

	private int buildStringDecoderCode(
		long[] charKeys,
		String ownerClassName,
		ClassNode simpleLongClass,
		FieldTypeWriter listField,
		ExceptionCallEntry callEntry,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		char[] xorKey = new char[charKeys.length];

		for (int i = 0; i < charKeys.length; i++) {
			xorKey[i] = (char)(charKeys[i] & 127L);
		}

		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		ArrayList<TryCatchBlockInfo> tryCatchBlocks = new ArrayList<>();
		this.xorString("java.lang.management.RuntimeMXBean", xorKey);
		LabelNode runtimeMxBeanTryStartLabel = new LabelNode(true, 384);
		LabelNode runtimeMxBeanTryEndLabel = new LabelNode(true, 640);
		LabelNode runtimeMxBeanCatchHandlerLabel = new LabelNode(true, 1152);
		LabelNode afterRuntimeMxBeanTryLabel = new LabelNode(true, 1);
		ClassInfoWriter exceptionClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Exception", typeWriters);
		tryCatchBlocks.add(new TryCatchBlockInfo(exceptionClassInfo, runtimeMxBeanTryStartLabel, runtimeMxBeanTryEndLabel, runtimeMxBeanCatchHandlerLabel));
		LabelNode platformMxBeansTryStartLabel = new LabelNode(true, 384);
		LabelNode platformMxBeansTryEndLabel = new LabelNode(true, 640);
		LabelNode platformMxBeansCatchHandlerLabel = new LabelNode(true, 1152);
		LabelNode afterPlatformMxBeansTryLabel = new LabelNode(true, 1);
		tryCatchBlocks.add(new TryCatchBlockInfo(exceptionClassInfo, platformMxBeansTryStartLabel, platformMxBeansTryEndLabel, platformMxBeansCatchHandlerLabel));
		LabelNode inputArgumentsTryStartLabel = new LabelNode(true, 384);
		LabelNode inputArgumentsTryEndLabel = new LabelNode(true, 640);
		LabelNode inputArgumentsCatchHandlerLabel = new LabelNode(true, 1152);
		tryCatchBlocks.add(new TryCatchBlockInfo(exceptionClassInfo, inputArgumentsTryStartLabel, inputArgumentsTryEndLabel, inputArgumentsCatchHandlerLabel));
		LabelNode decoderLabel = new LabelNode(true, 1);
		LabelNode exitLabel = new LabelNode(true, 1);
		LabelNode caseRuntimeMxBeanLabel = new LabelNode(true, 1);
		LabelNode caseGetPlatformMxBeansLabel = new LabelNode(true, 1);
		LabelNode caseManagementFactoryLabel = new LabelNode(true, 1);
		LabelNode caseGetInputArgumentsLabel = new LabelNode(true, 1);
		LabelNode caseXdebugLabel = new LabelNode(true, 1);
		LabelNode caseXrunjdwpLabel = new LabelNode(true, 1);
		LabelNode caseAgentlibJdwpLabel = new LabelNode(true, 1);
		LabelNode caseAgentpathLabel = new LabelNode(true, 1);
		LabelNode caseJavaagentLabel = new LabelNode(true, 1);
		LabelNode[] caseLabels = new LabelNode[]{caseRuntimeMxBeanLabel, caseGetPlatformMxBeansLabel, caseManagementFactoryLabel, caseGetInputArgumentsLabel, caseXdebugLabel, caseXrunjdwpLabel, caseAgentlibJdwpLabel, caseAgentpathLabel};
		LabelNode decodeLoopStartLabel = new LabelNode(true, 1);
		LabelNode decodeLoopEndLabel = new LabelNode(true, 1);
		LabelNode argIteratorLoopStartLabel = new LabelNode(true, 1);
		LabelNode nResetTo10Label = new LabelNode(true, 1);
		LabelNode nResetTo17Label = new LabelNode(true, 1);
		LabelNode noMatchingFlagLabel = new LabelNode(true, 1);
		LabelNode argCheckExitLabel = new LabelNode(true, 1);
		LabelNode flagMatchedLabel = new LabelNode(true, 1);
		int runtimeMxBeanClassLocalIndex = callEntry.allocateLocalIndex();
		int decodeCharsLocalIndex = callEntry.allocateLocalIndex();
		int decodeIndexLocalIndex = callEntry.allocateLocalIndex();
		int mxBeanInstanceLocalIndex = callEntry.allocateLocalIndex();
		int platformMxBeansMethodNameLocalIndex = callEntry.allocateLocalIndex();
		int inputArgumentsMethodNameLocalIndex = callEntry.allocateLocalIndex();
		int xdebugFlagLocalIndex = callEntry.allocateLocalIndex();
		int xrunjdwpFlagLocalIndex = callEntry.allocateLocalIndex();
		int agentlibJdwpFlagLocalIndex = callEntry.allocateLocalIndex();
		int agentpathFlagLocalIndex = callEntry.allocateLocalIndex();
		int javaagentFlagLocalIndex = callEntry.allocateLocalIndex();
		LocalVariableTable localVariableTable = callEntry.getLocalVariableTable();
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(runtimeMxBeanClassLocalIndex, localVariableTable, 5));
		instructions.add(runtimeMxBeanTryStartLabel);
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(platformMxBeansMethodNameLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(mxBeanInstanceLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(inputArgumentsMethodNameLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(xdebugFlagLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(xrunjdwpFlagLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(agentlibJdwpFlagLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(agentpathFlagLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(AbstractInsnNode.storeObject(javaagentFlagLocalIndex, localVariableTable, 5));
		this.emitEncryptedString("java.lang.management.RuntimeMXBean", 0, caseRuntimeMxBeanLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		MethodCallWriter forNameCallWriter = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, forNameCallWriter));
		instructions.add(AbstractInsnNode.storeObject(runtimeMxBeanClassLocalIndex, localVariableTable, 5));
		instructions.add(runtimeMxBeanTryEndLabel);
		instructions.add(new GotoNode(afterRuntimeMxBeanTryLabel));
		instructions.add(runtimeMxBeanCatchHandlerLabel);
		instructions.add(InsnNode.of(POP));
		instructions.add(new GotoNode(exitLabel));
		instructions.add(afterRuntimeMxBeanTryLabel);
		instructions.add(AbstractInsnNode.loadObject(runtimeMxBeanClassLocalIndex, localVariableTable, 5));
		instructions.add(new JumpInsnNode(IFNULL, exitLabel));
		instructions.add(platformMxBeansTryStartLabel);
		this.emitEncryptedString("getPlatformMXBeans", 1, caseGetPlatformMxBeansLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		instructions.add(AbstractInsnNode.storeObject(platformMxBeansMethodNameLocalIndex, localVariableTable, 5));
		this.emitEncryptedString("java.lang.management.ManagementFactory", 2, caseManagementFactoryLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		MethodCallWriter managementFactoryForNameCallWriter = constantPoolManager.getOrCreateMethodRef("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, managementFactoryForNameCallWriter));
		instructions.add(AbstractInsnNode.loadObject(platformMxBeansMethodNameLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ICONST_1));
		ClassInfoWriter classClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Class", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, classClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(new WideConstantPoolInsnNode(LDC_W, classClassInfo));
		instructions.add(InsnNode.of(AASTORE));
		MethodCallWriter getMethodCallWriter = constantPoolManager.getOrCreateMethodRef(
			"java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", typeWriters, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getMethodCallWriter));
		instructions.add(InsnNode.of(ACONST_NULL));
		instructions.add(InsnNode.of(ICONST_1));
		ClassInfoWriter objectClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Object", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.loadObject(runtimeMxBeanClassLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(AASTORE));
		MethodCallWriter invokeCallWriter = constantPoolManager.getOrCreateMethodRef(
			"java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", typeWriters, workspace, classProvider
		);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, invokeCallWriter));
		ClassInfoWriter listClassInfo = constantPoolManager.getOrCreateClassInfo("java/util/List", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, listClassInfo));
		instructions.add(InsnNode.of(ICONST_0));
		InterfaceMethodCallWriter listGetCallWriter = constantPoolManager.getOrCreateInterfaceMethod("java/util/List", "get", "(I)Ljava/lang/Object;", typeWriters, workspace, classProvider);
		instructions.add(new InvokeInterfaceNode(listGetCallWriter));
		instructions.add(AbstractInsnNode.storeObject(mxBeanInstanceLocalIndex, localVariableTable, 5));
		instructions.add(platformMxBeansTryEndLabel);
		instructions.add(new GotoNode(afterPlatformMxBeansTryLabel));
		instructions.add(platformMxBeansCatchHandlerLabel);
		instructions.add(InsnNode.of(POP));
		instructions.add(new GotoNode(exitLabel));
		instructions.add(afterPlatformMxBeansTryLabel);
		instructions.add(AbstractInsnNode.loadObject(mxBeanInstanceLocalIndex, localVariableTable, 5));
		instructions.add(new JumpInsnNode(IFNULL, exitLabel));
		instructions.add(inputArgumentsTryStartLabel);
		this.emitEncryptedString("getInputArguments", 3, caseGetInputArgumentsLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		instructions.add(AbstractInsnNode.storeObject(inputArgumentsMethodNameLocalIndex, localVariableTable, 5));
		this.emitEncryptedString("-Xdebug", 4, caseXdebugLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		instructions.add(AbstractInsnNode.storeObject(xdebugFlagLocalIndex, localVariableTable, 5));
		this.emitEncryptedString("-Xrunjdwp", 5, caseXrunjdwpLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		instructions.add(AbstractInsnNode.storeObject(xrunjdwpFlagLocalIndex, localVariableTable, 5));
		this.emitEncryptedString("-agentlib:jdwp", 6, caseAgentlibJdwpLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		instructions.add(AbstractInsnNode.storeObject(agentlibJdwpFlagLocalIndex, localVariableTable, 5));
		this.emitEncryptedString("-agentpath:", 7, caseAgentpathLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		instructions.add(AbstractInsnNode.storeObject(agentpathFlagLocalIndex, localVariableTable, 5));
		this.emitEncryptedString("-javaagent:", 8, caseJavaagentLabel, decoderLabel, instructions, xorKey, constantPoolManager, typeWriters);
		instructions.add(AbstractInsnNode.storeObject(javaagentFlagLocalIndex, localVariableTable, 5));
		instructions.add(AbstractInsnNode.loadObject(runtimeMxBeanClassLocalIndex, localVariableTable, 5));
		instructions.add(AbstractInsnNode.loadObject(inputArgumentsMethodNameLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, classClassInfo));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, getMethodCallWriter));
		instructions.add(AbstractInsnNode.loadObject(mxBeanInstanceLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, objectClassInfo));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, invokeCallWriter));
		ClassInfoWriter collectionClassInfo = constantPoolManager.getOrCreateClassInfo("java/util/Collection", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, collectionClassInfo));
		InterfaceMethodCallWriter collectionIteratorCallWriter = constantPoolManager.getOrCreateInterfaceMethod("java/util/Collection", "iterator", "()Ljava/util/Iterator;", typeWriters, workspace, classProvider);
		instructions.add(new InvokeInterfaceNode(collectionIteratorCallWriter));
		instructions.add(argIteratorLoopStartLabel);
		instructions.add(InsnNode.of(DUP));
		InterfaceMethodCallWriter hasNextCallWriter = constantPoolManager.getOrCreateInterfaceMethod("java/util/Iterator", "hasNext", "()Z", typeWriters, workspace, classProvider);
		instructions.add(new InvokeInterfaceNode(hasNextCallWriter));
		instructions.add(new JumpInsnNode(IFEQ, argCheckExitLabel));
		instructions.add(InsnNode.of(DUP));
		InterfaceMethodCallWriter nextCallWriter = constantPoolManager.getOrCreateInterfaceMethod("java/util/Iterator", "next", "()Ljava/lang/Object;", typeWriters, workspace, classProvider);
		instructions.add(new InvokeInterfaceNode(nextCallWriter));
		ClassInfoWriter stringClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/String", typeWriters);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, stringClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(xdebugFlagLocalIndex, localVariableTable, 5));
		MethodCallWriter stringEqualsCallWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringEqualsCallWriter));
		instructions.add(new JumpInsnNode(IFNE, flagMatchedLabel));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(xrunjdwpFlagLocalIndex, localVariableTable, 5));
		MethodCallWriter stringStartsWithCallWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "startsWith", "(Ljava/lang/String;)Z", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringStartsWithCallWriter));
		instructions.add(new JumpInsnNode(IFNE, flagMatchedLabel));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(agentlibJdwpFlagLocalIndex, localVariableTable, 5));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringStartsWithCallWriter));
		instructions.add(new JumpInsnNode(IFNE, flagMatchedLabel));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(agentpathFlagLocalIndex, localVariableTable, 5));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringStartsWithCallWriter));
		instructions.add(new JumpInsnNode(IFNE, flagMatchedLabel));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(javaagentFlagLocalIndex, localVariableTable, 5));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, stringStartsWithCallWriter));
		instructions.add(new JumpInsnNode(IFEQ, noMatchingFlagLabel));
		instructions.add(flagMatchedLabel);
		instructions.add(InsnNode.of(POP));
		FieldNode stateFieldNode = (FieldNode)simpleLongClass.findField("n", "I");
		FieldTypeWriter stateFieldRef = constantPoolManager.getOrCreateFieldRef(stateFieldNode, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, stateFieldRef));
		instructions.add(AbstractInsnNode.newInt(1));
		instructions.add(new JumpInsnNode(IF_ICMPLE, nResetTo17Label));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, stateFieldRef));
		instructions.add(AbstractInsnNode.newInt(17));
		instructions.add(new JumpInsnNode(IF_ICMPEQ, nResetTo10Label));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, stateFieldRef));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(ISUB));
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, stateFieldRef));
		instructions.add(new GotoNode(argCheckExitLabel));
		instructions.add(nResetTo10Label);
		instructions.add(AbstractInsnNode.newInt(10));
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, stateFieldRef));
		instructions.add(new GotoNode(argCheckExitLabel));
		instructions.add(nResetTo17Label);
		instructions.add(AbstractInsnNode.newInt(17));
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, stateFieldRef));
		instructions.add(new GotoNode(argCheckExitLabel));
		instructions.add(noMatchingFlagLabel);
		instructions.add(InsnNode.of(POP));
		instructions.add(new GotoNode(argIteratorLoopStartLabel));
		instructions.add(argCheckExitLabel);
		instructions.add(InsnNode.of(POP));
		instructions.add(inputArgumentsTryEndLabel);
		instructions.add(new GotoNode(exitLabel));
		instructions.add(inputArgumentsCatchHandlerLabel);
		instructions.add(InsnNode.of(POP));
		instructions.add(new GotoNode(exitLabel));
		instructions.add(decoderLabel);
		MethodCallWriter toCharArrayCallWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "toCharArray", "()[C", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, toCharArrayCallWriter));
		instructions.add(AbstractInsnNode.storeObject(decodeCharsLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(decodeIndexLocalIndex, localVariableTable, 5));
		instructions.add(decodeLoopStartLabel);
		instructions.add(AbstractInsnNode.loadInt(decodeIndexLocalIndex, localVariableTable, 5));
		instructions.add(AbstractInsnNode.loadObject(decodeCharsLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(ARRAYLENGTH));
		instructions.add(new JumpInsnNode(IF_ICMPGE, decodeLoopEndLabel));
		instructions.add(AbstractInsnNode.loadObject(decodeCharsLocalIndex, localVariableTable, 5));
		instructions.add(AbstractInsnNode.loadInt(decodeIndexLocalIndex, localVariableTable, 5));
		instructions.add(InsnNode.of(DUP2));
		instructions.add(InsnNode.of(CALOAD));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, listField));
		MethodCallWriter arrayListGetCallWriter = constantPoolManager.getOrCreateMethodRef("java/util/ArrayList", "get", "(I)Ljava/lang/Object;", typeWriters, workspace, classProvider);
		instructions.add(AbstractInsnNode.loadInt(decodeIndexLocalIndex, localVariableTable, 5));
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, arrayListGetCallWriter));
		ClassInfoWriter ownerClassInfo = constantPoolManager.getOrCreateClassInfo(ownerClassName, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(CHECKCAST, ownerClassInfo));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.newInt(6));
		MethodNode keyMethodNode = simpleLongClass.findMethodNode(new MethodSignature("a", "(II)J"));
		MethodCallWriter keyMethodCallWriter = constantPoolManager.getOrCreateClassMethodRef(keyMethodNode, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, keyMethodCallWriter));
		instructions.add(AbstractInsnNode.ldcLong(127L, constantPoolManager, typeWriters));
		instructions.add(InsnNode.of(LAND));
		instructions.add(InsnNode.of(L2I));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(IXOR));
		instructions.add(InsnNode.of(I2C));
		instructions.add(InsnNode.of(CASTORE));
		instructions.add(AbstractInsnNode.IINC(decodeIndexLocalIndex, 1, localVariableTable, 5));
		instructions.add(new GotoNode(decodeLoopStartLabel));
		instructions.add(decodeLoopEndLabel);
		instructions.add(new NewNode(stringClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(AbstractInsnNode.loadObject(decodeCharsLocalIndex, localVariableTable, 5));
		MethodCallWriter stringConstructorCallWriter = constantPoolManager.getOrCreateMethodRef("java/lang/String", "<init>", "([C)V", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, stringConstructorCallWriter));
		instructions.add(InsnNode.of(SWAP));
		instructions.add(new TableSwitchNode(caseJavaagentLabel, 0, 7, caseLabels));
		instructions.add(exitLabel);
		callEntry.setTryCatchBlocks(tryCatchBlocks.toArray(new TryCatchBlockInfo[tryCatchBlocks.size()]));
		int bytecodeSize = 0;

		for (AbstractInsnNode instruction : instructions) {
			bytecodeSize += instruction.getBytecodeSize();
		}

		callEntry.getInstructions().addAll(instructions);
		return bytecodeSize;
	}

	private void createChangedMethods(
		MultiMap<ClassNode, MethodNode> indirectMethodsByClass, MultiMap<ClassNode, MethodNode> referencedMethodsByClass, Set<ClassNode> desStringClasses, Set<ClassNode> integerEncryptClasses, Set<ClassNode> longEncryptClasses
	) {
		HashSet<ClassNode> allClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(indirectMethodsByClass.size() + referencedMethodsByClass.size() + desStringClasses.size() + integerEncryptClasses.size() + longEncryptClasses.size()));
		allClasses.addAll(indirectMethodsByClass.keySet());
		allClasses.addAll(referencedMethodsByClass.keySet());
		allClasses.addAll(desStringClasses);
		allClasses.addAll(integerEncryptClasses);
		allClasses.addAll(longEncryptClasses);

		for (ClassNode classNode : allClasses) {
			if (!classNode.hasInnerClassInfo()) {
				List<MethodNode> indirectMethods = indirectMethodsByClass.get(classNode);
				Set<MethodNode> methodsToChange;
				if (indirectMethods != null) {
					methodsToChange = this.getMethodsToChange(classNode, indirectMethods);
				} else {
					methodsToChange = Collections.emptySet();
				}

				boolean hasNonClinitMethods = false;
				boolean hasUnchangedMethods = false;
				boolean hasMethodsToChange = !methodsToChange.isEmpty();
				MethodNode staticInitializer = null;
				if (indirectMethods != null) {
					for (AbstractMethodNode indirectMethod : indirectMethods) {
						if (indirectMethod.isClinit()) {
							staticInitializer = (MethodNode)indirectMethod;
						} else {
							hasNonClinitMethods = true;
							if (methodsToChange.isEmpty() || !methodsToChange.contains(indirectMethod)) {
								hasUnchangedMethods = true;
							}
						}
					}
				}

				List<MethodNode> referencedMethods = referencedMethodsByClass.get(classNode);
				if (referencedMethods != null) {
					for (AbstractMethodNode referencedMethod : referencedMethods) {
						hasNonClinitMethods = true;
						if (methodsToChange.isEmpty() || !methodsToChange.contains(referencedMethod)) {
							hasUnchangedMethods = true;
						}
					}
				}

				if (staticInitializer == null && (hasNonClinitMethods || desStringClasses.contains(classNode) || integerEncryptClasses.contains(classNode) || longEncryptClasses.contains(classNode))) {
					staticInitializer = classNode.getOrCreateStaticInitializer(this.workspace, "Method Parameter List Changing", 5);
				}

				ChangedMethod clinitChangedMethod = null;
				if (staticInitializer != null) {
					clinitChangedMethod = new ChangedMethod(staticInitializer, hasNonClinitMethods, !hasUnchangedMethods && hasMethodsToChange);
					this.changedMethodByMethod.put(staticInitializer, clinitChangedMethod);
				}

				for (MethodNode methodToChange : methodsToChange) {
					ChangedMethod changedMethod = new ChangedMethod(methodToChange);
					if (!Options.skipClinitDependencies) {
						clinitChangedMethod.addDependentMethod(changedMethod);
					}

					this.changedMethodByMethod.put(methodToChange, changedMethod);
				}
			}
		}
	}

	public List<Triplet> resolveMpcClassTriplets(
		Set<ClassNode> includedClasses,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> classesByModule,
		ReferenceGraph<ArchiveNode, ArchiveNode> moduleReferenceGraph,
		ReferenceGraph<ArchiveNode, String> moduleClassNameGraph,
		AtomicReference<Triplet> defaultClassTripletRef,
		Map<ArchiveNode, Triplet> archiveToMpcTripletMap,
		Map<ClassNode, Triplet> classToMpcTripletMap,
		String packageName,
		ReadOnlyMap<String, String> packageRenameMap,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyMap<String, String> internalToBinaryNameMap
	) {
		ArrayList<Triplet> mpcTriplets = new ArrayList<>();
		int initialArchiveTripletCount = archiveToMpcTripletMap.size();
		HashSet<String> allowedPackages = GlobalUtils.newHashSet();
		HashSet<ClassNode> nonModuleSourceClasses = GlobalUtils.newHashSet();
		HashSet<ClassNode> classesNotInModules = GlobalUtils.newHashSet();
		includedClasses = InjectedClassManager.collectEligibleClasses(includedClasses, nonModuleSourceClasses, classesNotInModules, allowedPackages);
		int nonModuleSourceClassCount = nonModuleSourceClasses.size();
		Map<String, Triplet> tripletsByClassName = GlobalUtils.newHashMap();
		HashSet<String> reservedClassNames = GlobalUtils.newHashSet();
		if (this.changeLogContext != null) {
			Triplet parameterChangerDataClasses = this.changeLogContext.getParameterChangeClasses();
			if (parameterChangerDataClasses != null) {
				parameterChangerDataClasses = this.validateAndResolveClassNames(parameterChangerDataClasses, internalToBinaryNameMap, allowedPackages, tripletsByClassName);
				if (parameterChangerDataClasses != null) {
					defaultClassTripletRef.set(parameterChangerDataClasses);
					reservedClassNames.add((String)parameterChangerDataClasses.getFirst());
					reservedClassNames.add((String)parameterChangerDataClasses.getSecond());
					reservedClassNames.add((String)parameterChangerDataClasses.getThird());

					for (ClassNode classNotInModule : classesNotInModules) {
						classToMpcTripletMap.put(classNotInModule, parameterChangerDataClasses);
					}
				}
			}

			for (ArchiveNode moduleArchive : this.classesByModule.keySet()) {
				String moduleName = moduleArchive.getModuleName();
				Triplet moduleParameterChangeClasses = this.changeLogContext.getModuleParameterChangeClasses(moduleName);
				if (moduleParameterChangeClasses != null) {
					if (classesByModule.containsKey(moduleArchive)) {
						moduleParameterChangeClasses = this.validateAndResolveClassNames(moduleParameterChangeClasses, internalToBinaryNameMap, moduleClassNameGraph.getReferences(moduleArchive), tripletsByClassName);
						if (moduleParameterChangeClasses != null) {
							archiveToMpcTripletMap.put(moduleArchive, moduleParameterChangeClasses);
							reservedClassNames.add((String)moduleParameterChangeClasses.getFirst());
							reservedClassNames.add((String)moduleParameterChangeClasses.getSecond());
							reservedClassNames.add((String)moduleParameterChangeClasses.getThird());
							List<ClassNode> moduleClasses = classesByModule.get(moduleArchive);
							if (moduleClasses != null) {
								for (ClassNode moduleClass : moduleClasses) {
									classToMpcTripletMap.put(moduleClass, moduleParameterChangeClasses);
								}
							}
						}
					} else {
						this.changeLogContext.logMessage("Module '" + moduleName + "' no longer requires the extra Method Parameter List Changing classes that are specified.");
					}
				}
			}

			if ((nonModuleSourceClassCount == 0 || !defaultClassTripletRef.isNull()) && initialArchiveTripletCount == archiveToMpcTripletMap.size()) {
				for (Entry<String, Triplet> tripletEntry : tripletsByClassName.entrySet()) {
					mpcTriplets.add(tripletEntry.getValue());
				}

				return mpcTriplets;
			}

			if (nonModuleSourceClassCount > 0) {
				String autoReflectionClassName = this.changeLogContext.getAutoReflectionClassName();
				if (autoReflectionClassName != null) {
					reservedClassNames.add(GlobalUtils.binaryToInternalName(autoReflectionClassName));
				}

				String obfuscateReferencesClassName = this.changeLogContext.getObfuscateReferencesClassName();
				if (obfuscateReferencesClassName != null) {
					reservedClassNames.add(GlobalUtils.binaryToInternalName(obfuscateReferencesClassName));
				}
			}

			Iterator<ArchiveNode> archiveIterator = classesByModule.keySet().iterator();

			while (archiveIterator.hasNext()) {
				String nextModuleName = archiveIterator.next().getModuleName();
				String moduleAutoReflectionClassName = this.changeLogContext.getModuleAutoReflectionClass(nextModuleName);
				if (moduleAutoReflectionClassName != null) {
					reservedClassNames.add(GlobalUtils.binaryToInternalName(moduleAutoReflectionClassName));
				}

				String moduleObfuscateReferencesClassName = this.changeLogContext.getModuleObfuscateReferencesClass(nextModuleName);
				if (moduleObfuscateReferencesClassName != null) {
					reservedClassNames.add(GlobalUtils.binaryToInternalName(moduleObfuscateReferencesClassName));
				}
			}
		}

		AtomicReference<String> dataClassNameRef = new AtomicReference<>();
		HashMap<ArchiveNode, String> archiveToDataClassNameMap = GlobalUtils.newHashMap();
		HashMap<ClassNode, String> classToDataClassNameMap = GlobalUtils.newHashMap();

		for (int tripletIndex = 0; tripletIndex < 3; tripletIndex++) {
			String existingDataClassName = null;
			Triplet dataClassTriplet = defaultClassTripletRef.isNull() ? null : defaultClassTripletRef.get();
			if (dataClassTriplet != null) {
				existingDataClassName = (String)dataClassTriplet.getByIndex(tripletIndex);
			}

			dataClassNameRef.setNull();
			archiveToDataClassNameMap.clear();

			for (Entry<ArchiveNode, Triplet> archiveTripletEntry : archiveToMpcTripletMap.entrySet()) {
				String existingArchiveDataClassName = (String)archiveTripletEntry.getValue().getByIndex(tripletIndex);
				if (existingArchiveDataClassName != null) {
					archiveToDataClassNameMap.put(archiveTripletEntry.getKey(), existingArchiveDataClassName);
				}
			}

			classToDataClassNameMap.clear();

			for (Entry<ClassNode, Triplet> classTripletEntry : classToMpcTripletMap.entrySet()) {
				String existingClassDataClassName = (String)classTripletEntry.getValue().getByIndex(tripletIndex);
				if (existingClassDataClassName != null) {
					classToDataClassNameMap.put(classTripletEntry.getKey(), existingClassDataClassName);
				}
			}

			InjectedClassManager.planAuxiliaryClassNames(
				includedClasses,
				existingDataClassName,
				dataClassNameRef,
				archiveToDataClassNameMap,
				classToDataClassNameMap,
				"methodParameterChangesPackage",
				packageName,
				nonModuleSourceClasses,
				classesNotInModules,
				classesByModule,
				moduleReferenceGraph,
				moduleClassNameGraph,
				packageRenameMap,
				classNameToInternalNameMap,
				internalToBinaryNameMap,
				reservedClassNames,
				this.newNamesPrefix,
				this.mixedCaseClassNames,
				this.uniqueClassNames,
				this.randomizeObfuscation,
				"Method Parameter List Changing",
				this.overrideNames,
				this.classRepository,
				this.scriptContext
			);
			if (!dataClassNameRef.isNull()) {
				String plannedDataClassName = dataClassNameRef.get();
				dataClassTriplet = defaultClassTripletRef.get();
				if (dataClassTriplet == null) {
					dataClassTriplet = tripletsByClassName.get(plannedDataClassName);
					if (dataClassTriplet == null) {
						dataClassTriplet = new Triplet(plannedDataClassName, null, null);
						tripletsByClassName.put(plannedDataClassName, dataClassTriplet);
					}

					defaultClassTripletRef.set(dataClassTriplet);
				} else if (dataClassTriplet.getByIndex(tripletIndex) == null) {
					dataClassTriplet.setByIndex(plannedDataClassName, tripletIndex);
				}
			}

			if (!archiveToDataClassNameMap.isEmpty()) {
				for (Entry<ArchiveNode, String> archiveNameEntry : archiveToDataClassNameMap.entrySet()) {
					ArchiveNode archive = archiveNameEntry.getKey();
					String plannedArchiveDataClassName = archiveNameEntry.getValue();
					Triplet archiveTriplet = archiveToMpcTripletMap.get(archive);
					if (archiveTriplet == null) {
						archiveTriplet = tripletsByClassName.get(plannedArchiveDataClassName);
						if (archiveTriplet == null) {
							archiveTriplet = new Triplet(plannedArchiveDataClassName, null, null);
							tripletsByClassName.put(plannedArchiveDataClassName, archiveTriplet);
						}

						archiveToMpcTripletMap.put(archive, archiveTriplet);
					} else if (archiveTriplet.getByIndex(tripletIndex) == null) {
						archiveTriplet.setByIndex(plannedArchiveDataClassName, tripletIndex);
					}
				}
			}

			for (Entry<ClassNode, String> classNameEntry : classToDataClassNameMap.entrySet()) {
				ClassNode classNode = classNameEntry.getKey();
				String plannedClassDataClassName = classNameEntry.getValue();
				if (classToMpcTripletMap.get(classNode) == null) {
					Triplet classNameTriplet = tripletsByClassName.get(plannedClassDataClassName);
					classToMpcTripletMap.put(classNode, classNameTriplet);
				}
			}

			if (tripletIndex == 0) {
				for (Entry<String, Triplet> firstPassTripletEntry : tripletsByClassName.entrySet()) {
					mpcTriplets.add(firstPassTripletEntry.getValue());
				}

				tripletsByClassName = new ReadOnlyMap<>(tripletsByClassName);
			}
		}

		return mpcTriplets;
	}

	private int emitCreateListEntry(
		Long saltValue,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter listField,
		MethodCallWriter ctorRef,
		MethodCallWriter addRef,
		String className,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AtomicReference<LongNode> longConstantRef,
		boolean java7OrNewer
	) {
		WideConstantPoolInsnNode insnNode = new WideConstantPoolInsnNode(GETSTATIC, listField);
		int bytecodeSize = 0 + insnNode.getBytecodeSize();
		instructions.add(insnNode);
		ClassInfoWriter listClassInfo = constantPoolManager.getOrCreateClassInfo(className, typeWriters);
		NewNode newListNode = new NewNode(listClassInfo);
		bytecodeSize += newListNode.getBytecodeSize();
		instructions.add(newListNode);
		bytecodeSize++;
		instructions.add(InsnNode.of(DUP));
		if (typeWriters.size() > 32000) {
			int encryptedConstantSize = AbstractInsnNode.encryptLongConstant(saltValue, instructions, constantPoolManager, typeWriters, longConstantRef);
			bytecodeSize += encryptedConstantSize;
		} else {
			AbstractInsnNode ldcLongInsn = AbstractInsnNode.ldcLong(saltValue, constantPoolManager, typeWriters);
			bytecodeSize += ldcLongInsn.getBytecodeSize();
			instructions.add(ldcLongInsn);
		}

		insnNode = new WideConstantPoolInsnNode(INVOKESPECIAL, ctorRef);
		bytecodeSize += insnNode.getBytecodeSize();
		instructions.add(insnNode);
		insnNode = new WideConstantPoolInsnNode(INVOKEVIRTUAL, addRef);
		bytecodeSize += insnNode.getBytecodeSize();
		instructions.add(insnNode);
		if (java7OrNewer) {
			bytecodeSize++;
			instructions.add(InsnNode.of(POP));
		}

		return bytecodeSize;
	}

	private void appendGeneratedMethodCalls(
		ClassNode fallbackClass,
		ClassNode decrypterInterface,
		ClassNode simpleLongClass,
		LocalVariableTable localVariableTable,
		List<MethodNode> generatedMethods,
		ArrayList<AbstractInsnNode> instructions,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		for (MethodNode generatedMethod : generatedMethods) {
			MethodCallWriter methodCallWriter = constantPoolManager.getOrCreateClassMethodRef(generatedMethod, typeWriters);
			instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 5));
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, methodCallWriter));
		}
	}

	private Set<MethodNode> getMethodsToChange(ClassNode classNode, List<MethodNode> candidateMethods) {
		HashSet<MethodNode> methodsToChange = GlobalUtils.newHashSet();
		TrimExcludedMemberSet trimExcludedMemberSet = this.scriptContext.getTrimExcludedMemberSet();
		if (trimExcludedMemberSet != null) {
			for (MethodNode candidateMethod : candidateMethods) {
				if (!candidateMethod.isClinit() && trimExcludedMemberSet.containsMethod(candidateMethod) && this.isMainMethod(candidateMethod) && !candidateMethod.getMethodOwner().hasInnerClassInfo()) {
					methodsToChange.add(candidateMethod);
				}
			}
		}

		for (MethodNode relationshipFileMethod : this.relationshipFileMethods) {
			if (relationshipFileMethod.getMethodOwner() == classNode) {
				if (candidateMethods.contains(relationshipFileMethod)) {
					methodsToChange.add(relationshipFileMethod);
					if (this.scriptContext.isVerbose()) {
						this.scriptContext
							.logRaw(
								"Setting initialization of method '"
									+ relationshipFileMethod.getOriginalNameWithParams()
									+ "' in class '"
									+ relationshipFileMethod.getClassNameBinary()
									+ "' as specified in relationship file '"
									+ Options.classToMethodInitSpecFile
									+ "'.",
								true
							);
					}
				} else {
					this.scriptContext
						.logError(
							"Method '"
								+ relationshipFileMethod.getOriginalNameWithParams()
								+ "' in class '"
								+ relationshipFileMethod.getClassNameBinary()
								+ "' specified in relationship file '"
								+ Options.classToMethodInitSpecFile
								+ "' but it has no relevant structures."
						);
				}
			}
		}

		return methodsToChange;
	}

	private List<MethodNode> createExtraCallMethods(List<ExtraCallEntry> extraCallEntries, ClassNode fallbackClass, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, AbstractWorkspace workspace) {
		ArrayList<MethodNode> generatedMethods = new ArrayList<>(extraCallEntries.size());
		int generatedMethodIndex = 0;

		for (ExtraCallEntry extraCallEntry : extraCallEntries) {
			MethodNode generatedMethod = fallbackClass.createMethodWithCode(
				"b" + generatedMethodIndex++, "()V", extraCallEntry.getInstructions(), 4, 1, 1, false, extraCallEntry.getLocalVariableTable(), new TryCatchBlockInfo[0], "MethodParameterListChanging", typeWriters, null, workspace, 5
			);
			generatedMethods.add(generatedMethod);
		}

		return generatedMethods;
	}

	public LocalVariableOperand emitSaltEncodedLongLocal(
		Long longValue,
		LocalVariableIndexCounter localIndexCounter,
		List<AbstractInsnNode> instructions,
		MethodNode method,
		Map<MethodNode, LocalVariableOperand> localOperandsByMethod,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters
	) {
		ArrayList<AbstractInsnNode> insnBuffer = new ArrayList<>();
		ChangedMethod changedMethod = null;
		if (this.changedMethodByMethod != null) {
			changedMethod = this.changedMethodByMethod.get(method);
		}

		if (changedMethod != null) {
			FieldTypeWriter saltFieldRef = changedMethod.getSaltFieldRef();
			if (saltFieldRef != null) {
				insnBuffer.add(new WideConstantPoolInsnNode(GETSTATIC, saltFieldRef));
			}

			long encodedLongValue = longValue ^ changedMethod.getMethodSalt();
			insnBuffer.add(AbstractInsnNode.ldcLong(encodedLongValue, constantPoolManager, typeWriters));
			insnBuffer.add(InsnNode.of(LXOR));
		} else {
			insnBuffer.add(AbstractInsnNode.ldcLong(longValue, constantPoolManager, typeWriters));
		}

		int localVarIndex = localIndexCounter.get();
		localIndexCounter.add(2);
		LocalVariableTable localVariableTable = method.getLocalVariableTable();
		insnBuffer.add(AbstractInsnNode.storeLong(localVarIndex, localVariableTable, 5));
		LocalVariableSlot localVariableSlot = localVariableTable.getVariable(localVarIndex);
		instructions.addAll(insnBuffer);
		localOperandsByMethod.put(method, localVariableSlot);
		return localVariableSlot;
	}

	private void remapDecrypterReferences(ClassNode auxiliaryClass, MethodParamObfuscationContext obfuscationContext) {
		HashMap<String, String> decrypterClassNameRemap = GlobalUtils.newHashMap(13);
		decrypterClassNameRemap.put(this.decrypterPlaceholders[0], obfuscationContext.getILongDecrypterClassName());
		decrypterClassNameRemap.put(this.decrypterPlaceholders[1], obfuscationContext.getFallbackLongDecrypterClassName());
		decrypterClassNameRemap.put(this.decrypterPlaceholders[2], obfuscationContext.getSimpleLongDecrypterClassName());
		auxiliaryClass.remapReferences(1, 1, decrypterClassNameRemap, GlobalUtils.newHashSet(), GlobalUtils.newHashSet(), GlobalUtils.newHashMap(), null);
	}

	private void addInitializationDependency(ClassHierarchyNode dominantClass, ClassHierarchyNode dependentClass, boolean falseDependency, Map<ClassNode, ChangedMethod> changedMethodsByClass, String specificationSource, boolean reportErrors) {
		AbstractClassNode dominantClassNode = dominantClass.getClassNode();
		if (!falseDependency || this.flowGroupingClasses == null || !this.flowGroupingClasses.contains(dominantClassNode)) {
			ChangedMethod dominantChangedMethod = changedMethodsByClass.get(dominantClassNode);
			if (dominantChangedMethod != null) {
				AbstractClassNode dependentClassNode = dependentClass.getClassNode();
				if (!falseDependency && this.flowGroupingClasses != null && this.flowGroupingClasses.contains(dependentClassNode)) {
					if (reportErrors) {
						this.scriptContext
							.logError(
								"Class '"
									+ dependentClassNode.getClassNameBinary()
									+ "' specified as dependent upon initialization of "
									+ dominantClassNode.getClassNameBinary()
									+ "' but '"
									+ dependentClassNode.getClassNameBinary()
									+ "' is used in Flow Obfuscation."
							);
					}

					return;
				}

				ChangedMethod dependentChangedMethod = changedMethodsByClass.get(dependentClassNode);
				if (dependentChangedMethod != null) {
					boolean dependencyEstablished = false;
					boolean alreadyDependent = false;
					if (!dependentChangedMethod.hasInitDependency() && !dependentChangedMethod.hasFalseInitDependency()) {
						if (falseDependency) {
							dominantChangedMethod.addFalseDependentMethod(dependentChangedMethod);
						} else {
							dominantChangedMethod.addDependentMethod(dependentChangedMethod);
						}

						dependencyEstablished = true;
						if (specificationSource != null && this.scriptContext.isVerbose()) {
							this.scriptContext
								.logRaw(
									"Setting initialization of class '"
										+ dependentClassNode.getClassNameBinary()
										+ " as"
										+ (falseDependency ? " FALSELY" : "")
										+ " dependent upon initialization of '"
										+ dominantClassNode.getClassNameBinary()
										+ "' because '"
										+ specificationSource
										+ "'",
									true
								);
						}
					} else {
						alreadyDependent = true;
						if (reportErrors) {
							if (dependentChangedMethod.hasInitDependency()) {
								this.scriptContext
									.logError(
										"Class '"
											+ dependentClassNode.getClassNameBinary()
											+ "' specified as a"
											+ (falseDependency ? " FALSE" : "")
											+ " dependent on '"
											+ dominantClassNode.getClassNameBinary()
											+ "' but the relationship could not be established because '"
											+ dependentClassNode.getClassNameBinary()
											+ "' is already dependent upon class '"
											+ dependentChangedMethod.getInitDependency().getMethodNode().getClassNameBinary()
											+ "'."
									);
							} else {
								this.scriptContext
									.logError(
										"Class '"
											+ dependentClassNode.getClassNameBinary()
											+ "' specified as a"
											+ (falseDependency ? " FALSE" : "")
											+ " dependent on '"
											+ dominantClassNode.getClassNameBinary()
											+ "' but the relationship could not be established because '"
											+ dependentClassNode.getClassNameBinary()
											+ "' is already dependent upon class '"
											+ dependentChangedMethod.getFalseInitDependency().getMethodNode().getClassNameBinary()
											+ "'."
									);
							}
						}
					}

					if (reportErrors && !dependencyEstablished && !alreadyDependent) {
						this.scriptContext
							.logError(
								"Class '"
									+ dependentClassNode.getClassNameBinary()
									+ "' specified as a"
									+ (falseDependency ? " FALSE" : "")
									+ " dependent on '"
									+ dominantClassNode.getClassNameBinary()
									+ "' but the relationship could not be established because '"
									+ dependentClassNode.getClassNameBinary()
									+ "' has no relevant structures."
							);
					}
				} else if (reportErrors) {
					this.scriptContext
						.logError(
							"Class '"
								+ dependentClassNode.getClassNameBinary()
								+ "' specified as a"
								+ (falseDependency ? " FALSE" : "")
								+ " dependent on '"
								+ dominantClassNode.getClassNameBinary()
								+ "' but it has no relevant structures."
						);
				}
			} else if (reportErrors) {
				this.scriptContext
					.logError("Class '" + dominantClassNode.getClassNameBinary() + "' specified as a" + (falseDependency ? " FALSE" : "") + " dominant class but it has no relevant structures.");
			}
		} else if (reportErrors) {
			this.scriptContext.logError("Class '" + dominantClassNode.getClassNameBinary() + "' specified as a false dominant class but it is used in Flow Obfuscation.");
		}
	}

	public void registerChangedMethod(MethodNode method, ChangedMethod changedMethod) {
		if (this.changedMethodByMethod != null) {
			this.changedMethodByMethod.put(method, changedMethod);
		}
	}

	private List<ChangedMethod> resolveInitDependencies() {
		ClassTreeManager classTreeManager = this.workspace.getClassTreeManager();
		HashMap<ClassNode, ChangedMethod> changedMethodByClassNode = GlobalUtils.newHashMap(this.changedMethodByMethod.size());
		ArrayList<ChangedMethod> allChangedMethods = new ArrayList<>(this.changedMethodByMethod.size());

		for (Entry<MethodNode, ChangedMethod> changedMethodEntry : this.changedMethodByMethod.entrySet()) {
			ChangedMethod changedMethod = changedMethodEntry.getValue();
			if (changedMethodEntry.getKey().isClinit()) {
				changedMethodByClassNode.put(changedMethodEntry.getKey().getMethodOwner(), changedMethod);
			}

			allChangedMethods.add(changedMethod);
		}

		if (!Options.skipHierarchyClinitPass) {
			for (ClassHierarchyNode classRootHierarchyNode : classTreeManager.getWorkspaceClassRoots()) {
				if (!classRootHierarchyNode.isInterface()) {
					this.propagateChangedMethods(classRootHierarchyNode, changedMethodByClassNode);
				}
			}
		}

		if (this.innerClassReferenceGraph != null && !Options.skipClinitDependencyPass) {
			for (Entry<ClassNode, Set<ClassNode>> innerClassRefEntry : this.innerClassReferenceGraph.getAdjacencyEntries()) {
				ClassNode enclosingClassNode = innerClassRefEntry.getKey();

				for (ClassNode innerClassNode : innerClassRefEntry.getValue()) {
					ClassHierarchyNode enclosingClassHierarchyNode = ClassHierarchyNode.getNodeByInternalName(enclosingClassNode.getInternalName());
					ClassHierarchyNode innerClassHierarchyNode = ClassHierarchyNode.getNodeByInternalName(innerClassNode.getInternalName());
					if (enclosingClassHierarchyNode != null && innerClassHierarchyNode != null) {
						this.addClassInitDependency(enclosingClassHierarchyNode, innerClassHierarchyNode, changedMethodByClassNode, null, false);
					}
				}
			}
		}

		if (Options.classInitSpecFile != null) {
			File initSpecFile = new File(Options.classInitSpecFile);
			if (initSpecFile.exists() && !initSpecFile.isDirectory()) {
				if (this.scriptContext.isVerbose()) {
					this.scriptContext.logRaw("Reading '" + initSpecFile + "' to find special Method Parameter List Changing class initialization specifications.", true);
				}

				try (FileReader initSpecReader = new FileReader(initSpecFile)) {
					Properties initSpecProperties = new Properties();
					initSpecProperties.load(initSpecReader);
					String initSpecSource = "specified in file \"" + initSpecFile.getAbsolutePath() + "\"";

					for (String dependentClassName : initSpecProperties.stringPropertyNames()) {
						String dependentInternalName = GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(dependentClassName.trim()), this.classNameToInternalNameMap);
						String prerequisiteClassName = initSpecProperties.getProperty(dependentClassName);
						ClassHierarchyNode prerequisiteHierarchyNode = ClassHierarchyNode.getNodeByInternalName(
							GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(prerequisiteClassName.trim()), this.classNameToInternalNameMap)
						);
						if (prerequisiteHierarchyNode == null) {
							this.scriptContext
								.logError(
									"Class '" + prerequisiteClassName + "' not opened. It was specified as initialized before class '" + dependentClassName + "' in file '" + initSpecFile.getAbsolutePath() + "'. (1)"
								);
						} else if (!prerequisiteHierarchyNode.isWorkspaceClass()) {
							this.scriptContext
								.logError(
									"Class '" + prerequisiteClassName + "' not opened. It was specified as initialized before class '" + dependentClassName + "' in file '" + initSpecFile.getAbsolutePath() + "'. (2)"
								);
						} else {
							ClassHierarchyNode dependentHierarchyNode = ClassHierarchyNode.getNodeByInternalName(dependentInternalName);
							if (dependentHierarchyNode == null) {
								this.scriptContext
									.logError(
										"Class '" + dependentClassName + "' not opened. It was specified as initialized after class '" + prerequisiteClassName + "' in file '" + initSpecFile.getAbsolutePath() + "'. (3)"
									);
							} else if (!dependentHierarchyNode.isWorkspaceClass()) {
								this.scriptContext
									.logError(
										"Class '" + dependentClassName + "' not opened. It was specified as initialized after class '" + prerequisiteClassName + "' in file '" + initSpecFile.getAbsolutePath() + "'. (4)"
									);
							} else {
								this.addClassInitDependency(prerequisiteHierarchyNode, dependentHierarchyNode, changedMethodByClassNode, initSpecSource, true);
							}
						}
					}

				} catch (IOException initSpecIoException) {
					this.scriptContext.logFatalError("Couldn't open relationship file '" + initSpecFile.getAbsolutePath() + "'. : " + initSpecIoException + "' (1)");
				}
			} else {
				this.scriptContext.logFatalError("Relationship file '" + initSpecFile.getAbsolutePath() + "' not found or is directory. (1)");
			}
		}

		if (Options.falseClassInitSpecFile != null) {
			File falseInitSpecFile = new File(Options.falseClassInitSpecFile);
			if (falseInitSpecFile.exists() && !falseInitSpecFile.isDirectory()) {
				if (this.scriptContext.isVerbose()) {
					this.scriptContext.logRaw("Reading '" + falseInitSpecFile + "' to find special Method Parameter List Changing FALSE class initialization specifications.", true);
				}

				try (FileReader falseInitSpecReader = new FileReader(falseInitSpecFile)) {
					Properties falseInitSpecProperties = new Properties();
					falseInitSpecProperties.load(falseInitSpecReader);
					String falseInitSpecSource = "specified in file \"" + falseInitSpecFile.getAbsolutePath() + "\"";

					for (String falseDependentClassName : falseInitSpecProperties.stringPropertyNames()) {
						String falseDependentInternalName = GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(falseDependentClassName.trim()), this.classNameToInternalNameMap);
						String falsePrerequisiteClassName = falseInitSpecProperties.getProperty(falseDependentClassName);
						ClassHierarchyNode falsePrerequisiteHierarchyNode = ClassHierarchyNode.getNodeByInternalName(
							GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(falsePrerequisiteClassName.trim()), this.classNameToInternalNameMap)
						);
						if (falsePrerequisiteHierarchyNode == null) {
							this.scriptContext
								.logError(
									"Class '"
										+ falsePrerequisiteClassName
										+ "' not opened. It was specified as falsely initialized before class '"
										+ falseDependentClassName
										+ "' in file '"
										+ falseInitSpecFile.getAbsolutePath()
										+ "'. (5)"
								);
						} else if (!falsePrerequisiteHierarchyNode.isWorkspaceClass()) {
							this.scriptContext
								.logError(
									"Class '"
										+ falsePrerequisiteClassName
										+ "' not opened. It was specified as falsely initialized before class '"
										+ falseDependentClassName
										+ "' in file '"
										+ falseInitSpecFile.getAbsolutePath()
										+ "'. (6)"
								);
						} else {
							ClassHierarchyNode falseDependentHierarchyNode = ClassHierarchyNode.getNodeByInternalName(falseDependentInternalName);
							if (falseDependentHierarchyNode == null) {
								this.scriptContext
									.logError(
										"Class '"
											+ falseDependentClassName
											+ "' not opened. It was specified as falsely initialized after class '"
											+ falsePrerequisiteClassName
											+ "' in file '"
											+ falseInitSpecFile.getAbsolutePath()
											+ "'. (7)"
									);
							} else if (!falseDependentHierarchyNode.isWorkspaceClass()) {
								this.scriptContext
									.logError(
										"Class '"
											+ falseDependentClassName
											+ "' not opened. It was specified as falsely initialized after class '"
											+ falsePrerequisiteClassName
											+ "' in file '"
											+ falseInitSpecFile.getAbsolutePath()
											+ "'. (8)"
									);
							} else {
								this.addInitializationDependency(falsePrerequisiteHierarchyNode, falseDependentHierarchyNode, true, changedMethodByClassNode, falseInitSpecSource, true);
							}
						}
					}
				} catch (IOException falseInitSpecIoException) {
					this.scriptContext.logFatalError("Couldn't open 'false' relationship file '" + falseInitSpecFile.getAbsolutePath() + "'. : " + falseInitSpecIoException + "' (1)");
				}
			} else {
				this.scriptContext.logFatalError("'False' relationship file '" + falseInitSpecFile.getAbsolutePath() + "' not found or is directory. (1)");
			}
		}

		if (this.classInitOrderBuilder != null) {
			ReferenceGraph<ClassNode, ClassNode> initOrderReferenceGraph = this.classInitOrderBuilder.getReferenceGraph();
			String initOrderSpecSource = "specified in a \"" + ClassInitializationOrderStatement.COMMAND_NAME + "\" statement";

			for (Entry<ClassNode, Set<ClassNode>> initOrderRefEntry : initOrderReferenceGraph.getAdjacencyEntries()) {
				ClassNode orderPrerequisiteClassNode = initOrderRefEntry.getKey();

				for (ClassNode orderDependentClassNode : initOrderRefEntry.getValue()) {
					ChangedMethod orderPrerequisiteChangedMethod = changedMethodByClassNode.get(orderPrerequisiteClassNode);
					boolean dependencyAvailable = orderPrerequisiteChangedMethod != null && !orderPrerequisiteChangedMethod.hasInitDependency() && !orderPrerequisiteChangedMethod.hasFalseInitDependency();
					ClassHierarchyNode orderPrerequisiteHierarchyNode = ClassHierarchyNode.getNodeByInternalName(orderPrerequisiteClassNode.getInternalName());
					ClassHierarchyNode orderDependentHierarchyNode = ClassHierarchyNode.getNodeByInternalName(orderDependentClassNode.getInternalName());
					if (dependencyAvailable && (Options.forceFalseDependency || random.nextInt(4) == 0 && !Options.dontRandomizeFalseDependency)) {
						this.addInitializationDependency(orderDependentHierarchyNode, orderPrerequisiteHierarchyNode, true, changedMethodByClassNode, initOrderSpecSource, Options.logClassInitOrderErrors);
					} else {
						this.addInitializationDependency(orderPrerequisiteHierarchyNode, orderDependentHierarchyNode, false, changedMethodByClassNode, initOrderSpecSource, Options.logClassInitOrderErrors);
					}
				}
			}
		}

		ArrayList<ChangedMethod> methodsWithoutInitDependency = new ArrayList<>();

		for (ChangedMethod candidateChangedMethod : allChangedMethods) {
			if (!candidateChangedMethod.hasInitDependency()) {
				methodsWithoutInitDependency.add(candidateChangedMethod);
			}
		}

		return methodsWithoutInitDependency;
	}

	public Triplet getMpcClassTriplet(ClassNode classNode) {
		return this.classToMpcClassTripletMap.get(classNode);
	}

	private List<MethodNode> createSizedExtraCallMethods(
		List<ExtraCallEntry> extraCallEntries, ClassNode fallbackClass, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, int maxStack, int maxLocals, AbstractWorkspace workspace
	) {
		ArrayList<MethodNode> createdMethods = new ArrayList<>(extraCallEntries.size());
		int methodIndex = 0;

		for (ExtraCallEntry extraCallEntry : extraCallEntries) {
			MethodNode createdMethod = fallbackClass.createMethodWithCode(
				"d" + methodIndex++,
				"()V",
				extraCallEntry.getInstructions(),
				maxStack,
				maxLocals,
				1,
				false,
				extraCallEntry.getLocalVariableTable(),
				new TryCatchBlockInfo[0],
				"MethodParameterListChanging",
				typeWriters,
				null,
				workspace,
				5
			);
			createdMethods.add(createdMethod);
		}

		return createdMethods;
	}

	private void emitExtraCallInitCode(
		ClassNode simpleLongClass,
		ClassNode fallbackClass,
		LocalVariableTable localVariableTable,
		ArrayList<AbstractInsnNode> instructions,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AtomicInt additionalLocals,
		AtomicInt requiredStackSize,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean java5OrNewer
	) {
		additionalLocals.set(1);
		requiredStackSize.set(4);
		String collectionInternalName;
		String collectionDescriptor;
		if (java5OrNewer) {
			collectionInternalName = "java/util/ArrayList";
			collectionDescriptor = "Ljava/util/ArrayList;";
		} else {
			collectionInternalName = "java/util/Vector";
			collectionDescriptor = "Ljava/util/Vector;";
		}

		FieldNode collectionField = (FieldNode)simpleLongClass.findField("f", collectionDescriptor);
		FieldTypeWriter collectionFieldRef = constantPoolManager.getOrCreateFieldRef(collectionField, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, collectionFieldRef));
		MethodCallWriter collectionSizeMethodRef = constantPoolManager.getOrCreateMethodRef(collectionInternalName, "size", "()I", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, collectionSizeMethodRef));
		FieldNode collectionSizeField = (FieldNode)simpleLongClass.findField("i", "I");
		FieldTypeWriter collectionSizeFieldRef = constantPoolManager.getOrCreateFieldRef(collectionSizeField, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, collectionSizeFieldRef));
		MethodNode extraCallInitMethod = simpleLongClass.findMethodNode(new MethodSignature("c", "()V"));
		MethodCallWriter extraCallInitMethodRef = constantPoolManager.getOrCreateClassMethodRef(extraCallInitMethod, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, extraCallInitMethodRef));
		instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 5));
		MethodNode extraCallMethod = fallbackClass.findMethodNode(new MethodSignature("d", "()V"));
		MethodCallWriter extraCallMethodRef = constantPoolManager.getOrCreateClassMethodRef(extraCallMethod, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, extraCallMethodRef));
	}

	private Triplet getResolvedMethodTriplet(Triplet classTriplet) {
		if (!this.resolvedMethodsByTriplet.containsKey(classTriplet)) {
			this.resolveMethodTriplet(classTriplet);
		}

		return this.resolvedMethodsByTriplet.get(classTriplet);
	}

	private InstructionBlock buildBitPermutationLoadBlock(
		ClassNode fallbackClass, ClassNode simpleLongClass, LocalVariableTable localVariableTable, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, AtomicInt maxLocals, AtomicInt maxStack
	) {
		maxStack.set(1);
		maxLocals.set(1);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		FieldNode bitPermutationField = (FieldNode)simpleLongClass.findField("e", "[I");
		FieldTypeWriter bitPermutationFieldRef = constantPoolManager.getOrCreateFieldRef(bitPermutationField, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, bitPermutationFieldRef));
		return new InstructionBlock(instructions, -1, 1, 1);
	}

	public int[] getBitPermutation() {
		return this.bitPermutation;
	}

	private int emitBitPermutationStore(
		ClassNode auxiliaryClass,
		boolean permutationChanged,
		int[] bitPermutation,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter permutationFieldRef,
		MethodCallWriter cloneMethodRef,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		int bytecodeSize;
		if (permutationChanged) {
			AbstractInsnNode intInsn = AbstractInsnNode.newInt(64);
			bytecodeSize = 0 + intInsn.getBytecodeSize();
			instructions.add(intInsn);
			bytecodeSize += 2;
			instructions.add(new NewArrayNode(10));

			for (int permutationIndex = 0; permutationIndex < 64; permutationIndex++) {
				bytecodeSize++;
				instructions.add(InsnNode.of(DUP));
				intInsn = AbstractInsnNode.newInt(permutationIndex);
				bytecodeSize += intInsn.getBytecodeSize();
				instructions.add(intInsn);
				intInsn = AbstractInsnNode.newInt(bitPermutation[permutationIndex]);
				bytecodeSize += intInsn.getBytecodeSize();
				instructions.add(intInsn);
				bytecodeSize++;
				instructions.add(InsnNode.of(IASTORE));
			}
		} else {
			WideConstantPoolInsnNode fieldInsn = new WideConstantPoolInsnNode(GETSTATIC, permutationFieldRef);
			bytecodeSize = 0 + fieldInsn.getBytecodeSize();
			instructions.add(fieldInsn);
			fieldInsn = new WideConstantPoolInsnNode(INVOKEVIRTUAL, cloneMethodRef);
			bytecodeSize += fieldInsn.getBytecodeSize();
			instructions.add(fieldInsn);
			ClassInfoWriter intArrayClassInfo = constantPoolManager.getOrCreateClassInfo("[I", typeWriters);
			fieldInsn = new WideConstantPoolInsnNode(CHECKCAST, intArrayClassInfo);
			bytecodeSize += fieldInsn.getBytecodeSize();
			instructions.add(fieldInsn);
		}

		WideConstantPoolInsnNode storeFieldInsn = new WideConstantPoolInsnNode(PUTSTATIC, permutationFieldRef);
		bytecodeSize += storeFieldInsn.getBytecodeSize();
		instructions.add(storeFieldInsn);
		return bytecodeSize;
	}

	private void readRelationshipFileMethods() {
		if (Options.classToMethodInitSpecFile != null) {
			File specFile = new File(Options.classToMethodInitSpecFile);
			if (specFile.exists() && !specFile.isDirectory()) {
				if (this.scriptContext.isVerbose()) {
					this.scriptContext.logRaw("Reading '" + specFile + "' to find special Method Parameter List Changing class to method initialization specifications.", true);
				}

				try (BufferedReader reader = new BufferedReader(new FileReader(specFile))) {
					while (true) {
						String line;
						if ((line = reader.readLine()) == null) {
							break;
						}

						line = line.trim();
						if (line.length() > 0 && !line.startsWith("//")) {
							int separatorIndex = line.indexOf(32);
							if (separatorIndex == -1 || separatorIndex >= line.length() - 3) {
								this.scriptContext
									.logFatalError(
										"Invalid line in relationship specification file '"
											+ specFile.getAbsolutePath()
											+ "'. Line not in format like 'mypackage.MyClass0 void methodName(int[], mypackage.Class1)' : '"
											+ line
											+ "' (1)"
									);
							}

							String className = line.substring(0, separatorIndex);
							String methodSpec = line.substring(separatorIndex + 1);
							ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(
								GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(className), this.classNameToInternalNameMap)
							);
							if (classHierarchyNode == null || !classHierarchyNode.isWorkspaceClass()) {
								this.scriptContext.logFatalError("Relationship specification file '" + specFile.getAbsolutePath() + "' specifies class '" + className + "' which has not been opened.");
							}

							ClassNode declaringClassNode = (ClassNode)classHierarchyNode.getClassNode();
							int openParenIndex = methodSpec.indexOf(40);
							int closeParenIndex = methodSpec.indexOf(41);
							if (openParenIndex < 1 || closeParenIndex == -1 || closeParenIndex != methodSpec.length() - 1) {
								this.scriptContext
									.logFatalError(
										"Relationship specification file '"
											+ specFile.getAbsolutePath()
											+ "' specifies invalid method signature '"
											+ methodSpec
											+ "' which is not in format like 'void methodName(int[], mypackage.Class1)' (1)."
									);
							}

							String returnTypeAndName = methodSpec.substring(0, openParenIndex);
							int nameSeparatorIndex = returnTypeAndName.indexOf(32);
							if (nameSeparatorIndex < 1 || nameSeparatorIndex == returnTypeAndName.length() - 1) {
								this.scriptContext
									.logFatalError(
										"Relationship specification file '"
											+ specFile.getAbsolutePath()
											+ "' specifies invalid method signature '"
											+ methodSpec
											+ "' which is not in format like 'void methodName(int[], mypackage.Class1)' (2)."
									);
							}

							String returnTypeDescriptor = MethodSignature.fromHumanReadableType(returnTypeAndName.substring(0, nameSeparatorIndex));
							String methodName = returnTypeAndName.substring(nameSeparatorIndex + 1);
							String parametersDescriptor = MethodSignature.fromHumanReadableParameters(methodSpec.substring(openParenIndex + 1, closeParenIndex));
							MethodSignature methodSignature = new MethodSignature(methodName, parametersDescriptor, returnTypeDescriptor);
							if (methodSignature.isStaticInitializer()) {
								this.scriptContext
									.logFatalError("Relationship specification file '" + specFile.getAbsolutePath() + "' specifies a class initializer method in the line '" + line + "'.");
							}

							MethodSignature resolvedSignature = GlobalUtils.getFromRegistryOrDefault(declaringClassNode, methodSignature, this.methodRegistryView);
							MethodNode methodNode = declaringClassNode.findMethodNode(resolvedSignature);
							if (methodNode == null) {
								this.scriptContext
									.logFatalError(
										"Method '"
											+ methodSignature
											+ "' not found in class '"
											+ className
											+ "'. The method was specified in relationship specification file '"
											+ specFile.getAbsolutePath()
											+ "' in the line '"
											+ line
											+ "'."
									);
							} else {
								this.relationshipFileMethods.add(methodNode);
							}
						}
					}
				} catch (IOException ioException) {
					this.scriptContext.logFatalError("Couldn't open relationship file '" + specFile.getAbsolutePath() + "'. : " + ioException + "' (2)");
				}
			} else {
				this.scriptContext.logFatalError("Relationship file '" + specFile.getAbsolutePath() + "' not found or is a directory. (2)");
			}
		}
	}

	public int buildSaltInitCode(ClassNode classNode, List<TypeWriter> typeWriters, ChangedMethod changedMethod, ChangedMethod initDependency, AbstractWorkspace workspace, ClassProvider classProvider) {
		ArrayList<AbstractInsnNode> decryptionInstructions = new ArrayList<>();
		Triplet mpcClassTriplet = this.getMpcClassTriplet(classNode);
		ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
		byte maxStack;
		if (mpcClassTriplet != null) {
			Triplet resolvedMethodTriplet = this.getResolvedMethodTriplet(mpcClassTriplet);
			MethodNode lockObjectMethod = (MethodNode)resolvedMethodTriplet.getFirst();
			MethodNode staticDecrypterMethod = (MethodNode)resolvedMethodTriplet.getSecond();
			MethodNode interfaceDecrypterMethod = (MethodNode)resolvedMethodTriplet.getThird();
			if (Options.ZKM_METHOD_PARAM_SYNCH) {
				MethodCallWriter monitorEnterLockRef = constantPoolManager.getOrCreateClassMethodRef(lockObjectMethod, typeWriters);
				decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, monitorEnterLockRef));
				decryptionInstructions.add(new MonitorEnterNode());
			}

			MethodCallWriter staticDecrypterMethodRef = constantPoolManager.getOrCreateClassMethodRef(staticDecrypterMethod, typeWriters);
			InterfaceMethodCallWriter interfaceDecrypterMethodRef = constantPoolManager.getOrCreateInterfaceMethodRef(interfaceDecrypterMethod, typeWriters);
			decryptionInstructions.add(AbstractInsnNode.ldcLong(changedMethod.getMethodValue(), constantPoolManager, typeWriters));
			decryptionInstructions.add(AbstractInsnNode.ldcLong(changedMethod.getRandomSaltValue(), constantPoolManager, typeWriters));
			if (classNode.isJava7OrNewer() && !Options.dontUseMethodHandlesLookup) {
				MethodCallWriter lookupMethodRef = constantPoolManager.getOrCreateMethodRef(
					"java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", typeWriters, workspace, classProvider
				);
				decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, lookupMethodRef));
				MethodCallWriter lookupClassMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodHandles$Lookup", "lookupClass", "()Ljava/lang/Class;", typeWriters, workspace, classProvider);
				decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, lookupClassMethodRef));
			} else {
				decryptionInstructions.add(InsnNode.of(ACONST_NULL));
			}

			decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, staticDecrypterMethodRef));
			decryptionInstructions.add(AbstractInsnNode.ldcLong(changedMethod.getSaltSeed(), constantPoolManager, typeWriters));
			decryptionInstructions.add(new InvokeInterfaceNode(interfaceDecrypterMethodRef));
			if (initDependency != null && initDependency.sharesSaltField()) {
				decryptionInstructions.add(new WideConstantPoolInsnNode(GETSTATIC, initDependency.getSaltFieldRef()));
				decryptionInstructions.add(InsnNode.of(LXOR));
			}

			if (Options.ZKM_METHOD_PARAM_SYNCH) {
				MethodCallWriter monitorExitLockRef = constantPoolManager.getOrCreateClassMethodRef(lockObjectMethod, typeWriters);
				decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, monitorExitLockRef));
				decryptionInstructions.add(new MonitorExitNode());
			}

			maxStack = 5;
		} else {
			decryptionInstructions.add(AbstractInsnNode.ldcLong(changedMethod.getMethodSalt(), constantPoolManager, typeWriters));
			maxStack = 2;
		}

		changedMethod.setDecryptionInstructions(decryptionInstructions);
		return maxStack;
	}

	public void buildMethodParameterChanges(
		MultiMap<ClassNode, MethodNode> unreferencedMethodsByClass,
		MultiMap<ClassNode, MethodNode> referencedMethodsByClass,
		Set<ClassNode> desStringEncryptionClasses,
		Set<ClassNode> integerEncryptionClasses,
		Set<ClassNode> longEncryptionClasses,
		AtomicReference<Triplet> defaultMpcClassTripletRef,
		Map<ArchiveNode, Triplet> archiveToMpcClassTripletMap,
		IReferenceGraph<ClassNode, ClassNode> methodParameterChangeReferenceGraph,
		String methodParameterChangesPackage,
		ReadOnlyMap<String, String> packageRenameMap,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyMap<String, String> internalToBinaryNameMap,
		Map<MethodNode, ChangedMethod> changedMethodByMethod,
		AtomicReference<ClassNode[]> generatedClassesRef,
		Inheritance inheritance,
		ChangeLogContext changeLogContext
	) {
		this.changedMethodByMethod = changedMethodByMethod;
		HashSet<ClassNode> includedClasses = GlobalUtils.newHashSet(unreferencedMethodsByClass.size());

		for (ClassNode unreferencedClass : unreferencedMethodsByClass.keySet()) {
			if (unreferencedClass.isWorkspaceNode()) {
				includedClasses.add(unreferencedClass);
			}
		}

		for (ClassNode referencedClass : referencedMethodsByClass.keySet()) {
			if (referencedClass.isWorkspaceNode()) {
				includedClasses.add(referencedClass);
			}
		}

		includedClasses.addAll(desStringEncryptionClasses);
		includedClasses.addAll(integerEncryptionClasses);
		includedClasses.addAll(longEncryptionClasses);
		int includedClassCount = includedClasses.size();
		int moduleCount = this.classesByModule.size();
		MultiMap<ArchiveNode, ClassNode> includedClassesByModule = new MultiMap<>(moduleCount);

		for (Entry<ArchiveNode, List<ClassNode>> classesByModuleEntry : this.classesByModule.entrySet()) {
			ArchiveNode moduleNode = classesByModuleEntry.getKey();

			for (ClassNode moduleClass : classesByModuleEntry.getValue()) {
				if (includedClasses.contains(moduleClass)) {
					includedClassesByModule.put(moduleNode, moduleClass);
				}
			}
		}

		UnmodifiableMultiMap<ArchiveNode, ClassNode> classesByModule = new UnmodifiableMultiMap<>(includedClassesByModule);
		int includedModuleCount = classesByModule.size();
		ReferenceGraph<ArchiveNode, ArchiveNode> moduleReferenceGraph = new ReferenceGraph<>(includedModuleCount);
		ReferenceGraph<ArchiveNode, String> moduleClassNameGraph = new ReferenceGraph<>(includedModuleCount);
		InjectedClassManager.filterNamedModules(classesByModule, moduleReferenceGraph, moduleClassNameGraph);
		AtomicReference<Triplet> defaultClassTripletRef = new AtomicReference<>();
		HashMap<ArchiveNode, Triplet> archiveToMpcTripletMap = GlobalUtils.newHashMap(GlobalUtils.newHashMap(includedModuleCount));
		HashMap<ClassNode, Triplet> classToMpcTripletMap = GlobalUtils.newHashMap(GlobalUtils.newHashMap(includedClassCount));
		List<Triplet> mpcClassTriplets = this.resolveMpcClassTriplets(includedClasses, classesByModule, moduleReferenceGraph, moduleClassNameGraph, defaultClassTripletRef, archiveToMpcTripletMap, classToMpcTripletMap, methodParameterChangesPackage, packageRenameMap, classNameToInternalNameMap, internalToBinaryNameMap);
		ClassNode[] generatedClasses = null;
		HashMap<Triplet, Triplet> decrypterClassTripletsByMpcTriplet = GlobalUtils.newHashMap(GlobalUtils.newHashMap(mpcClassTriplets.size()));

		for (Triplet mpcClassTriplet : mpcClassTriplets) {
			LinkedHashSet<String> dataClasses = new LinkedHashSet<>(13);
			dataClasses.add((String)mpcClassTriplet.getFirst());
			dataClasses.add((String)mpcClassTriplet.getSecond());
			dataClasses.add((String)mpcClassTriplet.getThird());
			AtomicReference<ClassNode> dataClassNode = new AtomicReference<>();
			HashMap<ArchiveNode, ClassNode> archiveToDataClassNodeMap = GlobalUtils.newHashMap();
			HashMap<ClassNode, ClassNode> classToDataClassNodeMap = GlobalUtils.newHashMap();
			OrderedSet<ClassNode> generatedDataClasses = new OrderedSet<>(3);
			HashMap<ClassNode, String> classToDataClassNameMap = GlobalUtils.newHashMap();
			AtomicReference<String> dataClassName = new AtomicReference<>();
			HashMap<ArchiveNode, String> archiveToDataClassNameMap = GlobalUtils.newHashMap();
			MethodParamObfuscationContext obfuscationContext = new MethodParamObfuscationContext(mpcClassTriplet);
			generatedClasses = InjectedClassManager.generateAndRegisterClasses(
				dataClasses,
				classToDataClassNameMap,
				dataClassName,
				dataClassNode,
				archiveToDataClassNodeMap,
				classToDataClassNodeMap,
				generatedDataClasses,
				archiveToDataClassNameMap,
				this.injectClassMajorVersion,
				this.injectClassMinorVersion,
				this,
				this.workspace,
				obfuscationContext
			);
			if (Options.ZKM_METHOD_PARAMETER_MAP_CLASS != null) {
				HashMap<String, String> hashTableTypeRenameMap = GlobalUtils.newHashMap(13);
				String customHashTableInternalName = Options.ZKM_METHOD_PARAMETER_MAP_CLASS.replace('.', '/');
				if (generatedDataClasses.iterator().next().isJava5OrNewer()) {
					hashTableTypeRenameMap.put("java/util/concurrent/ConcurrentHashMap", customHashTableInternalName);
				} else {
					hashTableTypeRenameMap.put("java/util/Hashtable", customHashTableInternalName);
				}

				Iterator<ClassNode> dataClassIterator = generatedDataClasses.iterator();

				while (dataClassIterator.hasNext()) {
					dataClassIterator.next().remapReferences(1, 1, hashTableTypeRenameMap, GlobalUtils.newHashSet(), GlobalUtils.newHashSet(), GlobalUtils.newHashMap(), null);
				}
			}

			Triplet decrypterClassTriplet = new Triplet(generatedDataClasses.get(0), generatedDataClasses.get(1), generatedDataClasses.get(2));
			decrypterClassTripletsByMpcTriplet.put(mpcClassTriplet, decrypterClassTriplet);
		}

		generatedClassesRef.set(generatedClasses);
		if (!defaultClassTripletRef.isNull()) {
			defaultMpcClassTripletRef.set(decrypterClassTripletsByMpcTriplet.get(defaultClassTripletRef.get()));
		}

		for (Entry<ArchiveNode, Triplet> mpcTripletEntry : archiveToMpcTripletMap.entrySet()) {
			Triplet decrypterClassTripletForArchive = decrypterClassTripletsByMpcTriplet.get(mpcTripletEntry.getValue());
			archiveToMpcClassTripletMap.put(mpcTripletEntry.getKey(), decrypterClassTripletForArchive);
		}

		for (Entry<ClassNode, Triplet> classToMpcTripletEntry : classToMpcTripletMap.entrySet()) {
			Triplet decrypterClassTripletForClass = decrypterClassTripletsByMpcTriplet.get(classToMpcTripletEntry.getValue());
			ClassNode classNode = classToMpcTripletEntry.getKey();
			this.classToMpcClassTripletMap.put(classToMpcTripletEntry.getKey(), decrypterClassTripletForClass);

			for (int tripletIndex = 0; tripletIndex < 3; tripletIndex++) {
				ClassNode decrypterClass = (ClassNode)decrypterClassTripletForClass.getByIndex(tripletIndex);
				methodParameterChangeReferenceGraph.addReference(decrypterClass, classNode);
				if (!classNode.isFromFilesystem() && decrypterClass.isFromFilesystem()) {
					decrypterClass.markLoadedFromFilesystem();
				}
			}
		}

		HashSet<Triplet> decrypterClassTriplets = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(decrypterClassTripletsByMpcTriplet.size() * 3));

		for (Entry<Triplet, Triplet> decrypterTripletEntry : decrypterClassTripletsByMpcTriplet.entrySet()) {
			decrypterClassTriplets.add(decrypterTripletEntry.getValue());
		}

		if (this.scriptContext.isVerbose()) {
			PrintWriter writer = this.scriptContext.getWriter();
			MultiMap<Triplet, ArchiveNode> archivesByTriplet = new MultiMap<>(decrypterClassTripletsByMpcTriplet.size() * 3, archiveToMpcClassTripletMap.size());

			for (Entry<ArchiveNode, Triplet> archiveToTripletEntry : archiveToMpcClassTripletMap.entrySet()) {
				ArchiveNode archiveNode = archiveToTripletEntry.getKey();
				archivesByTriplet.put(archiveToTripletEntry.getValue(), archiveNode);
			}

			String logMessage = InjectedClassManager.logCreatedTripletClasses("Method Parameter List Changing", archivesByTriplet, decrypterClassTriplets);
			writer.print(logMessage);
		}

		Map<MethodNode, long[]> saltValuesByMethod = null;
		if (Options.antiDebugger && changeLogContext != null && changeLogContext.hasParameterChangeLookupData()) {
			HashMap<ClassNode, long[]> saltValuesByClass = GlobalUtils.newHashMap();
			saltValuesByMethod = changeLogContext.buildMethodSaltValues(saltValuesByClass);
			if (!saltValuesByClass.isEmpty()) {
				for (Entry<ClassNode, long[]> saltValuesEntry : saltValuesByClass.entrySet()) {
					ClassNode classNodeWithoutInit = saltValuesEntry.getKey();
					MethodNode staticInitializer = classNodeWithoutInit.getOrCreateStaticInitializer(this.workspace, "Method Parameter List Changing", 5);
					unreferencedMethodsByClass.put(classNodeWithoutInit, staticInitializer);
					saltValuesByMethod.put(staticInitializer, saltValuesEntry.getValue());
					if (!this.classToMpcClassTripletMap.containsKey(classNodeWithoutInit)) {
						Triplet defaultTriplet = decrypterClassTriplets.iterator().next();
						this.classToMpcClassTripletMap.put(classNodeWithoutInit, defaultTriplet);
					}
				}
			}
		}

		this.createChangedMethods(unreferencedMethodsByClass, referencedMethodsByClass, desStringEncryptionClasses, integerEncryptionClasses, longEncryptionClasses);
		List<ChangedMethod> initDependencyMethods = this.resolveInitDependencies();
		ArrayList<ChangedMethod> changedMethods = new ArrayList<>(this.changedMethodByMethod.size());

		for (Entry<MethodNode, ChangedMethod> changedMethodEntry : this.changedMethodByMethod.entrySet()) {
			changedMethods.add(changedMethodEntry.getValue());
		}

		int maxSaltCount = SaltManager.maxSaltCountFor(changedMethods.size(), new AtomicInt());
		ArrayList<Object> decrypterConstants = new ArrayList<>(maxSaltCount / 2);
		ArrayList<Object> saltValueList = new ArrayList<>(maxSaltCount);
		AtomicReference<int[]> bitPermutationRef = new AtomicReference<>();
		ArrayList<Object> tableVariantList = new ArrayList<>();
		ArrayList<Pair> saltLinkPairs = new ArrayList<>(maxSaltCount);
		ArrayList<Pair> saltValuePairs = new ArrayList<>(maxSaltCount);
		SaltManager saltManager;
		if (Options.antiDebugger && changeLogContext != null && changeLogContext.hasParameterChangeLookupData()) {
			saltManager = new SaltManager(changedMethods, decrypterConstants, saltValueList, bitPermutationRef, tableVariantList, saltLinkPairs, saltValuesByMethod, randomLongIterator, random);
		} else {
			this.saltOrders = new ArrayList<>();
			AtomicReference<int[]> finalBitPermutationRef = new AtomicReference<>();
			saltManager = new SaltManager(
				initDependencyMethods, changedMethods, decrypterConstants, saltValueList, bitPermutationRef, tableVariantList, saltLinkPairs, saltValuePairs, this.saltOrders, finalBitPermutationRef, randomLongIterator, random, EnvironmentUtils.isJava7OrNewer(this.injectClassMajorVersion)
			);
			this.bitPermutation = finalBitPermutationRef.get();
		}

		int indexBitOffset = saltManager.getIndexBitOffset();
		int maxSortDepth = saltManager.getMaxSortDepth();
		int tableBatchSize = saltManager.getTableBatchSize();
		this.buildDecrypterClasses(decrypterClassTriplets, initDependencyMethods, indexBitOffset, maxSortDepth, tableBatchSize, decrypterConstants, saltValueList, bitPermutationRef, tableVariantList, saltLinkPairs, saltValuePairs, this.workspace, this.workspace.getClassProvider(), inheritance);
	}

	private void emitSingletonInit(
		ClassNode auxiliaryClass,
		ArrayList<AbstractInsnNode> instructions,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		AtomicInt additionalLocals,
		AtomicInt requiredStackSize,
		AtomicReference<TryCatchBlockInfo[]> tryCatchBlockInfoRef,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean java5OrNewer
	) {
		String internalName = auxiliaryClass.getInternalName();
		String fieldDescriptor = 'L' + internalName + ';';
		ClassInfoWriter classInfoWriter = constantPoolManager.getOrCreateClassInfo(internalName, typeWriters);
		instructions.add(new NewNode(classInfoWriter));
		instructions.add(InsnNode.of(DUP));
		MethodCallWriter constructorRef = constantPoolManager.getOrCreateMethodRef(internalName, "<init>", "()V", typeWriters, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorRef));
		FieldNode singletonField = (FieldNode)auxiliaryClass.findField("a", fieldDescriptor);
		FieldTypeWriter singletonFieldRef = constantPoolManager.getOrCreateFieldRef(singletonField, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, singletonFieldRef));
	}

	public SaltOrder getSaltOrder(int index) {
		return this.saltOrders.get(index);
	}

	public void setClassEncryptionKeys(Map<AbstractClassNode, Long> classEncryptionKeys) {
		this.classEncryptionKeys = classEncryptionKeys;
	}

	private List<ExceptionCallEntry> buildSimpleLongInitEntries(
		List<Object> saltValues, ClassNode simpleLongClass, ConstantPoolManager constantPoolManager, List<TypeWriter> cpEntries, AbstractWorkspace workspace, ClassProvider classProvider, boolean java5OrNewer
	) {
		ArrayList<ExceptionCallEntry> callEntries = new ArrayList<>();
		ExceptionCallEntry callEntry = new ExceptionCallEntry(30000);
		ArrayList<AbstractInsnNode> instructions = callEntry.getInstructions();
		callEntries.add(callEntry);
		FieldNode permutationField = (FieldNode)simpleLongClass.findField("e", "[I");
		FieldTypeWriter permutationFieldRef = constantPoolManager.getOrCreateFieldRef(permutationField, cpEntries);
		String listClassName;
		String listFieldDescriptor;
		String addMethodName;
		String addMethodDescriptor;
		if (java5OrNewer) {
			listClassName = "java/util/ArrayList";
			listFieldDescriptor = "Ljava/util/ArrayList;";
			addMethodName = "add";
			addMethodDescriptor = "(Ljava/lang/Object;)Z";
		} else {
			listClassName = "java/util/Vector";
			listFieldDescriptor = "Ljava/util/Vector;";
			addMethodName = "addElement";
			addMethodDescriptor = "(Ljava/lang/Object;)V";
		}

		FieldNode listFieldNode = (FieldNode)simpleLongClass.findField("f", listFieldDescriptor);
		FieldTypeWriter listField = constantPoolManager.getOrCreateFieldRef(listFieldNode, cpEntries);
		String ownerClassName = simpleLongClass.getInternalName();
		MethodNode constructorMethod = simpleLongClass.findMethodNode(new MethodSignature("<init>", "(J)V"));
		MethodCallWriter constructorRef = constantPoolManager.getOrCreateClassMethodRef(constructorMethod, cpEntries);
		MethodCallWriter addRef = constantPoolManager.getOrCreateMethodRef(listClassName, addMethodName, addMethodDescriptor, cpEntries, workspace, classProvider);
		MethodCallWriter cloneMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/Object", "clone", "()Ljava/lang/Object;", cpEntries, workspace, classProvider);
		int bytecodeSize = 0;
		int[] previousBitPermutation = null;
		int saltValueCount = saltValues.size();
		AtomicReference<LongNode> longConstantRef = new AtomicReference<>();

		for (int i = 0; i < saltValueCount; i++) {
			Object saltValue = saltValues.get(i);
			if (saltValue instanceof Long) {
				bytecodeSize += this.emitCreateListEntry((Long)saltValue, instructions, listField, constructorRef, addRef, ownerClassName, constantPoolManager, cpEntries, longConstantRef, java5OrNewer);
			} else if (saltValue instanceof long[]) {
				bytecodeSize += this.buildStringDecoderCode((long[])saltValue, ownerClassName, simpleLongClass, listField, callEntry, constantPoolManager, cpEntries, workspace, classProvider);
			} else {
				int[] bitPermutation = (int[])saltValue;
				boolean permutationChanged;
				if (previousBitPermutation == null) {
					previousBitPermutation = bitPermutation;
					permutationChanged = true;
				} else if (GlobalUtils.areIntArraysEqual(bitPermutation, previousBitPermutation)) {
					permutationChanged = false;
				} else {
					permutationChanged = true;
				}

				bytecodeSize += this.emitBitPermutationStore(simpleLongClass, permutationChanged, bitPermutation, instructions, permutationFieldRef, cloneMethodRef, constantPoolManager, cpEntries, workspace, classProvider);
			}

			if (bytecodeSize > 65035 && i < saltValueCount - 1) {
				instructions.add(InsnNode.of(RETURN));
				callEntry = new ExceptionCallEntry(30000);
				instructions = callEntry.getInstructions();
				callEntries.add(callEntry);
				bytecodeSize = 0;
			}
		}

		instructions.add(InsnNode.of(RETURN));
		instructions.trimToSize();
		return callEntries;
	}

	private void addClassInitDependency(ClassHierarchyNode prerequisiteClass, ClassHierarchyNode dependentClass, Map<ClassNode, ChangedMethod> changedMethodsByClass, String reason, boolean logErrors) {
		this.addInitializationDependency(prerequisiteClass, dependentClass, false, changedMethodsByClass, reason, logErrors);
	}

	public void putMpcClassTriplet(ClassNode classNode, Triplet mpcClassTriplet) {
		this.classToMpcClassTripletMap.put(classNode, mpcClassTriplet);
	}

	public void addStringEncryptionClass(ClassNode classNode) {
		this.stringEncryptionClasses.add(classNode);
	}

	public Long getOrCreateMethodEncryptionKey(MethodNode methodNode) {
		ClassNode classNode = methodNode.getMethodOwner();
		Long encryptionKey = null;
		if (this.classEncryptionKeys != null) {
			encryptionKey = this.classEncryptionKeys.get(classNode);
		}

		if (encryptionKey == null) {
			encryptionKey = !Options.useFixedParamSalt ? newRandomLong() : 255L;
		}

		this.methodEncryptionKeys.put(methodNode, encryptionKey);
		return encryptionKey;
	}

	private List<ExtraCallEntry> buildSaltPairEntries(
		List<Pair> saltLinkPairs,
		List<Pair> saltValuePairs,
		ClassNode fallbackClass,
		ClassNode decrypterInterface,
		ClassNode simpleLongClass,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> cpEntries,
		AtomicReference<LongNode> cachedLongMask,
		AtomicInt maxLocals,
		AtomicInt maxStack
	) {
		maxLocals.set(1);
		maxStack.set(2);
		ArrayList<ExtraCallEntry> callEntries = new ArrayList<>();
		ExtraCallEntry callEntry = new ExtraCallEntry(27000, "()V", false);
		ArrayList<AbstractInsnNode> instructions = callEntry.getInstructions();
		callEntries.add(callEntry);
		int bytecodeSize = 0;
		String decryptMethodDescriptor = "(J)L" + decrypterInterface.getInternalName() + ';';
		MethodNode decryptMethod = simpleLongClass.findMethodNode(new MethodSignature("c", decryptMethodDescriptor));
		MethodCallWriter decryptMethodRef = constantPoolManager.getOrCreateClassMethodRef(decryptMethod, cpEntries);
		String linkMethodDescriptor = "(L" + decrypterInterface.getInternalName() + ";)V";
		MethodNode linkMethod = decrypterInterface.findMethodNode(new MethodSignature("a", linkMethodDescriptor));
		InterfaceMethodCallWriter linkMethodRef = constantPoolManager.getOrCreateInterfaceMethodRef(linkMethod, cpEntries);
		int linkPairCount = saltLinkPairs.size();

		for (int linkPairIndex = 0; linkPairIndex < linkPairCount; linkPairIndex++) {
			Pair linkPair = saltLinkPairs.get(linkPairIndex);
			if (cpEntries.size() > 32000) {
				int linkKeyEncryptedSize = AbstractInsnNode.encryptLongConstant((Long)linkPair.getKey(), instructions, constantPoolManager, cpEntries, cachedLongMask);
				bytecodeSize += linkKeyEncryptedSize;
			} else {
				AbstractInsnNode linkKeyInstruction = AbstractInsnNode.ldcLong((Long)linkPair.getKey(), constantPoolManager, cpEntries);
				instructions.add(linkKeyInstruction);
				bytecodeSize += linkKeyInstruction.getBytecodeSize();
			}

			WideConstantPoolInsnNode linkDecryptInstruction = new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethodRef);
			bytecodeSize += linkDecryptInstruction.getBytecodeSize();
			instructions.add(linkDecryptInstruction);
			if (cpEntries.size() > 32000) {
				int linkValueEncryptedSize = AbstractInsnNode.encryptLongConstant((Long)linkPair.getValue(), instructions, constantPoolManager, cpEntries, cachedLongMask);
				bytecodeSize += linkValueEncryptedSize;
			} else {
				AbstractInsnNode linkValueInstruction = AbstractInsnNode.ldcLong((Long)linkPair.getValue(), constantPoolManager, cpEntries);
				instructions.add(linkValueInstruction);
				bytecodeSize += linkValueInstruction.getBytecodeSize();
			}

			linkDecryptInstruction = new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethodRef);
			bytecodeSize += linkDecryptInstruction.getBytecodeSize();
			instructions.add(linkDecryptInstruction);
			InvokeInterfaceNode linkInstruction = new InvokeInterfaceNode(linkMethodRef);
			bytecodeSize += linkInstruction.getBytecodeSize();
			instructions.add(linkInstruction);
			if (bytecodeSize > 65035) {
				instructions.add(InsnNode.of(RETURN));
				callEntry = new ExtraCallEntry(instructions.size() + 100, "()V", false);
				instructions = callEntry.getInstructions();
				callEntries.add(callEntry);
				bytecodeSize = 0;
			}
		}

		MethodNode addValueMethod = decrypterInterface.findMethodNode(new MethodSignature("b", "(J)V"));
		InterfaceMethodCallWriter addValueMethodRef = constantPoolManager.getOrCreateInterfaceMethodRef(addValueMethod, cpEntries);
		int valuePairCount = saltValuePairs.size();

		for (int valuePairIndex = 0; valuePairIndex < valuePairCount; valuePairIndex++) {
			Pair valuePair = saltValuePairs.get(valuePairIndex);
			if (cpEntries.size() > 32000) {
				int valueKeyEncryptedSize = AbstractInsnNode.encryptLongConstant((Long)valuePair.getKey(), instructions, constantPoolManager, cpEntries, cachedLongMask);
				bytecodeSize += valueKeyEncryptedSize;
			} else {
				AbstractInsnNode valueKeyInstruction = AbstractInsnNode.ldcLong((Long)valuePair.getKey(), constantPoolManager, cpEntries);
				bytecodeSize += valueKeyInstruction.getBytecodeSize();
				instructions.add(valueKeyInstruction);
			}

			WideConstantPoolInsnNode valueDecryptInstruction = new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethodRef);
			bytecodeSize += valueDecryptInstruction.getBytecodeSize();
			instructions.add(valueDecryptInstruction);
			if (cpEntries.size() > 32000) {
				int valueEncryptedSize = AbstractInsnNode.encryptLongConstant((Long)valuePair.getValue(), instructions, constantPoolManager, cpEntries, cachedLongMask);
				bytecodeSize += valueEncryptedSize;
			} else {
				AbstractInsnNode valueInstruction = AbstractInsnNode.ldcLong((Long)valuePair.getValue(), constantPoolManager, cpEntries);
				bytecodeSize += valueInstruction.getBytecodeSize();
				instructions.add(valueInstruction);
			}

			InvokeInterfaceNode addValueInstruction = new InvokeInterfaceNode(addValueMethodRef);
			bytecodeSize += addValueInstruction.getBytecodeSize();
			instructions.add(addValueInstruction);
			if (bytecodeSize > 65035 && valuePairIndex < valuePairCount - 1) {
				instructions.add(InsnNode.of(RETURN));
				callEntry = new ExtraCallEntry(instructions.size() + 100, "()V", false);
				instructions = callEntry.getInstructions();
				callEntries.add(callEntry);
				bytecodeSize = 0;
			}
		}

		instructions.add(InsnNode.of(RETURN));
		instructions.trimToSize();
		return callEntries;
	}

	public boolean shouldEncryptLongsInMethod(MethodNode methodNode) {
		if (methodNode == null) {
			return false;
		} else {
			ClassNode classNode = methodNode.getMethodOwner();
			return (classNode.isJava8OrNewer() || classNode.isJava1_4OrNewer() && !classNode.isInterface())
				&& this.isChangedMethod(methodNode)
				&& this.longEncryptionClasses.contains(classNode)
				&& Options.ZKM_LONG_ENCRYPT_DES
				&& GlobalUtils.isClassPresent("javax.crypto.Cipher");
		}
	}

	private List<ExtraCallEntry> buildSaltPermutationEntries(
		List<Object> saltPermutations,
		ClassNode fallbackClass,
		ClassNode decrypterInterface,
		ClassNode simpleLongClass,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> cpEntries,
		AtomicInt maxLocals,
		AtomicInt maxStack,
		AtomicReference<LongNode> cachedLongMask,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		maxLocals.set(1);
		maxStack.set(4);
		ArrayList<ExtraCallEntry> callEntries = new ArrayList<>();
		ExtraCallEntry callEntry = new ExtraCallEntry(44000, "()V", false);
		ArrayList<AbstractInsnNode> instructions = callEntry.getInstructions();
		callEntries.add(callEntry);
		int bytecodeSize = 0;
		String decryptMethodDescriptor = "(J)L" + decrypterInterface.getInternalName() + ';';
		MethodNode decryptMethod = simpleLongClass.findMethodNode(new MethodSignature("c", decryptMethodDescriptor));
		MethodCallWriter decryptMethodRef = constantPoolManager.getOrCreateClassMethodRef(decryptMethod, cpEntries);
		MethodNode getPermutationMethod = decrypterInterface.findMethodNode(new MethodSignature("b", "()[I"));
		InterfaceMethodCallWriter getPermutationMethodRef = constantPoolManager.getOrCreateInterfaceMethodRef(getPermutationMethod, cpEntries);
		int saltPermutationCount = saltPermutations.size();

		for (int permutationIndex = 0; permutationIndex < saltPermutationCount; permutationIndex++) {
			Object saltValue = saltPermutations.get(permutationIndex++);
			int[] permutation = (int[])saltPermutations.get(permutationIndex);
			if (cpEntries.size() > 32000) {
				int saltValueEncryptedSize = AbstractInsnNode.encryptLongConstant((Long)saltValue, instructions, constantPoolManager, cpEntries, cachedLongMask);
				bytecodeSize += saltValueEncryptedSize;
			} else {
				AbstractInsnNode saltValueInstruction = AbstractInsnNode.ldcLong((Long)saltValue, constantPoolManager, cpEntries);
				bytecodeSize += saltValueInstruction.getBytecodeSize();
				instructions.add(saltValueInstruction);
			}

			WideConstantPoolInsnNode decryptInstruction = new WideConstantPoolInsnNode(INVOKESTATIC, decryptMethodRef);
			bytecodeSize += decryptInstruction.getBytecodeSize();
			instructions.add(decryptInstruction);
			InvokeInterfaceNode getPermutationInstruction = new InvokeInterfaceNode(getPermutationMethodRef);
			bytecodeSize += getPermutationInstruction.getBytecodeSize();
			instructions.add(getPermutationInstruction);
			bytecodeSize++;
			instructions.add(InsnNode.of(DUP));

			for (int i = 0; i < 64; i++) {
				AbstractInsnNode intConstantInstruction = AbstractInsnNode.newInt(i);
				bytecodeSize += intConstantInstruction.getBytecodeSize();
				instructions.add(intConstantInstruction);
				intConstantInstruction = AbstractInsnNode.newInt(permutation[i]);
				bytecodeSize += intConstantInstruction.getBytecodeSize();
				instructions.add(intConstantInstruction);
				bytecodeSize++;
				instructions.add(InsnNode.of(IASTORE));
				if (i < 63) {
					bytecodeSize++;
					instructions.add(InsnNode.of(DUP));
				}
			}

			if (bytecodeSize > 65035 && permutationIndex < saltPermutationCount - 1) {
				instructions.add(InsnNode.of(RETURN));
				callEntry = new ExtraCallEntry(instructions.size() + 100, "()V", false);
				instructions = callEntry.getInstructions();
				callEntries.add(callEntry);
				bytecodeSize = 0;
			}
		}

		instructions.add(InsnNode.of(RETURN));
		instructions.trimToSize();
		return callEntries;
	}

	private void appendDecrypterInitCalls(
		ClassNode fallbackClass,
		ClassNode decrypterInterface,
		ClassNode simpleLongClass,
		LocalVariableTable localVarTable,
		List<MethodNode> initMethods,
		ArrayList<AbstractInsnNode> instructions,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> cpEntries,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		for (MethodNode initMethod : initMethods) {
			MethodCallWriter initMethodRef = constantPoolManager.getOrCreateClassMethodRef(initMethod, cpEntries);
			instructions.add(AbstractInsnNode.loadObject(0, localVarTable, 5));
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, initMethodRef));
		}

		String decrypterInitDescriptor = "(L" + fallbackClass.getInternalName() + ";)V";
		MethodNode decrypterInitMethod = simpleLongClass.findMethodNode(new MethodSignature("b", decrypterInitDescriptor));
		MethodCallWriter decrypterInitMethodRef = constantPoolManager.getOrCreateClassMethodRef(decrypterInitMethod, cpEntries);
		instructions.add(AbstractInsnNode.loadObject(0, localVarTable, 5));
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, decrypterInitMethodRef));
	}

	private Triplet validateAndResolveClassNames(Triplet classNames, ReadOnlyMap<String, String> classNameToInternalNameMap, Set<String> allowedPackages, Map<String, Triplet> tripletsByClassName) {
		Triplet resolvedClassNames = classNames;

		for (int classNameIndex = 0; classNameIndex < 3; classNameIndex++) {
			String validatedClassName = InjectedClassManager.validateUserConfiguredName(
				"MethodParameterChangeClasses:", (String)classNames.getByIndex(classNameIndex), null, null, "Method Parameter List Changing", allowedPackages, this.changeLogContext, classNameToInternalNameMap
			);
			if (validatedClassName == null) {
				return null;
			}

			classNames.setByIndex(validatedClassName, classNameIndex);
		}

		String firstClassName = (String)classNames.getFirst();
		if (tripletsByClassName.containsKey(firstClassName)) {
			resolvedClassNames = tripletsByClassName.get(firstClassName);
		} else {
			tripletsByClassName.put(firstClassName, classNames);
		}

		return resolvedClassNames;
	}

	public List<AbstractInsnNode> buildParamDecryptionCall(
		ClassNode targetClass, Long methodSaltKey, Long orderSaltKey, Long valueKey, AtomicInt codeSize, List<TypeWriter> cpEntries, ConstantPoolManager constantPoolManager, AbstractWorkspace workspace, ClassProvider classProvider
	) {
		ArrayList<AbstractInsnNode> decryptionInstructions = new ArrayList<>();
		Triplet mpcClassTriplet = this.getMpcClassTriplet(targetClass);
		int estimatedCodeSize = 0;
		Triplet resolvedMethodTriplet = this.getResolvedMethodTriplet(mpcClassTriplet);
		MethodNode lockObjectMethod = (MethodNode)resolvedMethodTriplet.getFirst();
		MethodNode staticDecrypterMethod = (MethodNode)resolvedMethodTriplet.getSecond();
		MethodNode interfaceDecrypterMethod = (MethodNode)resolvedMethodTriplet.getThird();
		if (Options.ZKM_METHOD_PARAM_SYNCH) {
			MethodCallWriter monitorEnterLockRef = constantPoolManager.getOrCreateClassMethodRef(lockObjectMethod, cpEntries);
			decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, monitorEnterLockRef));
			decryptionInstructions.add(new MonitorEnterNode());
			estimatedCodeSize += 4;
		}

		MethodCallWriter staticDecrypterMethodRef = constantPoolManager.getOrCreateClassMethodRef(staticDecrypterMethod, cpEntries);
		InterfaceMethodCallWriter interfaceDecrypterMethodRef = constantPoolManager.getOrCreateInterfaceMethodRef(interfaceDecrypterMethod, cpEntries);
		decryptionInstructions.add(AbstractInsnNode.ldcLong(methodSaltKey, constantPoolManager, cpEntries));
		decryptionInstructions.add(AbstractInsnNode.ldcLong(orderSaltKey, constantPoolManager, cpEntries));
		estimatedCodeSize += 6;
		if (targetClass.isJava7OrNewer() && !Options.dontUseMethodHandlesLookup) {
			MethodCallWriter lookupMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;", cpEntries, workspace, classProvider);
			decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, lookupMethodRef));
			MethodCallWriter lookupClassMethodRef = constantPoolManager.getOrCreateMethodRef("java/lang/invoke/MethodHandles$Lookup", "lookupClass", "()Ljava/lang/Class;", cpEntries, workspace, classProvider);
			decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, lookupClassMethodRef));
			estimatedCodeSize += 6;
		} else {
			decryptionInstructions.add(InsnNode.of(ACONST_NULL));
			estimatedCodeSize++;
		}

		decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, staticDecrypterMethodRef));
		decryptionInstructions.add(AbstractInsnNode.ldcLong(valueKey, constantPoolManager, cpEntries));
		decryptionInstructions.add(new InvokeInterfaceNode(interfaceDecrypterMethodRef));
		estimatedCodeSize += 11;
		if (Options.ZKM_METHOD_PARAM_SYNCH) {
			MethodCallWriter monitorExitLockRef = constantPoolManager.getOrCreateClassMethodRef(lockObjectMethod, cpEntries);
			decryptionInstructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, monitorExitLockRef));
			decryptionInstructions.add(new MonitorExitNode());
			estimatedCodeSize += 4;
		}

		codeSize.set(estimatedCodeSize);
		return decryptionInstructions;
	}

	private void buildDecrypterClinit(
		ClassNode simpleLongClass,
		LocalVariableTable localVarTable,
		List<MethodNode> initMethods,
		int saltBitWidth,
		int sortDepthLimit,
		int tableCloneInterval,
		List<AbstractInsnNode> instructions,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> cpEntries,
		AtomicInt extraLocals,
		AtomicInt maxStack,
		AtomicReference<TryCatchBlockInfo[]> tryCatchBlocks,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		boolean java5OrNewer
	) {
		tryCatchBlocks.setNull();
		extraLocals.set(3);
		maxStack.set(4);
		LabelNode loopStartLabel = new LabelNode(true, 1);
		LabelNode loopEndLabel = new LabelNode(true, 1);
		instructions.add(AbstractInsnNode.newInt(saltBitWidth));
		FieldNode saltBitWidthField = (FieldNode)simpleLongClass.findField("h", "I");
		FieldTypeWriter saltBitWidthFieldRef = constantPoolManager.getOrCreateFieldRef(saltBitWidthField, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, saltBitWidthFieldRef));
		instructions.add(AbstractInsnNode.newInt(sortDepthLimit));
		FieldNode sortDepthLimitField = (FieldNode)simpleLongClass.findField("n", "I");
		FieldTypeWriter sortDepthLimitFieldRef = constantPoolManager.getOrCreateFieldRef(sortDepthLimitField, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, sortDepthLimitFieldRef));
		instructions.add(AbstractInsnNode.newInt(tableCloneInterval));
		FieldNode tableCloneIntervalField = (FieldNode)simpleLongClass.findField("l", "I");
		FieldTypeWriter tableCloneIntervalFieldRef = constantPoolManager.getOrCreateFieldRef(tableCloneIntervalField, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, tableCloneIntervalFieldRef));
		instructions.add(AbstractInsnNode.newInt(64));
		instructions.add(new NewArrayNode(11));
		FieldNode bitMasksField = (FieldNode)simpleLongClass.findField("k", "[J");
		FieldTypeWriter bitMasksFieldRef = constantPoolManager.getOrCreateFieldRef(bitMasksField, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, bitMasksFieldRef));
		instructions.add(InsnNode.of(LCONST_1));
		instructions.add(AbstractInsnNode.storeLong(0, localVarTable, 5));
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(AbstractInsnNode.storeInt(2, localVarTable, 5));
		instructions.add(loopStartLabel);
		instructions.add(AbstractInsnNode.loadInt(2, localVarTable, 5));
		instructions.add(AbstractInsnNode.newInt(64));
		instructions.add(new JumpInsnNode(IF_ICMPGE, loopEndLabel));
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, bitMasksFieldRef));
		instructions.add(AbstractInsnNode.loadInt(2, localVarTable, 5));
		instructions.add(InsnNode.loadLong(0, localVarTable, 5));
		instructions.add(InsnNode.of(LASTORE));
		instructions.add(InsnNode.loadLong(0, localVarTable, 5));
		instructions.add(InsnNode.of(ICONST_1));
		instructions.add(InsnNode.of(LSHL));
		instructions.add(AbstractInsnNode.storeLong(0, localVarTable, 5));
		instructions.add(AbstractInsnNode.IINC(2, 1, localVarTable, 5));
		instructions.add(new GotoNode(loopStartLabel));
		instructions.add(loopEndLabel);
		ClassInfoWriter objectClassInfo = constantPoolManager.getOrCreateClassInfo("java/lang/Object", cpEntries);
		MethodCallWriter objectCtorRef = constantPoolManager.getOrCreateMethodRef("java/lang/Object", "<init>", "()V", cpEntries, workspace, classProvider);
		instructions.add(new NewNode(objectClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, objectCtorRef));
		FieldNode lockObjectField = (FieldNode)simpleLongClass.findField("o", "Ljava/lang/Object;");
		FieldTypeWriter lockObjectFieldRef = constantPoolManager.getOrCreateFieldRef(lockObjectField, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, lockObjectFieldRef));
		ClassInfoWriter vectorClassInfo = constantPoolManager.getOrCreateClassInfo("java/util/Vector", cpEntries);
		MethodCallWriter vectorCtorRef = constantPoolManager.getOrCreateMethodRef("java/util/Vector", "<init>", "()V", cpEntries, workspace, classProvider);
		instructions.add(new NewNode(vectorClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, vectorCtorRef));
		FieldNode vectorField = (FieldNode)simpleLongClass.findField("m", "Ljava/util/Vector;");
		FieldTypeWriter vectorFieldRef = constantPoolManager.getOrCreateFieldRef(vectorField, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, vectorFieldRef));
		String collectionInternalName;
		String collectionDescriptor;
		if (java5OrNewer) {
			collectionInternalName = "java/util/ArrayList";
			collectionDescriptor = "Ljava/util/ArrayList;";
		} else {
			collectionInternalName = "java/util/Vector";
			collectionDescriptor = "Ljava/util/Vector;";
		}

		ClassInfoWriter collectionClassInfo = constantPoolManager.getOrCreateClassInfo(collectionInternalName, cpEntries);
		MethodCallWriter collectionCtorRef = constantPoolManager.getOrCreateMethodRef(collectionInternalName, "<init>", "()V", cpEntries, workspace, classProvider);
		instructions.add(new NewNode(collectionClassInfo));
		instructions.add(InsnNode.of(DUP));
		instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, collectionCtorRef));
		FieldNode collectionField = (FieldNode)simpleLongClass.findField("f", collectionDescriptor);
		FieldTypeWriter collectionFieldRef = constantPoolManager.getOrCreateFieldRef(collectionField, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, collectionFieldRef));

		for (MethodNode initMethod : initMethods) {
			MethodCallWriter initMethodRef = constantPoolManager.getOrCreateClassMethodRef(initMethod, cpEntries);
			instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, initMethodRef));
		}

		FieldNode collectionSizeField = (FieldNode)simpleLongClass.findField("i", "I");
		FieldTypeWriter collectionSizeFieldRef = constantPoolManager.getOrCreateFieldRef(collectionSizeField, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, collectionFieldRef));
		MethodCallWriter collectionSizeMethodRef = constantPoolManager.getOrCreateMethodRef(collectionInternalName, "size", "()I", cpEntries, workspace, classProvider);
		instructions.add(new WideConstantPoolInsnNode(INVOKEVIRTUAL, collectionSizeMethodRef));
		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, collectionSizeFieldRef));
		MethodNode extraCallInitMethod = simpleLongClass.findMethodNode(new MethodSignature("c", "()V"));
		MethodCallWriter extraCallInitMethodRef = constantPoolManager.getOrCreateClassMethodRef(extraCallInitMethod, cpEntries);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, extraCallInitMethodRef));
	}

	private boolean isMainMethod(AbstractMethodNode method) {
		MethodSignature methodSignature = method.getMethodSignature();
		return method.isPublic() && method.isStatic() && methodSignature.equals(ApplicationEntryPoints.MAIN_METHOD_SIGNATURE);
	}

	public boolean shouldEncryptIntegersInMethod(MethodNode methodNode) {
		if (methodNode == null) {
			return false;
		} else {
			ClassNode methodOwner = methodNode.getMethodOwner();
			return (methodOwner.isJava8OrNewer() || methodOwner.isJava1_4OrNewer() && !methodOwner.isInterface())
				&& this.isChangedMethod(methodNode)
				&& this.integerEncryptionClasses.contains(methodOwner)
				&& Options.ZKM_INTEGER_ENCRYPT_DES
				&& GlobalUtils.isClassPresent("javax.crypto.Cipher");
		}
	}

	public boolean isChangedMethod(MethodNode methodNode) {
		return this.changedMethodByMethod != null ? this.changedMethodByMethod.get(methodNode) != null : false;
	}

	static {
		long bitMask = 1L;

		for (int i = 0; i < 64; i++) {
			bitMasks[i] = bitMask;
			bitMask <<= 1;
		}
	}

	public static class MethodParamObfuscationContext implements AuxiliaryWiringStrategy {
		private final Triplet classNames;
		private Triplet generatedClasses;


		public MethodParamObfuscationContext(Triplet var1) {
			this.classNames = var1;
		}

		public int getDecrypterRoleIndex(String var1) {
			if (((String)this.classNames.getFirst()).equals(var1)) {
				return 0;
			} else {
				return ((String)this.classNames.getSecond()).equals(var1) ? 1 : 2;
			}
		}

		public void registerClassNode(ClassNode var1, int var2) {
			switch (var2) {
				case 0:
					this.generatedClasses = new Triplet(var1, null, null);
					break;
				case 1:
					this.generatedClasses.setSecond(var1);
					break;
				case 2:
					this.generatedClasses.setThird(var1);
			}
		}

		@Override
		public boolean shouldAutoRegister() {
			return false;
		}

		public String getILongDecrypterClassName() {
			return (String)this.classNames.getFirst();
		}

		public String getSimpleLongDecrypterClassName() {
			return (String)this.classNames.getThird();
		}

		public String getFallbackLongDecrypterClassName() {
			return (String)this.classNames.getSecond();
		}
	}
}

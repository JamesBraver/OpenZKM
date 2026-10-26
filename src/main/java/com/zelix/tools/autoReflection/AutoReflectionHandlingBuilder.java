package com.zelix.tools.autoReflection;

import com.zelix.enums.MixCaseType;
import com.zelix.event.EventRegistry;
import com.zelix.exceptions.AutoReflectionFailedException;
import com.zelix.exceptions.HashCollisionException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.code.MethodInvocation;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.framework.writer.reflect.EntryType;
import com.zelix.framework.writer.reflect.ReflectionRewriteSnippet;
import com.zelix.framework.writer.reflect.RewriteScope;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.tools.AuxiliaryClassGenerator;
import com.zelix.tools.AuxiliaryWiringStrategy;
import com.zelix.tools.InjectedClassManager;
import com.zelix.tools.parameterChanger.dataset.ParameterRotateEntry;
import com.zelix.tools.renamer.naming.AlphaNameGenerator;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.collections.UnmodifiableMultiMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.Workspace;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class AutoReflectionHandlingBuilder implements AuxiliaryClassGenerator {
	private static final String[] commonFieldPrefixes = new String[]{"is"};
	private static final String[] commonMethodPrefixes = new String[]{"get", "set", "is"};
	private static final MethodSignature[] legacyMethodSignatures = new MethodSignature[]{
		new MethodSignature("a(Ljava/util/Hashtable;)V"),
		new MethodSignature("b(Ljava/util/Hashtable;)V"),
		new MethodSignature("c(Ljava/util/Hashtable;)V"),
		new MethodSignature("d(Ljava/util/Hashtable;)V"),
		new MethodSignature("e(Ljava/util/Hashtable;)V"),
		new MethodSignature("f(Ljava/util/Hashtable;)V"),
		new MethodSignature("g(Ljava/util/Hashtable;)V"),
		new MethodSignature("h(Ljava/util/Hashtable;)V"),
		new MethodSignature("i(Ljava/util/Hashtable;)V"),
		new MethodSignature("j(Ljava/util/Hashtable;)V"),
		new MethodSignature("k(Ljava/util/Hashtable;)V")
	};
	private static final MethodSignature[] modernMethodSignatures = new MethodSignature[]{
		new MethodSignature("a(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("b(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("c(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("d(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("e(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("f(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("g(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("h(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("i(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("j(Ljava/util/concurrent/ConcurrentHashMap;)V"),
		new MethodSignature("k(Ljava/util/concurrent/ConcurrentHashMap;)V")
	};
	public static final int minNameLength;
	private static final int minHashEntryCount;
	private final AutoReflectionExcludes autoReflectionExcludes;
	private final AbstractWorkspace workspace;
	private final String hashAlgorithm;
	private final boolean randomizeObfuscation;
	private final List<String> userConfiguredNames;

	public AutoReflectionHandlingBuilder(AutoReflectionExcludes autoReflectionExcludes, List<String> userConfiguredNames, AbstractWorkspace workspace, String hashAlgorithm, boolean randomizeObfuscation) {
		this.autoReflectionExcludes = autoReflectionExcludes;
		this.workspace = workspace;
		this.hashAlgorithm = hashAlgorithm;
		this.randomizeObfuscation = randomizeObfuscation;
		this.userConfiguredNames = userConfiguredNames;
		DebugUtils.assertTrue(true, new String[]{"MAX_RADIX is too small : 36 < 36"});
	}

	private static String computeNameHash(String name, MessageDigest messageDigest) throws UnsupportedEncodingException {
		messageDigest.reset();
		messageDigest.update(name.getBytes("UTF-8"));
		return new BigInteger(messageDigest.digest()).toString(36);
	}

	@Override
	public ClassNode generate(String className, int majorVersion, int minorVersion, AuxiliaryWiringStrategy wiringStrategy) {
		int effectiveMajorVersion = majorVersion;
		if (Options.ZKM_AUTOREFLECTION_MAJOR_VERSION != null && Options.ZKM_AUTOREFLECTION_MINOR_VERSION != null) {
			try {
				effectiveMajorVersion = Integer.parseInt(Options.ZKM_AUTOREFLECTION_MAJOR_VERSION);
			} catch (NumberFormatException ignoredMajorVersionParse) {
			}
		}

		int effectiveMinorVersion = minorVersion;
		if (Options.ZKM_AUTOREFLECTION_MAJOR_VERSION != null && Options.ZKM_AUTOREFLECTION_MINOR_VERSION != null) {
			try {
				effectiveMinorVersion = Integer.parseInt(Options.ZKM_AUTOREFLECTION_MINOR_VERSION);
			} catch (NumberFormatException ignoredMinorVersionParse) {
			}
		}

		try {
			byte[] classBytes;
			if (effectiveMajorVersion >= 49) {
				classBytes = ReflectionRewriteSnippet.MODERN_CLASS_TEMPLATE;
			} else {
				classBytes = ReflectionRewriteSnippet.LEGACY_CLASS_TEMPLATE;
			}

			ByteDataBuffer classBytesBuffer = ByteDataBuffer.fromBytes(classBytes, false);
			ClasspathResource classpathResource = ClasspathResource.createSynthetic("AutoReflection." + className);
			AtomicReference<String> progressMessage = new AtomicReference<>();
			EventRegistry eventRegistry = new EventRegistry();
			PrintWriter logWriter = new PrintWriter(new StringWriter());
			PrintWriter unusedWriter = new PrintWriter(new StringWriter());
			ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes = new ThreeLevelMultiMap<>();
			ClassNode classNode = new ClassNode(classBytesBuffer, classpathResource, progressMessage, eventRegistry, logWriter, unusedWriter, unknownAttributes, 9);
			classNode.setAllMethodGenerationTypes(9);
			if (!this.hashAlgorithm.equals("SHA-512")) {
				classNode.updateUtf8ConstantValue("SHA-512", this.hashAlgorithm);
			}

			classNode.renameClassInternal(className, GlobalUtils.newHashMap(13));
			classNode.setMajorVersion(effectiveMajorVersion);
			classNode.setMinorVersion(effectiveMinorVersion);
			return classNode;
		} catch (IOException ioException) {
			throw new AutoReflectionFailedException("AutoReflection (A) : " + ioException.getMessage(), ioException);
		} catch (ProcessingException processingException) {
			throw new AutoReflectionFailedException("AutoReflection (B) : " + processingException.getMessage(), processingException);
		}
	}

	private void addMethodMappings(
		DualKeyMap<String, MethodSignature, MethodSignature> candidateMethodMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> methodMappings,
		boolean publicOnly,
		String nameFilter,
		ReflectionType reflectionType,
		Map<String, String> inverseClassMappings,
		ScriptContext scriptContext
	) {
		Enumeration<String> classNameEnumeration = candidateMethodMappings.getGroupKeys();

		while (classNameEnumeration.hasMoreElements()) {
			String className = classNameEnumeration.nextElement();
			ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(className);
			Map<MethodSignature, MethodSignature> classCandidateMethodMappings = candidateMethodMappings.getGroup(className);
			if (classCandidateMethodMappings != null) {
				for (Entry<MethodSignature, MethodSignature> signatureEntry : classCandidateMethodMappings.entrySet()) {
					MethodSignature originalSignature = signatureEntry.getKey();
					MethodSignature renamedSignature = signatureEntry.getValue();
					MethodNode methodNode = classNode.findMethodNode(renamedSignature);
					if (!renamedSignature.getName().equals(originalSignature.getName())
						&& (!publicOnly || methodNode.isPublic())
						&& (nameFilter == null || originalSignature.getName().equals(nameFilter))
						&& (reflectionType == ReflectionType.BOTH || reflectionType == ReflectionType.INSTANCE && !methodNode.isStatic() || reflectionType == ReflectionType.STATIC && methodNode.isStatic())) {
						methodMappings.put(className, originalSignature, renamedSignature);
						this.warnWeakMethodNameHash(className, originalSignature, renamedSignature, inverseClassMappings, scriptContext);
					}
				}
			}
		}
	}

	private String createRandomHashKey(Random random, Set<String> usedKeys) {
		String generatedName;
		do {
			char fillChar = ReflectionRewriteSnippet.NAME_FILL_CHARS[random.nextInt(ReflectionRewriteSnippet.NAME_FILL_CHARS.length)];
			String randomText = Long.toString(random.nextLong(), 36);
			int justification = random.nextBoolean() ? 82 : 76;
			int targetLength = Math.max(17, randomText.length() + 1);
			char[] nameChars = StringUtils.justificateString(randomText, justification, targetLength, fillChar).toCharArray();
			GlobalUtils.shuffleWithFixedSeed(nameChars);
			generatedName = new String(nameChars);
		} while (!usedKeys.add(generatedName));

		return generatedName;
	}

	private void addSubclassFieldMappings(
		boolean includeProtected,
		boolean includeDefaultAccess,
		String packageName,
		boolean includePrivate,
		String nameFilter,
		ReflectionType reflectionType,
		String className,
		DualKeyMap<String, FieldSignature, FieldSignature> candidateFieldMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings,
		Map<String, String> inverseClassMappings,
		AbstractWorkspace workspace,
		ScriptContext scriptContext
	) {
		this.addClassFieldMappings(includeProtected, includeDefaultAccess, packageName, includePrivate, nameFilter, reflectionType, className, candidateFieldMappings, fieldMappings, inverseClassMappings, scriptContext);

		for (String internalClassName : inverseClassMappings.keySet()) {
			if (workspace.isClassInstanceOf(internalClassName, className)) {
				this.addClassFieldMappings(includeProtected, includeDefaultAccess, AbstractClassNode.getPackageName(internalClassName), includePrivate, nameFilter, reflectionType, internalClassName, candidateFieldMappings, fieldMappings, inverseClassMappings, scriptContext);
			}
		}
	}

	private Map<String, String> buildFieldLookupKeys(String separator, DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings) {
		HashMap<String, String> fieldLookupKeys = GlobalUtils.newHashMap();
		Enumeration<String> classNameEnumeration = fieldMappings.getGroupKeys();

		while (classNameEnumeration.hasMoreElements()) {
			String className = classNameEnumeration.nextElement();

			for (Entry<FieldSignature, FieldSignature> signatureEntry : fieldMappings.getGroup(className).entrySet()) {
				FieldSignature originalSignature = signatureEntry.getKey();
				FieldSignature renamedSignature = signatureEntry.getValue();
				StringBuilder lookupKeyBuilder = new StringBuilder();
				lookupKeyBuilder.append(className);
				lookupKeyBuilder.append(separator);
				lookupKeyBuilder.append(originalSignature.getName());
				fieldLookupKeys.put(lookupKeyBuilder.toString(), renamedSignature.getName());
			}
		}

		return fieldLookupKeys;
	}

	public int buildLookupMappings(
		Map<String, String> classMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> methodMappings,
		Set<String> allClassNames,
		ClassNode dataClass,
		String hashAlgorithm,
		ParameterRotateEntry parameterRotateEntry,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		ScriptContext scriptContext
	) throws UnsupportedEncodingException {
		boolean java5OrNewer = dataClass.isJava5OrNewer();
		ConstantPoolManager constantPoolManager = dataClass.getConstantPoolManager();

		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(hashAlgorithm);
		} catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			throw new ProcessingException(noSuchAlgorithmException.getMessage(), noSuchAlgorithmException);
		}

		int lookupMappingCount = classMappings.size() + fieldMappings.getTotalEntryCount() + methodMappings.getTotalEntryCount();
		ArrayList<AbstractInsnNode[]> lookupInstructions = new ArrayList<>(GlobalUtils.getInitialCapacity(lookupMappingCount));
		MethodSignature[] methodSignatures;
		if (java5OrNewer) {
			methodSignatures = modernMethodSignatures;
		} else {
			methodSignatures = legacyMethodSignatures;
		}

		ArrayList<MethodNode> methodNodes = new ArrayList<>(methodSignatures.length);

		for (MethodSignature methodSignature : methodSignatures) {
			MethodNode resolvedMethodNode = dataClass.findMethodNode(methodSignature);
			methodNodes.add(resolvedMethodNode);
		}

		Random random = GlobalUtils.newRandom(31);
		int suffixCharIndex = -1;

		while (++suffixCharIndex <= ReflectionRewriteSnippet.NAME_SUFFIX_CHARS.length) {
			String separator = String.valueOf(ReflectionRewriteSnippet.NAME_SUFFIX_CHARS[suffixCharIndex]);
			Map<String, String> classLookupKeys = this.buildClassLookupKeys(separator, classMappings);
			Map<String, String> fieldLookupKeys = this.buildFieldLookupKeys(separator, fieldMappings);
			Map<String, String> methodLookupKeys = this.buildMethodLookupKeys(separator, methodMappings);
			boolean collisionDetected = false;
			ArrayList<TypeWriter> cpEntries = new ArrayList<>();
			HashSet<String> usedHashes = GlobalUtils.newHashSet();
			MethodCallWriter methodCallWriter = constantPoolManager.getOrCreateMethodRef(
				dataClass.getInternalName(),
				"a",
				java5OrNewer ? "(Ljava/util/concurrent/ConcurrentHashMap;Ljava/lang/String;)V" : "(Ljava/util/Hashtable;Ljava/lang/String;)V",
				cpEntries,
				workspace,
				classProvider
			);
			HashSet<Entry<String, String>> lookupMappings = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(lookupMappingCount));
			lookupMappings.addAll(classLookupKeys.entrySet());
			lookupMappings.addAll(fieldLookupKeys.entrySet());
			lookupMappings.addAll(methodLookupKeys.entrySet());
			if (lookupMappings.size() < minHashEntryCount) {
				this.padLookupMappings(lookupMappings, classMappings, allClassNames, classLookupKeys, fieldLookupKeys, methodLookupKeys);
			}

			ArrayList<Entry<String, String>> shuffledLookupMappings = new ArrayList<>(lookupMappings);
			GlobalUtils.shuffle(shuffledLookupMappings, random);
			ArrayList<String> packedLookupStrings = new ArrayList<>();

			label115: {
				try {
					this.buildLookupStrings(shuffledLookupMappings, packedLookupStrings, usedHashes, messageDigest);
				} catch (HashCollisionException ignored) {
					collisionDetected = true;
					break label115;
				}

				this.addLookupInstructions(lookupInstructions, packedLookupStrings, methodCallWriter, constantPoolManager, cpEntries);
				if (allClassNames != null) {
					for (String className : allClassNames) {
						if (!usedHashes.add(computeNameHash(className, messageDigest))) {
							collisionDetected = true;
						}
					}
				}
			}

			if (!collisionDetected) {
				dataClass.updateUtf8ConstantValue(String.valueOf('~'), separator);
				int lookupInstructionCount = lookupInstructions.size();
				if (lookupInstructionCount <= methodNodes.size() * 5950 && (Options.analyzeReferenceDataClass || cpEntries.size() <= 65000)) {
					if (parameterRotateEntry != null) {
						AbstractClassNode[] classNodeArray = new AbstractClassNode[]{dataClass};
						Inheritance inheritance = new Inheritance(classNodeArray, classProvider);
						MethodNode staticInitializerNode = dataClass.findMethodNode(MethodSignature.STATIC_INITIALIZER);
						parameterRotateEntry.initChangedMethod(staticInitializerNode, 0);
						ArrayList<AbstractInsnNode> rotatedInstructions = new ArrayList<>();
						rotatedInstructions.addAll(parameterRotateEntry.buildDecryptionPrologue(staticInitializerNode, cpEntries, inheritance, 9));
						staticInitializerNode.insertInstructionsAtOffsetSimple(rotatedInstructions, 0, 0, 0, "Auto Reflection Handling");
					}

					constantPoolManager.appendEntriesToPool(cpEntries);
					GlobalUtils.shuffle(lookupInstructions, random);
					int startIndex = 0;
					int instructionCount = 0;

					for (MethodNode methodNode : methodNodes) {
						ArrayList<AbstractInsnNode> instructionBuffer = new ArrayList<>(17850);
						LocalVariableTable localVariableTable = methodNode.getLocalVariableTable();
						instructionBuffer.add(AbstractInsnNode.loadObject(0, localVariableTable, 9));
						int chunkSize = Math.min(5950, lookupInstructionCount - startIndex);
						int endIndex = startIndex + chunkSize;

						for (int lookupInstructionIndex = startIndex; lookupInstructionIndex < endIndex; lookupInstructionIndex++) {
							for (AbstractInsnNode insnNode : lookupInstructions.get(lookupInstructionIndex)) {
								instructionBuffer.add(insnNode);
								instructionCount++;
							}
						}

						startIndex = endIndex;
						instructionBuffer.add(InsnNode.of(POP));
						methodNode.insertInstructionsAtOffsetSimple(instructionBuffer, 0, 7, 0, "Auto Reflection Handling");
					}

					return lookupMappingCount;
				}

				scriptContext.logSpecialError(
					"Auto Reflection Handling : Too many class, method and/or field name lookup mappings. : "
						+ lookupInstructionCount
						+ " : "
						+ cpEntries.size()
						+ " : Consider using ZKM Script 'accessedByReflection' statement to specify only those classes, fields and methods which are actually accessed by Reflection and for which the Reflection access has not been resolved by "
						+ CopyrightConstants.NAME
						+ "."
				);
				return 0;
			}
		}

		throw new ProcessingException("Unable to automatically handle Reflection calls after " + ReflectionRewriteSnippet.NAME_SUFFIX_CHARS.length + " attempts.");
	}

	private void addLookupInstructions(List<AbstractInsnNode[]> lookupInstructions, List<String> packedLookupStrings, MethodCallWriter methodCallWriter, ConstantPoolManager constantPoolManager, List<TypeWriter> cpEntries) {
		for (String packedLookupString : packedLookupStrings) {
			AbstractInsnNode[] lookupInstructionArray = this.createLookupInstructions(packedLookupString, methodCallWriter, constantPoolManager, cpEntries);
			lookupInstructions.add(lookupInstructionArray);
		}
	}

	private void collectCandidateMappings(
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyMap<String, String> internalToBinaryNameMap,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldRegistryView,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodRegistryView,
		Map<String, String> classMappings,
		Map<String, String> inverseClassMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> candidateFieldMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> candidateMethodMappings
	) {
		for (Entry<String, String> classMappingEntry : classNameToInternalNameMap.entrySet()) {
			String className = classMappingEntry.getKey();
			String internalClassName = classMappingEntry.getValue();
			if (this.autoReflectionExcludes == null || this.autoReflectionExcludes.isClassNameExcluded(internalClassName)) {
				classMappings.put(className, internalClassName);
				inverseClassMappings.put(internalClassName, className);
			}
		}

		Enumeration<AbstractClassNode> fieldClassNodeEnumeration = fieldRegistryView.getGroupKeys();

		while (fieldClassNodeEnumeration.hasMoreElements()) {
			AbstractClassNode fieldClassNode = fieldClassNodeEnumeration.nextElement();
			String fieldClassInternalName = fieldClassNode.getInternalName();

			for (Entry<FieldSignature, FieldSignature> fieldSignatureEntry : fieldRegistryView.getReadOnlyGroup(fieldClassNode).entrySet()) {
				FieldSignature originalFieldSignature = fieldSignatureEntry.getKey();
				FieldSignature renamedFieldSignature = fieldSignatureEntry.getValue();
				FieldNode fieldNode = this.workspace.getFieldByNameAndDescriptor(GlobalUtils.binaryToInternalName(fieldClassInternalName), renamedFieldSignature.getName(), renamedFieldSignature.getDescriptor());
				DebugUtils.assertNotNullObject(fieldNode, new Object[]{fieldClassInternalName, originalFieldSignature, renamedFieldSignature});
				if (this.autoReflectionExcludes == null || this.autoReflectionExcludes.isFieldExcluded(fieldNode)) {
					candidateFieldMappings.put(fieldClassInternalName, originalFieldSignature, renamedFieldSignature);
				}
			}
		}

		Enumeration<AbstractClassNode> methodClassNodeEnumeration = methodRegistryView.getGroupKeys();

		while (methodClassNodeEnumeration.hasMoreElements()) {
			AbstractClassNode methodClassNode = methodClassNodeEnumeration.nextElement();
			String methodClassInternalName = methodClassNode.getInternalName();
			if (methodClassNode.isWorkspaceNode()) {
				for (Entry<MethodSignature, MethodSignature> methodSignatureEntry : methodRegistryView.getReadOnlyGroup(methodClassNode).entrySet()) {
					MethodSignature originalMethodSignature = methodSignatureEntry.getKey();
					MethodSignature renamedMethodSignature = methodSignatureEntry.getValue();
					MethodNode methodNode = this.workspace.getMethodBySignature((ClassNode)methodClassNode, renamedMethodSignature);
					DebugUtils.assertNotNullObject(methodNode, new Object[]{methodClassNode, methodClassInternalName, originalMethodSignature, renamedMethodSignature});
					if (this.autoReflectionExcludes == null || this.autoReflectionExcludes.isMethodExcluded(methodNode)) {
						candidateMethodMappings.put(methodClassInternalName, originalMethodSignature, renamedMethodSignature);
					}
				}
			}
		}
	}

	private String xorEncodeName(String lookupKey, String mappedName, MessageDigest messageDigest) throws UnsupportedEncodingException {
		messageDigest.reset();
		byte[] hashInput = (lookupKey + messageDigest.getAlgorithm()).getBytes("UTF-8");
		messageDigest.update(hashInput);
		byte[] hashBytes = messageDigest.digest();
		char[] mappedNameChars = mappedName.toCharArray();
		StringBuffer encodedNameBuffer = new StringBuffer(mappedNameChars.length);

		for (int charIndex = 0; charIndex < mappedNameChars.length; charIndex++) {
			char nameChar = mappedNameChars[charIndex];
			int keyByte;
			if (charIndex < hashBytes.length - 1) {
				keyByte = hashBytes[charIndex];
			} else {
				keyByte = hashBytes[charIndex % hashBytes.length];
			}

			encodedNameBuffer.append((char)(nameChar ^ (char)keyByte));
		}

		return encodedNameBuffer.toString();
	}

	private void addSubclassMethodMappings(
		boolean includeProtected,
		boolean includeDefaultAccess,
		String packageName,
		boolean includePrivate,
		String nameFilter,
		ReflectionType reflectionType,
		String className,
		DualKeyMap<String, MethodSignature, MethodSignature> candidateMethodMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> methodMappings,
		Map<String, String> inverseClassMappings,
		AbstractWorkspace workspace,
		ScriptContext scriptContext
	) {
		this.addClassMethodMappings(includeProtected, includeDefaultAccess, packageName, includePrivate, nameFilter, reflectionType, className, candidateMethodMappings, methodMappings, inverseClassMappings, scriptContext);

		for (String internalClassName : inverseClassMappings.keySet()) {
			if (workspace.isClassInstanceOf(internalClassName, className)) {
				this.addClassMethodMappings(includeProtected, includeDefaultAccess, AbstractClassNode.getPackageName(internalClassName), includePrivate, nameFilter, reflectionType, internalClassName, candidateMethodMappings, methodMappings, inverseClassMappings, scriptContext);
			}
		}
	}

	private void addFieldMappings(
		DualKeyMap<String, FieldSignature, FieldSignature> candidateFieldMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings,
		boolean publicOnly,
		String nameFilter,
		ReflectionType reflectionType,
		Map<String, String> inverseClassMappings,
		ScriptContext scriptContext
	) {
		Enumeration<String> classNameEnumeration = candidateFieldMappings.getGroupKeys();

		while (classNameEnumeration.hasMoreElements()) {
			String className = classNameEnumeration.nextElement();
			ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(className);
			Map<FieldSignature, FieldSignature> classCandidateFieldMappings = candidateFieldMappings.getGroup(className);
			if (classCandidateFieldMappings != null) {
				for (Entry<FieldSignature, FieldSignature> signatureEntry : classCandidateFieldMappings.entrySet()) {
					FieldSignature originalSignature = signatureEntry.getKey();
					FieldSignature renamedSignature = signatureEntry.getValue();
					FieldNode fieldNode = classNode.findFieldNode(renamedSignature);
					if (!renamedSignature.getName().equals(originalSignature.getName())
						&& (nameFilter == null || originalSignature.getName().equals(nameFilter))
						&& (!publicOnly || fieldNode.isPublic())
						&& (reflectionType == ReflectionType.BOTH || reflectionType == ReflectionType.INSTANCE && !fieldNode.isStatic() || reflectionType == ReflectionType.STATIC && fieldNode.isStatic())) {
						fieldMappings.put(className, originalSignature, renamedSignature);
						this.warnWeakFieldNameHash(className, originalSignature, renamedSignature, inverseClassMappings, scriptContext);
					}
				}
			}
		}
	}

	public void logIncludedMappings(
		ReadOnlyMap<String, String> internalToBinaryNameMap,
		Map<String, String> classMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> methodMappings,
		PrintWriter printWriter
	) {
		for (String mappedClassName : classMappings.keySet()) {
			printWriter.println("\tIncluded AutoReflection mapping for class '" + GlobalUtils.internalToBinaryName(mappedClassName) + "'");
		}

		for (String fieldClassName : fieldMappings.getGroupKeySet()) {
			String mappedFieldClassName = GlobalUtils.getMappedOrDefault(fieldClassName, internalToBinaryNameMap);

			for (Entry<FieldSignature, FieldSignature> signatureEntry : fieldMappings.getGroup(fieldClassName).entrySet()) {
				printWriter.println(
					"\tIncluded AutoReflection mapping for field '"
						+ MethodSignature.formatRemappedType(signatureEntry.getKey().getDescriptor(), internalToBinaryNameMap)
						+ " "
						+ signatureEntry.getKey().getName()
						+ "' in class '"
						+ GlobalUtils.internalToBinaryName(mappedFieldClassName)
						+ "'"
				);
			}
		}

		for (String methodClassName : methodMappings.getGroupKeySet()) {
			String mappedMethodClassName = GlobalUtils.getMappedOrDefault(methodClassName, internalToBinaryNameMap);

			for (Entry<MethodSignature, MethodSignature> methodSignatureEntry : methodMappings.getGroup(methodClassName).entrySet()) {
				printWriter.println(
					"\tIncluded AutoReflection mapping for method '"
						+ methodSignatureEntry.getKey().getName()
						+ MethodSignature.formatRemappedParameters(methodSignatureEntry.getKey().getFullDescriptor(), internalToBinaryNameMap)
						+ "' in class '"
						+ GlobalUtils.internalToBinaryName(mappedMethodClassName)
						+ "'"
				);
			}
		}
	}

	private void addClassMethodMappings(
		boolean includeProtected,
		boolean includeDefaultAccess,
		String packageName,
		boolean includePrivate,
		String nameFilter,
		ReflectionType reflectionType,
		String className,
		DualKeyMap<String, MethodSignature, MethodSignature> candidateMethodMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> methodMappings,
		Map<String, String> inverseClassMappings,
		ScriptContext scriptContext
	) {
		Map<MethodSignature, MethodSignature> classCandidateMethodMappings = candidateMethodMappings.getGroup(className);
		ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(className);
		if (classCandidateMethodMappings != null) {
			for (Entry<MethodSignature, MethodSignature> signatureEntry : classCandidateMethodMappings.entrySet()) {
				MethodSignature originalSignature = signatureEntry.getKey();
				MethodSignature renamedSignature = signatureEntry.getValue();
				if (!renamedSignature.getName().equals(originalSignature.getName()) && (nameFilter == null || originalSignature.getName().equals(nameFilter))) {
					MethodNode methodNode = classNode.findMethodNode(renamedSignature);
					switch (reflectionType) {
						case INSTANCE:
							if (methodNode.isStatic()) {
								continue;
							}
							break;
						case STATIC:
							if (!methodNode.isStatic()) {
								continue;
							}
					}

					if (methodNode.isPublic() || includeProtected && methodNode.isProtected() || includeDefaultAccess && methodNode.isDefaultAccess() && classNode.getPackageName().equals(packageName) || includePrivate && methodNode.isPrivate()
						)
					 {
						methodMappings.put(className, originalSignature, renamedSignature);
						this.warnWeakMethodNameHash(className, originalSignature, renamedSignature, inverseClassMappings, scriptContext);
					}
				}
			}
		}
	}

	private void warnWeakClassNameHash(String originalClassName, String renamedClassName, ScriptContext scriptContext) {
		String originalSimpleName = AbstractClassNode.getSimpleNameFromInternalName(originalClassName);
		String renamedSimpleName = AbstractClassNode.getSimpleNameFromInternalName(renamedClassName);
		String originalPackageName = AbstractClassNode.getPackageName(originalClassName);
		String renamedPackageName = AbstractClassNode.getPackageName(renamedClassName);
		if (originalPackageName.equals(renamedPackageName)) {
			if (originalSimpleName.length() < minNameLength) {
				scriptContext.logWarning(
					"Auto Reflection Handling : AutoReflection hash is "
						+ (originalSimpleName.length() < 8 ? "very " : "")
						+ "vulnerable to brute force attack : Class mapping '"
						+ GlobalUtils.internalToBinaryName(originalClassName)
						+ "'=>'"
						+ GlobalUtils.internalToBinaryName(renamedClassName)
						+ "'. Only "
						+ originalSimpleName.length()
						+ " characters have to be guessed in class name. If class is not actually accessed via Reflection then use ZKM Script 'accessedByReflection' statement to specify only those classes, methods and fields that are accessed by Reflection. (A)"
				);
			}
		} else {
			StringTokenizer originalPackageTokens = new StringTokenizer(originalPackageName, "/");
			StringTokenizer renamedPackageTokens = new StringTokenizer(renamedPackageName, "/");
			int maxDifferingTokenLength = 0;
			int remainingPackagePathLength = 0;
			String lastDifferingToken = "";
			int packageTokenCount = originalPackageTokens.countTokens();

			for (int i = 0; i < packageTokenCount; i++) {
				String originalPackageToken = originalPackageTokens.nextToken();
				if (renamedPackageTokens.hasMoreTokens()) {
					String renamedPackageToken = renamedPackageTokens.nextToken();
					if (!originalPackageToken.equals(renamedPackageToken)) {
						lastDifferingToken = originalPackageToken;
						if (originalPackageToken.length() > maxDifferingTokenLength) {
							maxDifferingTokenLength = originalPackageToken.length();
						}
					}
				} else {
					remainingPackagePathLength += lastDifferingToken.length();
					lastDifferingToken = "";
					remainingPackagePathLength += originalPackageToken.length() + (remainingPackagePathLength > 0 ? 1 : 0);
					if (originalPackageToken.length() > maxDifferingTokenLength) {
						maxDifferingTokenLength = originalPackageToken.length();
					}
				}
			}

			int maxPackageGuessableLength = Math.max(maxDifferingTokenLength, remainingPackagePathLength);
			if (originalSimpleName.equals(renamedSimpleName)) {
				if (maxPackageGuessableLength < minNameLength) {
					scriptContext.logWarning(
						"Auto Reflection Handling : AutoReflection hash is "
							+ (maxPackageGuessableLength < 8 ? "very " : "")
							+ "vulnerable to brute force attack : Class mapping '"
							+ GlobalUtils.internalToBinaryName(originalClassName)
							+ "'=>'"
							+ GlobalUtils.internalToBinaryName(renamedClassName)
							+ "'. As few as "
							+ maxPackageGuessableLength
							+ " characters would have to be guessed in package name. If class is not actually accessed via Reflection then use ZKM Script 'accessedByReflection' statement to specify only those classes, methods and fields that are accessed by Reflection. (B)"
					);
				}
			} else {
				int maxGuessableWithClassName = Math.max(maxPackageGuessableLength, originalSimpleName.length() + remainingPackagePathLength + (remainingPackagePathLength > 0 ? 1 : 0));
				if (maxGuessableWithClassName < minNameLength) {
					scriptContext.logWarning(
						"Auto Reflection Handling : AutoReflection hash is "
							+ (maxGuessableWithClassName < 8 ? "very " : "")
							+ "vulnerable to brute force attack : Class mapping '"
							+ GlobalUtils.internalToBinaryName(originalClassName)
							+ "'=>'"
							+ GlobalUtils.internalToBinaryName(renamedClassName)
							+ "'. As few as "
							+ maxGuessableWithClassName
							+ " characters have to be guessed in package or class name. If class is not actually accessed via Reflection then use ZKM Script 'accessedByReflection' statement to specify only those classes, methods and fields that are accessed by Reflection. (C)"
					);
				}
			}
		}
	}

	private void padLookupMappings(
		Set<Entry<String, String>> lookupMappings, Map<String, String> classMappings, Set<String> allClassNames, Map<String, String> classLookupKeys, Map<String, String> fieldLookupKeys, Map<String, String> methodLookupKeys
	) {
		Random random = GlobalUtils.newRandom(32);
		int missingEntryCount = minHashEntryCount - lookupMappings.size();
		HashMap<String, String> newLookupMappings = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(missingEntryCount));
		Collection<String> alreadyMappedClassNames = classMappings.values();
		HashSet<String> unmappedClassNames = GlobalUtils.newHashSet(allClassNames);
		unmappedClassNames.removeAll(alreadyMappedClassNames);
		int classNameEntryCount;
		if (classLookupKeys.size() == 0) {
			classNameEntryCount = 0;
		} else if (methodLookupKeys.size() == 0 && fieldLookupKeys.size() == 0) {
			classNameEntryCount = Math.min(missingEntryCount, unmappedClassNames.size());
		} else {
			classNameEntryCount = Math.min(missingEntryCount / 2, unmappedClassNames.size());
		}

		HashSet<String> usedKeys = GlobalUtils.newHashSet();
		int generatedNameEntryCount = missingEntryCount - classNameEntryCount;
		Iterator<String> unmappedClassNameIterator = unmappedClassNames.iterator();

		for (int classNameEntryIndex = 0; classNameEntryIndex < classNameEntryCount; classNameEntryIndex++) {
			String classEntryLookupKey = this.createRandomHashKey(random, usedKeys);
			String unmappedClassName = unmappedClassNameIterator.next();
			newLookupMappings.put(classEntryLookupKey, unmappedClassName);
		}

		AlphaNameGenerator nameGenerator = new AlphaNameGenerator(0);

		for (int generatedEntryIndex = 0; generatedEntryIndex < generatedNameEntryCount; generatedEntryIndex++) {
			String generatedEntryLookupKey = this.createRandomHashKey(random, usedKeys);
			newLookupMappings.put(generatedEntryLookupKey, nameGenerator.generateName(random.nextInt(200)));
		}

		lookupMappings.addAll(newLookupMappings.entrySet());
	}

	private void warnWeakFieldNameHash(String className, FieldSignature originalSignature, FieldSignature renamedSignature, Map<String, String> inverseClassMappings, ScriptContext scriptContext) {
		String originalFieldName = originalSignature.getName();
		int fieldNameLength = originalFieldName.length();
		String matchedPrefix = null;

		for (String prefix : commonFieldPrefixes) {
			if (originalFieldName.startsWith(prefix)) {
				matchedPrefix = prefix;
				break;
			}
		}

		if (matchedPrefix != null) {
			int nameLengthWithoutPrefix = fieldNameLength - matchedPrefix.length();
			if (nameLengthWithoutPrefix < minNameLength) {
				String mappedClassName = GlobalUtils.getMappedOrDefault(className, inverseClassMappings);
				scriptContext.logWarning(
					"Auto Reflection Handling : AutoReflection hash is "
						+ (fieldNameLength < 8 ? "very " : "")
						+ "vulnerable to brute force attack : Field mapping '"
						+ originalSignature.toRemappedString(inverseClassMappings)
						+ "'=>'"
						+ renamedSignature.toRemappedString(inverseClassMappings)
						+ "' in class '"
						+ GlobalUtils.internalToBinaryName(mappedClassName)
						+ "'. Given that the prefix '"
						+ matchedPrefix
						+ "' is common, only "
						+ nameLengthWithoutPrefix
						+ " characters have to be guessed in field name. If field is not actually accessed via Reflection then use ZKM Script 'accessedByReflection' statement to specify only those classes, methods and fields that are accessed by Reflection. (A)"
				);
			}
		} else if (fieldNameLength < minNameLength) {
			String mappedClassNameNoPrefix = GlobalUtils.getMappedOrDefault(className, inverseClassMappings);
			scriptContext.logWarning(
				"Auto Reflection Handling : AutoReflection hash is "
					+ (fieldNameLength < 8 ? "very " : "")
					+ "vulnerable to brute force attack : Field mapping '"
					+ originalSignature.toRemappedString(inverseClassMappings)
					+ "'=>'"
					+ renamedSignature.toRemappedString(inverseClassMappings)
					+ "' in class '"
					+ GlobalUtils.internalToBinaryName(mappedClassNameNoPrefix)
					+ "'. Only "
					+ fieldNameLength
					+ " characters have to be guessed in field name. If field is not actually accessed via Reflection then use ZKM Script 'accessedByReflection' statement to specify only those classes, methods and fields that are accessed by Reflection. (B)"
			);
		}
	}

	private void buildLookupStrings(List<Entry<String, String>> lookupMappings, List<String> packedLookupStrings, Set<String> usedHashes, MessageDigest messageDigest) throws HashCollisionException, UnsupportedEncodingException {
		int lookupMappingCount = lookupMappings.size();
		int processedEntryCount = 0;
		int packedLength = 0;
		StringBuilder packedStringBuilder = new StringBuilder();

		for (Entry<String, String> lookupMappingEntry : lookupMappings) {
			StringBuilder entryBuilder = new StringBuilder();
			String lookupKey = lookupMappingEntry.getKey();
			String mappedName = lookupMappingEntry.getValue();
			String nameHash = computeNameHash(lookupKey, messageDigest);
			if (!usedHashes.add(nameHash)) {
				throw new HashCollisionException();
			}

			String encodedName = this.xorEncodeName(lookupKey, mappedName, messageDigest);
			entryBuilder.append((char)encodedName.length());
			entryBuilder.append(encodedName);
			entryBuilder.append((char)nameHash.length());
			entryBuilder.append(nameHash);
			int entryUtf8Length = GlobalUtils.getUTF8Length(entryBuilder.toString());
			if (entryUtf8Length > 16384) {
				DebugUtils.assertTrue(false, new String[]{encodedName.length() + " : " + entryUtf8Length, encodedName.substring(0, 100)});
			}

			if (packedLength + entryUtf8Length > 16384 || packedLookupStrings.size() == 0 && lookupMappingCount > 3 && processedEntryCount == lookupMappingCount - 2) {
				packedLookupStrings.add(packedStringBuilder.toString());
				packedLength = 0;
				packedStringBuilder.setLength(0);
			}

			packedLength += entryUtf8Length;
			packedStringBuilder.append((CharSequence)entryBuilder);
			processedEntryCount++;
		}

		if (packedLength > 0) {
			packedLookupStrings.add(packedStringBuilder.toString());
		}
	}

	private AbstractInsnNode[] createLookupInstructions(String lookupString, MethodCallWriter methodCallWriter, ConstantPoolManager constantPoolManager, List<TypeWriter> cpEntries) {
		StringNode stringNode = constantPoolManager.createString(lookupString, cpEntries);
		return new AbstractInsnNode[]{InsnNode.of(DUP), new WideConstantPoolInsnNode(LDC_W, stringNode), new WideConstantPoolInsnNode(INVOKESTATIC, methodCallWriter)};
	}

	private Map<String, String> buildClassLookupKeys(String separator, Map<String, String> classMappings) {
		HashMap<String, String> classLookupKeys = GlobalUtils.newHashMap();

		for (Entry<String, String> classMappingEntry : classMappings.entrySet()) {
			StringBuilder lookupKeyBuilder = new StringBuilder();
			lookupKeyBuilder.append(classMappingEntry.getKey());
			lookupKeyBuilder.append(separator);
			classLookupKeys.put(lookupKeyBuilder.toString(), classMappingEntry.getValue());
		}

		return classLookupKeys;
	}

	private void addSuperclassMethodMappings(
		boolean includeProtected,
		boolean includeDefaultAccess,
		String packageName,
		boolean includePrivate,
		String nameFilter,
		ReflectionType reflectionType,
		String className,
		DualKeyMap<String, MethodSignature, MethodSignature> candidateMethodMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> methodMappings,
		Map<String, String> inverseClassMappings,
		ScriptContext scriptContext
	) {
		this.addClassMethodMappings(includeProtected, includeDefaultAccess, packageName, includePrivate, nameFilter, reflectionType, className, candidateMethodMappings, methodMappings, inverseClassMappings, scriptContext);
		ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(className);
		if (classNode != null) {
			String superclassName = classNode.getSuperclassName();
			ClassNode superclassNode = ClassHierarchyNode.getWorkspaceClassNode(superclassName);
			if (superclassNode != null && superclassNode.isWorkspaceNode()) {
				this.addSuperclassMethodMappings(includeProtected, includeDefaultAccess, packageName, false, nameFilter, reflectionType, superclassName, candidateMethodMappings, methodMappings, inverseClassMappings, scriptContext);
			}

			for (String interfaceName : classNode.getInterfaceNames()) {
				ClassNode interfaceNode = ClassHierarchyNode.getWorkspaceClassNode(interfaceName);
				if (interfaceNode != null && interfaceNode.isWorkspaceNode()) {
					this.addSuperclassMethodMappings(includeProtected, includeDefaultAccess, packageName, false, nameFilter, reflectionType, interfaceName, candidateMethodMappings, methodMappings, inverseClassMappings, scriptContext);
				}
			}
		}
	}

	public void collectReflectionMappings(
		MultiMap<MethodNode, MethodInvocation> reflectionInvocations,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyMap<String, String> internalToBinaryNameMap,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldRegistryView,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodRegistryView,
		Map<String, String> classMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> methodMappings,
		boolean hasAccessedByReflection,
		boolean hasAccessedByReflectionExclude,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		ScriptContext scriptContext
	) {
		HashMap<String, String> candidateClassMappings = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classNameToInternalNameMap.size()));
		HashMap<String, String> candidateInverseClassMappings = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(internalToBinaryNameMap.size()));
		DualKeyMap<String, FieldSignature, FieldSignature> candidateFieldMappings = new DualKeyMap<>(GlobalUtils.getInitialCapacity(fieldRegistryView.getGroupCount()), false);
		DualKeyMap<String, MethodSignature, MethodSignature> candidateMethodMappings = new DualKeyMap<>(GlobalUtils.getInitialCapacity(methodRegistryView.getGroupCount()), false);
		this.collectCandidateMappings(classNameToInternalNameMap, internalToBinaryNameMap, fieldRegistryView, methodRegistryView, candidateClassMappings, candidateInverseClassMappings, candidateFieldMappings, candidateMethodMappings);
		boolean allClassesProcessed = false;
		boolean listResourceBundleProcessed = false;
		boolean instanceMethodsProcessed = false;
		boolean staticMethodsProcessed = false;
		boolean publicMethodsProcessed = false;
		boolean instanceFieldsProcessed = false;
		boolean staticFieldsProcessed = false;
		boolean publicFieldsProcessed = false;

		for (Entry<MethodNode, List<MethodInvocation>> invocationEntry : reflectionInvocations.entrySet()) {
			invocationEntry.getKey().getMethodOwner().getInternalName();

			for (MethodInvocation methodInvocation : invocationEntry.getValue()) {
				RewriteScope rewriteScope = methodInvocation.getPattern().getRewriteScope();
				String memberName = methodInvocation.getMemberName();
				switch (rewriteScope) {
					case ALL_CLASSES:
						if (allClassesProcessed) {
							break;
						}

						allClassesProcessed = true;
						if (candidateClassMappings.size() == 0) {
							this.warnUndeclaredReflection(methodInvocation, hasAccessedByReflection, hasAccessedByReflectionExclude, EntryType.CLASS, scriptContext);
						} else {
							for (Entry<String, String> classMappingEntry : candidateClassMappings.entrySet()) {
								String className = classMappingEntry.getKey();
								String internalClassName = classMappingEntry.getValue();
								if (!className.equals(internalClassName)) {
									classMappings.put(className, internalClassName);
									this.warnWeakClassNameHash(className, internalClassName, scriptContext);
								}
							}
						}
						break;
					case ALL_PUBLIC_METHODS:
						if (publicMethodsProcessed || instanceMethodsProcessed || staticMethodsProcessed) {
							break;
						}

						if (candidateMethodMappings.getGroupCount() == 0) {
							this.warnUndeclaredReflection(methodInvocation, hasAccessedByReflection, hasAccessedByReflectionExclude, EntryType.METHOD, scriptContext);
						} else {
							if (methodInvocation.hasAllTargetClassNames()) {
								Iterator<String> publicMethodTargetIterator = methodInvocation.getTargetClassNames().iterator();

								while (publicMethodTargetIterator.hasNext()) {
									String publicMethodTargetClassName = GlobalUtils.getMappedOrDefault(publicMethodTargetIterator.next(), classNameToInternalNameMap);
									this.addHierarchyMethodMappings(publicMethodTargetClassName, memberName, candidateMethodMappings, methodMappings, candidateInverseClassMappings, workspace, scriptContext);
								}
								break;
							}

							this.addMethodMappings(candidateMethodMappings, methodMappings, true, memberName, ReflectionType.BOTH, candidateInverseClassMappings, scriptContext);
							publicMethodsProcessed = memberName == null;
						}
						break;
					case ALL_PUBLIC_FIELDS:
						if (publicFieldsProcessed || instanceFieldsProcessed || staticFieldsProcessed) {
							break;
						}

						if (candidateFieldMappings.getGroupCount() == 0) {
							this.warnUndeclaredReflection(methodInvocation, hasAccessedByReflection, hasAccessedByReflectionExclude, EntryType.FIELD, scriptContext);
						} else {
							if (methodInvocation.hasAllTargetClassNames()) {
								Iterator<String> publicFieldTargetIterator = methodInvocation.getTargetClassNames().iterator();

								while (publicFieldTargetIterator.hasNext()) {
									String publicFieldTargetClassName = GlobalUtils.getMappedOrDefault(publicFieldTargetIterator.next(), classNameToInternalNameMap);
									this.addHierarchyFieldMappings(publicFieldTargetClassName, memberName, candidateFieldMappings, fieldMappings, candidateInverseClassMappings, workspace, scriptContext);
								}
								break;
							}

							this.addFieldMappings(candidateFieldMappings, fieldMappings, true, memberName, ReflectionType.BOTH, candidateInverseClassMappings, scriptContext);
							publicFieldsProcessed = memberName == null;
						}
						break;
					case ALL_METHODS_OF_CLASS:
						if (instanceMethodsProcessed || staticMethodsProcessed) {
							break;
						}

						if (candidateMethodMappings.getGroupCount() == 0) {
							this.warnUndeclaredReflection(methodInvocation, hasAccessedByReflection, hasAccessedByReflectionExclude, EntryType.METHOD, scriptContext);
						} else {
							if (methodInvocation.hasAllTargetClassNames()) {
								Iterator<String> classMethodTargetIterator = methodInvocation.getTargetClassNames().iterator();

								while (classMethodTargetIterator.hasNext()) {
									String classMethodTargetClassName = GlobalUtils.getMappedOrDefault(classMethodTargetIterator.next(), classNameToInternalNameMap);
									this.addSubclassMethodMappings(true, true, AbstractClassNode.getPackageName(classMethodTargetClassName), true, memberName, ReflectionType.BOTH, classMethodTargetClassName, candidateMethodMappings, methodMappings, candidateInverseClassMappings, workspace, scriptContext);
								}
								break;
							}

							this.addMethodMappings(candidateMethodMappings, methodMappings, false, memberName, ReflectionType.BOTH, candidateInverseClassMappings, scriptContext);
							instanceMethodsProcessed = memberName == null;
							staticMethodsProcessed = memberName == null;
							publicMethodsProcessed = memberName == null;
						}
						break;
					case ALL_INSTANCE_FIELDS_OF_CLASS:
					case ALL_FIELDS_OF_CLASS:
						if (instanceFieldsProcessed || staticFieldsProcessed) {
							break;
						}

						if (candidateFieldMappings.getGroupCount() == 0) {
							this.warnUndeclaredReflection(methodInvocation, hasAccessedByReflection, hasAccessedByReflectionExclude, EntryType.FIELD, scriptContext);
						} else {
							if (methodInvocation.hasAllTargetClassNames()) {
								Iterator<String> classFieldTargetIterator = methodInvocation.getTargetClassNames().iterator();

								while (classFieldTargetIterator.hasNext()) {
									String classFieldTargetClassName = GlobalUtils.getMappedOrDefault(classFieldTargetIterator.next(), classNameToInternalNameMap);
									this.addSubclassFieldMappings(true, true, AbstractClassNode.getPackageName(classFieldTargetClassName), true, memberName, ReflectionType.BOTH, classFieldTargetClassName, candidateFieldMappings, fieldMappings, candidateInverseClassMappings, workspace, scriptContext);
								}
								break;
							}

							this.addFieldMappings(candidateFieldMappings, fieldMappings, false, memberName, ReflectionType.BOTH, candidateInverseClassMappings, scriptContext);
							instanceFieldsProcessed = memberName == null;
							staticFieldsProcessed = memberName == null;
							publicFieldsProcessed = memberName == null;
						}
						break;
					case ALL_VIRTUAL_METHODS_ACCESSIBLE_FROM_CLASS:
					case ALL_STATIC_METHODS_ACCESSIBLE_FROM_CLASS:
						ReflectionType methodReflectionType;
						if (rewriteScope == RewriteScope.ALL_VIRTUAL_METHODS_ACCESSIBLE_FROM_CLASS) {
							methodReflectionType = ReflectionType.INSTANCE;
						} else {
							methodReflectionType = ReflectionType.STATIC;
						}

						if ((methodReflectionType != ReflectionType.INSTANCE || instanceMethodsProcessed) && (methodReflectionType != ReflectionType.STATIC || staticMethodsProcessed)) {
							break;
						}

						if (candidateMethodMappings.getGroupCount() == 0) {
							this.warnUndeclaredReflection(methodInvocation, hasAccessedByReflection, hasAccessedByReflectionExclude, EntryType.METHOD, scriptContext);
						} else if (methodInvocation.hasAllTargetClassNames()) {
							Iterator<String> accessibleMethodTargetIterator = methodInvocation.getTargetClassNames().iterator();

							while (accessibleMethodTargetIterator.hasNext()) {
								String accessibleMethodTargetClassName = GlobalUtils.getMappedOrDefault(accessibleMethodTargetIterator.next(), classNameToInternalNameMap);
								this.addSuperclassMethodMappings(true, true, AbstractClassNode.getPackageName(accessibleMethodTargetClassName), true, memberName, methodReflectionType, accessibleMethodTargetClassName, candidateMethodMappings, methodMappings, candidateInverseClassMappings, scriptContext);
								this.addSubclassMethodMappings(true, true, AbstractClassNode.getPackageName(accessibleMethodTargetClassName), true, memberName, methodReflectionType, accessibleMethodTargetClassName, candidateMethodMappings, methodMappings, candidateInverseClassMappings, workspace, scriptContext);
							}
						} else {
							this.addMethodMappings(candidateMethodMappings, methodMappings, false, memberName, methodReflectionType, candidateInverseClassMappings, scriptContext);
							if (methodReflectionType == ReflectionType.INSTANCE) {
								instanceMethodsProcessed = memberName == null;
							} else {
								staticMethodsProcessed = memberName == null;
							}
						}
						break;
					case ALL_STATIC_FIELDS_ACCESSIBLE_FROM_CLASS:
					case ALL_INSTANCE_FIELDS_ACCESSIBLE_FROM_CLASS:
						ReflectionType fieldReflectionType;
						if (rewriteScope == RewriteScope.ALL_INSTANCE_FIELDS_ACCESSIBLE_FROM_CLASS) {
							fieldReflectionType = ReflectionType.INSTANCE;
						} else {
							fieldReflectionType = ReflectionType.STATIC;
						}

						if (publicFieldsProcessed || (fieldReflectionType != ReflectionType.INSTANCE || instanceFieldsProcessed) && (fieldReflectionType != ReflectionType.STATIC || staticFieldsProcessed)) {
							break;
						}

						if (candidateFieldMappings.getGroupCount() == 0) {
							this.warnUndeclaredReflection(methodInvocation, hasAccessedByReflection, hasAccessedByReflectionExclude, EntryType.FIELD, scriptContext);
						} else if (methodInvocation.hasAllTargetClassNames()) {
							Iterator<String> accessibleFieldTargetIterator = methodInvocation.getTargetClassNames().iterator();

							while (accessibleFieldTargetIterator.hasNext()) {
								String accessibleFieldTargetClassName = GlobalUtils.getMappedOrDefault(accessibleFieldTargetIterator.next(), classNameToInternalNameMap);
								this.addSuperclassFieldMappings(true, true, AbstractClassNode.getPackageName(accessibleFieldTargetClassName), true, memberName, fieldReflectionType, accessibleFieldTargetClassName, candidateFieldMappings, fieldMappings, candidateInverseClassMappings, scriptContext);
								this.addSubclassFieldMappings(true, true, AbstractClassNode.getPackageName(accessibleFieldTargetClassName), true, memberName, fieldReflectionType, accessibleFieldTargetClassName, candidateFieldMappings, fieldMappings, candidateInverseClassMappings, workspace, scriptContext);
							}
						} else {
							this.addFieldMappings(candidateFieldMappings, fieldMappings, true, memberName, fieldReflectionType, candidateInverseClassMappings, scriptContext);
							if (fieldReflectionType == ReflectionType.INSTANCE) {
								instanceFieldsProcessed = memberName == null;
							} else {
								staticFieldsProcessed = memberName == null;
							}
						}
						break;
					case ALL_LIST_RESOUCE_BUNDLE:
						if (!allClassesProcessed && !listResourceBundleProcessed) {
							listResourceBundleProcessed = true;
							if (candidateClassMappings.size() == 0) {
								this.warnUndeclaredReflection(methodInvocation, hasAccessedByReflection, hasAccessedByReflectionExclude, EntryType.CLASS, scriptContext);
							} else {
								for (Entry<String, String> resourceBundleMappingEntry : candidateClassMappings.entrySet()) {
									String resourceBundleClassName = resourceBundleMappingEntry.getKey();
									String resourceBundleInternalClassName = resourceBundleMappingEntry.getValue();
									if (!resourceBundleClassName.equals(resourceBundleInternalClassName) && workspace.isClassInstanceOf(resourceBundleInternalClassName, "java/util/ListResourceBundle")) {
										classMappings.put(resourceBundleClassName, resourceBundleInternalClassName);
										this.warnWeakClassNameHash(resourceBundleClassName, resourceBundleInternalClassName, scriptContext);
									}
								}
							}
						}
					case NONE:
				}
			}
		}
	}

	private void warnWeakMethodNameHash(String className, MethodSignature originalSignature, MethodSignature renamedSignature, Map<String, String> inverseClassMappings, ScriptContext scriptContext) {
		String originalMethodName = originalSignature.getName();
		int methodNameLength = originalMethodName.length();
		String matchedPrefix = null;

		for (String prefix : commonMethodPrefixes) {
			if (originalMethodName.startsWith(prefix)) {
				matchedPrefix = prefix;
				break;
			}
		}

		if (matchedPrefix != null) {
			int nameLengthWithoutPrefix = methodNameLength - matchedPrefix.length();
			if (nameLengthWithoutPrefix < minNameLength) {
				String mappedClassName = GlobalUtils.getMappedOrDefault(className, inverseClassMappings);
				scriptContext.logWarning(
					"Auto Reflection Handling : AutoReflection hash is "
						+ (nameLengthWithoutPrefix < 8 ? "very " : "")
						+ "vulnerable to brute force attack : Method mapping '"
						+ originalSignature.getSignature(inverseClassMappings)
						+ "'=>'"
						+ renamedSignature.getSignature(inverseClassMappings)
						+ "' in class '"
						+ GlobalUtils.internalToBinaryName(mappedClassName)
						+ "'. Given that the prefix '"
						+ matchedPrefix
						+ "' is common, only "
						+ nameLengthWithoutPrefix
						+ " characters have to be guessed in method name. If method is not actually accessed via Reflection then use ZKM Script 'accessedByReflection' statement to specify only those classes, methods and fields that are accessed by Reflection. (A)"
				);
			}
		} else if (methodNameLength < minNameLength) {
			String mappedClassNameNoPrefix = GlobalUtils.getMappedOrDefault(className, inverseClassMappings);
			scriptContext.logWarning(
				"Auto Reflection Handling : AutoReflection hash is "
					+ (methodNameLength < 8 ? "very " : "")
					+ "vulnerable to brute force attack : Method mapping '"
					+ originalSignature.getSignature(inverseClassMappings)
					+ "'=>'"
					+ renamedSignature.getSignature(inverseClassMappings)
					+ "' in class '"
					+ GlobalUtils.internalToBinaryName(mappedClassNameNoPrefix)
					+ "'. Only "
					+ methodNameLength
					+ " characters have to be guessed in method name. If method is not actually accessed via Reflection then use ZKM Script 'accessedByReflection' statement to specify only those classes, methods and fields that are accessed by Reflection. (B)"
			);
		}
	}

	public Set<String> planAuxiliaryClassName(
		Set<ClassNode> allWorkspaceClasses,
		AtomicReference<String> dataClassName,
		Map<ArchiveNode, String> archiveToDataClassNameMap,
		Map<ClassNode, String> classToDataClassNameMap,
		ChangeLogContext changeLogContext,
		String autoReflectionPackage,
		ReferenceGraph<ArchiveNode, ArchiveNode> archiveDependencyGraph,
		ReferenceGraph<ArchiveNode, String> archivePackageReferences,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> archiveToClassesMap,
		ReadOnlyMap<String, String> packageRenameMap,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyMap<String, String> internalToBinaryNameMap,
		String newNamesPrefix,
		MixCaseType mixedCaseClassNames,
		boolean uniqueClassNames,
		Workspace workspace,
		ClassRepository classRepository,
		ScriptContext scriptContext
	) {
		HashSet<String> eligiblePackageNames = GlobalUtils.newHashSet();
		HashSet<ClassNode> eligibleClasses = GlobalUtils.newHashSet();
		HashSet<ClassNode> shareableClasses = GlobalUtils.newHashSet();
		allWorkspaceClasses = InjectedClassManager.collectEligibleClasses(allWorkspaceClasses, eligibleClasses, shareableClasses, eligiblePackageNames);
		int shareableClassCount = shareableClasses.size();
		HashSet<String> plannedInternalNames = GlobalUtils.newHashSet();
		HashSet<String> usedInternalNames = GlobalUtils.newHashSet();
		if (changeLogContext != null) {
			String configuredClassName = changeLogContext.getAutoReflectionClassName();
			if (configuredClassName != null) {
				String validatedClassName = InjectedClassManager.validateUserConfiguredName(
					"AutoReflectionClass:", configuredClassName, "autoReflectionPackage", autoReflectionPackage, "Auto Reflection Handling", eligiblePackageNames, changeLogContext, internalToBinaryNameMap
				);
				if (validatedClassName != null) {
					dataClassName.set(validatedClassName);
					plannedInternalNames.add(validatedClassName);
					usedInternalNames.add(GlobalUtils.binaryToInternalName(validatedClassName));

					for (ClassNode classNode : eligibleClasses) {
						classToDataClassNameMap.put(classNode, validatedClassName);
					}
				}
			}

			for (ArchiveNode archiveNode : archiveToClassesMap.keySet()) {
				String moduleName = archiveNode.getModuleName();
				String configuredModuleClassName = changeLogContext.getModuleAutoReflectionClass(moduleName);
				if (configuredModuleClassName != null) {
					String validatedModuleClassName = InjectedClassManager.validateUserConfiguredName(
						"AutoReflectionClass:", configuredModuleClassName, "autoReflectionPackage", autoReflectionPackage, "Auto Reflection Handling", archivePackageReferences.getReferences(archiveNode), changeLogContext, internalToBinaryNameMap
					);
					if (validatedModuleClassName != null) {
						archiveToDataClassNameMap.put(archiveNode, validatedModuleClassName);
						plannedInternalNames.add(validatedModuleClassName);
						usedInternalNames.add(GlobalUtils.binaryToInternalName(validatedModuleClassName));

						for (ClassNode archiveClassNode : archiveToClassesMap.get(archiveNode)) {
							classToDataClassNameMap.put(archiveClassNode, validatedModuleClassName);
						}
					}
				}
			}

			if ((shareableClassCount == 0 || !dataClassName.isNull()) && archiveToClassesMap.size() == archiveToDataClassNameMap.size()) {
				InjectedClassManager.propagateArchiveMappings(allWorkspaceClasses, dataClassName, archiveToDataClassNameMap, shareableClasses, classToDataClassNameMap, archiveToClassesMap);
				return plannedInternalNames;
			}

			if (shareableClassCount > 0) {
				String configuredReferencesClassName = changeLogContext.getObfuscateReferencesClassName();
				if (configuredReferencesClassName != null) {
					usedInternalNames.add(GlobalUtils.binaryToInternalName(configuredReferencesClassName));
				}
			}

			Iterator<ArchiveNode> archiveNodeIterator = archiveToClassesMap.keySet().iterator();

			while (archiveNodeIterator.hasNext()) {
				String obfuscateRefsModuleName = archiveNodeIterator.next().getModuleName();
				String configuredModuleReferencesClassName = changeLogContext.getModuleObfuscateReferencesClass(obfuscateRefsModuleName);
				if (configuredModuleReferencesClassName != null) {
					usedInternalNames.add(GlobalUtils.binaryToInternalName(configuredModuleReferencesClassName));
				}
			}
		}

		return InjectedClassManager.planAuxiliaryClassNames(
			allWorkspaceClasses,
			null,
			dataClassName,
			archiveToDataClassNameMap,
			classToDataClassNameMap,
			"autoReflectionPackage",
			autoReflectionPackage,
			eligibleClasses,
			shareableClasses,
			archiveToClassesMap,
			archiveDependencyGraph,
			archivePackageReferences,
			packageRenameMap,
			classNameToInternalNameMap,
			internalToBinaryNameMap,
			usedInternalNames,
			newNamesPrefix,
			mixedCaseClassNames,
			uniqueClassNames,
			this.randomizeObfuscation,
			"Auto Reflection Handling",
			this.userConfiguredNames,
			classRepository,
			scriptContext
		);
	}

	private void warnUndeclaredReflection(MethodInvocation reflectionInvocation, boolean hasAccessedByReflection, boolean hasAccessedByReflectionExclude, EntryType entryType, ScriptContext scriptContext) {
		String statementDescription;
		if (hasAccessedByReflection && hasAccessedByReflectionExclude) {
			statementDescription = "net affect of 'accessedByReflection' and 'accessedByReflectionExclude' statements specifies";
		} else if (hasAccessedByReflection) {
			statementDescription = "'accessedByReflection' statement specifies";
		} else {
			statementDescription = "'accessedByReflectionExclude' statement leaves";
		}

		scriptContext.logWarning(
			"AutoReflection : A "
				+ entryType.toString().toLowerCase()
				+ " is accessed by reflection in '"
				+ reflectionInvocation.getSourcePath()
				+ "' but "
				+ statementDescription
				+ " no corresponding "
				+ entryType.toString().toLowerCase()
				+ " as accessed by Reflection."
		);
	}

	private Map<String, String> buildMethodLookupKeys(String separator, DualKeyMap<String, MethodSignature, MethodSignature> methodMappings) {
		HashMap<String, String> methodLookupKeys = GlobalUtils.newHashMap();
		Enumeration<String> classNameEnumeration = methodMappings.getGroupKeys();

		while (classNameEnumeration.hasMoreElements()) {
			String className = classNameEnumeration.nextElement();

			for (Entry<MethodSignature, MethodSignature> signatureEntry : methodMappings.getGroup(className).entrySet()) {
				MethodSignature originalSignature = signatureEntry.getKey();
				MethodSignature renamedSignature = signatureEntry.getValue();
				StringBuilder lookupKeyBuilder = new StringBuilder();
				lookupKeyBuilder.append(className);
				lookupKeyBuilder.append(separator);
				lookupKeyBuilder.append(originalSignature.getName());
				lookupKeyBuilder.append(separator);

				for (String parameterDescriptor : TypeWriter.parseParameterDescriptors(renamedSignature.getFullDescriptor())) {
					if (parameterDescriptor.startsWith("L") && parameterDescriptor.endsWith(";")) {
						parameterDescriptor = parameterDescriptor.substring(1, parameterDescriptor.length() - 1);
					}

					lookupKeyBuilder.append(parameterDescriptor);
					lookupKeyBuilder.append(separator);
				}

				methodLookupKeys.put(lookupKeyBuilder.toString(), renamedSignature.getName());
			}
		}

		return methodLookupKeys;
	}

	private void addSuperclassFieldMappings(
		boolean includeProtected,
		boolean includeDefaultAccess,
		String packageName,
		boolean includePrivate,
		String nameFilter,
		ReflectionType reflectionType,
		String className,
		DualKeyMap<String, FieldSignature, FieldSignature> candidateFieldMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings,
		Map<String, String> inverseClassMappings,
		ScriptContext scriptContext
	) {
		this.addClassFieldMappings(includeProtected, includeDefaultAccess, packageName, includePrivate, nameFilter, reflectionType, className, candidateFieldMappings, fieldMappings, inverseClassMappings, scriptContext);
		ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(className);
		if (classNode != null) {
			String superclassName = classNode.getSuperclassName();
			ClassNode superclassNode = ClassHierarchyNode.getWorkspaceClassNode(superclassName);
			if (superclassNode != null && superclassNode.isWorkspaceNode()) {
				this.addSuperclassFieldMappings(includeProtected, includeDefaultAccess, packageName, false, nameFilter, reflectionType, superclassName, candidateFieldMappings, fieldMappings, inverseClassMappings, scriptContext);
			}

			for (String interfaceName : classNode.getInterfaceNames()) {
				ClassNode interfaceNode = ClassHierarchyNode.getWorkspaceClassNode(interfaceName);
				if (interfaceNode != null && interfaceNode.isWorkspaceNode()) {
					this.addSuperclassFieldMappings(includeProtected, includeDefaultAccess, packageName, false, nameFilter, reflectionType, interfaceName, candidateFieldMappings, fieldMappings, inverseClassMappings, scriptContext);
				}
			}
		}
	}

	private void addHierarchyFieldMappings(
		String className,
		String nameFilter,
		DualKeyMap<String, FieldSignature, FieldSignature> candidateFieldMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings,
		Map<String, String> inverseClassMappings,
		AbstractWorkspace workspace,
		ScriptContext scriptContext
	) {
		this.addSuperclassFieldMappings(false, false, null, false, nameFilter, ReflectionType.BOTH, className, candidateFieldMappings, fieldMappings, inverseClassMappings, scriptContext);
		this.addSubclassFieldMappings(false, false, null, false, nameFilter, ReflectionType.BOTH, className, candidateFieldMappings, fieldMappings, inverseClassMappings, workspace, scriptContext);
	}

	private void addHierarchyMethodMappings(
		String className,
		String nameFilter,
		DualKeyMap<String, MethodSignature, MethodSignature> candidateMethodMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> methodMappings,
		Map<String, String> inverseClassMappings,
		AbstractWorkspace workspace,
		ScriptContext scriptContext
	) {
		this.addSuperclassMethodMappings(false, false, null, false, nameFilter, ReflectionType.BOTH, className, candidateMethodMappings, methodMappings, inverseClassMappings, scriptContext);
		this.addSubclassMethodMappings(false, false, null, false, nameFilter, ReflectionType.BOTH, className, candidateMethodMappings, methodMappings, inverseClassMappings, workspace, scriptContext);
	}

	private void addClassFieldMappings(
		boolean includeProtected,
		boolean includeDefaultAccess,
		String packageName,
		boolean includePrivate,
		String nameFilter,
		ReflectionType reflectionType,
		String className,
		DualKeyMap<String, FieldSignature, FieldSignature> candidateFieldMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings,
		Map<String, String> inverseClassMappings,
		ScriptContext scriptContext
	) {
		Map<FieldSignature, FieldSignature> classCandidateFieldMappings = candidateFieldMappings.getGroup(className);
		ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(className);
		if (classCandidateFieldMappings != null) {
			for (Entry<FieldSignature, FieldSignature> signatureEntry : classCandidateFieldMappings.entrySet()) {
				FieldSignature originalSignature = signatureEntry.getKey();
				FieldSignature renamedSignature = signatureEntry.getValue();
				if (!renamedSignature.getName().equals(originalSignature.getName()) && (nameFilter == null || originalSignature.getName().equals(nameFilter))) {
					FieldNode fieldNode = classNode.findFieldNode(renamedSignature);
					switch (reflectionType) {
						case INSTANCE:
							if (fieldNode.isStatic()) {
								continue;
							}
							break;
						case STATIC:
							if (!fieldNode.isStatic()) {
								continue;
							}
					}

					if (fieldNode.isPublic() || includeProtected && fieldNode.isProtected() || includeDefaultAccess && fieldNode.isDefaultAccess() && classNode.getPackageName().equals(packageName) || includePrivate && fieldNode.isPrivate()
						)
					 {
						fieldMappings.put(className, originalSignature, renamedSignature);
						this.warnWeakFieldNameHash(className, originalSignature, renamedSignature, inverseClassMappings, scriptContext);
					}
				}
			}
		}
	}

	static {
		int effectiveMinNameLength = 11;
		if (Options.autoReflectionMinLength != null) {
			try {
				int parsedMinNameLength = Integer.parseInt(Options.autoReflectionMinLength);
				if (parsedMinNameLength >= 8) {
					effectiveMinNameLength = parsedMinNameLength;
				}
			} catch (NumberFormatException ignoredNameLengthParse) {
			}
		}

		minNameLength = effectiveMinNameLength;
		int effectiveMinHashEntryCount = 100;
		if (Options.minAutoReflectionEntries != null) {
			try {
				effectiveMinHashEntryCount = Integer.parseInt(Options.minAutoReflectionEntries);
			} catch (NumberFormatException ignoredHashCountParse) {
			}
		}

		minHashEntryCount = effectiveMinHashEntryCount;
	}
}

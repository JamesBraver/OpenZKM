package com.zelix.klassmaster.changelogs.parser.context;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.FlaggedMethodSignature;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.changelogs.*;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.tools.parameterChanger.extra.ExtraParameter;
import com.zelix.utils.*;
import com.zelix.utils.collections.*;
import com.zelix.utils.collections.readOnly.ReadOnlyList;
import com.zelix.utils.dataset.AtomicLong;
import com.zelix.utils.dataset.ComparablePair;
import com.zelix.utils.dataset.Triplet;
import com.zelix.utils.enumerations.SnapshotEnumerator;
import com.zelix.utils.references.AtomicReference;
import com.zelix.utils.references.IdentityReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.Workspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;

public class ChangeLogContext extends AbstractChangeLogContext {
	public static String versionPrefix = "version=";
	public static String encodingPrefix = "encoding=";
	public static String aggressiveOverloadingFlag = "aggessiveMethodOverloading";
	private Set<FlowObfuscationEntry> flowObfuscationEntries = GlobalUtils.newHashSet();
	private ReferenceGraph<String, FlowObfuscationEntry> flowObfuscationReferenceGraph = new ReferenceGraph<>();
	private Set<String> flowObfuscationClasses = GlobalUtils.newHashSet();
	private List<FlowObfuscationEntry> flowObfuscationEntryList = new ArrayList<>();
	private Map<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> memberClassEntries = GlobalUtils.newHashMap();
	private Map<String, String> autoReflectionClassByModule = GlobalUtils.newHashMap();
	private Map<String, String> obfuscateReferencesByModule = GlobalUtils.newHashMap();
	private Map<String, Triplet> moduleParameterChangeClasses = GlobalUtils.newHashMap();
	private boolean fieldKeysHaveType = true;
	private boolean methodKeysHaveReturnType = true;
	private final Map<Triplet, Triplet> parameterChangeTriplets = GlobalUtils.newHashMap();
	private String logFileName;
	private ScriptContext scriptContext;
	private final boolean loose;
	private Map<AbstractClassNode, Long> classEncryptionKeys;
	private int packageMappingCount;
	private Map<String, String> newToOldClassNameMap;
	private String autoReflectionClassName;
	private Map<String, String> oldToNewPackageNameMap;
	private Map<AbstractMethodNode, long[]> methodSaltValues;
	private boolean hasError;
	private DualKeyMap<String, FieldSignature, FieldSignature> processedFieldMappingsReverse;
	private boolean sawFlowObfuscationEntry;
	private HierarchicalMultiMap<String, MethodKey, MethodMappingInfo> rawMethodMappings;
	private DualKeyMap<String, MethodSignature, FlaggedMethodSignature> processedMethodMappings;
	private Map<AbstractClassNode, long[]> classSaltValues;
	private HierarchicalMultiMap<String, Integer, Integer> lineNumberMappings;
	private DualKeyMap<String, String, String> newToOldPackageNameMap;
	private Triplet parameterChangerDataClasses;
	private Map<String, String> oldToNewClassNameMap;
	private HierarchicalMultiMap<String, FieldKey, String> rawFieldMappings;
	private Set<AbstractMethodNode> parameterObfuscatedMethods;
	private Map<AbstractMethodNode, ExtraParameter[]> methodParameterChangeMap;
	private String obfuscateReferencesClassName;
	private Map<AbstractMethodNode, Long> methodEncryptionKeys;
	private ThreeLevelMultiMap<String, ComparablePair<String, String>, String, String> newToOldMethodNames;
	private Map<String, String> oldClassNameToSourceName;
	private Map<String, String> prunedClassMappings;
	private boolean parsed;
	private DualKeyMap<String, FieldSignature, FieldSignature> processedFieldMappings;
	private String fileLocationHint;
	private int classMappingCount;
	private HierarchicalMultiMap<String, FieldKey, String> prunedClassFieldMappings;

	public ChangeLogContext(String logFileName, ScriptContext scriptContext, boolean loose) {
		this.logFileName = logFileName;
		this.scriptContext = scriptContext;
		this.loose = loose;
	}

	public ChangeLogContext(String logFileName, ScriptContext scriptContext) {
		this(logFileName, scriptContext, false);
	}

	private static String hashFlowObfuscationEntryKey(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(value.getBytes(), 0, value.length());
			byte[] digestBytes = digest.digest();
			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < digestBytes.length; i++) {
				String hexByte = Integer.toHexString(digestBytes[i] & 255);
				if (hexByte.length() == 1) {
					builder.append('0');
				}

				builder.append(hexByte);
			}

			return builder.toString();
		} catch (NoSuchAlgorithmException noMd5) {
			DebugUtils.assertTrue(false, new String[]{noMd5.toString()});
			return "";
		}
	}

	private FieldKey findFieldKeyByHash(String className, String hash) {
		MultiMap<FieldKey, String> fieldMappings = this.rawFieldMappings.get(className);

		for (FieldKey fieldKey : fieldMappings.keySet()) {
			String mappedValue = fieldKey.getName() + " " + MethodSignature.fromHumanReadableType(fieldKey.getDescriptor()) + "java.lang.Object[]";
			if (hashFlowObfuscationEntryKey(mappedValue).equals(hash)) {
				return fieldKey;
			}
		}

		return null;
	}

	private MethodKey findMethodKeyByHash(String className, String hash) {
		MultiMap<MethodKey, MethodMappingInfo> methodMappings = this.rawMethodMappings.get(className);

		for (MethodKey methodKey : methodMappings.keySet()) {
			String mappedValue = methodKey.getName()
				+ MethodSignature.fromHumanReadableParameters(methodKey.getParameterDescriptor())
				+ MethodSignature.fromHumanReadableType(methodKey.getDescriptor())
				+ "java.lang.Object[]";
			if (hashFlowObfuscationEntryKey(mappedValue).equals(hash)) {
				return methodKey;
			}
		}

		return null;
	}

	public static String getSimpleClassName(String className) {
		int lastDotIndex = className.lastIndexOf(46);
		return lastDotIndex == -1 ? className : className.substring(lastDotIndex + 1);
	}

	public static boolean hasAggressiveOverloading(File changeLogFile) {
		BufferedReader reader = null;

		try {
			reader = FileUtils.createBufferedReader(changeLogFile);
			String firstLine = reader.readLine();
			if (firstLine == null) {
				return false;
			}

			StringTokenizer tokenizer = new StringTokenizer(firstLine);

			do {
				if (!tokenizer.hasMoreTokens()) {
					return false;
				}
			} while (!tokenizer.nextToken().equals(aggressiveOverloadingFlag));
		} catch (IOException ignoredRead) {
			return false;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignoredClose) {
				}
			}
		}

		return true;
	}

	public static MethodSignature buildChangedMethodSignature(AbstractMethodNode method, ExtraParameter[] extraParameters) {
		String parameterDescriptor = ParameterPattern.buildParameterDescriptor(MethodSignature.parseParameterDescriptors(method.getDescriptor()), extraParameters);
		return new MethodSignature(method.getName(), parameterDescriptor, method.getReturnDescriptor());
	}

	public static String getLastSegment(String value, int separator) {
		int separatorIndex = value.lastIndexOf(separator);
		return separatorIndex != -1 ? value.substring(separatorIndex + 1) : value;
	}

	private static String decodeFieldDescriptor(String encodedType, List<FlowObfuscationEntry> flowObfuscationEntries, String className) {
		char typeCode = encodedType.charAt(0);
		if (typeCode >= '0' && typeCode <= '5') {
			switch (typeCode) {
				case '1':
					return "Z";
				case '2':
					return "I";
				case '3':
					return "Ljava/lang/String;";
				case '4':
					return "[I";
				case '5':
					return "[Ljava/lang/String;";
				default:
					return null;
			}
		} else {
			int payloadStartIndex = AbstractChangeLogContext.payloadStartIndex(encodedType);
			int classIndex = (int)GlobalUtils.baseNToLong(encodedType.substring(0, payloadStartIndex), "MQoSAkmVrHyDwxKuhLPOURetTIFYqWnpbzfjEsXcZlNCdvgJGBia");
			String internalClassName;
			if (classIndex < flowObfuscationEntries.size() && flowObfuscationEntries.get(classIndex) != null) {
				internalClassName = flowObfuscationEntries.get(classIndex).getInternalClassName();
			} else {
				if (classIndex != flowObfuscationEntries.size()) {
					return null;
				}

				internalClassName = GlobalUtils.binaryToInternalName(className);
			}

			return "[L" + internalClassName + ';';
		}
	}

	public static String getPackageName(String className) {
		int lastDotIndex = className.lastIndexOf(46);
		return lastDotIndex == -1 ? "" : className.substring(0, lastDotIndex);
	}

	private static String adjustPackageName(String newPackageName, String expectedSuperPackageName, String actualSuperPackageName) {
		int newPackageDepth = StringUtils.countOccurrences(newPackageName, '.') + (newPackageName.length() > 0 ? 1 : 0);
		int expectedSuperDepth = StringUtils.countOccurrences(expectedSuperPackageName, '.') + (expectedSuperPackageName.length() > 0 ? 1 : 0);
		int actualSuperDepth = StringUtils.countOccurrences(actualSuperPackageName, '.') + (actualSuperPackageName.length() > 0 ? 1 : 0);
		String adjustedPackageName;
		if (newPackageDepth == expectedSuperDepth + 1) {
			adjustedPackageName = actualSuperPackageName + "." + getSimpleClassName(newPackageName);
		} else if (newPackageDepth == expectedSuperDepth) {
			if (!newPackageName.equals(expectedSuperPackageName) && actualSuperDepth >= expectedSuperDepth) {
				adjustedPackageName = getPackageName(actualSuperPackageName) + "." + getSimpleClassName(newPackageName);
			} else {
				adjustedPackageName = actualSuperPackageName;
			}
		} else if (newPackageDepth < expectedSuperDepth) {
			adjustedPackageName = actualSuperPackageName;
		} else {
			int dotIndex = 0;

			for (int superSegmentIndex = 0; superSegmentIndex < expectedSuperDepth; superSegmentIndex++) {
				dotIndex = newPackageName.indexOf(46, dotIndex);
			}

			adjustedPackageName = actualSuperPackageName + "." + newPackageName.substring(dotIndex + 1);
		}

		return adjustedPackageName;
	}

	private static MultiMap<String, FlowObfuscationEntry> groupByOriginalClassName(Set<FlowObfuscationEntry> flowObfuscationEntries) {
		MultiMap<String, FlowObfuscationEntry> entriesByOriginalClassName = new MultiMap<>(flowObfuscationEntries.size());

		for (FlowObfuscationEntry entry : flowObfuscationEntries) {
			entriesByOriginalClassName.put(entry.getOriginalClassName(), entry);
		}

		return entriesByOriginalClassName;
	}

	public static String buildFileLocationHint(String filePath, ScriptContext scriptContext) {
		File changeLogFile = new File(filePath);
		if (TempFileManager.isManagedTempFile(changeLogFile)) {
			String originalFilePath = TempFileManager.getOriginalName(changeLogFile.getAbsolutePath());
			if (originalFilePath != null) {
				return " : Original file is \"" + new File(originalFilePath).getAbsolutePath() + "\"";
			} else {
				File defaultDirectory = scriptContext.getDefaultDirectory();
				File savedChangeLogFile = new File(defaultDirectory, "ZKM_saved_ChangeLog.txt");
				if (savedChangeLogFile.exists()) {
					savedChangeLogFile.delete();
				}

				try {
					FileUtils.copyFile(filePath, savedChangeLogFile.getAbsolutePath());
					return " : Contents saved to \"" + savedChangeLogFile.getAbsolutePath() + "\"";
				} catch (IOException ignoredCopy) {
					return "";
				}
			}
		} else {
			return "";
		}
	}

	public static String getLogFileEncoding(File changeLogFile) {
		BufferedReader reader = null;
		AtomicReference<String> detectedEncoding = new AtomicReference<>(null);
		String firstLine = null;

		try {
			reader = FileUtils.createBufferedReader(changeLogFile, detectedEncoding);
			firstLine = reader.readLine();
		} catch (IOException ignoredRead) {
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignoredClose) {
				}
			}
		}

		if (firstLine != null) {
			StringTokenizer tokenizer = new StringTokenizer(firstLine);

			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.startsWith(encodingPrefix)) {
					if (token.length() > encodingPrefix.length() + 2) {
						String encodingValue = token.substring(encodingPrefix.length() + 1, token.length() - 1).trim();
						if (encodingValue.trim().length() > 0) {
							return encodingValue.trim();
						}
					}
					break;
				}
			}
		}

		return detectedEncoding.get();
	}

	public boolean hasNewPackageName(String packageName) {
		return this.getNewPackageName(packageName) != null;
	}

	public String getOldTypeName(String typeName) {
		if (this.newToOldClassNameMap == null) {
			return typeName;
		} else {
			int arrayBracketIndex = typeName.indexOf(91);
			String baseTypeName;
			String arraySuffix;
			if (arrayBracketIndex > -1) {
				baseTypeName = typeName.substring(0, arrayBracketIndex);
				arraySuffix = typeName.substring(arrayBracketIndex);
			} else {
				baseTypeName = typeName;
				arraySuffix = "";
			}

			String oldTypeName = this.newToOldClassNameMap.get(GlobalUtils.internalToBinaryName(baseTypeName));
			return oldTypeName == null ? typeName : oldTypeName + arraySuffix;
		}
	}

	public void initKeyTypes() {
		if (this.oldToNewClassNameMap != null) {
			ArrayList<String> classNames = new ArrayList<>(this.oldToNewClassNameMap.keySet());
			Collections.sort(classNames);
			this.initFieldKeysHaveType(classNames);
			this.initMethodKeysHaveReturnType(classNames);
		}
	}

	public String getModuleAutoReflectionClass(String moduleName) {
		return this.autoReflectionClassByModule.get(moduleName);
	}

	private MethodMappingInfo createMethodMappingInfo(String newName, String parameterDescriptor) {
		MethodMappingInfo mappingInfo;
		if (this.hasParameterChangeData()) {
			String remappedParameterList = this.remapParameterList(parameterDescriptor, this.oldToNewClassNameMap);
			mappingInfo = new MethodMappingInfo(newName, remappedParameterList);
		} else {
			mappingInfo = new MethodMappingInfo(newName);
		}

		return mappingInfo;
	}

	public List<String> getOldMethodSignatures(String className, String newMethodName) {
		if (this.newToOldMethodNames == null) {
			this.buildNewToOldMethodNames();
		}

		ArrayList<String> methodSignatures = new ArrayList<>();
		if (this.newToOldMethodNames != null) {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			HierarchicalMultiMap<ComparablePair<String, String>, String, String> oldMethodNamesByMethodKey = this.newToOldMethodNames.get(binaryClassName);
			Enumeration<ComparablePair<String, String>> methodKeyEnumeration = oldMethodNamesByMethodKey.keys();

			while (methodKeyEnumeration.hasMoreElements()) {
				ComparablePair<String, String> methodKey = methodKeyEnumeration.nextElement();
				if (methodKey.getKey().equals(newMethodName)) {
					MultiMap<String, String> oldMethodNamesByParameterList = oldMethodNamesByMethodKey.get(methodKey);
					Enumeration<String> parameterListEnumeration = oldMethodNamesByParameterList.keys();

					while (parameterListEnumeration.hasMoreElements()) {
						String parameterList = parameterListEnumeration.nextElement();
						String remappedParameterList = this.remapParameterList(parameterList, this.newToOldClassNameMap);

						for (String methodName : oldMethodNamesByParameterList.get(parameterList)) {
							methodSignatures.add(methodName + "(" + remappedParameterList + ")");
						}
					}
				}
			}
		}

		return methodSignatures;
	}

	public void logSpecialError(String message) {
		this.hasError = true;
		this.scriptContext.logSpecialError("Input change log '" + this.logFileName + "': " + message + this.getFileLocationHint());
	}

	public ArrayList<String> getOldPackageNames() {
		ArrayList<String> oldPackageNames = new ArrayList<>(this.packageMappingCount);
		if (this.oldToNewPackageNameMap != null) {
			for (String oldPackageName : this.oldToNewPackageNameMap.keySet()) {
				oldPackageNames.add(GlobalUtils.binaryToInternalName(oldPackageName));
			}
		}

		Collections.sort(oldPackageNames);
		return oldPackageNames;
	}

	public void markParsed() {
		this.parsed = true;
	}

	private void initMethodKeysHaveReturnType(List<String> classNames) {
		if (this.rawMethodMappings != null) {
			boolean firstMethodEntry = true;
			int classNameCount = classNames.size();

			for (int classNameIndex = 0; classNameIndex < classNameCount; classNameIndex++) {
				String className = classNames.get(classNameIndex);
				MultiMap<MethodKey, MethodMappingInfo> mappingsByMethodKey = this.rawMethodMappings.get(className);
				if (mappingsByMethodKey != null) {
					ReadOnlyList<MethodKey> methodKeys = new ReadOnlyList<>(mappingsByMethodKey.keys());
					Collections.sort(methodKeys);

					for (int methodKeyIndex = 0; methodKeyIndex < methodKeys.size(); methodKeyIndex++) {
						MethodKey methodKey = methodKeys.get(methodKeyIndex);
						String methodName = methodKey.getName();
						String methodDescriptor = methodKey.getDescriptor();
						if (firstMethodEntry) {
							firstMethodEntry = false;
							this.methodKeysHaveReturnType = methodDescriptor != null;
						} else if (this.methodKeysHaveReturnType != (methodDescriptor != null)) {
							this.logSpecialError(
								"Method \""
									+ methodName
									+ "("
									+ methodKey.getParameterDescriptor()
									+ ")\" in class \""
									+ className
									+ "\" "
									+ (this.methodKeysHaveReturnType ? "has no return type" : "has a return type")
									+ " which is inconsistent with the previous method entries. All method entries must have return types or no method entries can have return types."
							);
						}
					}
				}
			}
		}
	}

	public boolean isSimpleNameChanged(String className) {
		String newClassName = this.getNewClassName(className);
		if (newClassName != null) {
			String oldSimpleName = getLastSegment(className, 47);
			String newSimpleName = getLastSegment(newClassName, 47);
			return !newSimpleName.equals(oldSimpleName);
		} else {
			return false;
		}
	}

	@Override
	public void addClassMapping(String newClassName, String oldClassName) {
		if (this.oldToNewClassNameMap == null) {
			int initialCapacity = GlobalUtils.getInitialCapacity(this.classMappingCount);
			this.oldToNewClassNameMap = GlobalUtils.newHashMap(initialCapacity);
			this.newToOldClassNameMap = GlobalUtils.newHashMap(initialCapacity);
		}

		if (this.oldToNewClassNameMap.put(oldClassName, newClassName) != null) {
			this.logSpecialError("Class \"" + oldClassName + "\" appears more than once.");
		}

		String previousOldClassName = this.newToOldClassNameMap.put(newClassName, oldClassName);
		if (previousOldClassName != null) {
			this.logSpecialError("Classes \"" + previousOldClassName + "\" and \"" + oldClassName + "\" are both mapped to \"" + newClassName + "\"");
		}
	}

	public MultiMap<MethodKey, MethodMappingInfo> getRawMethodMappings(String className) {
		if (this.rawMethodMappings != null) {
			MultiMap<MethodKey, MethodMappingInfo> mappingsByMethodKey = this.rawMethodMappings.get(className);
			return mappingsByMethodKey == null ? null : new MultiMap<>(mappingsByMethodKey);
		} else {
			return null;
		}
	}

	public boolean hasNewMethodName(String className, MethodSignature methodSignature) {
		return this.getNewMethodName(className, methodSignature) != null;
	}

	private void addProcessedFieldMapping(String classNameInLog, String className, FieldSignature fieldSignature, FieldSignature newFieldSignature, Map<String, String> classNameMappings) {
		if (this.processedFieldMappings.put(className, fieldSignature, newFieldSignature) != null) {
			this.logSpecialError("Field \"" + fieldSignature.toRemappedString(classNameMappings) + "\" appears more than once in class \"" + classNameInLog + "\"");
		}

		FieldSignature previousFieldSignature = this.processedFieldMappingsReverse.put(className, newFieldSignature, fieldSignature);
		if (previousFieldSignature != null) {
			this.logSpecialError(
				"Fields \""
					+ previousFieldSignature.toRemappedString(classNameMappings)
					+ "\" and \""
					+ fieldSignature.toRemappedString(classNameMappings)
					+ "\" are both mapped to \""
					+ newFieldSignature.toRemappedString(classNameMappings)
					+ "\" in class \""
					+ classNameInLog
					+ "\""
			);
		}
	}

	@Override
	public boolean isLineNumberChangeSupported() {
		return true;
	}

	public boolean hasNewClass(String newClassName) {
		return this.newToOldClassNameMap != null && this.newToOldClassNameMap.containsKey(GlobalUtils.internalToBinaryName(newClassName));
	}

	public ExtraParameter[] getExtraParameters(AbstractMethodNode method) {
		return this.methodParameterChangeMap == null ? null : this.methodParameterChangeMap.get(method);
	}

	public List<String> getClassOldMethodSignatures(String oldClassName) {
		ArrayList<String> methodSignatures = new ArrayList<>();
		if (this.rawMethodMappings != null) {
			MultiMap<MethodKey, MethodMappingInfo> mappingsByMethodKey = this.rawMethodMappings.get(oldClassName);
			if (mappingsByMethodKey != null) {
				StringBuilder signatureBuilder = new StringBuilder();
				Enumeration<MethodKey> methodKeyEnumeration = mappingsByMethodKey.keys();

				while (methodKeyEnumeration.hasMoreElements()) {
					MethodKey methodKey = methodKeyEnumeration.nextElement();
					signatureBuilder.setLength(0);
					signatureBuilder.append(methodKey.getDescriptor());
					signatureBuilder.append(" ");
					signatureBuilder.append(methodKey.getName());
					signatureBuilder.append('(');
					signatureBuilder.append(methodKey.getParameterDescriptor());
					signatureBuilder.append(')');
					methodSignatures.add(signatureBuilder.toString());
				}
			}
		}

		return methodSignatures;
	}

	private void addProcessedMethodMapping(
		String classNameInLog,
		String className,
		String parameterDescriptor,
		MethodSignature methodSignature,
		FlaggedMethodSignature newMethodSignature,
		Map<String, String> classNameMappings,
		DualKeyMap<String, FlaggedMethodSignature, MethodSignature> newToOldMethodSignatures,
		DualKeyMap<String, MethodSignature, MethodSignature> oldToNewMethodSignatures,
		DualKeyMap<String, MethodSignature, ExtraParameter[]> extraParametersBySignature
	) {
		if (this.processedMethodMappings.put(className, methodSignature, newMethodSignature) != null) {
			this.logSpecialError("Method '" + methodSignature.getSignature(classNameMappings) + "' appears more than once in class '" + classNameInLog + "'");
		}

		MethodSignature previousMethodSignature = newToOldMethodSignatures.put(className, newMethodSignature, methodSignature);
		String remappedParameters = methodSignature.getRemappedParameters(classNameMappings);
		if (previousMethodSignature != null) {
			this.logSpecialError(
				"Methods '"
					+ previousMethodSignature.getName()
					+ remappedParameters
					+ "' and '"
					+ methodSignature.getName()
					+ remappedParameters
					+ "' are both mapped to the same name '"
					+ newMethodSignature.getName()
					+ "' in class '"
					+ classNameInLog
					+ "'"
			);
		}

		if (oldToNewMethodSignatures != null) {
			MethodSignature extraParameterSignature = this.applyExtraParameters(newMethodSignature, GlobalUtils.binaryToInternalName(className), extraParametersBySignature);
			MethodSignature previousExtraParameterSignature = oldToNewMethodSignatures.put(className, extraParameterSignature, methodSignature);
			if (previousExtraParameterSignature != null) {
				this.logSpecialError(
					"Methods '"
						+ previousExtraParameterSignature.getSignature(classNameMappings)
						+ "' and '"
						+ methodSignature.getSignature(classNameMappings)
						+ "' are both mapped to '"
						+ newMethodSignature.toFullString()
						+ "' in class '"
						+ classNameInLog
						+ "'"
				);
			}
		}
	}

	public ArrayList<String> getNewClassNames() {
		ArrayList<String> newClassNames = new ArrayList<>(this.classMappingCount);
		if (this.newToOldClassNameMap != null) {
			SnapshotEnumerator<String> classNameEnumeration = new SnapshotEnumerator<>(this.newToOldClassNameMap.keySet());

			while (classNameEnumeration.hasMoreElements()) {
				String className = classNameEnumeration.nextElement();
				newClassNames.add(GlobalUtils.binaryToInternalName(className));
			}
		}

		return newClassNames;
	}

	private FieldKey createNormalizedFieldKey(String name, String descriptor, int accessFlags) {
		String fieldDescriptor;
		if (TypeWriter.isValidFieldDescriptor(descriptor)) {
			fieldDescriptor = TypeWriter.descriptorToHumanReadable(descriptor);
		} else {
			fieldDescriptor = descriptor;
		}

		FieldKey fieldKey;
		if (this.fieldKeysHaveType) {
			fieldKey = new FieldKey(name, fieldDescriptor, accessFlags);
		} else {
			fieldKey = new FieldKey(name, null, accessFlags);
		}

		return fieldKey;
	}

	public String getOldClassName(String newClassName) {
		if (this.newToOldClassNameMap == null) {
			return null;
		} else {
			String oldClassName = this.newToOldClassNameMap.get(GlobalUtils.internalToBinaryName(newClassName));
			return oldClassName == null ? null : GlobalUtils.binaryToInternalName(oldClassName);
		}
	}

	private ComparablePair<String, String> createMethodNameKey(String methodName, String returnType) {
		return new ComparablePair<>(methodName, this.methodKeysHaveReturnType ? returnType : null);
	}

	public boolean hasFlowObfuscationData() {
		return this.flowObfuscationEntries != null && this.flowObfuscationEntries.size() > 0;
	}

	public void checkPackageMappings() {
		if (this.oldToNewClassNameMap != null) {
			HashMap<String, String> newPackageByOldPackage = GlobalUtils.newHashMap();

			for (Entry<String, String> classMappingEntry : this.oldToNewClassNameMap.entrySet()) {
				String oldClassName = classMappingEntry.getKey();
				String oldPackageName = getPackageName(oldClassName);
				String newClassName = classMappingEntry.getValue();
				String newPackageName = getPackageName(newClassName);
				String expectedNewPackageName = null;
				if (this.oldToNewPackageNameMap != null) {
					expectedNewPackageName = this.oldToNewPackageNameMap.get(oldPackageName);
				}

				if (oldPackageName.length() > 0) {
					if (oldPackageName.equals(newPackageName)) {
						if (expectedNewPackageName != null) {
							if (!newPackageName.equals(expectedNewPackageName)) {
								this.logSpecialError(
									"Class \"" + oldClassName + "\" is remapped to \"" + newClassName + "\" but package '" + oldPackageName + "\" should be remapped to \"" + expectedNewPackageName + "\" (1)"
								);
							}
						} else {
							String previousNewPackageName = newPackageByOldPackage.put(oldPackageName, newPackageName);
							if (previousNewPackageName != null && !newPackageName.equals(previousNewPackageName)) {
								this.logSpecialError(
									"Class \""
										+ oldClassName
										+ "\" is remapped to \""
										+ newClassName
										+ "\" but the package remapping is inconsistent with that of another class : \""
										+ newPackageName
										+ " != "
										+ previousNewPackageName
										+ "\"."
								);
							}
						}
					} else if (expectedNewPackageName != null) {
						if (!newPackageName.equals(expectedNewPackageName)) {
							this.logSpecialError("Class \"" + oldClassName + "\" is remapped to \"" + newClassName + "\" but package '" + oldPackageName + "\" should be remapped to \"" + expectedNewPackageName + "\" (2)");
						}
					} else {
						this.logSpecialError("Class \"" + oldClassName + "\" is remapped to \"" + newClassName + "\" but there is no corresponding packge level remapping.");
					}
				} else if (newPackageName.length() > 0) {
					this.logSpecialError("Class \"" + oldClassName + "\" is remapped to \"" + newClassName + "\" but current version cannot handle remapping of default package.");
				}
			}

			Entry<String, String>[] packageEntries = new Entry[newPackageByOldPackage.size()];
			newPackageByOldPackage.entrySet().toArray(packageEntries);
			ChangeLogContext.EntryKeyComparator entryKeyComparator = new ChangeLogContext.EntryKeyComparator(this);
			Arrays.sort(packageEntries, entryKeyComparator);

			for (int entryIndex = 0; entryIndex < packageEntries.length; entryIndex++) {
				this.addPackageNameMapping(packageEntries[entryIndex].getValue(), packageEntries[entryIndex].getKey(), false);
			}
		}
	}

	public void checkNameClashes() {
		if (this.oldToNewClassNameMap != null && this.oldToNewPackageNameMap != null && this.scriptContext.hasLoadedClasses()) {
			for (Entry<String, String> classMappingEntry : this.oldToNewClassNameMap.entrySet()) {
				String oldClassName = classMappingEntry.getKey();
				String newClassName = classMappingEntry.getValue();
				if (this.newToOldPackageNameMap.containsGroup(newClassName)) {
					Map<String, String> packageRemappingGroup = this.newToOldPackageNameMap.getGroup(newClassName);
					ArrayList<String> oldPackageNames = new ArrayList<>(packageRemappingGroup.keySet());
					Collections.sort(oldPackageNames);
					this.logSpecialError(
						"Class \""
							+ oldClassName
							+ "\" and package \""
							+ oldPackageNames.get(0)
							+ "\" are both remapped to \""
							+ newClassName
							+ "\". This could cause problems because some classes have been opened from the file system."
					);
				}
			}

			if (this.autoReflectionClassName != null && this.newToOldClassNameMap.containsKey(this.autoReflectionClassName)) {
				this.logSpecialError(
					"Class \""
						+ this.autoReflectionClassName
						+ "\" appears as the AutoReflection lookup class but it also appears as a new class name for the class '"
						+ this.newToOldClassNameMap.get(this.autoReflectionClassName)
						+ "'."
				);
			}

			if (this.obfuscateReferencesClassName != null && this.newToOldClassNameMap.containsKey(this.obfuscateReferencesClassName)) {
				this.logSpecialError(
					"Class \""
						+ this.obfuscateReferencesClassName
						+ "\" appears as the Reference Obfuscation lookup class but it also appears as a new class name for the class '"
						+ this.newToOldClassNameMap.get(this.obfuscateReferencesClassName)
						+ "'."
				);
			}
		}
	}

	public String getNewMethodName(String className, String methodName, String[] parameterTypes, String returnType) {
		if (this.rawMethodMappings != null) {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			String parameterList = this.formatParameterList(parameterTypes);
			MethodKey methodKey = this.createMethodKey(methodName, parameterList, returnType, 0);
			List<MethodMappingInfo> mappingInfos = this.rawMethodMappings.getValues(binaryClassName, methodKey);
			if (mappingInfos != null && mappingInfos.size() > 0) {
				if (mappingInfos.size() == 1) {
					MethodMappingInfo mappingInfo = mappingInfos.get(0);
					return !methodName.equals(mappingInfo.getNewName()) ? mappingInfo.getNewName() : null;
				} else {
					this.logSpecialError("Class '" + binaryClassName + "' has multiple methods '" + methodName + "(" + parameterList + ")'.");
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public boolean isLooseChangeLog() {
		return this.loose;
	}

	@Override
	public void addInnerClassMappings(String className, Map<Integer, Integer> lineNumberMappings) {
		if (this.lineNumberMappings == null) {
			this.lineNumberMappings = new HierarchicalMultiMap<>(this.classMappingCount);
		}

		MultiMap<Integer, Integer> reversedLineNumberMappings = new MultiMap<>(lineNumberMappings.size(), 3);

		for (Entry<Integer, Integer> lineNumberEntry : lineNumberMappings.entrySet()) {
			reversedLineNumberMappings.put(lineNumberEntry.getValue(), lineNumberEntry.getKey());
		}

		this.lineNumberMappings.put(className, reversedLineNumberMappings);
	}

	@Override
	public void addTraceBackClass(String className, List<String> data) {
		if (!this.hasError) {
			this.parseFlowObfuscationEntry(className, data, true, "TraceBackClass:", null);
		}
	}

	public String getNewFieldName(String className, String fieldName, String fieldType) {
		return this.lookupNewFieldName(className, fieldName, fieldType, this.rawFieldMappings);
	}

	public boolean hasNewPackageNameGroup(String packageName) {
		return this.newToOldPackageNameMap != null && this.newToOldPackageNameMap.containsGroup(GlobalUtils.internalToBinaryName(packageName));
	}

	public String getNewMethodSignatureString(String className, String methodName, String[] parameterTypes, String returnType) {
		if (this.rawMethodMappings == null) {
			return null;
		} else {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			String parameterList = this.formatParameterList(parameterTypes);
			MethodKey methodKey = this.createMethodKey(methodName, parameterList, returnType, 0);
			List<MethodMappingInfo> mappingInfos = this.rawMethodMappings.getValues(binaryClassName, methodKey);
			if (mappingInfos != null && mappingInfos.size() > 0) {
				if (mappingInfos.size() != 1) {
					this.logSpecialError("Class '" + binaryClassName + "' has multiple methods '" + methodName + "(" + parameterList + ")'.");
					return null;
				} else {
					MethodMappingInfo mappingInfo = mappingInfos.get(0);
					StringBuilder parameterListBuilder = new StringBuilder();

					for (int parameterIndex = 0; parameterIndex < parameterTypes.length; parameterIndex++) {
						parameterListBuilder.append(parameterTypes[parameterIndex]);
						if (parameterIndex < parameterTypes.length - 1) {
							parameterListBuilder.append(", ");
						}
					}

					String effectiveParameterList = mappingInfo.getEffectiveParameterList();
					if (!methodName.equals(mappingInfo.getNewName()) || mappingInfo.isParameterChangeDeclared() || effectiveParameterList != null && !effectiveParameterList.equals(parameterListBuilder.toString())) {
						StringBuilder signatureBuilder = new StringBuilder();
						String returnDescriptor = methodKey.getDescriptor();
						if (returnDescriptor != null && returnDescriptor.length() > 0) {
							signatureBuilder.append(this.getOldTypeName(returnDescriptor));
							signatureBuilder.append(' ');
						}

						signatureBuilder.append(mappingInfo.getNewName());
						signatureBuilder.append('(');
						signatureBuilder.append(effectiveParameterList);
						signatureBuilder.append(')');
						return signatureBuilder.toString();
					} else {
						return null;
					}
				}
			} else {
				return null;
			}
		}
	}

	public boolean hasMethodMapping(String className, String methodName, String[] parameterTypes, String returnType) {
		if (this.rawMethodMappings != null) {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			String parameterList = this.formatParameterList(parameterTypes);
			MethodKey methodKey = this.createMethodKey(methodName, parameterList, returnType, 0);
			return this.rawMethodMappings.containsInnerKey(binaryClassName, methodKey);
		} else {
			return false;
		}
	}

	public void buildParameterChangeData(ClassProvider classProvider) {
		if (this.rawMethodMappings != null && this.hasParameterChangeData()) {
			this.methodEncryptionKeys = GlobalUtils.newHashMap();
			this.methodParameterChangeMap = GlobalUtils.newHashMap();
			this.classEncryptionKeys = GlobalUtils.newHashMap();
			this.methodSaltValues = GlobalUtils.newHashMap();
			this.classSaltValues = GlobalUtils.newHashMap();
			AtomicReference<ExtraParameter[]> extraParametersReference = new AtomicReference<>();

			for (Entry<String, MultiMap<MethodKey, MethodMappingInfo>> classMappingEntry : this.rawMethodMappings.entrySet()) {
				String binaryClassName = classMappingEntry.getKey();
				String internalClassName = GlobalUtils.binaryToInternalName(binaryClassName);
				AbstractClassNode classNode = classProvider.getClassNode(internalClassName);

				for (Entry<MethodKey, List<MethodMappingInfo>> methodMappingEntry : classMappingEntry.getValue().entrySet()) {
					MethodKey methodKey = methodMappingEntry.getKey();
					String returnType = MethodSignature.fromHumanReadableType(methodKey.getDescriptor());
					String methodName = methodKey.getName();
					MethodSignature methodSignature = new MethodSignature(methodName, MethodSignature.fromHumanReadableParameters(methodKey.getParameterDescriptor()), returnType);
					MethodMappingInfo mappingInfo = methodMappingEntry.getValue().get(0);
					AbstractMethodNode methodNode = classNode.findMethod(methodSignature);
					if (methodNode != null || methodSignature.isStaticInitializer()) {
						String newClassName = GlobalUtils.binaryToInternalName(GlobalUtils.getMappedOrDefault(binaryClassName, this.oldToNewClassNameMap));
						if (mappingInfo.hasParameterChangeData()) {
							String parameterChangeData = mappingInfo.getParameterChangeData();
							extraParametersReference.setNull();
							Long encryptionKey = AbstractChangeLogContext.decodeParameterChangeData(
								parameterChangeData, MethodSignature.fromHumanReadableParameters(mappingInfo.getNewParametersDescriptor()), extraParametersReference, newClassName
							);
							if (methodNode != null) {
								this.methodEncryptionKeys.put(methodNode, encryptionKey);
								this.methodParameterChangeMap.put(methodNode, extraParametersReference.get());
							} else {
								this.classEncryptionKeys.put(classNode, encryptionKey);
							}
						} else if (methodNode != null) {
							this.methodParameterChangeMap.put(methodNode, ExtraParameter.EMPTY_ARRAY);
						}

						if (mappingInfo.hasParameterChangeLookupData() && Options.antiDebugger) {
							this.setParameterChangeLookupData(true);
							String parameterChangeLookupData = mappingInfo.getParameterChangeLookupData();
							AtomicLong saltValue0 = new AtomicLong();
							AtomicLong saltValue1 = new AtomicLong();
							AtomicLong saltValue2 = new AtomicLong();
							AtomicLong saltValue3 = new AtomicLong();
							AtomicLong saltValue4 = new AtomicLong();
							AbstractChangeLogContext.decodeSaltValues(parameterChangeLookupData, saltValue0, saltValue1, saltValue2, saltValue3, saltValue4, newClassName);
							long[] saltValues = new long[]{saltValue0.get(), saltValue1.get(), saltValue2.get(), saltValue3.get(), saltValue4.get()};
							if (methodNode != null) {
								this.methodSaltValues.put(methodNode, saltValues);
							} else {
								this.classSaltValues.put(classNode, saltValues);
							}
						}
					}
				}
			}
		}
	}

	private void mergeFlowObfuscationReferences(ChangeLogContext other, DualKeyMap<String, IdentityReference, IdentityReference> referenceMappings) {
		if (this.flowObfuscationReferenceGraph == null) {
			this.flowObfuscationReferenceGraph = other.flowObfuscationReferenceGraph.deepClone();
		} else {
			Enumeration<String> packageNameEnumeration = other.flowObfuscationReferenceGraph.getMembersAsEnumeration();

			while (packageNameEnumeration.hasMoreElements()) {
				String packageName = packageNameEnumeration.nextElement();
				Set<FlowObfuscationEntry> otherReferences = other.flowObfuscationReferenceGraph.getReferences(packageName);
				if (this.flowObfuscationReferenceGraph.hasMember(packageName)) {
					ArrayList<FlowObfuscationEntry> otherEntries = new ArrayList<>(otherReferences);
					Collections.sort(otherEntries);
					String lastOriginalClassName = null;
					MultiMap<String, FlowObfuscationEntry> entriesByOriginalClassName = groupByOriginalClassName(this.flowObfuscationReferenceGraph.getReferences(packageName));
					ArrayList<FlowObfuscationEntry> candidateEntries = null;

					for (int i = 0; i < otherEntries.size(); i++) {
						FlowObfuscationEntry otherEntry = otherEntries.get(i);
						String originalClassName = otherEntry.getOriginalClassName();
						if (lastOriginalClassName == null || !originalClassName.equals(lastOriginalClassName)) {
							lastOriginalClassName = originalClassName;
							List<FlowObfuscationEntry> entriesForOriginalClassName = entriesByOriginalClassName.get(originalClassName);
							if (entriesForOriginalClassName != null) {
								candidateEntries = new ArrayList<>(entriesForOriginalClassName);
								Collections.sort(candidateEntries);
							} else {
								candidateEntries = null;
							}
						}

						if (candidateEntries == null || candidateEntries.size() <= 0) {
							this.flowObfuscationReferenceGraph.addReference(packageName, otherEntry);
						} else if (otherEntry.getOriginalFieldName() == null) {
							FlowObfuscationEntry matchedEntry = candidateEntries.remove(0);
							referenceMappings.put(packageName, new IdentityReference(otherEntry), new IdentityReference(matchedEntry));
						} else {
							FlowObfuscationEntry thisEntry = null;
							int candidateIndex = candidateEntries.indexOf(otherEntry);
							if (candidateIndex > -1) {
								thisEntry = candidateEntries.remove(candidateIndex);
							}

							if (thisEntry != null) {
								if (!thisEntry.getOriginalFieldDescriptor().equals(otherEntry.getOriginalFieldDescriptor())) {
									this.logError(
										"Field '"
											+ thisEntry.getOriginalFieldName()
											+ "' in class '"
											+ originalClassName
											+ "' has type '"
											+ TypeWriter.descriptorToHumanReadable(thisEntry.getOriginalFieldDescriptor())
											+ "' in this log but type '"
											+ TypeWriter.descriptorToHumanReadable(otherEntry.getOriginalFieldDescriptor())
											+ "' in '"
											+ other.logFileName
											+ "'. Logs could not be fully merged. You must distribute this application as a whole. (2)"
									);
									if (otherEntry.hasOriginalSetter()) {
										if (thisEntry.hasOriginalSetter()) {
											if (!thisEntry.getOriginalSetterName().equals(otherEntry.getOriginalSetterName())) {
												this.logError(
													"Method '"
														+ thisEntry.getOriginalSetterName()
														+ '('
														+ thisEntry.getHumanReadableFieldType()
														+ ")' in class '"
														+ originalClassName
														+ "' in this log is inconsistent with method '"
														+ otherEntry.getOriginalSetterName()
														+ '('
														+ otherEntry.getHumanReadableFieldType()
														+ ")' in '"
														+ other.logFileName
														+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
												);
											}

											if (!thisEntry.getOriginalGetterName().equals(otherEntry.getOriginalGetterName())) {
												this.logError(
													"Method '"
														+ thisEntry.getHumanReadableFieldType()
														+ " "
														+ thisEntry.getOriginalGetterName()
														+ "()' in class '"
														+ originalClassName
														+ "' in this log is inconsistent with method '"
														+ otherEntry.getHumanReadableFieldType()
														+ " "
														+ otherEntry.getOriginalGetterName()
														+ "()' in '"
														+ other.logFileName
														+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
												);
											}

											if (thisEntry.getOriginalBooleanGetterName() != null && !thisEntry.getOriginalBooleanGetterName().equals(otherEntry.getOriginalBooleanGetterName())) {
												this.logError(
													"Method '"
														+ thisEntry.getHumanReadableFieldType()
														+ " "
														+ thisEntry.getOriginalBooleanGetterName()
														+ "()' in class '"
														+ originalClassName
														+ "' in this log is inconsistent with method '"
														+ otherEntry.getHumanReadableFieldType()
														+ " "
														+ otherEntry.getOriginalBooleanGetterName()
														+ "()' in '"
														+ other.logFileName
														+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
												);
											}

											if (thisEntry.getProcessingFlag() != otherEntry.getProcessingFlag()) {
												this.logError(
													"Field '"
														+ thisEntry.getOriginalFieldName()
														+ "' in class '"
														+ originalClassName
														+ "' in this log is inconsistent in nature with the equivalent field in '"
														+ other.logFileName
														+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
												);
											}
										} else {
											thisEntry.setOriginalSetterName(otherEntry.getOriginalSetterName());
											thisEntry.setOriginalGetterName(otherEntry.getOriginalGetterName());
											thisEntry.setOriginalBooleanGetterName(otherEntry.getOriginalBooleanGetterName());
											thisEntry.setRemappedSetterName(otherEntry.getRemappedSetterName());
											thisEntry.setRemappedGetterName(otherEntry.getRemappedGetterName());
											thisEntry.setRemappedBooleanGetterName(otherEntry.getRemappedBooleanGetterName());
											thisEntry.setProcessingFlag(otherEntry.getProcessingFlag());
										}
									}
								}

								referenceMappings.put(packageName, new IdentityReference(otherEntry), new IdentityReference(thisEntry));
							} else {
								Iterator<FlowObfuscationEntry> candidateIterator = candidateEntries.iterator();

								while (candidateIterator.hasNext()) {
									FlowObfuscationEntry candidateEntry = candidateIterator.next();
									if (candidateEntry.getOriginalFieldName() == null) {
										thisEntry = candidateEntry;
										candidateIterator.remove();
									}
								}

								if (thisEntry != null) {
									this.flowObfuscationReferenceGraph.removeReference(packageName, thisEntry);
									this.flowObfuscationReferenceGraph.addReference(packageName, otherEntry);
									referenceMappings.put(packageName, new IdentityReference(thisEntry), new IdentityReference(otherEntry));
								} else {
									this.flowObfuscationReferenceGraph.addReference(packageName, otherEntry);
								}
							}
						}
					}
				} else {
					this.flowObfuscationReferenceGraph.addReferences(packageName, otherReferences);
				}
			}
		}
	}

	public Enumeration<FlowObfuscationEntry> getFlowObfuscationEntries() {
		ArrayList<FlowObfuscationEntry> sortedEntries = new ArrayList<>(this.flowObfuscationEntries);
		Collections.sort(sortedEntries);
		return Collections.enumeration(sortedEntries);
	}

	public void applyParameterChangeExcludes(ParameterChangerExcludes parameterExcludes, Set<MethodNode> eligibleMethods, Map<String, String> classNameMappings, AbstractWorkspace workspace) {
		if (this.methodParameterChangeMap != null) {
			boolean parameterChangeSuppressed = false;
			HashMap<AbstractMethodNode, ExtraParameter[]> newParameterChangeMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.methodParameterChangeMap.size()));

			for (Entry<AbstractMethodNode, ExtraParameter[]> parameterChangeEntry : this.methodParameterChangeMap.entrySet()) {
				AbstractMethodNode method = parameterChangeEntry.getKey();
				ExtraParameter[] extraParameters = parameterChangeEntry.getValue();
				if (method.isWorkspaceNode()) {
					if (!parameterExcludes.isMethodExcluded((MethodNode)method)) {
						if (extraParameters.length > 0) {
							newParameterChangeMap.put(method, ExtraParameter.EMPTY_ARRAY);
							MethodSignature changedMethodSignature = buildChangedMethodSignature(method, extraParameters);
							this.logWarning(
								"Method '"
									+ method.getAccessFlagsString()
									+ " "
									+ method.getOriginalNameWithParams()
									+ "' in class '"
									+ method.getClassNameBinary()
									+ "' could not have its parameter list changed to '"
									+ changedMethodSignature.toHumanReadableString()
									+ "' because it has been excluded and the input change log is specified as loose. Its parameter list will not be changed."
							);
							parameterChangeSuppressed = true;
						} else {
							newParameterChangeMap.put(method, extraParameters);
						}
					} else if (extraParameters.length == 0) {
						this.logWarning(
							"Method '"
								+ method.getAccessFlagsString()
								+ " "
								+ method.getOriginalNameWithParams()
								+ "' in class '"
								+ method.getClassNameBinary()
								+ "' appears in the change log as not having its parameter list changed but it has not been excluded from parameter list changing. The method may have its parameter list changed because the change log is specified as loose. (A)"
						);
						parameterChangeSuppressed = true;
					} else {
						newParameterChangeMap.put(method, extraParameters);
					}
				} else {
					newParameterChangeMap.put(method, extraParameters);
				}
			}

			if (parameterChangeSuppressed) {
				Map<AbstractMethodNode, ExtraParameter[]> oldParameterChangeMap = this.methodParameterChangeMap;
				this.methodParameterChangeMap = newParameterChangeMap;
				oldParameterChangeMap.clear();
				this.validateParameterChanges(classNameMappings);
			}
		} else {
			for (MethodNode eligibleMethod : eligibleMethods) {
				this.logWarning(
					"Method '"
						+ eligibleMethod.getAccessFlagsString()
						+ " "
						+ eligibleMethod.getOriginalNameWithParams()
						+ "' in class '"
						+ eligibleMethod.getClassNameBinary()
						+ "' appears in the change log as not having its parameter list changed but it has not been excluded from parameter list changing. The method may have its parameter list changed because the change log is specified as loose. (B)"
				);
			}
		}
	}

	public boolean isInjectedClassName(String className) {
		String binaryClassName = GlobalUtils.internalToBinaryName(className);
		return this.autoReflectionClassName != null && this.autoReflectionClassName.equals(binaryClassName)
			|| this.obfuscateReferencesClassName != null && this.obfuscateReferencesClassName.equals(binaryClassName);
	}

	public String getModuleObfuscateReferencesClass(String moduleName) {
		return this.obfuscateReferencesByModule.get(moduleName);
	}

	public MethodSignature getNewMethodSignature(String className, MethodSignature methodSignature) {
		if (this.processedMethodMappings == null) {
			return null;
		} else {
			FlaggedMethodSignature flaggedMethodSignature = this.processedMethodMappings.get(GlobalUtils.internalToBinaryName(className), methodSignature);
			if (flaggedMethodSignature != null) {
				return flaggedMethodSignature.getFlag()
					? new MethodSignature(flaggedMethodSignature.getName(), "([Ljava/lang/Object;)", flaggedMethodSignature.getReturnDescriptor())
					: new MethodSignature(flaggedMethodSignature.getName(), flaggedMethodSignature.getParameterDescriptor(), flaggedMethodSignature.getReturnDescriptor());
			} else {
				return null;
			}
		}
	}

	public void applyDefaultExcludes(DefaultExcludes defaultExcludes) {
		if (defaultExcludes != null) {
			HashMap<String, String> updatedOldToNewPackageNameMap = null;
			DualKeyMap<String, String, String> updatedNewToOldPackageNameMap = null;
			HashSet<String> excludedPackageNames = null;
			if (this.oldToNewPackageNameMap != null) {
				excludedPackageNames = GlobalUtils.newHashSet();
				updatedOldToNewPackageNameMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.oldToNewPackageNameMap.size()));
				updatedNewToOldPackageNameMap = new DualKeyMap<>(this.newToOldPackageNameMap.getGroupCount());
				ArrayList<String> oldPackageNames = new ArrayList<>(this.oldToNewPackageNameMap.keySet());
				Collections.sort(oldPackageNames);

				for (int i = 0; i < oldPackageNames.size(); i++) {
					String oldPackageName = oldPackageNames.get(i);
					String newPackageName = this.oldToNewPackageNameMap.get(oldPackageName);
					if (defaultExcludes.isPackageExcluded(GlobalUtils.binaryToInternalName(oldPackageName))) {
						if (!oldPackageName.equals(newPackageName)) {
							excludedPackageNames.add(oldPackageName);
							updatedOldToNewPackageNameMap.put(oldPackageName, oldPackageName);
							this.logWarning(
								"Package '"
									+ oldPackageName
									+ "' could not be renamed to '"
									+ newPackageName
									+ "' because it has been excluded and the input change log is specified as loose. It will not be renamed."
							);
							updatedOldToNewPackageNameMap.put(oldPackageName, oldPackageName);
							updatedNewToOldPackageNameMap.put(oldPackageName, oldPackageName, oldPackageName);
						} else {
							updatedOldToNewPackageNameMap.put(oldPackageName, newPackageName);
							updatedNewToOldPackageNameMap.put(newPackageName, oldPackageName, oldPackageName);
						}
					} else {
						String parentPackageName = getPackageName(oldPackageName);
						String newParentPackageName = this.oldToNewPackageNameMap.get(parentPackageName);
						String mappedParentPackageName = updatedOldToNewPackageNameMap.get(parentPackageName);
						if (excludedPackageNames.contains(parentPackageName)) {
							String renamedParentAdjustedName = adjustPackageName(newPackageName, newParentPackageName, mappedParentPackageName);
							if (!renamedParentAdjustedName.equals(newPackageName)) {
								excludedPackageNames.add(oldPackageName);
								this.logWarning(
									"Package '"
										+ oldPackageName
										+ "' could not be renamed to '"
										+ newPackageName
										+ "' because the mapping of its superpackage has been changed. It will be renamed to '"
										+ renamedParentAdjustedName
										+ "'"
								);
							}

							updatedOldToNewPackageNameMap.put(oldPackageName, renamedParentAdjustedName);
							updatedNewToOldPackageNameMap.put(renamedParentAdjustedName, oldPackageName, oldPackageName);
						} else if (defaultExcludes.isPackageExcluded(GlobalUtils.binaryToInternalName(parentPackageName)) && !newPackageName.startsWith(newParentPackageName + ".")) {
							String excludedParentAdjustedName = adjustPackageName(newPackageName, newParentPackageName, mappedParentPackageName);
							if (!excludedParentAdjustedName.equals(newPackageName)) {
								excludedPackageNames.add(oldPackageName);
								this.logWarning(
									"Package '" + oldPackageName + "' could not be renamed to '" + newPackageName + "' because its superpackage has been excluded. It will be renamed to '" + excludedParentAdjustedName + "'"
								);
							}

							updatedOldToNewPackageNameMap.put(oldPackageName, excludedParentAdjustedName);
							updatedNewToOldPackageNameMap.put(excludedParentAdjustedName, oldPackageName, oldPackageName);
						} else {
							updatedOldToNewPackageNameMap.put(oldPackageName, newPackageName);
							updatedNewToOldPackageNameMap.put(newPackageName, oldPackageName, oldPackageName);
						}
					}
				}
			}

			if (updatedOldToNewPackageNameMap != null) {
				this.oldToNewPackageNameMap.clear();
				this.newToOldPackageNameMap.clearAll();
				this.oldToNewPackageNameMap = updatedOldToNewPackageNameMap;
				this.newToOldPackageNameMap = updatedNewToOldPackageNameMap;
			}

			HashMap<String, String> updatedOldToNewClassNameMap = null;
			HashMap<String, String> updatedNewToOldClassNameMap = null;
			if (this.oldToNewClassNameMap != null) {
				updatedOldToNewClassNameMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.oldToNewClassNameMap.size()));
				updatedNewToOldClassNameMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.newToOldClassNameMap.size()));

				for (Entry<String, String> classNameEntry : this.oldToNewClassNameMap.entrySet()) {
					String oldClassName = classNameEntry.getKey();
					String newClassName = classNameEntry.getValue();
					String oldSimpleName = getSimpleClassName(oldClassName);
					String newSimpleName = getSimpleClassName(newClassName);
					String classPackageName = getPackageName(oldClassName);
					String mappedPackageName = null;
					if (updatedOldToNewPackageNameMap != null) {
						mappedPackageName = updatedOldToNewPackageNameMap.get(classPackageName);
					}

					if (mappedPackageName == null) {
						mappedPackageName = classPackageName;
					}

					if (excludedPackageNames != null && excludedPackageNames.contains(classPackageName)) {
						String remappedClassName;
						if (defaultExcludes.isClassNameExcluded(GlobalUtils.binaryToInternalName(oldClassName)) && !oldSimpleName.equals(newSimpleName)) {
							remappedClassName = mappedPackageName + "." + oldSimpleName;
							this.logWarning(
								"Class '"
									+ oldClassName
									+ "' could not be renamed to '"
									+ newClassName
									+ "' because it has been excluded and the mapping of its package name has been changed. It will be renamed to '"
									+ remappedClassName
									+ "'"
							);
						} else {
							remappedClassName = mappedPackageName + "." + newSimpleName;
							this.logWarning(
								"Class '"
									+ oldClassName
									+ "' could not be renamed to '"
									+ newClassName
									+ "' because the mapping of its package name has been changed. It will be renamed to '"
									+ remappedClassName
									+ "'"
							);
						}

						updatedOldToNewClassNameMap.put(oldClassName, remappedClassName);
						String clashingClassName = updatedNewToOldClassNameMap.put(remappedClassName, oldClassName);
						if (clashingClassName != null) {
							this.logFatalError("Class name clash after package and class exclusions.  Both '" + oldClassName + "' and '" + clashingClassName + "' are renamed to '" + remappedClassName + "'.");
						}
					} else {
						String adjustedClassName;
						if (defaultExcludes.isClassNameExcluded(GlobalUtils.binaryToInternalName(oldClassName)) && !oldSimpleName.equals(newSimpleName)) {
							adjustedClassName = mappedPackageName + "." + oldSimpleName;
							this.logWarning("Class '" + oldClassName + "' could not be renamed to '" + newClassName + "' because it has been excluded. It will be renamed to '" + adjustedClassName + "'");
						} else {
							adjustedClassName = newClassName;
						}

						updatedOldToNewClassNameMap.put(oldClassName, adjustedClassName);
						String duplicateClassName = updatedNewToOldClassNameMap.put(adjustedClassName, oldClassName);
						if (duplicateClassName != null) {
							this.logFatalError("Class name clash after class exclusions.  Both '" + oldClassName + "' and '" + duplicateClassName + "' are renamed to '" + adjustedClassName + "'.");
						}
					}
				}
			}

			if (updatedOldToNewClassNameMap != null) {
				this.oldToNewClassNameMap.clear();
				this.newToOldClassNameMap.clear();
				this.oldToNewClassNameMap = updatedOldToNewClassNameMap;
				this.newToOldClassNameMap = updatedNewToOldClassNameMap;
			}
		}
	}

	private void mergeMemberClassEntries(
		ChangeLogContext other, HashMap<IdentityReference, IdentityReference> entryMappings, DualKeyMap<String, IdentityReference, IdentityReference> referenceMappings
	) {
		if (this.memberClassEntries == null) {
			this.memberClassEntries = GlobalUtils.newHashMap(other.memberClassEntries);
		} else {
			for (Entry<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> otherMemberClassEntry : other.memberClassEntries.entrySet()) {
				String className = otherMemberClassEntry.getKey();
				KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry> otherEntryPair = otherMemberClassEntry.getValue();
				if (!this.memberClassEntries.containsKey(className)) {
					this.memberClassEntries.put(className, otherEntryPair);
				}
			}

			IdentityReference identityKey = new IdentityReference();

			for (Entry<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> memberClassEntry : this.memberClassEntries.entrySet()) {
				String packageName = getPackageName(memberClassEntry.getKey());
				KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry> entryPair = memberClassEntry.getValue();
				FlowObfuscationEntry keyEntry = entryPair.getKey();
				FlowObfuscationEntry valueEntry = entryPair.getValue();
				identityKey.set(keyEntry);
				IdentityReference mappedReference;
				if ((mappedReference = entryMappings.get(identityKey)) != null) {
					entryPair.setKey((FlowObfuscationEntry)mappedReference.get());
				}

				identityKey.set(valueEntry);
				if ((mappedReference = referenceMappings.get(packageName, identityKey)) != null) {
					entryPair.setValue((FlowObfuscationEntry)mappedReference.get());
				}
			}
		}
	}

	public boolean hasOldMethodName(String className, String newMethodName, String[] parameterTypes, String returnType) {
		if (this.newToOldMethodNames == null) {
			this.buildNewToOldMethodNames();
		}

		if (this.newToOldMethodNames != null) {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			ComparablePair<String, String> methodNameKey = this.createMethodNameKey(newMethodName, returnType);
			String parameterList = this.formatParameterList(parameterTypes);
			return this.newToOldMethodNames.containsInnerKey(binaryClassName, methodNameKey, parameterList);
		} else {
			return false;
		}
	}

	public ExtraParameter[] propagateParameterChange(AbstractMethodNode method, AbstractMethodNode correspondingMethod, ExtraParameter[] extraParameters) {
		ExtraParameter[] removedExtraParameters = this.methodParameterChangeMap.remove(method);
		boolean existingParameterChange = this.hasParameterChangeEntry(correspondingMethod);
		ExtraParameter[] previousExtraParameters = this.methodParameterChangeMap.put(correspondingMethod, extraParameters);
		if (existingParameterChange) {
			if (extraParameters.length == 0 && previousExtraParameters.length > 0) {
				MethodSignature correspondingSignature = buildChangedMethodSignature(correspondingMethod, extraParameters);
				String existingMappingWarning = "Method '"
					+ method.getOriginalNameWithParams()
					+ " in class '"
					+ method.getClassNameBinary()
					+ "' is specified to have no parameter list change but the corresponding method in class '"
					+ correspondingMethod.getClassNameBinary()
					+ "' is mapped to new parameter list '"
					+ correspondingSignature.toFullString()
					+ "'. Using mapping of '"
					+ correspondingSignature.toFullString()
					+ "' for both methods.";
				this.logWarning(existingMappingWarning);
				this.methodParameterChangeMap.put(correspondingMethod, previousExtraParameters);
				return previousExtraParameters;
			}

			if (extraParameters.length > 0 && previousExtraParameters.length == 0) {
				MethodSignature changedMethodSignature = buildChangedMethodSignature(method, extraParameters);
				String missingMappingWarning = "Method '"
					+ method.getOriginalNameWithParams()
					+ " in class '"
					+ method.getClassNameBinary()
					+ "' is mapped to new parameter list '"
					+ changedMethodSignature.toFullString()
					+ "' but the corresponding method in class '"
					+ correspondingMethod.getClassNameBinary()
					+ "' is specified to have no parameter list change. Using a specification of no parameter list change for both methods.";
				this.logWarning(missingMappingWarning);
				this.methodParameterChangeMap.put(correspondingMethod, previousExtraParameters);
				return previousExtraParameters;
			}

			if (!GlobalUtils.areEquals(extraParameters, previousExtraParameters)) {
				MethodSignature methodSignature = buildChangedMethodSignature(method, extraParameters);
				MethodSignature correspondingMethodSignature = buildChangedMethodSignature(correspondingMethod, extraParameters);
				String mismatchWarning = "Method '"
					+ method.getOriginalNameWithParams()
					+ " in class '"
					+ method.getClassNameBinary()
					+ "' is mapped to parameter list '"
					+ methodSignature.toFullString()
					+ "' but the corresponding method in class '"
					+ correspondingMethod.getClassNameBinary()
					+ "' is mapped to parameter list '"
					+ correspondingMethodSignature.toFullString()
					+ "'. Using mapping of '"
					+ correspondingMethodSignature.toFullString()
					+ "' for both methods.";
				this.logWarning(mismatchWarning);
				this.methodParameterChangeMap.put(correspondingMethod, previousExtraParameters);
				return previousExtraParameters;
			}
		}

		return extraParameters;
	}

	public ArrayList<String> getClassOldFieldSignatures(String oldClassName) {
		ArrayList<String> fieldSignatures = new ArrayList<>();
		if (this.rawFieldMappings != null) {
			MultiMap<FieldKey, String> fieldMappings = this.rawFieldMappings.get(oldClassName);
			if (fieldMappings != null) {
				Enumeration<FieldKey> fieldKeyEnumeration = fieldMappings.keys();

				while (fieldKeyEnumeration.hasMoreElements()) {
					FieldKey fieldKey = fieldKeyEnumeration.nextElement();
					StringBuilder signatureBuffer = new StringBuilder();
					signatureBuffer.append(fieldKey.getDescriptor());
					signatureBuffer.append(" ");
					signatureBuffer.append(fieldKey.getName());
					fieldSignatures.add(signatureBuffer.toString());
				}
			}
		}

		return fieldSignatures;
	}

	@Override
	public void addMethodMappings(String className, MultiMap<MethodKey, MethodMappingInfo> methodMappings) {
		if (this.rawMethodMappings == null) {
			this.rawMethodMappings = new HierarchicalMultiMap<>(this.classMappingCount, 5, 5);
		}

		this.rawMethodMappings.put(className, methodMappings);
		if (!this.hasParameterChangeData()) {
			Enumeration<MethodMappingInfo> parameterChangeEnumeration = methodMappings.elements();

			while (parameterChangeEnumeration.hasMoreElements()) {
				if (parameterChangeEnumeration.nextElement().hasParameterChangeData()) {
					this.setParameterChangeData(true);
					break;
				}
			}
		}

		if (!this.hasObfuscatedParameters()) {
			Enumeration<MethodMappingInfo> obfuscatedParameterEnumeration = methodMappings.elements();

			while (obfuscatedParameterEnumeration.hasMoreElements()) {
				if (obfuscatedParameterEnumeration.nextElement().isParameterObfuscated()) {
					this.markHasObfuscatedParameters();
					break;
				}
			}
		}
	}

	public String getLogFileName() {
		return this.logFileName;
	}

	private void validateParameterChanges(Map<String, String> classNameMappings) {
		DualKeyMap<AbstractClassNode, MethodSignature, MethodSignature> originalToNewSignatures = new DualKeyMap<>();
		DualKeyMap<AbstractClassNode, MethodSignature, MethodSignature> newToOriginalSignatures = new DualKeyMap<>();

		for (Entry<AbstractMethodNode, ExtraParameter[]> parameterChangeEntry : this.methodParameterChangeMap.entrySet()) {
			AbstractMethodNode method = parameterChangeEntry.getKey();
			MethodSignature originalSignature = method.getOriginalMethodSignature();
			String parameterDescriptor = ParameterPattern.buildParameterDescriptor(MethodSignature.parseParameterDescriptors(method.getParameterDescriptor()), parameterChangeEntry.getValue());
			MethodSignature changedSignature = new MethodSignature(method.getName(), parameterDescriptor, method.getReturnDescriptor());
			if (originalToNewSignatures.put(method.getClassNode(), originalSignature, changedSignature) != null) {
				MethodSignature remappedSignature = MethodSignature.remap(changedSignature, classNameMappings, true);
				this.logSpecialError(
					"Method '"
						+ method.getAccessFlagsString()
						+ " "
						+ method.getSignature()
						+ "' in class '"
						+ method.getClassNameBinary()
						+ "' could not be changed to '"
						+ remappedSignature
						+ "' because of a method signature clash."
				);
			}

			MethodSignature clashingSignature = newToOriginalSignatures.put(method.getClassNode(), changedSignature, originalSignature);
			if (clashingSignature != null) {
				MethodSignature duplicateRemappedSignature = MethodSignature.remap(changedSignature, classNameMappings, true);
				this.logSpecialError(
					"Method '"
						+ method.getAccessFlagsString()
						+ " "
						+ method.getSignature()
						+ "' in class '"
						+ method.getClassNameBinary()
						+ "' could not be changed to '"
						+ duplicateRemappedSignature
						+ "' because of a method '"
						+ clashingSignature
						+ "' is mapped to the same new signature."
				);
			}
		}
	}

	@Override
	public void addPackageMapping(String newPackageName, String oldPackageName) {
		this.addPackageNameMapping(newPackageName, oldPackageName, true);
	}

	public MultiMap<FieldKey, String> getRawFieldMappings(String className) {
		if (this.rawFieldMappings != null) {
			MultiMap<FieldKey, String> fieldMappings = this.rawFieldMappings.get(className);
			return fieldMappings == null ? null : new MultiMap<>(fieldMappings);
		} else {
			return null;
		}
	}

	private MethodKey createMethodKey(String name, String parameterDescriptor, String returnType, int accessFlags) {
		return new MethodKey(name, parameterDescriptor, this.methodKeysHaveReturnType ? returnType : null, accessFlags);
	}

	public void removeExtraParameters(AbstractMethodNode method) {
		if (this.methodParameterChangeMap != null) {
			ExtraParameter[] removedExtraParameters = this.methodParameterChangeMap.remove(method);
		}
	}

	public boolean hasFlowObfuscationEntry(String className) {
		return this.flowObfuscationClasses.contains(GlobalUtils.internalToBinaryName(className));
	}

	public void applyFieldExcludes(DefaultExcludes defaultExcludes, Map<String, String> classNameMappings, AbstractWorkspace workspace) {
		DualKeyMap<String, FieldSignature, FieldSignature> updatedFieldMappings = new DualKeyMap<>(this.processedFieldMappings.getGroupCount());
		DualKeyMap<String, FieldSignature, FieldSignature> updatedFieldMappingsReverse = new DualKeyMap<>(this.processedFieldMappingsReverse.getGroupCount());
		Enumeration<String> classNameEnumeration = this.processedFieldMappings.getGroupKeys();

		while (classNameEnumeration.hasMoreElements()) {
			String className = classNameEnumeration.nextElement();

			for (Entry<FieldSignature, FieldSignature> fieldMapping : this.processedFieldMappings.getGroup(className).entrySet()) {
				FieldSignature oldSignature = fieldMapping.getKey();
				FieldSignature newSignature = fieldMapping.getValue();
				FieldNode fieldNode = workspace.getFieldByNameAndDescriptor(GlobalUtils.binaryToInternalName(className), oldSignature.getName(), oldSignature.getDescriptor());
				String mappedClassName = GlobalUtils.internalToBinaryName(GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(className), classNameMappings));
				if (defaultExcludes.isFieldExcluded(fieldNode)) {
					if (!oldSignature.getName().equals(newSignature.getName())) {
						FieldSignature retainedSignature = (FieldSignature)oldSignature.clone();
						this.logWarning(
							"Field '"
								+ oldSignature.getName()
								+ "' in class '"
								+ mappedClassName
								+ "' could not be renamed to '"
								+ newSignature.getName()
								+ "' because it has been excluded and the input change log is specified as loose. It will not be renamed."
						);
						updatedFieldMappings.put(className, oldSignature, retainedSignature);
						FieldSignature clashingSignature = updatedFieldMappingsReverse.put(className, retainedSignature, oldSignature);
						if (clashingSignature != null) {
							this.logFatalError(
								"Field name clash in class '"
									+ mappedClassName
									+ "' after field exclusions.  Both '"
									+ oldSignature.getName()
									+ "' and '"
									+ clashingSignature.getName()
									+ "' are renamed to '"
									+ retainedSignature.getName()
									+ "'. (1)"
							);
						}
					} else {
						updatedFieldMappings.put(className, oldSignature, newSignature);
						FieldSignature duplicateSignature = updatedFieldMappingsReverse.put(className, newSignature, oldSignature);
						if (duplicateSignature != null) {
							this.logFatalError(
								"Field name clash in class '"
									+ mappedClassName
									+ "' after field exclusions.  Both '"
									+ oldSignature.getName()
									+ "' and '"
									+ duplicateSignature.getName()
									+ "' are renamed to '"
									+ newSignature.getName()
									+ "'. (2)"
							);
						}
					}
				} else {
					updatedFieldMappings.put(className, oldSignature, newSignature);
					FieldSignature conflictingSignature = updatedFieldMappingsReverse.put(className, newSignature, oldSignature);
					if (conflictingSignature != null) {
						this.logFatalError(
							"Field name clash in class '"
								+ mappedClassName
								+ "' after field exclusions.  Both '"
								+ oldSignature.getName()
								+ "' and '"
								+ conflictingSignature.getName()
								+ "' are renamed to '"
								+ newSignature.getName()
								+ "'. (3)"
						);
					}
				}
			}
		}

		this.processedFieldMappings = updatedFieldMappings;
		this.processedFieldMappingsReverse = updatedFieldMappingsReverse;
	}

	public String getAutoReflectionClassName() {
		return this.autoReflectionClassName;
	}

	public Map<String, String> getOldToNewClassNameMap() {
		return this.oldToNewClassNameMap != null ? GlobalUtils.newHashMap(this.oldToNewClassNameMap) : null;
	}

	public boolean hasLineNumberMappings() {
		return this.lineNumberMappings != null && this.lineNumberMappings.size() > 0;
	}

	@Override
	public void setModuleObfuscateRefsClass(String className, String moduleName) {
		this.obfuscateReferencesByModule.put(moduleName, className);
	}

	public FlaggedMethodSignature reconcileMethodMapping(String className, String implementationClassName, MethodSignature methodSignature, FlaggedMethodSignature newMethodSignature, Map<String, String> classNameMappings) {
		String binaryImplementationClassName = GlobalUtils.internalToBinaryName(implementationClassName);
		String binaryClassName = GlobalUtils.internalToBinaryName(className);
		MethodSignature removedSignature = this.processedMethodMappings.remove(binaryClassName, methodSignature);
		FlaggedMethodSignature previousSignature = this.processedMethodMappings.put(binaryImplementationClassName, methodSignature, newMethodSignature);
		if (previousSignature != null && !previousSignature.equals(newMethodSignature)) {
			String mappedClassName = classNameMappings.get(className);
			if (mappedClassName == null) {
				mappedClassName = binaryClassName;
			}

			String mappedImplementationClassName = classNameMappings.get(implementationClassName);
			if (mappedImplementationClassName == null) {
				mappedImplementationClassName = binaryImplementationClassName;
			}

			String warningMessage = "Method '"
				+ methodSignature.getSignature(classNameMappings)
				+ " in class '"
				+ mappedClassName
				+ "' is mapped to new name '"
				+ newMethodSignature.getName()
				+ "' but the corresponding method in class '"
				+ mappedImplementationClassName
				+ "' is mapped to new name '"
				+ previousSignature.getName()
				+ "'. Using mapping of '"
				+ previousSignature.getName()
				+ "' for both methods.";
			this.logWarning(warningMessage);
			this.processedMethodMappings.put(binaryImplementationClassName, methodSignature, previousSignature);
			return previousSignature;
		} else {
			return newMethodSignature;
		}
	}

	public MultiMap<String, FlowObfuscationEntry> getFlowObfuscationReferences() {
		MultiMap<String, FlowObfuscationEntry> referencesByPackageName = new MultiMap<>(this.flowObfuscationReferenceGraph.size());
		Enumeration<String> packageNameEnumeration = this.flowObfuscationReferenceGraph.getMembersAsEnumeration();

		while (packageNameEnumeration.hasMoreElements()) {
			String packageName = packageNameEnumeration.nextElement();
			Set<FlowObfuscationEntry> references = this.flowObfuscationReferenceGraph.getReferences(packageName);
			ArrayList<FlowObfuscationEntry> sortedReferences = new ArrayList<>(references.size());

			for (FlowObfuscationEntry reference : references) {
				sortedReferences.add(reference);
			}

			Collections.sort(sortedReferences);
			referencesByPackageName.put(packageName, sortedReferences);
		}

		return referencesByPackageName;
	}

	public void updateClassMapping(String oldClassName, String newClassName) {
		String binaryOldClassName = GlobalUtils.internalToBinaryName(oldClassName);
		String binaryNewClassName = GlobalUtils.internalToBinaryName(newClassName);
		String previousNewClassName;
		if (this.hasOldClass(binaryOldClassName)) {
			previousNewClassName = this.oldToNewClassNameMap.get(binaryOldClassName);
			this.pruneClassMapping(binaryOldClassName);
		} else {
			previousNewClassName = this.prunedClassMappings.get(binaryOldClassName);
		}

		this.addClassMapping(previousNewClassName, binaryNewClassName);
	}

	public ArrayList<KeyValuePair<MethodSignature, FlaggedMethodSignature>> getMethodSignatureMappings(String className) {
		className = GlobalUtils.internalToBinaryName(className);
		if (this.processedMethodMappings != null) {
			Map<MethodSignature, FlaggedMethodSignature> signatureMappings = this.processedMethodMappings.getGroup(className);
			if (signatureMappings != null) {
				ArrayList<KeyValuePair<MethodSignature, FlaggedMethodSignature>> signaturePairs = new ArrayList<>();

				for (Entry<MethodSignature, FlaggedMethodSignature> signatureMapping : signatureMappings.entrySet()) {
					signaturePairs.add(new KeyValuePair<>(signatureMapping.getKey(), signatureMapping.getValue()));
				}

				return signaturePairs;
			}
		}

		return null;
	}

	public String getOldFieldName(String newClassName, String newFieldName, String fieldType) {
		return this.lookupOldFieldName(newClassName, newFieldName, fieldType, this.rawFieldMappings);
	}

	public Map<MethodNode, Long> getMethodEncryptionKeys() {
		HashMap<MethodNode, Long> encryptionKeys = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.methodEncryptionKeys.size()));

		for (Entry<AbstractMethodNode, Long> encryptionKeyEntry : this.methodEncryptionKeys.entrySet()) {
			encryptionKeys.put((MethodNode)encryptionKeyEntry.getKey(), encryptionKeyEntry.getValue());
		}

		return encryptionKeys;
	}

	private MethodSignature applyExtraParameters(MethodSignature signature, String className, DualKeyMap<String, MethodSignature, ExtraParameter[]> extraParametersBySignature) {
		ExtraParameter[] extraParameters = extraParametersBySignature.get(className, signature);
		if (extraParameters == null) {
			return signature;
		} else {
			String parameterDescriptor = ParameterPattern.buildParameterDescriptor(MethodSignature.parseParameterDescriptors(signature.getParameterDescriptor()), extraParameters);
			return new MethodSignature(signature.getName(), parameterDescriptor, signature.getReturnDescriptor());
		}
	}

	public Set<AbstractMethodNode> getParameterObfuscatedMethods() {
		return this.parameterObfuscatedMethods;
	}

	public MultiMap<Integer, Integer> getLineNumberMappings(String className) {
		if (this.lineNumberMappings == null) {
			return null;
		} else {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			return this.lineNumberMappings.get(binaryClassName);
		}
	}

	public Enumeration<FieldSignature> getRenamedFieldSignatures(String className) {
		className = GlobalUtils.internalToBinaryName(className);
		if (this.processedFieldMappingsReverse != null) {
			Map<FieldSignature, FieldSignature> reverseFieldMappings = this.processedFieldMappingsReverse.getGroup(className);
			if (reverseFieldMappings != null) {
				ArrayList<FieldSignature> newSignatures = new ArrayList<>(reverseFieldMappings.size());

				for (FieldSignature newSignature : reverseFieldMappings.keySet()) {
					FieldSignature oldSignature = reverseFieldMappings.get(newSignature);
					if (!newSignature.equals(oldSignature)) {
						newSignatures.add(newSignature);
					}
				}

				return Collections.enumeration(newSignatures);
			}
		}

		return null;
	}

	public boolean hasParameterChangeEntry(AbstractMethodNode method) {
		return this.methodParameterChangeMap == null ? false : this.methodParameterChangeMap.containsKey(method);
	}

	@Override
	public void addFieldMappings(String className, MultiMap<FieldKey, String> fieldMappings) {
		if (this.rawFieldMappings == null) {
			this.rawFieldMappings = new HierarchicalMultiMap<>(this.classMappingCount, 5, 5);
		}

		this.rawFieldMappings.put(className, fieldMappings);
	}

	public void removeRawFieldMapping(String className, String fieldName, String descriptor) {
		String binaryClassName = GlobalUtils.internalToBinaryName(className);
		FieldKey fieldKey = this.createNormalizedFieldKey(fieldName, descriptor, 0);
		this.rawFieldMappings.removeAll(binaryClassName, fieldKey);
	}

	public String getSourceName(String oldClassName) {
		if (this.oldClassNameToSourceName != null) {
			String binaryClassName = GlobalUtils.internalToBinaryName(oldClassName);
			return this.oldClassNameToSourceName.get(binaryClassName);
		} else {
			return null;
		}
	}

	public void logFatalError(String message) {
		this.hasError = true;
		this.scriptContext.logFatalError("Input change log '" + this.logFileName + "': " + message + this.getFileLocationHint());
	}

	public Enumeration<String> getFieldMappingClasses() {
		if (this.processedFieldMappings == null) {
			return null;
		} else {
			ArrayList<String> classNames = new ArrayList<>(this.processedFieldMappings.getGroupCount());
			Enumeration<String> classNameEnumeration = this.processedFieldMappings.getGroupKeys();

			while (classNameEnumeration.hasMoreElements()) {
				String className = classNameEnumeration.nextElement();
				classNames.add(GlobalUtils.binaryToInternalName(className));
			}

			return Collections.enumeration(classNames);
		}
	}

	public List<AbstractMethodNode> getChangedParameterMethods() {
		if (this.methodParameterChangeMap != null) {
			ArrayList<AbstractMethodNode> changedMethods = new ArrayList<>(this.methodParameterChangeMap.size());

			for (Entry<AbstractMethodNode, ExtraParameter[]> parameterChangeEntry : this.methodParameterChangeMap.entrySet()) {
				ExtraParameter[] extraParameters = parameterChangeEntry.getValue();
				if (extraParameters != null && extraParameters.length > 0) {
					changedMethods.add(parameterChangeEntry.getKey());
				}
			}

			return changedMethods;
		} else {
			return new ArrayList<>();
		}
	}

	public void pruneClassMapping(String className) {
		if (className != null) {
			className = GlobalUtils.internalToBinaryName(className);
		}

		if (this.oldToNewClassNameMap != null) {
			String newClassName = this.oldToNewClassNameMap.get(className);
			String removedNewClassName = this.oldToNewClassNameMap.remove(className);
			if (this.prunedClassMappings == null) {
				this.prunedClassMappings = GlobalUtils.newHashMap();
			}

			this.prunedClassMappings.put(className, newClassName);
			this.newToOldClassNameMap.remove(newClassName);
			if (this.rawFieldMappings != null) {
				MultiMap<FieldKey, String> fieldMappings = this.rawFieldMappings.remove(className);
				if (fieldMappings != null) {
					if (this.prunedClassFieldMappings == null) {
						this.prunedClassFieldMappings = new HierarchicalMultiMap<>();
					}

					this.prunedClassFieldMappings.put(className, fieldMappings);
				}
			}

			if (this.rawMethodMappings != null) {
				this.rawMethodMappings.remove(className);
			}

			if (this.memberClassEntries != null) {
				this.memberClassEntries.remove(className);
			}

			if (this.oldClassNameToSourceName != null) {
				this.oldClassNameToSourceName.remove(newClassName);
			}

			if (this.lineNumberMappings != null) {
				this.lineNumberMappings.remove(newClassName);
			}

			this.logWarning("Class '" + className + "' no longer appears in the opened classes and will be assumed to be no longer used.");
		}
	}

	public boolean hasOldClass(String oldClassName) {
		return this.oldToNewClassNameMap != null && this.oldToNewClassNameMap.containsKey(GlobalUtils.internalToBinaryName(oldClassName));
	}

	private void mergeRawMethodMappings(ChangeLogContext other, List<String> classNames) {
		if (this.rawMethodMappings != null && this.methodKeysHaveReturnType != other.methodKeysHaveReturnType) {
			this.logSpecialError(
				"Change log methods "
					+ (this.methodKeysHaveReturnType ? "have return types" : "have no return types")
					+ " which is inconsistent with change log \""
					+ other.logFileName
					+ "\" whose methods "
					+ (other.methodKeysHaveReturnType ? "have return types" : "have no return types")
					+ ". All method entries in change logs being merged must have return types or no method entries can have return types."
			);
		} else if (this.rawMethodMappings != null && this.hasParameterChangeData() != other.hasParameterChangeData()) {
			this.logSpecialError(
				"Change log methods "
					+ (this.hasParameterChangeData() ? "have new method parameter data" : "have no new method parameter data")
					+ " which is inconsistent with change log \""
					+ other.logFileName
					+ "\" whose methods "
					+ (other.hasParameterChangeData() ? "have new method parameter data" : "have no new method parameter data")
					+ ". All method entries in change logs being merged must have new method parameter data or no method entries can have new method parameter data."
			);
		} else {
			int classCount = classNames.size();

			for (int i = 0; i < classCount; i++) {
				String className = classNames.get(i);
				MultiMap<MethodKey, MethodMappingInfo> otherClassMethodMappings = other.rawMethodMappings.get(className);
				if (otherClassMethodMappings != null) {
					ReadOnlyList<MethodKey> otherMethodKeys = new ReadOnlyList<>(otherClassMethodMappings.keys());
					Collections.sort(otherMethodKeys);

					for (int methodKeyIndex = 0; methodKeyIndex < otherMethodKeys.size(); methodKeyIndex++) {
						MethodKey methodKey = otherMethodKeys.get(methodKeyIndex);
						String methodName = methodKey.getName();
						String returnDescriptor = methodKey.getDescriptor();
						String parameterDescriptor = methodKey.getParameterDescriptor();
						List<MethodMappingInfo> otherMethodMappings = otherClassMethodMappings.get(methodKey);
						if (otherMethodMappings.size() > 1) {
							if (other.methodKeysHaveReturnType) {
								this.logSpecialError(
									"More than one method \"" + returnDescriptor + " " + methodName + "(" + parameterDescriptor + ")\" appears in class \"" + className + "\" in log \"" + other.logFileName + "\"."
								);
							} else {
								this.logSpecialError(
									"More than one method \""
										+ methodName
										+ "("
										+ parameterDescriptor
										+ ")\" appears in class \""
										+ className
										+ "\" in log \""
										+ other.logFileName
										+ "\" but there are no method return types to allow the mappings to be distinguished."
								);
							}
						} else {
							MethodMappingInfo otherMethodMapping = otherMethodMappings.get(0);
							if (this.rawMethodMappings != null) {
								List<MethodMappingInfo> existingMethodMappings = this.rawMethodMappings.getValues(className, methodKey);
								if (existingMethodMappings != null && existingMethodMappings.size() > 0) {
									if (existingMethodMappings.size() > 1) {
										if (this.methodKeysHaveReturnType) {
											this.logSpecialError("More than one method \"" + returnDescriptor + " " + methodName + "(" + parameterDescriptor + ")\" appears in class \"" + className + "\".");
										} else {
											this.logSpecialError(
												"More than one method \""
													+ methodName
													+ "("
													+ parameterDescriptor
													+ ")\" appears in class \""
													+ className
													+ "\" there are no method return types to allow the mappings to be distinguished."
											);
										}
									} else {
										MethodMappingInfo existingMethodMapping = existingMethodMappings.get(0);
										if (!existingMethodMapping.equals(otherMethodMapping)) {
											if (existingMethodMapping.getNewName().equals(otherMethodMapping.getNewName()) && existingMethodMapping.getEffectiveParameterList().equals(otherMethodMapping.getEffectiveParameterList())) {
												if (existingMethodMapping.getParameterChangeData() == null && otherMethodMapping.getParameterChangeData() != null
													|| existingMethodMapping.getParameterChangeData() != null && otherMethodMapping.getParameterChangeData() == null
													|| existingMethodMapping.getParameterChangeData() != null && otherMethodMapping.getParameterChangeData() != null && !existingMethodMapping.getParameterChangeData().equals(otherMethodMapping.getParameterChangeData())) {
													this.logWarning(
														"Method \""
															+ (returnDescriptor != null ? returnDescriptor + " " : "")
															+ methodName
															+ "("
															+ parameterDescriptor
															+ ")\" mapping clash in class \""
															+ className
															+ "\" while merging with \""
															+ other.logFileName
															+ "\". Parameter change data is different : '"
															+ existingMethodMapping.getParameterChangeData()
															+ "'!='"
															+ otherMethodMapping.getParameterChangeData()
															+ ". The data from '"
															+ this.getLogFileName()
															+ "' will be used."
													);
												} else {
													this.logWarning(
														"Method \""
															+ (returnDescriptor != null ? returnDescriptor + " " : "")
															+ methodName
															+ "("
															+ parameterDescriptor
															+ ")\" mapping clash in class \""
															+ className
															+ "\" while merging with \""
															+ other.logFileName
															+ "\". Change log '"
															+ this.getLogFileName()
															+ "' says "
															+ (existingMethodMapping.isParameterObfuscated() ? "" : "not ")
															+ "parameter obfuscated but '"
															+ other.logFileName
															+ "' says "
															+ (otherMethodMapping.isParameterObfuscated() ? "" : "not ")
															+ "parameter obfuscated. Will be treated as "
															+ (existingMethodMapping.isParameterObfuscated() ? "" : "not ")
															+ "parameter obfuscated."
													);
												}
											} else {
												this.logWarning(
													"Method \""
														+ (returnDescriptor != null ? returnDescriptor + " " : "")
														+ methodName
														+ "("
														+ parameterDescriptor
														+ ")\" mapping clash in class \""
														+ className
														+ "\" while merging with \""
														+ other.logFileName
														+ "\". Will be mapped to \""
														+ existingMethodMapping.getNewMethodSignature()
														+ "\" and not \""
														+ otherMethodMapping.getNewMethodSignature()
														+ "\""
														+ (existingMethodMapping.isParameterObfuscated() ? " and will be parameter obfuscated." : ".")
												);
											}
										}
									}
								} else {
									this.putRawMethodMapping(className, methodKey, otherMethodMapping);
								}
							} else {
								this.putRawMethodMapping(className, methodKey, otherMethodMapping);
							}
						}
					}
				}
			}
		}
	}

	private void mergeRawFieldMappings(ChangeLogContext other, List<String> classNames) {
		if (this.rawFieldMappings != null && this.fieldKeysHaveType != other.fieldKeysHaveType) {
			this.logSpecialError(
				"Change log fields "
					+ (this.fieldKeysHaveType ? "have types" : "have no types")
					+ " which is inconsistent with change log \""
					+ other.logFileName
					+ "\" whose fields "
					+ (other.fieldKeysHaveType ? "have types" : "have no types")
					+ ". All field entries in change logs being merged must have types or no field entries can have types."
			);
		} else {
			int classCount = classNames.size();

			for (int i = 0; i < classCount; i++) {
				String className = classNames.get(i);
				MultiMap<FieldKey, String> otherClassFieldMappings = other.rawFieldMappings.get(className);
				if (otherClassFieldMappings != null) {
					ReadOnlyList<FieldKey> otherFieldKeys = new ReadOnlyList<>(otherClassFieldMappings.keys());
					Collections.sort(otherFieldKeys);
					int fieldKeyCount = otherFieldKeys.size();

					for (int fieldKeyIndex = 0; fieldKeyIndex < fieldKeyCount; fieldKeyIndex++) {
						FieldKey fieldKey = otherFieldKeys.get(fieldKeyIndex);
						String fieldName = fieldKey.getName();
						String fieldDescriptor = fieldKey.getDescriptor();
						List<String> otherFieldNameMappings = otherClassFieldMappings.get(fieldKey);
						if (otherFieldNameMappings.size() > 1) {
							if (other.fieldKeysHaveType) {
								this.logSpecialError("More than one field \"" + fieldDescriptor + " " + fieldName + "\" appears in class \"" + className + "\" in log \"" + other.logFileName + "\".");
							} else {
								this.logSpecialError(
									"More than one field with the name \""
										+ fieldName
										+ "\" appears in class \""
										+ className
										+ "\" in log \""
										+ other.logFileName
										+ "\" but there are no field types to allow the mappings to be distinguished."
								);
							}
						} else {
							String otherFieldName = otherFieldNameMappings.get(0);
							if (this.rawFieldMappings != null) {
								List<String> existingFieldNameMappings = this.rawFieldMappings.getValues(className, fieldKey);
								if (existingFieldNameMappings != null && existingFieldNameMappings.size() > 0) {
									if (existingFieldNameMappings.size() == 1) {
										String existingFieldName = existingFieldNameMappings.get(0);
										if (!existingFieldName.equals(otherFieldName)) {
											this.logWarning(
												"Field \""
													+ (fieldDescriptor != null ? fieldDescriptor + " " : "")
													+ fieldName
													+ "\" mapping clash in class \""
													+ className
													+ "\" while merging with \""
													+ other.logFileName
													+ "\". Will be mapped to \""
													+ existingFieldName
													+ "\" and not \""
													+ otherFieldName
													+ "\""
											);
										}
									} else if (existingFieldNameMappings.size() > 1) {
										if (this.fieldKeysHaveType) {
											this.logSpecialError("More than one field \"" + fieldDescriptor + " " + fieldName + "\" appears in class \"" + className + "\" in log.");
										} else {
											this.logSpecialError(
												"More than one field with the name \""
													+ fieldName
													+ "\" appears in class \""
													+ className
													+ "\" in log but there are no field types to allow the mappings to be distinguished."
											);
										}
									}
								} else {
									this.putRawFieldMapping(className, fieldKey, otherFieldName);
								}
							} else {
								this.putRawFieldMapping(className, fieldKey, otherFieldName);
							}
						}
					}
				}
			}
		}
	}

	public FlaggedMethodSignature getFlaggedMethodSignature(String className, MethodSignature methodSignature) {
		return this.processedMethodMappings == null ? null : this.processedMethodMappings.get(GlobalUtils.internalToBinaryName(className), methodSignature);
	}

	public boolean hasNoParameterChange(AbstractMethodNode method) {
		if (this.methodParameterChangeMap == null) {
			return false;
		} else {
			ExtraParameter[] extraParameters = this.methodParameterChangeMap.get(method);
			return extraParameters == null ? false : extraParameters.length == 0;
		}
	}

	public void applyMethodExcludes(DefaultExcludes defaultExcludes, Map<String, String> classNameMappings, AbstractWorkspace workspace) {
		if (this.processedMethodMappings != null) {
			DualKeyMap<String, MethodSignature, FlaggedMethodSignature> retainedMethodMappings = new DualKeyMap<>(this.processedMethodMappings.getGroupCount());
			DualKeyMap<String, FlaggedMethodSignature, MethodSignature> clashDetectionMap = new DualKeyMap<>(this.processedMethodMappings.getGroupCount());

			for (Entry<String, Map<MethodSignature, FlaggedMethodSignature>> classEntry : this.processedMethodMappings.getEntrySet()) {
				String binaryClassName = classEntry.getKey();

				for (Entry<MethodSignature, FlaggedMethodSignature> methodEntry : classEntry.getValue().entrySet()) {
					MethodSignature oldSignature = methodEntry.getKey();
					FlaggedMethodSignature newSignature = methodEntry.getValue();
					ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(binaryClassName));
					MethodNode methodNode = workspace.getMethodBySignature(classNode, oldSignature);
					String newClassName = GlobalUtils.internalToBinaryName(GlobalUtils.getMappedOrDefault(GlobalUtils.binaryToInternalName(binaryClassName), classNameMappings));
					if (defaultExcludes.isMethodExcluded(methodNode)) {
						if (!oldSignature.getName().equals(newSignature.getName())) {
							FlaggedMethodSignature unrenamedSignature = new FlaggedMethodSignature(oldSignature.getName(), newSignature.getParameterDescriptor(), newSignature.getReturnDescriptor(), newSignature.getFlag());
							this.logWarning(
								"Method '"
									+ oldSignature.getSignature(classNameMappings)
									+ "' in class '"
									+ newClassName
									+ "' could not be renamed to '"
									+ newSignature.getName()
									+ "' because it has been excluded and the input change log is specified as loose. It will not be renamed."
							);
							retainedMethodMappings.put(binaryClassName, oldSignature, unrenamedSignature);
							MethodSignature previousExcludedSignature = clashDetectionMap.put(binaryClassName, unrenamedSignature, oldSignature);
							if (previousExcludedSignature != null) {
								this.logFatalError(
									"Method name clash in class '"
										+ newClassName
										+ "' after method exclusions with a loose input change log.  Both '"
										+ oldSignature.getSignature(classNameMappings)
										+ "' and '"
										+ previousExcludedSignature.getSignature(classNameMappings)
										+ "' are renamed to '"
										+ unrenamedSignature.getSignature(classNameMappings)
										+ "'. (1)"
								);
							}
						} else {
							retainedMethodMappings.put(binaryClassName, oldSignature, newSignature);
							MethodSignature previousUnrenamedSignature = clashDetectionMap.put(binaryClassName, newSignature, oldSignature);
							if (previousUnrenamedSignature != null) {
								this.logFatalError(
									"Method name clash in class '"
										+ newClassName
										+ "' after method exclusions with a loose input change log.  Both '"
										+ oldSignature.getSignature(classNameMappings)
										+ "' and '"
										+ previousUnrenamedSignature.getSignature(classNameMappings)
										+ "' are renamed to '"
										+ newSignature.getSignature(classNameMappings)
										+ "'. (2)"
								);
							}
						}
					} else {
						retainedMethodMappings.put(binaryClassName, oldSignature, newSignature);
						MethodSignature previousRetainedSignature = clashDetectionMap.put(binaryClassName, newSignature, oldSignature);
						if (previousRetainedSignature != null) {
							this.logFatalError(
								"Method name clash in class '"
									+ newClassName
									+ "' after method exclusions with a loose input change log.  Both '"
									+ oldSignature.getSignature(classNameMappings)
									+ "' and '"
									+ previousRetainedSignature.getSignature(classNameMappings)
									+ "' are renamed to '"
									+ newSignature.getSignature(classNameMappings)
									+ "'. (3)"
							);
						}
					}
				}
			}

			DualKeyMap<String, MethodSignature, FlaggedMethodSignature> oldProcessedMethodMappings = this.processedMethodMappings;
			this.processedMethodMappings = retainedMethodMappings;
			clashDetectionMap.clearAll();
			oldProcessedMethodMappings.clearAll();
		}
	}

	public void buildProcessedFieldMappings(
		Map<String, String> oldToNewClassNameMappings, Map<String, String> newToOldClassNameMappings, ReferenceGraph<AbstractClassNode, FieldSignature> fieldReferences, AbstractWorkspace workspace, ClassRepository classRepository
	) {
		this.processedFieldMappings = new DualKeyMap<>(this.classMappingCount);
		this.processedFieldMappingsReverse = new DualKeyMap<>(this.classMappingCount);
		if (this.rawFieldMappings != null) {
			Enumeration<String> classNameEnumeration = this.rawFieldMappings.keys();

			while (classNameEnumeration.hasMoreElements()) {
				String oldClassName = classNameEnumeration.nextElement();
				String oldClassInternalName = GlobalUtils.binaryToInternalName(oldClassName);
				String newClassInternalName = oldToNewClassNameMappings.get(oldClassInternalName);
				String newClassName;
				if (newClassInternalName == null) {
					newClassInternalName = oldClassInternalName;
					newClassName = oldClassName;
				} else {
					newClassName = GlobalUtils.internalToBinaryName(newClassInternalName);
				}

				AbstractClassNode classNode = ClassHierarchyNode.getClassNodeByInternalName(newClassInternalName);
				if (classNode == null) {
					classNode = classRepository.loadClass(oldClassInternalName, false);
				}

				boolean fromWorkspace = classNode.isWorkspaceNode();
				MultiMap<FieldKey, String> classRawFieldMappings = this.rawFieldMappings.get(oldClassName);
				Enumeration<FieldKey> fieldKeyEnumeration = classRawFieldMappings.keys();

				label102:
				while (fieldKeyEnumeration.hasMoreElements()) {
					FieldKey fieldKey = fieldKeyEnumeration.nextElement();
					String fieldName = fieldKey.getName();
					String logFieldDescriptor = fieldKey.getDescriptor();
					List<String> newFieldNameList = classRawFieldMappings.get(fieldKey);
					int newFieldNameCount = newFieldNameList.size();
					if (this.fieldKeysHaveType) {
						if (newFieldNameCount > 1) {
							this.logFatalError("Field \"" + logFieldDescriptor + " " + fieldName + "\" in class \"" + oldClassName + "\" appears more than once in log.");
						}

						String loggedNewFieldName = newFieldNameList.get(0);
						String fieldDescriptor = MethodSignature.fromHumanReadableType(logFieldDescriptor, oldToNewClassNameMappings);
						Object existingOldField;
						if (fromWorkspace) {
							existingOldField = workspace.getFieldByNameAndDescriptor(newClassInternalName, fieldName, fieldDescriptor);
						} else {
							existingOldField = classNode.findField(fieldName, fieldDescriptor);
						}

						FieldSignature oldFieldSignature = new FieldSignature(fieldName, fieldDescriptor);
						if (existingOldField == null && (fromWorkspace || fieldReferences == null || !fieldReferences.isReferencedBy(classNode, oldFieldSignature))) {
							if (!fromWorkspace && !fieldKey.isPrivate()) {
								this.logWarning(
									"Contains field '"
										+ oldFieldSignature.toRemappedString(newToOldClassNameMappings)
										+ "' in class '"
										+ oldClassName
										+ "' but there is no corresponding field in "
										+ classNode.getRawSourcePath()
										+ " (1)."
								);
							}
						} else {
							FieldSignature newFieldSignature = new FieldSignature(loggedNewFieldName, fieldDescriptor);
							this.addProcessedFieldMapping(oldClassName, newClassName, oldFieldSignature, newFieldSignature, newToOldClassNameMappings);
						}
					} else if (newFieldNameCount > 1) {
						this.logSpecialError("There are " + newFieldNameCount + " field entries named \"" + fieldName + "\" for class \"" + oldClassName + "\" but no field types are present in log.");
					} else {
						String newFieldName = newFieldNameList.get(0);
						if (!fromWorkspace && fieldReferences != null) {
							Set<FieldSignature> classFieldReferences = fieldReferences.getReferences(classNode);
							if (classFieldReferences != null) {
								for (FieldSignature fieldReference : classFieldReferences) {
									if (fieldReference.getName().equals(fieldName)) {
										FieldSignature newReferenceSignature = new FieldSignature(newFieldName, fieldReference.getDescriptor());
										this.addProcessedFieldMapping(oldClassName, newClassName, fieldReference, newReferenceSignature, newToOldClassNameMappings);
										continue label102;
									}
								}
							}
						}

						Object foundFields;
						if (fromWorkspace) {
							foundFields = workspace.getFieldsByClassAndName(newClassInternalName, fieldName);
						} else {
							foundFields = classNode.findFieldsByName(fieldName);
						}

						if (foundFields != null && ((Object[])foundFields).length != 0) {
							if (((Object[])foundFields).length == 1) {
								String existingFieldDescriptor = ((AbstractFieldNode)((Object[])foundFields)[0]).getDescriptor();
								FieldSignature existingFieldSignature = new FieldSignature(fieldName, existingFieldDescriptor);
								FieldSignature renamedFieldSignature = new FieldSignature(newFieldName, existingFieldDescriptor);
								this.addProcessedFieldMapping(oldClassName, newClassName, existingFieldSignature, renamedFieldSignature, newToOldClassNameMappings);
							} else {
								this.logSpecialError(
									"There are " + ((Object[])foundFields).length + " fields named \"" + fieldName + "\" in class \"" + oldClassName + "\" but no field types are present in log."
								);
							}
						} else if (!classNode.isWorkspaceNode()) {
							this.logWarning("Contains field '" + fieldName + "' in class '" + oldClassName + "' but there is no corresponding field in " + classNode.getRawSourcePath() + " (2).");
						}
					}
				}
			}
		}
	}

	public Map<MethodNode, long[]> buildMethodSaltValues(Map<ClassNode, long[]> classesWithoutStaticInitializer) {
		HashMap<MethodNode, long[]> saltValuesByMethodNode = GlobalUtils.newHashMap();

		for (Entry<AbstractMethodNode, long[]> methodSaltEntry : this.methodSaltValues.entrySet()) {
			if (methodSaltEntry.getKey().isWorkspaceNode()) {
				saltValuesByMethodNode.put((MethodNode)methodSaltEntry.getKey(), methodSaltEntry.getValue());
			}
		}

		for (Entry<AbstractClassNode, long[]> classSaltEntry : this.classSaltValues.entrySet()) {
			if (classSaltEntry.getKey().isWorkspaceNode()) {
				ClassNode classNode = (ClassNode)classSaltEntry.getKey();
				MethodNode staticInitializerNode = classNode.findMethodNode(MethodSignature.STATIC_INITIALIZER);
				if (staticInitializerNode != null) {
					saltValuesByMethodNode.put(staticInitializerNode, classSaltEntry.getValue());
				} else {
					classesWithoutStaticInitializer.put(classNode, classSaltEntry.getValue());
				}
			}
		}

		return saltValuesByMethodNode;
	}

	private void parseFlowObfuscationEntry(String className, List<String> data, boolean isClassEntry, String entryType, String packageName) {
		this.sawFlowObfuscationEntry = true;
		String packageSuffix = isClassEntry ? "" : " for package '" + packageName + "'";
		if (this.oldToNewClassNameMap != null && this.oldToNewClassNameMap.containsKey(className)) {
			String newInternalClassName = GlobalUtils.binaryToInternalName(GlobalUtils.getMappedOrDefault(className, this.oldToNewClassNameMap));
			boolean corruptData = false;
			String decodedClassName = null;
			String decodedFieldDescriptor = null;
			String decodedSetterName = null;
			String decodedGetterName = null;
			String decodedBooleanGetterName = null;
			boolean processingFlag = false;

			if (data.size() < 2) {
				this.flowObfuscationEntryList.add(null);
				return;
			}

			boolean hashedEntry = data.get(0).charAt(0) == '0';

			for (int i = 0; i < data.size(); i++) {
				if (hashedEntry) {
					String dataElement = data.get(i);
					if (dataElement.length() > 1) {
						char elementKind = dataElement.charAt(0);
						String elementHash = dataElement.substring(1);
						if (elementKind == '0') {
							FieldKey fieldKey = this.findFieldKeyByHash(className, elementHash);
							decodedClassName = fieldKey.getName();
							decodedFieldDescriptor = MethodSignature.fromHumanReadableType(fieldKey.getDescriptor());
							if (decodedFieldDescriptor == null) {
								this.logSpecialError("Corrupt Data: '" + elementHash + "' for " + entryType + packageSuffix + " '" + GlobalUtils.internalToBinaryName(className) + "' (C)");
								corruptData = true;
								break;
							}
						} else {
							String mappedMethodName = this.findMethodKeyByHash(className, elementHash).getName();

							switch (elementKind) {
								case 'a':
									decodedSetterName = mappedMethodName;
									break;
								case 'b':
									decodedGetterName = mappedMethodName;
									break;
								case 'c':
									decodedBooleanGetterName = mappedMethodName;
							}
						}
					} else {
						processingFlag = dataElement.charAt(0) >= 'A' && dataElement.charAt(0) <= 'Z';
					}
				} else if (i == 0) {
					String firstDataElement = data.get(i);
					if ((firstDataElement.charAt(0) < '1' || firstDataElement.charAt(0) > '5') && "MQoSAkmVrHyDwxKuhLPOURetTIFYqWnpbzfjEsXcZlNCdvgJGBia".indexOf(firstDataElement.charAt(0)) == -1) {
						this.logSpecialError("Corrupt Data: '" + firstDataElement + "' for " + entryType + packageSuffix + " '" + GlobalUtils.internalToBinaryName(className) + "' (A)");
						corruptData = true;
						break;
					}

					try {
						decodedClassName = AbstractChangeLogContext.decodeName(firstDataElement, newInternalClassName);
					} catch (NumberFormatException ignoredNameDecode) {
						this.logSpecialError("Corrupt Data: '" + firstDataElement + "' for " + entryType + packageSuffix + " '" + GlobalUtils.internalToBinaryName(className) + "' (B)");
						corruptData = true;
						break;
					}

					decodedFieldDescriptor = decodeFieldDescriptor(firstDataElement, this.flowObfuscationEntryList, className);
					if (decodedFieldDescriptor == null) {
						this.logSpecialError("Corrupt Data: '" + firstDataElement + "' for " + entryType + packageSuffix + " '" + GlobalUtils.internalToBinaryName(className) + "' (C)");
						corruptData = true;
						break;
					}
				} else {
					String dataElement = data.get(i);
					if (dataElement.length() > 1) {
						String decodedMemberName;
						try {
							decodedMemberName = AbstractChangeLogContext.decodeNameInternal(dataElement.substring(1), newInternalClassName);
						} catch (NumberFormatException ignoredMemberNameDecode) {
							this.logSpecialError("Corrupt Data: '" + dataElement + "' for " + entryType + packageSuffix + " '" + GlobalUtils.internalToBinaryName(className) + "' (D)");
							corruptData = true;
							break;
						}

						switch (dataElement.charAt(0)) {
							case 'a':
								decodedSetterName = decodedMemberName;
								break;
							case 'b':
								decodedGetterName = decodedMemberName;
								break;
							case 'c':
								decodedBooleanGetterName = decodedMemberName;
						}
					} else {
						processingFlag = dataElement.charAt(0) >= 'A' && dataElement.charAt(0) <= 'Z';
					}
				}
			}

			if (corruptData) {
				this.flowObfuscationEntryList.add(null);
			} else {
				AtomicReference<String> errorMessageRef = new AtomicReference<>();
				FlowObfuscationEntry flowObfuscationEntry = new FlowObfuscationEntry(className, decodedClassName, decodedFieldDescriptor, decodedSetterName, decodedGetterName, decodedBooleanGetterName, processingFlag, packageName, this, entryType, errorMessageRef);
				if (!errorMessageRef.isNull()) {
					this.logSpecialError(errorMessageRef.get());
				}

				if (isClassEntry) {
					if (!this.flowObfuscationEntries.add(flowObfuscationEntry)) {
						this.logSpecialError(
							"Duplicate " + entryType + " '" + GlobalUtils.internalToBinaryName(className) + "' entries with field name '" + flowObfuscationEntry.getOriginalFieldName() + "'"
						);
					}
				} else if (!this.flowObfuscationReferenceGraph.addReference(packageName, flowObfuscationEntry)) {
					this.logSpecialError(
						"Duplicate " + entryType + packageSuffix + " '" + GlobalUtils.internalToBinaryName(className) + "' entries with field name '" + flowObfuscationEntry.getOriginalFieldName() + "'"
					);
				}

				this.flowObfuscationClasses.add(className);
				this.flowObfuscationEntryList.add(flowObfuscationEntry);
			}
		} else {
			this.logSpecialError(
				"Class '"
					+ GlobalUtils.internalToBinaryName(className)
					+ "' appears as a Flow Obfuscation "
					+ entryType
					+ packageSuffix
					+ " but does not appear in the main part of the change log. (A)"
			);
			this.flowObfuscationEntryList.add(null);
		}
	}

	public Triplet getModuleParameterChangeClasses(String moduleName) {
		return this.moduleParameterChangeClasses.get(moduleName);
	}

	public void buildProcessedMethodMappings(Map<String, String> oldToNewClassNameMappings, Map<String, String> newToOldClassNameMappings, AbstractWorkspace workspace, TypeResolver typeResolver, ClassRepository classRepository) {
		this.processedMethodMappings = new DualKeyMap<>(this.classMappingCount);
		if (this.rawMethodMappings != null) {
			DualKeyMap<String, FlaggedMethodSignature, MethodSignature> clashDetectionMap = new DualKeyMap<>(this.classMappingCount);
			DualKeyMap<String, MethodSignature, MethodSignature> parameterChangeClashMap = null;
			DualKeyMap<String, MethodSignature, ExtraParameter[]> extraParametersBySignature = null;
			if (this.hasParameterChangeData()) {
				parameterChangeClashMap = new DualKeyMap<>(this.classMappingCount);
				extraParametersBySignature = new DualKeyMap<>(this.classMappingCount);

				for (Entry<AbstractMethodNode, ExtraParameter[]> parameterChangeEntry : this.methodParameterChangeMap.entrySet()) {
					AbstractMethodNode changedMethod = parameterChangeEntry.getKey();
					extraParametersBySignature.put(changedMethod.getInternalName(), changedMethod.getMethodSignature(), parameterChangeEntry.getValue());
				}
			}

			Enumeration<String> classNameEnumeration = this.rawMethodMappings.keys();

			while (classNameEnumeration.hasMoreElements()) {
				String oldClassName = classNameEnumeration.nextElement();
				String oldClassInternalName = GlobalUtils.binaryToInternalName(oldClassName);
				String newClassInternalName = oldToNewClassNameMappings.get(oldClassInternalName);
				String newClassName;
				if (newClassInternalName == null) {
					newClassInternalName = oldClassInternalName;
					newClassName = oldClassName;
				} else {
					newClassName = GlobalUtils.internalToBinaryName(newClassInternalName);
				}

				String oldSimpleClassName = AbstractClassNode.getSimpleNameFromInternalName(oldClassInternalName);
				AbstractClassNode.getSimpleNameFromInternalName(newClassInternalName);
				Object classNode = ClassHierarchyNode.getClassNodeByInternalName(newClassInternalName);
				if (classNode == null) {
					classNode = classRepository.loadClass(oldClassInternalName, false);
				}

				for (Entry<MethodKey, List<MethodMappingInfo>> methodKeyEntry : this.rawMethodMappings.get(oldClassName).entrySet()) {
					MethodKey methodKey = methodKeyEntry.getKey();
					String methodName = methodKey.getName();
					String returnDescriptor = methodKey.getDescriptor();
					String parameterDescriptor = methodKey.getParameterDescriptor();
					String internalParameterDescriptor = MethodSignature.fromHumanReadableParameters(parameterDescriptor, oldToNewClassNameMappings);
					List<MethodMappingInfo> mappingInfos = methodKeyEntry.getValue();
					if (this.methodKeysHaveReturnType) {
						int mappingCount = mappingInfos.size();
						if (mappingCount == 1) {
							MethodMappingInfo mappingInfo = mappingInfos.get(0);
							String internalReturnDescriptor = MethodSignature.fromHumanReadableType(returnDescriptor, oldToNewClassNameMappings);
							String oldMethodName = methodKey.getName();
							String newMethodName = mappingInfo.getNewName();
							FlaggedMethodSignature newFlaggedSignature = new FlaggedMethodSignature(newMethodName, internalParameterDescriptor, internalReturnDescriptor, mappingInfo.isParameterObfuscated());
							MethodSignature oldMethodSignature = new MethodSignature(oldMethodName, internalParameterDescriptor, internalReturnDescriptor);
							if (((AbstractClassNode)classNode).isWorkspaceNode()) {
								if (workspace.containsMethodSignature(oldMethodSignature, (ClassNode)classNode)) {
									this.addProcessedMethodMapping(oldClassName, newClassName, parameterDescriptor, oldMethodSignature, newFlaggedSignature, newToOldClassNameMappings, clashDetectionMap, parameterChangeClashMap, extraParametersBySignature);
								}
							} else if (((AbstractClassNode)classNode).findMethod(oldMethodSignature) != null) {
								this.addProcessedMethodMapping(oldClassName, newClassName, parameterDescriptor, oldMethodSignature, newFlaggedSignature, newToOldClassNameMappings, clashDetectionMap, parameterChangeClashMap, extraParametersBySignature);
							} else if (!methodKey.isPrivate() && !oldMethodName.equals("<clinit>")) {
								this.logWarning(
									"Contains method '"
										+ oldMethodSignature.toSignatureString(newToOldClassNameMappings)
										+ "' in class '"
										+ oldClassName
										+ "' but there is no corresponding method in "
										+ ((AbstractClassNode)classNode).getRawSourcePath()
										+ " (1)."
								);
							}
						} else if (mappingCount > 1) {
							this.logSpecialError(
								"Methods with the signature \""
									+ methodName
									+ "("
									+ parameterDescriptor
									+ ")\" in class \""
									+ oldClassName
									+ "\" appears "
									+ mappingCount
									+ " times but there are no method return types to distinguish them."
							);
						}
					} else {
						String expectedNewName = null;

						for (int i = 0; i < mappingInfos.size(); i++) {
							MethodMappingInfo candidateMappingInfo = mappingInfos.get(i);
							if (expectedNewName == null) {
								expectedNewName = candidateMappingInfo.getNewName();
							} else if (!candidateMappingInfo.getNewName().equals(expectedNewName)) {
								this.logSpecialError(
									"Two methods with the signature \""
										+ methodName
										+ "("
										+ parameterDescriptor
										+ ")\" in class \""
										+ oldClassName
										+ "\" are mapped to the different new method names \""
										+ expectedNewName
										+ "\" and \""
										+ candidateMappingInfo.getNewName()
										+ "\""
								);
							}
						}

						MethodParameterKey parameterKey = new MethodParameterKey(methodName, internalParameterDescriptor);
						Object overloads;
						if (((AbstractClassNode)classNode).isWorkspaceNode()) {
							overloads = workspace.getMethodNodesByParameterKey((ClassNode)classNode, parameterKey);
						} else {
							overloads = ((AbstractClassNode)classNode).findMethods(parameterKey);
						}

						if (overloads == null) {
							if (!((AbstractClassNode)classNode).isWorkspaceNode()) {
								this.logWarning(
									"Contains method '"
										+ parameterKey.getSignature(newToOldClassNameMappings)
										+ "' in class '"
										+ oldClassName
										+ "' but there is no corresponding method in "
										+ ((AbstractClassNode)classNode).getRawSourcePath()
										+ " (2)."
								);
							}
						} else {
							Comparator<AbstractMethodNode> methodComparator = (left, right) -> {
								if (left.equals(right)) {
									return 0;
								} else {
									return Workspace.isSameSignatures(left, right, typeResolver) ? 1 : -1;
								}
							};
							Arrays.sort((AbstractMethodNode[])overloads, methodComparator);

							for (int overloadIndex = 0; overloadIndex < mappingInfos.size(); overloadIndex++) {
								if (overloadIndex < ((Object[])overloads).length) {
									String overloadReturnDescriptor = ((AbstractMethodNode)((Object[])overloads)[0]).getMethodSignature().getReturnDescriptor();
									MethodMappingInfo overloadMappingInfo = mappingInfos.get(overloadIndex);
									String overloadMethodName = methodKey.getName();
									String overloadNewMethodName = overloadMappingInfo.getNewName();
									if (overloadMethodName.equals(oldSimpleClassName)) {
										overloadMethodName = "<init>";
										overloadNewMethodName = "<init>";
									}

									MethodSignature overloadOldSignature = new MethodSignature(overloadMethodName, internalParameterDescriptor, overloadReturnDescriptor);
									FlaggedMethodSignature overloadNewSignature = new FlaggedMethodSignature(overloadNewMethodName, internalParameterDescriptor, overloadReturnDescriptor, false);
									this.addProcessedMethodMapping(oldClassName, newClassName, parameterDescriptor, overloadOldSignature, overloadNewSignature, newToOldClassNameMappings, clashDetectionMap, parameterChangeClashMap, extraParametersBySignature);
								}
							}
						}
					}
				}
			}

			this.rawMethodMappings = null;
		}
	}

	public boolean hasFieldMapping(String className, String fieldName, String fieldType) {
		if (this.rawFieldMappings != null) {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			FieldKey fieldKey = this.createNormalizedFieldKey(fieldName, fieldType, 0);
			return this.rawFieldMappings.containsInnerKey(binaryClassName, fieldKey);
		} else {
			return false;
		}
	}

	public void collectParameterObfuscatedMethods(ClassProvider classProvider) {
		this.parameterObfuscatedMethods = GlobalUtils.newHashSet();
		if (this.hasObfuscatedParameters()) {
			this.parameterObfuscatedMethods = GlobalUtils.newHashSet();

			for (Entry<String, MultiMap<MethodKey, MethodMappingInfo>> classEntry : this.rawMethodMappings.entrySet()) {
				String oldClassName = classEntry.getKey();
				String oldClassInternalName = GlobalUtils.binaryToInternalName(oldClassName);
				AbstractClassNode classNode = classProvider.getClassNode(oldClassInternalName);

				for (Entry<MethodKey, List<MethodMappingInfo>> methodKeyEntry : classEntry.getValue().entrySet()) {
					MethodKey methodKey = methodKeyEntry.getKey();
					String internalReturnDescriptor = MethodSignature.fromHumanReadableType(methodKey.getDescriptor());
					String methodName = methodKey.getName();
					MethodSignature methodSignature = new MethodSignature(methodName, MethodSignature.fromHumanReadableParameters(methodKey.getParameterDescriptor()), internalReturnDescriptor);
					MethodMappingInfo mappingInfo = methodKeyEntry.getValue().get(0);
					AbstractMethodNode methodNode = classNode.findMethod(methodSignature);
					if (methodNode != null && mappingInfo.isParameterObfuscated()) {
						if (!methodName.equals(mappingInfo.getNewName())) {
							this.parameterObfuscatedMethods.add(methodNode);
						} else if (mappingInfo.isParameterObfuscated()) {
							this.logFatalError(
								"Method '"
									+ methodNode.getDisplayName()
									+ "' in class '"
									+ oldClassName
									+ "' is specified as having its parameters obfuscated but not having its name changed. This not supported. Has the change log been edited?"
							);
						}
					}
				}
			}
		}
	}

	public void logError(String message) {
		this.scriptContext.logError("Input change log '" + this.logFileName + "': " + message + this.getFileLocationHint());
	}

	public void logMessage(String message) {
		this.scriptContext.logMessage("Input change log '" + this.logFileName + "': " + message + this.getFileLocationHint());
	}

	public void logWarning(String message) {
		this.scriptContext.logWarning("Input change log '" + this.logFileName + "': " + message + this.getFileLocationHint());
	}

	public void putRawMethodMapping(String className, MethodKey methodKey, MethodMappingInfo methodMapping) {
		if (this.rawMethodMappings == null) {
			this.rawMethodMappings = new HierarchicalMultiMap<>(this.classMappingCount, 5, 5);
		}

		this.rawMethodMappings.put(className, methodKey, methodMapping);
		this.methodKeysHaveReturnType = methodKey.getDescriptor() != null;
	}

	@Override
	public void addMemberClass(String className, List<String> data) {
		if (this.oldToNewClassNameMap != null && this.oldToNewClassNameMap.containsKey(className)) {
			String newClassName = this.oldToNewClassNameMap.get(className);
			String encodedData = data.get(0);

			try {
				int[] memberClassIndexes = AbstractChangeLogContext.decodeMemberClassIndexes(encodedData, GlobalUtils.binaryToInternalName(newClassName));
				FlowObfuscationEntry ownerEntry = this.flowObfuscationEntryList.get(memberClassIndexes[0]);
				FlowObfuscationEntry packageEntry = this.flowObfuscationEntryList.get(memberClassIndexes[1]);
				if (ownerEntry != null && packageEntry != null) {
					KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry> memberClassEntryPair = new KeyValuePair<>(ownerEntry, packageEntry);
					this.memberClassEntries.put(className, memberClassEntryPair);
				}
			} catch (NumberFormatException ignored) {
				this.logSpecialError("Corrupt Data:  '" + encodedData + "' for MemberClass: '" + className + "' => '" + newClassName + "' (D)");
			} catch (IndexOutOfBoundsException ignored2) {
				this.logSpecialError("Corrupt Data:  '" + encodedData + "' for MemberClass: '" + className + "' => '" + newClassName + "' (E)");
			}
		} else {
			this.logSpecialError("MemberClass: '" + className + "' does not exist in the main body of the change log");
		}
	}

	private void mergeFlowObfuscationEntries(ChangeLogContext other, Map<IdentityReference, IdentityReference> entryMappings) {
		if (this.flowObfuscationEntries == null) {
			this.flowObfuscationEntries = GlobalUtils.newHashSet(other.flowObfuscationEntries);
		} else {
			ArrayList<FlowObfuscationEntry> otherEntries = new ArrayList<>(other.flowObfuscationEntries);
			Collections.sort(otherEntries);
			String lastOriginalClassName = null;
			MultiMap<String, FlowObfuscationEntry> entriesByOriginalClassName = groupByOriginalClassName(this.flowObfuscationEntries);
			ArrayList<FlowObfuscationEntry> candidateEntries = null;

			for (FlowObfuscationEntry entry : otherEntries) {
				String originalClassName = entry.getOriginalClassName();
				if (lastOriginalClassName == null || !originalClassName.equals(lastOriginalClassName)) {
					lastOriginalClassName = originalClassName;
					List<FlowObfuscationEntry> groupEntries = entriesByOriginalClassName.get(originalClassName);
					if (groupEntries != null) {
						candidateEntries = new ArrayList<>(groupEntries);
						Collections.sort(candidateEntries);
					} else {
						candidateEntries = null;
					}
				}

				if (candidateEntries == null || candidateEntries.size() <= 0) {
					this.flowObfuscationEntries.add(entry);
				} else if (entry.getOriginalFieldName() == null) {
					FlowObfuscationEntry fallbackEntry = candidateEntries.remove(0);
					entryMappings.put(new IdentityReference(entry), new IdentityReference(fallbackEntry));
				} else {
					FlowObfuscationEntry matchedEntry = null;
					int matchedIndex = candidateEntries.indexOf(entry);
					if (matchedIndex > -1) {
						matchedEntry = candidateEntries.remove(matchedIndex);
					}

					if (matchedEntry != null) {
						if (!matchedEntry.getOriginalFieldDescriptor().equals(entry.getOriginalFieldDescriptor())) {
							this.logError(
								"Field '"
									+ matchedEntry.getOriginalFieldName()
									+ "' in class '"
									+ originalClassName
									+ "' has type '"
									+ TypeWriter.descriptorToHumanReadable(matchedEntry.getOriginalFieldDescriptor())
									+ "' in this log but type '"
									+ TypeWriter.descriptorToHumanReadable(entry.getOriginalFieldDescriptor())
									+ "' in '"
									+ other.logFileName
									+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
							);
						}

						if (entry.hasOriginalSetter()) {
							if (matchedEntry.hasOriginalSetter()) {
								if (!matchedEntry.getOriginalSetterName().equals(entry.getOriginalSetterName())) {
									this.logError(
										"Method '"
											+ matchedEntry.getOriginalSetterName()
											+ '('
											+ matchedEntry.getHumanReadableFieldType()
											+ ")' in class '"
											+ originalClassName
											+ "' in this log is inconsistent with method '"
											+ entry.getOriginalSetterName()
											+ '('
											+ entry.getHumanReadableFieldType()
											+ ")' in '"
											+ other.logFileName
											+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
									);
								}

								if (!matchedEntry.getOriginalGetterName().equals(entry.getOriginalGetterName())) {
									this.logError(
										"Method '"
											+ matchedEntry.getHumanReadableFieldType()
											+ " "
											+ matchedEntry.getOriginalGetterName()
											+ "()' in class '"
											+ originalClassName
											+ "' in this log is inconsistent with method '"
											+ entry.getHumanReadableFieldType()
											+ " "
											+ entry.getOriginalGetterName()
											+ "()' in '"
											+ other.logFileName
											+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
									);
								}

								if (matchedEntry.getOriginalBooleanGetterName() != null && !matchedEntry.getOriginalBooleanGetterName().equals(entry.getOriginalBooleanGetterName())) {
									this.logError(
										"Method '"
											+ matchedEntry.getHumanReadableFieldType()
											+ " "
											+ matchedEntry.getOriginalBooleanGetterName()
											+ "()' in class '"
											+ originalClassName
											+ "' in this log is inconsistent with method '"
											+ entry.getHumanReadableFieldType()
											+ " "
											+ entry.getOriginalBooleanGetterName()
											+ "()' in '"
											+ other.logFileName
											+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
									);
								}

								if (matchedEntry.getProcessingFlag() != entry.getProcessingFlag()) {
									this.logError(
										"Field '"
											+ matchedEntry.getOriginalFieldName()
											+ "' in class '"
											+ originalClassName
											+ "' in this log is inconsistent in nature with the equivalent field in '"
											+ other.logFileName
											+ "'. Logs could not be fully merged. You must distribute this application as a whole. (1)"
									);
								}
							} else {
								matchedEntry.setOriginalSetterName(entry.getOriginalSetterName());
								matchedEntry.setOriginalGetterName(entry.getOriginalGetterName());
								matchedEntry.setOriginalBooleanGetterName(entry.getOriginalBooleanGetterName());
								matchedEntry.setRemappedSetterName(entry.getRemappedSetterName());
								matchedEntry.setRemappedGetterName(entry.getRemappedGetterName());
								matchedEntry.setRemappedBooleanGetterName(entry.getRemappedBooleanGetterName());
								matchedEntry.setProcessingFlag(entry.getProcessingFlag());
							}
						}

						entryMappings.put(new IdentityReference(entry), new IdentityReference(matchedEntry));
					} else {
						Iterator<FlowObfuscationEntry> entryIterator = candidateEntries.iterator();

						while (entryIterator.hasNext()) {
							FlowObfuscationEntry fieldlessCandidate = entryIterator.next();
							if (fieldlessCandidate.getOriginalFieldName() == null) {
								matchedEntry = fieldlessCandidate;
								entryIterator.remove();
							}
						}

						if (matchedEntry != null) {
							Boolean.valueOf(this.flowObfuscationEntries.remove(matchedEntry));
							this.flowObfuscationEntries.add(entry);
							entryMappings.put(new IdentityReference(matchedEntry), new IdentityReference(entry));
						} else {
							this.flowObfuscationEntries.add(entry);
						}
					}
				}
			}
		}
	}

	public Set<String> getOriginalFieldNames(String className) {
		HashSet<String> originalFieldNames = GlobalUtils.newHashSet();
		String binaryClassName = GlobalUtils.internalToBinaryName(className);

		for (int i = 0; i < this.flowObfuscationEntryList.size(); i++) {
			FlowObfuscationEntry entry = this.flowObfuscationEntryList.get(i);
			if (entry.getOriginalClassName().equals(binaryClassName)) {
				originalFieldNames.add(entry.getOriginalFieldName());
			}
		}

		return originalFieldNames;
	}

	public void removeProcessedFieldMapping(String className, FieldSignature fieldSignature) {
		className = GlobalUtils.internalToBinaryName(className);
		if (this.processedFieldMappings != null) {
			FieldSignature newSignature = this.processedFieldMappings.remove(className, fieldSignature);
			FieldSignature oldSignature = this.processedFieldMappingsReverse.remove(className, newSignature);
		}
	}

	public Map<AbstractClassNode, Long> getClassEncryptionKeys() {
		return GlobalUtils.newHashMap(this.classEncryptionKeys);
	}

	public String getNewMethodName(String className, MethodSignature methodSignature) {
		if (this.processedMethodMappings == null) {
			return null;
		} else {
			MethodSignature mappedSignature = this.processedMethodMappings.get(GlobalUtils.internalToBinaryName(className), methodSignature);
			if (mappedSignature == null) {
				return null;
			} else {
				String mappedName = mappedSignature.getName();
				return mappedName.equals(methodSignature.getName()) ? null : mappedName;
			}
		}
	}

	@Override
	public void incrementPackageMappingCount() {
		this.packageMappingCount++;
	}

	private MethodKey createNormalizedMethodKey(String name, String parameterDescriptor, String returnType, int accessFlags) {
		String normalizedReturnType;
		if (TypeWriter.isValidFieldDescriptor(returnType)) {
			normalizedReturnType = TypeWriter.descriptorToHumanReadable(returnType);
		} else {
			normalizedReturnType = returnType;
		}

		MethodKey methodKey;
		if (this.methodKeysHaveReturnType) {
			methodKey = new MethodKey(name, parameterDescriptor, normalizedReturnType, accessFlags);
		} else {
			methodKey = new MethodKey(name, parameterDescriptor, null, accessFlags);
		}

		return methodKey;
	}

	public boolean hasParameterChange(AbstractMethodNode method) {
		if (this.methodParameterChangeMap == null) {
			return false;
		} else {
			ExtraParameter[] extraParameters = this.methodParameterChangeMap.get(method);
			return extraParameters == null ? false : extraParameters.length > 0;
		}
	}

	@Override
	public void setModuleAutoReflectionClass(String className, String moduleName) {
		this.autoReflectionClassByModule.put(moduleName, className);
	}

	private void mergeLineNumberMappings(ChangeLogContext other, List<String> classNames) {
		for (String className : classNames) {
			if (this.getLineNumberMappings(className) == null) {
				MultiMap<Integer, Integer> otherLineNumbers = other.getLineNumberMappings(className);
				if (otherLineNumbers != null) {
					if (this.lineNumberMappings == null) {
						this.lineNumberMappings = new HierarchicalMultiMap<>(other.classMappingCount);
					}

					this.lineNumberMappings.put(className, new MultiMap<>(otherLineNumbers));
				}
			}
		}
	}

	@Override
	public boolean isFieldNameChangeSupported() {
		return true;
	}

	public Triplet getParameterChangeClasses() {
		return this.parameterChangerDataClasses;
	}

	public boolean hasError() {
		return this.hasError;
	}

	public String getOldMethodName(String className, String newMethodName, String[] parameterTypes, String returnType) {
		if (this.newToOldMethodNames == null) {
			this.buildNewToOldMethodNames();
		}

		if (this.newToOldMethodNames != null) {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			ComparablePair<String, String> methodNameKey = this.createMethodNameKey(newMethodName, returnType);
			String parameterDescriptor = this.formatParameterList(parameterTypes);
			List<String> oldMethodNames = this.newToOldMethodNames.getValues(binaryClassName, methodNameKey, parameterDescriptor);
			if (oldMethodNames != null && oldMethodNames.size() > 0) {
				if (oldMethodNames.size() == 1) {
					String oldMethodName = oldMethodNames.get(0);
					return !oldMethodName.equals(newMethodName) ? oldMethodName : null;
				} else {
					this.logSpecialError("Class '" + binaryClassName + "' has multiple methods '" + newMethodName + "(" + parameterDescriptor + ")'.");
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public ArrayList<String> getOldClassNames() {
		ArrayList<String> oldClassNames = new ArrayList<>(this.classMappingCount);
		if (this.oldToNewClassNameMap != null) {
			SnapshotEnumerator<String> classNameEnumeration = new SnapshotEnumerator<>(this.oldToNewClassNameMap.keySet());

			while (classNameEnumeration.hasMoreElements()) {
				String className = classNameEnumeration.nextElement();
				oldClassNames.add(GlobalUtils.binaryToInternalName(className));
			}
		}

		return oldClassNames;
	}

	private void mergePackageMappings(ChangeLogContext other) {
		ArrayList<String> oldPackageNames = new ArrayList<>(other.oldToNewPackageNameMap.keySet());
		Collections.sort(oldPackageNames);
		int packageCount = oldPackageNames.size();

		for (int i = 0; i < packageCount; i++) {
			String oldPackageName = oldPackageNames.get(i);
			if (this.oldToNewPackageNameMap != null && this.oldToNewPackageNameMap.containsKey(oldPackageName)) {
				String existingNewPackageName = this.oldToNewPackageNameMap.get(oldPackageName);
				String otherNewPackageName = other.oldToNewPackageNameMap.get(oldPackageName);
				if (!existingNewPackageName.equals(otherNewPackageName)) {
					this.logWarning(
						"Package name \""
							+ oldPackageName
							+ "\" mapping clash while merging with \""
							+ other.logFileName
							+ "\". Will be mapped to \""
							+ existingNewPackageName
							+ "\" and not \""
							+ otherNewPackageName
							+ "\""
					);
				}
			} else {
				String newPackageName = other.oldToNewPackageNameMap.get(oldPackageName);
				int oldPackageLastSeparator = oldPackageName.lastIndexOf(46);
				if (oldPackageLastSeparator == -1) {
					this.addPackageNameMapping(newPackageName, oldPackageName, false);
				} else {
					String parentOldPackageName = oldPackageName.substring(0, oldPackageLastSeparator);
					String parentNewPackageName = this.oldToNewPackageNameMap.get(parentOldPackageName);
					String otherParentNewPackageName = other.oldToNewPackageNameMap.get(parentOldPackageName);
					if (otherParentNewPackageName.equals(newPackageName)) {
						this.addPackageNameMapping(parentNewPackageName, oldPackageName, false);
						if (!newPackageName.equals(parentNewPackageName)) {
							this.logWarning(
								"Package name \""
									+ oldPackageName
									+ "\" partial mapping clash (A) while merging with \""
									+ other.logFileName
									+ "\". Will be mapped to \""
									+ parentNewPackageName
									+ "\" and not \""
									+ newPackageName
									+ "\""
							);
						}
					} else {
						int newPackageDepth = StringUtils.countOccurrences(newPackageName, '.') + 1;
						int otherParentNewPackageDepth = StringUtils.countOccurrences(otherParentNewPackageName, '.') + 1;
						String mappedPackageName;
						if (otherParentNewPackageDepth >= newPackageDepth) {
							mappedPackageName = parentNewPackageName;
						} else {
							int newPackageLastSeparator = newPackageName.lastIndexOf(46);
							String leafPackageName = newPackageName.substring(newPackageLastSeparator + 1);
							mappedPackageName = parentNewPackageName + "." + leafPackageName;
						}

						this.addPackageNameMapping(mappedPackageName, oldPackageName, false);
						if (!newPackageName.equals(mappedPackageName)) {
							this.logWarning(
								"Package name \""
									+ oldPackageName
									+ "\" partial mapping clash (B) while merging with \""
									+ other.logFileName
									+ "\". Will be mapped to \""
									+ mappedPackageName
									+ "\" and not \""
									+ newPackageName
									+ "\""
							);
						}
					}
				}
			}
		}
	}

	public void updateRawFieldMapping(String className, String oldFieldName, String descriptor, String newFieldName, int accessFlags) {
		String binaryClassName = GlobalUtils.internalToBinaryName(className);
		FieldKey fieldKey = this.createNormalizedFieldKey(oldFieldName, descriptor, accessFlags);
		List<String> newFieldNames = this.rawFieldMappings.getValues(binaryClassName, fieldKey);
		if (newFieldNames.size() == 1) {
			String retainedFieldName = newFieldNames.get(0);
			this.rawFieldMappings.removeAll(binaryClassName, fieldKey);
			this.createNormalizedFieldKey(newFieldName, descriptor, accessFlags);
			this.rawFieldMappings.put(binaryClassName, fieldKey, retainedFieldName);
		} else {
			String humanReadableDescriptor = TypeWriter.descriptorToHumanReadable(descriptor);
			this.logFatalError("Class '" + binaryClassName + "' has multiple field mappings for field '" + humanReadableDescriptor + " " + oldFieldName + "'.");
		}
	}

	private void buildNewToOldMethodNames() {
		this.newToOldMethodNames = new ThreeLevelMultiMap<>();
		if (this.rawMethodMappings != null) {
			Enumeration<String> classNameEnumeration = this.rawMethodMappings.keys();

			while (classNameEnumeration.hasMoreElements()) {
				String className = classNameEnumeration.nextElement();
				String newClassName = this.getNewClassName(className);
				if (newClassName == null) {
					newClassName = className;
				} else {
					newClassName = GlobalUtils.internalToBinaryName(newClassName);
				}

				MultiMap<MethodKey, MethodMappingInfo> classMethodMappings = this.rawMethodMappings.get(className);
				Enumeration<MethodKey> methodKeyEnumeration = classMethodMappings.keys();

				while (methodKeyEnumeration.hasMoreElements()) {
					MethodKey methodKey = methodKeyEnumeration.nextElement();
					String parameterDescriptor = methodKey.getParameterDescriptor();
					String newParameterDescriptor = this.remapParameterList(parameterDescriptor, this.oldToNewClassNameMap);
					String returnTypeDescriptor = methodKey.getDescriptor();
					String newReturnTypeDescriptor = null;
					if (returnTypeDescriptor != null) {
						newReturnTypeDescriptor = AbstractChangeLogContext.remapDescriptor(returnTypeDescriptor, this.oldToNewClassNameMap);
					}

					for (MethodMappingInfo mappingInfo : classMethodMappings.get(methodKey)) {
						ComparablePair<String, String> methodNameKey = new ComparablePair<>(mappingInfo.getNewName(), newReturnTypeDescriptor);
						String effectiveParameterDescriptor;
						if (mappingInfo.hasParameterChangeData()) {
							effectiveParameterDescriptor = mappingInfo.getEffectiveParameterList();
						} else {
							effectiveParameterDescriptor = newParameterDescriptor;
						}

						this.newToOldMethodNames.put(newClassName, methodNameKey, effectiveParameterDescriptor, methodKey.getName());
					}
				}
			}
		}
	}

	public Map<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> getMemberClassEntries() {
		return GlobalUtils.newHashMap(this.memberClassEntries);
	}

	private void initFieldKeysHaveType(List<String> classNames) {
		if (this.rawFieldMappings != null) {
			boolean firstEntry = true;
			int classCount = classNames.size();

			for (int classIndex = 0; classIndex < classCount; classIndex++) {
				String className = classNames.get(classIndex);
				MultiMap<FieldKey, String> classFieldMappings = this.rawFieldMappings.get(className);
				if (classFieldMappings != null) {
					ReadOnlyList<FieldKey> fieldKeys = new ReadOnlyList<>(classFieldMappings.keys());
					Collections.sort(fieldKeys);

					for (int fieldKeyIndex = 0; fieldKeyIndex < fieldKeys.size(); fieldKeyIndex++) {
						FieldKey fieldKey = fieldKeys.get(fieldKeyIndex);
						String fieldName = fieldKey.getName();
						if (firstEntry) {
							firstEntry = false;
							this.fieldKeysHaveType = fieldKey.getDescriptor() != null;
						} else if (this.fieldKeysHaveType != (fieldKey.getDescriptor() != null)) {
							this.logSpecialError(
								"Field \""
									+ fieldName
									+ "\" in class \""
									+ className
									+ "\" "
									+ (this.fieldKeysHaveType ? "has no type." : "has a type")
									+ " which is inconsistent with the previous field entries. All field entries must have types or no field entries can have types."
							);
						}
					}
				}
			}
		}
	}

	public boolean wasClassPruned(String className) {
		return this.prunedClassMappings != null && this.prunedClassMappings.containsKey(GlobalUtils.internalToBinaryName(className));
	}

	public ArrayList<String> getNewPackageNames() {
		ArrayList<String> newPackageNames = new ArrayList<>(this.packageMappingCount);
		HashSet<String> addedPackageNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.packageMappingCount));
		if (this.newToOldPackageNameMap != null) {
			Enumeration<String> groupKeyEnumeration = this.newToOldPackageNameMap.getGroupKeys();

			while (groupKeyEnumeration.hasMoreElements()) {
				String newPackageName = groupKeyEnumeration.nextElement();

				for (String groupPackageName : this.newToOldPackageNameMap.getGroup(newPackageName).keySet()) {
					if (!newPackageName.equals(groupPackageName) && addedPackageNames.add(newPackageName)) {
						newPackageNames.add(GlobalUtils.binaryToInternalName(newPackageName));
					}
				}
			}
		}

		return newPackageNames;
	}

	public String getFileLocationHint() {
		if (this.fileLocationHint != null) {
			return this.fileLocationHint;
		} else {
			this.fileLocationHint = buildFileLocationHint(this.logFileName, this.scriptContext);
			return this.fileLocationHint;
		}
	}

	private void putRawFieldMapping(String className, FieldKey fieldKey, String newFieldName) {
		if (this.rawFieldMappings == null) {
			this.rawFieldMappings = new HierarchicalMultiMap<>(this.classMappingCount, 5, 5);
		}

		this.rawFieldMappings.put(className, fieldKey, newFieldName);
		this.fieldKeysHaveType = fieldKey.getDescriptor() != null;
	}

	public boolean hasNewFieldName(String className, FieldSignature fieldSignature) {
		return this.getNewFieldName(className, fieldSignature) != null;
	}

	@Override
	public void setParameterChangeClasses(List<String> classNames) {
		Triplet parameterChangeClasses = new Triplet(classNames.get(0), classNames.get(1), classNames.get(2));
		if (this.parameterChangeTriplets.containsKey(parameterChangeClasses)) {
			parameterChangeClasses = this.parameterChangeTriplets.get(parameterChangeClasses);
		}

		this.parameterChangerDataClasses = parameterChangeClasses;
	}

	public String getNewFieldName(String className, FieldSignature fieldSignature) {
		if (this.processedFieldMappings == null) {
			return null;
		} else {
			FieldSignature mappedSignature = this.processedFieldMappings.get(GlobalUtils.internalToBinaryName(className), fieldSignature);
			return mappedSignature != null && !mappedSignature.equals(fieldSignature) ? mappedSignature.getName() : null;
		}
	}

	@Override
	public void setAutoReflectionClass(String className) {
		this.autoReflectionClassName = className;
	}

	public void applyParameterObfuscationExcludes(ParameterChangerExcludes parameterExcludes, Hierarchy hierarchy) {
		if (this.parameterObfuscatedMethods != null) {
			Iterator<AbstractMethodNode> methodIterator = this.parameterObfuscatedMethods.iterator();

			while (methodIterator.hasNext()) {
				AbstractMethodNode method = methodIterator.next();
				if (method.isWorkspaceNode()) {
					AbstractMethodNode baseMethod = hierarchy.getBaseMethod(method);
					if (!parameterExcludes.isMethodExcluded((MethodNode)method) || baseMethod != null && baseMethod.isWorkspaceNode() && !parameterExcludes.isMethodExcluded((MethodNode)baseMethod)) {
						methodIterator.remove();
						this.logWarning(
							"Method '"
								+ method.getAccessFlagsString()
								+ " "
								+ method.getOriginalNameWithParams()
								+ "' in class '"
								+ method.getClassNameBinary()
								+ "' could not have its parameter list obfuscated because it has been directly or indirectly excluded from Method Parameter Obfuscation and the input change log is specified as loose. Its parameter list will not be obfuscated. (A)"
						);
					}
				}
			}
		}
	}

	private String lookupOldFieldName(String newClassName, String newFieldName, String fieldType, HierarchicalMultiMap<String, FieldKey, String> fieldMappings) {
		if (fieldMappings != null) {
			String oldClassName = this.getOldClassName(newClassName);
			if (oldClassName == null) {
				oldClassName = newClassName;
			} else {
				oldClassName = GlobalUtils.internalToBinaryName(oldClassName);
			}

			String oldFieldType = this.getOldTypeName(fieldType);
			if (oldFieldType == null) {
				oldFieldType = fieldType;
			} else {
				oldFieldType = GlobalUtils.internalToBinaryName(oldFieldType);
			}

			MultiMap<FieldKey, String> classFieldMappings = fieldMappings.get(oldClassName);
			if (classFieldMappings == null) {
				return newFieldName;
			} else {
				for (Entry<FieldKey, List<String>> fieldEntry : classFieldMappings.entrySet()) {
					FieldKey fieldKey = fieldEntry.getKey();
					if (fieldKey.getDescriptor().equals(oldFieldType) && fieldEntry.getValue().contains(newFieldName)) {
						return fieldKey.getName();
					}
				}

				return newFieldName;
			}
		} else {
			return newFieldName;
		}
	}

	public void removeProcessedMethodMapping(String className, MethodSignature methodSignature) {
		if (this.processedMethodMappings != null) {
			MethodSignature removedSignature = this.processedMethodMappings.remove(GlobalUtils.internalToBinaryName(className), methodSignature);
		}
	}

	public Set<String> getOldPackageNamesForNewName(String newPackageName) {
		if (this.newToOldPackageNameMap == null) {
			return null;
		} else {
			Map<String, String> packageGroup = this.newToOldPackageNameMap.getGroup(GlobalUtils.internalToBinaryName(newPackageName));
			if (packageGroup == null) {
				return null;
			} else {
				HashSet<String> oldPackageNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(packageGroup.size()));
				Iterator<String> oldPackageNameIterator = packageGroup.keySet().iterator();

				while (oldPackageNameIterator.hasNext()) {
					String oldPackageName = GlobalUtils.binaryToInternalName(oldPackageNameIterator.next());
					oldPackageNames.add(oldPackageName);
				}

				return oldPackageNames;
			}
		}
	}

	public void updateRawMethodMapping(String className, String oldMethodName, String parameterDescriptor, String returnType, String newMethodName, int accessFlags) {
		String binaryClassName = GlobalUtils.internalToBinaryName(className);
		MethodKey methodKey = this.createNormalizedMethodKey(oldMethodName, parameterDescriptor, returnType, accessFlags);
		List<MethodMappingInfo> mappingInfos = this.rawMethodMappings.getValues(binaryClassName, methodKey);
		if (mappingInfos.size() == 1) {
			MethodMappingInfo mappingInfo = mappingInfos.get(0);
			this.rawMethodMappings.removeAll(binaryClassName, methodKey);
			MethodKey newMethodKey = this.createNormalizedMethodKey(newMethodName, parameterDescriptor, methodKey.getDescriptor(), accessFlags);
			this.rawMethodMappings.put(binaryClassName, newMethodKey, mappingInfo);
		} else {
			this.logFatalError("Class '" + binaryClassName + "' has multiple method mappings for method '" + methodKey.getDescriptor() + " " + methodKey.getName() + '(' + parameterDescriptor + ")'.");
		}
	}

	public ChangeLogContext mergeWith(ChangeLogContext other, AbstractLogger logger, ScriptContext scriptContext) {
		if (other.oldToNewPackageNameMap != null) {
			this.mergePackageMappings(other);
		}

		if (this.hasError()) {
			return this;
		} else {
			ArrayList<String> oldClassNames = null;
			if (other.oldToNewClassNameMap != null) {
				oldClassNames = new ArrayList<>(other.oldToNewClassNameMap.keySet());
				Collections.sort(oldClassNames);
				int classCount = oldClassNames.size();

				for (int i = 0; i < classCount; i++) {
					String oldClassName = oldClassNames.get(i);
					if (this.oldToNewClassNameMap != null && this.oldToNewClassNameMap.containsKey(oldClassName)) {
						String existingNewClassName = this.oldToNewClassNameMap.get(oldClassName);
						String otherNewClassName = other.oldToNewClassNameMap.get(oldClassName);
						if (!existingNewClassName.equals(otherNewClassName)) {
							this.logWarning(
								"Class name \""
									+ oldClassName
									+ "\" mapping clash while merging with \""
									+ other.logFileName
									+ "\". Will be mapped to \""
									+ existingNewClassName
									+ "\" and not \""
									+ otherNewClassName
									+ "\""
							);
						}
					} else {
						String newClassName = other.oldToNewClassNameMap.get(oldClassName);
						if (this.newToOldClassNameMap != null && this.newToOldClassNameMap.containsKey(newClassName)) {
							String existingOldClassName = this.newToOldClassNameMap.get(newClassName);
							this.logSpecialError(
								"Class name \""
									+ existingOldClassName
									+ "\"  \""
									+ newClassName
									+ "\" mapping clash while merging with \""
									+ other.logFileName
									+ "\". Cannot also map \""
									+ oldClassName
									+ "\" to \""
									+ newClassName
									+ "\""
							);
						} else {
							this.addClassMapping(newClassName, oldClassName);
						}
					}
				}

				if (this.hasError()) {
					return this;
				}

				this.rebuildNewToOldClassNameMap(other.logFileName);
			}

			if (this.hasError()) {
				return this;
			} else {
				if (other.rawFieldMappings != null) {
					this.mergeRawFieldMappings(other, oldClassNames);
				}

				if (this.hasError()) {
					return this;
				} else {
					if (other.rawMethodMappings != null) {
						this.mergeRawMethodMappings(other, oldClassNames);
					}

					if (this.hasError()) {
						return this;
					} else {
						HashMap<IdentityReference, IdentityReference> entryMappings = GlobalUtils.newHashMap();
						DualKeyMap<String, IdentityReference, IdentityReference> referenceMappings = new DualKeyMap<>();
						if (other.flowObfuscationEntries != null) {
							this.mergeFlowObfuscationEntries(other, entryMappings);
						}

						if (other.flowObfuscationReferenceGraph != null) {
							this.mergeFlowObfuscationReferences(other, referenceMappings);
						}

						if (other.memberClassEntries != null) {
							this.mergeMemberClassEntries(other, entryMappings, referenceMappings);
						}

						if (this.hasError()) {
							return this;
						} else {
							if (other.lineNumberMappings != null) {
								this.mergeLineNumberMappings(other, oldClassNames);
							}

							if (other.autoReflectionClassName != null) {
								if (this.autoReflectionClassName != null && !this.autoReflectionClassName.equals(other.autoReflectionClassName)) {
									this.logSpecialError(
										"Clashing \"AutoReflectionClass:\" values. \""
											+ this.autoReflectionClassName
											+ "\" clashes with \""
											+ other.autoReflectionClassName
											+ "\" in '"
											+ other.getLogFileName()
											+ "'."
									);
								} else {
									this.autoReflectionClassName = other.autoReflectionClassName;
								}
							}

							if (other.autoReflectionClassByModule != null && !other.autoReflectionClassByModule.isEmpty()) {
								if (this.autoReflectionClassByModule == null) {
									this.autoReflectionClassByModule = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(other.autoReflectionClassByModule.size()));
								}

								for (Entry<String, String> reflectionEntry : other.autoReflectionClassByModule.entrySet()) {
									String reflectionModule = reflectionEntry.getKey();
									String reflectionClassName = reflectionEntry.getValue();
									String existingReflectionClassName = this.autoReflectionClassByModule.get(reflectionModule);
									if (existingReflectionClassName != null && !existingReflectionClassName.equals(reflectionClassName)) {
										this.logSpecialError(
											"Clashing \"AutoReflectionClass:\" values for module '"
												+ reflectionModule
												+ "'. \""
												+ existingReflectionClassName
												+ "\" clashes with \""
												+ reflectionClassName
												+ "\" in '"
												+ other.getLogFileName()
												+ "'."
										);
									} else {
										this.autoReflectionClassByModule.put(reflectionModule, reflectionClassName);
									}
								}
							}

							if (other.obfuscateReferencesClassName != null) {
								if (this.obfuscateReferencesClassName != null && !this.obfuscateReferencesClassName.equals(other.obfuscateReferencesClassName)) {
									this.logSpecialError(
										"Clashing \"ObfuscateReferencesClass:\" values. \""
											+ this.obfuscateReferencesClassName
											+ "\" clashes with \""
											+ other.obfuscateReferencesClassName
											+ "\" in '"
											+ other.getLogFileName()
											+ "'."
									);
								} else {
									this.obfuscateReferencesClassName = other.obfuscateReferencesClassName;
								}
							}

							if (other.obfuscateReferencesByModule != null && !other.obfuscateReferencesByModule.isEmpty()) {
								if (this.obfuscateReferencesByModule == null) {
									this.obfuscateReferencesByModule = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(other.obfuscateReferencesByModule.size()));
								}

								for (Entry<String, String> referencesEntry : other.obfuscateReferencesByModule.entrySet()) {
									String referencesModule = referencesEntry.getKey();
									String referencesClassName = referencesEntry.getValue();
									String existingReferencesClassName = this.obfuscateReferencesByModule.get(referencesModule);
									if (existingReferencesClassName != null && !existingReferencesClassName.equals(referencesClassName)) {
										this.logSpecialError(
											"Clashing \"ObfuscateReferencesClass:\" values for module '"
												+ referencesModule
												+ "'. \""
												+ existingReferencesClassName
												+ "\" clashes with \""
												+ referencesClassName
												+ "\" in '"
												+ other.getLogFileName()
												+ "'."
										);
									} else {
										this.obfuscateReferencesByModule.put(referencesModule, referencesClassName);
									}
								}
							}

							if (other.parameterChangerDataClasses != null) {
								Triplet otherParameterChangeTriplet = other.parameterChangerDataClasses;
								if (this.parameterChangerDataClasses != null && !this.parameterChangerDataClasses.equals(other.parameterChangerDataClasses)) {
									this.logSpecialError(
										"Clashing \"MethodParameterChangeClasses:\" values. \"{"
											+ (String)this.parameterChangerDataClasses.getFirst()
											+ ", "
											+ (String)this.parameterChangerDataClasses.getSecond()
											+ ", "
											+ (String)this.parameterChangerDataClasses.getThird()
											+ "}\" clash with \"{"
											+ (String)otherParameterChangeTriplet.getFirst()
											+ ", "
											+ (String)otherParameterChangeTriplet.getSecond()
											+ ", "
											+ (String)otherParameterChangeTriplet.getThird()
											+ "}\" in '"
											+ other.getLogFileName()
											+ "'."
									);
								} else if (this.parameterChangerDataClasses == null || !this.parameterChangerDataClasses.equals(otherParameterChangeTriplet)) {
									this.parameterChangerDataClasses = new Triplet(otherParameterChangeTriplet.getFirst(), otherParameterChangeTriplet.getSecond(), otherParameterChangeTriplet.getThird());
								}
							}

							if (other.moduleParameterChangeClasses != null && !other.moduleParameterChangeClasses.isEmpty()) {
								if (this.moduleParameterChangeClasses == null) {
									this.moduleParameterChangeClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(other.moduleParameterChangeClasses.size()));
								}

								for (Entry<String, Triplet> parameterChangeEntry : other.moduleParameterChangeClasses.entrySet()) {
									String parameterChangeModule = parameterChangeEntry.getKey();
									Triplet parameterChangeTriplet = parameterChangeEntry.getValue();
									Triplet existingParameterChangeTriplet = this.moduleParameterChangeClasses.get(parameterChangeModule);
									if (existingParameterChangeTriplet != null && !existingParameterChangeTriplet.equals(parameterChangeTriplet)) {
										this.logSpecialError(
											"Clashing \"MethodParameterChangeClasses:\" values for module '"
												+ parameterChangeModule
												+ "'. \"{"
												+ (String)existingParameterChangeTriplet.getFirst()
												+ ", "
												+ (String)existingParameterChangeTriplet.getSecond()
												+ ", "
												+ (String)existingParameterChangeTriplet.getThird()
												+ "}\" clash with \"{"
												+ (String)parameterChangeTriplet.getFirst()
												+ ", "
												+ (String)parameterChangeTriplet.getSecond()
												+ ", "
												+ (String)parameterChangeTriplet.getThird()
												+ "}\" in '"
												+ other.getLogFileName()
												+ "'."
										);
									} else if (existingParameterChangeTriplet == null || !existingParameterChangeTriplet.equals(parameterChangeTriplet)) {
										this.moduleParameterChangeClasses.put(parameterChangeModule, new Triplet(parameterChangeTriplet.getFirst(), parameterChangeTriplet.getSecond(), parameterChangeTriplet.getThird()));
									}
								}
							}

							this.logFileName = this.logFileName + "<-" + other.logFileName;
							return this;
						}
					}
				}
			}
		}
	}

	public String getObfuscateReferencesClassName() {
		return this.obfuscateReferencesClassName;
	}

	public void addRawMethodMapping(String className, String oldMethodName, String parameterDescriptor, String returnType, String newMethodName, int accessFlags) {
		String binaryClassName = GlobalUtils.internalToBinaryName(className);
		MethodKey methodKey = this.createNormalizedMethodKey(oldMethodName, parameterDescriptor, returnType, accessFlags);
		MethodMappingInfo mappingInfo = this.createMethodMappingInfo(newMethodName, parameterDescriptor);
		this.rawMethodMappings.put(binaryClassName, methodKey, mappingInfo);
	}

	@Override
	public void setModuleParameterChangeClasses(List<String> classNames, String moduleName) {
		Triplet parameterChangeClasses = new Triplet(classNames.get(0), classNames.get(1), classNames.get(2));
		if (this.parameterChangeTriplets.containsKey(parameterChangeClasses)) {
			parameterChangeClasses = this.parameterChangeTriplets.get(parameterChangeClasses);
		}

		this.moduleParameterChangeClasses.put(moduleName, parameterChangeClasses);
	}

	public boolean hasOldPackageName(String packageName) {
		return this.oldToNewPackageNameMap != null && this.oldToNewPackageNameMap.containsKey(GlobalUtils.internalToBinaryName(packageName));
	}

	public List<KeyValuePair<AbstractMethodNode, ExtraParameter[]>> getExternalParameterChanges() {
		ArrayList<KeyValuePair<AbstractMethodNode, ExtraParameter[]>> externalParameterChanges = new ArrayList<>();
		if (this.methodParameterChangeMap != null) {
			for (Entry<AbstractMethodNode, ExtraParameter[]> parameterChangeEntry : this.methodParameterChangeMap.entrySet()) {
				AbstractMethodNode method = parameterChangeEntry.getKey();
				if (!method.isWorkspaceNode()) {
					externalParameterChanges.add(new KeyValuePair<>(method, parameterChangeEntry.getValue()));
				}
			}
		}

		return externalParameterChanges;
	}

	public ArrayList<KeyValuePair<FieldSignature, FieldSignature>> getFieldSignatureMappings(String className) {
		className = GlobalUtils.internalToBinaryName(className);
		if (this.processedFieldMappings != null) {
			Map<FieldSignature, FieldSignature> fieldMappings = this.processedFieldMappings.getGroup(className);
			if (fieldMappings != null) {
				ArrayList<KeyValuePair<FieldSignature, FieldSignature>> signatureMappings = new ArrayList<>();

				for (Entry<FieldSignature, FieldSignature> fieldEntry : fieldMappings.entrySet()) {
					signatureMappings.add(new KeyValuePair<>(fieldEntry.getKey(), fieldEntry.getValue()));
				}

				return signatureMappings;
			}
		}

		return null;
	}

	public boolean hasLoadedClasses() {
		return this.scriptContext.hasLoadedClasses();
	}

	public void addRawFieldMapping(String className, String fieldName, String descriptor, String newFieldName, int accessFlags) {
		String binaryClassName = GlobalUtils.internalToBinaryName(className);
		FieldKey fieldKey = this.createNormalizedFieldKey(fieldName, descriptor, accessFlags);
		this.rawFieldMappings.put(binaryClassName, fieldKey, newFieldName);
	}

	public ReferenceGraph<String, MethodSignature> getRenamedMethodGraph() {
		ReferenceGraph<String, MethodSignature> methodGraph = new ReferenceGraph<>(this.processedMethodMappings.getGroupCount());

		for (Entry<String, Map<MethodSignature, FlaggedMethodSignature>> classEntry : this.processedMethodMappings.getEntrySet()) {
			String className = classEntry.getKey();

			for (Entry<MethodSignature, FlaggedMethodSignature> methodEntry : classEntry.getValue().entrySet()) {
				MethodSignature oldSignature = methodEntry.getKey();
				MethodSignature newSignature = methodEntry.getValue();
				if (!oldSignature.getName().equals(newSignature.getName())) {
					methodGraph.addReference(GlobalUtils.binaryToInternalName(className), newSignature);
				}
			}
		}

		return methodGraph;
	}

	public boolean isParameterObfuscatedMethod(AbstractMethodNode method) {
		return this.parameterObfuscatedMethods != null && this.parameterObfuscatedMethods.contains(method);
	}

	@Override
	public void incrementClassMappingCount() {
		this.classMappingCount++;
	}

	private void addPackageNameMapping(String newPackageName, String oldPackageName, boolean verifyParentPackages) {
		if (this.oldToNewPackageNameMap == null) {
			int initialCapacity = GlobalUtils.getInitialCapacity(Math.min(this.packageMappingCount, 5));
			this.oldToNewPackageNameMap = GlobalUtils.newHashMap(initialCapacity);
			this.newToOldPackageNameMap = new DualKeyMap<>(initialCapacity);
		}

		if (newPackageName.length() > 0 && oldPackageName.length() == 0) {
			this.logSpecialError("Default package is mapped to \"" + newPackageName + "\". Current version cannot handle remapping of default package name.");
		}

		String previousNewPackageName = this.oldToNewPackageNameMap.put(oldPackageName, newPackageName);
		if (previousNewPackageName != null && !previousNewPackageName.equals(newPackageName)) {
			this.logSpecialError("Package \"" + oldPackageName + "\" is mapped to \"" + previousNewPackageName + "\" and \"" + newPackageName + "\"");
		}

		StringTokenizer packageNameTokenizer = new StringTokenizer(oldPackageName, ".");
		StringBuffer packagePrefixBuffer = new StringBuffer(oldPackageName.length());

		while (packageNameTokenizer.hasMoreTokens()) {
			String packageSegment = packageNameTokenizer.nextToken();
			if (packagePrefixBuffer.length() > 0) {
				packagePrefixBuffer.append(".");
			}

			packagePrefixBuffer.append(packageSegment);
			if (packagePrefixBuffer.length() == oldPackageName.length()) {
				break;
			}

			String ancestorPackageName = packagePrefixBuffer.toString();
			if (!this.oldToNewPackageNameMap.containsKey(ancestorPackageName)) {
				if (verifyParentPackages) {
					this.logSpecialError("Package \"" + oldPackageName + "\" is not preceded by a mapping for \"" + ancestorPackageName + "\"");
				} else {
					this.oldToNewPackageNameMap.put(ancestorPackageName, ancestorPackageName);
					this.newToOldPackageNameMap.put(ancestorPackageName, ancestorPackageName, ancestorPackageName);
				}
			}
		}

		String previousOldPackageName = this.newToOldPackageNameMap.put(newPackageName, oldPackageName, oldPackageName);
	}

	public void removePackageMapping(String packageName) {
		if (packageName != null) {
			packageName = GlobalUtils.internalToBinaryName(packageName);
		}

		if (this.oldToNewPackageNameMap != null) {
			String mappedPackageName = this.oldToNewPackageNameMap.get(packageName);
			this.oldToNewPackageNameMap.remove(packageName);
			this.newToOldPackageNameMap.remove(mappedPackageName, packageName);
			this.logWarning("Package '" + packageName + "' no longer appears in the opened classes and will be assumed to be no longer used.");
		}
	}

	public String getNewPackageName(String oldPackageName) {
		if (this.oldToNewPackageNameMap == null) {
			return null;
		} else {
			String mappedPackageName = this.oldToNewPackageNameMap.get(GlobalUtils.internalToBinaryName(oldPackageName));
			if (mappedPackageName == null) {
				return null;
			} else {
				String mappedInternalPackageName = GlobalUtils.binaryToInternalName(mappedPackageName);
				return mappedInternalPackageName.equals(oldPackageName) ? null : mappedInternalPackageName;
			}
		}
	}

	private void rebuildNewToOldClassNameMap(String otherLogFileName) {
		this.newToOldClassNameMap.clear();

		for (Entry<String, String> classNameEntry : this.oldToNewClassNameMap.entrySet()) {
			String oldClassName = classNameEntry.getKey();
			String oldPackageName = getPackageName(oldClassName);
			String newClassName = classNameEntry.getValue();
			String newSimpleClassName = getSimpleClassName(newClassName);
			if (oldPackageName.length() == 0) {
				String previousOldClassName = this.newToOldClassNameMap.put(newClassName, oldClassName);
				if (previousOldClassName != null) {
					this.logSpecialError(
						"Mapping clash while merging with \""
							+ otherLogFileName
							+ "\". Class name \""
							+ previousOldClassName
							+ "\"  \""
							+ newClassName
							+ "\". Cannot also map \""
							+ oldClassName
							+ "\" to \""
							+ newClassName
							+ "\" (1)"
					);
					return;
				}
			} else {
				String newPackageName = this.oldToNewPackageNameMap.get(oldPackageName);
				String newQualifiedClassName;
				if (newPackageName.length() > 0) {
					newQualifiedClassName = newPackageName + "." + newSimpleClassName;
				} else {
					newQualifiedClassName = newSimpleClassName;
				}

				classNameEntry.setValue(newQualifiedClassName);
				String previousQualifiedOldClassName = this.newToOldClassNameMap.put(newQualifiedClassName, oldClassName);
				if (previousQualifiedOldClassName != null) {
					this.logSpecialError(
						"Mapping clash while merging with \""
							+ otherLogFileName
							+ "\". Class name \""
							+ previousQualifiedOldClassName
							+ "\" already mapped to \""
							+ newQualifiedClassName
							+ "\". Cannot also map \""
							+ oldClassName
							+ "\" to \""
							+ newQualifiedClassName
							+ "\" (2)"
					);
					return;
				}
			}
		}
	}

	public boolean hasProcessedMethodMapping(String className, MethodSignature methodSignature) {
		if (className.equals("com/zelix/b/TestInterface11")) {
		}

		return this.processedMethodMappings != null && this.processedMethodMappings.contains(GlobalUtils.internalToBinaryName(className), methodSignature);
	}

	private String lookupNewFieldName(String className, String fieldName, String fieldType, HierarchicalMultiMap<String, FieldKey, String> fieldMappings) {
		if (fieldMappings != null) {
			String binaryClassName = GlobalUtils.internalToBinaryName(className);
			FieldKey fieldKey = this.createNormalizedFieldKey(fieldName, fieldType, 0);
			List<String> mappedFieldNames = fieldMappings.getValues(binaryClassName, fieldKey);
			if (mappedFieldNames == null) {
				return null;
			} else if (mappedFieldNames.size() == 1) {
				String mappedFieldName = mappedFieldNames.get(0);
				return fieldName.equals(mappedFieldName) ? null : mappedFieldName;
			} else {
				if (this.methodKeysHaveReturnType) {
					this.logFatalError("Class '" + binaryClassName + "' has multiple fields '" + fieldType + " " + fieldName + "'.");
				} else {
					this.logFatalError("Class '" + binaryClassName + "' has multiple fields named '" + fieldName + "' but field type not present.");
				}

				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public void addForwardClass(String packageName, String className, List<String> data) {
		if (!this.hasError) {
			this.parseFlowObfuscationEntry(className, data, false, "ForwardClass:", packageName);
		}
	}

	public boolean hasProcessedFieldMapping(String className, FieldSignature fieldSignature) {
		return this.processedFieldMappings != null && this.processedFieldMappings.contains(GlobalUtils.internalToBinaryName(className), fieldSignature);
	}

	@Override
	public void setObfuscateReferencesClass(String className) {
		this.obfuscateReferencesClassName = className;
	}

	public void removeRawMethodMapping(String className, String methodName, String parameterDescriptor, String returnType) {
		String binaryClassName = GlobalUtils.internalToBinaryName(className);
		MethodKey methodKey = this.createNormalizedMethodKey(methodName, parameterDescriptor, returnType, 0);
		this.rawMethodMappings.removeAll(binaryClassName, methodKey);
	}

	@Override
	public void setClassSourceName(String className, String sourceName) {
		if (this.oldClassNameToSourceName == null) {
			this.oldClassNameToSourceName = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.classMappingCount));
		}

		this.oldClassNameToSourceName.put(className, sourceName);
	}

	public String getNewClassName(String oldClassName) {
		if (this.oldToNewClassNameMap == null) {
			return null;
		} else {
			String binaryClassName = GlobalUtils.internalToBinaryName(oldClassName);
			String mappedClassName = this.oldToNewClassNameMap.get(binaryClassName);
			return mappedClassName != null && !mappedClassName.equals(binaryClassName) ? GlobalUtils.binaryToInternalName(mappedClassName) : null;
		}
	}

	public static class EntryKeyComparator implements Comparator<Entry<String, String>> {
		public final ChangeLogContext changeLogContext;

		public EntryKeyComparator(ChangeLogContext changeLogContext) {
			this.changeLogContext = changeLogContext;
		}

		public int compareByKey(Entry<String, String> entry1, Entry<String, String> entry2) {
			String firstKey = entry1.getKey();
			String secondKey = entry2.getKey();
			return firstKey.compareTo(secondKey);
		}

		@Override
		public int compare(Entry<String, String> o1, Entry<String, String> o2) {
			return this.compareByKey(o1, o2);
		}
	}
}

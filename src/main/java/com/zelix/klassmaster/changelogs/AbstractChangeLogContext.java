package com.zelix.klassmaster.changelogs;

import com.zelix.framework.signature.MethodSignature;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.flow.Condition;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.tools.parameterChanger.extra.ExtraParameter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.AtomicLong;
import com.zelix.utils.references.AtomicReference;

import java.util.*;

public abstract class AbstractChangeLogContext {
	public static final IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
	public static final char[] BASE50_CHARSET = "MQoSAkmVrHyDwxKuhLPOURetTIFYqWnpbzfjEsXcZlNCdvgJGBia".toCharArray();
	public static final char[] BASE64_CHARSET_ZKM = "8WSNy5E-tIAreXQwVzuqnBoskGYJgKP2pfhFD$Z3TRbLc714xHdiO6UC9Mj_0amvl".toCharArray();
	private static final char BASE64_PAD_CHAR = "8WSNy5E-tIAreXQwVzuqnBoskGYJgKP2pfhFD$Z3TRbLc714xHdiO6UC9Mj_0amvl".charAt(0);
	private static final char POSITIVE_SIGN_CHAR = BASE64_CHARSET_ZKM[BASE64_CHARSET_ZKM.length - 2];
	private static final char NEGATIVE_SIGN_CHAR = BASE64_CHARSET_ZKM[BASE64_CHARSET_ZKM.length - 1];
	private static final int[] EMPTY_INT_ARRAY = new int[0];
	private boolean hasParameterChangeData = false;
	private boolean hasParameterChangeLookupData = false;
	private boolean hasObfuscatedParameters = false;

	private static String encodeLongMagnitude(long value, String key, AtomicBool negative) {
		long obfuscatedValue = GlobalUtils.obfuscateLong(value, key);
		long magnitude;
		if (obfuscatedValue < 0L) {
			negative.set(true);
			magnitude = obfuscatedValue * -1L;
		} else {
			negative.set(false);
			magnitude = obfuscatedValue;
		}

		return GlobalUtils.longToBaseN(magnitude, BASE64_CHARSET_ZKM);
	}

	public static String decodeName(String encodedName, String key) {
		int payloadStart = payloadStartIndex(encodedName);
		return decodeNameInternal(encodedName.substring(payloadStart), key);
	}

	public static Long decodeParameterChangeData(String encodedData, String parameterDescriptors, AtomicReference<ExtraParameter[]> extraParameters, String key) {
		if (encodedData.length() < 14) {
			return decodeShortParameterData(encodedData, parameterDescriptors, extraParameters, key);
		} else {
			String saltBlock = encodedData.substring(0, 8);
			String parameterBlock = encodedData.substring(8);
			long saltValue = GlobalUtils.obfuscateLong(GlobalUtils.baseNToLong(saltBlock, "8WSNy5E-tIAreXQwVzuqnBoskGYJgKP2pfhFD$Z3TRbLc714xHdiO6UC9Mj_0amvl"), key);
			long packedParameterIndexes = GlobalUtils.obfuscateLong(GlobalUtils.baseNToLong(parameterBlock, "8WSNy5E-tIAreXQwVzuqnBoskGYJgKP2pfhFD$Z3TRbLc714xHdiO6UC9Mj_0amvl"), key);
			ArrayList<Integer> parameterIndexes = new ArrayList<>();

			for (int chunkIndex = 0; chunkIndex < 3; chunkIndex++) {
				int parameterIndex = (int)(packedParameterIndexes & 511L);
				if (parameterIndex <= 255) {
					parameterIndexes.add(INTEGER_CACHE.valueOf(parameterIndex));
				}

				packedParameterIndexes >>>= 9;
			}

			Collections.reverse(parameterIndexes);
			extraParameters.set(buildExtraParameters(parameterDescriptors, parameterIndexes));
			return saltValue;
		}
	}

	public static String encodeParameterChangeData(ParameterPattern parameterPattern, Long saltValue, MethodSignature methodSignature, String key) {
		int[] extraParameterIndexes;
		if (parameterPattern == null) {
			extraParameterIndexes = EMPTY_INT_ARRAY;
		} else {
			extraParameterIndexes = parameterPattern.getExtraParameterIndexes();
		}

		String encodedSalt = StringUtils.justificateString(GlobalUtils.longToBaseN(GlobalUtils.obfuscateLong(saltValue, key), BASE64_CHARSET_ZKM), 82, 8, BASE64_CHARSET_ZKM[0]);
		long packedParameterIndexes = 0L;

		for (int chunkIndex = 0; chunkIndex < 3; chunkIndex++) {
			if (chunkIndex < extraParameterIndexes.length) {
				packedParameterIndexes |= extraParameterIndexes[chunkIndex];
			} else {
				packedParameterIndexes |= 256L;
			}

			if (chunkIndex < 2) {
				packedParameterIndexes <<= 9;
			}
		}

		String encodedParameters = StringUtils.justificateString(
			GlobalUtils.longToBaseN(GlobalUtils.obfuscateLong(packedParameterIndexes, key), BASE64_CHARSET_ZKM), 82, 6, BASE64_CHARSET_ZKM[0]
		);
		return encodedSalt + encodedParameters;
	}

	private static long decodeLongBlock(String block, String key, boolean negative) {
		long magnitude = GlobalUtils.baseNToLong(block, "8WSNy5E-tIAreXQwVzuqnBoskGYJgKP2pfhFD$Z3TRbLc714xHdiO6UC9Mj_0amvl");
		if (negative) {
			magnitude *= -1L;
		}

		return GlobalUtils.obfuscateLong(magnitude, key);
	}

	public static String encodeMemberClassData(int classEntryIndex, int packageEntryIndex, String className, boolean upperCaseLetter, boolean lowerCaseLetter, Random random) {
		String classAndPackageIndexes = classEntryIndex + StringUtils.justificateString(String.valueOf(packageEntryIndex), 82, 4, 48);
		char classNameCharSum = 0;

		for (int i = 0; i < className.length(); i++) {
			classNameCharSum += className.charAt(i);
		}

		long memberClassValue = Long.parseLong(Long.toOctalString(Long.parseLong(classAndPackageIndexes))) + classNameCharSum;
		StringBuilder memberClassDataBuffer = new StringBuilder();
		String memberClassValueString = String.valueOf(memberClassValue);
		memberClassDataBuffer.append(memberClassValueString);
		if (upperCaseLetter) {
			char randomUpperCaseChar = (char)(random.nextInt(26) + 65);
			memberClassDataBuffer.append(' ');
			memberClassDataBuffer.append(randomUpperCaseChar);
		}

		if (lowerCaseLetter) {
			char randomLowerCaseChar = (char)(random.nextInt(26) + 97);
			if (!upperCaseLetter) {
				memberClassDataBuffer.append(' ');
			}

			memberClassDataBuffer.append(randomLowerCaseChar);
		}

		return memberClassDataBuffer.toString();
	}

	public static int[] decodeMemberClassIndexes(String encodedData, String className) {
		long encodedValue = Long.parseLong(encodedData);
		char classNameCharSum = 0;

		for (int i = 0; i < className.length(); i++) {
			classNameCharSum += className.charAt(i);
		}

		String octalIndexes = String.valueOf(Long.parseLong(String.valueOf(encodedValue - classNameCharSum), 8));
		if (octalIndexes.length() < 5) {
			octalIndexes = StringUtils.justificateString(octalIndexes, 82, 5, 48);
		}

		int indexesLength = octalIndexes.length();
		int classIndex = Integer.parseInt(octalIndexes.substring(0, indexesLength - 4));
		int packageIndex = Integer.parseInt(octalIndexes.substring(indexesLength - 4));
		return new int[]{classIndex, packageIndex};
	}

	public static String getEncoding() {
		return Options.ZKM_NEW_CHANGE_LOG_ENCODING;
	}

	public static String encodeFieldDescriptor(String descriptor, Integer elementClassIndex) {
		if (descriptor.equals("Z")) {
			return "1";
		} else if (descriptor.equals("I")) {
			return "2";
		} else if (descriptor.equals("Ljava/lang/String;")) {
			return "3";
		} else if (descriptor.equals("[I")) {
			return "4";
		} else if (descriptor.equals("[Ljava/lang/String;")) {
			return "5";
		} else {
			DebugUtils.assertNotNull(elementClassIndex, "'" + elementClassIndex + "' descriptor='" + descriptor + "'");
			return GlobalUtils.longToBaseN(elementClassIndex.intValue(), BASE50_CHARSET);
		}
	}

	public static String encodeLongBlock(long value, String key) {
		AtomicBool negativeFlag = new AtomicBool();
		String encodedMagnitude = encodeLongMagnitude(value, key, negativeFlag);
		char signChar;
		if (negativeFlag.get()) {
			signChar = NEGATIVE_SIGN_CHAR;
		} else {
			signChar = POSITIVE_SIGN_CHAR;
		}

		return signChar + StringUtils.justificateString(encodedMagnitude, 82, 11, BASE64_PAD_CHAR);
	}

	public static String encodeFlowObfuscationEntry(
		String fieldDescriptor, String fieldName, String setterName, String getterName, String booleanGetterName, boolean processingFlag, String key, Integer elementClassIndex, Random random
	) {
		StringBuilder entryBuffer = new StringBuilder();
		entryBuffer.append(encodeFieldDescriptor(fieldDescriptor, elementClassIndex));
		entryBuffer.append(encodeNameInternal(fieldName, key));
		entryBuffer.append(encodeAccessorNames(setterName, getterName, booleanGetterName, processingFlag, key, random));
		return entryBuffer.toString();
	}

	public static String encodeFlowObfuscationCondition(
		Condition condition, String key, Random random
	) {
		StringBuilder entryBuffer = new StringBuilder();
		entryBuffer.append('0');
		String fieldHashInput = condition.getFieldName() + " " + condition.getFieldDescriptor() + "java.lang.Object[]";
		entryBuffer.append(GlobalUtils.md5Hex(fieldHashInput));
		if (condition.hasSetterMethod()) {
			entryBuffer.append(' ');
			entryBuffer.append('a');
			String setterHashInput = condition.getSetterMethodName() + condition.getSetterMethodOriginalDescriptor() + "java.lang.Object[]";
			entryBuffer.append(GlobalUtils.md5Hex(setterHashInput));
		}

		if (condition.hasGetterMethod()) {
			entryBuffer.append(' ');
			entryBuffer.append('b');
			String getterHashInput = condition.getGetterMethodName() + condition.getGetterMethodOriginalDescriptor() + "java.lang.Object[]";
			entryBuffer.append(GlobalUtils.md5Hex(getterHashInput));
		}

		if (condition.hasBooleanGetterMethod()) {
			entryBuffer.append(' ');
			entryBuffer.append('c');
			String booleanGetterHashInput = condition.getBooleanGetterMethodName() + condition.getBooleanGetterMethodOriginalDescriptor() + "java.lang.Object[]";
			entryBuffer.append(GlobalUtils.md5Hex(booleanGetterHashInput));
		}

		char processingFlagChar;
		if (condition.isPositiveCondition()) {
			processingFlagChar = (char)(random.nextInt(26) + 65);
		} else {
			processingFlagChar = (char)(random.nextInt(26) + 97);
		}

		entryBuffer.append(' ');
		entryBuffer.append(processingFlagChar);
		return entryBuffer.toString();
	}

	public static String remapDescriptor(String descriptor, Map<String, String> classNameMap) {
		int arrayBracketIndex = descriptor.indexOf(91);
		String baseDescriptor;
		String arraySuffix;
		if (arrayBracketIndex > -1) {
			baseDescriptor = descriptor.substring(0, arrayBracketIndex);
			arraySuffix = descriptor.substring(arrayBracketIndex);
		} else {
			baseDescriptor = descriptor;
			arraySuffix = "";
		}

		if (!MethodSignature.isHumanReadablePrimitive(baseDescriptor)) {
			String mappedClassName = GlobalUtils.getMappedOrDefault(baseDescriptor, classNameMap);
			return mappedClassName + arraySuffix;
		} else {
			return descriptor;
		}
	}

	public static void decodeSaltValues(String encodedData, AtomicLong salt1, AtomicLong salt2, AtomicLong salt3, AtomicLong salt4, AtomicLong salt5, String key) {
		String encodedBlock = encodedData.substring(0, 12);
		boolean negativeFlag = encodedBlock.charAt(0) == NEGATIVE_SIGN_CHAR;
		salt1.set(decodeLongBlock(encodedBlock.substring(1), key, negativeFlag));
		encodedBlock = encodedData.substring(12, 24);
		negativeFlag = encodedBlock.charAt(0) == NEGATIVE_SIGN_CHAR;
		salt2.set(decodeLongBlock(encodedBlock.substring(1), key, negativeFlag));
		encodedBlock = encodedData.substring(24, 36);
		negativeFlag = encodedBlock.charAt(0) == NEGATIVE_SIGN_CHAR;
		salt3.set(decodeLongBlock(encodedBlock.substring(1), key, negativeFlag));
		encodedBlock = encodedData.substring(36, 48);
		negativeFlag = encodedBlock.charAt(0) == NEGATIVE_SIGN_CHAR;
		salt4.set(decodeLongBlock(encodedBlock.substring(1), key, negativeFlag));
		encodedBlock = encodedData.substring(48, 60);
		negativeFlag = encodedBlock.charAt(0) == NEGATIVE_SIGN_CHAR;
		salt5.set(decodeLongBlock(encodedBlock.substring(1), key, negativeFlag));
	}

	public static Long decodeShortParameterData(String encodedData, String parameterDescriptors, AtomicReference<ExtraParameter[]> extraParameters, String key) {
		long packedParameterIndexes = GlobalUtils.obfuscateLong(GlobalUtils.baseNToLong(encodedData, "8WSNy5E-tIAreXQwVzuqnBoskGYJgKP2pfhFD$Z3TRbLc714xHdiO6UC9Mj_0amvl"), key);
		ArrayList<Integer> parameterIndexes = new ArrayList<>();

		for (int chunkIndex = 0; chunkIndex < 3; chunkIndex++) {
			int parameterIndex = (int)(packedParameterIndexes & 511L);
			if (parameterIndex <= 255) {
				parameterIndexes.add(INTEGER_CACHE.valueOf(parameterIndex));
			}

			packedParameterIndexes >>>= 9;
		}

		Collections.reverse(parameterIndexes);
		extraParameters.set(buildExtraParameters(parameterDescriptors, parameterIndexes));
		return packedParameterIndexes;
	}

	public static String decodeNameInternal(String encodedValue, String key) {
		long encodedLong = Long.parseLong(encodedValue);
		char keyCharSum = 0;

		for (int i = 0; i < key.length(); i++) {
			keyCharSum += key.charAt(i);
		}

		String octalDigits = String.valueOf(Long.parseLong(String.valueOf(encodedLong - keyCharSum), 8));
		StringBuilder nameBuffer = new StringBuilder();

		while (octalDigits.length() > 3) {
			int lastChunkStart = octalDigits.length() - 3;
			String digitChunk = octalDigits.substring(lastChunkStart);
			nameBuffer.append((char)Integer.parseInt(digitChunk));
			octalDigits = octalDigits.substring(0, lastChunkStart);
		}

		nameBuffer.append((char)Integer.parseInt(octalDigits));
		return nameBuffer.reverse().toString();
	}

	private static String encodeNameInternal(String name, String key) {
		StringBuilder charCodeBuffer = new StringBuilder(name.length() * 3);

		for (int i = 0; i < name.length(); i++) {
			charCodeBuffer.append(StringUtils.justificateString(String.valueOf((int) name.charAt(i)), 82, 3, 48));
		}

		String octalString = Long.toOctalString(Long.parseLong(charCodeBuffer.toString()));
		int keyCharSum = 0;

		for (int keyIndex = 0; keyIndex < key.length(); keyIndex++) {
			keyCharSum += key.charAt(keyIndex);
		}

		return String.valueOf(Long.parseLong(octalString) + keyCharSum);
	}

	public static int payloadStartIndex(String encodedValue) {
		char firstChar = encodedValue.charAt(0);
		if (firstChar >= '0' && firstChar <= '9') {
			return 1;
		} else {
			char[] valueChars = encodedValue.toCharArray();
			int payloadStart = valueChars.length;

			for (int i = 0; i < valueChars.length; i++) {
				char currentChar = encodedValue.charAt(i);
				if (currentChar >= '0' && currentChar <= '9') {
					payloadStart = i;
					break;
				}
			}

			return payloadStart;
		}
	}

	public static String encodeAccessorNames(String setterName, String getterName, String booleanGetterName, boolean processingFlag, String key, Random random) {
		StringBuilder accessorNamesBuffer = new StringBuilder();
		if (setterName != null) {
			accessorNamesBuffer.append(' ');
			accessorNamesBuffer.append('a');
			accessorNamesBuffer.append(encodeNameInternal(setterName, key));
		}

		if (getterName != null) {
			accessorNamesBuffer.append(' ');
			accessorNamesBuffer.append('b');
			accessorNamesBuffer.append(encodeNameInternal(getterName, key));
		}

		if (booleanGetterName != null) {
			accessorNamesBuffer.append(' ');
			accessorNamesBuffer.append('c');
			accessorNamesBuffer.append(encodeNameInternal(booleanGetterName, key));
		}

		char processingFlagChar;
		if (processingFlag) {
			processingFlagChar = (char)(random.nextInt(26) + 65);
		} else {
			processingFlagChar = (char)(random.nextInt(26) + 97);
		}

		accessorNamesBuffer.append(' ');
		accessorNamesBuffer.append(processingFlagChar);
		return accessorNamesBuffer.toString();
	}

	public static String encodeSaltValues(long salt1, long salt2, long salt3, long salt4, long salt5, String key) {
		StringBuilder encodedSaltsBuffer = new StringBuilder();
		encodedSaltsBuffer.append(encodeLongBlock(salt1, key));
		encodedSaltsBuffer.append(encodeLongBlock(salt2, key));
		encodedSaltsBuffer.append(encodeLongBlock(salt3, key));
		encodedSaltsBuffer.append(encodeLongBlock(salt4, key));
		encodedSaltsBuffer.append(encodeLongBlock(salt5, key));
		return encodedSaltsBuffer.toString();
	}

	private static ExtraParameter[] buildExtraParameters(String parameterDescriptors, List<Integer> parameterIndexes) {
		ExtraParameter[] extraParameters = new ExtraParameter[parameterIndexes.size()];
		List<String> parsedDescriptors = MethodSignature.parseParameterDescriptors(parameterDescriptors);

		for (int i = 0; i < parameterIndexes.size(); i++) {
			int parameterIndex = parameterIndexes.get(i);
			String parameterDescriptor = parsedDescriptors.get(parameterIndex);
			extraParameters[i] = new ExtraParameter(parameterIndex, parsedDescriptors.get(parameterIndex).charAt(0));
		}

		return extraParameters;
	}

	public abstract void addClassMapping(String newClassName, String oldClassName);

	public abstract boolean isLineNumberChangeSupported();

	public void setParameterChangeData(boolean value) {
		this.hasParameterChangeData = value;
	}

	public abstract void addInnerClassMappings(String className, Map<Integer, Integer> lineNumberMappings);

	public abstract void addTraceBackClass(String className, List<String> data);

	public abstract void addMethodMappings(String className, MultiMap<MethodKey, MethodMappingInfo> methodMappings);

	public abstract void addPackageMapping(String newPackageName, String oldPackageName);

	public boolean hasParameterChangeLookupData() {
		return this.hasParameterChangeLookupData;
	}

	public void markHasObfuscatedParameters() {
		this.hasObfuscatedParameters = true;
	}

	public void setParameterChangeLookupData(boolean value) {
		this.hasParameterChangeLookupData = value;
	}

	public abstract void setModuleObfuscateRefsClass(String className, String moduleName);

	public abstract void addFieldMappings(String className, MultiMap<FieldKey, String> fieldMappings);

	public String formatParameterList(String[] parameters) {
		StringBuilder parameterListBuffer = new StringBuilder();

		for (int i = 0; i < parameters.length; i++) {
			parameterListBuffer.append(parameters[i]);
			if (i < parameters.length - 1) {
				parameterListBuffer.append(", ");
			}
		}

		return parameterListBuffer.toString();
	}

	public String remapParameterList(String parameterList, Map<String, String> classNameMap) {
		if (parameterList != null && parameterList.length() != 0) {
			StringBuilder remappedListBuffer = new StringBuilder();
			StringTokenizer descriptorTokenizer = new StringTokenizer(parameterList, ",");

			while (descriptorTokenizer.hasMoreTokens()) {
				String descriptor = descriptorTokenizer.nextToken().trim();
				remappedListBuffer.append(remapDescriptor(descriptor, classNameMap));
				if (descriptorTokenizer.hasMoreTokens()) {
					remappedListBuffer.append(", ");
				}
			}

			return remappedListBuffer.toString();
		} else {
			return "";
		}
	}

	public abstract void addMemberClass(String className, List<String> data);

	public boolean hasObfuscatedParameters() {
		return this.hasObfuscatedParameters;
	}

	public abstract void incrementPackageMappingCount();

	public abstract void setModuleAutoReflectionClass(String className, String moduleName);

	public abstract boolean isFieldNameChangeSupported();

	public boolean hasParameterChangeData() {
		return this.hasParameterChangeData;
	}

	public abstract void setParameterChangeClasses(List<String> classNames);

	public abstract void setAutoReflectionClass(String className);

	public abstract void setModuleParameterChangeClasses(List<String> classNames, String moduleName);

	public abstract void incrementClassMappingCount();

	public Integer getCachedInteger(int value) {
		return INTEGER_CACHE.valueOf(value);
	}

	public abstract void addForwardClass(String packageName, String className, List<String> data);

	public abstract void setObfuscateReferencesClass(String className);

	public abstract void setClassSourceName(String className, String sourceName);
}

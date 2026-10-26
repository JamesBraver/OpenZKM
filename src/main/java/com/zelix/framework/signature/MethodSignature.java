package com.zelix.framework.signature;

import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import java.beans.Introspector;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class MethodSignature extends AbstractMethodKey {
	public static final Map<String, String> PRIMITIVE_NAME_TO_DESCRIPTOR = GlobalUtils.newHashMap(17);
	public static final MethodSignature DEFAULT_CONSTRUCTOR = new MethodSignature("<init>()V");
	public static final MethodSignature STATIC_INITIALIZER = new MethodSignature("<clinit>()V");
	public static final MethodSignature MAIN_METHOD = new MethodSignature("main([Ljava/lang/String;)V");
	public static final MethodSignature INVOKE_METHOD = new MethodSignature("invoke([Ljava/lang/Object;)Ljava/lang/Object;");
	public static final MethodSignature INVOKE_EXACT_METHOD = new MethodSignature("invokeExact([Ljava/lang/Object;)Ljava/lang/Object;");
	private static final Set<String> METHOD_HANDLE_NAMES = GlobalUtils.newHashSet();
	private static final Set<String> VAR_HANDLE_NAMES = GlobalUtils.newHashSet();
	private final String returnDescriptor;
	private final int cachedHashCode;
	private MethodParameterKey parameterKeyCache;

	public MethodSignature(String name, String parameterDescriptor, String returnDescriptor) {
		super(name, parameterDescriptor);
		this.returnDescriptor = returnDescriptor.intern();
		this.cachedHashCode = (name + parameterDescriptor + returnDescriptor).hashCode();
	}

	private MethodSignature(String name, String parameterDescriptor, String returnDescriptor, int cachedHashCode, MethodParameterKey parameterKeyCache) {
		super(name, parameterDescriptor);
		this.returnDescriptor = returnDescriptor.intern();
		this.cachedHashCode = cachedHashCode;
		this.parameterKeyCache = parameterKeyCache;
	}

	public MethodSignature(String nameAndDescriptor) {
		this(nameAndDescriptor.substring(0, nameAndDescriptor.indexOf("(")), nameAndDescriptor.substring(nameAndDescriptor.indexOf("(")));
	}

	public MethodSignature(String name, String descriptor) {
		super(name, descriptor);
		this.returnDescriptor = descriptor.substring(this.parameterDescriptor.length()).intern();
		this.cachedHashCode = (name + this.parameterDescriptor + this.returnDescriptor).hashCode();
	}

	public MethodSignature(AbstractMethodKey methodKey) {
		super(methodKey);
		if (methodKey instanceof MethodSignature) {
			MethodSignature methodSignature = (MethodSignature)methodKey;
			this.returnDescriptor = methodSignature.returnDescriptor;
		} else if (methodKey instanceof MethodParameterKey) {
			MethodParameterKey methodParameterKey = (MethodParameterKey)methodKey;
			this.returnDescriptor = methodParameterKey.getReturnDescriptor();
			DebugUtils.assertNotNull(this.returnDescriptor, "Null return type in '" + this.name + this.parameterDescriptor + "'");
		} else {
			this.returnDescriptor = null;
		}

		this.cachedHashCode = (this.name + this.parameterDescriptor + this.returnDescriptor).hashCode();
	}

	public static String fromHumanReadableParameters(String humanReadableParams) {
		return fromHumanReadableParameters(humanReadableParams, null);
	}

	public static String formatRemappedParameters(String parameterDescriptor, Map<String, String> classNameMap) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(parameterDescriptor);
		int parameterCount = parameterDescriptors.size();

		for (int i = 0; i < parameterCount; i++) {
			String parameterTypeDescriptor = parameterDescriptors.get(i);
			builder.append(formatRemappedType(parameterTypeDescriptor, classNameMap));
			if (i + 1 < parameterCount) {
				builder.append(", ");
			}
		}

		return builder.append(')').toString();
	}

	public static final String extractParameterDescriptor(String descriptor) {
		return descriptor.substring(0, descriptor.lastIndexOf(")") + 1);
	}

	public static MethodSignature remap(MethodSignature signature, Map<String, String> classNameMap, boolean useInternalNames) {
		String fullDescriptor = signature.getFullDescriptor();
		if (!useInternalNames) {
			fullDescriptor = fullDescriptor.replace('/', '.');
		}

		String remappedDescriptor = remapDescriptor(fullDescriptor, classNameMap);
		if (remappedDescriptor.equals(fullDescriptor)) {
			return signature;
		} else {
			if (!useInternalNames) {
				remappedDescriptor = remappedDescriptor.replace('.', '/');
			}

			return new MethodSignature(signature.getName(), remappedDescriptor);
		}
	}

	public static String formatRemappedType(String descriptor, Map<String, String> classNameMap) {
		String arraySuffix = "";
		String humanReadableType = TypeWriter.descriptorToHumanReadable(descriptor, false, false);
		if (humanReadableType.endsWith("]")) {
			int arrayBracketIndex = humanReadableType.indexOf(91);
			arraySuffix = humanReadableType.substring(arrayBracketIndex);
			humanReadableType = humanReadableType.substring(0, arrayBracketIndex);
		}

		String mappedType = GlobalUtils.getMappedOrDefault(humanReadableType, classNameMap);
		return mappedType.replace('/', '.') + arraySuffix;
	}

	public static String fromHumanReadableType(String humanReadableType, Map<String, String> classNameMap) {
		StringBuilder builder = new StringBuilder(Math.max(humanReadableType.length(), 16));
		int arrayDepth = 0;
		int arrayMarkerIndex = humanReadableType.indexOf("[]");
		String elementTypeName;
		if (arrayMarkerIndex > -1) {
			arrayDepth = StringUtils.countOccurrences(humanReadableType, "[]");
			elementTypeName = humanReadableType.substring(0, arrayMarkerIndex);
		} else {
			elementTypeName = humanReadableType;
		}

		for (int i = 0; i < arrayDepth; i++) {
			builder.append("[");
		}

		StringTokenizer nameTokenizer = new StringTokenizer(elementTypeName, ".");
		int namePartCount = nameTokenizer.countTokens();
		if (namePartCount == 1) {
			String simpleTypeName = nameTokenizer.nextToken();
			String primitiveDescriptor = PRIMITIVE_NAME_TO_DESCRIPTOR.get(simpleTypeName);
			if (primitiveDescriptor != null) {
				builder.append(primitiveDescriptor);
			} else {
				simpleTypeName = GlobalUtils.getMappedOrDefault(simpleTypeName, classNameMap);
				builder.append("L");
				builder.append(simpleTypeName);
				builder.append(";");
			}
		} else {
			StringBuilder internalNameBuilder = new StringBuilder();

			for (int namePartIndex = 0; namePartIndex < namePartCount; namePartIndex++) {
				if (namePartIndex > 0) {
					internalNameBuilder.append("/");
				}

				internalNameBuilder.append(nameTokenizer.nextToken());
			}

			String mappedInternalName = GlobalUtils.getMappedOrDefault(internalNameBuilder.toString(), classNameMap);
			builder.append("L");
			builder.append(mappedInternalName);
			builder.append(";");
		}

		return builder.toString();
	}

	public static String createAccessorName(String prefix, String fieldName) {
		StringBuilder builder = new StringBuilder(prefix.length() + fieldName.length());
		builder.append(prefix);
		char firstChar = fieldName.charAt(0);
		if (Character.isLowerCase(firstChar) && prefix.length() > 0 && (fieldName.length() == 1 || !Character.isUpperCase(fieldName.charAt(1)))) {
			builder.append(Character.toUpperCase(firstChar));
			builder.append(fieldName.substring(1));
		} else {
			builder.append(fieldName);
		}

		return builder.toString();
	}

	public static String fromHumanReadableType(String humanReadableType) {
		return fromHumanReadableType(humanReadableType, null);
	}

	public static boolean isHandleMethod(String ownerInternalName, String methodName, ClassHierarchy classHierarchy) {
		if (METHOD_HANDLE_NAMES.contains(methodName)) {
			return ownerInternalName.equals("java/lang/invoke/MethodHandle") ? true : classHierarchy.isClassInstanceOf(ownerInternalName, "java/lang/invoke/MethodHandle");
		} else if (VAR_HANDLE_NAMES.contains(methodName)) {
			return ownerInternalName.equals("java/lang/invoke/VarHandle") ? true : classHierarchy.isClassInstanceOf(ownerInternalName, "java/lang/invoke/VarHandle");
		} else {
			return false;
		}
	}

	public static boolean isHumanReadablePrimitive(String typeName) {
		return PRIMITIVE_NAME_TO_DESCRIPTOR.containsKey(typeName);
	}

	public static String fromInternalName(String internalName) {
		String canonicalInternalName = internalName.replace('.', '/');
		StringBuilder builder = new StringBuilder(Math.max(canonicalInternalName.length(), 16));
		int nameLength = canonicalInternalName.length();
		if ((nameLength > 1 || nameLength == 1 && !TypeWriter.isPrimitiveDescriptor(canonicalInternalName)) && canonicalInternalName.charAt(0) != '[' && canonicalInternalName.charAt(nameLength - 1) != ';') {
			builder.append('L');
			builder.append(canonicalInternalName);
			builder.append(';');
			return builder.toString();
		} else {
			int arrayBracketIndex = canonicalInternalName.lastIndexOf(91);
			if (arrayBracketIndex > -1 && (arrayBracketIndex < nameLength - 2 || arrayBracketIndex < nameLength - 1 && !TypeWriter.isPrimitiveDescriptor(canonicalInternalName.substring(arrayBracketIndex + 1)))) {
				String elementTypeName = canonicalInternalName.substring(arrayBracketIndex + 1);
				if (elementTypeName.charAt(0) != 'L' && elementTypeName.charAt(elementTypeName.length() - 1) != ';') {
					builder.append(canonicalInternalName.substring(0, arrayBracketIndex + 1));
					builder.append('L');
					builder.append(elementTypeName);
					builder.append(';');
					return builder.toString();
				}
			}

			return canonicalInternalName;
		}
	}

	public static final String extractReturnDescriptor(String descriptor) {
		return descriptor.substring(descriptor.lastIndexOf(")") + 1);
	}

	public static List<String> parseParameterDescriptors(String descriptor) {
		return TypeWriter.parseParameterDescriptors(descriptor);
	}

	public static String remapDescriptor(String descriptor, Map<String, String> classNameMap) {
		List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(descriptor);
		StringBuilder builder = new StringBuilder();
		builder.append("(");

		for (int i = 0; i < parameterDescriptors.size(); i++) {
			String remappedParameterDescriptor = remapType(parameterDescriptors.get(i), classNameMap);
			builder.append(remappedParameterDescriptor);
		}

		builder.append(")");
		String remappedReturnDescriptor = remapType(descriptor.substring(descriptor.indexOf(")") + 1), classNameMap);
		builder.append(remappedReturnDescriptor);
		return builder.toString();
	}

	public static String fromHumanReadableParameters(String humanReadableParams, Map<String, String> classNameMap) {
		if (humanReadableParams != null && humanReadableParams.length() != 0) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("(");
			StringTokenizer parameterTokenizer = new StringTokenizer(humanReadableParams, ",");

			while (parameterTokenizer.hasMoreTokens()) {
				String parameterTypeName = parameterTokenizer.nextToken().trim();
				buffer.append(fromHumanReadableType(parameterTypeName, classNameMap));
			}

			return buffer.append(')').toString();
		} else {
			return "()";
		}
	}

	public static String getFieldNameFromAccessor(String accessorName, String prefix) {
		String fieldNameCandidate = accessorName.substring(prefix.length());
		return fieldNameCandidate.length() > 0 ? Introspector.decapitalize(fieldNameCandidate) : "";
	}

	public static String remapType(String descriptor, Map<String, String> classNameMap) {
		int arrayDepth = 0;

		for (int charIndex = 0; charIndex < descriptor.length() && descriptor.charAt(charIndex) == '['; charIndex++) {
			arrayDepth++;
		}

		String elementDescriptor = descriptor.substring(arrayDepth);
		String remappedElementDescriptor;
		if (elementDescriptor.length() > 1) {
			elementDescriptor = elementDescriptor.substring(1, elementDescriptor.length() - 1);
			remappedElementDescriptor = "L" + GlobalUtils.getMappedOrDefault(elementDescriptor, classNameMap) + ";";
		} else {
			remappedElementDescriptor = elementDescriptor;
		}

		return arrayDepth > 0 ? descriptor.substring(0, arrayDepth) + remappedElementDescriptor : remappedElementDescriptor;
	}

	public String toFullString() {
		return TypeWriter.descriptorToHumanReadable(this.returnDescriptor) + " " + this.toHumanReadableString();
	}

	public String toSignatureString(Map<String, String> classNameMap, String constructorName) {
		return this.getHumanReadableReturnType(classNameMap) + " " + (constructorName != null && this.isConstructor() ? constructorName : this.name) + this.getRemappedParameters(classNameMap);
	}

	@Override
	public final String getReturnDescriptor() {
		return this.returnDescriptor;
	}

	public String getHumanReadableReturnType(Map<String, String> classNameMap) {
		return formatRemappedType(this.returnDescriptor, classNameMap);
	}

	@Override
	public Object clone() {
		return new MethodSignature(this.name, this.parameterDescriptor, this.returnDescriptor, this.cachedHashCode, this.parameterKeyCache);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof MethodSignature) {
			MethodSignature other = (MethodSignature)obj;
			return this.cachedHashCode == other.cachedHashCode
				&& this.name == other.name
				&& this.parameterDescriptor == other.parameterDescriptor
				&& this.returnDescriptor == other.returnDescriptor;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.cachedHashCode;
	}

	public String toSignatureString(Map<String, String> classNameMap) {
		return this.toSignatureString(classNameMap, null);
	}

	public final String getFullDescriptor() {
		StringBuilder builder = new StringBuilder(this.parameterDescriptor.length() + this.returnDescriptor.length());
		builder.append(this.parameterDescriptor);
		builder.append(this.returnDescriptor);
		return builder.toString();
	}

	public String toFullHumanReadableString() {
		StringBuilder builder = new StringBuilder();
		if (this.returnDescriptor != null && this.returnDescriptor.length() > 0) {
			builder.append(TypeWriter.descriptorToHumanReadable(this.returnDescriptor));
			builder.append(" ");
		}

		builder.append(this.toHumanReadableString());
		return builder.toString();
	}

	public MethodParameterKey getParameterKey() {
		if (this.parameterKeyCache == null) {
			this.parameterKeyCache = new MethodParameterKey(this.name, this.parameterDescriptor, this.returnDescriptor);
		}

		return this.parameterKeyCache;
	}

	@Override
	public String toString() {
		return TypeWriter.descriptorToHumanReadable(this.returnDescriptor) + " " + this.toHumanReadableString();
	}

	static {
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("byte", "B");
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("char", "C");
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("double", "D");
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("float", "F");
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("int", "I");
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("long", "J");
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("short", "S");
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("boolean", "Z");
		PRIMITIVE_NAME_TO_DESCRIPTOR.put("void", "V");
		METHOD_HANDLE_NAMES.add("invoke");
		METHOD_HANDLE_NAMES.add("invokeExact");
		METHOD_HANDLE_NAMES.add("invokeBasic");
		METHOD_HANDLE_NAMES.add("linkToVirtual");
		METHOD_HANDLE_NAMES.add("linkToStatic");
		METHOD_HANDLE_NAMES.add("linkToSpecial");
		METHOD_HANDLE_NAMES.add("linkToInterface");
		VAR_HANDLE_NAMES.add("get");
		VAR_HANDLE_NAMES.add("set");
		VAR_HANDLE_NAMES.add("getVolatile");
		VAR_HANDLE_NAMES.add("setVolatile");
		VAR_HANDLE_NAMES.add("getOpaque");
		VAR_HANDLE_NAMES.add("setOpaque");
		VAR_HANDLE_NAMES.add("getAcquire");
		VAR_HANDLE_NAMES.add("setRelease");
		VAR_HANDLE_NAMES.add("compareAndSet");
		VAR_HANDLE_NAMES.add("compareAndExchange");
		VAR_HANDLE_NAMES.add("compareAndExchangeAcquire");
		VAR_HANDLE_NAMES.add("compareAndExchangeRelease");
		VAR_HANDLE_NAMES.add("weakCompareAndSet");
		VAR_HANDLE_NAMES.add("weakCompareAndSetPlain");
		VAR_HANDLE_NAMES.add("weakCompareAndSetAcquire");
		VAR_HANDLE_NAMES.add("weakCompareAndSetRelease");
		VAR_HANDLE_NAMES.add("getAndSet");
		VAR_HANDLE_NAMES.add("getAndSetAcquire");
		VAR_HANDLE_NAMES.add("getAndSetRelease");
		VAR_HANDLE_NAMES.add("getAndAdd");
		VAR_HANDLE_NAMES.add("getAndAddAcquire");
		VAR_HANDLE_NAMES.add("getAndAddReleasegetAndAddRelease");
		VAR_HANDLE_NAMES.add("getAndBitwiseOr");
		VAR_HANDLE_NAMES.add("getAndBitwiseOrAcquire");
		VAR_HANDLE_NAMES.add("getAndBitwiseOrRelease");
		VAR_HANDLE_NAMES.add("getAndBitwiseAnd");
		VAR_HANDLE_NAMES.add("getAndBitwiseAndAcquire");
		VAR_HANDLE_NAMES.add("getAndBitwiseAndRelease");
		VAR_HANDLE_NAMES.add("getAndBitwiseXor");
		VAR_HANDLE_NAMES.add("getAndBitwiseXorAcquire");
		VAR_HANDLE_NAMES.add("getAndBitwiseXorRelease");
	}
}

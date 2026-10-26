package com.zelix.framework.writer.base;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.constant.*;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.*;
import com.zelix.klassmaster.config.Options;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.ClassHierarchyNode;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class TypeWriter implements Comparable<TypeWriter> {
	public static final Map<String, String> descriptorToPrimitiveNameMap = GlobalUtils.newHashMap(17);
	public static final Map<String, String> descriptorToWrapperNameMap = GlobalUtils.newHashMap(17);
	public static final Map<String, String> wrapperNameToDescriptorMap = GlobalUtils.newHashMap(17);
	public int tag;
	public AbstractConstantPoolWriter constantPool;

	public TypeWriter(int tag, AbstractConstantPoolWriter constantPool) {
		this.tag = tag;
		this.constantPool = constantPool;
	}

	public static String remapDescriptor(String descriptor, Map<String, String> map) {
		int classStartIndex = 0;

		while ((classStartIndex = descriptor.indexOf(76, classStartIndex)) != -1) {
			int classEndIndex = descriptor.indexOf(59, classStartIndex);
			if (classEndIndex == -1) {
				break;
			}

			String rawClassName = descriptor.substring(classStartIndex + 1, classEndIndex);
			String internalName = null;
			if (rawClassName.indexOf(46) > 0 && rawClassName.lastIndexOf(46) < rawClassName.length() && rawClassName.indexOf(47) == -1) {
				internalName = rawClassName.replace('.', '/');
			}

			String mappedName = map.get(internalName == null ? rawClassName : internalName);
			if (mappedName != null) {
				if (internalName != null) {
					mappedName = mappedName.replace('/', '.');
				}

				if (!mappedName.equals(rawClassName)) {
					descriptor = descriptor.substring(0, classStartIndex + 1) + mappedName + descriptor.substring(classEndIndex);
					classEndIndex = classEndIndex + mappedName.length() - rawClassName.length();
				}
			}

			classStartIndex = classEndIndex;
		}

		return descriptor;
	}

	public static String descriptorToHumanReadable(String descriptor, boolean strict) {
		return descriptorToHumanReadable(descriptor, strict, true);
	}

	public static List<String> getParameterTypeNames(String descriptor) {
		List<String> parameterTypes = parseParameterDescriptors(descriptor);
		int parameterCount = parameterTypes.size();

		for (int i = 0; i < parameterCount; i++) {
			String parameterType = parameterTypes.get(i);
			parameterTypes.set(i, descriptorToInternalName(parameterType));
		}

		return parameterTypes;
	}

	public static String normalizeToInternalName(String className) {
		String classNameBody = className;
		int classNameLength = className.length();
		if (className.endsWith(";")) {
			StringBuffer prefixBuffer = new StringBuffer();
			if (className.startsWith("[")) {
				prefixBuffer.append('[');

				for (int i = 1; i < classNameLength; i++) {
					char currentChar = classNameBody.charAt(i);
					if (currentChar != '[') {
						if (currentChar != 'L') {
							return null;
						}

						prefixBuffer.append('L');
						break;
					}

					prefixBuffer.append('[');
				}
			} else {
				if (!className.startsWith("L")) {
					return null;
				}

				prefixBuffer.append('L');
			}

			classNameBody = classNameBody.substring(prefixBuffer.length(), classNameLength - 1);
		}

		String internalName;
		if (classNameBody.indexOf(46) != -1) {
			internalName = classNameBody.replace('.', '/');
		} else {
			internalName = classNameBody;
		}

		int bodyLength = classNameBody.length();
		int dotIndex = classNameBody.indexOf(46);
		int lastDotIndex = classNameBody.lastIndexOf(46);
		int slashIndex = classNameBody.indexOf(47);
		int lastSlashIndex = classNameBody.lastIndexOf(47);
		return internalName.length() >= 3
				&& Character.isJavaIdentifierStart(internalName.charAt(0))
				&& (dotIndex > 0 && lastDotIndex < bodyLength - 1 && slashIndex == -1 || slashIndex > 0 && lastSlashIndex < bodyLength - 1 && dotIndex == -1)
			? internalName
			: null;
	}

	public static List<ClassNode> getReferencedWorkspaceClasses(String descriptor) {
		ArrayList<ClassNode> workspaceClasses = new ArrayList<>();
		List<String> typeDescriptors = getAllTypeDescriptors(descriptor);

		for (int i = 0; i < typeDescriptors.size(); i++) {
			ClassNode workspaceClass = getArrayElementWorkspaceNode(typeDescriptors.get(i));
			if (workspaceClass != null) {
				workspaceClasses.add(workspaceClass);
			}
		}

		return workspaceClasses;
	}

	public static String getReturnDescriptor(String descriptor) {
		return descriptor.substring(descriptor.indexOf(")") + 1);
	}

	public static String getStackReturnType(NameAndTypeWriter nameAndTypeWriter) {
		String descriptor = nameAndTypeWriter.getDescriptor();
		String stackType;
		if (isMethodDescriptor(descriptor)) {
			stackType = getReturnDescriptor(descriptor);
		} else {
			stackType = descriptor;
		}

		if (stackType.equals("V")) {
			return null;
		} else {
			return !stackType.equals("B") && !stackType.equals("C") && !stackType.equals("S") && !stackType.equals("Z") ? stackType : "I";
		}
	}

	public static List<String> parseParameterDescriptors(String descriptor, boolean strict) {
		String parameterSection = MethodSignature.extractParameterDescriptor(descriptor);
		if (!strict || parameterSection.length() >= 2 && parameterSection.charAt(0) == '(' && parameterSection.charAt(parameterSection.length() - 1) == ')') {
			DebugUtils.assertTrue(parameterSection.length() >= 2, new String[]{"'" + parameterSection + "' : '" + descriptor + "'"});
			String parameterText = parameterSection.substring(1, parameterSection.indexOf(41));
			ArrayList<String> parameterDescriptors = new ArrayList<>();

			while (parameterText.length() != 0) {
				int elementTypeIndex = 0;

				while (parameterText.charAt(elementTypeIndex) == '[') {
					elementTypeIndex++;
				}

				int parameterEndIndex;
				if (parameterText.charAt(elementTypeIndex) == 'L') {
					parameterEndIndex = parameterText.indexOf(59);
					if (strict && (parameterEndIndex == -1 || parameterEndIndex == elementTypeIndex + 1)) {
						return null;
					}
				} else {
					parameterEndIndex = elementTypeIndex;
				}

				String parameterDescriptor = parameterText.substring(0, parameterEndIndex + 1);
				parameterDescriptors.add(parameterDescriptor);
				if (strict && parameterDescriptor.length() == 1 && descriptorToPrimitiveNameMap.get(parameterDescriptor) == null) {
					return null;
				}

				if (parameterEndIndex == parameterText.length() - 1) {
					parameterText = "";
				} else {
					parameterText = parameterText.substring(parameterEndIndex + 1);
				}
			}

			parameterDescriptors.trimToSize();
			return parameterDescriptors;
		} else {
			return null;
		}
	}

	public static String remapClassName(String className, Map<String, String> map, boolean isKnownClassName) {
		if (className != null && className.length() != 0) {
			boolean dottedForm = false;
			if (className.indexOf(".") > -1 && className.indexOf("/") == -1) {
				dottedForm = true;
			}

			String internalName = dottedForm ? className.replace(".", "/") : className;
			String mappedName;
			if (isKnownClassName) {
				mappedName = GlobalUtils.getMappedOrDefault(internalName, map);
			} else if ((className.indexOf(".") != -1 || className.indexOf("/") != -1)
				&& (className.indexOf(".") == -1 || className.indexOf("/") == -1)
				&& className.charAt(0) != '.'
				&& className.charAt(0) != '/'
				&& className.charAt(className.length() - 1) != '.'
				&& className.charAt(className.length() - 1) != '/') {
				StringTokenizer nameTokenizer = new StringTokenizer(className.replace(".", "/"), "/");
				boolean allTokensValid = true;

				while (nameTokenizer.hasMoreTokens()) {
					String nameToken = nameTokenizer.nextToken();
					if (!ConstantPoolManager.isJavaIdentifier(nameToken)) {
						allTokensValid = false;
						break;
					}

					if (nameToken.length() < 2) {
						allTokensValid = false;
						break;
					}
				}

				if (allTokensValid) {
					mappedName = GlobalUtils.getMappedOrDefault(internalName, map);
				} else {
					mappedName = className;
				}
			} else {
				mappedName = className;
			}

			return dottedForm ? mappedName.replace("/", ".") : mappedName;
		} else {
			return className;
		}
	}

	public static String sourceNameToDescriptor(String sourceName) {
		if (sourceName.length() != 1) {
			int arraySuffixIndex = sourceName.indexOf("[]");
			String descriptor;
			if (arraySuffixIndex > -1) {
				descriptor = sourceName.substring(0, arraySuffixIndex);
			} else {
				descriptor = sourceName;
			}

			int lastBracketIndex = descriptor.lastIndexOf(91);
			if (lastBracketIndex > -1) {
				descriptor = descriptor.substring(lastBracketIndex + 1);
			}

			if (descriptor.length() == 1) {
				if (!isPrimitiveDescriptor(descriptor)) {
					descriptor = "L" + descriptor + ";";
				}
			} else {
				descriptor = descriptor.replace('.', '/');
				if (!descriptor.startsWith("L") || !descriptor.endsWith(";")) {
					descriptor = "L" + descriptor + ";";
				}
			}

			int arrayDimensionCount = 0;
			if (arraySuffixIndex > -1) {
				arrayDimensionCount = StringUtils.countOccurrences(sourceName, "[]");
			} else if (lastBracketIndex > -1) {
				arrayDimensionCount = lastBracketIndex + 1;
			}

			for (int i = 0; i < arrayDimensionCount; i++) {
				descriptor = '[' + descriptor;
			}

			return descriptor;
		} else {
			return isPrimitiveDescriptor(sourceName) ? sourceName : "L" + sourceName + ";";
		}
	}

	public static List<String> getReadableParameterTypes(String descriptor) {
		List<String> parameterTypes = parseParameterDescriptors(descriptor);
		int parameterCount = parameterTypes.size();

		for (int i = 0; i < parameterCount; i++) {
			String parameterType = parameterTypes.get(i);
			parameterTypes.set(i, descriptorToHumanReadable(parameterType));
		}

		return parameterTypes;
	}

	public static String descriptorToInternalName(String descriptor) {
		String descriptorBody = descriptor.replace('.', '/');
		StringBuilder resultBuilder = new StringBuilder();
		int arrayDepth = 0;

		for (int i = 0; i < descriptor.length() && descriptor.charAt(i) == '['; i++) {
			arrayDepth++;
		}

		resultBuilder.append(descriptorBody.substring(0, arrayDepth));
		descriptorBody = descriptorBody.substring(arrayDepth);
		if (descriptorBody.charAt(0) == 'L') {
			if (descriptorBody.length() <= 2 || descriptorBody.charAt(descriptorBody.length() - 1) != ';') {
				return null;
			}

			if (arrayDepth != 0) {
				descriptorBody = descriptorBody.replace('/', '.');
			}

			resultBuilder.append(descriptorBody);
		} else {
			resultBuilder.append(descriptorBody.charAt(0));
		}

		return resultBuilder.toString();
	}

	public static TypeWriter readConstantPoolEntry(int index, ByteDataBuffer byteDataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		int tagID = byteDataBuffer.readUnsignedByte();
		switch (tagID) {
			case 1:
				return new UTFStringWriter(index, byteDataBuffer, constantPool);
			case 2:
			case 13:
			case 14:
			default:
				throw new ClassParseFailedException("Class file '" + constantPool.getOwnerRawSourcePath() + "' is corrupt. Tag '" + tagID + "' at " + index + " is invalid (2)");
			case 3:
				return new IntegerNode(index, byteDataBuffer, constantPool);
			case 4:
				return new FloatNode(index, byteDataBuffer, constantPool);
			case 5:
				return new LongNode(index, byteDataBuffer, constantPool);
			case 6:
				return new DoubleNode(index, byteDataBuffer, constantPool);
			case 7:
				return new RawClassInfoWriter(index, byteDataBuffer, constantPool);
			case 8:
				return new StringTypeWriter(index, byteDataBuffer, constantPool);
			case 9:
				return new FieldRefInfoWriter(index, byteDataBuffer, constantPool);
			case 10:
				return new MethodRefInfoWriter(index, byteDataBuffer, constantPool);
			case 11:
				return new InterfaceMethodRefInfoWriter(index, byteDataBuffer, constantPool);
			case 12:
				return new UnresolvedNameAndTypeEntry(index, byteDataBuffer, constantPool);
			case 15:
				return new MethodHandleInfoWriter(index, byteDataBuffer, constantPool);
			case 16:
				return new MethodTypeWriter(index, byteDataBuffer, constantPool);
			case 17:
				return new ConstantDynamicInfoWriter(index, byteDataBuffer, constantPool);
			case 18:
				return new InvokeDynamicInfoWriter(index, byteDataBuffer, constantPool);
			case 19:
				return new ModuleConstantWriter(index, byteDataBuffer, constantPool);
			case 20:
				return new PackageConstantWriter(index, byteDataBuffer, constantPool);
		}
	}

	public static final String formatMethodSignature(String name, String descriptor) {
		StringBuilder signatureBuilder = new StringBuilder();
		int closingParenIndex = descriptor.indexOf(41);
		if (closingParenIndex > -1) {
			signatureBuilder.append(descriptorToHumanReadable(descriptor.substring(closingParenIndex + 1)));
			signatureBuilder.append(' ');
			signatureBuilder.append(name);
			signatureBuilder.append('(');
			List<String> parameterTypes = getReadableParameterTypes(descriptor);
			int parameterCount = parameterTypes.size();

			for (int i = 0; i < parameterCount; i++) {
				signatureBuilder.append(parameterTypes.get(i));
				if (i < parameterCount - 1) {
					signatureBuilder.append(", ");
				}
			}

			signatureBuilder.append(')');
		} else {
			signatureBuilder.append(descriptorToHumanReadable(descriptor));
			signatureBuilder.append(' ');
			signatureBuilder.append(name);
		}

		return signatureBuilder.toString();
	}

	public static List<String> parseParameterDescriptors(String descriptor) {
		return parseParameterDescriptors(descriptor, false);
	}

	public static boolean isValidMethodDescriptor(String descriptor) {
		if (descriptor.length() < 2 || descriptor.charAt(0) != '(' || descriptor.indexOf(")") == -1) {
			return false;
		} else if (descriptor.indexOf(",") <= -1 && descriptor.indexOf("]") <= -1 && descriptor.indexOf(" ") <= -1) {
			String remainingDescriptors = descriptor.substring(1, descriptor.lastIndexOf(")"));
			if (descriptor.lastIndexOf(")") < descriptor.length() - 1) {
				remainingDescriptors = remainingDescriptors + descriptor.substring(descriptor.lastIndexOf(")") + 1);
			}

			while (remainingDescriptors.length() != 0) {
				int elementTypeIndex = 0;

				while (remainingDescriptors.charAt(elementTypeIndex) == '[') {
					elementTypeIndex++;
				}

				int parameterEndIndex;
				if (remainingDescriptors.charAt(elementTypeIndex) == 'L') {
					parameterEndIndex = remainingDescriptors.indexOf(59);
					if (parameterEndIndex == -1 || parameterEndIndex == elementTypeIndex + 1) {
						return false;
					}
				} else {
					parameterEndIndex = elementTypeIndex;
				}

				String parameterDescriptor = remainingDescriptors.substring(0, parameterEndIndex + 1);
				if (parameterDescriptor.length() == 1 && descriptorToPrimitiveNameMap.get(parameterDescriptor) == null) {
					return false;
				}

				if (parameterEndIndex == remainingDescriptors.length() - 1) {
					remainingDescriptors = "";
				} else {
					remainingDescriptors = remainingDescriptors.substring(parameterEndIndex + 1);
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public static String getPrimitiveName(String descriptor) {
		return descriptorToPrimitiveNameMap.get(descriptor);
	}

	public static boolean isDoubleSlotType(String descriptor) {
		return descriptor.equals("J") || descriptor.equals("D");
	}

	public static String descriptorToHumanReadable(String descriptor) {
		return descriptorToHumanReadable(descriptor, false, true);
	}

	public static String getWrapperName(String descriptor) {
		return descriptorToWrapperNameMap.get(descriptor);
	}

	public static String getDescriptorForWrapper(String wrapperName) {
		return wrapperNameToDescriptorMap.get(wrapperName);
	}

	public static AbstractClassNode getArrayElementClassNode(String descriptor) {
		String internalName = AbstractClassNode.getArrayElementInternalName(descriptor);
		if (internalName != null) {
			AbstractClassNode classNode = ClassHierarchyNode.getClassNodeByInternalName(internalName);
			if (classNode != null) {
				return classNode;
			}
		}

		return null;
	}

	public static String tryRemapDescriptor(String descriptor, Map<String, String> map, AtomicBool atomicBool) {
		atomicBool.set(false);
		String descriptorBody = descriptor;
		int descriptorLength = descriptor.length();
		StringBuilder prefixBuilder = null;
		String trailingSemicolon = null;
		if (descriptor.endsWith(";")) {
			prefixBuilder = new StringBuilder();
			if (descriptor.startsWith("[")) {
				prefixBuilder.append('[');

				for (int i = 1; i < descriptorLength; i++) {
					char currentChar = descriptorBody.charAt(i);
					if (currentChar != '[') {
						if (currentChar != 'L') {
							return descriptor;
						}

						prefixBuilder.append('L');
						break;
					}

					prefixBuilder.append('[');
				}
			} else {
				if (!descriptor.startsWith("L")) {
					return descriptor;
				}

				prefixBuilder.append('L');
			}

			trailingSemicolon = ";";
			descriptorBody = descriptorBody.substring(prefixBuilder.length(), descriptorLength - 1);
		}

		boolean dottedForm = false;
		String internalName;
		if (descriptorBody.indexOf(46) != -1 && descriptorBody.indexOf(47) == -1) {
			internalName = descriptorBody.replace('.', '/');
			dottedForm = true;
		} else {
			internalName = descriptorBody;
		}

		String mappedName = map.get(internalName);
		atomicBool.set(mappedName != null);
		int bodyLength = descriptorBody.length();
		int dotIndex = descriptorBody.indexOf(46);
		int lastDotIndex = descriptorBody.lastIndexOf(46);
		int slashIndex = descriptorBody.indexOf(47);
		int lastSlashIndex = descriptorBody.lastIndexOf(47);
		if (mappedName != null
			&& !mappedName.equals(internalName)
			&& internalName.length() >= Options.minDottedNameLength
			&& Character.isJavaIdentifierStart(internalName.charAt(0))
			&& (dotIndex > 0 && lastDotIndex < bodyLength - 1 && slashIndex == -1 || slashIndex > 0 && lastSlashIndex < bodyLength - 1 && dotIndex == -1)) {
			if (dottedForm) {
				mappedName = mappedName.replace('/', '.');
			}

			StringBuilder resultBuilder = new StringBuilder();
			if (prefixBuilder != null) {
				resultBuilder.append(prefixBuilder.toString());
			}

			resultBuilder.append(mappedName);
			if (trailingSemicolon != null) {
				resultBuilder.append(trailingSemicolon);
			}

			return resultBuilder.toString();
		} else {
			return descriptor;
		}
	}

	public static List<String> getStackParameterTypes(NameAndTypeWriter nameAndTypeWriter) {
		List<String> parameterTypes = parseParameterDescriptors(nameAndTypeWriter.getDescriptor());
		int parameterCount = parameterTypes.size();

		for (int i = 0; i < parameterCount; i++) {
			String parameterType = parameterTypes.get(i);
			if (parameterType.equals("B") || parameterType.equals("C") || parameterType.equals("S") || parameterType.equals("Z")) {
				parameterTypes.set(i, "I");
			}
		}

		return parameterTypes;
	}

	public static TypeWriter readConstantPoolManagerEntry(int index, ByteDataBuffer byteDataBuffer, ConstantPoolManager constantPoolManager) throws IOException {
		int tagID = byteDataBuffer.readUnsignedByte();
		switch (tagID) {
			case 1:
				return new UTFStringWriter(index, byteDataBuffer, constantPoolManager);
			case 2:
			case 13:
			case 14:
			default:
				throw new ClassParseFailedException("Class file '" + constantPoolManager.getOwnerRawSourcePath() + "' is corrupt. Tag '" + tagID + "' at " + index + " is invalid (1)");
			case 3:
				return new IntegerNode(index, byteDataBuffer, constantPoolManager);
			case 4:
				return new FloatNode(index, byteDataBuffer, constantPoolManager);
			case 5:
				return new LongNode(index, byteDataBuffer, constantPoolManager);
			case 6:
				return new DoubleNode(index, byteDataBuffer, constantPoolManager);
			case 7:
				return new RawClassInfoWriter(index, byteDataBuffer, constantPoolManager);
			case 8:
				return new StringTypeWriter(index, byteDataBuffer, constantPoolManager);
			case 9:
				return new FieldRefInfoWriter(index, byteDataBuffer, constantPoolManager);
			case 10:
				return new MethodRefInfoWriter(index, byteDataBuffer, constantPoolManager);
			case 11:
				return new InterfaceMethodRefInfoWriter(index, byteDataBuffer, constantPoolManager);
			case 12:
				return new UnresolvedNameAndTypeEntry(index, byteDataBuffer, constantPoolManager);
			case 15:
				return new MethodHandleInfoWriter(index, byteDataBuffer, constantPoolManager);
			case 16:
				return new MethodTypeWriter(index, byteDataBuffer, constantPoolManager);
			case 17:
				return new ConstantDynamicInfoWriter(index, byteDataBuffer, constantPoolManager);
			case 18:
				return new InvokeDynamicInfoWriter(index, byteDataBuffer, constantPoolManager);
			case 19:
				return new ModuleConstantWriter(index, byteDataBuffer, constantPoolManager);
			case 20:
				return new PackageConstantWriter(index, byteDataBuffer, constantPoolManager);
		}
	}

	public static String getClassInternalName(String descriptor) {
		int descriptorLength = descriptor.length();
		return descriptorLength > 2 && descriptor.charAt(0) == 'L' && descriptor.charAt(descriptorLength - 1) == ';' ? descriptor.substring(1, descriptorLength - 1) : descriptor;
	}

	public static ClassNode getArrayElementWorkspaceNode(String descriptor) {
		String internalName = AbstractClassNode.getArrayElementInternalName(descriptor);
		if (internalName != null) {
			ClassNode workspaceClassNode = ClassHierarchyNode.getWorkspaceClassNode(internalName);
			if (workspaceClassNode != null) {
				return workspaceClassNode;
			}
		}

		return null;
	}

	public static String descriptorToHumanReadable(String descriptor, boolean strict, boolean toBinaryName) {
		int closingParenIndex = descriptor.indexOf(")");
		String typeDescriptor;
		if (closingParenIndex > -1) {
			typeDescriptor = descriptor.substring(closingParenIndex + 1);
		} else {
			typeDescriptor = descriptor;
		}

		int arrayDepth = 0;

		for (int arrayPrefixIndex = 0; arrayPrefixIndex < typeDescriptor.length() && typeDescriptor.charAt(arrayPrefixIndex) == '['; arrayPrefixIndex++) {
			arrayDepth++;
		}

		String elementTypeDescriptor = typeDescriptor.substring(arrayDepth);
		String typeName;
		if (elementTypeDescriptor.charAt(0) == 'L') {
			if (strict && (elementTypeDescriptor.length() <= 2 || elementTypeDescriptor.charAt(elementTypeDescriptor.length() - 1) != ';')) {
				return null;
			}

			typeName = elementTypeDescriptor.substring(1, elementTypeDescriptor.length() - 1);
			if (toBinaryName) {
				typeName = typeName.replace('/', '.');
			}
		} else {
			String primitiveName = descriptorToPrimitiveNameMap.get(String.valueOf(elementTypeDescriptor.charAt(0)));
			if (strict && primitiveName == null) {
				return null;
			}

			typeName = primitiveName;
		}

		for (int i = 0; i < arrayDepth; i++) {
			typeName = typeName + "[]";
		}

		return typeName;
	}


	public static String getConstantPoolTagName(TypeTags typeTags) {
		switch (typeTags) {
			case UTF8:
				return "CONSTANT_Utf8";
			case INTEGER:
				return "CONSTANT_Integer";
			case FLOAT:
				return "CONSTANT_Float";
			case LONG:
				return "CONSTANT_Long";
			case DOUBLE:
				return "CONSTANT_Double";
			case CLASS:
				return "CONSTANT_Class";
			case STRING:
				return "CONSTANT_String";
			case FIELDREF:
				return "CONSTANT_Fieldref";
			case METHODREF:
				return "CONSTANT_Methodref";
			case INTERFACE_METHODREF:
				return "CONSTANT_InterfaceMethodref";
			case NAME_AND_TYPE:
				return "CONSTANT_NameAndType";
			case METHOD_HANDLE:
				return "CONSTANT_MethodHandle";
			case METHOD_TYPE:
				return "CONSTANT_MethodType";
			case CONSTANT_DYNAMIC:
				return "CONSTANT_Dynamic";
			case INVOKE_DYNAMIC:
				return "CONSTANT_InvokeDynamic";
			case MODULE:
				return "CONSTANT_Module";
			case PACKAGE:
				return "CONSTANT_Package";
			default:
				return "";
		}
	}

	public static List<String> getAllTypeDescriptors(String descriptor) {
		List<String> typeDescriptors = parseParameterDescriptors(descriptor);
		String returnDescriptor = getReturnDescriptor(descriptor);
		typeDescriptors.add(returnDescriptor);
		return typeDescriptors;
	}

	public static boolean isValidFieldDescriptor(String descriptor) {
		if (descriptor.indexOf(",") <= -1 && descriptor.indexOf(93) <= -1 && descriptor.indexOf(46) <= -1 && descriptor.indexOf(" ") <= -1) {
			int arrayDepth = 0;

			while (descriptor.charAt(arrayDepth) == '[') {
				arrayDepth++;
			}

			String elementTypeDescriptor = descriptor.substring(arrayDepth);
			return elementTypeDescriptor.startsWith("L")
				? elementTypeDescriptor.endsWith(";") || elementTypeDescriptor.length() > 2
				: elementTypeDescriptor.length() == 1 && descriptorToPrimitiveNameMap.containsKey(elementTypeDescriptor);
		} else {
			return false;
		}
	}

	public static boolean isMethodDescriptor(String descriptor) {
		return descriptor.charAt(0) == '(' && descriptor.indexOf(")") > 0;
	}

	public static String getStackWrapperName(String descriptor) {
		switch (descriptor.charAt(0)) {
			case 'B':
			case 'C':
			case 'S':
				descriptor = "I";
			default:
				return descriptorToWrapperNameMap.get(descriptor);
		}
	}

	public static int calculateSlotSize(List<String> descriptors) {
		int slotSize = 0;
		Iterator<String> descriptorIterator = descriptors.iterator();

		while (descriptorIterator.hasNext()) {
			if (isDoubleSlotType(descriptorIterator.next())) {
				slotSize += 2;
			} else {
				slotSize++;
			}
		}

		return slotSize;
	}

	public static boolean isPrimitiveDescriptor(String descriptor) {
		return descriptor.length() == 1 && descriptorToPrimitiveNameMap.containsKey(descriptor);
	}

	public static List<AbstractClassNode> getReferencedClasses(String descriptor) {
		ArrayList<AbstractClassNode> referencedClasses = new ArrayList<>();
		List<String> typeDescriptors = getAllTypeDescriptors(descriptor);

		for (int i = 0; i < typeDescriptors.size(); i++) {
			AbstractClassNode classNode = getArrayElementClassNode(typeDescriptors.get(i));
			if (classNode != null) {
				if (classNode.hasInnerClassInfo()) {
					referencedClasses.addAll(classNode.getSelfAndAllInnerClasses());
				} else {
					referencedClasses.add(classNode);
				}
			}
		}

		return referencedClasses;
	}

	public final AbstractClassNode resolveOwnerContext(AbstractClassNode classNode) {
		AbstractClassNode ownerClassNode = this.getOwnerClassNode();
		AbstractClassNode resolvedContext;
		if (ownerClassNode.isInnerClass() && classNode.hasInnerClasses()) {
			resolvedContext = classNode.findInnerClassByOrdinal(ownerClassNode.getInnerClassOrdinal());
		} else {
			resolvedContext = classNode;
		}

		return resolvedContext;
	}

	public String toDisplayString() {
		return this.getValueAsString();
	}

	public final AbstractClassNode getOwnerClassNode() {
		return this.constantPool.getOwnerClassNode();
	}

	public String getValueAsString() {
		return "";
	}

	public final void setTag(int tag) {
		this.tag = tag;
	}

	public void writeToStream(DataOutputStream dos, Map<TypeWriter, TypeWriter> twMap) throws IOException {
		this.writeToStream(dos);
	}

	public final String getOwnerClassName() {
		return this.constantPool.getOwnerClassNameBinary();
	}

	public final ClassNode resolveOwnerWorkspaceContext(ClassNode classNode) {
		return (ClassNode)this.resolveOwnerContext(classNode);
	}

	public boolean isResolutionValid() {
		return true;
	}

	public int compareToByIndex(TypeWriter other) {
		if (this.tag < other.tag) {
			return -1;
		} else {
			return this.tag == other.tag ? 0 : 1;
		}
	}

	public final String getOwnerSourcePath() {
		return this.constantPool.getOwnerSourcePath();
	}

	public int getSlotSize() {
		return 1;
	}

	@Override
	public int compareTo(TypeWriter other) {
		return this.compareToByIndex(other);
	}

	public final String getOwnerRawSourcePath() {
		return this.constantPool.getOwnerRawSourcePath();
	}

	@Override
	public final boolean equals(Object obj) {
		return super.equals(obj);
	}

	public String toLoggableString() {
		return this.getValueAsString();
	}

	@Override
	public final int hashCode() {
		return super.hashCode();
	}

	public boolean isDoubleSlotTag() {
		TypeTags typeTag = this.getTypeTag();
		return typeTag.equals(TypeTags.LONG) || typeTag.equals(TypeTags.DOUBLE);
	}

	public final int getTag() {
		return this.tag;
	}

	public final String getOwnerInternalName() {
		return this.constantPool.getOwnerInternalName();
	}

	public boolean isResolvable() {
		return false;
	}

	public abstract void writeToStream(DataOutputStream dos) throws IOException;

	public abstract TypeTags getTypeTag();

	static {
		descriptorToPrimitiveNameMap.put("B", "byte");
		descriptorToPrimitiveNameMap.put("C", "char");
		descriptorToPrimitiveNameMap.put("D", "double");
		descriptorToPrimitiveNameMap.put("F", "float");
		descriptorToPrimitiveNameMap.put("I", "int");
		descriptorToPrimitiveNameMap.put("J", "long");
		descriptorToPrimitiveNameMap.put("S", "short");
		descriptorToPrimitiveNameMap.put("Z", "boolean");
		descriptorToPrimitiveNameMap.put("V", "void");
		descriptorToWrapperNameMap.put("B", "java/lang/Byte");
		descriptorToWrapperNameMap.put("C", "java/lang/Character");
		descriptorToWrapperNameMap.put("D", "java/lang/Double");
		descriptorToWrapperNameMap.put("F", "java/lang/Float");
		descriptorToWrapperNameMap.put("I", "java/lang/Integer");
		descriptorToWrapperNameMap.put("J", "java/lang/Long");
		descriptorToWrapperNameMap.put("S", "java/lang/Short");
		descriptorToWrapperNameMap.put("Z", "java/lang/Boolean");
		descriptorToWrapperNameMap.put("V", "java/lang/Void");
		wrapperNameToDescriptorMap.put("java/lang/Byte", "B");
		wrapperNameToDescriptorMap.put("java/lang/Character", "C");
		wrapperNameToDescriptorMap.put("java/lang/Double", "D");
		wrapperNameToDescriptorMap.put("java/lang/Float", "F");
		wrapperNameToDescriptorMap.put("java/lang/Integer", "I");
		wrapperNameToDescriptorMap.put("java/lang/Long", "J");
		wrapperNameToDescriptorMap.put("java/lang/Short", "S");
		wrapperNameToDescriptorMap.put("java/lang/Boolean", "Z");
		wrapperNameToDescriptorMap.put("java/lang/Void", "V");
	}
}

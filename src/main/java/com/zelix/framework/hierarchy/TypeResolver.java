package com.zelix.framework.hierarchy;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.klassmaster.config.Options;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.OrderedSet;
import java.util.ArrayList;
import java.util.Map;

public class TypeResolver {
	private final DualKeyMap<String, String, String> supertypeCache = new DualKeyMap<>(Options.ZKM_USE_PARALLEL);
	private Map<Long, Integer> innerClassOrdinalByThread = GlobalUtils.newHashMap();
	private final ClassHierarchy classHierarchy;
	private final ClassProvider classProvider;

	public TypeResolver(ClassHierarchy classHierarchy, ClassProvider classProvider) {
		this.classHierarchy = classHierarchy;
		this.classProvider = classProvider;
	}

	public static String toObjectDescriptor(String internalName) {
		return internalName.charAt(internalName.length() - 1) != ';' ? 'L' + internalName + ';' : internalName;
	}

	public static String toInternalName(String descriptor) {
		int descriptorLength = descriptor.length();
		if (descriptor.charAt(0) == 'L' && descriptor.charAt(descriptorLength - 1) == ';') {
			descriptor = descriptor.substring(1, descriptorLength - 1);
		}

		return descriptor;
	}

	public void setInnerClassOrdinal(Integer ordinal) {
		this.innerClassOrdinalByThread.put(GlobalUtils.getThreadId(), ordinal);
	}

	public ClassHierarchy getClassHierarchy() {
		return this.classHierarchy;
	}

	public void clearCache() {
		this.supertypeCache.clearAll();
	}

	public String getCommonArraySupertype(String type1, String type2, String context) {
		int type1Dimension = type1.lastIndexOf("[") + 1;
		int type2Dimension = type2.lastIndexOf("[") + 1;
		String type1Component = type1.substring(type1Dimension);
		String type2Component = type2.substring(type2Dimension);
		if (type1Dimension != type2Dimension) {
			String shallowerComponent;
			if (type1Dimension < type2Dimension) {
				shallowerComponent = type1Component;
			} else {
				shallowerComponent = type2Component;
			}

			return shallowerComponent.startsWith("L") && shallowerComponent.endsWith(";")
				? StringUtils.justificateString("", 76, Math.min(type1Dimension, type2Dimension), 91) + "Ljava/lang/Object;"
				: StringUtils.justificateString("", 76, Math.min(type1Dimension, type2Dimension) - 1, 91) + "Ljava/lang/Object;";
		} else if (type1Component.length() == 1 && type2Component.length() == 1) {
			if (type1Component.equals(type2Component)) {
				return type1;
			} else {
				return type1Dimension == 1 ? "Ljava/lang/Object;" : StringUtils.justificateString("", 76, type1Dimension - 1, 91) + "Ljava/lang/Object;";
			}
		} else if (type1Component.length() != 1 && type2Component.length() != 1) {
			String commonComponentSupertype = this.getCommonSupertype(type1Component, type2Component, context);
			return StringUtils.justificateString(commonComponentSupertype, 82, commonComponentSupertype.length() + type1Dimension, 91);
		} else {
			return "Ljava/lang/Object;";
		}
	}

	private boolean isArrayAssignable(String fromType, String toType, String context) {
		int fromDimension = fromType.lastIndexOf("[") + 1;
		int toDimension = toType.lastIndexOf("[") + 1;
		String fromComponent = fromType.substring(fromDimension);
		String toComponent = toType.substring(toDimension);
		if (fromDimension < toDimension) {
			return false;
		} else if (fromDimension > toDimension) {
			return toComponent.equals("Ljava/lang/Object;");
		} else if (fromComponent.length() == 1 && toComponent.length() == 1) {
			return fromComponent.equals(toComponent);
		} else {
			return fromComponent.length() != 1 && toComponent.length() != 1 ? this.isReferenceAssignable(fromComponent, toComponent, context) : false;
		}
	}

	public boolean isInterface(StackFrameType type, String context) {
		String internalName = toInternalName(type.getDescriptor());
		return internalName.charAt(0) == '[' ? false : this.classProvider.getClassNode(internalName, this.getInnerClassOrdinal(), context).isInterface();
	}

	public boolean isAssignable(String fromType, String toType) {
		if (fromType.equals(toType)) {
			return true;
		} else if (fromType.length() > 1 && toType.length() > 1) {
			return this.isReferenceAssignable(fromType, toType, null);
		} else if (fromType.length() == 1 && toType.length() == 1) {
			char fromTypeChar = fromType.charAt(0);
			char toTypeChar = toType.charAt(0);
			switch (fromTypeChar) {
				case 'B':
					return toTypeChar == 'B' || toTypeChar == 'C' || toTypeChar == 'I' || toTypeChar == 'J';
				case 'C':
					return toTypeChar == 'C' || toTypeChar == 'I' || toTypeChar == 'J';
				case 'D':
					return toTypeChar == 'D';
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
				case 'W':
				case 'X':
				case 'Y':
				default:
					return false;
				case 'F':
					return toTypeChar == 'F' || toTypeChar == 'D';
				case 'I':
					return toTypeChar == 'I' || toTypeChar == 'J';
				case 'J':
					return toTypeChar == 'J';
				case 'S':
					return toTypeChar == 'S' || toTypeChar == 'I' || toTypeChar == 'J';
				case 'V':
					return toTypeChar == 'V';
				case 'Z':
					return toTypeChar == 'Z';
			}
		} else {
			return false;
		}
	}

	public String getCommonSupertype(String type1, String type2, String context) {
		Integer innerClassOrdinal = this.getInnerClassOrdinal();
		if (type1.equals(type2)) {
			return type1;
		} else {
			String commonSupertype = this.supertypeCache.get(type1, type2);
			if (commonSupertype != null) {
				return commonSupertype;
			} else {
				String internalName1 = toInternalName(type1);
				String internalName2 = toInternalName(type2);
				if (internalName1.charAt(0) == '[') {
					if (internalName2.charAt(0) == '[') {
						commonSupertype = this.getCommonArraySupertype(internalName1, internalName2, context);
					} else if (this.classProvider.getClassNode(internalName2, innerClassOrdinal, context).isInterface()) {
						if (internalName2.equals("java/lang/Cloneable")) {
							commonSupertype = "Ljava/lang/Cloneable;";
						} else {
							commonSupertype = "Ljava/lang/Object;";
						}
					} else {
						commonSupertype = "Ljava/lang/Object;";
					}
				} else if (internalName2.charAt(0) == '[') {
					if (this.classProvider.getClassNode(internalName1, innerClassOrdinal, context).isInterface()) {
						if (internalName1.equals("java/lang/Cloneable")) {
							commonSupertype = "Ljava/lang/Cloneable;";
						} else {
							commonSupertype = "Ljava/lang/Object;";
						}
					} else {
						commonSupertype = "Ljava/lang/Object;";
					}
				} else {
					AbstractClassNode classNode1 = this.classProvider.getClassNode(internalName1, innerClassOrdinal, context);
					AbstractClassNode classNode2 = this.classProvider.getClassNode(internalName2, innerClassOrdinal, context);
					if (!classNode1.isInterface() && !classNode2.isInterface()) {
						ArrayList<String> superclassHierarchy1 = classNode1.getSuperclassHierarchy(this.classProvider, innerClassOrdinal);
						ArrayList<String> superclassHierarchy2 = classNode2.getSuperclassHierarchy(this.classProvider, innerClassOrdinal);
						int superclassCount = superclassHierarchy1.size();
						if (superclassHierarchy2.contains(internalName1)) {
							commonSupertype = toObjectDescriptor(internalName1);
						} else if (superclassHierarchy1.contains(internalName2)) {
							commonSupertype = toObjectDescriptor(internalName2);
						} else {
							for (int i = 0; i < superclassCount; i++) {
								String superclassName = superclassHierarchy1.get(i);
								if (superclassHierarchy2.indexOf(superclassName) != -1) {
									commonSupertype = toObjectDescriptor(superclassName);
									if (commonSupertype.equals("Ljava/lang/Object;")) {
										commonSupertype = this.getCommonInterfaceSupertype(internalName1, classNode1, internalName2, classNode2);
									}
									break;
								}
							}

							if (commonSupertype == null) {
								commonSupertype = "Ljava/lang/Object;";
							}
						}
					} else {
						commonSupertype = this.getCommonInterfaceSupertype(internalName1, classNode1, internalName2, classNode2);
					}
				}

				this.supertypeCache.put(type1, type2, commonSupertype);
				this.supertypeCache.put(type2, type1, commonSupertype);
				return commonSupertype;
			}
		}
	}

	private String getCommonInterfaceSupertype(String internalName1, AbstractClassNode classNode1, String internalName2, AbstractClassNode classNode2) {
		Integer innerClassOrdinal = this.getInnerClassOrdinal();
		String commonSupertype = null;
		OrderedSet<String> interfaces1 = classNode1.getAllInterfaces(this.classProvider, innerClassOrdinal);
		OrderedSet<String> interfaces2 = classNode2.getAllInterfaces(this.classProvider, innerClassOrdinal);
		int interfaceCount = interfaces1.size();
		if (classNode1.isInterface() && interfaces2.contains(internalName1)) {
			commonSupertype = toObjectDescriptor(internalName1);
		}

		if (commonSupertype == null && classNode2.isInterface() && interfaces1.contains(internalName2)) {
			commonSupertype = toObjectDescriptor(internalName2);
		}

		if (commonSupertype == null) {
			for (int i = 0; i < interfaceCount; i++) {
				String interfaceName = interfaces1.get(i);
				if (interfaces2.indexOf(interfaceName) != -1) {
					commonSupertype = toObjectDescriptor(interfaceName);
					break;
				}
			}

			if (commonSupertype == null) {
				commonSupertype = "Ljava/lang/Object;";
			}
		}

		return commonSupertype;
	}

	public ClassProvider getClassProvider() {
		return this.classProvider;
	}

	public Integer removeInnerClassOrdinal() {
		return this.innerClassOrdinalByThread.remove(GlobalUtils.getThreadId());
	}

	public boolean isReferenceAssignable(String fromType, String toType, String context) {
		if (fromType.equals(toType)) {
			return true;
		} else {
			fromType = toInternalName(fromType);
			toType = toInternalName(toType);
			Integer innerClassOrdinal = this.getInnerClassOrdinal();
			if (fromType.charAt(0) == '[') {
				if (toType.charAt(0) == '[') {
					return this.isArrayAssignable(fromType, toType, context);
				} else {
					return this.classProvider.getClassNode(toType, innerClassOrdinal, context).isInterface() ? toType.equals("java/lang/Cloneable") : toType.equals("java/lang/Object");
				}
			} else if (toType.charAt(0) == '[') {
				return false;
			} else {
				AbstractClassNode fromClassNode = this.classProvider.getClassNode(fromType, innerClassOrdinal, context);
				AbstractClassNode toClassNode = this.classProvider.getClassNode(toType, innerClassOrdinal, context);
				if (fromClassNode.isInterface()) {
					return toClassNode.isInterface() ? this.isSubclassOf(fromType, toType) : toType.equals("java/lang/Object");
				} else if (toClassNode.isInterface()) {
					if (Options.objectAssignableToInterfaces) {
						return !this.isSubclassOf(fromType, toType) ? fromType.equals("java/lang/Object") : true;
					} else {
						return this.isSubclassOf(fromType, toType);
					}
				} else {
					return this.isClassInstanceOf(fromType, toType);
				}
			}
		}
	}

	public boolean isSubclassOf(String subclassName, String superclassName) {
		return subclassName.equals(superclassName) ? true : this.classHierarchy.isSubclassOf(subclassName, superclassName);
	}

	public boolean isReferenceAssignable(StackFrameType fromType, StackFrameType toType, String context) {
		return this.isReferenceAssignable(fromType.getDescriptor(), toType.getDescriptor(), context);
	}

	public Integer getInnerClassOrdinal() {
		return this.innerClassOrdinalByThread.get(GlobalUtils.getThreadId());
	}

	public String getCommonSupertype(StackFrameType type1, StackFrameType type2, String context) {
		return this.getCommonSupertype(type1.getDescriptor(), type2.getDescriptor(), context);
	}

	public boolean isClassInstanceOf(String instanceType, String targetType) {
		return instanceType.equals(targetType) ? true : this.classHierarchy.isClassInstanceOf(instanceType, targetType);
	}
}

package com.zelix.tools.flow;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.JumpInsnNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.variable.LocalVariablesManager;
import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;

import java.util.List;
import java.util.Random;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class Condition implements Comparable<Condition> {
	private final AbstractFieldNode fieldNode;
	private final String fieldName;
	private final String fieldDescriptor;
	private final String ownerInternalName;
	private int accessRank;
	private AbstractMethodNode setterMethod;
	private AbstractMethodNode getterMethod;
	private AbstractMethodNode booleanGetterMethod;
	private String setterMethodName;
	private String getterMethodName;
	private String booleanGetterMethodName;
	private final boolean usesMethodAccess;
	private final boolean conditionPolarity;

	public Condition(AbstractFieldNode fieldNode) {
		this.fieldNode = fieldNode;
		this.fieldName = fieldNode.getNameDisplay();
		this.fieldDescriptor = fieldNode.getDescriptor();
		this.ownerInternalName = fieldNode.getInternalName();
		this.accessRank = fieldNode.getAccessRank();
		this.usesMethodAccess = false;
		this.conditionPolarity = false;
	}

	public Condition(AbstractFieldNode fieldNode, AbstractMethodNode setterMethod, AbstractMethodNode getterMethod, AbstractMethodNode booleanGetterMethod, Boolean conditionPolarity) {
		this.fieldNode = fieldNode;
		this.fieldName = fieldNode.getNameDisplay();
		this.fieldDescriptor = fieldNode.getDescriptor();
		this.ownerInternalName = fieldNode.getInternalName();
		this.accessRank = fieldNode.getAccessRank();
		this.setterMethod = setterMethod;
		this.getterMethod = getterMethod;
		this.booleanGetterMethod = booleanGetterMethod;
		this.setterMethodName = this.setterMethod.getNameDisplay();
		this.getterMethodName = this.getterMethod.getNameDisplay();
		if (this.booleanGetterMethod != null) {
			this.booleanGetterMethodName = this.booleanGetterMethod.getNameDisplay();
		}

		this.usesMethodAccess = true;
		this.conditionPolarity = conditionPolarity;
	}

	public Condition(AbstractFieldNode fieldNode, AbstractMethodNode setterMethod, AbstractMethodNode getterMethod, AbstractMethodNode booleanGetterMethod, Random random) {
		this.fieldNode = fieldNode;
		this.fieldName = fieldNode.getNameDisplay();
		this.fieldDescriptor = fieldNode.getDescriptor();
		this.ownerInternalName = fieldNode.getInternalName();
		this.accessRank = fieldNode.getAccessRank();
		this.setterMethod = setterMethod;
		this.getterMethod = getterMethod;
		this.booleanGetterMethod = booleanGetterMethod;
		this.setterMethodName = this.setterMethod.getNameDisplay();
		this.getterMethodName = this.getterMethod.getNameDisplay();
		if (this.booleanGetterMethod != null) {
			this.booleanGetterMethodName = this.booleanGetterMethod.getNameDisplay();
		}

		this.usesMethodAccess = true;
		this.conditionPolarity = random.nextBoolean() || Options.fixConditionPolarity;
	}

	public static String buildGetterMethodDescriptor(String fieldDescriptor) {
		return "()" + fieldDescriptor;
	}

	public static String buildSetterMethodDescriptor(String fieldDescriptor) {
		return '(' + fieldDescriptor + ")V";
	}

	public WideConstantPoolInsnNode createBooleanGetterInsn(ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		MethodCallWriter booleanGetterCallWriter = constantPoolManager.getOrCreateClassMethodRef(this.booleanGetterMethod, typeWriters);
		return new WideConstantPoolInsnNode(INVOKESTATIC, booleanGetterCallWriter);
	}

	public boolean isReferenceType() {
		return this.fieldDescriptor.charAt(0) == '[' || this.fieldDescriptor.charAt(0) == 'L';
	}

	public String getFieldDescriptor() {
		return this.fieldDescriptor;
	}

	public AbstractMethodNode getSetterMethod() {
		return this.setterMethod;
	}

	public int compareCondition(Condition other) {
		int ownerComparison = this.ownerInternalName.compareTo(other.getOwnerInternalName());
		return ownerComparison == 0 ? this.fieldNode.getNameDisplay().compareTo(other.getFieldName()) : ownerComparison;
	}

	public String getGetterMethodName() {
		return this.getterMethodName;
	}

	public WideConstantPoolInsnNode createFieldWriteInsn(ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		if (this.usesMethodAccess) {
			MethodCallWriter setterCallWriter = constantPoolManager.getOrCreateClassMethodRef(this.setterMethod, typeWriters);
			return new WideConstantPoolInsnNode(INVOKESTATIC, setterCallWriter);
		} else {
			FieldTypeWriter fieldWriter = constantPoolManager.getOrCreateFieldRef(this.fieldNode.getInternalName(), this.fieldNode.getNameDisplay(), this.fieldNode.getDescriptor(), typeWriters, this.fieldNode);
			return new WideConstantPoolInsnNode(PUTSTATIC, fieldWriter);
		}
	}

	public boolean canCallBooleanGetter() {
		return this.booleanGetterMethod != null;
	}

	public boolean hasGetterMethod() {
		return this.getterMethod != null;
	}

	public String getSetterMethodOriginalDescriptor() {
		return this.setterMethod != null ? this.setterMethod.getOriginalDescriptor() : null;
	}

	public String getGetterMethodOriginalDescriptor() {
		return this.getterMethod != null ? this.getterMethod.getOriginalDescriptor() : null;
	}

	public String getBooleanGetterMethodOriginalDescriptor() {
		return this.booleanGetterMethod != null ? this.booleanGetterMethod.getOriginalDescriptor() : null;
	}

	public WideConstantPoolInsnNode createFieldReadInsn(ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		if (this.usesMethodAccess) {
			MethodCallWriter getterCallWriter = constantPoolManager.getOrCreateClassMethodRef(this.getterMethod, typeWriters);
			return new WideConstantPoolInsnNode(INVOKESTATIC, getterCallWriter);
		} else {
			FieldTypeWriter fieldWriter = constantPoolManager.getOrCreateFieldRef(this.fieldNode, typeWriters);
			return new WideConstantPoolInsnNode(GETSTATIC, fieldWriter);
		}
	}

	public boolean isPositiveCondition() {
		return this.conditionPolarity;
	}

	public String getFieldNodeDescriptor() {
		return this.fieldNode.getDescriptor();
	}

	public AbstractMethodNode getGetterMethod() {
		return this.getterMethod;
	}

	public String getOwnerInternalName() {
		return this.ownerInternalName;
	}

	public AbstractInsnNode createStoreInsn(int localVarIndex, LocalVariablesManager localVariablesManager) {
		return !this.fieldDescriptor.equals("I") && !this.fieldDescriptor.equals("Z") ? AbstractInsnNode.storeObject(localVarIndex, localVariablesManager, 2) : AbstractInsnNode.storeInt(localVarIndex, localVariablesManager, 2);
	}

	@Override
	public int compareTo(Condition other) {
		return this.compareCondition(other);
	}

	public JumpInsnNode createJumpIfTrue(LabelNode labelNode) {
		return !this.fieldDescriptor.equals("I") && !this.fieldDescriptor.equals("Z") ? new JumpInsnNode(this.conditionPolarity ? IFNONNULL : IFNULL, labelNode) : new JumpInsnNode(this.conditionPolarity ? IFNE : IFEQ, labelNode);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Condition)) {
			return false;
		} else {
			Condition other = (Condition)obj;
			return this.fieldName.equals(other.fieldName) && this.ownerInternalName.equals(other.ownerInternalName);
		}
	}

	public boolean isStringField() {
		return this.fieldDescriptor.equals("Ljava/lang/String;");
	}

	public AbstractMethodNode getBooleanGetterMethod() {
		return this.booleanGetterMethod;
	}

	@Override
	public int hashCode() {
		return this.ownerInternalName.hashCode() ^ this.fieldName.hashCode();
	}

	public String getFieldName() {
		return this.fieldName;
	}

	public boolean isObjectArray() {
		return this.fieldDescriptor.charAt(0) == '[' && this.fieldDescriptor.length() > 2;
	}

	public AbstractClassNode getOwnerClassNode() {
		return this.fieldNode.getClassNode();
	}

	public AbstractInsnNode createLoadInsn(int localVariableIndex, LocalVariablesManager localVariablesManager) {
		return !this.fieldDescriptor.equals("I") && !this.fieldDescriptor.equals("Z") ? AbstractInsnNode.loadObject(localVariableIndex, localVariablesManager, 2) : AbstractInsnNode.loadInt(localVariableIndex, localVariablesManager, 2);
	}

	public boolean isBooleanField() {
		return this.fieldDescriptor.equals("Z");
	}

	public JumpInsnNode createJumpIfFalse(LabelNode label) {
		return !this.fieldDescriptor.equals("I") && !this.fieldDescriptor.equals("Z") ? new JumpInsnNode(this.conditionPolarity ? IFNULL : IFNONNULL, label) : new JumpInsnNode(this.conditionPolarity ? IFEQ : IFNE, label);
	}

	public String getBooleanGetterMethodName() {
		return this.booleanGetterMethodName;
	}

	public boolean isPrimitiveArray() {
		return this.fieldDescriptor.charAt(0) == '[' && this.fieldDescriptor.length() == 2;
	}

	public boolean isObjectType() {
		return this.fieldDescriptor.charAt(0) == 'L';
	}

	public boolean hasSetterMethod() {
		return this.setterMethod != null;
	}

	public FieldSignature getFieldSignature() {
		return new FieldSignature(this.fieldName, this.fieldDescriptor);
	}

	public String getSetterMethodName() {
		return this.setterMethodName;
	}

	public boolean hasBooleanGetterMethod() {
		return this.booleanGetterMethod != null;
	}
}

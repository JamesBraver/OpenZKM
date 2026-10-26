package com.zelix.framework.constant;

import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.readOnly.ReadOnlySet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.LDC;
import static com.zelix.framework.instructions.OpcodeConstants.LDC_W;

public class ConstantPoolVisitor {
	private Set<UTFStringWriter> utfStrings;
	private Set<NameAndTypeWriter> nameTypes;
	private Set<MemberTypeWriter> memberReferences;
	private Set<ClassInfoWriter> classInfos;
	private Set<StringNode> stringLiterals;
	private Set<ConstantWriter> numericConstants;
	private Set<DynamicConstantWriter> dynamicConstants;
	private Set<InvokeDynamicCallWriter> dynamicCalls;
	private Set<MethodDescriptorWriter> methodTypes;
	private Set<ResolvedMethodHandleWriter> methodHandles;
	private Set<ModuleNameWriter> moduleNames;
	private Set<PackageNameWriter> packageNames;
	private boolean buildGraph;
	private ReferenceGraph<TypeWriter, InstructionListNode> ldcReferenceGraph;
	private ReferenceGraph<TypeWriter, InstructionListNode> ldcWReferenceGraph;
	private ReferenceGraph<InstructionListNode, TypeWriter> reverseLdcGraph;
	private ReferenceGraph<InstructionListNode, TypeWriter> reverseLdcWGraph;

	public ConstantPoolVisitor(int expectedConstantCount, boolean buildGraph) {
		int initialCapacity = GlobalUtils.getInitialCapacity(expectedConstantCount);
		this.utfStrings = GlobalUtils.newHashSet(initialCapacity);
		this.nameTypes = GlobalUtils.newHashSet(initialCapacity);
		this.memberReferences = GlobalUtils.newHashSet(initialCapacity);
		this.classInfos = GlobalUtils.newHashSet(initialCapacity);
		this.stringLiterals = GlobalUtils.newHashSet(initialCapacity);
		this.numericConstants = GlobalUtils.newHashSet(initialCapacity);
		this.dynamicConstants = GlobalUtils.newHashSet(initialCapacity);
		this.dynamicCalls = GlobalUtils.newHashSet(initialCapacity);
		this.methodTypes = GlobalUtils.newHashSet(initialCapacity);
		this.methodHandles = GlobalUtils.newHashSet(initialCapacity);
		this.moduleNames = GlobalUtils.newHashSet(initialCapacity);
		this.packageNames = GlobalUtils.newHashSet(initialCapacity);
		this.buildGraph = buildGraph;
		if (buildGraph) {
			this.ldcReferenceGraph = new ReferenceGraph<>(initialCapacity);
			this.ldcWReferenceGraph = new ReferenceGraph<>(initialCapacity);
			this.reverseLdcGraph = new ReferenceGraph<>();
			this.reverseLdcWGraph = new ReferenceGraph<>();
		}
	}

	public ConstantPoolVisitor(int expectedConstantCount) {
		this(expectedConstantCount, false);
	}

	public boolean containsDynamicConstant(DynamicConstantWriter dynamicConstant) {
		return this.dynamicConstants.contains(dynamicConstant);
	}

	public List<TypeWriter> getLdcReferencedConstants() {
		return this.buildGraph ? new ArrayList<>(this.ldcReferenceGraph.getMembers()) : null;
	}

	public boolean containsMethodHandle(ResolvedMethodHandleWriter methodHandle) {
		return this.methodHandles.contains(methodHandle);
	}

	private boolean visitMethodHandle(Object owner, Object context, ResolvedMethodHandleWriter methodHandle) {
		boolean added = this.methodHandles.add(methodHandle);
		if (added) {
			this.visitMemberReference(owner, context, methodHandle.getTargetMemberWriter());
		}

		return added;
	}

	private boolean visitDynamicCall(Object owner, Object context, InvokeDynamicCallWriter dynamicCall) {
		boolean added = this.dynamicCalls.add(dynamicCall);
		if (added) {
			this.visitNameType(owner, context, dynamicCall.getTarget());
		}

		return added;
	}

	public Set<InstructionListNode> getReferencingInstructions(TypeWriter constant) {
		HashSet<InstructionListNode> referencingInstructions = GlobalUtils.newHashSet();
		Set<InstructionListNode> ldcReferences = this.ldcReferenceGraph.getReferences(constant);
		if (ldcReferences != null) {
			referencingInstructions.addAll(ldcReferences);
		}

		Set<InstructionListNode> ldcWReferences = this.ldcWReferenceGraph.getReferences(constant);
		if (ldcWReferences != null) {
			referencingInstructions.addAll(ldcWReferences);
		}

		return referencingInstructions;
	}

	public boolean containsDouble(DoubleNode doubleConstant) {
		return this.numericConstants.contains(doubleConstant);
	}

	private boolean visitNameType(Object owner, Object context, NameAndTypeWriter nameAndType) {
		boolean added = this.nameTypes.add(nameAndType);
		if (added) {
			this.visitUtfString(owner, context, nameAndType.getNameEntry());
			this.visitUtfString(owner, context, nameAndType.getDescriptorEntry());
		}

		return added;
	}

	public boolean containsNameAndType(NameAndTypeWriter nameAndType) {
		return this.nameTypes.contains(nameAndType);
	}

	public boolean containsInteger(IntegerNode integerConstant) {
		return this.numericConstants.contains(integerConstant);
	}

	public Set<ConstantWriter> getNumericConstants() {
		return new ReadOnlySet<>(this.numericConstants);
	}

	private boolean visitDouble(DoubleNode doubleConstant) {
		return this.numericConstants.add(doubleConstant);
	}

	public boolean containsFloat(FloatNode floatConstant) {
		return this.numericConstants.contains(floatConstant);
	}

	public List<TypeWriter> getLdcWReferencedConstants() {
		return this.buildGraph ? new ArrayList<>(this.ldcWReferenceGraph.getMembers()) : null;
	}

	private boolean visitStringLiteral(Object owner, Object context, StringNode stringLiteral) {
		this.stringLiterals.add(stringLiteral);
		boolean added = this.numericConstants.add(stringLiteral);
		if (added) {
			this.visitUtfString(owner, context, stringLiteral.getUtf8StringWriter());
		}

		return added;
	}

	private boolean visitClassInfo(Object owner, Object context, ClassInfoWriter classInfo) {
		boolean added = this.classInfos.add(classInfo);
		if (added) {
			this.visitUtfString(owner, context, classInfo.getNameWriter());
		}

		return added;
	}

	public Set<DynamicConstantWriter> getDynamicConstants() {
		return new ReadOnlySet<>(this.dynamicConstants);
	}

	private boolean visitMethodType(Object owner, Object context, MethodDescriptorWriter methodType) {
		boolean added = this.methodTypes.add(methodType);
		if (added) {
			this.visitUtfString(owner, context, methodType.getDescriptorWriter());
		}

		return added;
	}

	public boolean containsMemberReference(MemberTypeWriter memberReference) {
		return this.memberReferences.contains(memberReference);
	}

	public Set<UTFStringWriter> getUtfStrings() {
		return new ReadOnlySet<>(this.utfStrings);
	}

	public boolean containsDynamicCall(InvokeDynamicCallWriter dynamicCall) {
		return this.dynamicCalls.contains(dynamicCall);
	}

	private boolean visitUtfString(Object owner, Object context, UTFStringWriter utfString) {
		return this.utfStrings.add(utfString);
	}

	public boolean containsUtfString(UTFStringWriter utfString) {
		return this.utfStrings.contains(utfString);
	}

	public Set<MethodDescriptorWriter> getMethodTypes() {
		return new ReadOnlySet<>(this.methodTypes);
	}

	private boolean visitFloat(FloatNode floatConstant) {
		return this.numericConstants.add(floatConstant);
	}

	public Set<MemberTypeWriter> getMemberReferences() {
		return new ReadOnlySet<>(this.memberReferences);
	}

	public boolean containsStringLiteral(StringNode stringLiteral) {
		return this.stringLiterals.contains(stringLiteral);
	}

	public boolean containsLong(LongNode longConstant) {
		return this.numericConstants.contains(longConstant);
	}

	private boolean visitInteger(IntegerNode integerConstant) {
		return this.numericConstants.add(integerConstant);
	}

	public boolean contains(TypeWriter constant) {
		switch (constant.getTypeTag()) {
			case UTF8:
				return this.containsUtfString((UTFStringWriter)constant);
			case INTEGER:
				return this.containsInteger((IntegerNode)constant);
			case FLOAT:
				return this.containsFloat((FloatNode)constant);
			case LONG:
				return this.containsLong((LongNode)constant);
			case DOUBLE:
				return this.containsDouble((DoubleNode)constant);
			case CLASS:
				return this.containsClassInfo((ClassInfoWriter)constant);
			case STRING:
				return this.containsStringLiteral((StringNode)constant);
			case FIELDREF:
				return this.containsMemberReference((MemberTypeWriter)constant);
			case METHODREF:
				return this.containsMemberReference((MemberTypeWriter)constant);
			case INTERFACE_METHODREF:
				return this.containsMemberReference((MemberTypeWriter)constant);
			case NAME_AND_TYPE:
				return this.containsNameAndType((NameAndTypeWriter)constant);
			case METHOD_HANDLE:
				return this.containsMethodHandle((ResolvedMethodHandleWriter)constant);
			case METHOD_TYPE:
				return this.containsMethodType((MethodDescriptorWriter)constant);
			case CONSTANT_DYNAMIC:
				return this.containsDynamicConstant((DynamicConstantWriter)constant);
			case INVOKE_DYNAMIC:
				return this.containsDynamicCall((InvokeDynamicCallWriter)constant);
			case MODULE:
				return this.containsModuleName((ModuleNameWriter)constant);
			case PACKAGE:
				return this.containsPackageName((PackageNameWriter)constant);
			default:
				return false;
		}
	}

	public Set<InvokeDynamicCallWriter> getDynamicCalls() {
		return new ReadOnlySet<>(this.dynamicCalls);
	}

	private boolean visitPackageName(Object owner, Object context, PackageNameWriter packageName) {
		boolean added = this.packageNames.add(packageName);
		if (added) {
			this.visitUtfString(owner, context, packageName.getNameWriter());
		}

		return added;
	}

	public Set<NameAndTypeWriter> getNameAndTypes() {
		return new ReadOnlySet<>(this.nameTypes);
	}

	public boolean containsModuleName(ModuleNameWriter moduleName) {
		return this.moduleNames.contains(moduleName);
	}

	public boolean containsClassInfo(ClassInfoWriter classInfo) {
		return this.classInfos.contains(classInfo);
	}

	public Set<ResolvedMethodHandleWriter> getMethodHandles() {
		return new ReadOnlySet<>(this.methodHandles);
	}

	public boolean visit(TypeWriter node, Object owner, Object context) {
		if (this.buildGraph && owner instanceof AbstractInsnNode && context instanceof InstructionListNode) {
			AbstractInsnNode ownerInsn = (AbstractInsnNode)owner;
			InstructionListNode instructionNode = (InstructionListNode)context;
			if (ownerInsn.getOpcode() == LDC) {
				this.ldcReferenceGraph.addReference(node, instructionNode);
				this.reverseLdcGraph.addReference(instructionNode, node);
			} else if (ownerInsn.getOpcode() == LDC_W) {
				this.ldcWReferenceGraph.addReference(node, instructionNode);
				this.reverseLdcWGraph.addReference(instructionNode, node);
			}
		}

		switch (node.getTypeTag()) {
			case UTF8:
				return this.visitUtfString(owner, context, (UTFStringWriter)node);
			case INTEGER:
				return this.visitInteger((IntegerNode)node);
			case FLOAT:
				return this.visitFloat((FloatNode)node);
			case LONG:
				return this.visitLong((LongNode)node);
			case DOUBLE:
				return this.visitDouble((DoubleNode)node);
			case CLASS:
				return this.visitClassInfo(owner, context, (ClassInfoWriter)node);
			case STRING:
				return this.visitStringLiteral(owner, context, (StringNode)node);
			case FIELDREF:
				return this.visitMemberReference(owner, context, (MemberTypeWriter)node);
			case METHODREF:
				return this.visitMemberReference(owner, context, (MemberTypeWriter)node);
			case INTERFACE_METHODREF:
				return this.visitMemberReference(owner, context, (MemberTypeWriter)node);
			case NAME_AND_TYPE:
				return this.visitNameType(owner, context, (NameAndTypeWriter)node);
			case METHOD_HANDLE:
				return this.visitMethodHandle(owner, context, (ResolvedMethodHandleWriter)node);
			case METHOD_TYPE:
				return this.visitMethodType(owner, context, (MethodDescriptorWriter)node);
			case CONSTANT_DYNAMIC:
				return this.visitDynamicConstant(owner, context, (DynamicConstantWriter)node);
			case INVOKE_DYNAMIC:
				return this.visitDynamicCall(owner, context, (InvokeDynamicCallWriter)node);
			case MODULE:
				return this.visitModuleName(owner, context, (ModuleNameWriter)node);
			case PACKAGE:
				return this.visitPackageName(owner, context, (PackageNameWriter)node);
			default:
				return false;
		}
	}

	public Set<ClassInfoWriter> getClassInfos() {
		return new ReadOnlySet<>(this.classInfos);
	}

	public boolean containsMethodType(MethodDescriptorWriter methodType) {
		return this.methodTypes.contains(methodType);
	}

	public boolean containsPackageName(PackageNameWriter packageName) {
		return this.packageNames.contains(packageName);
	}

	private boolean visitMemberReference(Object owner, Object context, MemberTypeWriter memberReference) {
		boolean added = this.memberReferences.add(memberReference);
		if (added) {
			this.visitClassInfo(owner, context, memberReference.getOwner());
			this.visitNameType(owner, context, memberReference.getTarget());
		}

		return added;
	}

	private boolean visitDynamicConstant(Object owner, Object context, DynamicConstantWriter dynamicConstant) {
		boolean added = this.dynamicConstants.add(dynamicConstant);
		if (added) {
			this.visitNameType(owner, context, dynamicConstant.getTarget());
		}

		return added;
	}

	private boolean visitLong(LongNode longConstant) {
		return this.numericConstants.add(longConstant);
	}

	private boolean visitModuleName(Object owner, Object context, ModuleNameWriter moduleName) {
		boolean added = this.moduleNames.add(moduleName);
		if (added) {
			this.visitUtfString(owner, context, moduleName.getUtf8StringWriter());
		}

		return added;
	}
}

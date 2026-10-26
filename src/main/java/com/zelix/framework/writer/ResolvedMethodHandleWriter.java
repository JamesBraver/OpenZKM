package com.zelix.framework.writer;

import com.zelix.exceptions.LookupException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.TypeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.BaseMethodHandleWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public class ResolvedMethodHandleWriter extends BaseMethodHandleWriter implements IVisitable, TypeNode {
	public final MethodHandleKind kind;
	public MemberTypeWriter targetMemberWriter;

	public ResolvedMethodHandleWriter(
		int tag, AbstractConstantPoolWriter constantPool, MethodHandleKind kind, MemberTypeWriter targetMemberWriter, MultiMap<MemberTypeWriter, TypeNode> memberReferences
	) {
		super(tag, constantPool);
		this.kind = kind;
		this.targetMemberWriter = targetMemberWriter;
		memberReferences.put(targetMemberWriter, this);
	}

	public ResolvedMethodHandleWriter(int tag, AbstractConstantPoolWriter constantPool, MethodHandleKind kind, MemberTypeWriter targetMemberWriter) {
		super(tag, constantPool);
		this.kind = kind;
		this.targetMemberWriter = targetMemberWriter;
	}

	@Override
	public String toDisplayString() {
		return this.targetMemberWriter.getOwnerName() + '.' + this.targetMemberWriter.getName();
	}

	private boolean isTargetStatic() {
		switch (this.kind) {
			case REF_GET_FIELD:
			case REF_PUT_FIELD:
			case REF_INVOKE_VIRTUAL:
			case REF_INVOKE_SPECIAL:
			case REF_NEW_INVOKE_SPECIAL:
			case REF_INVOKE_INTERFACE:
				return false;
			case REF_GET_STATIC:
			case REF_PUT_STATIC:
			case REF_INVOKE_STATIC:
				return true;
			default:
				throw new LookupException("Unhandled reference kind : " + this.kind + " : " + this.getOwnerSourcePath());
		}
	}

	@Override
	public String getValueAsString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.kind);
		builder.append(" : ");
		builder.append(this.targetMemberWriter.getValueAsString());
		return builder.toString();
	}

	public String getTargetName() {
		return this.targetMemberWriter.getName();
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public void collectDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		String ownerName = this.targetMemberWriter.getOwnerName();
		AbstractClassNode ownerClassNode = null;
		Set<AbstractClassNode> classDependencySet;
		if (this.isTargetStatic()) {
			classDependencySet = signatureClasses;
		} else {
			classDependencySet = referencedClasses;
		}

		if (!ownerName.startsWith("[")) {
			ownerClassNode = ClassHierarchyNode.getClassNodeByInternalName(ownerName);
			if (ownerClassNode != null) {
				if (ownerClassNode.hasInnerClassInfo()) {
					classDependencySet.addAll(ownerClassNode.getSelfAndAllInnerClasses());
				} else {
					classDependencySet.add(ownerClassNode);
				}
			}
		}

		switch (this.kind) {
			case REF_GET_FIELD:
			case REF_PUT_FIELD:
			case REF_GET_STATIC:
			case REF_PUT_STATIC:
				AbstractFieldNode targetField = (AbstractFieldNode)this.targetMemberWriter.getTargetMember();
				if (targetField != null) {
					referencedFields.add(targetField);
					AbstractClassNode fieldOwnerClassNode = targetField.getClassNode();
					if (ownerClassNode == null || fieldOwnerClassNode != ownerClassNode) {
						if (fieldOwnerClassNode.hasInnerClassInfo()) {
							classDependencySet.addAll(fieldOwnerClassNode.getSelfAndAllInnerClasses());
						} else {
							classDependencySet.add(fieldOwnerClassNode);
						}
					}
				}
				break;
			case REF_INVOKE_VIRTUAL:
			case REF_INVOKE_SPECIAL:
			case REF_NEW_INVOKE_SPECIAL:
			case REF_INVOKE_INTERFACE:
			case REF_INVOKE_STATIC:
				AbstractMethodNode targetMethod = (AbstractMethodNode)this.targetMemberWriter.getTargetMember();
				if (targetMethod != null) {
					referencedMethods.add(targetMethod);
					AbstractClassNode methodOwnerClassNode = targetMethod.getClassNode();
					if ((ownerClassNode == null || methodOwnerClassNode != ownerClassNode) && !ClassHierarchyNode.isMissingClass(methodOwnerClassNode.getInternalName())) {
						if (methodOwnerClassNode.hasInnerClassInfo()) {
							classDependencySet.addAll(methodOwnerClassNode.getSelfAndAllInnerClasses());
						} else {
							classDependencySet.add(methodOwnerClassNode);
						}
					}
				}
		}
	}

	public MemberNode getTargetMember() {
		return this.targetMemberWriter.getTargetMember();
	}

	public boolean isInvokeStatic() {
		return this.kind == MethodHandleKind.REF_INVOKE_STATIC;
	}

	public boolean isMethodHandle() {
		boolean matchedInvokeKind = false;
		switch (this.kind) {
			case REF_GET_FIELD:
			case REF_PUT_FIELD:
			case REF_GET_STATIC:
			case REF_PUT_STATIC:
			default:
				break;
			case REF_INVOKE_VIRTUAL:
			case REF_INVOKE_SPECIAL:
			case REF_NEW_INVOKE_SPECIAL:
			case REF_INVOKE_STATIC:
				matchedInvokeKind = true;
				break;
			case REF_INVOKE_INTERFACE:
				matchedInvokeKind = true;
		}

		return matchedInvokeKind;
	}

	public MemberTypeWriter getTargetMemberWriter() {
		return this.targetMemberWriter;
	}

	public void collectWorkspaceTargets(Set<MethodNode> bootstrapTargets) {
		switch (this.kind) {
			case REF_INVOKE_VIRTUAL:
			case REF_INVOKE_SPECIAL:
			case REF_NEW_INVOKE_SPECIAL:
			case REF_INVOKE_INTERFACE:
			case REF_INVOKE_STATIC:
				AbstractMethodNode targetMethod = (AbstractMethodNode)this.targetMemberWriter.getTargetMember();
				if (targetMethod != null && targetMethod.isWorkspaceNode()) {
					bootstrapTargets.add((MethodNode)targetMethod);
				}
			case REF_GET_FIELD:
			case REF_PUT_FIELD:
			case REF_GET_STATIC:
			case REF_PUT_STATIC:
		}
	}

	public MethodHandleKind getKind() {
		return this.kind;
	}

	@Override
	public void updateMemberReference(MemberTypeWriter oldReference, MemberTypeWriter newReference) {
		if (this.targetMemberWriter == oldReference) {
			this.targetMemberWriter = newReference;
		}
	}

	public String getTargetDescriptor() {
		return this.targetMemberWriter.getDescriptor();
	}

	public void collectWorkspaceDependencies(Set<ClassNode> renamedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> renamedFields, Set<MethodNode> renamedMethods) {
		String ownerName = this.targetMemberWriter.getOwnerName();
		ClassNode ownerClassNode = null;
		if (!ownerName.startsWith("[")) {
			ownerClassNode = ClassHierarchyNode.getWorkspaceClassNode(ownerName);
			if (ownerClassNode != null) {
				ownerClassNode = this.resolveOwnerWorkspaceContext(ownerClassNode);
				if (this.isTargetStatic()) {
					signatureClasses.add(ownerClassNode);
				} else {
					renamedClasses.add(ownerClassNode);
				}
			}
		}

		switch (this.kind) {
			case REF_GET_FIELD:
			case REF_PUT_FIELD:
			case REF_GET_STATIC:
			case REF_PUT_STATIC:
				AbstractFieldNode targetField = (AbstractFieldNode)this.targetMemberWriter.getTargetMember();
				if (targetField != null && targetField.isWorkspaceNode()) {
					renamedFields.add((FieldNode)targetField);
					ClassNode fieldOwnerClassNode = (ClassNode)targetField.getClassNode();
					if (ownerClassNode == null || fieldOwnerClassNode != ownerClassNode) {
						fieldOwnerClassNode = this.resolveOwnerWorkspaceContext(fieldOwnerClassNode);
						if (this.isTargetStatic()) {
							signatureClasses.add(fieldOwnerClassNode);
						} else {
							renamedClasses.add(fieldOwnerClassNode);
						}
					}
				}
				break;
			case REF_INVOKE_VIRTUAL:
			case REF_INVOKE_SPECIAL:
			case REF_NEW_INVOKE_SPECIAL:
			case REF_INVOKE_INTERFACE:
			case REF_INVOKE_STATIC:
				AbstractMethodNode targetMethod = (AbstractMethodNode)this.targetMemberWriter.getTargetMember();
				if (targetMethod != null && targetMethod.isWorkspaceNode()) {
					renamedMethods.add((MethodNode)targetMethod);
					ClassNode methodOwnerClassNode = (ClassNode)targetMethod.getClassNode();
					if (ownerClassNode == null || methodOwnerClassNode != ownerClassNode) {
						this.resolveOwnerWorkspaceContext(ownerClassNode);
						if (this.isTargetStatic()) {
							signatureClasses.add(methodOwnerClassNode);
						} else {
							renamedClasses.add(methodOwnerClassNode);
						}
					}
				}
		}
	}

	public boolean matches(MethodHandleKind kind, MemberTypeWriter memberReference) {
		return this.kind == kind
			&& this.targetMemberWriter.getTypeTag().equals(memberReference.getTypeTag())
			&& this.targetMemberWriter.isTarget(memberReference.getOwnerName(), memberReference.getName(), memberReference.getDescriptor());
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(BaseMethodHandleWriter.TYPE_TAG.getTagID());
		dos.writeByte(this.kind.getReferenceKind());
		switch (this.kind) {
			case REF_GET_FIELD:
			case REF_PUT_FIELD:
			case REF_GET_STATIC:
			case REF_PUT_STATIC:
				dos.writeShort(this.targetMemberWriter.getTag());
				break;
			case REF_INVOKE_VIRTUAL:
			case REF_INVOKE_SPECIAL:
			case REF_NEW_INVOKE_SPECIAL:
			case REF_INVOKE_STATIC:
				dos.writeShort(this.targetMemberWriter.getTag());
				break;
			case REF_INVOKE_INTERFACE:
				dos.writeShort(this.targetMemberWriter.getTag());
		}
	}

	public String getTargetClassName() {
		return this.targetMemberWriter.getOwnerName();
	}
}

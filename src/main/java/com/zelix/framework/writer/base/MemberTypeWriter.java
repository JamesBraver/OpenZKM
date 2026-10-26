package com.zelix.framework.writer.base;

import com.zelix.exceptions.FileException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.IVisitable;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public abstract class MemberTypeWriter extends TypeWriter implements IVisitable, ReferenceUpdateListener, AbstractNamedWriter {
	public ClassInfoWriter owner;
	public NameAndTypeWriter target;
	public MemberNode targetMember;

	public MemberTypeWriter(
		int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractWorkspace workspace, ClassProvider classProvider, boolean resolveTarget
	) {
		super(index, constantPool);
		this.owner = owner;
		this.target = target;
		if (resolveTarget) {
			this.resolveTargetMember(workspace, classProvider, null);
		}

		//              + ", constantPool=" + constantPool + ", owner=" + owner.getInternalName() + ", target=" + target.nameUtf8Entry.getValue() + "|" + target.descriptorUtf8Entry.getValue()
		//              + ", workspace=" + workspace + ", classProvider=" + classProvider + ", resolveTarget=" + resolveTarget);
	}

	public MemberTypeWriter(int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, MemberNode targetMember) {
		super(index, constantPool);
		this.owner = owner;
		this.target = target;
		this.targetMember = targetMember;

		//              + ", constantPool=" + constantPool + ", owner=" + owner.getInternalName() + ", target=" + target.nameUtf8Entry.getValue() + "|" + target.descriptorUtf8Entry.getValue()
		//              + ", targetMember=" + targetMember);
	}

	public MemberTypeWriter(
		AbstractMemberRefInfoWriter memberRefInfoWriter, ClassInfoWriter owner, NameAndTypeWriter target, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) {
		super(memberRefInfoWriter.tag, memberRefInfoWriter.constantPool);
		this.owner = owner;
		this.target = target;
		classReferences.put(owner, this);

		//              + ", owner=" + owner.getInternalName() + ", target=" + target.nameUtf8Entry.getValue() + "|" + target.descriptorUtf8Entry.getValue()
		//              + ", classReferences=" + classReferences);
	}

	@Override
	public NameAndTypeWriter setTarget(NameAndTypeWriter newTarget) {
		NameAndTypeWriter previousTarget = this.target;
		this.target = newTarget;
		return previousTarget;
	}

	@Override
	public final String toDisplayString() {
		return this.owner.getValueAsString() + "." + this.target.toDisplayString();
	}

	public final String getDescriptor() {
		return this.target.getDescriptor();
	}

	public final boolean isTarget(String owner, String name, String desc) {
		return this.owner.getInternalName().equals(owner) && this.getName().equals(name) && this.getDescriptor().equals(desc);
	}

	public String getAsString() {
		return this.owner.getInternalName() + '~' + this.target.getAsString();
	}

	public final String getOwnerName() {
		return this.owner.getInternalName();
	}

	@Override
	public final String getValueAsString() {
		return this.owner.getValueAsString() + " " + this.target.getValueAsString();
	}

	@Override
	public final void writeToStream(DataOutputStream dos, Map<TypeWriter, TypeWriter> twMap) throws IOException {
		dos.writeByte(this.getTypeTag().getTagID());
		TypeWriter mappedOwner = twMap.get(this.owner);
		if (mappedOwner != null) {
			dos.writeShort(mappedOwner.getTag());
		} else {
			dos.writeShort(this.owner.getTag());
		}

		NameAndTypeWriter mappedTarget = (NameAndTypeWriter)twMap.get(this.target);
		if (mappedTarget != null) {
			dos.writeShort(mappedTarget.getTag());
		} else {
			dos.writeShort(this.target.getTag());
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.owner == oldReference) {
			this.owner = newReference;
		}
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public final NameAndTypeWriter getTarget() {
		return this.target;
	}

	public final String getName() {
		return this.target.getName();
	}

	public ClassInfoWriter getOwner() {
		return this.owner;
	}

	public MemberNode getTargetMember() {
		return this.targetMember;
	}

	public abstract void resolveTargetMember(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager);

	@Override
	public final void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(this.getTypeTag().getTagID());
		dos.writeShort(this.owner.getTag());
		dos.writeShort(this.target.getTag());
	}

	public final void updateTargetFromMember(ScriptContext scriptContext, AbstractWorkspace workspace) {
		if (this.targetMember != null) {
			String memberInternalName = this.targetMember.getInternalName();
			String tDesc = this.target.getDescriptor();
			String tmDesc = this.targetMember.getDescriptor();
			if (!ClassHierarchyNode.isMissingClass(memberInternalName)) {
				String targetName = this.target.getName();
				String memberName = this.targetMember.getName();
				if (!targetName.equals(memberName)) {
					this.target.setName(memberName);
				}

				if (!tDesc.equals(tmDesc)) {
					this.target.setDescriptor(tmDesc);
				}
			} else if (scriptContext != null && !tDesc.equals(tmDesc)) {
				boolean handleMethodDetected = false;

				try {
					handleMethodDetected = MethodSignature.isHandleMethod(this.targetMember.getClassName(), this.targetMember.getNameDisplay(), workspace);
				} catch (FileException ignored) {
				}

				if (!handleMethodDetected) {
					AbstractClassNode memberClassNode = this.targetMember.getClassNode();
					String memberType = this.targetMember.isField() ? "field" : "method";
					String warningMessage = "Class '"
						+ this.getOwnerClassName()
						+ "' accesses a "
						+ memberType
						+ " '"
						+ this.targetMember.getDisplayName()
						+ "' in class '"
						+ memberClassNode.getClassNameBinary()
						+ "'. However the descriptor of the "
						+ memberType
						+ " needs to be changed to '"
						+ TypeWriter.formatMethodSignature(this.targetMember.getNameDisplay(), tDesc)
						+ "'.  However this change cannot be made because class '"
						+ memberClassNode.getClassNameBinary()
						+ "' has not been opened for obfuscation.";
					scriptContext.logWarning(warningMessage);
				}
			}
		}
	}
}

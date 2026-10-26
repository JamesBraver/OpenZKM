package com.zelix.framework.writer.base;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.attributes.BootstrapMethodEntry;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.IVisitable;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.ResolvedMethodHandleWriter;
import com.zelix.utils.DebugUtils;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseDynamicCallWriter extends BaseDynamicWriter implements IVisitable, AbstractNamedWriter {
	public int bootstrapMethodIndex;
	public NameAndTypeWriter target;
	public BootstrapMethodEntry bootstrapMethodEntry;

	public BaseDynamicCallWriter(int index, AbstractConstantPoolWriter constantPool, int bootstrapMethodIndex, NameAndTypeWriter target) {
		super(index, constantPool);
		this.bootstrapMethodIndex = bootstrapMethodIndex;
		this.target = target;
	}

	public BaseDynamicCallWriter(int index, AbstractConstantPoolWriter constantPool, NameAndTypeWriter target, BootstrapMethodEntry bootstrapMethodEntry) {
		super(index, constantPool);
		this.target = target;
		this.bootstrapMethodEntry = bootstrapMethodEntry;
		this.bootstrapMethodIndex = bootstrapMethodEntry.getEntryIndex();
	}

	public BaseDynamicCallWriter(int index, NameAndTypeWriter target, InvokeDynamicCallWriter source) {
		super(index, source.constantPool);
		this.target = target;
		this.bootstrapMethodEntry = source.getBootstrapMethodEntry();
		this.bootstrapMethodIndex = this.bootstrapMethodEntry.getEntryIndex();
	}

	@Override
	public NameAndTypeWriter setTarget(NameAndTypeWriter newTarget) {
		NameAndTypeWriter previousTarget = this.target;
		this.target = newTarget;
		return previousTarget;
	}

	@Override
	public String getValueAsString() {
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		builder.append(this.target.getValueAsString());
		builder.append(']');
		builder.append(" : ");
		if (this.bootstrapMethodEntry != null) {
			builder.append(this.bootstrapMethodEntry.getDebugString());
		} else {
			builder.append(this.bootstrapMethodIndex);
		}

		return builder.toString();
	}

	@Override
	public void writeToStream(DataOutputStream dos, Map<TypeWriter, TypeWriter> twMap) throws IOException {
		dos.writeByte(this.getTypeTag().getTagID());
		if (this.bootstrapMethodEntry != null) {
			dos.writeShort(this.bootstrapMethodEntry.getEntryIndex());
		} else {
			dos.writeShort(this.bootstrapMethodIndex);
		}

		NameAndTypeWriter mappedTarget = (NameAndTypeWriter)twMap.get(this.target);
		if (mappedTarget != null) {
			dos.writeShort(mappedTarget.getTag());
		} else {
			dos.writeShort(this.target.getTag());
		}
	}

	public int getReturnStackSize() {
		return TypeWriter.getReturnDescriptor(this.target.getDescriptor()).equals("V") ? 0 : 1;
	}

	public String getDescriptor() {
		return this.target.getDescriptor();
	}

	public final NameAndTypeWriter getTarget() {
		return this.target;
	}

	public BootstrapMethodEntry getBootstrapMethodEntry() {
		return this.bootstrapMethodEntry;
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public void notifyParentToReindex() {
		if (this.bootstrapMethodEntry != null) {
			this.bootstrapMethodEntry.notifyParentToReindex();
		}
	}

	public void collectWorkspaceDependencies(Set<ClassNode> referencedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> referencedFields, Set<MethodNode> referencedMethods) {
		if (this.bootstrapMethodEntry != null) {
			this.bootstrapMethodEntry.collectWorkspaceDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
		}

		List<ClassNode> workspaceClassRefs = null;
		if (this.getTypeTag().equals(TypeTags.INVOKE_DYNAMIC)) {
			workspaceClassRefs = this.target.getWorkspaceClassRefs();
		} else if (this.getTypeTag().equals(TypeTags.CONSTANT_DYNAMIC)) {
			workspaceClassRefs = new ArrayList<>(1);
			ClassNode arrayElementClassNode = TypeWriter.getArrayElementWorkspaceNode(this.target.getDescriptor());
			if (arrayElementClassNode != null) {
				workspaceClassRefs.add(arrayElementClassNode);
			}
		} else {
			DebugUtils.assertTrue(false, new String[]{"Invalid tag : " + this.getTypeTag() + " '" + this.getOwnerSourcePath() + "'"});
		}

		AbstractClassNode ownerClassNode = this.getOwnerClassNode();
		if (ownerClassNode.hasInnerClassOrdinal()) {
			Integer ownerInnerClassOrdinal = ownerClassNode.getInnerClassOrdinal();
			ArrayList<ClassNode> innerClassOrdinalRefs = new ArrayList<>(workspaceClassRefs.size());

			for (ClassNode classNode : workspaceClassRefs) {
				if (classNode.hasInnerClasses()) {
					innerClassOrdinalRefs.add((ClassNode)classNode.findInnerClassByOrdinal(ownerInnerClassOrdinal));
				} else {
					innerClassOrdinalRefs.add(classNode);
				}
			}

			workspaceClassRefs = innerClassOrdinalRefs;
		}

		if (this.target.getName().equals("<init>")) {
			referencedClasses.addAll(workspaceClassRefs);
		} else {
			signatureClasses.addAll(workspaceClassRefs);
		}
	}

	public boolean setBootstrapMethodEntry(BootstrapMethodEntry bootstrapMethodEntry) {
		boolean entryChanged = false;
		if (this.bootstrapMethodEntry == null || this.bootstrapMethodEntry != bootstrapMethodEntry) {
			entryChanged = true;
		}

		this.bootstrapMethodEntry = bootstrapMethodEntry;
		return entryChanged;
	}

	public ResolvedMethodHandleWriter getBootstrapMethodHandle() {
		return this.bootstrapMethodEntry != null ? this.bootstrapMethodEntry.getBootstrapMethodHandle() : null;
	}

	public String getStackReturnType() {
		return getStackReturnType(this.target);
	}

	public String getTargetValueAsString() {
		return this.target.getValueAsString();
	}

	public void collectDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		if (this.bootstrapMethodEntry != null) {
			this.bootstrapMethodEntry.collectDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
		}

		List<AbstractClassNode> targetReferencedClasses = this.target.getReferencedClasses();
		if (this.target.getName().equals("<init>")) {
			referencedClasses.addAll(targetReferencedClasses);
		} else {
			signatureClasses.addAll(targetReferencedClasses);
		}
	}

	public int getBootstrapMethodIndex() {
		return this.bootstrapMethodEntry != null ? this.bootstrapMethodEntry.getEntryIndex() : this.bootstrapMethodIndex;
	}

	public TypeWriter[] getBootstrapArguments() {
		return this.bootstrapMethodEntry != null ? this.bootstrapMethodEntry.getBootstrapArguments() : null;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(this.getTypeTag().getTagID());
		if (this.bootstrapMethodEntry != null) {
			dos.writeShort(this.bootstrapMethodEntry.getEntryIndex());
		} else {
			dos.writeShort(this.bootstrapMethodIndex);
		}

		dos.writeShort(this.target.getTag());
	}
}

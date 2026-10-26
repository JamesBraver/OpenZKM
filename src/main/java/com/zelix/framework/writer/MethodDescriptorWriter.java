package com.zelix.framework.writer;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.BaseMethodTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodDescriptorWriter extends BaseMethodTypeWriter implements SimpleReferenceListener, IVisitable {
	private UTFStringWriter descriptorWriter;

	public MethodDescriptorWriter(int index, AbstractConstantPoolWriter constantPool, UTFStringWriter descriptorWriter) {
		super(index, constantPool);
		this.descriptorWriter = descriptorWriter;
	}

	@Override
	public String getValueAsString() {
		return this.getDescriptor();
	}

	@Override
	public void writeToStream(DataOutputStream dos, Map<TypeWriter, TypeWriter> twMap) throws IOException {
		dos.writeByte(BaseMethodTypeWriter.METHOD_TYPE.getTagID());
		UTFStringWriter remappedDescriptorEntry = (UTFStringWriter)twMap.get(this.descriptorWriter);
		if (remappedDescriptorEntry != null) {
			dos.writeShort(remappedDescriptorEntry.getTag());
		} else {
			dos.writeShort(this.descriptorWriter.getTag());
		}
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public void collectDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		List<AbstractClassNode> descriptorClasses = TypeWriter.getReferencedClasses(this.getDescriptor());
		signatureClasses.addAll(descriptorClasses);
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.descriptorWriter == oldReference) {
			this.descriptorWriter = newReference;
		}
	}

	public UTFStringWriter getDescriptorWriter() {
		return this.descriptorWriter;
	}

	public void collectWorkspaceDependencies(Set<ClassNode> renamedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> renamedFields, Set<MethodNode> renamedMethods) {
		List<ClassNode> descriptorClasses = TypeWriter.getReferencedWorkspaceClasses(this.getDescriptor());
		if (this.constantPool.getOwnerClassNode().isInnerClass()) {
			ArrayList<ClassNode> resolvedClasses = new ArrayList<>(descriptorClasses.size());

			for (ClassNode classNode : descriptorClasses) {
				resolvedClasses.add((ClassNode)this.resolveOwnerContext(classNode));
			}

			descriptorClasses = resolvedClasses;
		}

		signatureClasses.addAll(descriptorClasses);
	}

	public void remapDescriptor(HashMap<String, String> classNameMappings) {
		String descriptor = this.descriptorWriter.getValue();
		String remappedDescriptor = TypeWriter.remapDescriptor(descriptor, classNameMappings);
		if (remappedDescriptor != descriptor) {
			this.descriptorWriter.setValue(remappedDescriptor);
		}
	}

	public String getDescriptor() {
		return this.descriptorWriter.getValue();
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(BaseMethodTypeWriter.METHOD_TYPE.getTagID());
		dos.writeShort(this.descriptorWriter.getTag());
	}
}

package com.zelix.framework.annotation;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PrimitiveAnnotationElement extends AnnotationElementNode {
	private IntegerNode valueNode;

	public PrimitiveAnnotationElement(StructureNode parent, int typeTag, ByteDataBuffer dataBuffer) throws IOException {
		super(parent, typeTag);
		int constantIndex = dataBuffer.readUnsignedShort();
		TypeWriter constantEntry = this.getConstantPoolEntry(constantIndex);
		if (constantEntry != null && constantEntry instanceof IntegerNode) {
			this.valueNode = (IntegerNode)constantEntry;
		} else {
			this.setValid(false);
			this.setErrorMessage("Invalid component constant index : " + constantIndex + (constantEntry != null ? " : tag=" + constantEntry.getTypeTag() : ""));
		}
	}

	@Override
	public void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
	}

	@Override
	public boolean isArrayElement() {
		return false;
	}

	@Override
	public void syncName() {
	}

	@Override
	public boolean hasReferencedClass() {
		return false;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.valueNode.accept(visitor, this, this.getParent());
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		dataOutputStream.writeShort(this.valueNode.getTag());
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		dataOutputStream.writeShort(this.valueNode.getTag());
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
	}

	public Integer getValue() {
		return this.isValid() ? this.valueNode.getValue() : null;
	}

	@Override
	public void updateConstants() {
	}

	@Override
	public void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
	}

	@Override
	public String getTypeDescriptor() {
		return String.valueOf((char)this.getTypeTag());
	}

	@Override
	public String getReferencedClassName() {
		return null;
	}

	@Override
	public int getSize() {
		return 3;
	}
}

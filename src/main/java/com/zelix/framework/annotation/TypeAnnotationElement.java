package com.zelix.framework.annotation;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypeAnnotationElement extends AnnotationElementNode {
	private AnnotationNode annotationNode;

	public TypeAnnotationElement(StructureNode parent, int nameIndex, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex);
		this.annotationNode = AnnotationNode.readAnnotationNode(this, buf, utf8References);
		if (!this.annotationNode.isValid()) {
			this.setValid(false);
			this.setErrorMessage("Invalid nested annotation : " + this.annotationNode.getErrorMsg());
		}
	}

	@Override
	public void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		this.annotationNode.resolveTypeAndParams(classProvider, phantomClassManager, scriptContext);
	}

	@Override
	public boolean isArrayElement() {
		return false;
	}

	@Override
	public void syncName() {
		this.annotationNode.syncName();
	}

	@Override
	public boolean hasReferencedClass() {
		return false;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.annotationNode.acceptVisitor(visitor);
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		this.annotationNode.write(dataOutputStream);
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		this.annotationNode.writeRemapped(dataOutputStream, remappedConstants, scriptContext);
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		this.annotationNode.remapDescriptors(packageMappings, classNameMappings);
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (this.isValid()) {
			this.annotationNode.collectClassReferences(classReferences);
		}
	}

	@Override
	public void updateConstants() {
		this.annotationNode.updateConstants();
	}

	@Override
	public void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		this.annotationNode.collectWorkspaceRefs(classReferences, signatureClassReferences, fieldReferences, methodReferences);
	}

	@Override
	public String getTypeDescriptor() {
		return this.annotationNode.getTypeDescriptor();
	}

	@Override
	public String getReferencedClassName() {
		return null;
	}

	@Override
	public int getSize() {
		return 1 + this.annotationNode.getSize();
	}
}

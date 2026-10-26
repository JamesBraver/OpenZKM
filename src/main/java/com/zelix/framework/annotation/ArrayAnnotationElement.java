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

public class ArrayAnnotationElement extends AnnotationElementNode {
	private AnnotationElementNode[] elements;

	public ArrayAnnotationElement(StructureNode parent, int typeTag, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, typeTag);
		int elementCount = dataBuffer.readUnsignedShort();
		this.elements = new AnnotationElementNode[elementCount];

		for (int i = 0; i < elementCount; i++) {
			AnnotationElementNode element = AnnotationElementNode.createAnnotationElement(this, dataBuffer, utf8References);
			if (!element.isValid()) {
				this.setValid(false);
				this.setErrorMessage("Invalid nested annotation : " + element.getErrorMessage());
				return;
			}

			this.elements[i] = element;
		}
	}

	@Override
	public void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		for (int i = 0; i < this.elements.length; i++) {
			this.elements[i].resolveTypeAndParams(classProvider, phantomClassManager, scriptContext);
		}
	}

	@Override
	public boolean isArrayElement() {
		return true;
	}

	@Override
	public void syncName() {
		for (int i = 0; i < this.elements.length; i++) {
			this.elements[i].syncName();
		}
	}

	@Override
	public boolean hasReferencedClass() {
		return false;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		for (int i = 0; i < this.elements.length; i++) {
			this.elements[i].acceptVisitor(visitor);
		}
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		dataOutputStream.writeShort(this.elements.length);

		for (int i = 0; i < this.elements.length; i++) {
			this.elements[i].write(dataOutputStream);
		}
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		dataOutputStream.writeShort(this.elements.length);

		for (int i = 0; i < this.elements.length; i++) {
			this.elements[i].writeRemapped(dataOutputStream, remappedConstants, scriptContext);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
	}

	public AnnotationElementNode[] getElements() {
		return this.elements;
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		for (int i = 0; i < this.elements.length; i++) {
			this.elements[i].remapDescriptors(packageMappings, classNameMappings);
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (this.isValid()) {
			for (int i = 0; i < this.elements.length; i++) {
				this.elements[i].collectClassReferences(classReferences);
			}
		}
	}

	@Override
	public void updateConstants() {
		for (int i = 0; i < this.elements.length; i++) {
			this.elements[i].updateConstants();
		}
	}

	@Override
	public void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		for (int i = 0; i < this.elements.length; i++) {
			this.elements[i].collectWorkspaceRefs(classReferences, signatureClassReferences, fieldReferences, methodReferences);
		}
	}

	@Override
	public String getTypeDescriptor() {
		String commonDescriptor = null;

		for (int i = 0; i < this.elements.length; i++) {
			if (i == 0) {
				commonDescriptor = this.elements[i].getTypeDescriptor();
				if (commonDescriptor == null) {
					return null;
				}
			} else {
				String descriptor = this.elements[i].getTypeDescriptor();
				if (descriptor == null || !commonDescriptor.equals(descriptor)) {
					return null;
				}
			}
		}

		return commonDescriptor != null ? "[" + commonDescriptor : null;
	}

	@Override
	public String getReferencedClassName() {
		return null;
	}

	@Override
	public int getSize() {
		int size = 1;
		size += 2;

		for (int i = 0; i < this.elements.length; i++) {
			size += this.elements[i].getSize();
		}

		return size;
	}
}

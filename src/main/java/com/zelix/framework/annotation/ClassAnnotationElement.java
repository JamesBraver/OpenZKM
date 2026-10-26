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
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClassAnnotationElement extends AnnotationElementNode {
	private UTFStringWriter valueUtf;
	private int arrayDimensions;
	private AbstractClassNode classNode;

	public ClassAnnotationElement(StructureNode parent, int typeTag, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, typeTag);
		int constantIndex = dataBuffer.readUnsignedShort();
		TypeWriter constantEntry = this.getConstantPoolEntry(constantIndex);
		if (constantEntry != null && constantEntry instanceof UTFStringWriter) {
			this.valueUtf = (UTFStringWriter)constantEntry;
			utf8References.put(this.valueUtf, this);
		} else {
			this.setValid(false);
			this.setErrorMessage("Invalid component string class index : " + constantIndex + (constantEntry != null ? " : tag=" + constantEntry.getTypeTag() : ""));
		}
	}

	@Override
	public void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		AtomicInt arrayDepthHolder = new AtomicInt(0);
		String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.valueUtf.getValue(), arrayDepthHolder);
		this.arrayDimensions = arrayDepthHolder.get();
		if (elementInternalName != null) {
			this.classNode = classProvider.getClassNode(elementInternalName, "analyzing annotations in class '" + this.getSourcePath() + "' (A)", phantomClassManager);
		}
	}

	@Override
	public boolean isArrayElement() {
		return true;
	}

	@Override
	public void syncName() {
	}

	@Override
	public boolean hasReferencedClass() {
		return this.valueUtf != null;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.valueUtf.accept(visitor, this, this.getParent());
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		dataOutputStream.writeShort(this.valueUtf.getTag());
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		UTFStringWriter remappedValue = (UTFStringWriter)remappedConstants.get(this.valueUtf);
		if (remappedValue != null) {
			dataOutputStream.writeShort(remappedValue.getTag());
		} else {
			dataOutputStream.writeShort(this.valueUtf.getTag());
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.valueUtf == oldReference) {
			this.valueUtf = newReference;
		}
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		if (this.classNode != null) {
			this.valueUtf.setValue(AbstractClassNode.createClassDescriptor(this.classNode.getInternalName(), this.arrayDimensions));
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (this.isValid() && this.classNode != null) {
			classReferences.add(this.classNode);
		}
	}

	@Override
	public void updateConstants() {
	}

	@Override
	public void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (this.classNode != null && this.classNode.isWorkspaceNode()) {
			classReferences.add((ClassNode)this.classNode);
		}
	}

	@Override
	public String getTypeDescriptor() {
		return "Ljava/lang/Class;";
	}

	@Override
	public String getReferencedClassName() {
		if (this.valueUtf != null) {
			String value = this.valueUtf.getValue();
			return value.startsWith("[") ? value : value.substring(1, value.length() - 1);
		} else {
			return null;
		}
	}

	@Override
	public int getSize() {
		return 3;
	}
}

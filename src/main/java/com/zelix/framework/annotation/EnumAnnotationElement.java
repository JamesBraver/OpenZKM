package com.zelix.framework.annotation;

import com.zelix.exceptions.MissingMemberException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EnumAnnotationElement extends AnnotationElementNode {
	private UTFStringWriter typeUtf;
	private UTFStringWriter valueNameUtf;
	private AbstractFieldNode enumFieldNode;
	private AbstractClassNode enumClassNode;

	public EnumAnnotationElement(StructureNode parent, int typeTag, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, typeTag);
		int typeConstantIndex = dataBuffer.readUnsignedShort();
		TypeWriter constantEntry = this.getConstantPoolEntry(typeConstantIndex);
		if (constantEntry != null && constantEntry instanceof UTFStringWriter) {
			this.typeUtf = (UTFStringWriter)constantEntry;
			utf8References.put(this.typeUtf, this);
			int valueConstantIndex = dataBuffer.readUnsignedShort();
			constantEntry = this.getConstantPoolEntry(valueConstantIndex);
			if (constantEntry != null && constantEntry instanceof UTFStringWriter) {
				this.valueNameUtf = (UTFStringWriter)constantEntry;
				utf8References.put(this.valueNameUtf, this);
			} else {
				this.setValid(false);
				this.setErrorMessage("Invalid component enum value constant index : " + valueConstantIndex + (constantEntry != null ? " : tag=" + constantEntry.getTypeTag() : ""));
			}
		} else {
			this.setValid(false);
			this.setErrorMessage("Invalid component enum name constant index : " + typeConstantIndex + (constantEntry != null ? " : tag=" + constantEntry.getTypeTag() : ""));
		}
	}

	@Override
	public void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		AtomicInt arrayDepthHolder = new AtomicInt(0);
		String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.typeUtf.getValue(), arrayDepthHolder);
		if (elementInternalName != null) {
			AbstractClassNode classNode = this.getClassNode();
			Integer innerClassOrdinal = classNode.hasInnerClassOrdinal() ? classNode.getInnerClassOrdinal() : null;
			this.enumClassNode = classProvider.getClassNode(elementInternalName, innerClassOrdinal, "analyzing annotations in class '" + this.getSourcePath() + "' (B)", phantomClassManager);
		}

		if (this.enumClassNode != null) {
			this.enumFieldNode = this.enumClassNode.findField(this.valueNameUtf.getValue(), this.typeUtf.getValue());
			if (this.enumFieldNode == null
				&& (!Options.ZKM_IGNORE_MISSING_MEMBERS || this.enumClassNode.isWorkspaceNode())
				&& (phantomClassManager == null || !phantomClassManager.isFieldIgnored(this.enumClassNode, this.valueNameUtf.getValue(), this.typeUtf.getValue(), new AtomicReference<>()))) {
				throw new MissingMemberException(
					"Could not find field '"
						+ TypeWriter.descriptorToHumanReadable(this.typeUtf.getValue())
						+ " "
						+ this.valueNameUtf.getValue()
						+ "' in class '"
						+ this.enumClassNode.getSourcePath()
						+ "'. Please check the classpath and reopen your classes (C)."
				);
			}
		}
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

	public String getEnumValueName() {
		return this.valueNameUtf.getValueAsString();
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.typeUtf.accept(visitor, this, this.getParent());
		this.valueNameUtf.accept(visitor, this, this.getParent());
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		dataOutputStream.writeShort(this.typeUtf.getTag());
		dataOutputStream.writeShort(this.valueNameUtf.getTag());
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		dataOutputStream.writeByte(this.getTypeTag());
		AtomicInt arrayDepthHolder = new AtomicInt(0);
		AbstractClassNode.getArrayElementInternalName(this.typeUtf.getValue(), arrayDepthHolder);
		UTFStringWriter remappedType = (UTFStringWriter)remappedConstants.get(this.typeUtf);
		if (remappedType != null) {
			AbstractClassNode.getArrayElementInternalName(remappedType.getValue(), arrayDepthHolder);
			dataOutputStream.writeShort(remappedType.getTag());
		} else {
			dataOutputStream.writeShort(this.typeUtf.getTag());
		}

		UTFStringWriter remappedValueName = (UTFStringWriter)remappedConstants.get(this.valueNameUtf);
		if (remappedValueName != null) {
			dataOutputStream.writeShort(remappedValueName.getTag());
		} else {
			dataOutputStream.writeShort(this.valueNameUtf.getTag());
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.typeUtf == oldReference) {
			this.typeUtf = newReference;
		} else if (this.valueNameUtf == oldReference) {
			this.valueNameUtf = newReference;
		}
	}

	public String getEnumTypeDescriptor() {
		return this.typeUtf.getValueAsString();
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		if (this.enumClassNode != null) {
			this.typeUtf.setValue(AbstractClassNode.createClassDescriptor(this.enumClassNode.getInternalName(), 0));
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (this.isValid() && this.enumClassNode != null) {
			classReferences.add(this.enumClassNode);
		}
	}

	@Override
	public void updateConstants() {
		if (this.enumFieldNode != null && !this.enumFieldNode.getNameDisplay().equals(this.valueNameUtf.getValue())) {
			this.valueNameUtf.setValue(this.enumFieldNode.getName());
		}
	}

	@Override
	public void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (this.enumClassNode != null && this.enumClassNode.isWorkspaceNode()) {
			classReferences.add((ClassNode)this.enumClassNode);
			if (this.enumFieldNode != null && this.enumFieldNode.isWorkspaceNode()) {
				fieldReferences.add((FieldNode)this.enumFieldNode);
			}
		}
	}

	@Override
	public String getTypeDescriptor() {
		return this.typeUtf.getValue();
	}

	@Override
	public String getReferencedClassName() {
		return null;
	}

	@Override
	public int getSize() {
		return 5;
	}
}

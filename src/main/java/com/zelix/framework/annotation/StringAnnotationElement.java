package com.zelix.framework.annotation;

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
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StringAnnotationElement extends AnnotationElementNode {
	public static DualKeyMap<String, String, AnnotationElementTypes> ELEMENT_TYPE_OVERRIDES = new DualKeyMap<>();
	private UTFStringWriter valueUtf;
	private FieldNode fieldNode;

	public StringAnnotationElement(StructureNode parent, int typeTag, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, typeTag);
		int constantIndex = dataBuffer.readUnsignedShort();
		TypeWriter constantEntry = this.getConstantPoolEntry(constantIndex);
		if (constantEntry != null && constantEntry instanceof UTFStringWriter) {
			this.valueUtf = (UTFStringWriter)constantEntry;
			utf8References.put(this.valueUtf, this);
		} else {
			this.setValid(false);
			this.setErrorMessage("Invalid component string constant index : " + constantIndex + (constantEntry != null ? " : tag=" + constantEntry.getTypeTag() : ""));
		}
	}

	@Override
	public void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		AnnotationElementTypes elementType = ELEMENT_TYPE_OVERRIDES.get(this.getAnnotationTypeName(), this.getElementName());
		if (elementType != null && elementType == AnnotationElementTypes.FIELD) {
			FieldNode referencedField = this.findReferencedField();
			if (referencedField != null) {
				this.fieldNode = referencedField;
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

	public String getValue() {
		return this.valueUtf.getValueAsString();
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.valueUtf == oldReference) {
			this.valueUtf = newReference;
		}
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		AnnotationElementTypes elementType = ELEMENT_TYPE_OVERRIDES.get(this.getAnnotationTypeName(), this.getElementName());
		boolean packageType = elementType != null && elementType.equals(AnnotationElementTypes.PACKAGE);
		String value = this.valueUtf.getValue();
		if (packageType) {
			String remappedPackageName = TypeWriter.remapClassName(value, packageMappings, true);
			if (!remappedPackageName.equals(value)) {
				this.valueUtf.setValue(remappedPackageName);
			}
		} else if (elementType != AnnotationElementTypes.FIELD) {
			AtomicBool descriptorRemapped = new AtomicBool();
			String remappedDescriptor = TypeWriter.tryRemapDescriptor(value, classNameMappings, descriptorRemapped);
			if (!remappedDescriptor.equals(value)) {
				this.valueUtf.setValue(remappedDescriptor);
			} else if (!descriptorRemapped.get()) {
				remappedDescriptor = TypeWriter.remapClassName(value, packageMappings, false);
				if (!remappedDescriptor.equals(value)) {
					this.valueUtf.setValue(remappedDescriptor);
				}
			}
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (this.isValid()) {
			String internalName = TypeWriter.normalizeToInternalName(this.valueUtf.getValue());
			if (internalName != null) {
				AbstractClassNode classNode = ClassHierarchyNode.getClassNodeByInternalName(internalName);
				if (classNode != null) {
					classReferences.add(classNode);
				}
			}
		}
	}

	@Override
	public void updateConstants() {
		if (this.fieldNode != null && !this.fieldNode.getNameDisplay().equals(this.valueUtf.getValue())) {
			this.valueUtf.setValue(this.fieldNode.getNameDisplay());
		}
	}

	@Override
	public void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		String internalName = TypeWriter.normalizeToInternalName(this.valueUtf.getValue());
		if (internalName != null && ClassHierarchyNode.isWorkspaceClassByInternalName(internalName)) {
			classReferences.add(ClassHierarchyNode.getWorkspaceClassNode(internalName));
		}

		if (ELEMENT_TYPE_OVERRIDES.get(this.getAnnotationTypeName(), this.getElementName()) == AnnotationElementTypes.FIELD) {
			FieldNode referencedField = this.findReferencedField();
			if (referencedField != null) {
				fieldReferences.add(referencedField);
			}
		}
	}

	private FieldNode findReferencedField() {
		AbstractClassNode classNode = this.getClassNode();
		if (classNode.isWorkspaceNode()) {
			AbstractFieldNode[] fields = classNode.findFieldsByName(this.valueUtf.getValueAsString());
			if (fields.length == 1) {
				return (FieldNode)fields[0];
			}
		}

		return null;
	}

	@Override
	public String getTypeDescriptor() {
		return "Ljava/lang/String;";
	}

	public void setValue(String value) {
		this.valueUtf.setValue(value);
	}

	@Override
	public String getReferencedClassName() {
		return null;
	}

	@Override
	public int getSize() {
		return 3;
	}

	static {
		ELEMENT_TYPE_OVERRIDES.put("org/springframework/context/annotation/ComponentScan", "basePackages", AnnotationElementTypes.PACKAGE);
		ELEMENT_TYPE_OVERRIDES.put("org/springframework/context/annotation/ComponentScan", "value", AnnotationElementTypes.PACKAGE);
		ELEMENT_TYPE_OVERRIDES.put("javax/xml/bind/annotation/XmlType", "propOrder", AnnotationElementTypes.FIELD);
	}
}

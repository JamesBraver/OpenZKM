package com.zelix.framework.annotation;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.signature.MethodSignature;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotationParameterNode extends StructureNode implements SimpleReferenceListener, AnnotationElement {
	private boolean valid = true;
	private UTFStringWriter nameUtf;
	private String errorMessage;
	private AnnotationElementNode element;
	private AbstractMethodNode annotationMethod;

	public AnnotationParameterNode(StructureNode parent, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent);
		int nameIndex = dataBuffer.readUnsignedShort();
		TypeWriter nameEntry = this.getConstantPoolEntry(nameIndex);
		if (nameEntry != null && nameEntry instanceof UTFStringWriter) {
			this.nameUtf = (UTFStringWriter)nameEntry;
			utf8References.put(this.nameUtf, this);
			this.element = AnnotationElementNode.createAnnotationElement(this, dataBuffer, utf8References);
			if (!this.element.isValid()) {
				this.valid = false;
				this.errorMessage = "Invalid component value : " + this.element.getErrorMessage();
			}
		} else {
			this.valid = false;
			this.errorMessage = "Invalid component name index : " + nameIndex + (nameEntry != null ? " : tag=" + nameEntry.getTypeTag() + " : \"" + nameEntry.toDisplayString() + "\"" : "");
		}
	}

	public String getOriginalName() {
		if (this.valid) {
			return this.annotationMethod != null ? this.annotationMethod.getOriginalName() : this.getName();
		} else {
			return null;
		}
	}

	public String getName() {
		return this.valid ? this.nameUtf.getValue() : null;
	}

	@Override
	public boolean isValid() {
		return this.valid;
	}

	@Override
	public void syncName() {
		if (this.annotationMethod != null && !this.annotationMethod.getName().equals(this.nameUtf.getValue())) {
			this.nameUtf.setValue(this.annotationMethod.getName());
		}

		this.element.syncName();
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		if (this.valid) {
			this.nameUtf.accept(visitor, this, this.getParent());
			this.element.acceptVisitor(visitor);
		}
	}

	public AnnotationElementNode getElement() {
		return this.element;
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.nameUtf.getTag());
		this.element.write(dataOutputStream);
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		UTFStringWriter remappedName = (UTFStringWriter)remappedConstants.get(this.nameUtf);
		if (remappedName != null) {
			dataOutputStream.writeShort(remappedName.getTag());
		} else {
			dataOutputStream.writeShort(this.nameUtf.getTag());
		}

		this.element.writeRemapped(dataOutputStream, remappedConstants, scriptContext);
	}

	public void resolveTypeAndParams(AbstractClassNode typeClass, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		if (typeClass != null) {
			String paramName = this.nameUtf.getValue();
			String typeDescriptor = this.element.getTypeDescriptor();
			if (typeDescriptor != null) {
				this.annotationMethod = typeClass.findMethod(new MethodSignature(paramName, "()", typeDescriptor));
			}

			if (this.annotationMethod == null) {
				List<AbstractMethodNode> methods = typeClass.findMethodsByName(paramName);
				if (methods.size() == 1) {
					this.annotationMethod = methods.get(0);
				}
			}
		}

		this.element.resolveTypeAndParams(classProvider, phantomClassManager, scriptContext);
	}

	public String getReferencedClassName() {
		return this.element.isValid() && this.element.hasReferencedClass() ? this.element.getReferencedClassName() : null;
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.nameUtf == oldReference) {
			this.nameUtf = newReference;
		}
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		this.element.remapDescriptors(packageMappings, classNameMappings);
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (this.valid) {
			this.element.collectClassReferences(classReferences);
		}
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	public void collectWorkspaceRefs(AbstractClassNode typeClass, Set<ClassNode> referencedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> referencedFields, Set<MethodNode> referencedMethods) {
		if (typeClass != null && this.annotationMethod != null && this.annotationMethod.isWorkspaceNode()) {
			referencedMethods.add((MethodNode)this.annotationMethod);
		}

		this.element.collectWorkspaceRefs(referencedClasses, signatureClasses, referencedFields, referencedMethods);
	}

	@Override
	public void updateConstants() {
		this.element.updateConstants();
	}

	@Override
	public int getSize() {
		return 2 + this.element.getSize();
	}
}

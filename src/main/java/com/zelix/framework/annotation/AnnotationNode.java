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
import com.zelix.klassmaster.config.Options;
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

public class AnnotationNode extends StructureNode implements SimpleReferenceListener, AnnotationElement {
	public boolean valid = true;
	public UTFStringWriter typeDescUtf;
	public String errorMsg;
	public int paramCount;
	public AnnotationParameterNode[] params;
	public int arrayDepth;
	public AbstractClassNode typeClass;

	public AnnotationNode(StructureNode parent, int typeIndex, TypeWriter typeWriter, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent);
		if (typeWriter != null && typeWriter instanceof UTFStringWriter) {
			this.typeDescUtf = (UTFStringWriter)typeWriter;
			String typeDescriptor = this.typeDescUtf.getValue();
			if (typeDescriptor.startsWith("L") && typeDescriptor.endsWith(";")) {
				utf8References.put(this.typeDescUtf, this);
				this.paramCount = dataBuffer.readUnsignedShort();
				this.params = new AnnotationParameterNode[this.paramCount];

				for (int i = 0; i < this.paramCount; i++) {
					this.params[i] = new AnnotationParameterNode(this, dataBuffer, utf8References);
					if (!this.params[i].isValid()) {
						this.valid = false;
						this.errorMsg = this.params[i].getErrorMessage();
						return;
					}
				}
			} else {
				this.valid = false;
				this.errorMsg = "Invalid annotation type '" + typeDescriptor + "'";
			}
		} else {
			this.valid = false;
			this.errorMsg = "Invalid type index : " + typeIndex + (typeWriter != null ? " : tag=" + typeWriter.getTypeTag() + " : \"" + typeWriter.toDisplayString() + "\"" : "");
		}
	}

	public static AnnotationNode readAnnotationNode(StructureNode parent, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		int constantIndex = dataBuffer.readUnsignedShort();
		TypeWriter constantEntry = parent.getConstantPoolEntry(constantIndex);
		return (AnnotationNode)(constantEntry != null
				&& constantEntry instanceof UTFStringWriter
				&& ((UTFStringWriter)constantEntry).getValue().equals("Lkotlin/Metadata;")
				&& (Options.ZKM_TRANSLATE_KOTLIN || Options.translateKotlinMetadata)
			? new KotlinMetadataNode(parent, constantIndex, constantEntry, dataBuffer, utf8References)
			: new AnnotationNode(parent, constantIndex, constantEntry, dataBuffer, utf8References));
	}

	@Override
	public void syncName() {
		for (int i = 0; i < this.paramCount; i++) {
			this.params[i].syncName();
		}
	}

	@Override
	public boolean isValid() {
		return this.valid;
	}

	public String getOriginalTypeName() {
		if (this.valid) {
			return this.typeClass != null ? this.typeClass.getClassName() : this.getTypeInternalName();
		} else {
			return null;
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		if (this.valid) {
			this.typeDescUtf.accept(visitor, this, this.getParent());

			for (int i = 0; i < this.paramCount; i++) {
				this.params[i].acceptVisitor(visitor);
			}
		}
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.typeDescUtf.getTag());
		dataOutputStream.writeShort(this.paramCount);

		for (int i = 0; i < this.paramCount; i++) {
			this.params[i].write(dataOutputStream);
		}
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		UTFStringWriter remappedTypeDesc = (UTFStringWriter)remappedConstants.get(this.typeDescUtf);
		if (remappedTypeDesc != null) {
			dataOutputStream.writeShort(remappedTypeDesc.getTag());
		} else {
			dataOutputStream.writeShort(this.typeDescUtf.getTag());
		}

		dataOutputStream.writeShort(this.paramCount);

		for (int i = 0; i < this.paramCount; i++) {
			this.params[i].writeRemapped(dataOutputStream, remappedConstants, scriptContext);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.typeDescUtf == oldReference) {
			this.typeDescUtf = newReference;
		}
	}

	public String getTypeDescriptor() {
		return this.typeDescUtf.getValue();
	}

	public void collectWorkspaceRefs(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (this.typeClass != null && this.typeClass.isWorkspaceNode()) {
			classReferences.add((ClassNode)this.typeClass);
		}

		for (int i = 0; i < this.paramCount; i++) {
			this.params[i].collectWorkspaceRefs(this.typeClass, classReferences, signatureClassReferences, fieldReferences, methodReferences);
		}
	}

	public String findParamLiteral(String paramName, boolean matchOnOriginalNames) {
		for (int i = 0; i < this.paramCount; i++) {
			String candidateName;
			if (matchOnOriginalNames) {
				candidateName = this.params[i].getOriginalName();
			} else {
				candidateName = this.params[i].getName();
			}

			if (paramName.equals(candidateName)) {
				return this.params[i].getReferencedClassName();
			}
		}

		return null;
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		if (this.typeClass != null) {
			String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.typeDescUtf.getValue());
			String typeInternalName = this.typeClass.getInternalName();
			if (!typeInternalName.equals(elementInternalName)) {
				String justifiedDescriptor = AbstractClassNode.createClassDescriptor(this.typeClass.getInternalName(), this.arrayDepth);
				this.typeDescUtf.setValue(justifiedDescriptor);
			}

			for (int i = 0; i < this.paramCount; i++) {
				this.params[i].remapDescriptors(packageMappings, classNameMappings);
			}
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (this.valid) {
			if (this.typeClass != null) {
				classReferences.add(this.typeClass);
			}

			for (int i = 0; i < this.paramCount; i++) {
				this.params[i].collectClassReferences(classReferences);
			}
		}
	}

	public void resolveTypeAndParams(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		AtomicInt arrayDepthHolder = new AtomicInt(0);
		String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.typeDescUtf.getValue(), arrayDepthHolder);
		this.arrayDepth = arrayDepthHolder.get();
		if (elementInternalName != null) {
			this.typeClass = classProvider.getClassNode(elementInternalName, "analyzing annotations in class '" + this.getSourcePath() + "' (C)", phantomClassManager);
		}

		for (int i = 0; i < this.paramCount; i++) {
			this.params[i].resolveTypeAndParams(this.typeClass, classProvider, phantomClassManager, scriptContext);
		}
	}

	@Override
	public void updateConstants() {
		for (int i = 0; i < this.paramCount; i++) {
			this.params[i].updateConstants();
		}
	}

	public String getTypeInternalName() {
		if (this.valid) {
			String typeDescriptor = this.typeDescUtf.getValue();
			return typeDescriptor.substring(1, typeDescriptor.length() - 1);
		} else {
			return null;
		}
	}

	public AbstractClassNode getTypeClass() {
		return this.typeClass;
	}

	public String getErrorMsg() {
		return this.errorMsg;
	}

	@Override
	public int getSize() {
		int size = 4;

		for (int i = 0; i < this.paramCount; i++) {
			size += this.params[i].getSize();
		}

		return size;
	}
}

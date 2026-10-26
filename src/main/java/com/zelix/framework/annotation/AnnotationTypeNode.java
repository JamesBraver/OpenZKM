package com.zelix.framework.annotation;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractTypeAnnotationsAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnnotationTypeNode extends StructureNode implements SimpleReferenceListener, AnnotationElement {
	private boolean valid = true;
	private AnnotationTypeReferenceNode annotationTypeRef;
	private String errorMessage;
	private UTFStringWriter typeDescriptor;
	private int parameterCount;
	private AnnotationParameterNode[] parameters;
	private int arrayDimensions;
	private AbstractClassNode typeClass;

	public AnnotationTypeNode(
		AbstractTypeAnnotationsAttributeNode parent,
		ByteDataBuffer dataBuffer,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<Integer, IGraphElement> offsetReferences
	) throws IOException {
		super(parent);
		this.annotationTypeRef = new AnnotationTypeReferenceNode(this, dataBuffer, utf8References, offsetReferences);
		if (!this.annotationTypeRef.isValid()) {
			this.valid = false;
			this.errorMessage = this.annotationTypeRef.getErrorMessage();
		} else {
			int constantIndex = dataBuffer.readUnsignedShort();
			TypeWriter constantEntry = this.getConstantPoolEntry(constantIndex);
			if (constantEntry != null && constantEntry instanceof UTFStringWriter) {
				this.typeDescriptor = (UTFStringWriter)constantEntry;
				String descriptorText = this.typeDescriptor.getValue();
				if (descriptorText.startsWith("L") && descriptorText.endsWith(";")) {
					utf8References.put(this.typeDescriptor, this);
					this.parameterCount = dataBuffer.readUnsignedShort();
					this.parameters = new AnnotationParameterNode[this.parameterCount];

					for (int i = 0; i < this.parameterCount; i++) {
						this.parameters[i] = new AnnotationParameterNode(this, dataBuffer, utf8References);
						if (!this.parameters[i].isValid()) {
							this.valid = false;
							this.errorMessage = this.parameters[i].getErrorMessage();
							return;
						}
					}
				} else {
					this.valid = false;
					this.errorMessage = "Invalid annotation type '" + descriptorText + "'";
				}
			} else {
				this.valid = false;
				this.errorMessage = "Invalid type index : " + constantIndex + (constantEntry != null ? " : tag=" + constantEntry.getTypeTag() + " : \"" + constantEntry.toDisplayString() + "\"" : "");
			}
		}
	}

	public void collectAllReferences(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		if (this.typeClass != null && this.typeClass.isWorkspaceNode()) {
			classReferences.add((ClassNode)this.typeClass);
		}

		for (int i = 0; i < this.parameterCount; i++) {
			this.parameters[i].collectWorkspaceRefs(this.typeClass, classReferences, signatureClassReferences, fieldReferences, methodReferences);
		}

		this.annotationTypeRef.collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
	}

	public boolean isThrowsTarget() {
		return this.annotationTypeRef.isThrowsTarget();
	}

	@Override
	public void syncName() {
		for (int i = 0; i < this.parameterCount; i++) {
			this.parameters[i].syncName();
		}
	}

	@Override
	public boolean isValid() {
		return this.valid;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.typeDescriptor.accept(visitor, this, this.getParent());

		for (int i = 0; i < this.parameterCount; i++) {
			this.parameters[i].acceptVisitor(visitor);
		}

		this.annotationTypeRef.acceptVisitor(visitor);
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	@Override
	public void write(DataOutputStream dataOutputStream) throws IOException {
		this.annotationTypeRef.write(dataOutputStream);
		dataOutputStream.writeShort(this.typeDescriptor.getTag());
		dataOutputStream.writeShort(this.parameterCount);

		for (int i = 0; i < this.parameterCount; i++) {
			this.parameters[i].write(dataOutputStream);
		}
	}

	@Override
	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		this.annotationTypeRef.writeRemapped(dataOutputStream, remappedConstants, scriptContext);
		UTFStringWriter remappedTypeDesc = (UTFStringWriter)remappedConstants.get(this.typeDescriptor);
		if (remappedTypeDesc != null) {
			dataOutputStream.writeShort(remappedTypeDesc.getTag());
		} else {
			dataOutputStream.writeShort(this.typeDescriptor.getTag());
		}

		dataOutputStream.writeShort(this.parameterCount);

		for (int i = 0; i < this.parameterCount; i++) {
			this.parameters[i].writeRemapped(dataOutputStream, remappedConstants, scriptContext);
		}
	}

	public void resolveReferences(ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		AtomicInt arrayDepthHolder = new AtomicInt(0);
		String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.typeDescriptor.getValue(), arrayDepthHolder);
		if (elementInternalName != null) {
			AbstractClassNode classNode = this.getClassNode();
			Integer innerClassOrdinal = classNode.hasInnerClassOrdinal() ? classNode.getInnerClassOrdinal() : null;
			this.typeClass = classProvider.getClassNode(elementInternalName, innerClassOrdinal, "analyzing annotations in class '" + this.getSourcePath() + "' (D)", phantomClassManager);
		}

		this.arrayDimensions = arrayDepthHolder.get();

		for (int i = 0; i < this.parameterCount; i++) {
			this.parameters[i].resolveTypeAndParams(this.typeClass, classProvider, phantomClassManager, scriptContext);
		}
	}

	public boolean isTypeDependent() {
		return this.annotationTypeRef.isTypeDependent();
	}

	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		this.annotationTypeRef.registerToGraph(graph);
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.typeDescriptor == oldReference) {
			this.typeDescriptor = newReference;
		}
	}

	public boolean removeInGraph(HashSet<LabelNode> removedLabels, ReferenceGraph<LabelNode, IGraphElement> graph) {
		return this.annotationTypeRef.removeInGraph(removedLabels, graph);
	}

	public AbstractClassNode getTypeClass() {
		return this.typeClass;
	}

	@Override
	public void remapDescriptors(HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		if (this.typeClass != null) {
			String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.typeDescriptor.getValue());
			String typeInternalName = this.typeClass.getInternalName();
			if (!typeInternalName.equals(elementInternalName)) {
				String justifiedDescriptor = AbstractClassNode.createClassDescriptor(this.typeClass.getInternalName(), this.arrayDimensions);
				this.typeDescriptor.setValue(justifiedDescriptor);
			}
		}

		for (int i = 0; i < this.parameterCount; i++) {
			this.parameters[i].remapDescriptors(packageMappings, classNameMappings);
		}
	}

	@Override
	public void collectClassReferences(Set<AbstractClassNode> classReferences) {
		if (this.valid) {
			if (this.typeClass != null) {
				classReferences.add(this.typeClass);
			}

			for (int i = 0; i < this.parameterCount; i++) {
				this.parameters[i].collectClassReferences(classReferences);
			}

			this.annotationTypeRef.collectClassReferences(classReferences);
		}
	}

	@Override
	public void updateConstants() {
		for (int i = 0; i < this.parameterCount; i++) {
			this.parameters[i].updateConstants();
		}
	}

	@Override
	public int getSize() {
		int size = this.annotationTypeRef.getSize();
		size += 4;

		for (int i = 0; i < this.parameterCount; i++) {
			size += this.parameters[i].getSize();
		}

		return size;
	}
}

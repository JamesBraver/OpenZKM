package com.zelix.framework.attributes.declaration;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.TypeNode;
import com.zelix.framework.annotation.AnnotationContainer;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.IDynamicCallUpdatable;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RecordComponentNode extends StructureNode implements SimpleReferenceListener {
	private boolean isParsedSuccessfully = true;
	private UTFStringWriter nameWriter;
	private String errorMessage;
	private UTFStringWriter descriptorWriter;
	private AttributesNode[] attributes;
	public AbstractFieldNode resolvedField;
	public AbstractMethodNode resolvedAccessor;

	public RecordComponentNode(
		StructureNode parent,
		ByteDataBuffer dataBuffer,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes,
		PrintWriter printWriter
	) throws IOException {
		super(parent);
		int nameIndex = dataBuffer.readUnsignedShort();
		TypeWriter nameEntry = this.getConstantPoolEntry(nameIndex);
		if (nameEntry instanceof UTFStringWriter) {
			this.nameWriter = (UTFStringWriter)nameEntry;
			utf8References.put(this.nameWriter, this);
		} else {
			this.isParsedSuccessfully = false;
			this.errorMessage = "Name index does not point to a CONSTANT_Utf8 : " + nameEntry.getTypeTag();
		}

		int descriptorIndex = dataBuffer.readUnsignedShort();
		TypeWriter descriptorEntry = this.getConstantPoolEntry(descriptorIndex);
		if (descriptorEntry instanceof UTFStringWriter) {
			this.descriptorWriter = (UTFStringWriter)descriptorEntry;
			utf8References.put(this.descriptorWriter, this);
		} else {
			this.isParsedSuccessfully = false;
			this.errorMessage = "Descriptor index does not point to a CONSTANT_Utf8 : " + descriptorEntry.getTypeTag();
		}

		int attributeCount = dataBuffer.readUnsignedShort();
		this.attributes = new AttributesNode[attributeCount];
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences = new MultiMap<>();
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences = new MultiMap<>();
		MultiMap<IntegerNode, IntegerConstants> integerReferences = new MultiMap<>();
		MultiMap<LongNode, LongConstants> longReferences = new MultiMap<>();
		MultiMap<MemberTypeWriter, TypeNode> memberReferences = new MultiMap<>();
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences = new MultiMap<>();
		MultiMap<Integer, IGraphElement> graphElementReferences = new MultiMap<>();

		for (int i = 0; i < attributeCount; i++) {
			try {
				this.attributes[i] = AttributesNode.readAttribute(this, dataBuffer, utf8References, nameAndTypeReferences, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences, printWriter, graphElementReferences, unknownAttributes);
			} catch (ProcessingException ignored) {
				this.isParsedSuccessfully = false;
				this.errorMessage = "Invalid attribute : '" + ignored.getMessage() + "'";
				throw ignored;
			}
		}
	}

	public boolean isValid() {
		return this.isParsedSuccessfully;
	}

	public void collectAllReferences(HashSet<ClassNode> classReferences, HashSet<ClassNode> signatureClassReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		if (this.resolvedField != null && this.resolvedField.isWorkspaceNode()) {
			fieldReferences.add((FieldNode)this.resolvedField);
		}

		if (this.resolvedAccessor != null && this.resolvedAccessor.isWorkspaceNode()) {
			methodReferences.add((MethodNode)this.resolvedAccessor);
		}

		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}

	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageMappings, HashMap<String, String> classNameMappings) {
		if (this.isParsedSuccessfully) {
			String descriptor = this.descriptorWriter.getValue();
			String remappedDescriptor = TypeWriter.remapDescriptor(descriptor, classNameMappings);
			if (!remappedDescriptor.equals(descriptor)) {
				this.descriptorWriter.setValue(remappedDescriptor);
			}

			AttributesNode[] attributeNodes = this.attributes;
			int attributeCount = attributeNodes.length;

			for (int i = 0; i < attributeCount; i++) {
				attributeNodes[i].remapDescriptors(keepInnerClassInfo, keepGenericsInformation, packageMappings, classNameMappings);
			}
		}
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.nameWriter, this, this.getParent());
		visitor.visit(this.descriptorWriter, this, this.getParent());
		AttributesNode[] attributeNodes = this.attributes;
		int attributeCount = attributeNodes.length;

		for (int i = 0; i < attributeCount; i++) {
			attributeNodes[i].acceptVisitor(visitor);
		}
	}

	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.nameWriter.getTag());
		dataOutputStream.writeShort(this.descriptorWriter.getTag());
		dataOutputStream.writeShort(this.attributes.length);
		AttributesNode[] attributeNodes = this.attributes;
		int attributeCount = attributeNodes.length;

		for (int i = 0; i < attributeCount; i++) {
			attributeNodes[i].writeAttributeBody(dataOutputStream);
		}
	}

	public void syncAnnotationNames() {
		if (this.isParsedSuccessfully) {
			for (AttributesNode attributeNode : this.attributes) {
				if (attributeNode instanceof AnnotationContainer) {
					((AnnotationContainer)attributeNode).syncNames();
				}
			}
		}
	}

	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants, ScriptContext scriptContext) throws IOException {
		TypeWriter remappedName = remappedConstants.get(this.nameWriter);
		if (remappedName != null) {
			dataOutputStream.writeShort(remappedName.getTag());
		} else {
			dataOutputStream.writeShort(this.nameWriter.getTag());
		}

		TypeWriter remappedDescriptor = remappedConstants.get(this.descriptorWriter);
		if (remappedDescriptor != null) {
			dataOutputStream.writeShort(remappedDescriptor.getTag());
		} else {
			dataOutputStream.writeShort(this.descriptorWriter.getTag());
		}

		dataOutputStream.writeShort(this.attributes.length);
		AttributesNode[] attributeNodes = this.attributes;
		int attributeCount = attributeNodes.length;

		for (int i = 0; i < attributeCount; i++) {
			attributeNodes[i].writeAttributeBodyRemapped(dataOutputStream, remappedConstants, scriptContext);
		}
	}

	public void trimExcludedAnnotations(TrimExcludes trimExcludes, ScriptContext scriptContext, PrintWriter printWriter) {
		ArrayList<AttributesNode> keptAttributes = new ArrayList<>(this.attributes.length);

		for (AttributesNode attributeNode : this.attributes) {
			if (attributeNode instanceof AnnotationContainer) {
				if (!((AnnotationContainer)attributeNode).trimExcludedAnnotations(trimExcludes, scriptContext, printWriter)) {
					keptAttributes.add(attributeNode);
				}
			} else {
				keptAttributes.add(attributeNode);
			}
		}

		if (keptAttributes.size() < this.attributes.length) {
			this.attributes = keptAttributes.toArray(new AttributesNode[keptAttributes.size()]);
		}
	}

	public void resolveComponentTargets(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		AbstractClassNode classNode = this.getClassNode();
		String descriptor = this.descriptorWriter.getValue();
		this.resolvedField = classNode.findField(this.nameWriter.getValue(), descriptor);
		if (this.resolvedField == null) {
			AbstractFieldNode[] fields = classNode.getFields();
			ArrayList<AbstractFieldNode> matchingFields = new ArrayList<>();

			for (AbstractFieldNode field : fields) {
				if (field.getDescriptor().equals(descriptor)) {
					matchingFields.add(field);
				}
			}

			if (matchingFields.size() == 1) {
				this.resolvedField = matchingFields.get(0);
			}
		}

		String accessorDescriptor = "()" + descriptor;
		MethodSignature accessorSignature = new MethodSignature(this.nameWriter.getValue(), accessorDescriptor);
		this.resolvedAccessor = classNode.findMethod(accessorSignature);
		if (this.resolvedAccessor == null) {
			AbstractMethodNode[] methods = classNode.getMethods();
			ArrayList<AbstractMethodNode> matchingMethods = new ArrayList<>();

			for (AbstractMethodNode method : methods) {
				if (method.getDescriptor().equals(accessorDescriptor)
					&& (!method.getNameDisplay().equals("hashCode") || !method.getDescriptor().equals("()I"))
					&& (!method.getNameDisplay().equals("toString") || !method.getDescriptor().equals("()Ljava/lang/String;"))) {
					matchingMethods.add(method);
				}
			}

			if (matchingMethods.size() == 1) {
				this.resolvedAccessor = matchingMethods.get(0);
			}
		}

		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).resolveAnnotations(workspace, classProvider, phantomClassManager, scriptContext);
			}
		}
	}

	public void syncNameAndConstants() {
		if (this.isParsedSuccessfully) {
			if (this.resolvedField != null) {
				String fieldName = this.resolvedField.getNameDisplay();
				if (!this.nameWriter.getValue().equals(fieldName)) {
					this.nameWriter.setValue(fieldName);
				}
			}

			for (AttributesNode attributeNode : this.attributes) {
				if (attributeNode instanceof AnnotationContainer) {
					((AnnotationContainer)attributeNode).updateConstants();
				}
			}
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.nameWriter == oldReference) {
			this.nameWriter = newReference;
		} else if (this.descriptorWriter == oldReference) {
			this.descriptorWriter = newReference;
		}
	}

	public void removeSignatureAttributes() {
		ArrayList<AttributesNode> keptAttributes = new ArrayList<>(this.attributes.length);

		for (AttributesNode attributeNode : this.attributes) {
			if (!(attributeNode instanceof SignatureAttribute)) {
				keptAttributes.add(attributeNode);
			}
		}

		if (keptAttributes.size() < this.attributes.length) {
			this.attributes = keptAttributes.toArray(new AttributesNode[keptAttributes.size()]);
		}
	}
}

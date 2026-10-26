package com.zelix.framework;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.annotation.AnnotationContainer;
import com.zelix.framework.annotation.AnnotationNode;
import com.zelix.framework.attributes.DeprecatedAttribute;
import com.zelix.framework.attributes.SyntheticAttribute;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.base.AbstractAnnotationsAttributeNode;
import com.zelix.framework.attributes.base.AbstractParameterAnnotationsAttributeNode;
import com.zelix.framework.attributes.declaration.SignatureAttribute;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.listener.ReferenceSetContainer;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public abstract class MemberNode extends StructureNode implements SimpleReferenceListener, Comparable<MemberNode> {
	public AccessNode accessFlagsNode;
	public UTFStringWriter nameWriter;
	public UTFStringWriter descriptorWriter;
	public int attributeCount;
	public AttributesNode[] attributes;
	public final String originalName;
	public final String originalDescriptor;
	public int generationTypeFlag;
	private int internalStateFlags;

	public MemberNode(AbstractClassNode owner, UTFStringWriter nameWriter, UTFStringWriter descriptorWriter, AttributesNode[] attributes, int generationTypeFlag) {
		super(owner);
		this.accessFlagsNode = new AccessNode(this);
		this.nameWriter = nameWriter;
		this.descriptorWriter = descriptorWriter;
		this.attributeCount = attributes.length;
		this.attributes = attributes;
		this.originalName = nameWriter.getValue();
		this.originalDescriptor = descriptorWriter.getValue();
		this.generationTypeFlag = generationTypeFlag;

		if (false) {
			if (generationTypeFlag == 3) {
				System.out.println();
			}

			System.out.println("special MemberNode -> " + owner.getClassName() + " " + nameWriter.getValue() + " " + descriptorWriter.getValue() + " ,num=" + generationTypeFlag);
		}
	}

	public MemberNode(StructureNode parent, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		this(parent, buf, utf8References, (PrintWriter)null);
		this.generationTypeFlag = 0;
	}

	public MemberNode(StructureNode parent, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, PrintWriter printWriter) throws IOException {
		super(parent);
		this.accessFlagsNode = new AccessNode(this, buf);
		int nameIndex = buf.readUnsignedShort();
		int descriptorIndex = buf.readUnsignedShort();
		TypeWriter nameTypeWriter = this.getConstantPoolEntry(nameIndex);
		TypeWriter descriptorTypeWriter = this.getConstantPoolEntry(descriptorIndex);
		this.attributeCount = buf.readUnsignedShort();
		this.setNameAndDescriptorWriters(nameTypeWriter, descriptorTypeWriter, printWriter);
		if (utf8References != null) {
			utf8References.put(this.nameWriter, this);
			utf8References.put(this.descriptorWriter, this);
		}

		this.originalName = this.nameWriter.getValue();
		this.originalDescriptor = this.descriptorWriter.getValue();
		this.generationTypeFlag = 0;
	}

	public void setAccessFlags(int accessFlags) {
		this.accessFlagsNode.setAccessFlags(accessFlags);
		this.markChanged();
		this.notify(new AtomicInt(1), this, null);
	}

	public String getNameLowercase() {
		return this.nameWriter.getValue().toLowerCase();
	}

	public final ReferenceSetContainer getAllReferences() {
		ReferenceSetContainer allReferences = new ReferenceSetContainer();
		this.collectReferences(allReferences.getClassReferenceSet(), allReferences.getSignatureClassReferenceSet(), allReferences.getFieldReferenceSet(), allReferences.getMethodReferenceSet());
		return allReferences;
	}

	public final boolean isSynchronized() {
		return this.accessFlagsNode.isSynchronized();
	}

	public Enumeration<String> getAnnotationTypeNames() {
		Set<String> annotationTypeNames = this.getAnnotationTypeNameSet();
		return (Enumeration<String>)(annotationTypeNames.size() == 0 ? new EmptyEnumeration<>() : Collections.enumeration(annotationTypeNames));
	}

	public final boolean isDefaultAccess() {
		return this.accessFlagsNode.isDefaultAccess();
	}

	public final boolean isNameChanged() {
		return !this.originalName.equals(this.nameWriter.getValue());
	}

	public void collectAttributeReferences(AttributesNode attribute, Set<ClassNode> referencedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> referencedFields, Set<MethodNode> referencedMethods) {
	}

	public final boolean isProtected() {
		return this.accessFlagsNode.isProtected();
	}

	public String getAccessFlagsString() {
		return this.accessFlagsNode.getAccessFlagsString();
	}

	public final String getOriginalDescriptor() {
		return this.originalDescriptor;
	}

	public final int getAccessRank() {
		return this.accessFlagsNode.getAccessRank();
	}

	public void updateAttributeIndexes() {
	}

	public final void makeProtected() {
		this.accessFlagsNode.makeProtected();
	}

	public final int getAccessFlags() {
		return this.accessFlagsNode.getAccessFlags();
	}

	public final boolean hasAccessFlag(int flag) {
		return (this.accessFlagsNode.getAccessFlags() & flag) == flag;
	}

	public void setGenerationType(int generationType) {
		this.generationTypeFlag = generationType;
	}

	public final boolean isVolatile() {
		return this.accessFlagsNode.isVolatile();
	}

	public boolean isGeneratedMember() {
		return this.generationTypeFlag != 0;
	}

	public final void setDefaultAccess() {
		this.accessFlagsNode.setDefaultAccess();
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.accessFlagsNode.acceptVisitor(visitor);
		this.nameWriter.accept(visitor, this, this.getParent());
		this.descriptorWriter.accept(visitor, this, this.getParent());
		AttributesNode[] attributeNodes = this.attributes;
		int attributesLength = attributeNodes.length;

		for (int i = 0; i < attributesLength; i++) {
			attributeNodes[i].acceptVisitor(visitor);
		}
	}

	public Set<String> getAnnotationTypeNameSet() {
		HashSet<String> annotationTypeNames = GlobalUtils.newHashSet(13);

		for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AbstractAnnotationsAttributeNode) {
				String[] typeNames = ((AbstractAnnotationsAttributeNode)this.attributes[attributeIndex]).getAnnotationTypeNames();

				for (int typeNameIndex = 0; typeNameIndex < typeNames.length; typeNameIndex++) {
					annotationTypeNames.add(typeNames[typeNameIndex]);
				}
			}
		}

		return annotationTypeNames;
	}

	public void syncAnnotationNames() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).syncNames();
			}
		}
	}

	public final int compareToByName(MemberNode other) {
		return this.getNameLowercase().compareTo(other.getNameLowercase());
	}

	public final boolean isStatic() {
		return this.accessFlagsNode.isStatic();
	}

	public String getOwnerInternalName() {
		return ((AbstractClassNode)this.getParent()).getInternalName();
	}

	public final boolean isTransient() {
		return this.accessFlagsNode.isTransient();
	}

	public final boolean isAbstract() {
		return this.accessFlagsNode.isAbstract();
	}

	public final void setSynthetic(boolean flag) {
		this.accessFlagsNode.setSynthetic(flag);
	}

	public String getOriginalName() {
		return this.originalName;
	}

	public final void setValid(boolean flag) {
		if (flag) {
			this.clearInternalStateFlag(1);
		} else {
			this.setInternalStateFlag(1);
		}
	}

	public String getName() {
		return this.getNameDisplay();
	}

	public String getAnnotationsAsString(ClassHierarchy classHierarchy) {
		StringBuffer buffer = new StringBuffer();
		Enumeration<String> annotationTypeNameEnumeration = this.getAnnotationTypeNames();

		while (annotationTypeNameEnumeration.hasMoreElements()) {
			buffer.append('@');
			String annotationTypeName = annotationTypeNameEnumeration.nextElement();
			String binaryName = classHierarchy.getBinaryName(annotationTypeName);
			buffer.append(GlobalUtils.internalToBinaryName(binaryName));
			buffer.append(" ");
		}

		return buffer.toString();
	}

	public final boolean isPrivate() {
		return this.accessFlagsNode.isPrivate() && !this.accessFlagsNode.isProtected();
	}

	public void collectAnnotationReferences(Set<AbstractClassNode> referencedClasses) {
		for (int i = 0; i < this.attributeCount; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).collectClassReferences(referencedClasses);
			}
		}
	}

	public String getNameDisplay() {
		return this.nameWriter.getValue();
	}

	public final boolean isNative() {
		return this.accessFlagsNode.isNative();
	}

	public void setDescriptor(String descriptor) {
		this.descriptorWriter.setValue(descriptor);
		this.markChanged();
		this.notify(new AtomicInt(2), this, null);
	}

	public boolean isBridge() {
		if (this.getClassNode().isJava5OrNewer()) {
			return this.accessFlagsNode.isBridge();
		} else {
			for (int i = 0; i < this.attributeCount; i++) {
				if (this.attributes[i].getAttributeName().equals("Bridge")) {
					return true;
				}
			}

			return false;
		}
	}

	public final void remapDescriptor(HashMap<String, String> classNameMap) {
		String descriptor = this.getDescriptor();
		String remappedDescriptor = TypeWriter.remapDescriptor(descriptor, classNameMap);
		if (!remappedDescriptor.equals(descriptor)) {
			this.setDescriptor(remappedDescriptor);
		}
	}

	public void filterAttributes(TrimExcludes trimExcludes, TrimConfig trimConfig, ScriptContext scriptContext, PrintWriter printWriter) {
		ArrayList<AttributesNode> keptAttributes = new ArrayList<>();

		for (int i = 0; i < this.attributeCount; i++) {
			if (this.attributes[i] instanceof DeprecatedAttribute) {
				if (!trimConfig.deleteDeprecatedAttributes) {
					keptAttributes.add(this.attributes[i]);
				}
			} else if (this.attributes[i] instanceof AnnotationContainer) {
				AnnotationContainer annotationContainer = (AnnotationContainer)this.attributes[i];
				if (trimConfig.deleteAnnotationAttributes) {
					if (trimExcludes.hasAnnotationTypeExclusions() && !annotationContainer.trimExcludedAnnotations(trimExcludes, scriptContext, printWriter)) {
						keptAttributes.add(this.attributes[i]);
					}
				} else {
					keptAttributes.add(this.attributes[i]);
				}
			} else if (this.attributes[i] instanceof UnknownAttribute) {
				if (trimConfig.deleteUnknownAttributes) {
					UnknownAttribute unknownAttribute = (UnknownAttribute)this.attributes[i];
					printWriter.println(
						"\tDeleting unknown attribute '"
							+ unknownAttribute.getAttributeName()
							+ "' in '"
							+ this.getDisplayName()
							+ "' in class "
							+ ExcludeStatementManager.formatClassForDisplay(this.getClassNode(), trimExcludes)
					);
					if (scriptContext.isVerbose()) {
						scriptContext.getWriter()
							.println(
								"\tDeleting unknown attribute '"
									+ unknownAttribute.getAttributeName()
									+ "' in '"
									+ this.getDisplayName()
									+ "' in class "
									+ ExcludeStatementManager.formatClassForDisplay(this.getClassNode(), trimExcludes)
							);
					}
				} else {
					keptAttributes.add(this.attributes[i]);
				}
			} else {
				this.filterCodeAttribute(trimExcludes, trimConfig, this.attributes[i], scriptContext, printWriter);
				keptAttributes.add(this.attributes[i]);
			}
		}

		if (keptAttributes.size() < this.attributeCount) {
			this.attributes = keptAttributes.toArray(new AttributesNode[keptAttributes.size()]);
			this.attributeCount = this.attributes.length;
			this.updateAttributeIndexes();
		}
	}

	public boolean isDynamicallyGenerated() {
		return this.generationTypeFlag == 3;
	}

	public void stripSyntheticAttributes() {
		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributeCount);
		if (!this.isBridge()) {
			if (this.accessFlagsNode.isSynthetic()) {
				this.setSynthetic(false);
			}

			for (int i = 0; i < this.attributeCount; i++) {
				if (!(this.attributes[i] instanceof SyntheticAttribute)) {
					retainedAttributes.add(this.attributes[i]);
				}
			}

			int retainedCount = retainedAttributes.size();
			if (retainedCount < this.attributeCount) {
				this.attributes = new AttributesNode[retainedCount];

				for (int retainedIndex = 0; retainedIndex < retainedCount; retainedIndex++) {
					this.attributes[retainedIndex] = retainedAttributes.get(retainedIndex);
				}

				this.attributeCount = retainedCount;
				this.updateAttributeIndexes();
			}
		}
	}

	public void setNameAndDescriptorWriters(TypeWriter nameWriter, TypeWriter descriptorWriter, PrintWriter printWriter) {
		if (nameWriter instanceof UTFStringWriter && descriptorWriter instanceof UTFStringWriter) {
			this.nameWriter = (UTFStringWriter)nameWriter;
			this.descriptorWriter = (UTFStringWriter)descriptorWriter;
		} else {
			this.setValid(false);
			throw new ClassParseFailedException(this.getRawSourcePath() + " : Invalid Method or Field Descriptor (A)");
		}
	}

	public final boolean isDescriptorChanged() {
		return !this.originalDescriptor.equals(this.descriptorWriter.getValue());
	}

	@Override
	public int compareTo(MemberNode other) {
		return this.compareToByName(other);
	}

	public abstract boolean isField();

	public void pruneAnnotations() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).updateConstants();
			}
		}
	}

	@Override
	public final void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.nameWriter == oldReference) {
			this.nameWriter = newReference;
		} else if (this.descriptorWriter == oldReference) {
			this.descriptorWriter = newReference;
		}
	}

	public String getDescriptor() {
		return this.descriptorWriter.getValue();
	}

	public final void writeToStreamWithRemap(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		dataOutputStream.writeShort(this.accessFlagsNode.getAccessFlags());
		UTFStringWriter remappedNameWriter = (UTFStringWriter)writerMap.get(this.nameWriter);
		if (remappedNameWriter != null) {
			dataOutputStream.writeShort(remappedNameWriter.getTag());
		} else {
			dataOutputStream.writeShort(this.nameWriter.getTag());
		}

		UTFStringWriter remappedDescriptorWriter = (UTFStringWriter)writerMap.get(this.descriptorWriter);
		if (remappedDescriptorWriter != null) {
			dataOutputStream.writeShort(remappedDescriptorWriter.getTag());
		} else {
			dataOutputStream.writeShort(this.descriptorWriter.getTag());
		}

		dataOutputStream.writeShort(this.attributeCount);

		for (int i = 0; i < this.attributeCount; i++) {
			if (this.attributes[i] instanceof UnknownAttribute && this.attributes[i].getAttributeLength() > 0) {
				UnknownAttribute unknownAttribute = (UnknownAttribute)this.attributes[i];
				if (unknownAttribute.isSharedByManyClasses()) {
					scriptContext.logWarning(
						"Class members contain unknown attribute '" + unknownAttribute.getAttributeName() + "'. The integrity of this attribute may be affected by obfuscation."
					);
				} else {
					scriptContext.logWarning(
						"Member '"
							+ this.getNameDisplay()
							+ "' in class '"
							+ this.getBinaryName()
							+ "' contains unknown attribute '"
							+ unknownAttribute.getAttributeName()
							+ "'. The integrity of this attribute may be affected by obfuscation. Consider using the Trim function to delete it."
					);
				}
			}

			this.attributes[i].writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		}
	}

	public abstract void filterCodeAttribute(
		TrimExcludes trimExcludes, TrimConfig trimConfig, AttributesNode attribute, ScriptContext scriptContext, PrintWriter printWriter
	);

	public final void setFinal(boolean flag) {
		this.accessFlagsNode.setFinal(flag);
	}

	public boolean isMethod() {
		return !this.isField();
	}

	public void resolveAnnotations(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).resolveAnnotations(workspace, classProvider, phantomClassManager, scriptContext);
			}
		}
	}

	public final void setPublic() {
		this.accessFlagsNode.setPublic();
	}

	public abstract boolean isWorkspaceNode();

	public boolean hasInternalStateFlag(int flag) {
		return (this.internalStateFlags & flag) == flag;
	}

	public final void setStatic(boolean flag) {
		this.accessFlagsNode.setStatic(flag);
	}

	public boolean isGeneratedByControlFlow() {
		return this.generationTypeFlag == 2;
	}

	public final void makePrivate() {
		this.accessFlagsNode.makePrivate();
	}

	public void setInternalStateFlag(int flag) {
		this.internalStateFlags |= flag;
	}

	public final boolean isPublic() {
		return this.accessFlagsNode.isPublic();
	}

	public final boolean isFinal() {
		return this.accessFlagsNode.isFinal();
	}

	public Set<String> findAnnotationValue(String annotationName, String propertyName, boolean useOriginalNames) {
		HashSet<String> annotationValues = GlobalUtils.newHashSet(13);

		for (int i = 0; i < this.attributeCount; i++) {
			if (this.attributes[i] instanceof AbstractAnnotationsAttributeNode || this.attributes[i] instanceof AbstractParameterAnnotationsAttributeNode) {
				AnnotationContainer annotationContainer = (AnnotationContainer)this.attributes[i];
				AnnotationNode annotationNode;
				if (annotationContainer instanceof AbstractAnnotationsAttributeNode) {
					annotationNode = ((AbstractAnnotationsAttributeNode)annotationContainer).getAnnotationByType(annotationName, useOriginalNames);
				} else {
					annotationNode = ((AbstractParameterAnnotationsAttributeNode)annotationContainer).getParameterAnnotation(annotationName);
				}

				if (annotationNode != null) {
					String propertyValue = annotationNode.findParamLiteral(propertyName, useOriginalNames);
					if (propertyValue != null) {
						annotationValues.add(propertyValue);
					}
				}
			}
		}

		return annotationValues;
	}

	public void setName(String name) {
		this.nameWriter.setValue(name);
		this.markChanged();
		this.notify(new AtomicInt(0), this, null);
	}

	public void clearInternalStateFlag(int flag) {
		this.internalStateFlags &= ~flag;
	}

	public final void collectReferences(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences) {
		for (int i = 0; i < this.attributes.length; i++) {
			AttributesNode attribute = this.attributes[i];
			if (attribute instanceof SignatureAttribute) {
				((SignatureAttribute)attribute).collectReferencedClasses(signatureClassReferences);
			} else if (attribute instanceof AnnotationContainer) {
				((AnnotationContainer)attribute).collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			} else {
				this.collectAttributeReferences(attribute, classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}

	public final boolean isValid() {
		return !this.hasInternalStateFlag(1);
	}

	public final void remapTypes(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		String descriptor = this.getDescriptor();
		String remappedDescriptor = TypeWriter.remapDescriptor(descriptor, classNameMap);
		if (!remappedDescriptor.equals(descriptor)) {
			this.setDescriptor(remappedDescriptor);
		}

		for (int i = 0; i < this.attributes.length; i++) {
			this.attributes[i].remapDescriptors(keepInnerClassInfo, keepGenericsInformation, packageRenameMap, classNameMap);
		}
	}

	public abstract String getSignature();

	public abstract String getDisplayName();

	public final boolean isSynthetic() {
		if (this.getClassNode().isJava5OrNewer()) {
			return this.accessFlagsNode.isSynthetic();
		} else {
			for (int i = 0; i < this.attributeCount; i++) {
				if (this.attributes[i].getAttributeName().equals("Synthetic")) {
					return true;
				}
			}

			return false;
		}
	}

	public final String getOriginalNameForSignature() {
		return this.originalName;
	}
}

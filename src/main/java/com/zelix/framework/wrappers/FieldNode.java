package com.zelix.framework.wrappers;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.TypeNode;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.base.AbstractTypeAnnotationsAttributeNode;
import com.zelix.framework.attributes.declaration.ConstantValueAttribute;
import com.zelix.framework.attributes.declaration.SignatureAttribute;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.renamer.field.AbstractFieldNameGenerator;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.Workspace;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FieldNode extends AbstractFieldNode {
	public FieldNode(AbstractClassNode owner, UTFStringWriter nameWriter, UTFStringWriter descriptorWriter, AttributesNode[] attributes, int generationTypeFlag) {
		super(owner, nameWriter, descriptorWriter, attributes, generationTypeFlag);

		for (int i = 0; i < attributes.length; i++) {
			attributes[i].setParent(this);
		}
	}

	public FieldNode(
		StructureNode parent,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter printWriter,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes
	) throws IOException {
		super(parent, buf, utf8References, printWriter);
		this.attributes = new AttributesNode[this.attributeCount];

		for (int i = 0; i < this.attributes.length; i++) {
			this.attributes[i] = AttributesNode.readAttribute(this, buf, utf8References, nameAndTypeReferences, stringReferences, integerReferences, longReferences, classReferences, memberReferences, null, printWriter, null, unknownAttributes);
		}

		if (TypeWriter.descriptorToHumanReadable(this.getDescriptor(), true) == null) {
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid Field Descriptor");
			this.setValid(false);
		} else {
			this.setValid(true);
		}
	}

	public static String toArticleTypeName(String internalTypeName) {
		String simpleTypeName;
		int lastSlashIndex;
		if ((lastSlashIndex = internalTypeName.lastIndexOf("/")) != -1) {
			simpleTypeName = internalTypeName.substring(lastSlashIndex + 1);
		} else {
			simpleTypeName = internalTypeName;
		}

		simpleTypeName = StringUtils.replace(simpleTypeName, "[]", "Array");
		StringBuilder nameBuilder = new StringBuilder();
		char firstChar = Character.toUpperCase(simpleTypeName.charAt(0));
		if (firstChar != 'A' && firstChar != 'E' && firstChar != 'I' && firstChar != 'O' && firstChar != 'U') {
			nameBuilder = nameBuilder.append("a");
		} else {
			nameBuilder = nameBuilder.append("an");
		}

		nameBuilder.append(firstChar + simpleTypeName.substring(1));
		return nameBuilder.toString();
	}

	public boolean isIntLikeType() {
		String fieldDescriptor = this.descriptorWriter.getValue();
		if (fieldDescriptor.length() == 1) {
			switch (fieldDescriptor.charAt(0)) {
				case 'B':
				case 'C':
				case 'I':
				case 'S':
					return true;
				default:
					return false;
			}
		} else {
			return false;
		}
	}

	public void resolveAndApplyFieldRename(
		ClassHierarchyNode classHierarchyNode,
		AbstractFieldNameGenerator dictionaryGenerator,
		DualKeyMap<String, FieldSignature, FieldSignature> oldToNewSignatures,
		DualKeyMap<String, FieldSignature, FieldSignature> newToOldSignatures,
		Map<String, FieldNode> groupFieldNames,
		MultiMap<String, String> existingNamesByDescriptor,
		MultiMap<String, String> assignedNamesByDescriptor,
		Map<String, FieldNode> classFieldNames,
		Map<FieldNode, String> keptFieldNames,
		String internalClassName,
		DefaultExcludes defaultExcludes,
		Map<AbstractFieldNode, String> changeLogFieldNames,
		HashMap<AbstractFieldNode, String> resolvedFieldNames,
		HashMap<String, String> mappedClassNames,
		ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph,
		DualKeyMap<AbstractClassNode, FieldNode, AbstractClassNode> referencingClassByField,
		Map<FieldNode, String> accessorFieldNames
	) {
		String oldFieldName = this.getNameDisplay();
		String readableType = this.getReadableType();
		String newFieldName;
		if (defaultExcludes.isFieldExcluded(this)) {
			newFieldName = oldFieldName;
		} else if (keptFieldNames != null && keptFieldNames.containsKey(this)) {
			newFieldName = keptFieldNames.get(this);
		} else if (accessorFieldNames.containsKey(this)) {
			newFieldName = accessorFieldNames.get(this);
			groupFieldNames.put(newFieldName, this);
			classFieldNames.put(newFieldName, this);
			assignedNamesByDescriptor.put(this.getStaticDescriptorKey(), newFieldName);
		} else {
			String staticDescriptorKey = this.getStaticDescriptorKey();
			newFieldName = dictionaryGenerator.generateFieldName(classHierarchyNode, this, oldFieldName, readableType, staticDescriptorKey, groupFieldNames, existingNamesByDescriptor, assignedNamesByDescriptor, classFieldNames, this.isSynthetic(), changeLogFieldNames, resolvedFieldNames, referenceGraph, referencingClassByField);
		}

		if (newFieldName == null) {
			newFieldName = oldFieldName;
		}

		FieldSignature oldSignature = new FieldSignature(oldFieldName, this.getDescriptor());
		FieldSignature newSignature = new FieldSignature(newFieldName, this.getDescriptor());
		FieldSignature previousNewSignature = oldToNewSignatures.put(internalClassName, oldSignature, newSignature);
		FieldSignature previousOldSignature = newToOldSignatures.put(internalClassName, newSignature, oldSignature);
		if (previousOldSignature != null) {
			DebugUtils.assertTrue(
				false,
				new String[]{
					"Duplicate field name in class "
						+ GlobalUtils.internalToBinaryName(GlobalUtils.getMappedOrDefault(internalClassName, mappedClassNames))
						+ ". Both "
						+ oldSignature.getName()
						+ " and "
						+ previousOldSignature.getName()
						+ " renamed to '"
						+ newSignature.getName()
						+ "'."
				}
			);
		}

		resolvedFieldNames.put(this, newFieldName);
		if (!newFieldName.equals(oldFieldName)) {
			this.setName(newFieldName);
		}
	}

	@Override
	public ConstantValueAttribute getConstantValueAttribute() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof ConstantValueAttribute) {
				return (ConstantValueAttribute)this.attributes[i];
			}
		}

		return null;
	}

	public ClassNode getOwnerClass() {
		return (ClassNode)this.getParent();
	}

	public ClassNode getFieldTypeClass() {
		String elementInternalName = AbstractClassNode.getArrayElementInternalName(this.getDescriptor());
		return elementInternalName != null ? ClassHierarchyNode.getWorkspaceClassNode(elementInternalName) : null;
	}

	@Override
	public boolean isString() {
		return this.descriptorWriter.getValue().equals("Ljava/lang/String;");
	}

	public void pruneSignatureMetadata(int keepGenericsInformation) {
		if (keepGenericsInformation == 1) {
			ArrayList<AttributesNode> keptAttributes = new ArrayList<>(this.attributes.length);

			for (int i = 0; i < this.attributes.length; i++) {
				if (!(this.attributes[i] instanceof SignatureAttribute)) {
					if (this.attributes[i] instanceof AbstractTypeAnnotationsAttributeNode) {
						AbstractTypeAnnotationsAttributeNode typeAnnotationsAttribute = (AbstractTypeAnnotationsAttributeNode)this.attributes[i];
						typeAnnotationsAttribute.removeSignatureAnnotations();
						if (!typeAnnotationsAttribute.isEmpty()) {
							keptAttributes.add(this.attributes[i]);
						}
					} else {
						keptAttributes.add(this.attributes[i]);
					}
				}
			}

			if (keptAttributes.size() < this.attributes.length) {
				AttributesNode[] attributesArray = new AttributesNode[keptAttributes.size()];
				this.attributes = keptAttributes.toArray(attributesArray);
				this.attributeCount = this.attributes.length;
			}

			if (this.isEnum()) {
				this.setEnum(false);
			}
		}
	}

	@Override
	public AbstractClassNode getClassNode() {
		return this.getOwnerClass();
	}

	public boolean isLongType() {
		return this.descriptorWriter.getValue().equals("J");
	}

	public void renameWithPrefix(String oldPrefix, String newPrefix, DualKeyMap<String, String, String> renameMap, ClassNode ownerClass, Workspace workspace) {
		String internalName = this.getInternalName();
		String oldName = this.getNameDisplay();
		String unprefixedName = oldName.substring(oldPrefix.length());
		String newName = newPrefix + unprefixedName;
		if (workspace.isResourceRegistered(internalName, newName)) {
			StringBuffer digitBuffer = new StringBuffer();
			if (unprefixedName.length() > 0) {
				for (int charIndex = unprefixedName.length() - 1; charIndex >= 0; charIndex--) {
					char currentChar = unprefixedName.charAt(charIndex);
					if (Character.isDigit(currentChar)) {
						digitBuffer.insert(0, currentChar);
					}
				}
			}

			String baseName = newName.substring(0, newName.length() - digitBuffer.length());
			int numericSuffix = -1;
			if (digitBuffer.length() > 0) {
				try {
					numericSuffix = Integer.parseInt(digitBuffer.toString());
				} catch (NumberFormatException ignored) {
				}
			}

			do {
				newName = baseName + ++numericSuffix;
			} while (workspace.isResourceRegistered(internalName, newName));
		}

		this.setName(newName);
		workspace.renameFieldInRegistries(ownerClass, this, oldName);
		renameMap.put(internalName, oldName, newName);
	}

	@Override
	public boolean isWorkspaceNode() {
		return true;
	}

	public boolean removeConstantValueAttribute() {
		ConstantValueAttribute constantValueAttribute = null;
		AttributesNode[] remainingAttributes = new AttributesNode[this.attributes.length - 1];
		int writeIndex = 0;

		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof ConstantValueAttribute) {
				constantValueAttribute = (ConstantValueAttribute)this.attributes[i];
			} else if (writeIndex < remainingAttributes.length) {
				remainingAttributes[writeIndex++] = this.attributes[i];
			}
		}

		if (constantValueAttribute != null) {
			this.attributes = remainingAttributes;
			this.attributeCount = this.attributes.length;
			return true;
		} else {
			return false;
		}
	}
}

package com.zelix.tools.renamer.field;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.renamer.naming.NameGenerator;
import com.zelix.utils.IntegerUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.ClassHierarchyNode;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleFieldNameGenerator extends AbstractFieldNameGenerator {
	private String placeholder = "";
	private boolean legalIdentifiers;
	private boolean useAsciiCharactersOnly;
	private String baseName;
	private boolean allowOverwrite;
	private List<String> reservedNames;
	public NameGenerator nameGenerator;

	public SimpleFieldNameGenerator(int renameMode, boolean useAsciiCharactersOnly, boolean legalIdentifiers, boolean randomizeObfuscation, String baseName, boolean allowOverwrite, List<String> reservedNames) {
		super(renameMode, randomizeObfuscation);
		this.legalIdentifiers = legalIdentifiers;
		this.useAsciiCharactersOnly = useAsciiCharactersOnly;
		this.baseName = baseName;
		this.allowOverwrite = allowOverwrite;
		this.reservedNames = reservedNames;
	}

	@Override
	public String generateFieldName(
		ClassHierarchyNode classHierarchyNode,
		FieldNode fieldNode,
		String fieldName,
		String readableType,
		String descriptorKey,
		Map<String, FieldNode> groupFieldNames,
		MultiMap<String, String> existingNamesByDescriptor,
		MultiMap<String, String> assignedNamesByDescriptor,
		Map<String, FieldNode> classFieldNames,
		boolean synthetic,
		Map<AbstractFieldNode, String> changeLogFieldNames,
		HashMap<AbstractFieldNode, String> resolvedFieldNames,
		ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph,
		DualKeyMap<AbstractClassNode, FieldNode, AbstractClassNode> referencingClassByField
	) {
		if (this.renameMode == 1
			|| ((!fieldName.startsWith("this$") || !synthetic) && !fieldName.startsWith("this+") || !IntegerUtils.isInteger(fieldName.substring(5)))
				&& (!fieldName.startsWith("val$") || !synthetic)
				&& !fieldName.startsWith("val+")) {
			ClassNode classNode = classHierarchyNode.getClassNodeIfWorkspace();
			List<String> existingNames;
			if (this.allowOverwrite && (existingNames = existingNamesByDescriptor.get(descriptorKey)) != null) {
				for (int i = 0; i < existingNames.size(); i++) {
					String existingName = existingNames.get(i);
					if (existingName != this.placeholder) {
						if (!existingName.equals(fieldName) && this.isNameAvailable(fieldName, existingName, fieldNode, classFieldNames, groupFieldNames, referenceGraph, referencingClassByField, classNode, changeLogFieldNames, resolvedFieldNames)) {
							classFieldNames.put(existingName, fieldNode);
							existingNames.set(i, this.placeholder);
							return existingName;
						}

						existingNames.set(i, this.placeholder);
					}
				}
			}

			boolean showOldNames = Options.ZKM_DEBUG_SHOW_OLD_NAMES;

			String candidateName;
			do {
				candidateName = this.getNextName(classHierarchyNode);
				if (showOldNames) {
					candidateName = fieldName + '_' + candidateName;
				}
			} while (!this.isNameAvailable(fieldName, candidateName, fieldNode, classFieldNames, groupFieldNames, referenceGraph, referencingClassByField, classNode, changeLogFieldNames, resolvedFieldNames));

			groupFieldNames.put(candidateName, fieldNode);
			classFieldNames.put(candidateName, fieldNode);
			assignedNamesByDescriptor.put(descriptorKey, candidateName);
			return candidateName;
		} else {
			groupFieldNames.put(fieldName, fieldNode);
			classFieldNames.put(fieldName, fieldNode);
			return null;
		}
	}

	private String getNextName(ClassHierarchyNode classHierarchyNode) {
		if (classHierarchyNode != super.lastProcessedClass) {
			super.lastProcessedClass = classHierarchyNode;
			if (!this.useAsciiCharactersOnly) {
				this.nameGenerator = new NameGenerator(NameGenerator.UNICODES, NameGenerator.UNICODES, this.reservedNames, super.randomizeObfuscation);
			} else if (this.legalIdentifiers) {
				if (Options.noUnderscoreInFieldNames) {
					this.nameGenerator = new NameGenerator(NameGenerator.LETTERS, NameGenerator.LETTERS_NUMBERS, this.reservedNames, super.randomizeObfuscation);
				} else {
					this.nameGenerator = new NameGenerator(NameGenerator.LETTERS_, NameGenerator.LETTERS_NUMBERS_, this.reservedNames, super.randomizeObfuscation);
				}
			} else {
				this.nameGenerator = new NameGenerator(NameGenerator.NUMBERS, NameGenerator.LETTERS_NUMBERS_, this.reservedNames, super.randomizeObfuscation);
			}
		}

		return this.nameGenerator.next(this.baseName);
	}

	private boolean isNameAvailable(
		String currentName,
		String candidateName,
		FieldNode fieldNode,
		Map<String, FieldNode> classFieldNames,
		Map<String, FieldNode> groupFieldNames,
		ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph,
		DualKeyMap<AbstractClassNode, FieldNode, AbstractClassNode> referencingClassByField,
		ClassNode classNode,
		Map<AbstractFieldNode, String> changeLogFieldNames,
		HashMap<AbstractFieldNode, String> resolvedFieldNames
	) {
		if (currentName.equals(candidateName)) {
			return false;
		} else if (classFieldNames.containsKey(candidateName)) {
			return false;
		} else if (referenceGraph.isReferencedBy(classNode, new FieldSignature(candidateName, fieldNode.getDescriptor()))) {
			return false;
		} else {
			Map<FieldNode, AbstractClassNode> referencingClassesByField = referencingClassByField.getGroup(classNode);
			if (referencingClassesByField != null) {
				for (FieldNode referencingField : referencingClassesByField.keySet()) {
					if (this.isNameConflicting(candidateName, referencingField, changeLogFieldNames, resolvedFieldNames)) {
						return false;
					}
				}
			}

			Enumeration<AbstractFieldNode> referencedFieldNodes = classNode.getReferencedFieldNodes();
			if (referencedFieldNodes != null) {
				while (referencedFieldNodes.hasMoreElements()) {
					AbstractFieldNode referencedField = referencedFieldNodes.nextElement();
					if (this.isNameConflicting(candidateName, referencedField, changeLogFieldNames, resolvedFieldNames)) {
						return false;
					}
				}
			}

			return !groupFieldNames.containsKey(candidateName) || this.allowOverwrite;
		}
	}

	private boolean isNameConflicting(String candidateName, AbstractFieldNode otherField, Map<AbstractFieldNode, String> changeLogFieldNames, HashMap<AbstractFieldNode, String> resolvedFieldNames) {
		return changeLogFieldNames != null && candidateName.equals(changeLogFieldNames.get(otherField)) || candidateName.equals(resolvedFieldNames.get(otherField));
	}
}

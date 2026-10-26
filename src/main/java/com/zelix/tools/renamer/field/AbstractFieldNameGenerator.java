package com.zelix.tools.renamer.field;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.ClassHierarchyNode;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFieldNameGenerator {
	public int renameMode;
	public boolean randomizeObfuscation;
	public ClassHierarchyNode lastProcessedClass;

	public AbstractFieldNameGenerator(int renameMode, boolean randomizeObfuscation) {
		this.renameMode = renameMode;
		this.randomizeObfuscation = randomizeObfuscation;
	}

	public abstract String generateFieldName(
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
	);
}

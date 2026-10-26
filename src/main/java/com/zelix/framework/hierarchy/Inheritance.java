package com.zelix.framework.hierarchy;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class Inheritance {
	private final Object fieldLock = new Object();
	private final Object methodLock = new Object();
	private final Set<AbstractClassNode> allClasses;
	private final AbstractClassNode[] classArray;
	private final ClassProvider classProvider;
	private final Set<AbstractFieldNode> interfaceFields;
	private final Set<AbstractMethodNode> interfaceMethods;
	private final ReferenceGraph<AbstractClassNode, AbstractFieldNode> fieldReferenceGraph;
	private final ReferenceGraph<AbstractClassNode, AbstractMethodNode> methodReferenceGraph;
	private final ReferenceGraph<AbstractClassNode, AbstractClassNode> classReferenceGraph;

	public Inheritance(AbstractClassNode[] classNodes, ClassProvider classProvider) {
		int classCount = classNodes.length;
		this.allClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(classCount));

		for (AbstractClassNode classNode : classNodes) {
			this.allClasses.add(classNode);
			if (classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
					this.allClasses.add(innerClass);
				}
			}
		}

		this.classArray = this.allClasses.toArray(new AbstractClassNode[this.allClasses.size()]);
		classCount = this.classArray.length;
		this.classProvider = classProvider;
		this.interfaceFields = GlobalUtils.newHashSet();
		this.interfaceMethods = GlobalUtils.newHashSet();
		ReferenceGraph<AbstractClassNode, AbstractClassNode> classGraph = new ReferenceGraph<>(classCount, true);
		ReferenceGraph<AbstractClassNode, AbstractFieldNode> fieldGraph = new ReferenceGraph<>(classCount, true);
		ReferenceGraph<AbstractClassNode, AbstractMethodNode> methodGraph = new ReferenceGraph<>(classCount, true);
		this.analyzeHierarchies(classGraph, fieldGraph, methodGraph);
		this.fieldReferenceGraph = new ReferenceGraph<>(classCount, true);
		this.methodReferenceGraph = new ReferenceGraph<>(classCount, true);
		this.classReferenceGraph = new ReferenceGraph<>(classCount, true);
		this.buildMemberGraphs(classGraph, fieldGraph, methodGraph);
	}

	public Set<FieldSignature> getFieldSignatures(AbstractClassNode classNode) {
		Set<AbstractFieldNode> fieldNodes = this.fieldReferenceGraph.getReferences(classNode);
		if (fieldNodes == null) {
			return GlobalUtils.newHashSet(13);
		} else {
			HashSet<FieldSignature> fieldSignatures = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(fieldNodes.size()));

			for (AbstractFieldNode field : fieldNodes) {
				fieldSignatures.add(field.getFieldSignature());
			}

			return fieldSignatures;
		}
	}

	public Set<String> getFieldNames(AbstractClassNode classNode) {
		Set<AbstractFieldNode> fieldNodes = this.fieldReferenceGraph.getReferences(classNode);
		if (fieldNodes == null) {
			return GlobalUtils.newHashSet(13);
		} else {
			HashSet<String> fieldNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(fieldNodes.size()));

			for (AbstractFieldNode field : fieldNodes) {
				fieldNames.add(field.getNameDisplay());
			}

			return fieldNames;
		}
	}

	private void buildMemberGraphs(
		ReferenceGraph<AbstractClassNode, AbstractClassNode> classGraph,
		ReferenceGraph<AbstractClassNode, AbstractFieldNode> fieldGraph,
		ReferenceGraph<AbstractClassNode, AbstractMethodNode> methodGraph
	) {
		ReferenceGraph<AbstractClassNode, AbstractClassNode> descendantGraph = new ReferenceGraph<>(this.allClasses.size());

		for (Entry<AbstractClassNode, Set<AbstractClassNode>> ancestorEntry : classGraph.getAdjacencyEntries()) {
			AbstractClassNode classNode = ancestorEntry.getKey();

			for (AbstractClassNode ancestorClass : ancestorEntry.getValue()) {
				descendantGraph.addReference(ancestorClass, classNode);
			}
		}

		HashSet<AbstractClassNode> leafClasses = GlobalUtils.newHashSet();

		for (AbstractClassNode leafCandidate : this.allClasses) {
			if (!descendantGraph.hasMember(leafCandidate)) {
				leafClasses.add(leafCandidate);
			}
		}

		for (AbstractClassNode leafClass : leafClasses) {
			Set<AbstractClassNode> hierarchyClasses = classGraph.getReferences(leafClass);
			hierarchyClasses.add(leafClass);
			Set<AbstractFieldNode> fieldNodes = fieldGraph.getReferences(leafClass);
			this.fieldReferenceGraph.addReferences(leafClass, fieldNodes);
			Set<AbstractMethodNode> methodNodes = methodGraph.getReferences(leafClass);
			this.methodReferenceGraph.addReferences(leafClass, methodNodes);

			for (AbstractClassNode leafAncestor : classGraph.getReferences(leafClass)) {
				this.fieldReferenceGraph.addReferences(leafAncestor, fieldNodes);
				this.methodReferenceGraph.addReferences(leafAncestor, methodNodes);
				this.classReferenceGraph.addReferences(leafAncestor, hierarchyClasses);
			}
		}
	}

	public Set<MethodParameterKey> getMethodParameterKeys(AbstractClassNode classNode) {
		Set<AbstractMethodNode> methodNodes = this.methodReferenceGraph.getReferences(classNode);
		HashSet<MethodParameterKey> methodParameterKeys = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(methodNodes.size()));

		for (AbstractMethodNode method : methodNodes) {
			methodParameterKeys.add(method.getMethodParameterKey());
		}

		return methodParameterKeys;
	}

	private void analyzeHierarchies(
		ReferenceGraph<AbstractClassNode, AbstractClassNode> classGraph,
		ReferenceGraph<AbstractClassNode, AbstractFieldNode> fieldGraph,
		ReferenceGraph<AbstractClassNode, AbstractMethodNode> methodGraph
	) {
		for (int i = 0; i < this.classArray.length; i++) {
			AbstractClassNode classNode = this.classArray[i];
			String context = "analyzing fields and methods in the inheritance hierarchy of class '" + classNode.getRawSourcePath() + "'";
			this.collectHierarchyMembers(classNode, new ArrayList<>(), classGraph, new ArrayList<>(), fieldGraph, new ArrayList<>(), methodGraph, this.interfaceFields, this.interfaceMethods, context);
		}
	}

	public void registerField(AbstractFieldNode fieldNode) {
		AbstractClassNode classNode = fieldNode.getClassNode();
		if (classNode.isInterface()) {
			this.interfaceFields.add(fieldNode);
		}

		for (AbstractClassNode referencingClass : this.classReferenceGraph.getReferences(classNode)) {
			this.fieldReferenceGraph.addReference(referencingClass, fieldNode);
		}
	}

	public Object getMethodLock() {
		return this.methodLock;
	}

	public Object getFieldLock() {
		return this.fieldLock;
	}

	public Set<MethodSignature> getMethodSignatures(AbstractClassNode classNode) {
		Set<AbstractMethodNode> methodNodes = this.methodReferenceGraph.getReferences(classNode);
		HashSet<MethodSignature> methodSignatures = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(methodNodes.size()));

		for (AbstractMethodNode method : methodNodes) {
			methodSignatures.add(method.getMethodSignature());
		}

		return methodSignatures;
	}

	private void collectHierarchyMembers(
		AbstractClassNode classNode,
		List<Set<AbstractClassNode>> superClassSets,
		ReferenceGraph<AbstractClassNode, AbstractClassNode> classGraph,
		List<Set<AbstractFieldNode>> fieldSets,
		ReferenceGraph<AbstractClassNode, AbstractFieldNode> fieldGraph,
		List<Set<AbstractMethodNode>> methodSets,
		ReferenceGraph<AbstractClassNode, AbstractMethodNode> methodGraph,
		Set<AbstractFieldNode> interfaceFields,
		Set<AbstractMethodNode> interfaceMethods,
		String context
	) {
		Set<AbstractClassNode> registeredAncestors = classGraph.getReferences(classNode);
		if (registeredAncestors == null) {
			HashSet<AbstractClassNode> ancestors = GlobalUtils.newHashSet();
			classGraph.setReferences(classNode, ancestors);
			ArrayList<Set<AbstractClassNode>> pendingAncestorSets = new ArrayList<>(superClassSets);
			pendingAncestorSets.add(ancestors);
			HashSet<AbstractFieldNode> classFields = GlobalUtils.newHashSet();
			fieldGraph.setReferences(classNode, classFields);
			ArrayList<Set<AbstractFieldNode>> pendingFieldSets = new ArrayList<>(fieldSets);
			pendingFieldSets.add(classFields);
			AbstractFieldNode[] classNodeFields = classNode.getFields();

			for (int fieldIndex = 0; fieldIndex < classNodeFields.length; fieldIndex++) {
				AbstractFieldNode ownField = classNodeFields[fieldIndex];

				for (int pendingFieldSetIndex = 0; pendingFieldSetIndex < pendingFieldSets.size(); pendingFieldSetIndex++) {
					Set<AbstractFieldNode> pendingFieldSet = pendingFieldSets.get(pendingFieldSetIndex);
					pendingFieldSet.add(ownField);
				}

				if (classNode.isInterface()) {
					interfaceFields.add(ownField);
				}
			}

			HashSet<AbstractMethodNode> classMethods = GlobalUtils.newHashSet();
			methodGraph.setReferences(classNode, classMethods);
			ArrayList<Set<AbstractMethodNode>> pendingMethodSets = new ArrayList<>(methodSets);
			pendingMethodSets.add(classMethods);
			AbstractMethodNode[] classNodeMethods = classNode.getMethods();

			for (int methodIndex = 0; methodIndex < classNodeMethods.length; methodIndex++) {
				AbstractMethodNode ownMethod = classNodeMethods[methodIndex];
				if (classNode.isInterface()) {
					interfaceMethods.add(ownMethod);
				}

				for (int pendingMethodSetIndex = 0; pendingMethodSetIndex < pendingMethodSets.size(); pendingMethodSetIndex++) {
					Set<AbstractMethodNode> pendingMethodSet = pendingMethodSets.get(pendingMethodSetIndex);
					pendingMethodSet.add(ownMethod);
				}
			}

			if (!classNode.getInternalName().equals("java/lang/Object")) {
				String superclassName = classNode.getSuperclassName();
				Integer innerClassOrdinal = classNode.hasInnerClassOrdinal() ? classNode.getInnerClassOrdinal() : null;
				AbstractClassNode superClassNode = this.classProvider.getClassNode(superclassName, innerClassOrdinal, context);

				for (Set<AbstractClassNode> pendingAncestorSet : pendingAncestorSets) {
					pendingAncestorSet.add(superClassNode);
				}

				this.collectHierarchyMembers(superClassNode, pendingAncestorSets, classGraph, pendingFieldSets, fieldGraph, pendingMethodSets, methodGraph, interfaceFields, interfaceMethods, context);
				String[] interfaceNames = classNode.getInterfaceNames();

				for (int interfaceIndex = 0; interfaceIndex < interfaceNames.length; interfaceIndex++) {
					String interfaceName = interfaceNames[interfaceIndex];
					AbstractClassNode interfaceClassNode = this.classProvider.getClassNode(interfaceName, innerClassOrdinal, context);

					for (Set<AbstractClassNode> interfaceAncestorSet : pendingAncestorSets) {
						interfaceAncestorSet.add(interfaceClassNode);
					}

					this.collectHierarchyMembers(interfaceClassNode, pendingAncestorSets, classGraph, pendingFieldSets, fieldGraph, pendingMethodSets, methodGraph, interfaceFields, interfaceMethods, context);
				}
			}
		} else {
			for (AbstractClassNode ancestorClass : registeredAncestors) {
				for (Set<AbstractClassNode> superClassSet : superClassSets) {
					superClassSet.add(ancestorClass);
				}
			}

			for (AbstractFieldNode field : fieldGraph.getReferences(classNode)) {
				for (int fieldSetIndex = 0; fieldSetIndex < fieldSets.size(); fieldSetIndex++) {
					Set<AbstractFieldNode> fieldSet = fieldSets.get(fieldSetIndex);
					fieldSet.add(field);
				}
			}

			for (AbstractMethodNode method : methodGraph.getReferences(classNode)) {
				for (int methodSetIndex = 0; methodSetIndex < methodSets.size(); methodSetIndex++) {
					Set<AbstractMethodNode> methodSet = methodSets.get(methodSetIndex);
					methodSet.add(method);
				}
			}
		}
	}

	public void registerMethod(AbstractMethodNode methodNode) {
		AbstractClassNode classNode = methodNode.getClassNode();
		if (classNode.isInterface()) {
			this.interfaceMethods.add(methodNode);
		}

		for (AbstractClassNode referencingClass : this.classReferenceGraph.getReferences(classNode)) {
			this.methodReferenceGraph.addReference(referencingClass, methodNode);
		}
	}
}

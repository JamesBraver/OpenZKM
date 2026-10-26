package com.zelix.tools.excludes.base;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.Workspace;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractClassExcludeManager extends ExcludeStatementManager {
	protected Map<MethodNode, ClassNode> excludedMethods;
	protected Map<FieldNode, ClassNode> excludedFields;
	protected Map<ClassNode, ClassNode> processableClasses;
	protected Map<FieldNode, ClassNode> processableFields;
	protected Map<MethodNode, ClassNode> processableMethods;
	protected Map<ClassNode, ClassNode> excludedClasses;

	public AbstractClassExcludeManager(Workspace workspace, List<? extends ExclusionInclusionStatement> fixedClassesStatements, ScriptContext scriptContext) {
		super(workspace, fixedClassesStatements, scriptContext);
	}

	public final boolean isClassProcessable(ClassNode classNode) {
		return this.processableClasses.containsKey(classNode);
	}

	public Enumeration<ClassNode> getWorkspaceClasses() {
		return this.workspace.getClasses();
	}

	public final boolean isClassNameExcluded(String internalName) {
		ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(internalName);
		if (hierarchyNode != null) {
			ClassNode classNode = hierarchyNode.getClassNodeIfWorkspace();
			if (classNode != null) {
				return this.excludedClasses.containsKey(classNode);
			}
		}

		return false;
	}

	public boolean hasNoProcessableFields() {
		return this.processableFields.size() == 0;
	}

	public final Enumeration<ClassNode> getExcludedClasses() {
		return ArrayEnumeration.newCollectionEnumeration(this.excludedClasses.keySet());
	}

	public abstract boolean excludeClass(ClassNode classNode, String reason);

	public final Enumeration<ClassNode> getClassesByMethodName(String methodName) {
		return this.workspace.getClassesByMethodName(methodName);
	}

	public final boolean isClassOrInnerClassExcluded(ClassNode classNode) {
		if (classNode == null) {
			return false;
		} else {
			boolean excluded = this.excludedClasses.containsKey(classNode);
			if (!excluded && classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
					excluded = this.excludedClasses.containsKey(innerClass);
					if (excluded) {
						break;
					}
				}
			}

			return excluded;
		}
	}

	public boolean hasNoProcessableMethods() {
		return this.processableMethods.size() == 0;
	}

	public boolean isFieldExcluded(FieldNode fieldNode) {
		return this.excludedFields.containsKey(fieldNode);
	}

	public Enumeration<MethodNode> getExcludedMethods() {
		MethodNode[] excludedMethodArray = new MethodNode[this.excludedMethods.size()];
		Iterator<MethodNode> methodIterator = this.excludedMethods.keySet().iterator();
		int i = 0;

		while (methodIterator.hasNext()) {
			excludedMethodArray[i++] = methodIterator.next();
		}

		return new ArrayEnumeration<>(excludedMethodArray);
	}

	public Enumeration<MethodNode> getProcessableMethods() {
		MethodNode[] processableMethodArray = new MethodNode[this.processableMethods.size()];
		Iterator<MethodNode> methodIterator = this.processableMethods.keySet().iterator();
		int i = 0;

		while (methodIterator.hasNext()) {
			processableMethodArray[i++] = methodIterator.next();
		}

		return new ArrayEnumeration<>(processableMethodArray);
	}

	public final boolean isClassExcluded(ClassNode classNode) {
		return this.excludedClasses.containsKey(classNode);
	}

	public final boolean hasNoExcludedClasses() {
		return this.excludedClasses.size() == 0;
	}

	public Enumeration<MethodNode> getAllMethods() {
		HashSet<MethodNode> allMethods = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.excludedMethods.size() + this.processableMethods.size()));
		allMethods.addAll(this.excludedMethods.keySet());
		allMethods.addAll(this.processableMethods.keySet());
		return ArrayEnumeration.newCollectionEnumeration(allMethods);
	}

	public Enumeration<FieldNode> getExcludedFields() {
		FieldNode[] excludedFieldArray = new FieldNode[this.excludedFields.size()];
		Iterator<FieldNode> fieldIterator = this.excludedFields.keySet().iterator();
		int i = 0;

		while (fieldIterator.hasNext()) {
			excludedFieldArray[i++] = fieldIterator.next();
		}

		return new ArrayEnumeration<>(excludedFieldArray);
	}

	public final Enumeration<ClassNode> getProcessableClasses() {
		return ArrayEnumeration.newCollectionEnumeration(this.processableClasses.keySet());
	}

	public final boolean hasNoProcessableClasses() {
		return this.processableClasses.size() == 0;
	}

	public boolean isMethodExcluded(MethodNode methodNode) {
		return this.excludedMethods.containsKey(methodNode);
	}

	public Enumeration<FieldNode> getAllFields() {
		HashSet<FieldNode> allFields = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.excludedFields.size() + this.processableFields.size()));
		allFields.addAll(this.excludedFields.keySet());
		allFields.addAll(this.processableFields.keySet());
		return ArrayEnumeration.newCollectionEnumeration(allFields);
	}

	public Enumeration<FieldNode> getProcessableFields() {
		FieldNode[] processableFieldArray = new FieldNode[this.processableFields.size()];
		Iterator<FieldNode> fieldIterator = this.processableFields.keySet().iterator();
		int i = 0;

		while (fieldIterator.hasNext()) {
			processableFieldArray[i++] = fieldIterator.next();
		}

		return new ArrayEnumeration<>(processableFieldArray);
	}

	public Enumeration<ClassNode> getClassesByFieldName(String fieldName) {
		return this.workspace.getClassesByFieldName(fieldName);
	}
}

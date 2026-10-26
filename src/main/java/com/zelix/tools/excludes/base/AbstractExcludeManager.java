package com.zelix.tools.excludes.base;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.Workspace;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

public abstract class AbstractExcludeManager extends ExcludeStatementManager {
	protected HashSet<AbstractMethodNode> includedMethods;
	protected HashSet<AbstractFieldNode> includedFields;
	protected HashSet<AbstractClassNode> includedClasses;
	protected HashSet<AbstractClassNode> excludedClasses;
	protected HashSet<AbstractFieldNode> excludedFields;
	protected HashSet<AbstractMethodNode> excludedMethods;

	public AbstractExcludeManager(Workspace workspace, List<? extends ExclusionInclusionStatement> fixedClassesStatements, ScriptContext scriptContext) {
		super(workspace, fixedClassesStatements, scriptContext);
	}

	public Enumeration<AbstractFieldNode> getIncludedFields() {
		return ArrayEnumeration.newCollectionEnumeration(this.includedFields);
	}

	public Enumeration<AbstractFieldNode> getExcludedFields() {
		return ArrayEnumeration.newCollectionEnumeration(this.excludedFields);
	}

	public Enumeration<AbstractMethodNode> getExcludedMethods() {
		return ArrayEnumeration.newCollectionEnumeration(this.excludedMethods);
	}

	public abstract Enumeration<AbstractClassNode> getAnnotationSearchClasses();

	public boolean hasNoIncludedMethods() {
		return this.includedMethods.size() == 0;
	}

	public boolean hasNoIncludedFields() {
		return this.includedFields.size() == 0;
	}

	public Enumeration<AbstractMethodNode> getIncludedMethods() {
		return ArrayEnumeration.newCollectionEnumeration(this.includedMethods);
	}

	public final boolean hasNoExcludedClasses() {
		return this.excludedClasses.size() == 0;
	}
}

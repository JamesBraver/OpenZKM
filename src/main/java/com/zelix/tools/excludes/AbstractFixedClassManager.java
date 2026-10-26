package com.zelix.tools.excludes;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.tools.excludes.base.AbstractClassExcludeManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.workspace.Workspace;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public abstract class AbstractFixedClassManager extends AbstractClassExcludeManager {
	private final Set<ClassNode> fixedClasses = GlobalUtils.newHashSet();

	public AbstractFixedClassManager(Workspace workspace, List<? extends ExclusionInclusionStatement> fixedClassesStatements, ScriptContext scriptContext) {
		super(workspace, fixedClassesStatements, scriptContext);
	}

	public boolean isInFixedClasses(ClassNode klass) {
		return this.fixedClasses.contains(klass);
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		this.fixedClasses.add(classNode);
		boolean classRemoved = this.removeProcessableClass(classNode, classNode);
		if (super.scriptContext.isVerbose()) {
			super.logWriter.println("\tMatching class \"" + this.formatClassForDisplay(classNode) + "\" as fixed because of \"" + reason + "\"");
		}

		return classRemoved;
	}

	public Enumeration<ClassNode> enumerateFixedClasses() {
		return Collections.enumeration(this.fixedClasses);
	}

	public boolean removeProcessableClass(ClassNode classKey, ClassNode classValue) {
		Object removedClass = super.processableClasses.remove(classKey);
		if (removedClass != null) {
			super.excludedClasses.put(classKey, classValue);
		}

		return removedClass != null;
	}
}

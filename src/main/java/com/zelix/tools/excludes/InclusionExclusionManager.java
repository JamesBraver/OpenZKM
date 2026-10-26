package com.zelix.tools.excludes;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.tools.excludes.base.AbstractClassExcludeManager;
import com.zelix.workspace.Workspace;
import java.util.List;

public abstract class InclusionExclusionManager extends AbstractClassExcludeManager {
	protected List<? extends ExclusionInclusionStatement> inclusionStatements;

	public InclusionExclusionManager(
		Workspace workspace, List<? extends ExclusionInclusionStatement> excludeStatements, List<? extends ExclusionInclusionStatement> unexcludeStatements, ScriptContext scriptContext
	) {
		super(workspace, excludeStatements, scriptContext);
		this.inclusionStatements = unexcludeStatements;
	}

	public abstract void includeClass(ClassNode classNode, String reason);
}

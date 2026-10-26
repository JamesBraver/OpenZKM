package com.zelix.tools.excludes.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.workspace.Workspace;
import java.util.List;

public abstract class AbstractReferenceExcludeManager extends AbstractExcludeManager {
	protected List<? extends ExclusionInclusionStatement> excludeStatements;

	public AbstractReferenceExcludeManager(
		Workspace workspace, List<? extends ExclusionInclusionStatement> includeStatements, List<? extends ExclusionInclusionStatement> excludeStatements, ScriptContext scriptContext
	) {
		super(workspace, includeStatements, scriptContext);
		this.excludeStatements = excludeStatements;
	}
}

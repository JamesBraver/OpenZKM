package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;

public class DefaultTrimExcludeStatement extends ExclusionInclusionStatement {
	public DefaultTrimExcludeStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "default trim exclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
	}
}

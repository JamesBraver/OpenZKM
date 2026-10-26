package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;

public class DefaultMethodParameterObfuscationExcludeStatement extends ExclusionInclusionStatement {
	public DefaultMethodParameterObfuscationExcludeStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "default method parameter obfuscation exclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
	}
}

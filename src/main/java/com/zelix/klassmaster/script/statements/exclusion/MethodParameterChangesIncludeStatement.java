package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import java.io.PrintWriter;

public class MethodParameterChangesIncludeStatement extends ExclusionInclusionStatement {
	public MethodParameterChangesIncludeStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "methodParameterChangesInclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		PrintWriter writer = scriptContext.getWriter();
		String message = AbstractScriptNode.getTimestampPrefix() + " Setting method parameter changes inclusions...";
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addMethodParameterChangesIncludeStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}

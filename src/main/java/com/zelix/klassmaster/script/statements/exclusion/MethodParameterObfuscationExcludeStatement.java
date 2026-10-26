package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import java.io.PrintWriter;

public class MethodParameterObfuscationExcludeStatement extends MethodParameterChangesExcludeStatement {
	public MethodParameterObfuscationExcludeStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "methodParameterObfuscationExclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		PrintWriter writer = scriptContext.getWriter();
		String message = AbstractScriptNode.getTimestampPrefix() + " Setting method parameter obfuscation exclusions...";
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addMethodParameterObfuscationExcludeStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}

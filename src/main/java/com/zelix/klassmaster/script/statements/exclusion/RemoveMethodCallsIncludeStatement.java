package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.DetailedExclusionStatement;
import java.io.PrintWriter;

public class RemoveMethodCallsIncludeStatement extends DetailedExclusionStatement {
	public RemoveMethodCallsIncludeStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "removeMethodCallsInclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		PrintWriter writer = scriptContext.getWriter();
		String message = AbstractScriptNode.getTimestampPrefix() + " Setting remove method calls inclusions...";
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addRemoveMethodCallsIncludeStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}

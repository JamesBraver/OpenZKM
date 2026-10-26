package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import java.io.PrintWriter;

public class IgnoreMissingReferencesStatement extends ExclusionInclusionStatement {
	public IgnoreMissingReferencesStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "ignoreMissingReferences";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		String message = AbstractScriptNode.getTimestampPrefix() + " Setting references to be ignored...";
		PrintWriter writer = scriptContext.getWriter();
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addIgnoreMissingReferencesStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}

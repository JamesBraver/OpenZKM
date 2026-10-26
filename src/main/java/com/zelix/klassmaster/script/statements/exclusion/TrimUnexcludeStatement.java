package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import java.io.PrintWriter;

public class TrimUnexcludeStatement extends ExclusionInclusionStatement {
	public TrimUnexcludeStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "trimUnexclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		String message = AbstractScriptNode.getTimestampPrefix() + " Setting trim unexclusions...";
		PrintWriter writer = scriptContext.getWriter();
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addTrimUnexcludeStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}

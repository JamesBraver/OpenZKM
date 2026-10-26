package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import java.io.PrintWriter;

public class AccessedByReflectionExcludeStatement extends ExclusionInclusionStatement {
	public AccessedByReflectionExcludeStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "accessedByReflectionExclude";
	}

	@Override
	protected void printStatementMessage(ScriptContext scriptContext) {
		String message = AbstractScriptNode.getTimestampPrefix() + " Excluding from 'accessed by Reflection' set...";
		PrintWriter writer = scriptContext.getWriter();
		writer.println(message);
		System.out.println(message);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addAccessedByReflectionExcludeStatement(this);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}

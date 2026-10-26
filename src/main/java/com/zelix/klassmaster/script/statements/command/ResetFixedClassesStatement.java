package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import java.io.PrintWriter;

public class ResetFixedClassesStatement extends ScriptCommand {
	public ResetFixedClassesStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "resetFixedClasses";
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		this.executeCommand(scriptContext, scriptContext.getWarningCount(), scriptContext.getWarningCount(), scriptContext.getErrorCount());
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		PrintWriter writer = scriptContext.getWriter();
		String logMessage = AbstractScriptNode.getTimestampPrefix() + " Resetting fixed classes...";
		writer.println(logMessage);
		System.out.println(logMessage);
		scriptContext.clearFixedClasses();
	}
}

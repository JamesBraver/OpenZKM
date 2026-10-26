package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.klassmaster.script.statements.token.StringLiteral;
import java.io.PrintWriter;

public class PrintStatement extends ScriptCommand {
	public String message;

	public PrintStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "print";
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		this.getChildrenCount();
		StringLiteral messageLiteral = (StringLiteral)this.getChild(0);
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();
		messageLiteral.parseArguments(this, scriptContext);
		this.message = messageLiteral.getText();
		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		PrintWriter writer = scriptContext.getWriter();
		String logMessage = AbstractScriptNode.getTimestampPrefix() + " " + this.message;
		writer.println(logMessage);
		System.out.println(logMessage);
	}
}

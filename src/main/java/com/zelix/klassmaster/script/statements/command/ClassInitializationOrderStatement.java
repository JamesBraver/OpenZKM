package com.zelix.klassmaster.script.statements.command;

import com.zelix.framework.reference.LinkedReferenceGraph;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import java.io.IOException;
import java.io.PrintWriter;

public class ClassInitializationOrderStatement extends ScriptCommand {
	public static String COMMAND_NAME = "classInitializationOrder";
	private LinkedReferenceGraph<String, String> initializationOrder = new LinkedReferenceGraph<>();

	public ClassInitializationOrderStatement(int nodeType) {
		super(nodeType);
	}

	public void addInitializationOrder(String beforeClass, String afterClass) {
		this.initializationOrder.addReference(beforeClass, afterClass);
	}

	@Override
	public String getCommandName() {
		return COMMAND_NAME;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();
		PrintWriter writer = scriptContext.getWriter();
		String logMessage = AbstractScriptNode.getTimestampPrefix() + " Setting class initialization order...";
		writer.println(logMessage);
		System.out.println(logMessage);
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, scriptContext);
		}

		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptContext.addReferences(this.initializationOrder);
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}

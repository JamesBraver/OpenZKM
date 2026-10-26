package com.zelix.klassmaster.script.statements.command;

import com.zelix.exceptions.ProcessingException;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.logging.LogContextLogger;
import com.zelix.utils.function.EmptyConsumer;
import com.zelix.workspace.Workspace;
import java.io.PrintWriter;

public class RemoveMethodCallsStatement extends ScriptCommand {
	public RemoveMethodCallsStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();
		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	@Override
	public String getCommandName() {
		return "removeMethodCalls";
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		Workspace workspace = scriptContext.getWorkspace();
		if (!workspace.hasWorkspaceClasses()) {
			scriptContext.logFatalError("Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " before opening classes");
		} else if (workspace.isEmpty()) {
			scriptContext.logFatalError("Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " with no classes opened");
		} else if (!workspace.isErrorEmpty()) {
			scriptContext.logFatalError("Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " with unusable classes : " + workspace.getError());
		} else if (!workspace.hasClasses()) {
			scriptContext.logMessage(
				"Only module-info.class classes opened. Statement \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " will have no effect."
			);
			return;
		}

		PrintWriter writer = scriptContext.getWriter();
		String logMessage = AbstractScriptNode.getTimestampPrefix() + " Removing method calls...";
		writer.println(logMessage);
		System.out.println(logMessage);
		LogContextLogger logger = new LogContextLogger(scriptContext, AbstractScriptNode.getTimestampPrefix().length());
		EmptyConsumer emptyConsumer = EmptyConsumer.create();

		try {
			workspace.performRemoveMethodCallAnalysis(scriptContext.getRemoveMethodCallsIncludeStatements(), scriptContext.getRemoveMethodCallsExcludeStatements(), logger, emptyConsumer, null, scriptContext);
		} catch (ProcessingException processingException) {
			scriptContext.logFatalError(processingException.getMessage());
		}

		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
	}
}

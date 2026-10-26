package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.klassmaster.script.statements.token.IntegerLiteral;
import java.io.PrintWriter;

public class GarbageCollectStatement extends ScriptCommand {
	public int sleepMillis;

	public GarbageCollectStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "gc";
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		int childCount = this.getChildrenCount();
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();
		if (childCount == 1) {
			IntegerLiteral sleepMillisLiteral = (IntegerLiteral)this.getChild(0);
			sleepMillisLiteral.parseArguments(this, scriptContext);
			String sleepMillisText = sleepMillisLiteral.getText();

			try {
				this.sleepMillis = Integer.parseInt(sleepMillisText);
			} catch (NumberFormatException ignored) {
				this.sleepMillis = 500;
				if (this.sleepMillis < 0) {
					this.sleepMillis = 500;
				}
			}
		} else {
			this.sleepMillis = 500;
		}

		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		PrintWriter writer = scriptContext.getWriter();
		String logMessage = AbstractScriptNode.getTimestampPrefix() + " Garbage collecting...";
		writer.println(logMessage);
		System.out.println(logMessage);
		Runtime runtime = Runtime.getRuntime();
		int usedMemoryBeforeKb = (int)(runtime.totalMemory() - runtime.freeMemory()) / 1024;
		runtime.gc();
		if (scriptContext.isVerbose()) {
			writer.println("\tSleeping for " + this.sleepMillis + " milliseconds");
		}

		try {
			Thread.sleep(this.sleepMillis);
		} catch (InterruptedException ignored) {
		}

		int usedMemoryAfterKb = (int)(runtime.totalMemory() - runtime.freeMemory()) / 1024;
		long maxMemory = runtime.maxMemory();
		if (maxMemory > -1L) {
			writer.println("\tMaximum memory is " + maxMemory / 1024L + "K. " + usedMemoryAfterKb + "K in use. Changed by " + (usedMemoryAfterKb - usedMemoryBeforeKb) + "K.");
		} else {
			writer.println("\t" + usedMemoryAfterKb + "K in use. Changed by " + (usedMemoryAfterKb - usedMemoryBeforeKb) + "K.");
		}
	}
}

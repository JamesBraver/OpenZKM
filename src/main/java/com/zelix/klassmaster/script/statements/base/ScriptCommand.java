package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.ScriptLocation;
import com.zelix.utils.StringUtils;
import java.io.IOException;

public abstract class ScriptCommand extends ScriptStatementBase implements ScriptLocation {
	protected int lineNumber;

	public ScriptCommand(int nodeType) {
		super(nodeType);
	}

	@Override
	public abstract String getCommandName();

	@Override
	public ScriptCommand findScriptCommand() {
		return this;
	}

	public final void reportCountChanges(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount, String phase) {
		StringBuilder builder = new StringBuilder();
		int messageDelta = scriptContext.getMessageCount() - initialMessageCount;
		if (messageDelta > 0) {
			if (builder.length() > 0) {
				builder.append(" and ");
			}

			builder.append(messageDelta + (messageDelta > 1 ? " messages" : " message"));
		}

		int warningDelta = scriptContext.getWarningCount() - initialWarningCount;
		if (warningDelta > 0) {
			if (builder.length() > 0) {
				builder.append(" and ");
			}

			builder.append(warningDelta + (warningDelta > 1 ? " warnings" : " warning"));
		}

		int errorDelta = scriptContext.getErrorCount() - initialErrorCount;
		if (errorDelta > 0) {
			if (builder.length() > 0) {
				builder.append(" and ");
			}

			builder.append(errorDelta + (errorDelta > 1 ? " errors" : " error"));
		}

		if (builder.length() > 0) {
			System.out
				.println(
					StringUtils.repeat(AbstractScriptNode.getTimestampPrefix().length() + 1, 32)
						+ builder.toString()
						+ " detected "
						+ phase
						+ " \""
						+ this.getCommandName()
						+ "\" statement"
				);
		}
	}

	@Override
	public int getLineNumber() {
		return this.lineNumber;
	}

	protected abstract void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) throws IOException;

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
}

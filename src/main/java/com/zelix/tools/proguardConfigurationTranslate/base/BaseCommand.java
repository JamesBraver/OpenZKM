package com.zelix.tools.proguardConfigurationTranslate.base;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.parser.CommandStatement;

public abstract class BaseCommand extends ProguardNodeBase {
	protected int lineNumber;

	public BaseCommand(int commandId) {
		super(commandId);
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	public abstract String getProguardCommandName();

	public BaseCommand getPreviousCommand() {
		return ((CommandStatement)this.getParent()).getPreviousCommand();
	}

	@Override
	public abstract void parseArguments(
		ProguardNode parent, ProguardScriptTranslator scriptTranslator
	);

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	protected abstract void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount);
}

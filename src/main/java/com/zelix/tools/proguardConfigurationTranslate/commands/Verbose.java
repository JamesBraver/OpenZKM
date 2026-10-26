package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;

public class Verbose extends KeepCommand {
	public Verbose(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-verbose";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.setVerbose();
	}
}

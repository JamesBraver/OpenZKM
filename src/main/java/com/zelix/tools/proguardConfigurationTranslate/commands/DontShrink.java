package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;

public class DontShrink extends KeepCommand {
	public DontShrink(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontshrink";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.setDontShrink();
	}
}

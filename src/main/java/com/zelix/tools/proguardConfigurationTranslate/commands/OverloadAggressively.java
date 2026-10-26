package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;

public class OverloadAggressively extends KeepCommand {
	public OverloadAggressively(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-overloadaggressively";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.setAggressiveMethodRenaming();
	}
}

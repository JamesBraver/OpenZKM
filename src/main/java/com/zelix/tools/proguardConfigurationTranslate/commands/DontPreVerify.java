package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;

public class DontPreVerify extends KeepCommand {
	public DontPreVerify(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontpreverify";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.setDontPreVerify();
	}
}

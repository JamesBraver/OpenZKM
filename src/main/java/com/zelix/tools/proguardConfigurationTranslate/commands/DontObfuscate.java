package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;

public class DontObfuscate extends KeepCommand {
	public DontObfuscate(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontobfuscate";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.setDontObfuscate();
	}
}

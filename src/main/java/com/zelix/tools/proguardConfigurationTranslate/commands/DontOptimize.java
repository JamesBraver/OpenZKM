package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;

public class DontOptimize extends KeepCommand {
	public DontOptimize(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontoptimize";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.setDontOptimize();
	}
}

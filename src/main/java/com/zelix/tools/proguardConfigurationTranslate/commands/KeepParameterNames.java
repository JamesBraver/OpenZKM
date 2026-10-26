package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;

public class KeepParameterNames extends KeepCommand {
	public KeepParameterNames(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-keepparameternames";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.setKeepParameterNames();
	}
}

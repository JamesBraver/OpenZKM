package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;

public class DontUseMixedCaseClassNames extends KeepCommand {
	public DontUseMixedCaseClassNames(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontusemixedcaseclassnames";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.disableMixedCaseClassNames();
	}
}

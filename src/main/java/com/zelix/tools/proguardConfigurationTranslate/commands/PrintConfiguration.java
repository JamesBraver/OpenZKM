package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseSettingCommand;

public class PrintConfiguration extends BaseSettingCommand {
	public PrintConfiguration(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-printconfiguration";
	}
}

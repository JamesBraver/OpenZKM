package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseSettingCommand;

public class AddConfigurationDebugging extends BaseSettingCommand {
	public AddConfigurationDebugging(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-addconfigurationdebugging";
	}
}

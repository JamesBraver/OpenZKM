package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseSettingCommand;

public class Android extends BaseSettingCommand {
	public Android(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-android";
	}
}

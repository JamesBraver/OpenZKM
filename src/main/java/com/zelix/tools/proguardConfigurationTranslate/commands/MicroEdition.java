package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseSettingCommand;

public class MicroEdition extends BaseSettingCommand {
	public MicroEdition(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-microedition";
	}
}

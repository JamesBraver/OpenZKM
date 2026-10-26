package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseSettingCommand;

public class AdaptKotlinMetadata extends BaseSettingCommand {
	public AdaptKotlinMetadata(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-adaptkotlinmetadata";
	}
}

package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseSettingCommand;

public class AdaptResourceFileContents extends BaseSettingCommand {
	public AdaptResourceFileContents(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-adaptresourcefilecontents";
	}
}

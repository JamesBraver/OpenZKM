package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseSettingCommand;

public class WhyAreYouKeeping extends BaseSettingCommand {
	public WhyAreYouKeeping(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-whyareyoukeeping";
	}
}

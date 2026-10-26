package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseSettingCommand;

public class RenameSourceFileAttribute extends BaseSettingCommand {
	public RenameSourceFileAttribute(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-renamesourcefileattribute";
	}
}

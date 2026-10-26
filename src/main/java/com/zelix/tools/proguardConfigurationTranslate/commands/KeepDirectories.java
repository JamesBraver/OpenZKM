package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class KeepDirectories extends UnsupportedCommand {
	public KeepDirectories(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-keepdirectories";
	}
}

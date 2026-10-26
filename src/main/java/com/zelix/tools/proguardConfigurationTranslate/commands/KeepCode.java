package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class KeepCode extends UnsupportedCommand {
	public KeepCode(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-keepcode";
	}
}

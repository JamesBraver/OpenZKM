package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class Target extends UnsupportedCommand {
	public Target(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-target";
	}
}

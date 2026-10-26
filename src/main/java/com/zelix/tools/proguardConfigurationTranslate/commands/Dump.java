package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class Dump extends UnsupportedCommand {
	public Dump(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dump";
	}
}

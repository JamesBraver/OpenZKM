package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class Optimizations extends UnsupportedCommand {
	public Optimizations(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-optimizations";
	}
}

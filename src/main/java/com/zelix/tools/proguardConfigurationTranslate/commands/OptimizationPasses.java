package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class OptimizationPasses extends UnsupportedCommand {
	public OptimizationPasses(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-optimizationpasses";
	}
}

package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class ForceProcessing extends UnsupportedCommand {
	public ForceProcessing(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-forceprocessing";
	}
}

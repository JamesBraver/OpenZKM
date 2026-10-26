package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class AssumeValues extends UnsupportedCommand {
	public AssumeValues(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-assumevalues";
	}
}

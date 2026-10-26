package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.AssumeCommand;

public class AssumeNoEscapingParameters extends AssumeCommand {
	public AssumeNoEscapingParameters(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-assumenoescapingparameters";
	}
}

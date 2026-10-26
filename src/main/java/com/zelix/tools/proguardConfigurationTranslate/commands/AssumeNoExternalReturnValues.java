package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.AssumeCommand;

public class AssumeNoExternalReturnValues extends AssumeCommand {
	public AssumeNoExternalReturnValues(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-assumenoexternalreturnvalues";
	}
}

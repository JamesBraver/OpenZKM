package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.AssumeCommand;

public class AssumeNoSideEffects extends AssumeCommand {
	public AssumeNoSideEffects(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-assumenosideeffects";
	}
}

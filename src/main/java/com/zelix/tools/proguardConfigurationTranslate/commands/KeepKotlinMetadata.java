package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class KeepKotlinMetadata extends UnsupportedCommand {
	public KeepKotlinMetadata(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-keepkotlinmetadata";
	}
}

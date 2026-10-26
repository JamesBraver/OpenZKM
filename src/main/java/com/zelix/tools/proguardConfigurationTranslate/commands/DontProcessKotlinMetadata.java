package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class DontProcessKotlinMetadata extends UnsupportedCommand {
	public DontProcessKotlinMetadata(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontprocesskotlinmetadata";
	}
}

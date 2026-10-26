package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class AdaptResourceFileNames extends UnsupportedCommand {
	public AdaptResourceFileNames(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-adaptresourcefilenames";
	}
}

package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class ZipAlign extends UnsupportedCommand {
	public ZipAlign(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-zipalign";
	}
}

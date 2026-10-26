package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class DontCompress extends UnsupportedCommand {
	public DontCompress(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontcompress";
	}
}

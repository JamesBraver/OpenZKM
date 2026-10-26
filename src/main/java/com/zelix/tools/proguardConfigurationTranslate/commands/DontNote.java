package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class DontNote extends UnsupportedCommand {
	public DontNote(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontnote";
	}
}

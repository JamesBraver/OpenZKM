package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class SkipNonPublicLibraryClassMembers extends UnsupportedCommand {
	public SkipNonPublicLibraryClassMembers(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-skipnonpubliclibraryclassmembers";
	}
}

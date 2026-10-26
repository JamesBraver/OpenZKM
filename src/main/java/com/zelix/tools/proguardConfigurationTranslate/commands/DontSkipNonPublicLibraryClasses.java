package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class DontSkipNonPublicLibraryClasses extends UnsupportedCommand {
	public DontSkipNonPublicLibraryClasses(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-dontskipnonpubliclibraryclasses";
	}
}

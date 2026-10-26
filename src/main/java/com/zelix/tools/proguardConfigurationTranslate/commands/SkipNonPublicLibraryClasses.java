package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class SkipNonPublicLibraryClasses extends UnsupportedCommand {
	public SkipNonPublicLibraryClasses(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-skipnonpubliclibraryclasses";
	}
}

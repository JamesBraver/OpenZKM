package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class MergeInterfacesAggressively extends UnsupportedCommand {
	public MergeInterfacesAggressively(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-mergeinterfacesaggressively";
	}
}

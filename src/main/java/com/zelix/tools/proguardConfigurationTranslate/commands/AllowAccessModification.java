package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class AllowAccessModification extends UnsupportedCommand {
	public AllowAccessModification(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-allowaccessmodification";
	}
}

package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.UnsupportedCommand;

public class UseUniqueClassMemberNames extends UnsupportedCommand {
	public UseUniqueClassMemberNames(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-useuniqueclassmembernames";
	}
}

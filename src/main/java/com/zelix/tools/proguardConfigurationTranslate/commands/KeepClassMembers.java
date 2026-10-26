package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseKeepCommand;

public class KeepClassMembers extends BaseKeepCommand {
	public KeepClassMembers(int commandId) {
		super(commandId);
	}

	@Override
	public boolean isClassPattern() {
		return false;
	}

	@Override
	public String getProguardCommandName() {
		return "-keepclassmembers";
	}

	@Override
	public boolean usesContainingClause() {
		return false;
	}
}

package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseKeepCommand;

public class KeepClassesWithMembers extends BaseKeepCommand {
	public KeepClassesWithMembers(int commandId) {
		super(commandId);
	}

	@Override
	public boolean isClassPattern() {
		return true;
	}

	@Override
	public String getProguardCommandName() {
		return "-keepclasseswithmembers";
	}

	@Override
	public boolean usesContainingClause() {
		return true;
	}
}

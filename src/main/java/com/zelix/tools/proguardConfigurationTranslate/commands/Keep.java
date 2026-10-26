package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.base.BaseKeepCommand;

public class Keep extends BaseKeepCommand {
	public Keep(int commandId) {
		super(commandId);
	}

	@Override
	public boolean isClassPattern() {
		return true;
	}

	@Override
	public String getProguardCommandName() {
		return "-keep";
	}

	@Override
	public boolean usesContainingClause() {
		return false;
	}
}

package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.BaseKeepCommand;

public class KeepClassMemberNames extends BaseKeepCommand {
	public KeepClassMemberNames(int commandId) {
		super(commandId);
	}

	@Override
	public boolean isClassPattern() {
		return false;
	}

	@Override
	public String getProguardCommandName() {
		return "-keepclassmembernames";
	}

	@Override
	public boolean usesContainingClause() {
		return false;
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		this.addKeepModifier("allowshrinking");
		super.parseArguments(parent, scriptTranslator);
	}
}

package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.BaseKeepCommand;

public class KeepClassesWithMemberNames extends BaseKeepCommand {
	public KeepClassesWithMemberNames(int commandId) {
		super(commandId);
	}

	@Override
	public boolean isClassPattern() {
		return true;
	}

	@Override
	public String getProguardCommandName() {
		return "-keepclasseswithmembernames";
	}

	@Override
	public boolean usesContainingClause() {
		return true;
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		this.addKeepModifier("allowshrinking");
		super.parseArguments(parent, scriptTranslator);
	}
}

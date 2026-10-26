package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.BasePatternCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassSpecification;

public class If extends BasePatternCommand {
	public If(int commandId) {
		super(commandId);
	}

	@Override
	public boolean isClassPattern() {
		return false;
	}

	@Override
	public String getProguardCommandName() {
		return "-if";
	}

	public ClassSpecification getClassSpecification() {
		return (ClassSpecification)this.getChild(0);
	}

	@Override
	public boolean usesContainingClause() {
		return false;
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		super.parseArguments(parent, scriptTranslator);
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		ClassSpecification classSpecification = (ClassSpecification)this.getChild(0);
	}
}

package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.BaseCommand;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;

public class CommandStatement extends ProguardNodeBase {
	public CommandStatement(int commandId) {
		super(commandId);
	}

	public BaseCommand getPreviousCommand() {
		return ((ProguardConfigRootNode)this.getParent()).getPreviousCommand();
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		((BaseCommand)this.getChild(0)).parseArguments(this, scriptTranslator);
	}
}

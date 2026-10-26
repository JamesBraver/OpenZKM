package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.BaseCommand;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;

public class ProguardConfigRootNode extends ProguardNodeBase {
	private BaseCommand previousCommand;

	public ProguardConfigRootNode(int commandId) {
		super(commandId);
	}

	public BaseCommand getPreviousCommand() {
		return this.previousCommand;
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			CommandStatement commandStatement = (CommandStatement)this.getChild(i);
			commandStatement.parseArguments(this, scriptTranslator);
			this.previousCommand = (BaseCommand)commandStatement.getChild(0);
		}
	}
}

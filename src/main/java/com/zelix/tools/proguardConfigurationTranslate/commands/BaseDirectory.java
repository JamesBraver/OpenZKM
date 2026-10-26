package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;

public class BaseDirectory extends KeepCommand {
	public BaseDirectory(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-basedirectory";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.setBaseDirectory(((QuotedStringNode)this.getChild(0)).getValue());
	}
}

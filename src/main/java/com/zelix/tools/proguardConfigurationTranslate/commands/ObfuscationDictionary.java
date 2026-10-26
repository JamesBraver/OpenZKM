package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;

public class ObfuscationDictionary extends KeepCommand {
	public ObfuscationDictionary(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-obfuscationdictionary";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		if (this.getChildrenCount() > 0) {
			String obfuscationDictionaryFileName = ((QuotedStringNode)this.getChild(0)).getValue();
			scriptTranslator.setObfuscationDictionary(obfuscationDictionaryFileName);
		}
	}
}

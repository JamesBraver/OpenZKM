package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;

public class ClassObfuscationDictionary extends KeepCommand {
	public ClassObfuscationDictionary(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-classobfuscationdictionary";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		if (this.getChildrenCount() > 0) {
			String classNameDictionaryFileName = ((QuotedStringNode)this.getChild(0)).getValue();
			scriptTranslator.setClassNameDictionary(classNameDictionaryFileName);
		}
	}
}

package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;

public class PackageObfuscationDictionary extends KeepCommand {
	public PackageObfuscationDictionary(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-packageobfuscationdictionary";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		if (this.getChildrenCount() > 0) {
			String packageNameDictionaryFileName = ((QuotedStringNode)this.getChild(0)).getValue();
			scriptTranslator.setPackageNameDictionary(packageNameDictionaryFileName);
		}
	}
}

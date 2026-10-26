package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;

public class FlattenPackageHierarchy extends KeepCommand {
	public FlattenPackageHierarchy(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-flattenpackagehierarchy";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		if (this.getChildrenCount() == 1) {
			scriptTranslator.setFlattenedPackageName(((QuotedStringNode)this.getChild(0)).getValue());
		} else {
			scriptTranslator.setFlattenedPackageName("");
		}
	}
}

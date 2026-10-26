package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;

public class Include extends KeepCommand {
	public Include(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-include";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		scriptTranslator.logSpecialError(
			"ProGuard command '"
				+ this.getProguardCommandName()
				+ "' should have already been expanded : '"
				+ (this.getChildrenCount() > 0 ? ((QuotedStringNode)this.getChild(0)).getValue() : "")
				+ "'"
		);
	}
}

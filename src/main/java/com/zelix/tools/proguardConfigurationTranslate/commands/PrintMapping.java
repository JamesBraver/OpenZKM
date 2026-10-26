package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.QuotedStringNode;

public class PrintMapping extends KeepCommand {
	public PrintMapping(int commandId) {
		super(commandId);
	}

	@Override
	public String getProguardCommandName() {
		return "-printmapping";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		if (this.getChildrenCount() > 0) {
			String printMappingFileName = ((QuotedStringNode)this.getChild(0)).getValue();
			scriptTranslator.setPrintMapping(printMappingFileName);
		} else {
			scriptTranslator.setPrintMapping("ChangeLog.txt");
		}
	}
}

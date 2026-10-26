package com.zelix.tools.proguardConfigurationTranslate.base;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.commands.If;

public abstract class KeepCommand extends BaseCommand {
	public KeepCommand(int commandId) {
		super(commandId);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		BaseCommand previousCommand = this.getPreviousCommand();
		if (previousCommand != null && previousCommand instanceof If && !(this instanceof BaseKeepCommand)) {
			If ifCommand = (If)previousCommand;
			scriptTranslator.logError(
				"ProGuard '" + ifCommand.getProguardCommandName() + "' command at line " + ifCommand.getLineNumber() + " followed by '" + this.getProguardCommandName() + "' rather than a keep command."
			);
		}

		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		int messageCount = scriptTranslator.getMessageCount();
		int warningCount = scriptTranslator.getWarningCount();
		int errorCount = scriptTranslator.getErrorCount();
		this.executeCommand(scriptTranslator, messageCount, warningCount, errorCount);
	}
}

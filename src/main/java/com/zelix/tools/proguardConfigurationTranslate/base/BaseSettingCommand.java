package com.zelix.tools.proguardConfigurationTranslate.base;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;

public abstract class BaseSettingCommand extends BaseCommand {
	public BaseSettingCommand(int commandId) {
		super(commandId);
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		scriptTranslator.logWarning("ProGuard '" + this.getProguardCommandName() + "' command is not directly supported.");
	}

	@Override
	protected final void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
	}
}

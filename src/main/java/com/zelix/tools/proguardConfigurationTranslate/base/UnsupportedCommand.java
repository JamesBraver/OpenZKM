package com.zelix.tools.proguardConfigurationTranslate.base;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;

public abstract class UnsupportedCommand extends BaseCommand {
	public UnsupportedCommand(int commandId) {
		super(commandId);
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		scriptTranslator.logWarning("ProGuard '" + this.getProguardCommandName() + "' command is not supported.");
	}

	@Override
	protected final void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
	}
}

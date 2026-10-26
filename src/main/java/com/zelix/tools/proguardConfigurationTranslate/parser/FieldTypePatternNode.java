package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;

public class FieldTypePatternNode extends StringValueNode {
	public FieldTypePatternNode(int commandId) {
		super(commandId);
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		scriptTranslator.logWarning("Invalid syntax. Field type '" + this.getValue() + "' appears with '<fields>'. Field type ignored.");
	}
}

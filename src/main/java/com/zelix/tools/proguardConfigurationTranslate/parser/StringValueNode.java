package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;

public abstract class StringValueNode extends ProguardNodeBase {
	protected String value;

	public StringValueNode(int commandId) {
		super(commandId);
	}

	public final void setValue(String value) {
		this.value = value.trim();
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		super.parseArguments(parent, scriptTranslator);
	}

	public final String getValue() {
		return this.value;
	}
}

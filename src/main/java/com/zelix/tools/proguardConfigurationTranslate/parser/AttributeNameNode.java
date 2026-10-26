package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.commands.KeepAttributes;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NegationConsumer;

public class AttributeNameNode extends StringValueNode implements NegationConsumer {
	private boolean negated;

	public AttributeNameNode(int commandId) {
		super(commandId);
	}

	@Override
	public void setNegated() {
		this.negated = true;
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		super.parseArguments(parent, scriptTranslator);
		((KeepAttributes)this.getParent()).addAttribute((this.negated ? "!" : "") + this.getValue());
	}
}

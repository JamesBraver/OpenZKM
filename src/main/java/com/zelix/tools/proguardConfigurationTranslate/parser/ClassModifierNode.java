package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.ModifierConsumer;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NegationConsumer;

public class ClassModifierNode extends StringValueNode implements NegationConsumer {
	private boolean negated;

	public ClassModifierNode(int commandId) {
		super(commandId);
	}

	@Override
	public void setNegated() {
		this.negated = true;
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		((ModifierConsumer)this.getParent()).addModifier((this.negated ? "!" : "") + this.getValue());
	}
}

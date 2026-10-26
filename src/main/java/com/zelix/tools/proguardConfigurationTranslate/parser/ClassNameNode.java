package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NameConsumer;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NegationConsumer;

public class ClassNameNode extends StringValueNode implements NegationConsumer {
	private boolean negated;

	public ClassNameNode(int commandId) {
		super(commandId);
	}

	@Override
	public void setNegated() {
		this.negated = true;
	}

	public boolean isNegated() {
		return this.negated;
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		ProguardNode parentNode = this.getParent();
		if (parentNode instanceof NameConsumer) {
			((NameConsumer)parentNode).setName(this.getValue());
		}
	}
}

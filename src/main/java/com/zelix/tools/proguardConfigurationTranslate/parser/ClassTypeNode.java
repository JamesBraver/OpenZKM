package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NegationConsumer;

public class ClassTypeNode extends StringValueNode implements NegationConsumer {
	private boolean negated;

	public ClassTypeNode(int commandId) {
		super(commandId);
	}

	@Override
	public void setNegated() {
		this.negated = true;
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		super.parseArguments(parent, scriptTranslator);
		ClassSpecification classSpecification = (ClassSpecification)this.getParent();
		String typeName = this.getValue();
		if (typeName.equals("interface") || typeName.equals("enum")) {
			classSpecification.addModifier((this.negated ? "!" : "") + typeName);
		} else if (typeName.equals("@interface")) {
			classSpecification.addModifier((this.negated ? "!" : "") + "annotation");
		} else if (this.negated) {
			scriptTranslator.logWarning("'!" + this.getValue() + "' is not directly supported");
		}
	}
}

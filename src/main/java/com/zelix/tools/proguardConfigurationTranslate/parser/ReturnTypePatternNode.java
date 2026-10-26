package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;

public class ReturnTypePatternNode extends StringValueNode {
	public ReturnTypePatternNode(int commandId) {
		super(commandId);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		((MethodSpecificationNode)this.getParent()).setReturnTypePattern(this.getValue());
		if (!this.getValue().equals("***")) {
			scriptTranslator.logWarning("Method return types other than '***' are not supported in a method specification : '" + this.getValue() + "'");
		}
	}
}

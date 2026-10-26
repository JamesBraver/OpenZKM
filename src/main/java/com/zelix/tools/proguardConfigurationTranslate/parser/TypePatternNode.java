package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;

public class TypePatternNode extends StringValueNode {
	public TypePatternNode(int commandId) {
		super(commandId);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		((FieldSpecificationNode)this.getParent()).setTypePattern(this.getValue());
	}
}

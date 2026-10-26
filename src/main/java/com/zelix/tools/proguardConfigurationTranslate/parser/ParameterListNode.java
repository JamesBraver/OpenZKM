package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import java.util.ArrayList;
import java.util.List;

public class ParameterListNode extends ProguardNodeBase {
	private List<String> parameterPatterns = new ArrayList<>();

	public ParameterListNode(int commandId) {
		super(commandId);
	}

	public void addParameterPattern(String pattern) {
		this.parameterPatterns.add(pattern);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		((MethodSpecificationNode)this.getParent()).setParameterTypes(this.parameterPatterns);
	}
}

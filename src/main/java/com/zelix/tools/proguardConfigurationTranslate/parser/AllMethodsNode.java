package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import java.util.ArrayList;

public class AllMethodsNode extends ProguardNodeBase {
	public AllMethodsNode(int commandId) {
		super(commandId);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		MethodSpecificationNode methodSpec = (MethodSpecificationNode)this.getParent();
		methodSpec.setName("*");
		ArrayList<String> parameterTypes = new ArrayList<>(1);
		parameterTypes.add("*");
		methodSpec.setParameterTypes(parameterTypes);
	}
}

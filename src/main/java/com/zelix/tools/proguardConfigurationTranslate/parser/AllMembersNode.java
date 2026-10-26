package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import java.util.ArrayList;

public class AllMembersNode extends ProguardNodeBase {
	public AllMembersNode(int commandId) {
		super(commandId);
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		MemberSpecificationNode memberSpec = (MemberSpecificationNode)this.getParent();
		memberSpec.markAsMethodSpec();
		memberSpec.markAsFieldSpec();
		memberSpec.setName("*");
		ArrayList<String> parameterTypes = new ArrayList<>(1);
		parameterTypes.add("*");
		memberSpec.setParameterTypes(parameterTypes);
	}
}

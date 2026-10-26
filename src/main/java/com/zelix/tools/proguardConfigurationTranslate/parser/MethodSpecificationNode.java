package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NameConsumer;
import java.util.ArrayList;
import java.util.List;

public class MethodSpecificationNode extends ProguardNodeBase implements NameConsumer {
	private List<String> parameterTypes = new ArrayList<>();
	private String namePattern;
	private String returnTypePattern;

	public MethodSpecificationNode(int commandId) {
		super(commandId);
	}

	public void setParameterTypes(List<String> parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	@Override
	public void setName(String name) {
		this.namePattern = name;
	}

	public void setReturnTypePattern(String returnTypePattern) {
		this.returnTypePattern = returnTypePattern;
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		MemberSpecificationNode memberSpec = (MemberSpecificationNode)this.getParent();
		memberSpec.markAsMethodSpec();
		memberSpec.setName(this.namePattern);
		memberSpec.setParameterTypes(this.parameterTypes);
	}
}

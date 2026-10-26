package com.zelix.tools.proguardConfigurationTranslate.parser;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.ProguardNodeBase;
import com.zelix.tools.proguardConfigurationTranslate.javaccInterface.NameConsumer;

public class FieldSpecificationNode extends ProguardNodeBase implements NameConsumer {
	private String namePattern;
	private String typePattern;

	public FieldSpecificationNode(int commandId) {
		super(commandId);
	}

	public void setTypePattern(String typePattern) {
		this.typePattern = typePattern;
	}

	@Override
	public void setName(String name) {
		this.namePattern = name;
	}

	@Override
	public final void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		for (int i = 0; i < this.getChildrenCount(); i++) {
			this.getChild(i).parseArguments(this, scriptTranslator);
		}

		MemberSpecificationNode memberSpec = (MemberSpecificationNode)this.getParent();
		memberSpec.markAsFieldSpec();
		memberSpec.setName(this.namePattern);
		memberSpec.setTypePattern(this.typePattern);
	}
}

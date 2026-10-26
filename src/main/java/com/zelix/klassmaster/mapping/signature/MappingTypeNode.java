package com.zelix.klassmaster.mapping.signature;

import com.zelix.klassmaster.mapping.base.MappingNameNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingTypeNode extends MappingNameNodeBase {
	public MappingTypeNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		((MappingQualifiedTypeNode)parent).addNamePart(super.name);
	}
}

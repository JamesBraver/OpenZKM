package com.zelix.klassmaster.mapping.signature;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingArrayLevelNode extends MappingNodeBase {
	public MappingArrayLevelNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		((MappingQualifiedTypeNode)parent).incrementArrayLevel();
	}
}

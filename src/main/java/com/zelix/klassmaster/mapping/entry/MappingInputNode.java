package com.zelix.klassmaster.mapping.entry;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingInputNode extends MappingNodeBase {
	public MappingInputNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
	}
}

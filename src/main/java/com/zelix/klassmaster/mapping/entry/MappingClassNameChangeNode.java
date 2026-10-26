package com.zelix.klassmaster.mapping.entry;

import com.zelix.klassmaster.mapping.base.MappingNameChangeNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingClassNameChangeNode extends MappingNameChangeNodeBase {
	public MappingClassNameChangeNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
	}
}

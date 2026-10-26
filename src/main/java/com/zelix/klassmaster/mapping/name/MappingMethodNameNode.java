package com.zelix.klassmaster.mapping.name;

import com.zelix.klassmaster.mapping.base.MappingNameNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.NameConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingMethodNameNode extends MappingNameNodeBase {
	public MappingMethodNameNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		((NameConsumer)parent).setName(super.name);
	}
}

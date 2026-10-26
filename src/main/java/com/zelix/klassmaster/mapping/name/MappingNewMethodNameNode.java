package com.zelix.klassmaster.mapping.name;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.entry.MappingMethodNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.NameConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingNewMethodNameNode extends MappingNodeBase implements NameConsumer {
	private String newMethodName;

	public MappingNewMethodNameNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setName(String name) {
		this.newMethodName = name;
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		((MappingMethodNameChangeNode)parent).setNewName(this.newMethodName);
	}
}

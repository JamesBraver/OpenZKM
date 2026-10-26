package com.zelix.klassmaster.mapping.name;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.entry.MappingClassNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.NameConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingNewClassNameNode extends MappingNodeBase implements NameConsumer {
	private String newClassName;

	public MappingNewClassNameNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setName(String name) {
		this.newClassName = name;
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		((MappingClassNameChangeNode)parent).setNewName(this.newClassName);
	}
}

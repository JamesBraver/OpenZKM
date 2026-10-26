package com.zelix.klassmaster.mapping.name;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.entry.MappingClassNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.NameConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingOldClassNameNode extends MappingNodeBase implements NameConsumer {
	private String oldClassName;

	public MappingOldClassNameNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setName(String name) {
		this.oldClassName = name;
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		((MappingClassNameChangeNode)parent).setOldName(this.oldClassName);
	}
}

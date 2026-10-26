package com.zelix.klassmaster.mapping.name;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.entry.MappingFieldNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.NameConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingOldFieldNameNode extends MappingNodeBase implements NameConsumer {
	private String oldFieldName;

	public MappingOldFieldNameNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setName(String name) {
		this.oldFieldName = name;
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		((MappingFieldNameChangeNode)parent).setOldName(this.oldFieldName);
	}
}

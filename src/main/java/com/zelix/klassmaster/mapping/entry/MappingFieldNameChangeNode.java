package com.zelix.klassmaster.mapping.entry;

import com.zelix.klassmaster.mapping.base.MappingNameChangeNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.QualifiedTypeConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public class MappingFieldNameChangeNode extends MappingNameChangeNodeBase implements QualifiedTypeConsumer, MappingNameChangeNode {
	private String fieldType;

	public MappingFieldNameChangeNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setTypeSignature(String typeSignature) {
		this.fieldType = typeSignature;
	}

	@Override
	public void writeNameChange(String className, ProguardTranslateWriter writer) {
		writer.writeFieldMapping(className, this.fieldType, super.oldName, super.newName);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
	}
}

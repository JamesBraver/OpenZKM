package com.zelix.klassmaster.mapping.entry;

import com.zelix.klassmaster.mapping.base.MappingNameChangeNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.QualifiedTypeConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;
import java.util.List;

public class MappingMethodNameChangeNode extends MappingNameChangeNodeBase implements QualifiedTypeConsumer, MappingNameChangeNode {
	public List<String> argumentTypes;
	private String returnType;

	public MappingMethodNameChangeNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setTypeSignature(String typeSignature) {
		this.returnType = typeSignature;
	}

	@Override
	public void writeNameChange(String className, ProguardTranslateWriter writer) {
		writer.writeMethodMapping(className, this.returnType, super.oldName, super.newName, this.argumentTypes);
	}

	public void setArgumentTypes(List<String> argumentTypes) {
		this.argumentTypes = argumentTypes;
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
	}
}

package com.zelix.klassmaster.mapping.signature;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.entry.MappingMethodNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.NameConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;
import java.util.List;

public class MappingMethodSignatureNode extends MappingNodeBase implements NameConsumer {
	private List<String> argumentTypes;
	private String methodName;

	public MappingMethodSignatureNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setName(String name) {
		this.methodName = name;
	}

	public void setArgumentTypes(List<String> argumentTypes) {
		this.argumentTypes = argumentTypes;
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		MappingMethodNameChangeNode nameChangeNode = (MappingMethodNameChangeNode)parent;
		nameChangeNode.setOldName(this.methodName);
		nameChangeNode.setArgumentTypes(this.argumentTypes);
	}
}

package com.zelix.klassmaster.mapping.signature;

import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.klassmaster.mapping.javaccInterface.QualifiedTypeConsumer;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;
import java.util.ArrayList;
import java.util.List;

public class MappingMethodArgumentsNode extends MappingNodeBase implements QualifiedTypeConsumer {
	private List<String> argumentTypes = new ArrayList<>();

	public MappingMethodArgumentsNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void setTypeSignature(String typeSignature) {
		this.argumentTypes.add(typeSignature);
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		super.interpret(parent, writer);
		((MappingMethodSignatureNode)parent).setArgumentTypes(this.argumentTypes);
	}
}

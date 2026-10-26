package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.signature.MethodParameter;
import java.io.IOException;

public class ParameterComplexAnnotationSpecifier extends AbstractScriptNode {
	public ParameterComplexAnnotationSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		ScriptNode parameterAnnotationSpecifier = this.getChild(0);
		parameterAnnotationSpecifier.parseArguments(this, scriptContext);
		((MethodParameter)parent).setAnnotationSpecifier((ComplexAnnotationSpecifier)parameterAnnotationSpecifier);
	}
}

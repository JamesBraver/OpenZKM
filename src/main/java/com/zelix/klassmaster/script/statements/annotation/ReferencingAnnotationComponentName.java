package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;

public class ReferencingAnnotationComponentName extends TokenNode {
	public ReferencingAnnotationComponentName(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		((RenameFilterParameter)parent).setSearchByAnnotationProperty(this.text);
	}
}

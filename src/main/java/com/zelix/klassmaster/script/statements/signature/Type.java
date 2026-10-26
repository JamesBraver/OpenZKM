package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;

public class Type extends TokenNode {
	public Type(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		((QualifiedType)super.parent).addNamePart(this.getText());
	}
}

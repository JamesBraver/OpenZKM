package com.zelix.klassmaster.script.statements.token;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;

public class IntegerLiteral extends TokenNode {
	public IntegerLiteral(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
	}
}

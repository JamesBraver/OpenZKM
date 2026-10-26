package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;

public class ArrayLevel extends AbstractScriptNode {
	public ArrayLevel(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		((QualifiedType)parent).addArrayLevel();
	}
}

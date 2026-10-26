package com.zelix.klassmaster.script.statements.modifier;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;

public class ClassModifier extends TokenNode {
	public ClassModifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		((RenameFilterParameter)parent).addClassAccessFlag(this.text);
	}
}

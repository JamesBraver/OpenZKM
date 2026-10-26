package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;

public abstract class TokenNode extends AbstractScriptNode {
	protected String text;

	public TokenNode(int nodeType) {
		super(nodeType);
	}

	public final void setText(String text) {
		this.text = text;
	}

	public final String getText() {
		return this.text;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
	}
}

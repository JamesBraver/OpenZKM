package com.zelix.klassmaster.script.statements.name;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.signature.LinkMethodSignature;

public class LinkMethodName extends TokenNode {
	public LinkMethodName(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		((LinkMethodSignature)parent).setLinkMethodNamePattern(this.getText());
	}
}

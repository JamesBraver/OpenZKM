package com.zelix.klassmaster.script.statements.token;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;

public class SkipArchivePath extends TokenNode {
	public SkipArchivePath(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		if (this.getChildrenCount() == 1) {
			ScriptNode child = this.getChild(0);
			if (child instanceof StringLiteral) {
				StringLiteral stringLiteral = (StringLiteral)child;
				this.text = stringLiteral.getText();
			}
		}
	}
}

package com.zelix.klassmaster.script.statements.name;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;

public class LinkClassName extends TokenNode {
	public LinkClassName(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		((RenameFilterParameter)parent).setLinkClassNamePattern(this.getText());
	}
}

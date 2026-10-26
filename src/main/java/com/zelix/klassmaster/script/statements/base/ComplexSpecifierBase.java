package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NamePattern;
import java.io.IOException;

public abstract class ComplexSpecifierBase extends AbstractScriptNode {
	public ComplexSpecifierBase(int nodeType) {
		super(nodeType);
	}

	public final String getNamePattern() {
		return ((NamePattern)this.getChild(0)).toRuleString();
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChild(0).parseArguments(this, scriptContext);
	}

	public String toRuleString() {
		return ((NamePattern)this.getChild(0)).toRuleString();
	}

	public abstract boolean isExactName();
}

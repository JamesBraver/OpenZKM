package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NamePattern;
import java.io.IOException;

public abstract class BracketedSpecifierBase extends AbstractScriptNode {
	public boolean negated = false;

	public BracketedSpecifierBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, scriptContext);
		}
	}

	public String toRuleString() {
		StringBuilder builder = new StringBuilder();
		int childCount = this.children.length;
		if (this.negated || childCount > 1) {
			builder.append('(');
		}

		for (int i = 0; i < childCount; i++) {
			NamePattern specifier = (NamePattern)this.children[i];
			builder.append(specifier.toRuleString());
			if (i < childCount - 1) {
				builder.append(" || ");
			}
		}

		if (this.negated || childCount > 1) {
			builder.append(')');
		}

		return (this.negated ? "!" : "") + builder.toString();
	}

	public void setNegated() {
		this.negated = true;
	}
}

package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NamePattern;
import java.io.IOException;

public abstract class ConjunctiveSpecifierBase extends AbstractScriptNode {
	public ConjunctiveSpecifierBase(int nodeType) {
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
		StringBuffer builder = new StringBuffer();
		int childCount = this.children.length;
		if (childCount > 1) {
			builder.append('(');
		}

		for (int i = 0; i < childCount; i++) {
			NamePattern specifier = (NamePattern)this.children[i];
			builder.append(specifier.toRuleString());
			if (i < childCount - 1) {
				builder.append(" && ");
			}
		}

		if (childCount > 1) {
			builder.append(')');
		}

		return builder.toString();
	}
}

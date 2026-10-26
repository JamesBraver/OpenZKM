package com.zelix.klassmaster.script.statements.name;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import java.io.IOException;

public class QualifiedClassName extends AbstractScriptNode {
	private StringBuilder classNameBuilder = new StringBuilder();

	public QualifiedClassName(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childrenCount = this.getChildrenCount();

		for (int i = 0; i < childrenCount; i++) {
			ScriptNode childNode = this.getChild(i);
			childNode.parseArguments(this, scriptContext);
			WildcardNameMatcher nameMatcher = (WildcardNameMatcher)childNode;
			this.classNameBuilder.append(nameMatcher.toRuleString());
		}

		((NameAcceptor)parent).addName(this.getClassName());
	}

	public String getClassName() {
		return this.classNameBuilder.toString().replace('.', '/');
	}
}

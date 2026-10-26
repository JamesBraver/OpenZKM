package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import java.io.IOException;

public class DefaultExcludeInput extends AbstractScriptNode {
	public DefaultExcludeInput(int nodeType) {
		super(nodeType);
	}

	public ExclusionInclusionStatement getParsedStatement() {
		return this.getChildrenCount() > 0 ? (ExclusionInclusionStatement)this.getChild(0) : null;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childrenCount = this.getChildrenCount();

		for (int i = 0; i < childrenCount; i++) {
			this.getChild(i).parseArguments(this, scriptContext);
		}
	}
}

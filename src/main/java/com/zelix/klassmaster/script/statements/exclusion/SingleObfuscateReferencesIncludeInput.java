package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import java.io.IOException;

public class SingleObfuscateReferencesIncludeInput extends AbstractScriptNode {
	public SingleObfuscateReferencesIncludeInput(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childrenCount = this.getChildrenCount();

		for (int i = 0; i < childrenCount; i++) {
			this.getChild(i).parseArguments(this, scriptContext);
		}
	}

	public ObfuscateReferencesIncludeStatement getParsedStatement() {
		return this.getChildrenCount() > 0 ? (ObfuscateReferencesIncludeStatement)this.getChild(0) : null;
	}
}

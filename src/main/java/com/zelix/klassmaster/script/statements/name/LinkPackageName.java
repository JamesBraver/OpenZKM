package com.zelix.klassmaster.script.statements.name;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;

public class LinkPackageName extends AbstractScriptNode {
	public LinkPackageName(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		RenameFilterParameter filterParameter = (RenameFilterParameter)parent;
		int childrenCount = this.getChildrenCount();
		StringBuffer searchPathBuffer = new StringBuffer();

		for (int i = 0; i < childrenCount; i++) {
			searchPathBuffer.append(((TokenNode)this.getChild(i)).getText());
			if (i < childrenCount - 1) {
				searchPathBuffer.append("/");
			}
		}

		filterParameter.addSearchPath(searchPathBuffer.toString());
	}
}

package com.zelix.klassmaster.script.statements.clause;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.klassmaster.script.statements.token.StringLiteral;

public class JarQualifier extends AbstractScriptNode {
	public JarQualifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		this.getChildrenCount();
		String sourceFilePattern = ((StringLiteral)this.getChild(0)).getText();
		((RenameFilterParameter)parent).setSourceFilePattern(sourceFilePattern);
	}
}

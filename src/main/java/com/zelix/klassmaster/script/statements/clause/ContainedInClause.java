package com.zelix.klassmaster.script.statements.clause;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.DetailedExclusionStatement;
import com.zelix.klassmaster.script.statements.javaccInterface.ScriptLocation;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class ContainedInClause extends AbstractScriptNode implements ScriptLocation {
	private RenameFilterParameter filterParameter;

	public ContainedInClause(int nodeType) {
		super(nodeType);
	}

	public String toRuleString() {
		StringBuilder ruleText = new StringBuilder();
		int childCount = this.getChildrenCount();
		ruleText.append("containedIn");
		ruleText.append('{');
		if (childCount > 0) {
			RenameFilterParameter containedInClause = (RenameFilterParameter)this.getChild(0);
			ruleText.append(containedInClause.toRuleString());
		}

		ruleText.append('}');
		return ruleText.toString();
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		if (this.getChildrenCount() > 0) {
			this.filterParameter = (RenameFilterParameter)this.getChild(0);
			this.filterParameter.parseArguments(this, scriptContext);
		}
	}

	@Override
	public String getCommandName() {
		return ((DetailedExclusionStatement)super.parent).getCommandName();
	}

	@Override
	public int getLineNumber() {
		return ((DetailedExclusionStatement)super.parent).getLineNumber();
	}

	public RenameFilterParameter getFilterParameter() {
		return this.filterParameter;
	}
}

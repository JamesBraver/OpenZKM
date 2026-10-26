package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.ScriptLocation;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

public class Grouping extends AbstractScriptNode implements ScriptLocation {
	public Vector<RenameFilterParameter> filterParameters = new Vector<>();

	public Grouping(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return ((GroupingsStatement)super.parent).getCommandName();
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childrenCount = this.getChildrenCount();

		for (int i = 0; i < childrenCount; i++) {
			RenameFilterParameter filterParameter = (RenameFilterParameter)this.getChild(i);
			filterParameter.parseArguments(this, scriptContext);
			this.filterParameters.add(filterParameter);
		}
	}

	@Override
	public int getLineNumber() {
		return ((GroupingsStatement)super.parent).getLineNumber();
	}

	public Enumeration<RenameFilterParameter> getFilterParameters() {
		return this.filterParameters.elements();
	}
}

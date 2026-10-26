package com.zelix.klassmaster.script.statements.exclusion;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class SingleRenameFilterParameter extends ExclusionInclusionStatement {
	public SingleRenameFilterParameter(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChild(0).parseArguments(this, scriptContext);
	}

	@Override
	public String getCommandName() {
		return "Single exclude parameter";
	}

	@Override
	public void printStatementMessage(ScriptContext scriptContext) {
	}

	@Override
	public void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
	}

	public RenameFilterParameter getFilterParameter() {
		return (RenameFilterParameter)this.getChild(0);
	}
}

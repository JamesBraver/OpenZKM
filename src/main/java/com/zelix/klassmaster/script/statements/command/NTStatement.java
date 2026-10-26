package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.klassmaster.script.statements.base.ScriptStatementBase;
import java.io.IOException;

public class NTStatement extends ScriptStatementBase {
	protected ScriptCommand scriptCommand;

	public NTStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.scriptCommand = (ScriptCommand)this.getChild(0);
		this.scriptCommand.parseArguments(this, scriptContext);
	}
}

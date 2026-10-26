package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ExclusionInclusionStatementBase extends ScriptCommand {
	protected List<ScriptNode> filterParameters = new ArrayList<>();

	public ExclusionInclusionStatementBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();
		this.printStatementMessage(scriptContext);
		int childCount = this.getChildrenCount();
		this.collectFilterParameters(childCount, scriptContext);
		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	protected abstract void printStatementMessage(ScriptContext scriptContext);

	public void collectFilterParameters(int childCount, ScriptContext scriptContext) throws IOException {
		for (int i = 0; i < childCount; i++) {
			ScriptNode child = this.getChild(i);
			this.filterParameters.add(child);
			child.parseArguments(this, scriptContext);
		}
	}
}

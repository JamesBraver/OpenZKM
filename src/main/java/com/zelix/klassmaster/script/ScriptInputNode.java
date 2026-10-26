package com.zelix.klassmaster.script;

import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.OutputStatement;
import com.zelix.klassmaster.script.statements.base.ScriptStatementBase;
import com.zelix.klassmaster.script.statements.command.LoadStatement;
import com.zelix.klassmaster.script.statements.command.NTStatement;
import java.io.IOException;

public class ScriptInputNode extends AbstractScriptNode {
	public ScriptInputNode(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, scriptContext);
		}
	}

	public int getOpenStatementCount() {
		int openStatementCount = 0;
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			if ((ScriptStatementBase)((NTStatement)this.getChild(i)).getChild(0) instanceof LoadStatement) {
				openStatementCount++;
			}
		}

		return openStatementCount;
	}

	public int getOutputStatementCount() {
		int outputStatementCount = 0;
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			if ((ScriptStatementBase)((NTStatement)this.getChild(i)).getChild(0) instanceof OutputStatement) {
				outputStatementCount++;
			}
		}

		return outputStatementCount;
	}
}

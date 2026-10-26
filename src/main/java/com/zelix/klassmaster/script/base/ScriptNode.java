package com.zelix.klassmaster.script.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import java.io.IOException;

public interface ScriptNode {
	int getChildrenCount();

	void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException;

	void setChild(ScriptNode child, int index);

	void setParent(ScriptNode parent);

	ScriptNode getParent();

	ScriptCommand findScriptCommand();

	void onParseComplete();

	void onParseStart();

	ScriptNode getChild(int index);
}

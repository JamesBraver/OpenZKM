package com.zelix.klassmaster.script.statements.filter;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.FileFilterComponentBase;
import com.zelix.klassmaster.script.statements.javaccInterface.FileFilterComponentAcceptor;

public class AndFileFilterComponent extends AbstractScriptNode implements FileFilterComponentAcceptor {
	public AndFileFilterComponent(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		this.getChildrenCount();
		((FileFilterComponentBase)this.getChild(0)).parseArguments(this, scriptContext);
	}

	@Override
	public void addComponent(FileFilterComponentBase component) {
		((FileFilter)this.getParent()).appendToLastGroup(component);
	}
}

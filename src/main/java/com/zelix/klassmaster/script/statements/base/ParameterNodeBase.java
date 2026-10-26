package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.base.AbstractScriptNode;

public abstract class ParameterNodeBase extends AbstractScriptNode {
	public ParameterNodeBase(int nodeType) {
		super(nodeType);
	}

	protected abstract String getValue(int index);

	protected abstract int getValueCount();

	protected abstract String getParameterName();
}

package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;

public abstract class ActionParameter extends ParameterNodeBase {
	public ConfigurationParameter configurationParameter;

	public ActionParameter(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getValue(int index) {
		return this.configurationParameter.getValue(index);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		this.configurationParameter = (ConfigurationParameter)this.getChild(0);
		this.configurationParameter.parseArguments(this, scriptContext);
	}

	@Override
	public int getValueCount() {
		return this.configurationParameter.getValueCount();
	}

	@Override
	public String getParameterName() {
		return this.configurationParameter.getParameterName();
	}
}

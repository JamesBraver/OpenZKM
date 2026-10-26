package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;

public abstract class ConfigurationParameter extends ParameterNodeBase {
	private String parameterName;
	private TokenNode valueToken;

	public ConfigurationParameter(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getValue(int index) {
		if (index == 0) {
			return this.valueToken.getText();
		} else {
			throw new IllegalArgumentException(String.valueOf(index));
		}
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		this.valueToken = (TokenNode)this.getChild(0);
	}

	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}

	@Override
	protected int getValueCount() {
		return this.valueToken != null ? 1 : 0;
	}

	@Override
	public String getParameterName() {
		return this.parameterName;
	}
}

package com.zelix.klassmaster.script.statements.parameter;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ConfigurationParameter;
import com.zelix.klassmaster.script.statements.token.StringLiteral;
import java.util.ArrayList;

public class ChangeLogInParameter extends ConfigurationParameter {
	private ArrayList<String> values = new ArrayList<>();
	private boolean inReferencingClasses = false;

	public ChangeLogInParameter(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getValue(int index) {
		if (index < this.values.size()) {
			return this.values.get(index);
		} else {
			throw new IllegalArgumentException(String.valueOf(index));
		}
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			StringLiteral stringLiteral = (StringLiteral)this.getChild(i);
			this.values.add(stringLiteral.getText());
		}
	}

	public void markInReferencingClasses() {
		this.inReferencingClasses = true;
	}

	@Override
	protected int getValueCount() {
		return this.values != null ? this.values.size() : 0;
	}
}

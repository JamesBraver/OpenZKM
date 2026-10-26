package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;

public abstract class ChangeLogValueNodeBase extends ChangeLogsNodeBase {
	protected String value;

	public ChangeLogValueNodeBase(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
	}

	public String getValue() {
		return this.value;
	}
}

package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;

public abstract class ChangeLogEntryNodeBase extends ChangeLogsNodeBase {
	protected AbstractChangeLogContext context;

	public ChangeLogEntryNodeBase(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	protected abstract void applyToContext(AbstractChangeLogContext context);

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		this.context = context;
		int childCount = this.getChildCount();
		this.prepareParse(parent, context, childCount);

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).interpret(this, context);
		}

		this.applyToContext(context);
	}

	protected abstract void prepareParse(ChangeLogsNode parent, AbstractChangeLogContext context, int childCount);
}

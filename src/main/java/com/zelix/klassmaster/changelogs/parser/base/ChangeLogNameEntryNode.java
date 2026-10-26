package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;

public abstract class ChangeLogNameEntryNode extends ChangeLogsNodeBase implements NameReceiver {
	protected String name;

	public ChangeLogNameEntryNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public abstract void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context);

	@Override
	public final void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).interpret(this, context);
		}

		this.applyToParent(parent, context);
	}
}

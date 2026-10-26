package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;

public abstract class ChangeLogDataNodeBase extends ChangeLogsNodeBase {
	public ChangeLogDataNodeBase(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public final void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		ChangeLogFlowObfuscationNode flowObfuscationNode = (ChangeLogFlowObfuscationNode)parent;
		int childCount = this.getChildCount();

		for (int i = 0; i < childCount; i++) {
			String childValue = ((ChangeLogValueNodeBase)this.getChild(i)).getValue();
			flowObfuscationNode.addName(childValue);
		}
	}
}

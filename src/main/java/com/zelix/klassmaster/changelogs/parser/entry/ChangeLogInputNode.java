package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;

public class ChangeLogInputNode extends ChangeLogsNodeBase {
	public ChangeLogInputNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}
	}
}

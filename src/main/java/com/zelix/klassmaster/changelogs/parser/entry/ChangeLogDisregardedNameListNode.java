package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;

public class ChangeLogDisregardedNameListNode extends ChangeLogsNodeBase {
	public ChangeLogDisregardedNameListNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
	}
}

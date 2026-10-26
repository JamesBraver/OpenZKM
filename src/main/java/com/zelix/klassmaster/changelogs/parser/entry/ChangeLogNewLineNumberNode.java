package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogValueNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;

public class ChangeLogNewLineNumberNode extends ChangeLogsNodeBase {
	public ChangeLogNewLineNumberNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ChangeLogLineNumberChangeNode)parent).addNewLineNumber(((ChangeLogValueNodeBase)this.getChild(0)).getValue());
	}
}

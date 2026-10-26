package com.zelix.klassmaster.changelogs.parser.name;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameEntryNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogClassChangeNode;

public class ChangeLogNewClassNameNode extends ChangeLogNameEntryNode {
	public ChangeLogNewClassNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ChangeLogClassChangeNode)parent).setNewClassName(super.name);
	}
}

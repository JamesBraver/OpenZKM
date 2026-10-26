package com.zelix.klassmaster.changelogs.parser.name;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameEntryNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ClassNameReceiver;

public class ChangeLogOldClassNameNode extends ChangeLogNameEntryNode {
	public ChangeLogOldClassNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ClassNameReceiver)parent).setClassName(super.name);
	}
}

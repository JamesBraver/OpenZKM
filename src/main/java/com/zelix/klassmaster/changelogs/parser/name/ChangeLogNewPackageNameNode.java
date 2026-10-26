package com.zelix.klassmaster.changelogs.parser.name;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameEntryNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogPackageChangeNode;

public class ChangeLogNewPackageNameNode extends ChangeLogNameEntryNode {
	public ChangeLogNewPackageNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ChangeLogPackageChangeNode)parent).setNewPackageName(super.name);
	}
}

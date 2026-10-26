package com.zelix.klassmaster.changelogs.parser.name;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameEntryNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogPackageChangeNode;

public class ChangeLogOldPackageNameNode extends ChangeLogNameEntryNode {
	public ChangeLogOldPackageNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ChangeLogPackageChangeNode)parent).setOldPackageName(super.name);
	}
}

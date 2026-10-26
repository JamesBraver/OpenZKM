package com.zelix.klassmaster.changelogs.parser.name;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameEntryNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ModuleNameReceiver;

public class ChangeLogOldModuleNameNode extends ChangeLogNameEntryNode {
	public ChangeLogOldModuleNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ModuleNameReceiver)parent).setModuleName(super.name);
	}
}

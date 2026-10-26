package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogEntryNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ModuleNameReceiver;

public class ChangeLogModuleChangeNode extends ChangeLogEntryNodeBase implements ModuleNameReceiver {
	private String moduleName;

	public ChangeLogModuleChangeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	@Override
	protected void applyToContext(AbstractChangeLogContext context) {
	}

	@Override
	protected void prepareParse(ChangeLogsNode parent, AbstractChangeLogContext context, int childCount) {
	}
}

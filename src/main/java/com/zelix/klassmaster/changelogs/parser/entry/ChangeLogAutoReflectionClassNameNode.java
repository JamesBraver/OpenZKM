package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogEntryNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ClassNameReceiver;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ModuleNameReceiver;

public class ChangeLogAutoReflectionClassNameNode extends ChangeLogEntryNodeBase implements ClassNameReceiver, ModuleNameReceiver {
	private String moduleName;
	private String className;

	public ChangeLogAutoReflectionClassNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	@Override
	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	protected void applyToContext(AbstractChangeLogContext context) {
		if (this.moduleName != null && this.moduleName.length() > 0) {
			context.setModuleAutoReflectionClass(this.className, this.moduleName);
		} else {
			context.setAutoReflectionClass(this.className);
		}
	}

	@Override
	protected void prepareParse(ChangeLogsNode parent, AbstractChangeLogContext context, int childCount) {
	}
}

package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogEntryNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ClassNameReceiver;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ModuleNameReceiver;

public class ChangeLogObfuscateReferencesClassNameNode extends ChangeLogEntryNodeBase implements ClassNameReceiver, ModuleNameReceiver {
	private String className;
	private String moduleName;

	public ChangeLogObfuscateReferencesClassNameNode(int nodeKindIndex) {
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
			context.setModuleObfuscateRefsClass(this.className, this.moduleName);
		} else {
			context.setObfuscateReferencesClass(this.className);
		}
	}

	@Override
	protected void prepareParse(ChangeLogsNode parent, AbstractChangeLogContext context, int childCount) {
	}
}

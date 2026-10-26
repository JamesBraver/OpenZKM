package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogEntryNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ClassNameReceiver;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.ModuleNameReceiver;
import java.util.ArrayList;
import java.util.List;

public class ChangeLogMethodParameterClassNamesNode extends ChangeLogEntryNodeBase implements ClassNameReceiver, ModuleNameReceiver {
	private List<String> parameterChangeClasses = new ArrayList<>();
	private String moduleName;

	public ChangeLogMethodParameterClassNamesNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	@Override
	public void setClassName(String className) {
		this.parameterChangeClasses.add(className);
	}

	@Override
	protected void applyToContext(AbstractChangeLogContext context) {
		if (this.moduleName != null && this.moduleName.length() > 0) {
			context.setModuleParameterChangeClasses(this.parameterChangeClasses, this.moduleName);
		} else {
			context.setParameterChangeClasses(this.parameterChangeClasses);
		}
	}

	@Override
	protected void prepareParse(ChangeLogsNode parent, AbstractChangeLogContext context, int childCount) {
	}
}

package com.zelix.klassmaster.changelogs.parser.signature;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogMethodNameChangeNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;

public class ChangeLogParameterChangeDataNode extends ChangeLogsNodeBase implements NameReceiver {
	private String parameterChangeData;

	public ChangeLogParameterChangeDataNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setName(String name) {
		this.parameterChangeData = name;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}

		((ChangeLogMethodNameChangeNode)parent).setParameterChangeData(this.parameterChangeData);
	}
}

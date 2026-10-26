package com.zelix.klassmaster.changelogs.parser.signature;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogMethodNameChangeNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;

public class ChangeLogParameterChangeLookupDataNode extends ChangeLogsNodeBase implements NameReceiver {
	private String parameterChangeLookupData;

	public ChangeLogParameterChangeLookupDataNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setName(String name) {
		this.parameterChangeLookupData = name;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}

		((ChangeLogMethodNameChangeNode)parent).setParameterChangeLookupData(this.parameterChangeLookupData);
	}
}

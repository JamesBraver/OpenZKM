package com.zelix.klassmaster.changelogs.parser.name;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogFieldNameChangeNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;

public class ChangeLogNewFieldNameNode extends ChangeLogsNodeBase implements NameReceiver {
	private String newFieldName;

	public ChangeLogNewFieldNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setName(String name) {
		this.newFieldName = name;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		this.getChild(0).interpret(this, context);
		((ChangeLogFieldNameChangeNode)parent).setNewFieldName(this.newFieldName);
	}
}

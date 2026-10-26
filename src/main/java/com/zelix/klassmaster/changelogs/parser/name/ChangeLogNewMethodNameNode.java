package com.zelix.klassmaster.changelogs.parser.name;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogMethodNameChangeNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;

public class ChangeLogNewMethodNameNode extends ChangeLogsNodeBase implements NameReceiver {
	private String newMethodName;

	public ChangeLogNewMethodNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setName(String name) {
		this.newMethodName = name;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		this.getChild(0).interpret(this, context);
		((ChangeLogMethodNameChangeNode)parent).setNewMethodName(this.newMethodName);
	}
}

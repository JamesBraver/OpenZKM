package com.zelix.klassmaster.changelogs.parser.value;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogValueNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;

public class ChangeLogNameNode extends ChangeLogValueNodeBase {
	public ChangeLogNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((NameReceiver)parent).setName(this.getValue());
	}
}

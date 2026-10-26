package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogValueNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;

public class ChangeLogSourceNameNode extends ChangeLogsNodeBase implements NameReceiver {
	protected String sourceName;

	public ChangeLogSourceNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setName(String name) {
		this.sourceName = name;
	}

	@Override
	public final void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		ChangeLogsNodeBase childNode = (ChangeLogsNodeBase)this.getChild(0);
		childNode.interpret(this, context);
		if (childNode instanceof ChangeLogValueNodeBase) {
			this.sourceName = ((ChangeLogValueNodeBase)childNode).getValue();
		}

		this.applyToParent(parent, context);
	}

	public void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ChangeLogClassChangeNode)parent).setSourceName(this.sourceName);
	}
}

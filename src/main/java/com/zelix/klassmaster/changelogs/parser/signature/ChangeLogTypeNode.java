package com.zelix.klassmaster.changelogs.parser.signature;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogValueNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;

public class ChangeLogTypeNode extends ChangeLogValueNodeBase {
	public ChangeLogTypeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ChangeLogQualifiedTypeNode)parent).addNamePart(this.getValue());
	}
}

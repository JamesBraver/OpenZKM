package com.zelix.klassmaster.changelogs.parser.value;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogValueNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;

public class ChangeLogClassModifierNode extends ChangeLogValueNodeBase {
	public ChangeLogClassModifierNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
	}
}

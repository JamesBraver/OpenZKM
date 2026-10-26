package com.zelix.klassmaster.changelogs.parser.value;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameChangeNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogValueNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;

public class ChangeLogMemberModifierNode extends ChangeLogValueNodeBase {
	public ChangeLogMemberModifierNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		ChangeLogNameChangeNodeBase nameChangeNode = (ChangeLogNameChangeNodeBase)this.parent;
		if (this.value.equals("public")) {
			nameChangeNode.setPublic();
		} else if (this.value.equals("protected")) {
			nameChangeNode.makeProtected();
		} else if (this.value.equals("private")) {
			nameChangeNode.makePrivate();
		} else if (this.value.equals("static")) {
			nameChangeNode.markStatic();
		}
	}
}

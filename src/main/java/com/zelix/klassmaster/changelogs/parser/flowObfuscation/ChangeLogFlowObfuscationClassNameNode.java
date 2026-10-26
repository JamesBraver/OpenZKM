package com.zelix.klassmaster.changelogs.parser.flowObfuscation;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogFlowObfuscationNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameEntryNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;

public class ChangeLogFlowObfuscationClassNameNode extends ChangeLogNameEntryNode {
	public ChangeLogFlowObfuscationClassNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ChangeLogFlowObfuscationNode)parent).setClassName(super.name);
	}
}

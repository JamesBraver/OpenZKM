package com.zelix.klassmaster.changelogs.parser.flowObfuscation;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogFlowObfuscationNode;

public class ChangeLogMemberFlowObfuscationDataNode extends ChangeLogFlowObfuscationNode {
	public ChangeLogMemberFlowObfuscationDataNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	protected void applyToContext(AbstractChangeLogContext context) {
		if (super.className != null) {
			super.context.addMemberClass(super.className, super.nameList);
		}
	}
}

package com.zelix.klassmaster.changelogs.parser.flowObfuscation;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogFlowObfuscationNode;

public class ChangeLogMainFlowObfuscationDataNode extends ChangeLogFlowObfuscationNode {
	public ChangeLogMainFlowObfuscationDataNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	protected void applyToContext(AbstractChangeLogContext context) {
		if (super.className != null) {
			super.context.addTraceBackClass(super.className, super.nameList);
		}
	}
}

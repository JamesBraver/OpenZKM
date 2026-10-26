package com.zelix.klassmaster.changelogs.parser.flowObfuscation;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameEntryNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;

public class ChangeLogFlowObfuscationPackageNameNode extends ChangeLogNameEntryNode {
	public ChangeLogFlowObfuscationPackageNameNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void applyToParent(ChangeLogsNode parent, AbstractChangeLogContext context) {
		((ChangeLogPackageFlowObfuscationDataNode)parent).setPackageName(super.name);
	}
}

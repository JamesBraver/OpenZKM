package com.zelix.klassmaster.changelogs.parser.flowObfuscation;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogFlowObfuscationNode;

public class ChangeLogPackageFlowObfuscationDataNode extends ChangeLogFlowObfuscationNode {
	public String packageName;

	public ChangeLogPackageFlowObfuscationDataNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	protected void applyToContext(AbstractChangeLogContext context) {
		if (super.className != null) {
			if (this.packageName == null) {
				this.packageName = "";
			}

			super.context.addForwardClass(this.packageName, super.className, super.nameList);
		}
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
}

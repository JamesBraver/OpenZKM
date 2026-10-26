package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogEntryNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;

public class ChangeLogPackageChangeNode extends ChangeLogEntryNodeBase {
	public boolean packageNameNotChanged;
	public String oldPackageName;
	public String newPackageName;

	public ChangeLogPackageChangeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public void setPackageNameNotChanged() {
		this.packageNameNotChanged = true;
	}

	public void setOldPackageName(String oldPackageName) {
		this.oldPackageName = oldPackageName;
	}

	@Override
	protected void applyToContext(AbstractChangeLogContext context) {
		if (this.newPackageName == null) {
			if (this.packageNameNotChanged) {
				this.newPackageName = this.oldPackageName;
			} else {
				this.newPackageName = "";
			}
		}

		super.context.addPackageMapping(this.newPackageName, this.oldPackageName);
	}

	public void setNewPackageName(String newPackageName) {
		this.newPackageName = newPackageName;
	}

	@Override
	protected void prepareParse(ChangeLogsNode parent, AbstractChangeLogContext context, int childCount) {
		context.incrementPackageMappingCount();
	}
}

package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.framework.AccessNode;

public class ChangeLogNameChangeNodeBase extends ChangeLogsNodeBase {
	private boolean manufactured;
	private int accessFlags;

	public ChangeLogNameChangeNodeBase(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public int getAccessFlags() {
		return this.accessFlags;
	}

	public boolean getManufactured() {
		return this.manufactured;
	}

	public void makeProtected() {
		this.accessFlags = AccessNode.makeProtected(this.accessFlags);
	}

	public void setManufactured() {
		this.manufactured = true;
	}

	public void setPublic() {
		this.accessFlags = AccessNode.setPublic(this.accessFlags);
	}

	public void makePrivate() {
		this.accessFlags = AccessNode.makePrivate(this.accessFlags);
	}

	public void markStatic() {
		this.accessFlags = AccessNode.markStatic(this.accessFlags);
	}
}

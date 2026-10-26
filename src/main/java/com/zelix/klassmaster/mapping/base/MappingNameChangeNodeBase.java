package com.zelix.klassmaster.mapping.base;

public abstract class MappingNameChangeNodeBase extends MappingNodeBase {
	public String newName;
	public String oldName;

	public MappingNameChangeNodeBase(int nodeType) {
		super(nodeType);
	}

	public final String getOldName() {
		return this.oldName;
	}

	public final void setNewName(String newName) {
		this.newName = newName;
	}

	public final void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public final String getNewName() {
		return this.newName;
	}
}

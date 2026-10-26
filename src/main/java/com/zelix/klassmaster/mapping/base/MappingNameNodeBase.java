package com.zelix.klassmaster.mapping.base;

public abstract class MappingNameNodeBase extends MappingNodeBase {
	protected String name;

	public MappingNameNodeBase(int nodeType) {
		super(nodeType);
	}

	public final void setName(String name) {
		this.name = name.trim();
	}
}

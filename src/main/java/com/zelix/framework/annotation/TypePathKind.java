package com.zelix.framework.annotation;

public enum TypePathKind {
	ARRAY(0),
	NESTED(1),
	WILDCARD(2),
	TYPE(3);

	private final int index;

	private TypePathKind(int index) {
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}
}

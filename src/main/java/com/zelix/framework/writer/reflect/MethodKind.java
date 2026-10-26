package com.zelix.framework.writer.reflect;

public enum MethodKind {
	CONSTRUCTOR(1),
	INSTANCE(0),
	STATIC(-1);

	private final int value;

	private MethodKind(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}
}

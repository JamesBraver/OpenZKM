package com.zelix.framework.variable;

public enum LocalVarOperationType {
	INT_LOAD,
	INT_STORE,
	INT_INC,
	LONG_LOAD(true),
	LONG_STORE(true),
	FLOAT_LOAD,
	FLOAT_STORE,
	DOUBLE_LOAD(true),
	DOUBLE_STORE(true),
	OBJECT_LOAD,
	OBJECT_STORE,
	ADDRESS;

	private final boolean wide;

	private LocalVarOperationType(boolean wide) {
		this.wide = wide;
	}

	private LocalVarOperationType() {
		this(false);
	}

	public boolean isWide() {
		return this.wide;
	}
}

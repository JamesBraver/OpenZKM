package com.zelix.framework.variable;

public class LongVariableKey extends VariableKey {
	private final long value;

	public LongVariableKey(LocalVariableSlot variableSlot, long value) {
		super(variableSlot);
		this.value = value;
	}

	@Override
	public boolean hasLongValue() {
		return true;
	}

	public long getLongValue() {
		return this.value;
	}
}

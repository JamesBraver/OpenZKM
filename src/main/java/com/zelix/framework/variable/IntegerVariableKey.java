package com.zelix.framework.variable;

public class IntegerVariableKey extends VariableKey {
	private final int value;

	public IntegerVariableKey(LocalVariableSlot variableSlot, int value) {
		super(variableSlot);
		this.value = value;
	}

	public int getIntValue() {
		return this.value;
	}

	@Override
	public boolean hasLongValue() {
		return false;
	}
}

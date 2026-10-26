package com.zelix.framework.variable;

public abstract class VariableKey {
	public final LocalVariableSlot variableSlot;

	public VariableKey(LocalVariableSlot variableSlot) {
		this.variableSlot = variableSlot;
	}

	public abstract boolean hasLongValue();
}

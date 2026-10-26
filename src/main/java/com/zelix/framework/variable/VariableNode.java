package com.zelix.framework.variable;

public class VariableNode {
	private final LocalVariableSlot slot;
	private final int parameterIndex;

	public VariableNode(LocalVariableSlot slot, int parameterIndex) {
		this.slot = slot;
		this.parameterIndex = parameterIndex;
	}

	public LocalVariableSlot getVariableSlot() {
		return this.slot;
	}
}

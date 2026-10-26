package com.zelix.framework.variable;

public interface LocalVariableOperand {
	boolean hasFlag();

	boolean isWideSlotContinuation();

	boolean isWideSlotStart();

	int setIndex(int index);

	int getIndex();
}

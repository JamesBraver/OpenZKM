package com.zelix.framework.variable;

public interface LocalVariablesManager {
	LocalVariableSlot getOrCreateVariable(int index, LocalVarOperationType opType, int flags);
}

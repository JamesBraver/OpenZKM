package com.zelix.tools.parameterChanger.calls;

import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.variable.LocalVariableTable;
import java.util.ArrayList;

public class ExtraCallEntry {
	private final ArrayList<AbstractInsnNode> instructions;
	private final LocalVariableTable localVariableTable;

	public ExtraCallEntry(int initialCapacity, String methodDescriptor, boolean isStaticMethod) {
		this.instructions = new ArrayList<>(initialCapacity);
		this.localVariableTable = new LocalVariableTable(isStaticMethod, methodDescriptor, 5);
	}

	public LocalVariableTable getLocalVariableTable() {
		return this.localVariableTable;
	}

	public ArrayList<AbstractInsnNode> getInstructions() {
		return this.instructions;
	}
}

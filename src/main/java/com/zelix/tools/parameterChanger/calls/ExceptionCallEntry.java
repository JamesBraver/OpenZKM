package com.zelix.tools.parameterChanger.calls;

import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.variable.LocalVariableTable;
import java.util.ArrayList;

public class ExceptionCallEntry {
	private final ArrayList<AbstractInsnNode> instructions;
	private TryCatchBlockInfo[] tryCatchBlocks;
	private final LocalVariableTable localVariableTable;
	private int maxLocals;

	public ExceptionCallEntry(int initialCapacity) {
		this.instructions = new ArrayList<>(initialCapacity);
		this.tryCatchBlocks = new TryCatchBlockInfo[0];
		this.localVariableTable = new LocalVariableTable(true, "()V", 5);
	}

	public int getMaxLocals() {
		return this.maxLocals;
	}

	public TryCatchBlockInfo[] getTryCatchBlocks() {
		return this.tryCatchBlocks;
	}

	public ArrayList<AbstractInsnNode> getInstructions() {
		return this.instructions;
	}

	public void setTryCatchBlocks(TryCatchBlockInfo[] tryCatchBlocks) {
		this.tryCatchBlocks = tryCatchBlocks;
	}

	public int allocateLocalIndex() {
		return this.maxLocals++;
	}

	public LocalVariableTable getLocalVariableTable() {
		return this.localVariableTable;
	}
}

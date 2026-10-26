package com.zelix.framework.variable;

import com.zelix.framework.wrappers.MethodNode;

public class LocalVariableIndexCounter implements Indexable {
	private final MethodNode methodNode;
	private int index;
	private int initialIndex;
	private final LocalVariableTable localVariableTable;

	public LocalVariableIndexCounter(MethodNode methodNode) {
		this.methodNode = methodNode;
		this.index = methodNode.getMaxLocalsCompat();
		this.initialIndex = this.index;
		this.localVariableTable = methodNode.getLocalVariableTable();
	}

	@Override
	public int add(int increment) {
		this.index += increment;
		return this.index;
	}

	@Override
	public int increaseAndGet() {
		return ++this.index;
	}

	@Override
	public int get() {
		return this.index;
	}

	@Override
	public int getAndIncrease() {
		return this.index++;
	}
}

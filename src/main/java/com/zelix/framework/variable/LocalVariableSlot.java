package com.zelix.framework.variable;

public class LocalVariableSlot implements LocalVariableOperand {
	private int index = -1;
	private final int flag;
	private boolean wideSlotStart;
	private boolean wideSlotContinuation;

	public LocalVariableSlot(int index, boolean wideSlotStart, boolean wideSlotContinuation) {
		this(index, wideSlotStart, wideSlotContinuation, 0);
	}

	public LocalVariableSlot(int index, boolean wideSlotStart, boolean wideSlotContinuation, int flag) {
		this.index = index;
		this.flag = flag;
		this.wideSlotStart = wideSlotStart;
		this.wideSlotContinuation = wideSlotContinuation;
	}

	public LocalVariableSlot(int index) {
		this(index, 0);
	}

	public LocalVariableSlot(int index, int flag) {
		this.index = index;
		this.flag = flag;
	}

	public boolean needsWidePrefix() {
		return this.index > 255;
	}

	@Override
	public boolean hasFlag() {
		return this.flag != 0;
	}

	public void setAsWideSlotContinuation() {
		this.wideSlotContinuation = true;
	}

	@Override
	public boolean isWideSlotContinuation() {
		return this.wideSlotContinuation;
	}

	@Override
	public boolean isWideSlotStart() {
		return this.wideSlotStart;
	}

	public void setAsWideSlotStart() {
		this.wideSlotStart = true;
	}

	@Override
	public int setIndex(int index) {
		int previousIndex = this.index;
		this.index = index;
		return previousIndex;
	}

	@Override
	public int getIndex() {
		return this.index;
	}

	public void clearWideSlotStartFlag() {
		this.wideSlotStart = false;
	}

	public void clearWideSlotContinuation() {
		this.wideSlotContinuation = false;
	}
}

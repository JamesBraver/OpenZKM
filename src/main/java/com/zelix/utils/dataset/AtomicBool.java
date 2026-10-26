package com.zelix.utils.dataset;

public class AtomicBool {
	protected boolean value;

	public AtomicBool() {
		this(false);
	}

	public AtomicBool(boolean initialValue) {
		this.value = initialValue;
	}

	public void set(boolean value) {
		this.value = value;
	}

	@Override
	public Object clone() {
		return new AtomicBool(this.value);
	}

	public boolean get() {
		return this.value;
	}
}

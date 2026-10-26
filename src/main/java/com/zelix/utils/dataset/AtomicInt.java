package com.zelix.utils.dataset;

import com.zelix.framework.variable.Indexable;
import java.io.Serializable;

public class AtomicInt implements Indexable, Serializable, Comparable<AtomicInt> {
	private int intValue;

	public AtomicInt() {
		this(0);
	}

	public AtomicInt(int initialValue) {
		this.intValue = initialValue;
	}

	@Override
	public int add(int increment) {
		this.intValue += increment;
		return this.intValue;
	}

	public int compare(AtomicInt other) {
		if (this.intValue < other.intValue) {
			return -1;
		} else {
			return this.intValue == other.intValue ? 0 : 1;
		}
	}

	@Override
	public int increaseAndGet() {
		return ++this.intValue;
	}

	public void set(int value) {
		this.intValue = value;
	}

	@Override
	public int compareTo(AtomicInt other) {
		return this.compare(other);
	}

	public int decreaseAndGet() {
		this.intValue--;
		return this.intValue;
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof AtomicInt ? this.intValue == ((AtomicInt)obj).intValue : false;
	}

	@Override
	public int get() {
		return this.intValue;
	}

	@Override
	public int hashCode() {
		return this.intValue;
	}

	@Override
	public int getAndIncrease() {
		return this.intValue++;
	}
}

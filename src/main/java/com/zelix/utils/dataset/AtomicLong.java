package com.zelix.utils.dataset;

import java.io.Serializable;

public class AtomicLong implements Serializable, Comparable<AtomicLong> {
	protected long longValue;

	public AtomicLong() {
		this(0L);
	}

	public AtomicLong(long initialValue) {
		this.longValue = initialValue;
	}

	@Override
	public int compareTo(AtomicLong other) {
		return this.compare(other);
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof AtomicLong ? this.longValue == ((AtomicLong)obj).longValue : false;
	}

	@Override
	public int hashCode() {
		return (int)this.longValue;
	}

	public int compare(AtomicLong other) {
		if (this.longValue < other.longValue) {
			return -1;
		} else {
			return this.longValue == other.longValue ? 0 : 1;
		}
	}

	public void set(long value) {
		this.longValue = value;
	}

	public long get() {
		return this.longValue;
	}
}

package com.zelix.utils.dataset;

public class Pair {
	private final Object key;
	private Object value;

	public Pair(Object key) {
		if (key == null) {
			throw new IllegalArgumentException(this.getClass().getName());
		} else {
			this.key = key;
		}
	}

	public Pair(Object key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException(this.getClass().getName());
		} else {
			this.key = key;
			this.value = value;
		}
	}

	public Object getKey() {
		return this.key;
	}

	public Object getValue() {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair)) {
			return false;
		} else {
			Pair pair = (Pair)obj;
			return this.key.equals(pair.key) && (this.value == null && pair.value == null || this.value != null && pair.value != null && this.value.equals(pair.value));
		}
	}

	public Object setValue(Object value) {
		Object oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	@Override
	public int hashCode() {
		return this.value == null ? this.key.hashCode() : this.key.hashCode() ^ this.value.hashCode();
	}
}

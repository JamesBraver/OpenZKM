package com.zelix.utils.collections;

import java.io.Serializable;

public class KeyValuePair<K, V> implements Serializable {
	private K key;
	private V value;

	public KeyValuePair(K key, V value) {
		if (key == null) {
			throw new IllegalArgumentException(this.getClass().getName());
		} else {
			this.key = key;
			this.value = value;
		}
	}

	public K getKey() {
		return this.key;
	}

	public V getValue() {
		return this.value;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof KeyValuePair)) {
			return false;
		} else {
			KeyValuePair<?, ?> otherPair = (KeyValuePair<?, ?>)obj;
			return this.key.equals(otherPair.key) && (this.value == null && otherPair.value == null || this.value != null && otherPair.value != null && this.value.equals(otherPair.value));
		}
	}

	@Override
	public int hashCode() {
		return this.value == null ? this.key.hashCode() : this.key.hashCode() ^ this.value.hashCode();
	}

	public K setKey(K newKey) {
		if (newKey == null) {
			throw new IllegalArgumentException(this.getClass().getName());
		} else {
			K oldKey = this.key;
			this.key = newKey;
			return oldKey;
		}
	}

	public V setValue(V newValue) {
		V oldValue = this.value;
		this.value = newValue;
		return oldValue;
	}
}

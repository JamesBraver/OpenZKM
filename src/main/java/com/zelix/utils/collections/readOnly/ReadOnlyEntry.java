package com.zelix.utils.collections.readOnly;

import com.zelix.utils.collections.KeyValuePair;
import java.util.Map.Entry;

public class ReadOnlyEntry<K, V> implements Entry<K, V> {
	private final K key;
	private final V value;

	public ReadOnlyEntry(KeyValuePair<K, V> pair) {
		this.key = pair.getKey();
		this.value = pair.getValue();
	}

	public ReadOnlyEntry(K key, V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ReadOnlyEntry)) {
			return false;
		} else {
			ReadOnlyEntry<?, ?> otherEntry = (ReadOnlyEntry<?, ?>)obj;
			return this.key == null ? otherEntry.key == null : this.key.equals(otherEntry.key) && (this.value == null ? otherEntry.value == null : this.value.equals(otherEntry.value));
		}
	}

	@Override
	public K getKey() {
		return this.key;
	}

	@Override
	public V getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value == null ? 0 : this.value.hashCode());
	}

	@Override
	public V setValue(V value) {
		throw new UnsupportedOperationException();
	}
}

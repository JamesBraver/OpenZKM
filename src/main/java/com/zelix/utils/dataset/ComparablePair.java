package com.zelix.utils.dataset;

import java.io.Serializable;

public class ComparablePair<K extends Comparable<K>, V> implements Serializable, Comparable<ComparablePair<K, V>> {
	public K key;
	public V value;

	public ComparablePair(K key, V value) {
		if (key == null) {
			throw new IllegalArgumentException(this.getClass().getName());
		} else {
			this.key = key;
			this.value = value;
		}
	}

	public int compareByKey(ComparablePair<K, V> pair) {
		return this.key.compareTo(pair.key);
	}

	@Override
	public int compareTo(ComparablePair<K, V> other) {
		return this.compareByKey(other);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ComparablePair)) {
			return false;
		} else {
			ComparablePair<?, ?> comparablePair = (ComparablePair<?, ?>)obj;
			return this.key.equals(comparablePair.key) && (this.value == null && comparablePair.value == null || this.value != null && comparablePair.value != null && this.value.equals(comparablePair.value));
		}
	}

	@Override
	public int hashCode() {
		return this.value == null ? this.key.hashCode() : this.key.hashCode() ^ this.value.hashCode();
	}

	public K getKey() {
		return this.key;
	}
}

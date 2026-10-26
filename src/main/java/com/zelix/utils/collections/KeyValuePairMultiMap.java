package com.zelix.utils.collections;

import com.zelix.utils.enumerations.SnapshotEnumerator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class KeyValuePairMultiMap<T, K, V> {
	public MultiMap<T, KeyValuePair<K, V>> backingMultiMap;

	public KeyValuePairMultiMap(boolean threadSafe) {
		this.backingMultiMap = new MultiMap<>(threadSafe);
	}

	public KeyValuePairMultiMap(int initialCapacity, boolean threadSafe) {
		this.backingMultiMap = new MultiMap<>(initialCapacity, threadSafe);
	}

	public KeyValuePairMultiMap(int initialCapacity) {
		this(initialCapacity, false);
	}

	public KeyValuePairMultiMap() {
		this(false);
	}

	public void clear() {
		this.backingMultiMap.clear();
	}

	public boolean isEmpty() {
		return this.backingMultiMap.isEmpty();
	}

	public Set<Entry<T, List<KeyValuePair<K, V>>>> entrySet() {
		return this.backingMultiMap.entrySet();
	}

	public Enumeration<T> keys() {
		return new SnapshotEnumerator<>(this.backingMultiMap.keySet());
	}

	public List<KeyValuePair<K, V>> get(T key) {
		return this.backingMultiMap.get(key);
	}

	public int valueCount() {
		return this.backingMultiMap.valueCount();
	}

	public int size() {
		return this.backingMultiMap.size();
	}

	public void put(T mainKey, K pairKey, V pairValue) {
		KeyValuePair<K, V> pair = new KeyValuePair<>(pairKey, pairValue);
		this.backingMultiMap.put(mainKey, pair);
	}

	public Set<T> keySet() {
		return this.backingMultiMap.keySet();
	}
}

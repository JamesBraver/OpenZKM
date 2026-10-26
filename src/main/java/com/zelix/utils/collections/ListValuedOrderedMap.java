package com.zelix.utils.collections;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

public class ListValuedOrderedMap<K, V> {
	private int defaultListSize = 5;
	private OrderedMap<K, List<V>> backingStore;

	public ListValuedOrderedMap() {
		this(75, 5);
	}

	public ListValuedOrderedMap(int initialCapacity) {
		this(initialCapacity, 5);
	}

	public ListValuedOrderedMap(int initialCapacity, int defaultListSize) {
		this.defaultListSize = defaultListSize;
		this.backingStore = new OrderedMap<>(initialCapacity);
	}

	public synchronized Enumeration<K> keysEnumeration() {
		return this.backingStore.keysEnumeration();
	}

	public void putList(K key, List<V> values) {
		this.backingStore.put(key, values);
	}

	public boolean containsKey(Object key) {
		return this.backingStore.containsKey(key);
	}

	public int size() {
		return this.backingStore.size();
	}

	@Override
	public Object clone() {
		int entryCount = this.backingStore.size();
		ListValuedOrderedMap<K, V> clonedMap = new ListValuedOrderedMap<>(entryCount);

		for (Entry<K, List<V>> entry : this.backingStore.entrySet()) {
			ArrayList<V> copiedValues = new ArrayList<>(entry.getValue());
			clonedMap.putList(entry.getKey(), copiedValues);
		}

		return clonedMap;
	}

	public List<V> get(Object key) {
		return this.backingStore.get(key);
	}

	public void add(K key, V value) {
		List<V> values = this.backingStore.get(key);
		if (values == null) {
			ArrayList<V> newValues = new ArrayList<>(this.defaultListSize);
			newValues.add(value);
			this.backingStore.put(key, newValues);
		} else {
			values.add(value);
		}
	}
}

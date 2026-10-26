package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import java.util.IdentityHashMap;
import java.util.Set;

public class IdentityMap<K, V> {
	private final IdentityHashMap<K, V> backingMap;

	public IdentityMap(int initialCapacity) {
		this.backingMap = new IdentityHashMap<>(GlobalUtils.getInitialCapacity(initialCapacity));
	}

	public IdentityMap() {
		this.backingMap = new IdentityHashMap<>();
	}

	public Set<K> keySet() {
		return this.backingMap.keySet();
	}

	public V put(K key, V value) {
		return this.backingMap.put(key, value);
	}

	public void clear() {
		this.backingMap.clear();
	}

	public V remove(Object key) {
		return this.backingMap.remove(key);
	}

	public boolean containsKey(Object key) {
		return this.backingMap.containsKey(key);
	}

	public int size() {
		return this.backingMap.size();
	}

	public V get(Object key) {
		return this.backingMap.get(key);
	}

	public boolean isEmpty() {
		return this.backingMap.isEmpty();
	}
}

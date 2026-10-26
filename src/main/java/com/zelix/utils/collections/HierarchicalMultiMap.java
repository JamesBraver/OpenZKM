package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.enumerations.SnapshotEnumerator;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HierarchicalMultiMap<K1, K2, V> {
	public int defaultInnerMapCapacity;
	public int defaultInnerValueCapacity;
	private final boolean isConcurrent;
	public Map<K1, MultiMap<K2, V>> rootMap;

	public HierarchicalMultiMap(int mapCapacity, int innerValueCapacity, int innerMapCapacity, boolean useConcurrent) {
		this.defaultInnerMapCapacity = innerValueCapacity;
		this.defaultInnerValueCapacity = innerMapCapacity;
		this.isConcurrent = useConcurrent;
		if (useConcurrent) {
			this.rootMap = new ConcurrentHashMap<>(GlobalUtils.getInitialCapacity(mapCapacity));
		} else {
			this.rootMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(mapCapacity));
		}
	}

	public HierarchicalMultiMap(int mapCapacity) {
		this(mapCapacity, 10, 5, false);
	}

	public HierarchicalMultiMap(boolean useConcurrent) {
		this(75, 10, 5, useConcurrent);
	}

	public HierarchicalMultiMap(int mapCapacity, int innerValueCapacity, boolean useConcurrent) {
		this(mapCapacity, innerValueCapacity, 5, useConcurrent);
	}

	public HierarchicalMultiMap(int mapCapacity, int innerValueCapacity, int innerMapCapacity) {
		this(mapCapacity, innerValueCapacity, innerMapCapacity, false);
	}

	public HierarchicalMultiMap() {
		this(75, 10, 5, false);
	}

	public Set<Entry<K1, MultiMap<K2, V>>> entrySet() {
		return this.rootMap.entrySet();
	}

	public synchronized void putAll(K1 key, K2 innerKey, Collection<V> values) {
		MultiMap<K2, V> innerMap = this.rootMap.get(key);
		if (innerMap == null) {
			innerMap = new MultiMap<>(false, this.defaultInnerMapCapacity, values.size(), this.isConcurrent);
			this.rootMap.put(key, innerMap);
		}

		for (V value : values) {
			innerMap.put(innerKey, value);
		}
	}

	public MultiMap<K2, V> get(K1 key) {
		return this.rootMap.get(key);
	}

	public Set<K1> keySet() {
		return this.rootMap.keySet();
	}

	public synchronized boolean containsInnerKey(K1 key, K2 innerKey) {
		MultiMap<K2, V> innerMap = this.rootMap.get(key);
		return innerMap != null && innerMap.containsKey(innerKey);
	}

	public synchronized boolean remove(K1 key, K2 innerKey, V value) {
		MultiMap<K2, V> innerMap = this.rootMap.get(key);
		return innerMap != null && innerMap.remove(innerKey, value);
	}

	public synchronized List<V> getValues(K1 key, K2 innerKey) {
		MultiMap<K2, V> innerMap = this.rootMap.get(key);
		return innerMap == null ? null : innerMap.get(innerKey);
	}

	public MultiMap<K2, V> put(K1 key, MultiMap<K2, V> innerMap) {
		return this.rootMap.put(key, innerMap);
	}

	public synchronized void clear() {
		Iterator<MultiMap<K2, V>> innerMapIterator = this.rootMap.values().iterator();

		while (innerMapIterator.hasNext()) {
			innerMapIterator.next().clear();
		}

		this.rootMap.clear();
	}

	public synchronized void put(K1 key, K2 innerKey, V value) {
		MultiMap<K2, V> innerMap = this.rootMap.get(key);
		if (innerMap == null) {
			innerMap = new MultiMap<>(false, this.defaultInnerMapCapacity, this.defaultInnerValueCapacity, this.isConcurrent);
			innerMap.put(innerKey, value);
			this.rootMap.put(key, innerMap);
		} else {
			innerMap.put(innerKey, value);
		}
	}

	public synchronized List<V> removeAll(K1 key, K2 innerKey) {
		MultiMap<K2, V> innerMap = this.rootMap.get(key);
		return innerMap != null ? innerMap.removeAll(innerKey) : null;
	}

	public MultiMap<K2, V> remove(K1 key) {
		return this.rootMap.remove(key);
	}

	public int size() {
		return this.rootMap.size();
	}

	public boolean isEmpty() {
		return this.rootMap.isEmpty();
	}

	public synchronized boolean containsEntry(K1 key, K2 innerKey, V value) {
		MultiMap<K2, V> innerMap = this.rootMap.get(key);
		return innerMap != null && innerMap.containsEntry(innerKey, value);
	}

	public boolean containsKey(K1 key) {
		return this.rootMap.containsKey(key);
	}

	public Enumeration<K1> keys() {
		return new SnapshotEnumerator<>(this.rootMap.keySet());
	}
}

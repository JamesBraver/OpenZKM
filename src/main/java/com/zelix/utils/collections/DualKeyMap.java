package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class DualKeyMap<I, K, V> {
	private boolean ordered;
	private boolean concurrent;
	private int outerCapacity;
	private int innerCapacity;
	public Map<I, Map<K, V>> map;

	public DualKeyMap(int outerCapacity, boolean useConcurrent) {
		this(outerCapacity, 15, useConcurrent);
	}

	public DualKeyMap(int outerCapacity) {
		this(outerCapacity, 15, false);
	}

	public DualKeyMap() {
		this(75, 15, false);
	}

	public DualKeyMap(int outerCapacity, int innerCapacity) {
		this(outerCapacity, innerCapacity, false);
	}

	public DualKeyMap(boolean useConcurrent) {
		this(75, 15, useConcurrent);
	}

	public DualKeyMap(int outerCapacity, int innerCapacity, boolean useConcurrent) {
		this(false, outerCapacity, innerCapacity, useConcurrent);
	}

	public DualKeyMap(boolean ordered, int outerCapacity, int innerCapacity, boolean useConcurrent) {
		this.ordered = ordered;
		this.concurrent = useConcurrent;
		this.outerCapacity = GlobalUtils.getInitialCapacity(outerCapacity);
		this.innerCapacity = GlobalUtils.getInitialCapacity(innerCapacity);
		if (ordered) {
			this.map = new LinkedHashMap<>(this.outerCapacity);
		} else if (useConcurrent) {
			this.map = new ConcurrentHashMap<>(this.outerCapacity);
		} else {
			this.map = GlobalUtils.newHashMap(this.outerCapacity);
		}
	}

	public DualKeyMap(boolean ordered, boolean useConcurrent) {
		this(ordered, 75, 15, useConcurrent);
	}

	public Map<K, V> createInnerMap() {
		if (this.ordered) {
			return new LinkedHashMap<>(this.innerCapacity);
		}

		return (Map<K, V>)(this.concurrent ? new ConcurrentHashMap<>(this.innerCapacity) : GlobalUtils.newHashMap(this.innerCapacity));
	}

	public int getGroupCount() {
		return this.map.size();
	}

	public void clearAll() {
		Iterator<Map<K, V>> innerMapIterator = this.map.values().iterator();

		while (innerMapIterator.hasNext()) {
			innerMapIterator.next().clear();
		}

		this.map.clear();
	}

	public int getTotalEntryCount() {
		int totalCount = 0;

		for (Map<K, V> innerMap : this.map.values()) {
			if (innerMap != null) {
				totalCount += innerMap.size();
			}
		}

		return totalCount;
	}

	public V get(I groupKey, K innerKey) {
		Map<K, V> innerMap = this.map.get(groupKey);
		return innerMap == null ? null : innerMap.get(innerKey);
	}

	public Map<K, V> removeGroup(Object groupKey) {
		return this.map.remove(groupKey);
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public Enumeration<I> getGroupKeys() {
		return Collections.enumeration(this.map.keySet());
	}

	public Enumeration<Map<K, V>> getGroups() {
		return Collections.enumeration(this.map.values());
	}

	public Set<Entry<I, Map<K, V>>> getEntrySet() {
		return this.map.entrySet();
	}

	public DualKeyMap<I, K, V> clone() {
		int groupCount = this.map.size();
		DualKeyMap<I, K, V> clonedMap = new DualKeyMap<>(this.ordered, groupCount * 2 + 1, this.innerCapacity, this.concurrent);

		for (Entry<I, Map<K, V>> entry : this.map.entrySet()) {
			clonedMap.putGroup(entry.getKey(), this.copyMap(entry.getValue()));
		}

		return clonedMap;
	}

	public boolean contains(I groupKey, K innerKey) {
		Map<K, V> innerMap = this.map.get(groupKey);
		return innerMap == null ? false : innerMap.containsKey(innerKey);
	}

	public Map<K, V> getGroup(Object groupKey) {
		return this.map.get(groupKey);
	}

	public V put(I groupKey, K innerKey, V value) {
		V previousValue = null;
		Map<K, V> innerMap = this.map.get(groupKey);
		if (innerMap == null) {
			innerMap = this.createInnerMap();
			innerMap.put(innerKey, value);
			this.map.put(groupKey, innerMap);
		} else {
			previousValue = innerMap.put(innerKey, value);
		}

		return previousValue;
	}

	public Enumeration<V> getAllValues() {
		HashSet<V> allValues = GlobalUtils.newHashSet();
		Iterator<Map<K, V>> innerMapIterator = this.map.values().iterator();

		while (innerMapIterator.hasNext()) {
			Iterator<V> valueIterator = innerMapIterator.next().values().iterator();

			while (valueIterator.hasNext()) {
				allValues.add(valueIterator.next());
			}
		}

		return Collections.enumeration(allValues);
	}

	public V remove(I groupKey, K innerKey) {
		V removedValue = null;
		Map<K, V> innerMap = this.map.get(groupKey);
		if (innerMap != null) {
			removedValue = innerMap.remove(innerKey);
			if (innerMap.size() == 0) {
				this.map.remove(groupKey);
			}
		}

		return removedValue;
	}

	public Set<I> getGroupKeySet() {
		return this.map.keySet();
	}

	public Map<K, V> putGroup(I groupKey, Map<K, V> innerMap) {
		return this.map.put(groupKey, this.copyMap(innerMap));
	}

	public boolean containsGroup(I groupKey) {
		return this.map.containsKey(groupKey);
	}

	public Map<K, V> copyMap(Map<K, V> mapToCopy) {
		if (this.ordered) {
			return new LinkedHashMap<>(mapToCopy);
		}

		Map<K, V> copiedMap;
		if (this.concurrent) {
			copiedMap = new ConcurrentHashMap<>(mapToCopy);
		} else {
			copiedMap = GlobalUtils.newHashMap(mapToCopy);
		}

		return copiedMap;
	}
}

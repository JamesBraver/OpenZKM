package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.readOnly.ReadOnlyEntry;
import com.zelix.utils.enumerations.SnapshotEnumerator;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ThreeLevelMultiMap<K1, K2, K3, V> {
	public int defaultL2MapCapacity;
	public int defaultL3ValueListCapacity;
	public int defaultL3MapCapacity;
	public AbstractMap<K1, HierarchicalMultiMap<K2, K3, V>> rootMap;
	public boolean innerMapsConcurrent;

	public ThreeLevelMultiMap(int mapCapacity, int level2MapCapacity, int level3ValueCapacity) {
		this(mapCapacity, level2MapCapacity, level3ValueCapacity, 5);
	}

	public ThreeLevelMultiMap(int mapCapacity, int level2MapCapacity, int level3ValueCapacity, int level3MapCapacity) {
		this(mapCapacity, level2MapCapacity, level3ValueCapacity, level3MapCapacity, true);
	}

	public ThreeLevelMultiMap(int mapCapacity, int level2MapCapacity, int level3ValueCapacity, int level3MapCapacity, boolean useConcurrent) {
		this.defaultL2MapCapacity = level2MapCapacity;
		this.defaultL3ValueListCapacity = level3ValueCapacity;
		this.defaultL3MapCapacity = level3MapCapacity;
		if (useConcurrent) {
			this.rootMap = new ConcurrentHashMap<>(GlobalUtils.getInitialCapacity(mapCapacity));
		} else {
			this.rootMap = new LinkedHashMap<>(GlobalUtils.getInitialCapacity(mapCapacity));
		}
	}

	public ThreeLevelMultiMap() {
		this(75, 10, 5);
	}

	public ThreeLevelMultiMap(int mapCapacity) {
		this(mapCapacity, 10, 5, 5);
	}

	public synchronized boolean containsInnerKey(K1 key, K2 level2Key, K3 level3Key) {
		HierarchicalMultiMap<K2, K3, V> level2Map = this.rootMap.get(key);
		return level2Map == null ? false : level2Map.containsInnerKey(level2Key, level3Key);
	}

	public synchronized Set<Entry<K1, HierarchicalMultiMap<K2, K3, V>>> entrySet() {
		LinkedHashSet<Entry<K1, HierarchicalMultiMap<K2, K3, V>>> entries = new LinkedHashSet<>();

		for (Entry<K1, HierarchicalMultiMap<K2, K3, V>> entry : this.rootMap.entrySet()) {
			entries.add(new ReadOnlyEntry<>(entry.getKey(), entry.getValue()));
		}

		return Collections.unmodifiableSet(entries);
	}

	public synchronized boolean containsKey(K1 key) {
		return this.rootMap.containsKey(key);
	}

	public synchronized void put(K1 key, K2 level2Key, K3 level3Key, V value) {
		HierarchicalMultiMap<K2, K3, V> level2Map = this.rootMap.get(key);
		if (level2Map == null) {
			level2Map = (HierarchicalMultiMap<K2, K3, V>)(new HierarchicalMultiMap<>(
				this.defaultL2MapCapacity, this.defaultL3ValueListCapacity, this.defaultL3MapCapacity, this.innerMapsConcurrent
			));
			level2Map.put(level2Key, level3Key, value);
			this.rootMap.put(key, level2Map);
		} else {
			level2Map.put(level2Key, level3Key, value);
		}
	}

	public HierarchicalMultiMap<K2, K3, V> get(K1 key) {
		return this.rootMap.get(key);
	}

	public int size() {
		return this.rootMap.size();
	}

	public synchronized List<V> getValues(K1 key, K2 level2Key, K3 level3Key) {
		HierarchicalMultiMap<K2, K3, V> level2Map = this.rootMap.get(key);
		if (level2Map == null) {
			return null;
		} else {
			MultiMap<K3, V> level3Map = level2Map.get(level2Key);
			return level3Map == null ? null : level3Map.get(level3Key);
		}
	}

	public Enumeration<K1> keys() {
		return new SnapshotEnumerator<>(this.rootMap.keySet());
	}
}

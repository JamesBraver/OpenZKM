package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TripleKeyMap<K1, K2, K3, V> {
	public int nestedGroupMapCapacity;
	public int nestedLeafMapCapacity;
	public ConcurrentHashMap<K1, DualKeyMap<K2, K3, V>> rootMap;

	public TripleKeyMap(int mapCapacity, int groupMapCapacity) {
		this(mapCapacity, groupMapCapacity, 5);
	}

	public TripleKeyMap(int mapCapacity) {
		this(mapCapacity, 10, 5);
	}

	public TripleKeyMap(int mapCapacity, int groupMapCapacity, int leafMapCapacity) {
		this.nestedGroupMapCapacity = groupMapCapacity;
		this.nestedLeafMapCapacity = leafMapCapacity;
		this.rootMap = new ConcurrentHashMap<>(GlobalUtils.getInitialCapacity(mapCapacity));
	}

	public V remove(K1 key, K2 groupKey, K3 leafKey) {
		DualKeyMap<K2, K3, V> dualKeyMap = this.rootMap.get(key);
		if (dualKeyMap != null) {
			V removedValue = dualKeyMap.remove(groupKey, leafKey);
			if (dualKeyMap.getGroupCount() == 0) {
				this.rootMap.remove(key);
			}

			return removedValue;
		} else {
			return null;
		}
	}

	public DualKeyMap<K2, K3, V> getDualKeyMap(K1 key) {
		return this.rootMap.get(key);
	}

	public boolean containsEntry(K1 key, K2 groupKey, K3 leafKey) {
		DualKeyMap<K2, K3, V> dualKeyMap = this.rootMap.get(key);
		return dualKeyMap != null && dualKeyMap.contains(groupKey, leafKey);
	}

	public V put(K1 key, K2 groupKey, K3 leafKey, V value) {
		DualKeyMap<K2, K3, V> dualKeyMap = this.rootMap.get(key);
		if (dualKeyMap == null) {
			dualKeyMap = new DualKeyMap<>(this.nestedGroupMapCapacity, this.nestedLeafMapCapacity);
			V previousValue = dualKeyMap.put(groupKey, leafKey, value);
			this.rootMap.put(key, dualKeyMap);
			return previousValue;
		} else {
			return dualKeyMap.put(groupKey, leafKey, value);
		}
	}

	public Enumeration<K1> keys() {
		return this.rootMap.keys();
	}

	public V get(K1 key, K2 groupKey, K3 leafKey) {
		DualKeyMap<K2, K3, V> dualKeyMap = this.rootMap.get(key);
		if (dualKeyMap == null) {
			return null;
		} else {
			Map<K3, V> leafMap = dualKeyMap.getGroup(groupKey);
			return leafMap == null ? null : leafMap.get(leafKey);
		}
	}

	public Map<K3, V> getGroup(K1 key, K2 groupKey) {
		DualKeyMap<K2, K3, V> dualKeyMap = this.rootMap.get(key);
		return dualKeyMap == null ? null : dualKeyMap.getGroup(groupKey);
	}

	public boolean containsGroup(K1 key, K2 groupKey) {
		DualKeyMap<K2, K3, V> dualKeyMap = this.rootMap.get(key);
		return dualKeyMap != null && dualKeyMap.containsGroup(groupKey);
	}

	public DualKeyMap<K2, K3, V> removeDualKeyMap(K1 key) {
		return this.rootMap.remove(key);
	}
}

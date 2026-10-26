package com.zelix.utils.collections;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class HierarchicalMultiMapWrapper<K1, K2, V> {
	public final HierarchicalMultiMap<K1, K2, V> groupedMap;

	public HierarchicalMultiMapWrapper(HierarchicalMultiMap<K1, K2, V> groupedMap) {
		this.groupedMap = groupedMap;
	}

	public MultiMap<K2, V> flattenToMultiMap() {
		MultiMap<K2, V> flatMultiMap = new MultiMap<>(this.groupedMap.size() * 2);
		Iterator<Entry<K1, MultiMap<K2, V>>> iterator = this.groupedMap.entrySet().iterator();

		while (iterator.hasNext()) {
			for (Entry<K2, List<V>> innerEntry : iterator.next().getValue().entrySet()) {
				flatMultiMap.putAll(innerEntry.getKey(), innerEntry.getValue());
			}
		}

		return flatMultiMap;
	}

	public MultiMap<K2, V> getInnerMapCopy(K1 mapKey) {
		MultiMap<K2, V> innerMultiMap = this.groupedMap.get(mapKey);
		return innerMultiMap != null ? new MultiMap<>(innerMultiMap) : null;
	}

	public boolean containsKey(K1 mapKey) {
		return this.groupedMap.containsKey(mapKey);
	}
}

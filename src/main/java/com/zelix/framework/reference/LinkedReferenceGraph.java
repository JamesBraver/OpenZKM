package com.zelix.framework.reference;

import com.zelix.utils.GlobalUtils;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class LinkedReferenceGraph<K, V> extends ReferenceGraph<K, V> {
	public LinkedReferenceGraph() {
		super(new LinkedHashMap<>(GlobalUtils.getInitialCapacity(75)), 75, 16, false);
	}

	private LinkedReferenceGraph(int capacity) {
		super(new LinkedHashMap<>(GlobalUtils.getInitialCapacity(capacity)), 75, 16, false);
	}

	@Override
	public Map<K, Set<V>> createNodeMap(int capacity) {
		return new LinkedHashMap<>(capacity);
	}

	@Override
	public Set<V> createReferenceSet(int capacity) {
		return new LinkedHashSet<>(capacity);
	}

	@Override
	public ReferenceGraph<K, V> deepClone() {
		int size = this.referenceMap.size();
		LinkedReferenceGraph<K, V> cloneGraph = new LinkedReferenceGraph<>(size * 2 + 1);

		for (Entry<K, Set<V>> entry : this.referenceMap.entrySet()) {
			Set<V> originalSet = entry.getValue();
			LinkedHashSet<V> clonedLinkedSet = new LinkedHashSet<>(originalSet);
			cloneGraph.setReferences(entry.getKey(), clonedLinkedSet);
		}

		return cloneGraph;
	}
}

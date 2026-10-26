package com.zelix.framework.reference;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.HierarchicalMultiMap;
import com.zelix.utils.collections.HierarchicalMultiMapWrapper;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ReferenceGraphMap<K1, K2, V> {
	private final int defaultGraphInitialCapacity;
	private final int defaultGraphLoadFactor;
	public Map<K1, ReferenceGraph<K2, V>> graphMap;

	public ReferenceGraphMap() {
		this(75, 10);
	}

	public ReferenceGraphMap(int mapCapacity, int graphCapacity) {
		this(mapCapacity, graphCapacity, 16);
	}

	public ReferenceGraphMap(int mapCapacity, int graphCapacity, int graphLoadFactor) {
		this.defaultGraphInitialCapacity = graphCapacity;
		this.defaultGraphLoadFactor = graphLoadFactor;
		this.graphMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(mapCapacity));
	}

	public Set<V> getReferences(K1 mapKey, K2 graphMember) {
		ReferenceGraph<K2, V> graph = this.graphMap.get(mapKey);
		return graph == null ? null : graph.getReferences(graphMember);
	}

	public boolean addReference(K1 mapKey, K2 graphMember, V graphReference) {
		ReferenceGraph<K2, V> graph = this.graphMap.get(mapKey);
		if (graph == null) {
			graph = new ReferenceGraph<>(this.defaultGraphInitialCapacity, this.defaultGraphLoadFactor);
			boolean added = graph.addReference(graphMember, graphReference);
			this.graphMap.put(mapKey, graph);
			return added;
		} else {
			return graph.addReference(graphMember, graphReference);
		}
	}

	public ReferenceGraph<K1, K2> flattenToGraph() {
		ReferenceGraph<K1, K2> flatGraph = (ReferenceGraph<K1, K2>)(new ReferenceGraph<>(this.graphMap.size()));

		for (Entry<K1, ReferenceGraph<K2, V>> entry : this.getEntrySet()) {
			K1 mapKey = entry.getKey();

			for (K2 graphMember : entry.getValue().getMembers()) {
				flatGraph.addReference(mapKey, graphMember);
			}
		}

		return flatGraph;
	}

	public HierarchicalMultiMapWrapper<K1, K2, V> toHierarchicalMultiMap() {
		HierarchicalMultiMap<K1, K2, V> hierarchicalMap = new HierarchicalMultiMap<>(this.graphMap.size() * 2);

		for (Entry<K1, ReferenceGraph<K2, V>> mapEntry : this.getEntrySet()) {
			for (Entry<K2, Set<V>> adjacencyEntry : mapEntry.getValue().getAdjacencyEntries()) {
				for (V reference : adjacencyEntry.getValue()) {
					hierarchicalMap.put(mapEntry.getKey(), adjacencyEntry.getKey(), reference);
				}
			}
		}

		return new HierarchicalMultiMapWrapper<>(hierarchicalMap);
	}

	public ReferenceGraph<K2, V> getGraph(K1 mapKey) {
		return this.graphMap.get(mapKey);
	}

	public Set<Entry<K1, ReferenceGraph<K2, V>>> getEntrySet() {
		return this.graphMap.entrySet();
	}
}

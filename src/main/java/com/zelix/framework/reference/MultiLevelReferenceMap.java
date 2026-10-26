package com.zelix.framework.reference;

import com.zelix.utils.GlobalUtils;
import java.util.Map;

public class MultiLevelReferenceMap<K0, K1, K2, V> {
	private final int defaultInnerMapCapacity;
	private final int defaultGraphCapacity;
	private final int defaultGraphLoadFactor;
	public Map<K0, ReferenceGraphMap<K1, K2, V>> rootMap;

	public MultiLevelReferenceMap(int mapCapacity, int innerMapCapacity, int graphCapacity, int graphLoadFactor) {
		this.defaultInnerMapCapacity = innerMapCapacity;
		this.defaultGraphCapacity = graphCapacity;
		this.defaultGraphLoadFactor = graphLoadFactor;
		this.rootMap = GlobalUtils.newHashMap(mapCapacity);
	}

	public MultiLevelReferenceMap() {
		this(75, 10);
	}

	public MultiLevelReferenceMap(int mapCapacity, int innerMapCapacity) {
		this(mapCapacity, innerMapCapacity, 16, 16);
	}

	public boolean addReference(K0 outerKey, K1 mapKey, K2 graphMember, V graphReference) {
		ReferenceGraphMap<K1, K2, V> graphMap = this.rootMap.get(outerKey);
		if (graphMap == null) {
			graphMap = new ReferenceGraphMap<>(this.defaultInnerMapCapacity, this.defaultGraphCapacity, this.defaultGraphLoadFactor);
			boolean added = graphMap.addReference(mapKey, graphMember, graphReference);
			this.rootMap.put(outerKey, graphMap);
			return added;
		} else {
			return graphMap.addReference(mapKey, graphMember, graphReference);
		}
	}

	public ReferenceGraph<K2, V> getGraph(K0 outerKey, K1 mapKey) {
		ReferenceGraphMap<K1, K2, V> graphMap = this.rootMap.get(outerKey);
		return graphMap == null ? null : graphMap.getGraph(mapKey);
	}

	public void clear() {
		this.rootMap.clear();
	}
}

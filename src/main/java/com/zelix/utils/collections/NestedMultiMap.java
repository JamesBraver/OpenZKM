package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.enumerations.SnapshotEnumerator;

import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class NestedMultiMap<K1, K2, K3, V> {
	public Map<K1, KeyValuePairMultiMap<K2, K3, V>> nestedMap;

	public NestedMultiMap(int initialCapacity) {
		this.nestedMap = GlobalUtils.newHashMap(initialCapacity);
	}

	public void put(K1 outerKey, K2 innerKey, K3 pairKey, V pairValue) {
      //      System.out.println("Put in NestedMultiMap: " + outerKey + " , " + innerKey + " , " + pairKey + " , " + pairValue);
//      System.out.println("[" + ((WorkspaceClassNode)outerKey).getClassName() + "] method " + ((MethodNode)innerKey).F + ((MethodNode)innerKey).A + ", constant: " + pairKey);

		KeyValuePairMultiMap<K2, K3, V> multiMap = this.nestedMap.get(outerKey);
		if (multiMap == null) {
			multiMap = new KeyValuePairMultiMap<>();
			this.nestedMap.put(outerKey, multiMap);
		}

		multiMap.put(innerKey, pairKey, pairValue);
	}

	public KeyValuePairMultiMap<K2, K3, V> getMultiMap(K1 outerKey) {
		return this.nestedMap.get(outerKey);
	}

	public int size() {
		return this.nestedMap.size();
	}

	public void clear() {
		this.nestedMap.clear();
	}

	public Set<Entry<K1, KeyValuePairMultiMap<K2, K3, V>>> entrySet() {
		return this.nestedMap.entrySet();
	}

	public Enumeration<K1> keys() {
		return new SnapshotEnumerator<>(this.nestedMap.keySet());
	}
}

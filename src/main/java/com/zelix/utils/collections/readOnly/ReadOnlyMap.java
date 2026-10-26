package com.zelix.utils.collections.readOnly;

import com.zelix.utils.GlobalUtils;

import java.util.*;

public class ReadOnlyMap<K, V> implements Map<K, V> {
	private Map<K, V> internalMap;

	public ReadOnlyMap(Map<K, V> map) {
		if (map == null) {
			this.internalMap = GlobalUtils.newHashMap(13);
		} else {
			this.internalMap = map;
		}
	}

	public ReadOnlyMap() {
		this.internalMap = GlobalUtils.newHashMap(13);
	}

	public final synchronized Enumeration<K> getKeyEnumeration() {
		return Collections.enumeration(this.internalMap.keySet());
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final boolean containsKey(Object key) {
		return this.internalMap.containsKey(key);
	}

	@Override
	public final boolean containsValue(Object value) {
		return this.internalMap.containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Collections.unmodifiableSet(this.internalMap.entrySet());
	}

	public final Map<K, V> getWritableCopy() {
		return GlobalUtils.newHashMap(this.internalMap);
	}

	@Override
	public final V get(Object key) {
		return this.internalMap.get(key);
	}

	@Override
	public final boolean isEmpty() {
		return this.internalMap.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return new ReadOnlySet<>(this.internalMap.keySet());
	}

	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int size() {
		return this.internalMap.size();
	}

	@Override
	public final synchronized Collection<V> values() {
		return Collections.unmodifiableCollection(this.internalMap.values());
	}
}

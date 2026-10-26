package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class UnmodifiableMultiMap<K, V> implements IMultiMap<K, V> {
	private final MultiMap<K, V> delegate;

	public UnmodifiableMultiMap(MultiMap<K, V> instance) {
		this.delegate = instance;
	}

	@Override
	public Enumeration<V> elements() {
		return this.delegate.elements();
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public int valueCount() {
		return this.delegate.valueCount();
	}

	@Override
	public boolean remove(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized Set<K> keySet() {
		return GlobalUtils.newHashSet(this.delegate.keySet());
	}

	@Override
	public synchronized Enumeration<K> keys() {
		return this.delegate.keys();
	}

	@Override
	public void put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<K, List<V>>> entrySet() {
		return GlobalUtils.newHashSet(this.delegate.entrySet());
	}

	@Override
	public boolean containsKey(Object key) {
		return this.delegate.containsKey(key);
	}

	@Override
	public boolean containsEntry(K key, V value) {
		return this.delegate.containsEntry(key, value);
	}

	@Override
	public List<V> removeAll(K key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<V> get(K key) {
		List<V> values = this.delegate.get(key);
		return values != null ? new ArrayList<>(values) : null;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public Enumeration<V> getValuesEnumeration(K key) {
		return this.delegate.getValuesEnumeration(key);
	}
}

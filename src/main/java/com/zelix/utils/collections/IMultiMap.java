package com.zelix.utils.collections;

import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public interface IMultiMap<K, V> {
	Enumeration<V> elements();

	int size();

	int valueCount();

	boolean remove(K key, V value);

	Set<K> keySet();

	void put(K key, V value);

	Enumeration<K> keys();

	Set<Entry<K, List<V>>> entrySet();

	boolean containsKey(Object key);

	boolean containsEntry(K key, V value);

	List<V> removeAll(K key);

	List<V> get(K key);

	void clear();

	boolean isEmpty();

	Enumeration<V> getValuesEnumeration(K key);
}

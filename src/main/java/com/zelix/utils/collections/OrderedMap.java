package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.readOnly.ReadOnlyEntry;
import com.zelix.utils.collections.readOnly.ReadOnlySet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class OrderedMap<K, V> implements Serializable, Map<K, V> {
	private Map<K, KeyValuePair<K, V>> backingMap;
	private List<KeyValuePair<K, V>> orderedEntryList;

	public OrderedMap(OrderedMap<K, V> other) {
		this(other.size());
		synchronized (other) {
			int entryCount = other.size();

			for (int i = 0; i < entryCount; i++) {
				KeyValuePair<K, V> entry = other.orderedEntryList.get(i);
				K key = entry.getKey();
				V value = entry.getValue();
				KeyValuePair<K, V> copiedEntry = new KeyValuePair<>(key, value);
				this.backingMap.put(key, copiedEntry);
				this.orderedEntryList.add(copiedEntry);
			}
		}
	}

	public OrderedMap(int initialCapacity) {
		this.backingMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(initialCapacity));
		this.orderedEntryList = new ArrayList<>(initialCapacity);
	}

	public OrderedMap() {
		this.backingMap = GlobalUtils.newHashMap();
		this.orderedEntryList = new ArrayList<>();
	}

	public synchronized Enumeration<K> keysEnumeration() {
		return Collections.enumeration(this.getKeyList());
	}

	public synchronized V setEntryAt(int index, K key, V value) {
		if (index < 0 || index >= this.orderedEntryList.size()) {
			throw new IllegalArgumentException("Index " + index + " out of range : " + this.orderedEntryList.size());
		} else if (this.backingMap.containsKey(key)) {
			if (this.backingMap.get(key).equals(this.orderedEntryList.get(index))) {
				return this.orderedEntryList.get(index).getValue();
			} else {
				throw new IllegalArgumentException("'" + key + "' is not a unique key");
			}
		} else {
			KeyValuePair<K, V> newEntry = new KeyValuePair<>(key, value);
			KeyValuePair<K, V> replacedEntry = this.orderedEntryList.set(index, newEntry);
			KeyValuePair<K, V> removedEntry = this.backingMap.remove(replacedEntry.getKey());
			this.backingMap.put(key, newEntry);
			return removedEntry.getValue();
		}
	}

	private List<V> getValueList() {
		int entryCount = this.orderedEntryList.size();
		ArrayList<V> valueList = new ArrayList<>(entryCount);

		for (int i = 0; i < entryCount; i++) {
			KeyValuePair<K, V> entry = this.orderedEntryList.get(i);
			valueList.add(entry.getValue());
		}

		return valueList;
	}

	public synchronized V removeAt(int index) {
		if (index >= 0 && index < this.orderedEntryList.size()) {
			KeyValuePair<K, V> removedEntry = this.orderedEntryList.remove(index);
			return this.backingMap.remove(removedEntry.getKey()).getValue();
		} else {
			throw new IllegalArgumentException("Index " + index + " out of range : " + this.orderedEntryList.size());
		}
	}

	@Override
	public synchronized void clear() {
		this.backingMap.clear();
		this.orderedEntryList.clear();
	}

	@Override
	public synchronized Object clone() {
		return new OrderedMap<>(this);
	}

	@Override
	public synchronized boolean containsKey(Object key) {
		return this.backingMap.containsKey(key);
	}

	@Override
	public synchronized boolean containsValue(Object value) {
		int entryCount = this.orderedEntryList.size();

		for (int i = 0; i < entryCount; i++) {
			if (this.orderedEntryList.get(i).getValue().equals(value)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public synchronized Set<Entry<K, V>> entrySet() {
		ArrayList<Entry<K, V>> entries = new ArrayList<>(this.orderedEntryList.size());

		for (KeyValuePair<K, V> entry : this.orderedEntryList) {
			entries.add(new ReadOnlyEntry<>(entry));
		}

		return new ReadOnlySet<>(new OrderedSet<>(entries));
	}

	public int indexOfKey(Object key) {
		if (this.backingMap.containsKey(key)) {
			int entryCount = this.orderedEntryList.size();

			for (int i = 0; i < entryCount; i++) {
				if (this.orderedEntryList.get(i).getKey().equals(key)) {
					return i;
				}
			}
		}

		return -1;
	}

	@Override
	public synchronized V get(Object key) {
		KeyValuePair<K, V> entry = this.backingMap.get(key);
		return entry == null ? null : entry.getValue();
	}

	@Override
	public boolean isEmpty() {
		return this.backingMap.isEmpty();
	}

	@Override
	public synchronized Set<K> keySet() {
		return Collections.unmodifiableSet(this.backingMap.keySet());
	}

	private List<K> getKeyList() {
		int entryCount = this.orderedEntryList.size();
		ArrayList<K> keyList = new ArrayList<>(entryCount);

		for (int i = 0; i < entryCount; i++) {
			KeyValuePair<K, V> entry = this.orderedEntryList.get(i);
			keyList.add(entry.getKey());
		}

		return keyList;
	}

	@Override
	public synchronized V put(K key, V value) {
		V previousValue = null;
		KeyValuePair<K, V> entry = this.backingMap.get(key);
		if (entry == null) {
			entry = new KeyValuePair<>(key, value);
			this.backingMap.put(key, entry);
			this.orderedEntryList.add(entry);
		} else {
			previousValue = entry.setValue(value);
		}

		return previousValue;
	}

	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized V remove(Object key) {
		V removedValue = null;
		KeyValuePair<K, V> removedEntry = this.backingMap.remove(key);
		if (removedEntry != null) {
			int entryCount = this.orderedEntryList.size();

			for (int i = 0; i < entryCount; i++) {
				if (this.orderedEntryList.get(i).getKey().equals(key)) {
					this.orderedEntryList.remove(i);
					break;
				}
			}

			removedValue = removedEntry.getValue();
		}

		return removedValue;
	}

	@Override
	public int size() {
		return this.orderedEntryList.size();
	}

	public synchronized K getKeyAt(int index) {
		if (index >= 0 && index < this.orderedEntryList.size()) {
			return this.orderedEntryList.get(index).getKey();
		} else {
			throw new IllegalArgumentException("Index " + index + " out of range : " + this.orderedEntryList.size());
		}
	}

	@Override
	public synchronized Collection<V> values() {
		return this.getValueList();
	}
}

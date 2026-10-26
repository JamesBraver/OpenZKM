package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;

import java.io.PrintWriter;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SoftReferenceMap<K, V> implements Map<K, V> {
	private int initialCapacity;
	private String name;
	private SoftReference<Map<K, V>> softMap;
	private PrintWriter[] loggers;

	public SoftReferenceMap(String name, int initialCapacity) {
		this.initialCapacity = initialCapacity;
		this.name = name;
		this.initializeMap();
	}

	public SoftReferenceMap(String name) {
		this(name, 101);
	}

	private Map<K, V> recreateMap() {
		this.initializeMap();
		Map<K, V> backingMap = this.softMap.get();
		if (this.loggers != null) {
			for (int i = 0; i < this.loggers.length; i++) {
				this.loggers[i].println(this.name + " cleared due to lack of memory...");
				this.loggers[i].flush();
			}
		}

		return backingMap;
	}

	private void initializeMap() {
		this.softMap = new SoftReference<>(GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.initialCapacity)));
	}

	@Override
	public synchronized void clear() {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			backingMap = this.recreateMap();
		}

		backingMap.clear();
	}

	@Override
	public synchronized boolean containsKey(Object key) {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			this.recreateMap();
			return false;
		} else {
			return backingMap.containsKey(key);
		}
	}

	@Override
	public synchronized boolean containsValue(Object value) {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			this.recreateMap();
			return false;
		} else {
			return backingMap.containsValue(value);
		}
	}

	@Override
	public synchronized Set<Entry<K, V>> entrySet() {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			this.recreateMap();
			return Collections.emptySet();
		} else {
			return Collections.unmodifiableSet(backingMap.entrySet());
		}
	}

	@Override
	public synchronized V get(Object key) {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			backingMap = this.recreateMap();
		}

		return backingMap.get(key);
	}

	@Override
	public synchronized boolean isEmpty() {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			backingMap = this.recreateMap();
		}

		return backingMap.isEmpty();
	}

	@Override
	public synchronized Set<K> keySet() {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			this.recreateMap();
			return Collections.emptySet();
		} else {
			return Collections.unmodifiableSet(backingMap.keySet());
		}
	}

	@Override
	public synchronized V put(K key, V value) {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			backingMap = this.recreateMap();
		}

		return backingMap.put(key, value);
	}

	@Override
	public synchronized void putAll(Map<? extends K, ? extends V> m) {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			backingMap = this.recreateMap();
		}

		backingMap.putAll(m);
	}

	@Override
	public synchronized V remove(Object key) {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			backingMap = this.recreateMap();
		}

		return backingMap.remove(key);
	}

	@Override
	public synchronized int size() {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			backingMap = this.recreateMap();
		}

		return backingMap.size();
	}

	@Override
	public synchronized Collection<V> values() {
		Map<K, V> backingMap = this.softMap.get();
		if (backingMap == null) {
			this.recreateMap();
			return Collections.emptySet();
		} else {
			return Collections.unmodifiableCollection(backingMap.values());
		}
	}
}

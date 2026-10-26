package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class MethodNameMap<K, V> implements Map<K, V> {
	private boolean disabled;
	private Map<K, V> backingMap;

	public MethodNameMap(boolean disabled) {
		this.disabled = disabled;
		if (!disabled) {
			this.backingMap = GlobalUtils.newHashMap(13);
		}
	}

	public MethodNameMap(MethodNameMap<K, V> other) {
		this.disabled = other.disabled;
		if (!this.disabled) {
			this.backingMap = GlobalUtils.newHashMap(other);
		}
	}

	@Override
	public void clear() {
		if (!this.disabled) {
			this.backingMap.clear();
		}
	}

	@Override
	public final boolean containsKey(Object key) {
		return this.disabled ? false : this.backingMap.containsKey(key);
	}

	@Override
	public final boolean containsValue(Object value) {
		return this.disabled ? false : this.backingMap.containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return this.disabled ? Collections.emptySet() : this.backingMap.entrySet();
	}

	@Override
	public final V get(Object key) {
		return this.disabled ? null : this.backingMap.get(key);
	}

	@Override
	public final boolean isEmpty() {
		return this.disabled ? true : this.backingMap.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return this.disabled ? Collections.emptySet() : this.backingMap.keySet();
	}

	public boolean isDisabled() {
		return this.disabled;
	}

	@Override
	public V put(K key, V value) {
		return this.disabled ? null : this.backingMap.put(key, value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		if (!this.disabled) {
			this.backingMap.putAll(m);
		}
	}

	@Override
	public V remove(Object key) {
		return this.disabled ? null : this.backingMap.remove(key);
	}

	@Override
	public final int size() {
		return this.disabled ? 0 : this.backingMap.size();
	}

	@Override
	public final synchronized Collection<V> values() {
		if (this.disabled) {
			return Collections.emptyList();
		} else {
			ArrayList<V> values = new ArrayList<>(this.backingMap.size());
			Iterator<Entry<K, V>> entryIterator = this.backingMap.entrySet().iterator();

			while (entryIterator.hasNext()) {
				values.add(entryIterator.next().getValue());
			}

			return values;
		}
	}
}

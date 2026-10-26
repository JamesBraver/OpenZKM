package com.zelix.framework.code;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.KeyValuePair;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class MethodMapping implements Map<Object, KeyValuePair<?, ?>> {
	public Map<Object, KeyValuePair<?, ?>> pairs = GlobalUtils.newHashMap();

	public KeyValuePair<?, ?> putNewPair(Object key, Object firstValue, Object secondValue) {
		KeyValuePair<?, ?> newPair = new KeyValuePair<>(firstValue, secondValue);
		return this.pairs.put(key, newPair);
	}

	public KeyValuePair<?, ?> removePair(Object key) {
		return this.pairs.remove(key);
	}

	public KeyValuePair<?, ?> getPair(Object key) {
		return this.pairs.get(key);
	}

	@Override
	public void clear() {
		this.pairs.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.pairs.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.pairs.containsValue(value);
	}

	@Override
	public Set<Entry<Object, KeyValuePair<?, ?>>> entrySet() {
		return this.pairs.entrySet();
	}

	@Override
	public KeyValuePair<?, ?> get(Object key) {
		return this.getPair(key);
	}

	@Override
	public boolean isEmpty() {
		return this.pairs.isEmpty();
	}

	public KeyValuePair<?, ?> putPair(Object key, KeyValuePair<?, ?> pair) {
		return this.pairs.put(key, pair);
	}

	@Override
	public Set<Object> keySet() {
		return this.pairs.keySet();
	}

	@Override
	public KeyValuePair<?, ?> put(Object key, KeyValuePair<?, ?> value) {
		return this.putPair(key, value);
	}

	@Override
	public void putAll(Map<? extends Object, ? extends KeyValuePair<?, ?>> m) {
		this.pairs.putAll(m);
	}

	@Override
	public KeyValuePair<?, ?> remove(Object key) {
		return this.removePair(key);
	}

	@Override
	public int size() {
		return this.pairs.size();
	}

	@Override
	public Collection<KeyValuePair<?, ?>> values() {
		return this.pairs.values();
	}
}

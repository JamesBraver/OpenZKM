package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class MultiMap<K, V> implements IMultiMap<K, V> {
	private int initialValueCapacity = 5;
	private MultiMap<K, V>.Enumerator cachedValueEnumerator = null;
	private final boolean useIdentityMap;
	private final boolean threadSafe;
	public Map<K, List<V>> backingMap;

	public MultiMap(boolean useIdentityMap, int mapCapacity) {
		this(useIdentityMap, mapCapacity, 5, false);
	}

	public MultiMap(int mapCapacity, boolean threadSafe) {
		this(false, mapCapacity, 5, threadSafe);
	}

	public MultiMap(boolean useIdentityMap, int mapCapacity, int initialValueCapacity, boolean threadSafe) {
		this.initialValueCapacity = initialValueCapacity;
		this.useIdentityMap = useIdentityMap;
		this.threadSafe = threadSafe;
		int mapInitialCapacity = GlobalUtils.getInitialCapacity(mapCapacity);
		if (threadSafe) {
			this.backingMap = new ConcurrentHashMap<>(mapInitialCapacity);
		} else if (useIdentityMap) {
			this.backingMap = new IdentityHashMap<>(mapInitialCapacity);
		} else {
			this.backingMap = GlobalUtils.newHashMap(mapInitialCapacity);
		}
	}

	public MultiMap(int mapCapacity, int initialValueCapacity, boolean threadSafe) {
		this(false, mapCapacity, initialValueCapacity, threadSafe);
	}

	public MultiMap() {
		this(false, 75, 5, false);
	}

	public MultiMap(boolean threadSafe) {
		this(false, 75, 5, threadSafe);
	}

	public MultiMap(int mapCapacity) {
		this(false, mapCapacity, 5, false);
	}

	public MultiMap(MultiMap<K, V> other) {
		this.threadSafe = other.threadSafe;
		this.useIdentityMap = other.useIdentityMap;
		this.initialValueCapacity = other.initialValueCapacity;
		int mapInitialCapacity = GlobalUtils.getInitialCapacity(other.backingMap.size());
		if (this.threadSafe) {
			this.backingMap = new ConcurrentHashMap<>(mapInitialCapacity);
		} else if (this.useIdentityMap) {
			this.backingMap = new IdentityHashMap<>(mapInitialCapacity);
		} else {
			this.backingMap = GlobalUtils.newHashMap(mapInitialCapacity);
		}

		for (Entry<K, List<V>> entry : other.backingMap.entrySet()) {
			List<V> values = entry.getValue();
			List<V> copiedValues;
			if (this.threadSafe) {
				copiedValues = new Vector<>(entry.getValue());
			} else {
				copiedValues = new ArrayList<>(values);
			}

			this.backingMap.put(entry.getKey(), copiedValues);
		}
	}

	public MultiMap(int mapCapacity, int initialValueCapacity) {
		this(false, mapCapacity, initialValueCapacity, false);
	}

	public void putAll(MultiMap<K, V> other) {
		if (this.cachedValueEnumerator != null) {
			this.cachedValueEnumerator.invalidate();
			this.cachedValueEnumerator = null;
		}

		for (Entry<K, List<V>> entry : other.backingMap.entrySet()) {
			K key = entry.getKey();
			List<V> otherValues = entry.getValue();
			List<V> existingValues = this.backingMap.get(key);
			if (existingValues == null) {
				List<V> copiedValues;
				if (this.threadSafe) {
					copiedValues = new Vector<>(otherValues);
				} else {
					copiedValues = new ArrayList<>(otherValues);
				}

				this.backingMap.put(key, copiedValues);
			} else {
				existingValues.addAll(otherValues);
			}
		}
	}

	public void trimToSize() {
		for (List<V> valueList : this.backingMap.values()) {
			if (valueList instanceof Vector) {
				((Vector<V>)valueList).trimToSize();
			} else if (valueList instanceof ArrayList) {
				((ArrayList<V>)valueList).trimToSize();
			} else {
				try {
					Class<?> valueListClass = valueList.getClass();
					Method trimToSizeMethod = valueListClass.getMethod("trimToSize");
					if (trimToSizeMethod != null) {
						trimToSizeMethod.invoke(valueList);
					}
				} catch (NoSuchMethodException ignoredNoSuchMethod) {
				} catch (InvocationTargetException ignoredInvocationTarget) {
				} catch (IllegalAccessException ignoredIllegalAccess) {
				}
			}
		}
	}

	@Override
	public Enumeration<V> elements() {
		if (this.threadSafe) {
			synchronized (this.backingMap) {
				if (this.cachedValueEnumerator == null) {
					this.cachedValueEnumerator = new Enumerator();
				} else {
					this.cachedValueEnumerator.reset();
				}
			}
		} else if (this.cachedValueEnumerator == null) {
			this.cachedValueEnumerator = new Enumerator();
		} else {
			this.cachedValueEnumerator.reset();
		}

		return this.cachedValueEnumerator;
	}

	@Override
	public int size() {
		return this.backingMap.size();
	}

	@Override
	public int valueCount() {
		int totalCount = 0;

		for (List<V> valueList : this.backingMap.values()) {
			totalCount += valueList.size();
		}

		return totalCount;
	}

	public boolean isThreadSafe() {
		return this.threadSafe;
	}

	public List<V> put(K key, List<V> values) {
		if (this.cachedValueEnumerator != null) {
			this.cachedValueEnumerator.invalidate();
			this.cachedValueEnumerator = null;
		}

		return this.backingMap.put(key, values);
	}

	@Override
	public boolean remove(K key, V value) {
		if (this.cachedValueEnumerator != null) {
			this.cachedValueEnumerator.invalidate();
			this.cachedValueEnumerator = null;
		}

		List<V> values = this.backingMap.get(key);
		if (values == null) {
			return false;
		} else {
			boolean removed = false;
			Iterator<V> valueIterator = values.iterator();

			while (valueIterator.hasNext()) {
				if (valueIterator.next().equals(value)) {
					valueIterator.remove();
					removed = true;
				}
			}

			if (values.size() == 0) {
				this.backingMap.remove(key);
			}

			return removed;
		}
	}

	public void putAll(K key, Collection<V> values) {
		this.putAllInternal(key, values, false);
	}

	@Override
	public Set<K> keySet() {
		return this.backingMap.keySet();
	}

	@Override
	public void put(K key, V value) {
		if (this.cachedValueEnumerator != null) {
			this.cachedValueEnumerator.invalidate();
			this.cachedValueEnumerator = null;
		}

		List<V> values = this.backingMap.get(key);
		if (values == null) {
			if (this.threadSafe) {
				values = new Vector<>(this.initialValueCapacity);
			} else {
				values = new ArrayList<>(this.initialValueCapacity);
			}

			this.backingMap.put(key, values);
		}

		values.add(value);
	}

	@Override
	public synchronized Enumeration<K> keys() {
		return Collections.enumeration(this.backingMap.keySet());
	}

	private void putAllInternal(K key, Collection<V> values, boolean prepend) {
		if (this.cachedValueEnumerator != null) {
			this.cachedValueEnumerator.invalidate();
			this.cachedValueEnumerator = null;
		}

		List<V> existingValues = this.backingMap.get(key);
		if (existingValues == null) {
			if (this.threadSafe) {
				existingValues = new Vector<>(Math.max(this.initialValueCapacity, values.size()));
			} else {
				existingValues = new ArrayList<>(Math.max(this.initialValueCapacity, values.size()));
			}

			this.backingMap.put(key, existingValues);
		}

		if (prepend) {
			existingValues.addAll(0, values);
		} else {
			existingValues.addAll(values);
		}
	}

	@Override
	public boolean containsKey(Object key) {
		return this.backingMap.containsKey(key);
	}

	@Override
	public Set<Entry<K, List<V>>> entrySet() {
		return this.backingMap.entrySet();
	}

	@Override
	public boolean containsEntry(K key, V value) {
		List<V> values = this.backingMap.get(key);
		return values == null ? false : values.contains(value);
	}

	@Override
	public List<V> removeAll(K key) {
		if (this.cachedValueEnumerator != null) {
			this.cachedValueEnumerator.invalidate();
			this.cachedValueEnumerator = null;
		}

		return this.backingMap.remove(key);
	}

	@Override
	public List<V> get(K key) {
		return this.backingMap.get(key);
	}

	@Override
	public void clear() {
		Iterator<List<V>> valueListIterator = this.backingMap.values().iterator();

		while (valueListIterator.hasNext()) {
			valueListIterator.next().clear();
		}

		this.backingMap.clear();
		if (this.cachedValueEnumerator != null) {
			this.cachedValueEnumerator.invalidate();
			this.cachedValueEnumerator = null;
		}
	}

	@Override
	public boolean isEmpty() {
		return this.backingMap.isEmpty();
	}

	public void prependAll(K key, Collection<V> values) {
		this.putAllInternal(key, values, true);
	}

	@Override
	public Enumeration<V> getValuesEnumeration(K key) {
		List<V> values;
		return (values = this.backingMap.get(key)) == null ? null : Collections.enumeration(values);
	}

	private class Enumerator implements Enumeration<V> {
		private boolean invalid = false;
		private final OrderedSet<V> sortedValues = new OrderedSet<>(MultiMap.this.backingMap.size() * 5);
		private int index;

		private Enumerator() {
			for (List<V> valuesForKey : MultiMap.this.backingMap.values()) {
				this.sortedValues.addAll(valuesForKey);
			}

			this.reset();
		}

		private void invalidate() {
			this.invalid = true;
		}

		@Override
		public boolean hasMoreElements() {
			if (this.invalid) {
				throw new ConcurrentModificationException();
			} else {
				return this.index < this.sortedValues.size();
			}
		}

		@Override
		public V nextElement() {
			if (this.invalid) {
				throw new ConcurrentModificationException();
			} else {
				return this.sortedValues.get(this.index++);
			}
		}

		private void reset() {
			if (this.invalid) {
				throw new ConcurrentModificationException();
			} else {
				this.index = 0;
			}
		}
	}
}

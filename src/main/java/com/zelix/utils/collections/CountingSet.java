package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.PriorityEntry;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

public class CountingSet<E> implements Collection<E> {
	public Map<E, AtomicInt> countMap;

	public CountingSet(int initialCapacity) {
		this.countMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(initialCapacity));
	}

	public CountingSet() {
		this(75);
	}

	public synchronized List<E> getSortedByCount(boolean ascending) {
		int size = this.countMap.size();
		@SuppressWarnings("unchecked")
		PriorityEntry<E>[] entries = (PriorityEntry<E>[]) new PriorityEntry<?>[size];
		int index = 0;

		for (E element : this.countMap.keySet()) {
			AtomicInt count = this.countMap.get(element);
			entries[index++] = new PriorityEntry<>(count.get(), element);
		}

		Arrays.sort(entries);
		ArrayList<E> resultList = new ArrayList<>(size);
		if (ascending) {
			for (int i = 0; i < size; i++) {
				resultList.add(entries[i].getValue());
			}
		} else {
			for (int reverseIndex = size - 1; reverseIndex >= 0; reverseIndex--) {
				resultList.add(entries[reverseIndex].getValue());
			}
		}

		return resultList;
	}

	public synchronized int getCount(Object element) {
		AtomicInt count = this.countMap.get(element);
		return count == null ? 0 : count.get();
	}

	@Override
	public synchronized boolean add(E element) {
		AtomicInt count = this.countMap.get(element);
		if (count == null) {
			count = new AtomicInt(1);
			this.countMap.put(element, count);
		} else {
			count.increaseAndGet();
		}

		return true;
	}

	@Override
	public synchronized boolean addAll(Collection<? extends E> collection) {
		Iterator<? extends E> iterator = collection.iterator();

		while (iterator.hasNext()) {
			this.add((E)iterator.next());
		}

		return true;
	}

	@Override
	public void clear() {
		this.countMap.clear();
	}

	@Override
	public synchronized CountingSet<E> clone() {
		CountingSet<E> clonedSet = new CountingSet<>(this.countMap.size());

		for (Entry<E, AtomicInt> entry : this.countMap.entrySet()) {
			AtomicInt count = entry.getValue();
			clonedSet.setCount(entry.getKey(), count.get());
		}

		return clonedSet;
	}

	@Override
	public boolean contains(Object element) {
		return this.countMap.containsKey(element);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		Iterator<?> iterator = collection.iterator();

		while (iterator.hasNext()) {
			if (!this.countMap.containsKey(iterator.next())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean isEmpty() {
		return this.countMap.size() == 0;
	}

	@Override
	public synchronized Iterator<E> iterator() {
		return this.countMap.keySet().iterator();
	}

	public synchronized void setCount(E element, int count) {
		AtomicInt currentCount = this.countMap.get(element);
		if (currentCount == null) {
			this.countMap.put(element, new AtomicInt(count));
		} else {
			currentCount.set(count);
		}
	}

	@Override
	public synchronized boolean remove(Object element) {
		AtomicInt count = this.countMap.get(element);
		if (count != null) {
			if (count.get() == 1) {
				this.countMap.remove(element);
			} else {
				count.decreaseAndGet();
			}

			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean removeAll(Collection<?> collection) {
		int initialSize = this.countMap.size();
		Iterator<?> iterator = collection.iterator();

		while (iterator.hasNext()) {
			this.countMap.remove(iterator.next());
		}

		return initialSize != this.countMap.size();
	}

	@Override
	public synchronized boolean retainAll(Collection<?> collection) {
		int initialSize = this.countMap.size();
		Iterator<E> iterator = this.countMap.keySet().iterator();

		while (iterator.hasNext()) {
			E element = iterator.next();
			if (!collection.contains(element)) {
				iterator.remove();
			}
		}

		return initialSize != this.countMap.size();
	}

	@Override
	public int size() {
		return this.countMap.size();
	}

	@Override
	public Object[] toArray() {
		Object[] array = new Object[this.countMap.size()];
		int index = 0;
		Iterator<E> iterator = this.countMap.keySet().iterator();

		while (iterator.hasNext()) {
			array[index++] = iterator.next();
		}

		return array;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] array) {
		if (array.length < this.countMap.size()) {
			array = (T[])Array.newInstance(array.getClass().getComponentType(), this.countMap.size());
		}

		int index = 0;
		Iterator<E> iterator = this.countMap.keySet().iterator();

		while (iterator.hasNext()) {
			array[index++] = (T)iterator.next();
		}

		if (array.length > this.countMap.size()) {
			array[this.countMap.size()] = null;
		}

		return array;
	}
}

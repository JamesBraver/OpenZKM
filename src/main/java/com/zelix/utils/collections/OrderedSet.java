package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;

import java.io.Serializable;
import java.util.*;

public class OrderedSet<E> implements Serializable, Set<E> {
	private Set<E> set;
	private List<E> list;

	public OrderedSet(OrderedSet<E> other) {
		this(other.size());
		synchronized (other) {
			this.set.addAll(other);
			this.list.addAll(other);
		}
	}

	public OrderedSet(Collection<E> collection) {
		this(collection.size());
		synchronized (collection) {
			for (E element : collection) {
				if (this.set.add(element)) {
					this.list.add(element);
				}
			}
		}
	}

	public OrderedSet(int initialCapacity) {
		this.set = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(initialCapacity));
		this.list = new ArrayList<>(initialCapacity);
	}

	public OrderedSet() {
		this.set = GlobalUtils.newHashSet();
		this.list = new ArrayList<>();
	}

	public synchronized E get(int index) {
		if (index >= 0 && index < this.list.size()) {
			return this.list.get(index);
		} else {
			throw new IllegalArgumentException("Index " + index + " out of range : " + this.list.size());
		}
	}

	@Override
	public synchronized boolean add(E e) {
		if (this.set.add(e)) {
			this.list.add(e);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean changed = false;

		for (E element : c) {
			if (this.set.add(element)) {
				changed = true;
				this.list.add(element);
			}
		}

		return changed;
	}

	@Override
	public synchronized void clear() {
		this.set.clear();
		this.list.clear();
	}

	@Override
	public synchronized Object clone() {
		return new OrderedSet<>(this);
	}

	@Override
	public synchronized boolean contains(Object o) {
		return this.set.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.set.containsAll(c);
	}

	public int indexOf(E element) {
		return this.set.contains(element) ? this.list.indexOf(element) : -1;
	}

	@Override
	public boolean isEmpty() {
		return this.set.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private Object[] elements = OrderedSet.this.list.toArray(new Object[OrderedSet.this.list.size()]);
			private int index = 0;

			@Override
			public boolean hasNext() {
				return this.index < this.elements.length;
			}

			@SuppressWarnings("unchecked")
			@Override
			public E next() {
				if (this.index >= this.elements.length) {
					throw new NoSuchElementException(this.index + ">=" + this.elements.length);
				}

				return (E)this.elements[this.index++];
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public synchronized boolean remove(Object o) {
		boolean removed = this.set.remove(o);
		if (removed) {
			this.list.remove(o);
		}

		return removed;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean removed = this.set.removeAll(c);
		if (removed) {
			this.list.removeAll(c);
		}

		return removed;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean retained = this.set.retainAll(c);
		if (retained) {
			this.list.retainAll(c);
		}

		return retained;
	}

	@Override
	public int size() {
		return this.list.size();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return (T[])this.list.toArray(a);
	}

	@Override
	public Object[] toArray() {
		Object[] array = new Object[this.list.size()];
		return this.list.toArray(array);
	}
}

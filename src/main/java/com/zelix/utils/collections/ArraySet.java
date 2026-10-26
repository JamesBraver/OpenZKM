package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

public class ArraySet<E> implements Collection<E> {
	private E[] backingArray;

	public ArraySet(E[] backingArray) {
		if (backingArray == null) {
			throw new IllegalArgumentException();
		} else {
			this.backingArray = backingArray;
		}
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		for (int i = 0; i < this.backingArray.length; i++) {
			if (this.backingArray[i].equals(o)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		Iterator<?> elementIterator = c.iterator();

		while (elementIterator.hasNext()) {
			if (!this.contains(elementIterator.next())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean isEmpty() {
		return this.backingArray.length == 0;
	}

	@Override
	public Iterator<E> iterator() {
		return new ArraySetIterator<>(this);
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return this.backingArray.length;
	}

	@Override
	public Object[] toArray() {
		return GlobalUtils.clone(this.backingArray);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		if (a.length < this.backingArray.length) {
			a = (T[])Array.newInstance(a.getClass().getComponentType(), this.backingArray.length);
		}

		System.arraycopy(this.backingArray, 0, a, 0, this.backingArray.length);
		if (a.length > this.backingArray.length) {
			a[this.backingArray.length] = null;
		}

		return a;
	}

	public static <E> E[] getBackingArray(ArraySet<E> arraySet) {
		return arraySet.backingArray;
	}
}

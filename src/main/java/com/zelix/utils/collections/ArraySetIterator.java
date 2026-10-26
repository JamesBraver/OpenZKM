package com.zelix.utils.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArraySetIterator<E> implements Iterator<E> {
	public final ArraySet<E> arraySet;
	private int index;

	public ArraySetIterator(ArraySet<E> arraySet) {
		this.arraySet = arraySet;
	}

	@Override
	public boolean hasNext() {
		return this.index < ArraySet.<E>getBackingArray(this.arraySet).length;
	}

	@Override
	public E next() {
		if (this.index >= ArraySet.getBackingArray(this.arraySet).length) {
			throw new NoSuchElementException(this.index + ">=" + ArraySet.getBackingArray(this.arraySet).length);
		}

		return (E)ArraySet.getBackingArray(this.arraySet)[this.index++];
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}

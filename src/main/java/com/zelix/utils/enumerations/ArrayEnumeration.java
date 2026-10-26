package com.zelix.utils.enumerations;

import com.zelix.utils.GlobalUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayEnumeration<T> implements Enumeration<T> {
	private T[] array;
	private int size;
	private int index;

	public ArrayEnumeration(T[] array) {
		if (array == null) {
			throw new IllegalArgumentException();
		} else {
			this.array = (T[])((Object[])array.clone());
			this.size = this.array.length;
		}
	}

	public static <S> Enumeration<S> newCollectionEnumeration(Collection<S> collection) {
		if (collection == null) {
			throw new IllegalArgumentException("Collection cannot be null");
		} else {
			@SuppressWarnings("unchecked")
			S[] elements = (S[])(new Object[collection.size()]);
			int i = 0;
			Iterator<S> elementIterator = collection.iterator();

			while (elementIterator.hasNext()) {
				elements[i++] = elementIterator.next();
			}

			return new ArrayEnumeration<>(elements);
		}
	}

	public void sort(Comparator<T> comparator) {
		if (this.index > 0) {
			throw new IllegalStateException();
		} else {
			this.array = (T[])GlobalUtils.clone(this.array);
			Arrays.sort(this.array, comparator);
		}
	}

	@Override
	public final boolean hasMoreElements() {
		return this.index < this.size;
	}

	@Override
	public final T nextElement() {
		if (this.index >= this.size) {
			throw new NoSuchElementException(this.getClass().getName());
		} else {
			return this.array[this.index++];
		}
	}
}

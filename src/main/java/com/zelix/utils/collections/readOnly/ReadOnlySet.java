package com.zelix.utils.collections.readOnly;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ReadOnlySet<E> implements Set<E> {
	private Set<E> delegate;

	public ReadOnlySet(Set<E> delegate) {
		if (delegate == null) {
			throw new IllegalArgumentException("Null argument not allowed");
		} else {
			this.delegate = delegate;
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
	public final boolean contains(Object o) {
		return this.delegate.contains(o);
	}

	@Override
	public final boolean containsAll(Collection<?> c) {
		return this.delegate.containsAll(c);
	}

	@Override
	public final boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			private final Iterator<E> iterator = ReadOnlySet.this.delegate.iterator();

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public E next() {
				return this.iterator.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
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
	public final int size() {
		return this.delegate.size();
	}

	@Override
	public Object[] toArray() {
		return this.delegate.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return (T[])this.delegate.toArray(a);
	}
}

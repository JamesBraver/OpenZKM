package com.zelix.framework.code;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class InsnList<E> implements List<E> {
	private final List<E> list;

	public InsnList(List<E> list) {
		if (list == null) {
			throw new IllegalArgumentException("Null argument not allowed");
		} else {
			this.list = list;
		}
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
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
		return this.list.contains(o);
	}

	@Override
	public final boolean containsAll(Collection<?> c) {
		return this.list.containsAll(c);
	}

	@Override
	public E get(int index) {
		return this.list.get(index);
	}

	@Override
	public final int indexOf(Object o) {
		return this.list.indexOf(o);
	}

	@Override
	public final boolean isEmpty() {
		return this.list.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return new InsnIterator<>(this);
	}

	@Override
	public final int lastIndexOf(Object o) {
		return this.list.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return new InsnListIterator<>(this);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new InsnListIterator<>(this, index);
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
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
	public E set(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int size() {
		return this.list.size();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new InsnList<>(this.list.subList(fromIndex, toIndex));
	}

	@Override
	public Object[] toArray() {
		return this.list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return (T[])this.list.toArray(a);
	}

	public static <E> List<E> toList(InsnList<E> insnList) {
		return insnList.list;
	}
}

package com.zelix.utils.collections.readOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ReadOnlyList<E> implements List<E> {
	private ArrayList<E> internalList = new ArrayList<>();

	public ReadOnlyList(Enumeration<? extends E> elements) {
		while (elements.hasMoreElements()) {
			this.internalList.add((E)elements.nextElement());
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
	public boolean contains(Object o) {
		return this.internalList.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.internalList.containsAll(c);
	}

	@Override
	public boolean equals(Object obj) {
		return this.internalList.equals(obj);
	}

	@Override
	public E get(int index) {
		return this.internalList.get(index);
	}

	@Override
	public int hashCode() {
		return this.internalList.hashCode();
	}

	@Override
	public int indexOf(Object o) {
		return this.internalList.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return this.internalList.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return this.internalList.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.internalList.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return this.internalList.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return this.internalList.listIterator(index);
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
	public int size() {
		return this.internalList.size();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return this.internalList.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return this.internalList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return (T[])this.internalList.toArray(a);
	}
}

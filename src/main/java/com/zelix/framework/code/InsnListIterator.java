package com.zelix.framework.code;

import java.util.ListIterator;

public class InsnListIterator<E> implements ListIterator<E> {
	public final InsnList<E> insnList;
	private ListIterator<E> iterator;

	public InsnListIterator(InsnList<E> insnList) {
		this.insnList = insnList;
		this.iterator = InsnList.toList(this.insnList).listIterator();
		this.iterator = InsnList.toList(insnList).listIterator();
	}

	public InsnListIterator(InsnList<E> insnList, int index) {
		this.insnList = insnList;
		this.iterator = InsnList.toList(this.insnList).listIterator();
		this.iterator = InsnList.toList(insnList).listIterator(index);
	}

	@Override
	public void add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	@Override
	public boolean hasPrevious() {
		return this.iterator.hasPrevious();
	}

	@Override
	public E next() {
		return this.iterator.next();
	}

	@Override
	public int nextIndex() {
		return this.iterator.nextIndex();
	}

	@Override
	public E previous() {
		return this.iterator.previous();
	}

	@Override
	public int previousIndex() {
		return this.iterator.previousIndex();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(E e) {
		throw new UnsupportedOperationException();
	}
}

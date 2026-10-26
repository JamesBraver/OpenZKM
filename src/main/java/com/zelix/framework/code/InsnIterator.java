package com.zelix.framework.code;

import java.util.Iterator;

public class InsnIterator<E> implements Iterator<E> {
	public final InsnList<E> list;
	private Iterator<E> iter;

	public InsnIterator(InsnList<E> list) {
		this.list = list;
		this.iter = InsnList.toList(this.list).iterator();
	}

	@Override
	public boolean hasNext() {
		return this.iter.hasNext();
	}

	@Override
	public E next() {
		return this.iter.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}

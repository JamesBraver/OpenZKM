package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class IdentityHashSet<E> implements Set<E> {
	private final Set<E> backingSet = GlobalUtils.newHashSet();

	private Set<E> getBackingSet() {
		return this.backingSet;
	}

	@Override
	public boolean add(E element) {
		return this.backingSet.add(element);
	}

	@Override
	public boolean addAll(Collection<? extends E> collection) {
		return this.backingSet.addAll(collection);
	}

	@Override
	public void clear() {
		this.backingSet.clear();
	}

	@Override
	public boolean contains(Object element) {
		return this.backingSet.contains(element);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		return this.backingSet.containsAll(collection);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else {
			return !(obj instanceof IdentityHashSet) ? false : this.backingSet == ((IdentityHashSet<?>)obj).getBackingSet();
		}
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this.backingSet);
	}

	@Override
	public boolean isEmpty() {
		return this.backingSet.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return this.backingSet.iterator();
	}

	@Override
	public boolean remove(Object element) {
		return this.backingSet.remove(element);
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		return this.backingSet.removeAll(collection);
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		return this.backingSet.retainAll(collection);
	}

	@Override
	public int size() {
		return this.backingSet.size();
	}

	@Override
	public Object[] toArray() {
		return this.backingSet.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return (T[])this.backingSet.toArray(array);
	}
}

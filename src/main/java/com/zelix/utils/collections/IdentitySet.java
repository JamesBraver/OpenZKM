package com.zelix.utils.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IdentitySet<E> implements Set<E> {
	private final IdentityMap<E, E> backingSet = new IdentityMap<>();

	public IdentitySet(int initialCapacity) {
	}

	public IdentitySet() {
	}

	@Override
	public boolean add(E e) {
		return this.backingSet.put(e, e) == null;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean changed = false;

		for (E element : c) {
			if (this.backingSet.put(element, element) == null) {
				changed = true;
			}
		}

		return changed;
	}

	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		return Set.super.removeIf(filter);
	}

	@Override
	public void clear() {
		this.backingSet.clear();
	}

	@Override
	public Spliterator<E> spliterator() {
		return Set.super.spliterator();
	}

	@Override
	public Stream<E> stream() {
		return Set.super.stream();
	}

	@Override
	public Stream<E> parallelStream() {
		return Set.super.parallelStream();
	}

	@Override
	public boolean contains(Object o) {
		return this.backingSet.containsKey(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object element : c) {
			if (!this.backingSet.containsKey(element)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean isEmpty() {
		return this.backingSet.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return this.backingSet.keySet().iterator();
	}

	@Override
	public void forEach(Consumer<? super E> action) {
		Set.super.forEach(action);
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		return this.backingSet.remove(o) != null;
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
		return this.backingSet.size();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}
}

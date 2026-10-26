package com.zelix.utils.collections;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class NamedHashSet<E> extends HashSet<E> implements NamedSet<E> {
	private String name;

	public NamedHashSet(String name, Map<E, ?> elementMap) {
		this(name, elementMap.keySet());
	}

	public NamedHashSet(String name, Collection<E> elements) {
		super(elements);
		this.name = name;
	}

	public NamedHashSet(Collection<E> elements) {
		super(elements);
	}

	public NamedHashSet() {
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object clone() {
		return super.clone();
	}
}

package com.zelix.utils.collections;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

public class NamedLinkedHashSet<E> extends LinkedHashSet<E> implements NamedSet<E> {
	private String name;

	public NamedLinkedHashSet() {
	}

	public NamedLinkedHashSet(String name, Collection<E> elements) {
		super(elements);
		this.name = name;
	}

	public NamedLinkedHashSet(String name, Map<E, ?> elementMap) {
		this(name, elementMap.keySet());
	}

	public NamedLinkedHashSet(Collection<E> elements) {
		super(elements);
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

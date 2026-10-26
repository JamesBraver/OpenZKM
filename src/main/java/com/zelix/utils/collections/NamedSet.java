package com.zelix.utils.collections;

import java.util.Set;

public interface NamedSet<E> extends Set<E> {
	String getName();

	Object clone();
}

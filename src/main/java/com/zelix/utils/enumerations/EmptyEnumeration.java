package com.zelix.utils.enumerations;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class EmptyEnumeration<T> implements Enumeration<T> {
	@Override
	public final boolean hasMoreElements() {
		return false;
	}

	@Override
	public final T nextElement() {
		throw new NoSuchElementException();
	}
}

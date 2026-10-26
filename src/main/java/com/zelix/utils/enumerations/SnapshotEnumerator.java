package com.zelix.utils.enumerations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

public class SnapshotEnumerator<T> implements Enumeration<T> {
	private Enumeration<T> delegateEnumerator;

	public SnapshotEnumerator(Collection<T> collection) {
		ArrayList<T> snapshot = new ArrayList<>(collection);
		this.delegateEnumerator = Collections.enumeration(snapshot);
	}

	@Override
	public final boolean hasMoreElements() {
		return this.delegateEnumerator.hasMoreElements();
	}

	@Override
	public final T nextElement() {
		return this.delegateEnumerator.nextElement();
	}
}

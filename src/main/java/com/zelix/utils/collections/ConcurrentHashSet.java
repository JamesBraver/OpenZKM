package com.zelix.utils.collections;

import com.zelix.utils.GlobalUtils;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

public class ConcurrentHashSet<T> {
	private final HashSet<T> set;

	public ConcurrentHashSet(HashSet<T> set) {
		this.set = set;
	}

	public ConcurrentHashSet() {
		this.set = GlobalUtils.newHashSet();
	}

	public int size() {
		return this.set.size();
	}

	public boolean contains(T element) {
		return this.set.contains(element);
	}

	public Iterator<T> iterator() {
		return this.set.iterator();
	}

	public boolean add(T element) {
		return this.set.add(element);
	}

	public synchronized Enumeration<T> enumeration() {
		return Collections.enumeration(this.set);
	}

	@Override
	public ConcurrentHashSet<T> clone() {
		return new ConcurrentHashSet<>(GlobalUtils.clone(this.set));
	}
}

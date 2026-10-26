package com.zelix.utils;

import com.zelix.utils.collections.IdentityHashSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class UnionFind<T> {
	private final Map<T, IdentityHashSet<T>> elementSets = GlobalUtils.newHashMap();

	public Set<T> getElements() {
		return Collections.unmodifiableSet(this.elementSets.keySet());
	}

	public void union(T element1, T element2) {
		IdentityHashSet<T> set1 = this.elementSets.get(element1);
		IdentityHashSet<T> set2 = this.elementSets.get(element2);
		if (set1 == null && set2 == null) {
			IdentityHashSet<T> newSet = new IdentityHashSet<>();
			newSet.add(element1);
			newSet.add(element2);
			this.elementSets.put(element1, newSet);
			this.elementSets.put(element2, newSet);
		} else if (set1 != null && set2 != null) {
			for (T item : set2) {
				this.elementSets.remove(item);
				this.elementSets.put(item, set1);
			}

			set1.addAll(set2);
		} else if (set1 != null) {
			set1.add(element2);
			this.elementSets.put(element2, set1);
		} else {
			set2.add(element1);
			this.elementSets.put(element1, set2);
		}
	}

	public IdentityHashSet<T> getSet(T element) {
		return this.elementSets.get(element);
	}

	public Set<IdentityHashSet<T>> getDistinctSets() {
		HashSet<IdentityHashSet<T>> distinctSets = GlobalUtils.newHashSet();

		for (Entry<T, IdentityHashSet<T>> entry : this.elementSets.entrySet()) {
			distinctSets.add(entry.getValue());
		}

		return distinctSets;
	}

	public boolean contains(T element) {
		return this.elementSets.containsKey(element);
	}

	public Set<Entry<T, IdentityHashSet<T>>> getEntries() {
		return Collections.unmodifiableSet(this.elementSets.entrySet());
	}
}

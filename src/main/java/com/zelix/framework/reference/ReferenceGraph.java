package com.zelix.framework.reference;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReferenceGraph<K, V> implements IReferenceGraph<K, V> {
	public Map<K, Set<V>> referenceMap;
	protected int initialMemberCapacity;
	protected int initialReferenceSetCapacity;
	protected final boolean isThreadSafe;

	public ReferenceGraph(int memberCapacity, int referenceSetCapacity) {
		this(memberCapacity, referenceSetCapacity, false);
	}

	public ReferenceGraph(boolean isThreadSafe) {
		this(75, 16, isThreadSafe);
	}

	protected ReferenceGraph(Map<K, Set<V>> referenceMap, int memberCapacity, int referenceSetCapacity, boolean isThreadSafe) {
		this.referenceMap = referenceMap;
		this.initialMemberCapacity = GlobalUtils.getInitialCapacity(memberCapacity);
		this.initialReferenceSetCapacity = GlobalUtils.getInitialCapacity(referenceSetCapacity);
		this.isThreadSafe = isThreadSafe;
	}

	public ReferenceGraph(int memberCapacity, boolean isThreadSafe) {
		this(memberCapacity, 16, isThreadSafe);
	}

	public ReferenceGraph(int memberCapacity) {
		this(memberCapacity, 16);
	}

	public ReferenceGraph(int memberCapacity, int referenceSetCapacity, boolean isThreadSafe) {
		this.initialMemberCapacity = GlobalUtils.getInitialCapacity(memberCapacity);
		this.initialReferenceSetCapacity = GlobalUtils.getInitialCapacity(referenceSetCapacity);
		this.isThreadSafe = isThreadSafe;
		if (isThreadSafe) {
			this.referenceMap = new ConcurrentHashMap<>(this.initialMemberCapacity);
		} else {
			this.referenceMap = this.createNodeMap(this.initialMemberCapacity);
		}
	}

	public ReferenceGraph() {
		this(75, 16);
	}

	@Override
	public boolean hasMember(K member) {
		return this.referenceMap.containsKey(member);
	}

	@Override
	public boolean isReferencedBy(K member, V reference) {
		Set<V> references = this.referenceMap.get(member);
		return references == null ? false : references.contains(reference);
	}

	@Override
	public Set<K> getMembers() {
		return this.referenceMap.keySet();
	}

	public Set<V> setReferences(K member, Set<V> references) {
		return this.referenceMap.put(member, references);
	}

	public final MultiMap<K, V> toMultiMap() {
		int size = this.referenceMap.size();
		MultiMap<K, V> multiMap = new MultiMap<>(size * 2 + 1, this.isThreadSafe);
		if (this.isThreadSafe) {
			synchronized (this.referenceMap) {
				for (Entry<K, Set<V>> entry : this.referenceMap.entrySet()) {
					multiMap.putAll(entry.getKey(), entry.getValue());
				}
			}
		} else {
			for (Entry<K, Set<V>> mapEntry : this.referenceMap.entrySet()) {
				multiMap.putAll(mapEntry.getKey(), mapEntry.getValue());
			}
		}

		return multiMap;
	}

	@Override
	public void clear() {
		this.referenceMap.clear();
	}

	private void filterReferencesByTarget(Set<V> targetSet, Map<K, Set<V>> filteredMap) {
		for (Entry<K, Set<V>> entry : this.referenceMap.entrySet()) {
			K member = entry.getKey();
			Set<V> references = entry.getValue();
			Set<V> newReferences = this.createReferenceSet(GlobalUtils.getInitialCapacity(references.size()));

			for (V reference : references) {
				if (targetSet.contains(reference)) {
					newReferences.add(reference);
				}
			}

			if (!newReferences.isEmpty()) {
				filteredMap.put(member, newReferences);
			}
		}
	}

	@Override
	public final void retainReferences(V[] references) {
		HashSet<V> targetSet = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(references.length));

		for (V reference : references) {
			targetSet.add(reference);
		}

		Map<K, Set<V>> newMap = this.createNodeMap(GlobalUtils.getInitialCapacity(this.referenceMap.size()));
		if (this.isThreadSafe) {
			synchronized (this.referenceMap) {
				this.filterReferencesByTarget(targetSet, newMap);
			}
		} else {
			this.filterReferencesByTarget(targetSet, newMap);
		}

		this.referenceMap = newMap;
	}

	public Set<Entry<K, Set<V>>> getAdjacencyEntries() {
		return this.referenceMap.entrySet();
	}

	@Override
	public boolean removeReference(K member, V reference) {
		boolean removed = false;
		Set<V> references = this.referenceMap.get(member);
		if (references != null) {
			removed = references.remove(reference);
			if (references.size() == 0) {
				this.referenceMap.remove(member);
			}
		}

		return removed;
	}

	public Map<K, Set<V>> createNodeMap(int capacity) {
		return this.isThreadSafe ? new ConcurrentHashMap<>(capacity) : GlobalUtils.newHashMap(capacity);
	}

	@Override
	public Object clone() {
		return this.deepClone();
	}

	@Override
	public Set<V> removeMember(K member) {
		return this.referenceMap.remove(member);
	}

	@Override
	public Enumeration<K> getMembersAsEnumeration() {
		return Collections.enumeration(this.referenceMap.keySet());
	}

	public Set<V> createReferenceSet(int capacity) {
		return (Set<V>)(this.isThreadSafe ? Collections.newSetFromMap(new ConcurrentHashMap<>(capacity)) : GlobalUtils.newHashSet(capacity));
	}

	@Override
	public int size() {
		return this.referenceMap.size();
	}

	public void addReferences(K member, Collection<? extends V> references) {
		if (references != null) {
			for (V reference : references) {
				this.addReference(member, reference);
			}
		}
	}

	public Enumeration<V> getAllReferences() {
		Set<V> allReferences = this.createReferenceSet(101);
		Iterator<Entry<K, Set<V>>> iterator = this.referenceMap.entrySet().iterator();

		while (iterator.hasNext()) {
			Set<V> references = iterator.next().getValue();
			allReferences.addAll(references);
		}

		return Collections.enumeration(allReferences);
	}

	@Override
	public boolean isEmpty() {
		return this.referenceMap.size() == 0;
	}

	public ReferenceGraph<K, V> deepClone() {
		int size = this.referenceMap.size();
		ReferenceGraph<K, V> cloneGraph = new ReferenceGraph<>(size * 2 + 1, this.isThreadSafe);

		for (Entry<K, Set<V>> entry : this.referenceMap.entrySet()) {
			Set<V> originalSet = entry.getValue();
			int capacity = GlobalUtils.getInitialCapacity(originalSet.size());
			Set<V> clonedSet = (Set<V>)(this.isThreadSafe ? Collections.newSetFromMap(new ConcurrentHashMap<>(capacity)) : GlobalUtils.newHashSet(capacity));
			clonedSet.addAll(originalSet);
			cloneGraph.setReferences(entry.getKey(), clonedSet);
		}

		return cloneGraph;
	}

	@Override
	public Set<V> getReferences(K member) {
		return this.referenceMap.get(member);
	}

	@Override
	public boolean addReference(K member, V reference) {
		Set<V> references = this.referenceMap.get(member);
		boolean added;
		if (references == null) {
			references = this.createReferenceSet(this.initialReferenceSetCapacity);
			references.add(reference);
			this.referenceMap.put(member, references);
			added = true;
		} else {
			added = references.add(reference);
		}

		return added;
	}

	@Override
	public List<V> getReferencesAsList(K member) {
		return this.referenceMap.get(member) != null ? new ArrayList<>(this.referenceMap.get(member)) : null;
	}
}

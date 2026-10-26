package com.zelix.framework.reference;

import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public interface IReferenceGraph<K, V> {
	boolean hasMember(K member);

	boolean isReferencedBy(K member, V reference);

	Set<K> getMembers();

	void clear();

	void retainReferences(V[] references);

	boolean removeReference(K member, V reference);

	Set<V> removeMember(K member);

	Enumeration<K> getMembersAsEnumeration();

	int size();

	boolean isEmpty();

	boolean addReference(K member, V reference);

	Set<V> getReferences(K member);

	List<V> getReferencesAsList(K member);
}

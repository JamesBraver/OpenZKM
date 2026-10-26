package com.zelix.framework.reference;

import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class BitSetReferenceGraph<K, V> implements IReferenceGraph<K, V> {
	private static IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
	private Object[] referrerArray;
	private Map<K, BitSet> memberBitSetMap;
	private Map<V, Integer> referrerIndexMap;
	private int referrerCount;
	private final boolean ownsReferrerArray;

	public BitSetReferenceGraph(BitSetReferenceGraph<K, V> otherGraph, int loadFactorParams) {
		this(otherGraph.referrerArray, loadFactorParams, false);
		this.mergeWith(otherGraph);
	}

	public BitSetReferenceGraph(Object[] referrers, int loadFactorParams, boolean ownsArray) {
		if (ownsArray) {
			this.referrerArray = new Object[referrers.length];
			System.arraycopy(referrers, 0, this.referrerArray, 0, referrers.length);
		} else {
			this.referrerArray = referrers;
		}

		this.memberBitSetMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(loadFactorParams));
		this.referrerIndexMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.referrerArray.length));
		this.referrerCount = this.referrerArray.length;

		for (int i = 0; i < this.referrerCount; i++) {
			@SuppressWarnings("unchecked")
			V referrer = (V)this.referrerArray[i];
			this.referrerIndexMap.put(referrer, INTEGER_CACHE.valueOf(i));
		}

		this.ownsReferrerArray = ownsArray;
	}

	public BitSetReferenceGraph(V[] referrers, int loadFactorParams) {
		this(referrers, loadFactorParams, true);
	}

	@Override
	public boolean hasMember(K member) {
		return this.memberBitSetMap.containsKey(member);
	}

	@Override
	public boolean isReferencedBy(K member, V reference) {
		Integer index = this.referrerIndexMap.get(reference);
		if (index == null) {
			throw new IllegalArgumentException("Value '" + reference + "' doesn't exist in value array (C)");
		} else {
			BitSet bitSet = this.memberBitSetMap.get(member);
			return bitSet == null ? false : bitSet.get(index);
		}
	}

	@Override
	public Set<K> getMembers() {
		return GlobalUtils.newHashSet(this.memberBitSetMap.keySet());
	}

	public void mergeWith(BitSetReferenceGraph<K, V> otherGraph) {
		if (otherGraph.referrerArray != this.referrerArray) {
			throw new IllegalArgumentException(
				"Must be based upon the exact same array. : " + System.identityHashCode(this.referrerArray) + "!=" + System.identityHashCode(otherGraph.referrerArray)
			);
		} else {
			for (Entry<K, BitSet> entry : otherGraph.memberBitSetMap.entrySet()) {
				K member = entry.getKey();
				BitSet otherBitSet = entry.getValue();
				BitSet myBitSet = this.memberBitSetMap.get(member);
				if (myBitSet == null) {
					this.memberBitSetMap.put(member, (BitSet)otherBitSet.clone());
				} else {
					myBitSet.or(otherBitSet);
				}
			}
		}
	}

	@Override
	public void clear() {
		this.memberBitSetMap.clear();
	}

	@Override
	public void retainReferences(V[] references) {
		int length = references.length;
		HashMap<V, Integer> newIndexMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(length));

		for (int i = 0; i < length; i++) {
			V reference = references[i];
			newIndexMap.put(reference, INTEGER_CACHE.valueOf(i));
		}

		HashMap<K, BitSet> newBitSetMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.memberBitSetMap.size()));
		int currentReferrerCount = this.referrerArray.length;

		for (Entry<K, BitSet> entry : this.memberBitSetMap.entrySet()) {
			K member = entry.getKey();
			BitSet oldBitSet = entry.getValue();
			BitSet newBitSet = new BitSet(references.length);
			newBitSetMap.put(member, newBitSet);

			for (int referrerIndex = 0; referrerIndex < currentReferrerCount; referrerIndex++) {
				if (oldBitSet.get(referrerIndex)) {
					@SuppressWarnings("unchecked")
					V oldReference = (V)this.referrerArray[referrerIndex];
					Integer newIndex = newIndexMap.get(oldReference);
					if (newIndex != null) {
						newBitSet.set(newIndex);
					}
				}
			}
		}

		if (this.ownsReferrerArray) {
			this.referrerArray = new Object[length];
			System.arraycopy(references, 0, this.referrerArray, 0, length);
		} else {
			this.referrerArray = references;
		}

		this.memberBitSetMap = newBitSetMap;
		this.referrerIndexMap = newIndexMap;
		this.referrerCount = this.referrerArray.length;
	}

	@Override
	public boolean removeReference(K member, V reference) {
		Integer index = this.referrerIndexMap.get(reference);
		if (index == null) {
			throw new IllegalArgumentException("Value '" + reference + "' doesn't exist in value array (B)");
		} else {
			BitSet bitSet = this.memberBitSetMap.get(member);
			if (bitSet == null) {
				return false;
			} else {
				int bitIndex = index;
				boolean wasSet = bitSet.get(bitIndex);
				bitSet.clear(bitIndex);
				if (bitSet.cardinality() == 0) {
					this.memberBitSetMap.remove(member);
				}

				return wasSet;
			}
		}
	}

	@Override
	public Set<V> removeMember(K member) {
		BitSet bitSet = this.memberBitSetMap.remove(member);
		if (bitSet == null) {
			return null;
		} else {
			HashSet<V> resultReferences = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(bitSet.cardinality()));
			this.getReferrersFromBitSet(bitSet, resultReferences);
			return resultReferences;
		}
	}

	@Override
	public Enumeration<K> getMembersAsEnumeration() {
		return Collections.enumeration(this.memberBitSetMap.keySet());
	}

	@Override
	public int size() {
		return this.memberBitSetMap.size();
	}

	private Collection<V> getReferrersFromBitSet(BitSet bitSet, Collection<V> resultCollection) {
		int cardinality = bitSet.cardinality();
		int foundCount = 0;

		for (int i = 0; i < this.referrerCount; i++) {
			if (bitSet.get(i)) {
				@SuppressWarnings("unchecked")
				V referrer = (V)this.referrerArray[i];
				resultCollection.add(referrer);
				foundCount++;
			}

			if (foundCount >= cardinality) {
				break;
			}
		}

		return resultCollection;
	}

	@Override
	public boolean isEmpty() {
		return this.memberBitSetMap.isEmpty();
	}

	@Override
	public boolean addReference(K member, V reference) {
		Integer index = this.referrerIndexMap.get(reference);
		if (index == null) {
			throw new IllegalArgumentException("Value '" + reference + "' doesn't exist in value array (A)");
		} else {
			BitSet bitSet = this.memberBitSetMap.get(member);
			if (bitSet == null) {
				bitSet = new BitSet(this.referrerCount);
				this.memberBitSetMap.put(member, bitSet);
			}

			int bitIndex = index;
			boolean wasSet = bitSet.get(bitIndex);
			bitSet.set(bitIndex);
			return wasSet;
		}
	}

	@Override
	public Set<V> getReferences(K member) {
		BitSet bitSet = this.memberBitSetMap.get(member);
		if (bitSet == null) {
			return null;
		} else {
			HashSet<V> resultReferences = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(bitSet.cardinality()));
			this.getReferrersFromBitSet(bitSet, resultReferences);
			return resultReferences;
		}
	}

	@Override
	public List<V> getReferencesAsList(K member) {
		BitSet bitSet = this.memberBitSetMap.get(member);
		if (bitSet == null) {
			return null;
		} else {
			ArrayList<V> resultReferences = new ArrayList<>(bitSet.cardinality());
			this.getReferrersFromBitSet(bitSet, resultReferences);
			return resultReferences;
		}
	}
}

package com.zelix.tools.parameterChanger.salt;

import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.utils.collections.MultiMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SaltEntry implements Salt {
	private static int idCounter = 0;
	private final List<Long> saltValues = new ArrayList<>();
	private final int[] bitPermutation;
	private final int id;
	private SaltEntry previousSalt;
	private int index;
	private SaltEntry nextSalt;
	private long saltValue;

	public SaltEntry(long saltValue, int[] bitPermutation, MultiMap<int[], SaltEntry> entriesByPermutation, Set<int[]> permutationSet) {
		this.bitPermutation = bitPermutation;
		this.saltValues.add(saltValue);
		this.id = idCounter++;
		entriesByPermutation.put(bitPermutation, this);
		permutationSet.add(bitPermutation);
	}

	public SaltEntry(int highByte, long payload, int lowByte, int[] bitPermutation, MultiMap<int[], SaltEntry> entriesByPermutation, Set<int[]> permutationSet) {
		long packedSaltValue = ParameterChangerBuilder.packAndPermuteLongParts(highByte, payload, lowByte, bitPermutation);
		this.bitPermutation = bitPermutation;
		this.saltValues.add(packedSaltValue);
		this.id = idCounter++;
		entriesByPermutation.put(bitPermutation, this);
		permutationSet.add(bitPermutation);
	}

	public SaltEntry(SaltEntry parentSalt, int[] bitPermutation) {
		this.bitPermutation = bitPermutation;
		int indexBits = parentSalt.getIndexBits();
		long midBits = parentSalt.getMidBits();
		int parentHashCode = parentSalt.hashCode();
		long packedSaltValue = ParameterChangerBuilder.packAndPermuteLongParts(indexBits, midBits, parentHashCode, bitPermutation);
		this.saltValues.add(packedSaltValue);
		this.id = idCounter++;
	}

	@Override
	public void appendToChain(Salt salt) {
		SaltEntry saltEntry = (SaltEntry)salt;
		if (this != saltEntry) {
			if (this.nextSalt == null) {
				this.nextSalt = saltEntry;
				saltEntry.previousSalt = this;
			} else {
				this.nextSalt.appendToChain(saltEntry);
			}
		}
	}

	public String getKey() {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(this.getMidBits());
		keyBuilder.append(',');
		keyBuilder.append(this.hashCode());
		return keyBuilder.toString();
	}

	public long getValueKey() {
		return ParameterChangerBuilder.extractLowBits(this.getCurrentValue(), 56, this.bitPermutation);
	}

	@Override
	public long advanceChain(long mixValue) {
		long currentValue = this.getCurrentValue();
		int[] permutation = this.bitPermutation;
		long midBits = ParameterChangerBuilder.extractPermutedBitRange(currentValue, 8, 55, permutation);
		long mixedSaltValue = currentValue ^ this.saltValue ^ mixValue;
		this.saltValues.add(mixedSaltValue);
		if (this.nextSalt != null) {
			this.nextSalt.advanceChain(mixValue);
		}

		return midBits;
	}

	@Override
	public long getCurrentValue() {
		return this.saltValues.get(this.saltValues.size() - 1);
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Salt getNextSalt() {
		return this.nextSalt;
	}

	public int[] getBitPermutation() {
		return this.bitPermutation;
	}

	public int getIndexBits() {
		return (int)ParameterChangerBuilder.extractPermutedBitRange(this.getCurrentValue(), 56, 63, this.bitPermutation);
	}

	public Long getValueAt(int index) {
		return this.saltValues.get(index);
	}

	@Override
	public boolean isLessThanOrEqual(Salt other) {
		if (this == other) {
			return true;
		} else if (other instanceof SaltEntry) {
			SaltEntry saltEntry = (SaltEntry)other;
			return this.getIndexBits() - saltEntry.getIndexBits() <= 0;
		} else {
			return true;
		}
	}

	public long getSaltValue() {
		return this.saltValue;
	}

	public boolean chainIntersects(Set<SaltEntry> salts) {
		for (SaltEntry chainEntry = (SaltEntry)this.getNextSalt(); chainEntry != null; chainEntry = (SaltEntry)chainEntry.getNextSalt()) {
			if (salts.contains(chainEntry)) {
				return true;
			}
		}

		return false;
	}

	public int getIndex() {
		return this.index;
	}

	public void setSaltValue(long saltValue) {
		this.saltValue = saltValue;
	}

	public void pushValue(long value) {
		this.saltValues.add(value);
	}

	public long getValueBits(int lowBit, int highBit) {
		return ParameterChangerBuilder.extractPermutedBitRange(this.getCurrentValue(), lowBit, highBit, this.bitPermutation);
	}

	@Override
	public long getMidBits() {
		return ParameterChangerBuilder.extractPermutedBitRange(this.getCurrentValue(), 8, 55, this.bitPermutation);
	}

	public boolean chainContains(SaltEntry salt) {
		for (SaltEntry chainEntry = (SaltEntry)this.getNextSalt(); chainEntry != null; chainEntry = (SaltEntry)chainEntry.getNextSalt()) {
			if (chainEntry == salt) {
				return true;
			}
		}

		return false;
	}

	public void setBitPermutation(int[] bitPermutation) {
		System.arraycopy(bitPermutation, 0, this.bitPermutation, 0, this.bitPermutation.length);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof SaltEntry) {
			SaltEntry otherEntry = (SaltEntry)obj;
			return ParameterChangerBuilder.extractLowBits(this.getCurrentValue(), 56, this.bitPermutation) == ParameterChangerBuilder.extractLowBits(otherEntry.getCurrentValue(), 56, otherEntry.bitPermutation);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (int)ParameterChangerBuilder.extractLowBits(this.getCurrentValue(), 8, this.bitPermutation);
	}

	public boolean hasPreviousSalt() {
		return this.previousSalt != null;
	}

	public long getPayloadAt(int index) {
		return ParameterChangerBuilder.extractPermutedBitRange(this.getValueAt(index), 8, 55, this.bitPermutation);
	}

	@Override
	public boolean isSaltEntry() {
		return true;
	}
}

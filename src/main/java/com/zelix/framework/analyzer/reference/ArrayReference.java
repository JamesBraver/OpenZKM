package com.zelix.framework.analyzer.reference;

import com.zelix.framework.analyzer.tracker.ReflectionMethodCallTracker;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.dataset.AtomicInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ArrayReference implements Reference {
	private int elementCount;
	private HashSet<Reference>[] elementReferences;
	private List<String> errorMessages;

	public ArrayReference(int elementCount, List<String> errorMessages) {
		this.elementCount = elementCount;
		this.elementReferences = new HashSet[elementCount];
		this.errorMessages = errorMessages;
	}

	public ArrayReference(int elementCount) {
		this.elementCount = elementCount;
		this.elementReferences = new HashSet[elementCount];
		this.errorMessages = new ArrayList<>();
	}

	public void setElementReference(int index, Reference reference) {
		HashSet<Reference> elementReferenceSet = GlobalUtils.newHashSet(3);
		elementReferenceSet.add(reference);
		this.elementReferences[index] = elementReferenceSet;
		if (reference instanceof ErrorReference) {
			this.errorMessages.add(((ErrorReference)reference).getErrorMessage());
		}
	}

	@Override
	public boolean isSingleValue() {
		return this.elementCount == 1 && this.elementReferences[0].size() == 1 && this.elementReferences[0].iterator().next() instanceof NullReference;
	}

	public boolean isElementTypeResolved() {
		for (int i = 0; i < this.elementReferences.length; i++) {
			HashSet<Reference> elementReferenceSet = this.elementReferences[i];
			int referenceCount = elementReferenceSet.size();
			if (referenceCount == 0) {
				return false;
			}

			if (referenceCount > 1) {
				String commonValue = null;

				for (Reference reference : elementReferenceSet) {
					if (!reference.isResolved()) {
						return false;
					}

					String value = reference.getValue();
					if (commonValue == null) {
						commonValue = value;
					} else if (!commonValue.equals(value)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public String[][] getTypeCombinations() {
		if (this.elementReferences.length == 0) {
			return new String[0][0];
		} else {
			int combinationCount = 1;
			HashSet<String>[] elementTypes = new HashSet[this.elementReferences.length];

			for (int i = 0; i < this.elementReferences.length; i++) {
				HashSet<Reference> elementReferenceSet = this.elementReferences[i];
				if (elementReferenceSet.size() == 0) {
					return (String[][])null;
				}

				HashSet<String> typeNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(elementReferenceSet.size()));

				for (Reference reference : elementReferenceSet) {
					reference.getTypeName();
					typeNames.add(reference.getValue());
				}

				combinationCount *= typeNames.size();
				elementTypes[i] = typeNames;
			}

			String[][] typeCombinations = new String[combinationCount][elementTypes.length];
			AtomicInt combinationIndex = new AtomicInt(0);
			this.fillTypeCombinations(typeCombinations, elementTypes, combinationIndex, 0, new String[elementTypes.length]);
			return typeCombinations;
		}
	}

	@Override
	public boolean isResolved() {
		if (this.errorMessages.size() > 0) {
			return false;
		} else {
			for (int i = 0; i < this.elementReferences.length; i++) {
				Iterator<Reference> referenceIterator = this.elementReferences[i].iterator();

				while (referenceIterator.hasNext()) {
					if (!referenceIterator.next().isResolved()) {
						return false;
					}
				}
			}

			return true;
		}
	}

	@Override
	public String getTypeName() {
		return this.getValue();
	}

	public void setElementReferences(int index, HashSet<Reference> references) {
		this.elementReferences[index] = references;
	}

	public int getElementCount() {
		return this.elementCount;
	}

	public void resolvePendingReferences(HashMap<Integer, ReflectionMethodCallTracker> trackers, IntegerCache integerCache, InstructionListNode instructionList) {
		for (int i = 0; i < this.elementReferences.length; i++) {
			HashSet<Reference> elementReferenceSet = this.elementReferences[i];

			for (Reference reference : GlobalUtils.clone(elementReferenceSet)) {
				if (reference.isPending() && reference instanceof PendingReference && ((PendingReference)reference).getInstructionList() == instructionList) {
					ReflectionMethodCallTracker callTracker = trackers.get(integerCache.valueOf(reference.getInstructionIndex()));
					if (callTracker != null) {
						Set<Reference> parameterReferences = callTracker.getParameterReferences(0);
						elementReferenceSet.remove(reference);
						elementReferenceSet.addAll(parameterReferences);
					}
				}
			}
		}
	}

	public String toDisplayString(boolean useRawTypeNames) {
		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < this.elementReferences.length; i++) {
			HashSet<Reference> elementReferenceSet = this.elementReferences[i];
			buffer.append("[");
			Iterator<Reference> referenceIterator = elementReferenceSet.iterator();

			while (referenceIterator.hasNext()) {
				Reference reference = referenceIterator.next();
				buffer.append(useRawTypeNames && reference.isResolved() && reference.isSingleValue() ? reference.getValue() : reference.getDisplayString());
				if (referenceIterator.hasNext()) {
					buffer.append(",");
				}
			}

			buffer.append("]");
		}

		return buffer.toString();
	}

	@Override
	public int getInstructionIndex() {
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ArrayReference)) {
			return false;
		} else {
			ArrayReference other = (ArrayReference)obj;
			return this.elementReferences.equals(other.elementReferences) && this.errorMessages.equals(other.errorMessages);
		}
	}

	public ArrayList<Reference> getAllReferences() {
		ArrayList<Reference> allReferences = new ArrayList<>();

		for (int i = 0; i < this.elementReferences.length; i++) {
			for (Reference reference : this.elementReferences[i]) {
				allReferences.add(reference);
			}
		}

		return allReferences;
	}

	private void fillTypeCombinations(String[][] combinations, HashSet<String>[] elementTypes, AtomicInt counter, int elementIndex, String[] currentTypes) {
		Iterator<String> typeIterator = elementTypes[elementIndex].iterator();

		while (typeIterator.hasNext()) {
			String[] nextTypes = new String[currentTypes.length];
			System.arraycopy(currentTypes, 0, nextTypes, 0, elementIndex);
			nextTypes[elementIndex] = typeIterator.next();
			if (elementIndex + 1 < currentTypes.length) {
				this.fillTypeCombinations(combinations, elementTypes, counter, elementIndex + 1, nextTypes);
			} else {
				combinations[counter.get()] = nextTypes;
				counter.increaseAndGet();
			}
		}
	}

	@Override
	public int hashCode() {
		return this.elementReferences.hashCode() ^ this.errorMessages.hashCode();
	}

	@Override
	public String getValue() {
		return this.elementCount == 1 && this.elementReferences[0].size() == 1 ? this.elementReferences[0].iterator().next().getValue() : null;
	}

	public HashSet<Reference> getElementReferences(int index) {
		return this.elementReferences[index];
	}

	@Override
	public String getKey() {
		int elementIndex = 0;
		StringBuilder keyBuilder = new StringBuilder();

		for (HashSet<Reference> elementReferenceSet : this.elementReferences) {
			TreeSet<String> distinctValues = new TreeSet<>();
			Iterator<Reference> referenceIterator = elementReferenceSet.iterator();

			while (referenceIterator.hasNext()) {
				String value = referenceIterator.next().getValue();
				distinctValues.add(value != null ? value : "null");
			}

			int distinctValueCount = distinctValues.size();
			int valueIndex = 0;

			for (String distinctValue : distinctValues) {
				keyBuilder.append(distinctValue);
				if (valueIndex++ < distinctValueCount) {
					keyBuilder.append('!');
				}
			}

			if (elementIndex++ < this.elementCount) {
				keyBuilder.append('~');
			}
		}

		return keyBuilder.toString();
	}

	@Override
	public boolean isPending() {
		for (int i = 0; i < this.elementReferences.length; i++) {
			Iterator<Reference> referenceIterator = this.elementReferences[i].iterator();

			while (referenceIterator.hasNext()) {
				if (referenceIterator.next().isPending()) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public String getDisplayString() {
		return this.toDisplayString(false);
	}

	public ArrayList<Reference> getFirstElementReferences() {
		ArrayList<Reference> firstReferences = new ArrayList<>();

		for (int i = 0; i < this.elementReferences.length; i++) {
			Reference firstReference = null;
			Iterator<Reference> referenceIterator = this.elementReferences[i].iterator();
			if (referenceIterator.hasNext()) {
				firstReference = referenceIterator.next();
			}

			firstReferences.add(firstReference);
		}

		return firstReferences;
	}
}

package com.zelix.framework.analyzer.tracker;

import com.zelix.framework.analyzer.reference.ArrayReference;
import com.zelix.framework.analyzer.reference.PendingReference;
import com.zelix.framework.analyzer.reference.Reference;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.framework.writer.reflect.ReflectionParameter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ReflectionMethodCallTracker {
	private boolean privateCallChain = true;
	private ReflectionMethodPattern pattern;
	private List<HashSet<Reference>> parameterReferences;

	public ReflectionMethodCallTracker(ReflectionMethodPattern pattern) {
		this.pattern = pattern;
		this.parameterReferences = new ArrayList<>(pattern.getParameters().size());
	}

	public boolean isPrivateCallChain() {
		return this.privateCallChain;
	}

	public String getMemberName() {
		if (this.parameterReferences.size() < this.pattern.getParameters().size() + (this.pattern.isInvokedOnClassInstance() ? 1 : 0)) {
			return null;
		} else {
			int parameterIndex = 0;

			for (ReflectionParameter parameter : this.pattern.getParameters()) {
				if (parameter.isFieldNameParameter() || parameter.isMethodNameParameter()) {
					return this.getParameterValue(parameterIndex);
				}

				parameterIndex++;
			}

			return null;
		}
	}

	public void addParameterReferences(HashSet<Reference> references) {
		this.parameterReferences.add(references);
	}

	private boolean containsArrayReference(HashSet<Reference> references) {
		Iterator<Reference> referenceIterator = references.iterator();

		while (referenceIterator.hasNext()) {
			if (referenceIterator.next() instanceof ArrayReference) {
				return true;
			}
		}

		return false;
	}

	public int getTrackedParameterCount() {
		return this.parameterReferences.size();
	}

	public boolean isResolved() {
		if (this.parameterReferences.size() < this.pattern.getParameters().size() + (this.pattern.isInvokedOnClassInstance() ? 1 : 0)) {
			return false;
		} else {
			Iterator<HashSet<Reference>> parameterReferencesIterator = this.parameterReferences.iterator();

			while (parameterReferencesIterator.hasNext()) {
				Iterator<Reference> referenceIterator = parameterReferencesIterator.next().iterator();

				while (referenceIterator.hasNext()) {
					if (!referenceIterator.next().isResolved()) {
						return false;
					}
				}
			}

			return true;
		}
	}

	public void setPrivateCallChain(boolean privateCallChain) {
		this.privateCallChain = privateCallChain;
	}

	public Set<Reference> getParameterReferences(int index) {
		return this.parameterReferences.get(index);
	}

	public String getParameterValue(int index) {
		if (this.parameterReferences.size() < this.pattern.getParameters().size() + (this.pattern.isInvokedOnClassInstance() ? 1 : 0)) {
			return null;
		} else {
			HashSet<Reference> referenceSet = this.parameterReferences.get(index);
			HashSet<String> resolvedValues = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(referenceSet.size()));

			for (Reference reference : referenceSet) {
				if (!reference.isResolved()) {
					return null;
				}

				resolvedValues.add(reference.getValue());
			}

			return resolvedValues.size() == 1 ? resolvedValues.iterator().next() : null;
		}
	}

	public void resolveParameterReferences(HashMap<Integer, ReflectionMethodCallTracker> trackersByPriority, IntegerCache integerCache, InstructionListNode instructionListNode) {
		for (HashSet<Reference> parameterReferenceSet : new ArrayList<>(this.parameterReferences)) {
			for (Reference unresolvedReference : GlobalUtils.clone(parameterReferenceSet)) {
				if (unresolvedReference.isPending()) {
					if (unresolvedReference instanceof ArrayReference) {
						((ArrayReference)unresolvedReference).resolvePendingReferences(trackersByPriority, integerCache, instructionListNode);
					} else if (unresolvedReference instanceof PendingReference && ((PendingReference)unresolvedReference).getInstructionList() == instructionListNode) {
						ReflectionMethodCallTracker resolvingTracker = trackersByPriority.get(integerCache.valueOf(unresolvedReference.getInstructionIndex()));
						boolean bothMethodType = false;
						if (this.pattern.isMethodTypeType() && resolvingTracker.pattern.isMethodTypeType()) {
							bothMethodType = true;
						}

						if (resolvingTracker != null) {
							List<HashSet<Reference>> resolvingParameterReferences = resolvingTracker.parameterReferences;
							if (resolvingParameterReferences.size() <= 1) {
								if (this.pattern.isMethodTypeType()) {
									ArrayReference singleElementArrayReference = new ArrayReference(1);
									singleElementArrayReference.setElementReferences(0, resolvingParameterReferences.get(0));
									parameterReferenceSet.remove(unresolvedReference);
									parameterReferenceSet.add(singleElementArrayReference);
								} else {
									parameterReferenceSet.remove(unresolvedReference);
									parameterReferenceSet.addAll(resolvingParameterReferences.get(0));
								}
							} else {
								ArrayList<HashSet<Reference>> elementReferenceSets = new ArrayList<>();

								for (int parameterIndex = bothMethodType ? 1 : 0; parameterIndex < resolvingParameterReferences.size(); parameterIndex++) {
									HashSet<Reference> resolvingReferenceSet = resolvingParameterReferences.get(parameterIndex);
									if (!this.containsArrayReference(resolvingReferenceSet)) {
										elementReferenceSets.add(resolvingReferenceSet);
									} else {
										HashSet<Reference>[] positionReferenceSets = null;

										for (Reference resolvingArrayReference : resolvingReferenceSet) {
											if (!(resolvingArrayReference instanceof ArrayReference)) {
												return;
											}

											ArrayReference arrayReference = (ArrayReference)resolvingArrayReference;
											if (positionReferenceSets == null) {
												positionReferenceSets = (HashSet<Reference>[]) new HashSet<?>[arrayReference.getElementCount()];
											}

											if (arrayReference.getElementCount() != positionReferenceSets.length) {
												return;
											}

											for (int positionIndex = 0; positionIndex < arrayReference.getElementCount(); positionIndex++) {
												if (positionReferenceSets[positionIndex] == null) {
													positionReferenceSets[positionIndex] = arrayReference.getElementReferences(positionIndex);
												} else {
													positionReferenceSets[positionIndex].addAll(arrayReference.getElementReferences(positionIndex));
												}
											}

											for (HashSet<Reference> positionReferenceSet : positionReferenceSets) {
												elementReferenceSets.add(positionReferenceSet);
											}
										}
									}
								}

								ArrayReference combinedArrayReference = new ArrayReference(elementReferenceSets.size());

								for (int elementIndex = 0; elementIndex < elementReferenceSets.size(); elementIndex++) {
									combinedArrayReference.setElementReferences(elementIndex, elementReferenceSets.get(elementIndex));
								}

								parameterReferenceSet.remove(unresolvedReference);
								parameterReferenceSet.add(combinedArrayReference);
							}
						}
					}
				}
			}
		}
	}
}

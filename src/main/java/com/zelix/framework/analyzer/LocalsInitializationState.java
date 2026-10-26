package com.zelix.framework.analyzer;

import com.zelix.framework.instructions.LabelNode;
import java.util.BitSet;

public class LocalsInitializationState extends BitSet {
	private LabelNode ownerLabel;

	public LocalsInitializationState(LabelNode ownerLabel, int nbits) {
		super(nbits);
		this.ownerLabel = ownerLabel;
	}

	public LabelNode getOwnerLabel() {
		return this.ownerLabel;
	}

	@Override
	public Object clone() {
		LocalsInitializationState clonedState = (LocalsInitializationState)super.clone();
		clonedState.ownerLabel = this.ownerLabel;
		return clonedState;
	}

	public boolean hasSameLabel(LocalsInitializationState otherState) {
		return this.ownerLabel == otherState.ownerLabel;
	}
}

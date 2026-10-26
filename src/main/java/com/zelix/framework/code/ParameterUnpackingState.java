package com.zelix.framework.code;

import java.util.ArrayList;
import java.util.List;

public class ParameterUnpackingState {
	private final InstructionListNode ownerMethodBody;
	private final int parameterIndex;
	private final String parameterDescriptor;
	private final List<Integer> accessInstructionIndices = new ArrayList<>();
	private boolean hasStoreAccess;
	private boolean needsHeadUnpacking = true;
	private boolean needsCast = true;

	ParameterUnpackingState(InstructionListNode ownerMethodBody, int parameterIndex, String parameterDescriptor) {
		this.ownerMethodBody = ownerMethodBody;
		this.parameterIndex = parameterIndex;
		this.parameterDescriptor = parameterDescriptor;
		if (parameterDescriptor.equals("Ljava/lang/Object;")) {
			this.clearNeedsCast();
		}
	}

	void addAccess(int instructionIndex, boolean isStore) {
		this.accessInstructionIndices.add(instructionIndex);
		if (isStore) {
			this.hasStoreAccess = true;
		}
	}

	boolean hasSingleAccess() {
		return this.accessInstructionIndices.size() == 1;
	}

	int getSingleAccessIndex() {
		return this.accessInstructionIndices.get(0);
	}

	void disableHeadUnpacking() {
		this.needsHeadUnpacking = false;
	}

	void clearNeedsCast() {
		this.needsCast = false;
	}

	String getDescriptor() {
		return this.parameterDescriptor;
	}

	boolean needsHeadUnpacking() {
		return this.needsHeadUnpacking;
	}

	boolean hasStoreAccess() {
		return this.hasStoreAccess;
	}

	boolean needsCast() {
		return this.needsCast;
	}
}

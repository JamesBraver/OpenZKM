package com.zelix.framework.analyzer.reference;

import com.zelix.framework.code.InstructionListNode;

public class PendingReference implements Reference {
	private int instructionIndex = -1;
	private InstructionListNode instructionList;

	public PendingReference(int instructionIndex, InstructionListNode instructionList) {
		this.instructionIndex = instructionIndex;
		this.instructionList = instructionList;
	}

	@Override
	public boolean isSingleValue() {
		return false;
	}

	public InstructionListNode getInstructionList() {
		return this.instructionList;
	}

	@Override
	public boolean isResolved() {
		return false;
	}

	public String getParentString() {
		return this.instructionList.getParentString();
	}

	@Override
	public String getTypeName() {
		return this.getValue();
	}

	@Override
	public int getInstructionIndex() {
		return this.instructionIndex;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PendingReference) {
			PendingReference other = (PendingReference)obj;
			return this.instructionIndex == other.instructionIndex;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.instructionIndex;
	}

	@Override
	public String getValue() {
		return null;
	}

	@Override
	public String getKey() {
		return this.getValue();
	}

	@Override
	public boolean isPending() {
		return true;
	}

	@Override
	public String getDisplayString() {
		return "<pending>";
	}
}

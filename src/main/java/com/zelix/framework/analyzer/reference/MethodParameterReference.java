package com.zelix.framework.analyzer.reference;

import com.zelix.framework.code.InstructionListNode;

public class MethodParameterReference implements Reference {
	private int slotIndex = -1;
	private InstructionListNode instructionList;

	public MethodParameterReference(int slotIndex, InstructionListNode instructionList) {
		this.slotIndex = slotIndex;
		this.instructionList = instructionList;
	}

	@Override
	public boolean isSingleValue() {
		return true;
	}

	@Override
	public boolean isResolved() {
		return false;
	}

	@Override
	public String getTypeName() {
		return this.getValue();
	}

	public InstructionListNode getInstructionList() {
		return this.instructionList;
	}

	public int getSlotIndex() {
		return this.slotIndex;
	}

	@Override
	public int getInstructionIndex() {
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MethodParameterReference)) {
			return false;
		} else {
			MethodParameterReference other = (MethodParameterReference)obj;
			return this.instructionList == other.instructionList && this.slotIndex == other.slotIndex;
		}
	}

	@Override
	public int hashCode() {
		return this.instructionList.hashCode() ^ this.slotIndex;
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
		return false;
	}

	@Override
	public String getDisplayString() {
		return "<method parameter " + this.slotIndex + ">";
	}
}

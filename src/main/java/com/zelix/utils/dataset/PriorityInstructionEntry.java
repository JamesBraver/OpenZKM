package com.zelix.utils.dataset;

public class PriorityInstructionEntry implements Comparable<PriorityInstructionEntry> {
	public float priority;
	public Object instructionListNode;

	public PriorityInstructionEntry(float priority, Object instructionListNode) {
		this.priority = priority;
		this.instructionListNode = instructionListNode;
	}

	@Override
	public int compareTo(PriorityInstructionEntry other) {
		return this.compareByPriority(other);
	}

	public int compareByPriority(PriorityInstructionEntry other) {
		return Float.compare(this.priority, other.priority);
	}
}

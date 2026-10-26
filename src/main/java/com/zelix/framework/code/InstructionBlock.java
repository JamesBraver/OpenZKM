package com.zelix.framework.code;

import com.zelix.framework.instructions.AbstractInsnNode;
import java.util.ArrayList;
import java.util.List;

public class InstructionBlock implements Comparable<InstructionBlock> {
	public int priority = 0;
	public List<AbstractInsnNode> newInstructions;
	public int offset;
	public int replacedInsnCount;
	public int additionalStackRequired;

	public InstructionBlock(List<AbstractInsnNode> newInstructions, int offset, int replacedInsnCount, int additionalStackRequired) {
		if (newInstructions != null) {
			this.newInstructions = newInstructions;
		}

		this.offset = offset;
		this.replacedInsnCount = replacedInsnCount;
		this.additionalStackRequired = additionalStackRequired;
	}

	public InstructionBlock(AbstractInsnNode instruction, int offset, int replacedInsnCount, int additionalStackRequired) {
		this.newInstructions = new ArrayList<>();
		if (instruction != null) {
			this.newInstructions.add(instruction);
		}

		this.offset = offset;
		this.replacedInsnCount = replacedInsnCount;
		this.additionalStackRequired = additionalStackRequired;
	}

	public final int compareToPatch(InstructionBlock other) {
		if (this.offset < other.offset) {
			return -1;
		} else if (this.offset == other.offset) {
			if (this.priority < other.priority) {
				return -1;
			} else {
				return this.priority == other.priority ? 0 : 1;
			}
		} else {
			return 1;
		}
	}

	public final void setPriority(int priority) {
		this.priority = priority;
	}

	public int getReplacedInsnCount() {
		return this.replacedInsnCount;
	}

	public int getOffset() {
		return this.offset;
	}

	@Override
	public int compareTo(InstructionBlock o) {
		return this.compareToPatch(o);
	}

	public boolean insertBeforeOpcode(int opcode, List<? extends AbstractInsnNode> instructions) {
		boolean inserted = false;
		ArrayList<AbstractInsnNode> newList = new ArrayList<>(this.newInstructions.size() + instructions.size());

		for (AbstractInsnNode currentInsn : this.newInstructions) {
			if (currentInsn.getOpcode() == opcode) {
				newList.addAll(instructions);
				inserted = true;
			}

			newList.add(currentInsn);
		}

		if (inserted) {
			this.newInstructions = newList;
		}

		return inserted;
	}

	public List<AbstractInsnNode> getNewInstructions() {
		return this.newInstructions;
	}

	public void setAdditionalStackRequired(int additionalStackRequired) {
		this.additionalStackRequired = additionalStackRequired;
	}

	public int getAdditionalStackRequired() {
		return this.additionalStackRequired;
	}
}

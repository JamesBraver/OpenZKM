package com.zelix.framework.code;

import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.LabelNode;
import java.util.ArrayList;
import java.util.List;

public class LabeledInstructionBlockPatch extends InstructionBlock {
	public boolean flag;
	public LabelNode labelNode;

	public LabeledInstructionBlockPatch(boolean flag, LabelNode labelNode, AbstractInsnNode instruction, int offset, int replacedInsnCount, int additionalStackRequired) {
		super(instruction, offset, replacedInsnCount, additionalStackRequired);
		this.flag = flag;
		this.labelNode = labelNode;
	}

	public void appendInstructions(List<AbstractInsnNode> instructions) {
		int instructionCount = instructions.size();

		for (int i = 0; i < instructionCount; i++) {
			this.newInstructions.add(instructions.get(i));
		}
	}

	public boolean isFlagged() {
		return this.flag;
	}

	public void prependInstructions(List<AbstractInsnNode> instructions) {
		if (this.newInstructions.size() == 0) {
			for (int instructionIndex = 0; instructionIndex < instructions.size(); instructionIndex++) {
				this.newInstructions.add(instructions.get(instructionIndex));
			}
		} else {
			ArrayList<AbstractInsnNode> newList = new ArrayList<>(this.newInstructions.size() + instructions.size());

			for (int i = 0; i < instructions.size(); i++) {
				newList.add(instructions.get(i));
			}

			for (int existingInstructionIndex = 0; existingInstructionIndex < this.newInstructions.size(); existingInstructionIndex++) {
				newList.add(this.newInstructions.get(existingInstructionIndex));
			}

			this.newInstructions = newList;
		}
	}

	public LabelNode getLabelNode() {
		return this.labelNode;
	}
}

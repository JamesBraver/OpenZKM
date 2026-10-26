package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class GotoNode extends JumpInsnNode {
	public GotoNode(ByteDataBuffer dataBuffer, int bytecodeOffset, MultiMap<Integer, IGraphElement> elementsByOffset) throws IOException {
		super(GOTO, dataBuffer, bytecodeOffset, elementsByOffset);
	}

	public GotoNode(LabelNode targetLabel) {
		super(GOTO, targetLabel);
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		return new FrameNode(frame.getOperandStack(), frame.getLocalVariables(), frame.getLocalsInitState(), frame.getActiveMonitors());
	}

	@Override
	public List<AbstractInsnNode> expandToWideIfNeeded() {
		int branchOffset = this.getBranchOffset();
		if (branchOffset >= -32768 && branchOffset <= 32767) {
			return null;
		} else {
			ArrayList<AbstractInsnNode> instructions = new ArrayList<>(1);
			instructions.add(new WideJumpInsnNode(GOTO_W, this.targetLabel));
			return instructions;
		}
	}
}

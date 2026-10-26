package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class JSRNode extends JumpInsnNode {
	public JSRNode(LabelNode targetLabel) {
		super(JSR, targetLabel);
	}

	public JSRNode(ByteDataBuffer dataBuffer, int bytecodeOffset, MultiMap<Integer, IGraphElement> elementsByOffset) throws IOException {
		super(JSR, dataBuffer, bytecodeOffset, elementsByOffset);
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		StackFrameType[] frameOperandStack = frame.getOperandStack();
		StackFrameType[] frameLocalVariables = frame.getLocalVariables();
		int stackSize = frameOperandStack.length;
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
		System.arraycopy(frameOperandStack, 0, newOperandStack, 0, stackSize);
		newOperandStack[stackSize] = StackFrameType.RETURN_ADDRESS;
		return new FrameNode(newOperandStack, frameLocalVariables, frame.getLocalsInitState(), frame.getActiveMonitors());
	}

	@Override
	public boolean isJSR() {
		return true;
	}

	@Override
	public List<AbstractInsnNode> expandToWideIfNeeded() {
		int branchOffset = this.getBranchOffset();
		if (branchOffset >= -32768 && branchOffset <= 32767) {
			return null;
		} else {
			ArrayList<AbstractInsnNode> instructions = new ArrayList<>(1);
			instructions.add(new WideJumpInsnNode(JSR_W, this.targetLabel));
			return instructions;
		}
	}
}

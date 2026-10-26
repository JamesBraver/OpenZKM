package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.JSR_W;
import static com.zelix.framework.instructions.OpcodeConstants.GOTO_W;

public class WideJumpInsnNode extends JumpInsnNode {
	public WideJumpInsnNode(int opcode, ByteDataBuffer dataBuffer, int bytecodeOffset, MultiMap<Integer, IGraphElement> elementsByOffset) throws IOException {
		super(opcode, dataBuffer, bytecodeOffset, elementsByOffset);
	}

	public WideJumpInsnNode(int opcode, LabelNode targetLabel) {
		super(opcode, targetLabel);
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		StackFrameType[] frameLocalVariables = frame.getLocalVariables();
		StackFrameType[] frameOperandStack = frame.getOperandStack();
		int stackSize = frameOperandStack.length;
		Set<MonitorEnterNode> frameActiveMonitors = frame.getActiveMonitors();
		switch (this.opcode) {
			case GOTO_W:
				return new FrameNode(frameOperandStack, frameLocalVariables, frame.getLocalsInitState(), frameActiveMonitors);
			case JSR_W:
				StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
				System.arraycopy(frameOperandStack, 0, newOperandStack, 0, stackSize);
				newOperandStack[stackSize] = StackFrameType.RETURN_ADDRESS;
				return new FrameNode(newOperandStack, frameLocalVariables, frame.getLocalsInitState(), frameActiveMonitors);
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return null;
		}
	}

	@Override
	public int readBranchOffset(ByteDataBuffer dataBuffer) throws IOException {
		int offsetDelta = dataBuffer.readInt();
		return this.bytecodeOffset + offsetDelta;
	}

	@Override
	public boolean isJSR() {
		return this.getOpcode() == JSR_W;
	}

	@Override
	public List<AbstractInsnNode> expandToWideIfNeeded() {
		return null;
	}

	@Override
	public int getBytecodeSize() {
		return 5;
	}

	@Override
	public void writeBranchOffset(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeInt(this.getBranchOffset());
	}
}

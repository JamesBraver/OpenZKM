package com.zelix.framework.analyzer;

public class CompactFrame extends AnalyzedFrame {
	private static CompactFrame emptyFrame = new CompactFrame();

	public CompactFrame(FrameNode frameNode) {
		StackFrameType[] operandStack = frameNode.getOperandStack();
		StringBuilder frameKeyBuffer = new StringBuilder();
		super.stackDepth = 0;
		int operandStackCount = operandStack.length;

		for (int operandIndex = 0; operandIndex < operandStackCount; operandIndex++) {
			StackFrameType operandType = operandStack[operandIndex];
			super.stackDepth = super.stackDepth + operandType.getStackSize();
			if (!operandType.isResolved()) {
				super.unresolved = true;
			}

			frameKeyBuffer.append(operandType.getFrameKey());
			if (operandIndex < operandStackCount - 1) {
				frameKeyBuffer.append("|");
			}
		}

		frameKeyBuffer.append("||");
		StackFrameType[] localVariables = frameNode.getLocalVariables();
		int localVariableCount = localVariables.length;

		for (int localVarIndex = 0; localVarIndex < localVariableCount; localVarIndex++) {
			StackFrameType localVariableType = localVariables[localVarIndex];
			if (!localVariableType.isResolved()) {
				super.unresolved = true;
			}

			frameKeyBuffer.append(localVariableType.getFrameKey());
			if (localVarIndex < localVariableCount - 1) {
				frameKeyBuffer.append("|");
			}
		}

		LocalsInitializationState localsInitState = frameNode.getLocalsInitState();
		if (localsInitState != null) {
			frameKeyBuffer.append("|||");
			frameKeyBuffer.append(localsInitState.getOwnerLabel().hashCode());
		}

		super.frameKey = frameKeyBuffer.toString();
	}

	private CompactFrame() {
		super.frameKey = "";
		super.stackDepth = 0;
		super.unresolved = true;
	}
}

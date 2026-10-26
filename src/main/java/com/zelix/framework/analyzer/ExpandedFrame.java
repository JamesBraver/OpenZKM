package com.zelix.framework.analyzer;

public class ExpandedFrame extends AnalyzedFrame {
	private static ExpandedFrame emptyFrame = new ExpandedFrame();

	public ExpandedFrame(FrameNode frameNode) {
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
		boolean unresolvedMarkAppended = false;
		StackFrameType[] localVariables = frameNode.getLocalVariables();
		int localVariableCount = localVariables.length;

		for (int localVarIndex = 0; localVarIndex < localVariableCount; localVarIndex++) {
			if (!localVariables[localVarIndex].isResolved()) {
				if (!unresolvedMarkAppended) {
					frameKeyBuffer.append("**");
				}

				unresolvedMarkAppended = true;
				super.unresolved = true;
			}
		}

		LocalsInitializationState localsInitState = frameNode.getLocalsInitState();
		if (localsInitState != null) {
			frameKeyBuffer.append("|||");
			frameKeyBuffer.append(localsInitState.getOwnerLabel().hashCode());
		}

		super.frameKey = frameKeyBuffer.toString();
	}

	private ExpandedFrame() {
		super.frameKey = "";
		super.stackDepth = 0;
		super.unresolved = true;
	}
}

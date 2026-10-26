package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.LocalsInitializationState;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import java.io.PrintWriter;

public abstract class AbstractMonitorNode extends AbstractInsnNode {
	public StackFrameType monitorType;

	public AbstractMonitorNode(int opcode) {
		super(opcode);
	}

	@Override
	public int getStackEffect() {
		return -1;
	}

	@Override
	public final void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		textBuffer.append((CharSequence)indent);
		textBuffer.append((CharSequence)indent);
		textBuffer.append(this.getOpcodeMnemonics());
		String comment = AbstractInsnNode.getOpcodeComment(this.opcode);
		if (comment.length() > 0) {
			textBuffer.append("\t" + comment);
		}

		printWriter.println(textBuffer.toString());
	}

	@Override
	public boolean isMethodExit() {
		return false;
	}

	public StackFrameType getMonitorType() {
		return this.monitorType;
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		StackFrameType[] localVariables = frame.getLocalVariables();
		StackFrameType[] operandStack = frame.getOperandStack();
		int operandStackSize = operandStack.length;
		LocalsInitializationState localsInitState = frame.getLocalsInitState();
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(operandStackSize - 1);
		System.arraycopy(operandStack, 0, newOperandStack, 0, operandStackSize - 1);
		this.monitorType = operandStack[operandStack.length - 1];
		FrameNode newFrame = new FrameNode(newOperandStack, localVariables, localsInitState, frame.getActiveMonitors());
		this.updateFrameMonitors(newFrame);
		return newFrame;
	}

	public abstract void updateFrameMonitors(FrameNode frame);

	@Override
	public boolean isStackProducer() {
		return false;
	}

	@Override
	public boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		return false;
	}

	@Override
	public boolean isCategory2Producer() {
		return false;
	}

	@Override
	public boolean isStandaloneStackProducer() {
		return false;
	}

	@Override
	public boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		return stackIndex >= stackSize;
	}

	@Override
	public boolean canFallThrough() {
		return true;
	}

	@Override
	public boolean isMonitor() {
		return true;
	}

	@Override
	public String getOpcodeText() {
		return this.getOpcodeMnemonics();
	}
}

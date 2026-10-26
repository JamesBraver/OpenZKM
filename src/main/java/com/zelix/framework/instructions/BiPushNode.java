package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.utils.dataset.ByteDataBuffer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static com.zelix.framework.instructions.OpcodeConstants.BIPUSH;

public class BiPushNode extends AbstractInsnNode {
	public int value;

	public BiPushNode(int value) {
		super(BIPUSH);
		this.value = value;
	}

	public BiPushNode(ByteDataBuffer dataBuffer) throws IOException {
		super(BIPUSH);
		this.value = dataBuffer.readByte();
	}

	@Override
	public int getStackEffect() {
		return 1;
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		textBuffer.append(this.getOpcodeText());
		String comment = AbstractInsnNode.fillCommentPlaceholder(AbstractInsnNode.getOpcodeComment(this.opcode), String.valueOf(this.value));
		if (comment.length() > 0) {
			textBuffer.append("\t" + comment);
		}

		printWriter.println(indent.toString() + indent.toString() + textBuffer);
	}

	@Override
	public final boolean isMethodExit() {
		return false;
	}

	@Override
	public boolean isIntegerOpcode() {
		return true;
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		StackFrameType[] operandStack = frame.getOperandStack();
		StackFrameType[] localVariables = frame.getLocalVariables();
		int operandStackSize = operandStack.length;
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
		System.arraycopy(operandStack, 0, newOperandStack, 0, operandStackSize);
		newOperandStack[operandStackSize] = StackFrameType.INT;
		return new FrameNode(newOperandStack, localVariables, frame.getLocalsInitState(), frame.getActiveMonitors());
	}

	public int getValue() {
		return this.value;
	}

	@Override
	public boolean isStackProducer() {
		return true;
	}

	@Override
	public final boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		return false;
	}

	@Override
	public boolean isCategory2Producer() {
		return false;
	}

	@Override
	public int getBytecodeSize() {
		return 2;
	}

	@Override
	public boolean isStandaloneStackProducer() {
		return true;
	}

	@Override
	public final boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		return false;
	}

	@Override
	public final boolean canFallThrough() {
		return true;
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeByte(this.value);
	}

	@Override
	public String getOpcodeText() {
		StringBuilder textBuffer = new StringBuilder();
		textBuffer.append(this.getOpcodeMnemonics());
		textBuffer.append(' ');
		textBuffer.append(this.value);
		return textBuffer.toString();
	}
}

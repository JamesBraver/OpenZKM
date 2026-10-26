package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.ByteDataBuffer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static com.zelix.framework.instructions.OpcodeConstants.NEWARRAY;

public class NewArrayNode extends AbstractInsnNode {
	private static String[] arrayTypeNames = new String[12];
	private static Map<String, Integer> atypeByDescriptor = GlobalUtils.newHashMap();
	private int arrayType;

	public NewArrayNode(ByteDataBuffer dataBuffer) throws IOException {
		super(NEWARRAY);
		this.arrayType = dataBuffer.read();
	}

	public NewArrayNode(String typeDescriptor) {
		super(NEWARRAY);
		this.arrayType = atypeByDescriptor.get(typeDescriptor);
	}

	public NewArrayNode(int arrayType) {
		super(NEWARRAY);
		this.arrayType = arrayType;
	}

	@Override
	public int getStackEffect() {
		return 0;
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		String mnemonics = this.getOpcodeMnemonics();
		textBuffer.append(mnemonics + " " + this.arrayType);
		String arrayTypeName = arrayTypeNames[this.arrayType];
		String comment = this.getOpcodeComment();
		comment = AbstractInsnNode.fillCommentPlaceholder(comment, arrayTypeName);
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
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		StackFrameType[] operandStack = frame.getOperandStack();
		StackFrameType[] localVariables = frame.getLocalVariables();
		int operandStackSize = operandStack.length;
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(operandStackSize);
		System.arraycopy(operandStack, 0, newOperandStack, 0, operandStackSize - 1);
		switch (this.arrayType) {
			case 4:
				newOperandStack[operandStackSize - 1] = StackFrameType.BOOLEAN_ARRAY;
				break;
			case 5:
				newOperandStack[operandStackSize - 1] = StackFrameType.CHAR_ARRAY;
				break;
			case 6:
				newOperandStack[operandStackSize - 1] = StackFrameType.FLOAT_ARRAY;
				break;
			case 7:
				newOperandStack[operandStackSize - 1] = StackFrameType.DOUBLE_ARRAY;
				break;
			case 8:
				newOperandStack[operandStackSize - 1] = StackFrameType.BYTE_ARRAY;
				break;
			case 9:
				newOperandStack[operandStackSize - 1] = StackFrameType.SHORT_ARRAY;
				break;
			case 10:
				newOperandStack[operandStackSize - 1] = StackFrameType.INT_ARRAY;
				break;
			case 11:
				newOperandStack[operandStackSize - 1] = StackFrameType.LONG_ARRAY;
				break;
			default:
				DebugUtils.assertTrue(false, this.arrayType);
		}

		return new FrameNode(newOperandStack, localVariables, frame.getLocalsInitState(), frame.getActiveMonitors());
	}

	@Override
	public boolean isStackProducer() {
		return true;
	}

	@Override
	public final boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		return stackIndex == stackSize - 1;
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
		return false;
	}

	@Override
	public final boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		return stackIndex == stackSize - 1;
	}

	@Override
	public final boolean canFallThrough() {
		return true;
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeByte(this.arrayType);
	}

	@Override
	public String getOpcodeText() {
		StringBuilder textBuffer = new StringBuilder();
		textBuffer.append(this.getOpcodeMnemonics());
		textBuffer.append(' ');
		textBuffer.append(arrayTypeNames[this.arrayType]);
		return textBuffer.toString();
	}

	static {
		arrayTypeNames[4] = "boolean";
		arrayTypeNames[5] = "char";
		arrayTypeNames[6] = "float";
		arrayTypeNames[7] = "double";
		arrayTypeNames[8] = "byte";
		arrayTypeNames[9] = "short";
		arrayTypeNames[10] = "int";
		arrayTypeNames[11] = "long";
		atypeByDescriptor.put("Z", 4);
		atypeByDescriptor.put("C", 5);
		atypeByDescriptor.put("F", 6);
		atypeByDescriptor.put("D", 7);
		atypeByDescriptor.put("B", 8);
		atypeByDescriptor.put("S", 9);
		atypeByDescriptor.put("I", 10);
		atypeByDescriptor.put("J", 11);
	}
}

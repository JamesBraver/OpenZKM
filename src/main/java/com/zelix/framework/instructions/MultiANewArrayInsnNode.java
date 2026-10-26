package com.zelix.framework.instructions;

import com.zelix.framework.TypeNode;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.constant.ConstantPool;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import static com.zelix.framework.instructions.OpcodeConstants.*;

public class MultiANewArrayInsnNode extends WideConstantPoolInsnNode {
	private int dimensions;

	public MultiANewArrayInsnNode(
		ByteDataBuffer dataBuffer,
		ConstantPool constantPool,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences
	) throws IOException {
		super(MULTIANEWARRAY, dataBuffer, constantPool, stringReferences, integerReferences, longReferences, classReferences, memberReferences);
		this.dimensions = dataBuffer.read();
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		String mnemonics = this.getOpcodeMnemonics();
		textBuffer.append(mnemonics + " " + this.constantOperand.getTag() + " " + this.dimensions);
		String comment = AbstractInsnNode.fillCommentPlaceholder(
			AbstractInsnNode.fillCommentPlaceholder(AbstractInsnNode.getOpcodeComment(this.opcode), this.constantOperand.getValueAsString()), String.valueOf(this.dimensions)
		);
		if (comment.length() > 0) {
			textBuffer.append("\t" + comment);
		}

		printWriter.println(indent.toString() + indent.toString() + textBuffer);
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		StackFrameType[] operandStack = frame.getOperandStack();
		StackFrameType[] localVariables = frame.getLocalVariables();
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(operandStack.length - this.dimensions + 1);
		int newOperandStackSize = newOperandStack.length;
		System.arraycopy(operandStack, 0, newOperandStack, 0, newOperandStackSize - 1);
		String arrayDescriptor = ((ClassInfoWriter)this.constantOperand).getDescriptor();
		newOperandStack[newOperandStackSize - 1] = StackFrameType.getResolved(arrayDescriptor);
		return new FrameNode(newOperandStack, localVariables, frame.getLocalsInitState(), frame.getActiveMonitors());
	}

	@Override
	public boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		return stackIndex >= stackSize - 1;
	}

	@Override
	public int getBytecodeSize() {
		return 4;
	}

	@Override
	public boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		return stackIndex >= stackSize - 1;
	}

	@Override
	public void writeWithConstantMap(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> typeWriterMapping) throws IOException {
		super.writeWithConstantMap(dataOutputStream, typeWriterMapping);
		dataOutputStream.writeByte(this.dimensions);
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeByte(this.dimensions);
	}

	@Override
	public String getOpcodeText() {
		StringBuilder textBuffer = new StringBuilder();
		textBuffer.append(this.getOpcodeMnemonics());
		textBuffer.append(' ');
		textBuffer.append(this.constantOperand.getValueAsString());
		textBuffer.append(' ');
		textBuffer.append(this.dimensions);
		return textBuffer.toString();
	}
}

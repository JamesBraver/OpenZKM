package com.zelix.framework.instructions;

import com.zelix.framework.TypeNode;
import com.zelix.framework.constant.ConstantPool;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.InterfaceMethodCallWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static com.zelix.framework.instructions.OpcodeConstants.INVOKEINTERFACE;

public class InvokeInterfaceNode extends WideConstantPoolInsnNode {
	private int parameterStackSize;

	public InvokeInterfaceNode(
		ByteDataBuffer dataBuffer,
		ConstantPool constantPool,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences
	) throws IOException {
		super(INVOKEINTERFACE, dataBuffer, constantPool, stringReferences, integerReferences, longReferences, classReferences, memberReferences);
		this.parameterStackSize = dataBuffer.read();
		dataBuffer.skip(1L);
	}

	public InvokeInterfaceNode(InterfaceMethodCallWriter constantOperand) {
		super(INVOKEINTERFACE, constantOperand);
		this.parameterStackSize = constantOperand.getParameterStackSize() + 1;
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		String mnemonics = this.getOpcodeMnemonics();
		textBuffer.append(mnemonics + " " + this.constantOperand.getTag() + " " + this.parameterStackSize + " " + 0);
		String comment = AbstractInsnNode.fillCommentPlaceholder(
			AbstractInsnNode.fillCommentPlaceholder(AbstractInsnNode.getOpcodeComment(this.opcode), this.constantOperand.getValueAsString()), String.valueOf(this.parameterStackSize)
		);
		if (comment.length() > 0) {
			textBuffer.append("\t" + comment);
		}

		printWriter.println(indent.toString() + indent.toString() + textBuffer);
	}

	@Override
	public int getBytecodeSize() {
		return 5;
	}

	@Override
	public void writeWithConstantMap(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> typeWriterMapping) throws IOException {
		super.writeWithConstantMap(dataOutputStream, typeWriterMapping);
		dataOutputStream.writeByte(this.parameterStackSize);
		dataOutputStream.writeByte(0);
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeByte(this.parameterStackSize);
		dataOutputStream.writeByte(0);
	}

	@Override
	public String getOpcodeText() {
		StringBuilder textBuffer = new StringBuilder();
		textBuffer.append(this.getOpcodeMnemonics());
		textBuffer.append(' ');
		textBuffer.append(this.constantOperand.getValueAsString());
		return textBuffer.toString();
	}

	public void setParameterStackSize(int parameterStackSize) {
		this.parameterStackSize = parameterStackSize;
	}
}

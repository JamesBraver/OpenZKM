package com.zelix.framework.instructions;

import com.zelix.framework.TypeNode;
import com.zelix.framework.constant.ConstantPool;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.IDynamicCallUpdatable;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class InvokeDynamicNode extends WideConstantPoolInsnNode {
	public InvokeDynamicNode(InvokeDynamicCallWriter constantOperand) {
		super(INVOKEDYNAMIC, constantOperand);
	}

	public InvokeDynamicNode(
		ByteDataBuffer dataBuffer,
		ConstantPool constantPool,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences
	) throws IOException {
		super(INVOKEDYNAMIC, dataBuffer, constantPool, stringReferences, integerReferences, longReferences, classReferences, memberReferences);
		dataBuffer.skip(2L);
		dynamicCallReferences.put((InvokeDynamicCallWriter)this.constantOperand, this);
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		String mnemonics = this.getOpcodeMnemonics();
		InvokeDynamicCallWriter callWriter = (InvokeDynamicCallWriter)this.constantOperand;
		textBuffer.append(mnemonics + " " + (callWriter != null ? callWriter.getTag() : "null"));
		String comment = AbstractInsnNode.getOpcodeComment(this.opcode);
		comment = AbstractInsnNode.fillCommentPlaceholder(comment, callWriter != null ? callWriter.getTargetValueAsString() : "null");
		comment = AbstractInsnNode.fillCommentPlaceholder(comment, callWriter != null ? (callWriter.getBootstrapMethodHandle() != null ? callWriter.getBootstrapMethodHandle().toDisplayString() : "null") : "null");
		if (comment.length() > 0) {
			textBuffer.append("\t" + comment);
		}

		printWriter.println(indent.toString() + indent.toString() + textBuffer);
	}

	@Override
	public TypeWriter getTypeWriter() {
		return this.getIndyCallWriter();
	}

	@Override
	public int getBytecodeSize() {
		return 5;
	}

	@Override
	public void writeWithConstantMap(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> typeWriterMapping) throws IOException {
		super.writeWithConstantMap(dataOutputStream, typeWriterMapping);
		dataOutputStream.writeShort(0);
	}

	public InvokeDynamicCallWriter getIndyCallWriter() {
		return (InvokeDynamicCallWriter)this.constantOperand;
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeShort(0);
	}

	@Override
	public String getOpcodeText() {
		StringBuilder textBuffer = new StringBuilder();
		textBuffer.append(this.getOpcodeMnemonics());
		textBuffer.append(' ');
		InvokeDynamicCallWriter callWriter = (InvokeDynamicCallWriter)this.constantOperand;
		textBuffer.append(callWriter.getTag());
		return textBuffer.toString();
	}
}

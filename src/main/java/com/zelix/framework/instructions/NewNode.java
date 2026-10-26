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
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;

import static com.zelix.framework.instructions.OpcodeConstants.NEW;

public class NewNode extends WideConstantPoolInsnNode {
	private int bytecodeOffset = -1;

	public NewNode(TypeWriter constantOperand) {
		super(NEW, constantOperand);
	}

	public NewNode(
		ByteDataBuffer dataBuffer,
		ConstantPool constantPool,
		int bytecodeOffset,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences
	) throws IOException {
		super(NEW, dataBuffer, constantPool, stringReferences, integerReferences, longReferences, classReferences, memberReferences);
		this.bytecodeOffset = bytecodeOffset;
	}

	public int getBytecodeOffset() {
		return this.bytecodeOffset;
	}

	@Override
	public void setOffset(int bytecodeOffset) {
		this.bytecodeOffset = bytecodeOffset;
	}
}

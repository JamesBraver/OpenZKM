package com.zelix.framework.attributes.stackmap;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractStackMapAttribute;
import com.zelix.framework.attributes.code.StackMapTableAttribute;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class FullFrame extends AbstractStackMapFrame {
	private final int frameType = 255;
	private final int offsetDelta;
	private final VerificationTypeInfo[] locals;
	private final VerificationTypeInfo[] stack;

	public FullFrame(
		int frameType,
		StructureNode stackMapTableAttribute,
		ByteDataBuffer byteDataBuffer,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter,
		AtomicInt lastOffset,
		Map<Integer, VerificationTypeInfo> primitiveTypes,
		Map<Integer, VerificationTypeInfo> objectTypes
	) throws IOException {
		super(stackMapTableAttribute);
		IntegerCache integerCache = IntegerCache.getInstance();
		this.offsetDelta = byteDataBuffer.readUnsignedShort();
		int localCount = byteDataBuffer.readUnsignedShort();
		this.locals = new VerificationTypeInfo[localCount];

		for (int localIndex = 0; localIndex < localCount; localIndex++) {
			this.locals[localIndex] = VerificationTypeInfo.readFromBuffer((AbstractStackMapAttribute)stackMapTableAttribute, byteDataBuffer, elementsByOffset, classReferences, printWriter, primitiveTypes, objectTypes, integerCache);
			if (!this.locals[localIndex].isValid()) {
				this.isValid = false;
			}
		}

		int stackCount = byteDataBuffer.readUnsignedShort();
		this.stack = new VerificationTypeInfo[stackCount];

		for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
			this.stack[stackIndex] = VerificationTypeInfo.readFromBuffer((AbstractStackMapAttribute)stackMapTableAttribute, byteDataBuffer, elementsByOffset, classReferences, printWriter, primitiveTypes, objectTypes, integerCache);
			if (!this.stack[stackIndex].isValid()) {
				this.isValid = false;
			}
		}

		int previousOffset = lastOffset.get();
		if (previousOffset == -1) {
			this.labelOffset = this.offsetDelta;
		} else {
			this.labelOffset = previousOffset + 1 + this.offsetDelta;
		}

		lastOffset.set(this.labelOffset);
		elementsByOffset.put(integerCache.valueOf(this.labelOffset), this);
	}

	public FullFrame(StackMapTableAttribute stackMapTableAttribute, int offsetDelta, LabelNode labelNode, VerificationTypeInfo[] stack, VerificationTypeInfo[] locals) {
		super(stackMapTableAttribute);
		this.labelNode = labelNode;
		this.offsetDelta = offsetDelta;
		this.labelOffset = labelNode.getOffset();
		this.stack = stack;
		this.locals = locals;
	}

	@Override
	public int getSerializedSize() {
		int serializedSize = 1;
		int localCount = this.locals.length;
		int stackCount = this.stack.length;
		serializedSize += 2;
		serializedSize += 2;

		for (int localIndex = 0; localIndex < localCount; localIndex++) {
			serializedSize += this.locals[localIndex].getSerializedSize();
		}

		serializedSize += 2;

		for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
			serializedSize += this.stack[stackIndex].getSerializedSize();
		}

		return serializedSize;
	}

	@Override
	public int getFrameType() {
		return 255;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		int localCount = this.locals.length;
		int stackCount = this.stack.length;

		for (int localIndex = 0; localIndex < localCount; localIndex++) {
			this.locals[localIndex].acceptVisitor(visitor);
		}

		for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
			this.stack[stackIndex].acceptVisitor(visitor);
		}
	}

	@Override
	public void writeMapped(DataOutputStream dataOutputStream, AtomicInt lastOffset, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		int localCount = this.locals.length;
		int stackCount = this.stack.length;
		dataOutputStream.writeByte(255);
		dataOutputStream.writeShort(this.offsetDelta);
		dataOutputStream.writeShort(localCount);

		for (int localIndex = 0; localIndex < localCount; localIndex++) {
			this.locals[localIndex].writeToStream(dataOutputStream, writerMap);
		}

		dataOutputStream.writeShort(stackCount);

		for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
			this.stack[stackIndex].writeToStream(dataOutputStream, writerMap);
		}
	}
}

package com.zelix.framework.attributes.stackmap;

import com.zelix.framework.StructureNode;
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

public class SameLocals1StackItemFrame extends AbstractStackMapFrame {
	public final int frameType;
	public final VerificationTypeInfo stackItem;

	public SameLocals1StackItemFrame(
		int frameType,
		StructureNode parent,
		ByteDataBuffer buf,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter,
		AtomicInt previousOffset,
		Map<Integer, VerificationTypeInfo> primitiveTypeCache,
		Map<Integer, VerificationTypeInfo> objectTypeCache
	) throws IOException {
		super(parent);
		this.frameType = frameType;
		IntegerCache integerCache = IntegerCache.getInstance();
		int offsetDelta = this.frameType - 64;
		this.stackItem = VerificationTypeInfo.readFromBuffer((StackMapTableAttribute)parent, buf, elementsByOffset, classReferences, printWriter, primitiveTypeCache, objectTypeCache, integerCache);
		if (!this.stackItem.isValid()) {
			this.isValid = false;
		}

		int previousFrameOffset = previousOffset.get();
		if (previousFrameOffset == -1) {
			this.labelOffset = offsetDelta;
		} else {
			this.labelOffset = previousFrameOffset + 1 + offsetDelta;
		}

		previousOffset.set(this.labelOffset);
		elementsByOffset.put(integerCache.valueOf(this.labelOffset), this);
	}

	public SameLocals1StackItemFrame(StackMapTableAttribute stackMapAttribute, int frameType, LabelNode labelNode, VerificationTypeInfo stackItem) {
		super(stackMapAttribute);
		this.labelNode = labelNode;
		this.frameType = frameType;
		this.stackItem = stackItem;
	}

	@Override
	public int getSerializedSize() {
		return 1 + this.stackItem.getSerializedSize();
	}

	@Override
	public int getFrameType() {
		return this.frameType;
	}

	@Override
	public final void acceptVisitor(ConstantPoolVisitor visitor) {
		this.stackItem.acceptVisitor(visitor);
	}

	@Override
	public void writeMapped(DataOutputStream dataOutputStream, AtomicInt lastOffset, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		dataOutputStream.writeByte(this.frameType);
		this.stackItem.writeToStream(dataOutputStream, writerMap);
	}
}

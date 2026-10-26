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

public class SameLocals1StackItemFrameExtended extends AbstractStackMapFrame {
	private final int offsetDelta;
	private final VerificationTypeInfo stackItem;

	public SameLocals1StackItemFrameExtended(
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
		IntegerCache integerCache = IntegerCache.getInstance();
		this.offsetDelta = buf.readUnsignedShort();
		this.stackItem = VerificationTypeInfo.readFromBuffer((StackMapTableAttribute)parent, buf, elementsByOffset, classReferences, printWriter, primitiveTypeCache, objectTypeCache, integerCache);
		if (!this.stackItem.isValid()) {
			this.isValid = false;
		}

		int previousFrameOffset = previousOffset.get();
		if (previousFrameOffset == -1) {
			this.labelOffset = this.offsetDelta;
		} else {
			this.labelOffset = previousFrameOffset + 1 + this.offsetDelta;
		}

		previousOffset.set(this.labelOffset);
		elementsByOffset.put(integerCache.valueOf(this.labelOffset), this);
	}

	public SameLocals1StackItemFrameExtended(StackMapTableAttribute stackMapAttribute, int offsetDelta, LabelNode labelNode, VerificationTypeInfo stackItem) {
		super(stackMapAttribute);
		this.labelNode = labelNode;
		this.offsetDelta = offsetDelta;
		this.stackItem = stackItem;
	}

	@Override
	public int getSerializedSize() {
		int serializedSize = 1;
		serializedSize += 2;
		return serializedSize + this.stackItem.getSerializedSize();
	}

	@Override
	public int getFrameType() {
		return 247;
	}

	@Override
	public final void acceptVisitor(ConstantPoolVisitor visitor) {
		this.stackItem.acceptVisitor(visitor);
	}

	@Override
	public void writeMapped(DataOutputStream dataOutputStream, AtomicInt lastOffset, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		dataOutputStream.writeByte(this.getFrameType());
		dataOutputStream.writeShort(this.offsetDelta);
		this.stackItem.writeToStream(dataOutputStream, writerMap);
	}
}

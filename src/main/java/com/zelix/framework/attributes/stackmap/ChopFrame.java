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

public class ChopFrame extends AbstractStackMapFrame {
	private final int frameType;
	private final int offsetDelta;

	public ChopFrame(
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
		this.offsetDelta = buf.readUnsignedShort();
		int previousFrameOffset = previousOffset.get();
		if (previousFrameOffset == -1) {
			this.labelOffset = this.offsetDelta;
		} else {
			this.labelOffset = previousFrameOffset + 1 + this.offsetDelta;
		}

		previousOffset.set(this.labelOffset);
		elementsByOffset.put(integerCache.valueOf(this.labelOffset), this);
	}

	public ChopFrame(StackMapTableAttribute stackMapAttribute, int frameType, int offsetDelta, LabelNode labelNode) {
		super(stackMapAttribute);
		this.frameType = frameType;
		this.labelNode = labelNode;
		this.offsetDelta = offsetDelta;
	}

	@Override
	public int getSerializedSize() {
		byte serializedSize = 1;
		return serializedSize + 2;
	}

	@Override
	public int getFrameType() {
		return this.frameType;
	}

	@Override
	public final void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	@Override
	public void writeMapped(DataOutputStream dataOutputStream, AtomicInt lastOffset, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		dataOutputStream.writeByte(this.frameType);
		dataOutputStream.writeShort(this.offsetDelta);
	}
}

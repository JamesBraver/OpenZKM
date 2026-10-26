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

public class SameFrame extends AbstractStackMapFrame {
	public final int frameType;

	public SameFrame(StackMapTableAttribute stackMapTableAttribute, int frameType, LabelNode labelNode) {
		super(stackMapTableAttribute);
		this.frameType = frameType;
		this.labelNode = labelNode;
		this.labelOffset = labelNode.getOffset();
		this.isValid = true;
	}

	public SameFrame(
		int frameType,
		StructureNode parent,
		ByteDataBuffer byteDataBuffer,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter,
		AtomicInt previousLabelOffset,
		Map<Integer, VerificationTypeInfo> primitiveTypeCache,
		Map<Integer, VerificationTypeInfo> objectTypeCache
	) {
		super(parent);
		this.frameType = frameType;
		IntegerCache integerCache = IntegerCache.getInstance();
		int offsetDelta = this.frameType;
		int previousOffset = previousLabelOffset.get();
		if (previousOffset == -1) {
			this.labelOffset = offsetDelta;
		} else {
			this.labelOffset = previousOffset + 1 + offsetDelta;
		}

		previousLabelOffset.set(this.labelOffset);
		elementsByOffset.put(integerCache.valueOf(this.labelOffset), this);
	}

	@Override
	public int getSerializedSize() {
		return 1;
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
	}
}

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

public class AppendFrame extends AbstractStackMapFrame {
	private final int frameType;
	private final int offsetDelta;
	private final VerificationTypeInfo[] locals;

	public AppendFrame(StackMapTableAttribute stackMapTableAttribute, int frameType, int offsetDelta, LabelNode labelNode, VerificationTypeInfo[] locals) {
		super(stackMapTableAttribute);
		this.frameType = frameType;
		this.labelNode = labelNode;
		this.offsetDelta = offsetDelta;
		this.locals = locals;
	}

	public AppendFrame(
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
		this.frameType = frameType;
		IntegerCache integerCache = IntegerCache.getInstance();
		this.offsetDelta = byteDataBuffer.readUnsignedShort();
		int appendedLocalsCount = this.frameType - 251;
		this.locals = new VerificationTypeInfo[appendedLocalsCount];

		for (int localIndex = 0; localIndex < appendedLocalsCount; localIndex++) {
			this.locals[localIndex] = VerificationTypeInfo.readFromBuffer((StackMapTableAttribute)stackMapTableAttribute, byteDataBuffer, elementsByOffset, classReferences, printWriter, primitiveTypes, objectTypes, integerCache);
			if (!this.locals[localIndex].isValid()) {
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

	@Override
	public int getSerializedSize() {
		int serializedSize = 1;
		serializedSize += 2;

		for (VerificationTypeInfo typeInfo : this.locals) {
			serializedSize += typeInfo.getSerializedSize();
		}

		return serializedSize;
	}

	@Override
	public int getFrameType() {
		return this.frameType;
	}

	@Override
	public final void acceptVisitor(ConstantPoolVisitor visitor) {
		VerificationTypeInfo[] typeInfos = this.locals;
		int typeInfoCount = typeInfos.length;

		for (int typeInfoIndex = 0; typeInfoIndex < typeInfoCount; typeInfoIndex++) {
			typeInfos[typeInfoIndex].acceptVisitor(visitor);
		}
	}

	public VerificationTypeInfo[] getLocals() {
		return (VerificationTypeInfo[])this.locals.clone();
	}

	@Override
	public void writeMapped(DataOutputStream dataOutputStream, AtomicInt lastOffset, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		dataOutputStream.writeByte(this.frameType);
		dataOutputStream.writeShort(this.offsetDelta);
		VerificationTypeInfo[] typeInfos = this.locals;
		int typeInfoCount = typeInfos.length;

		for (int typeInfoIndex = 0; typeInfoIndex < typeInfoCount; typeInfoIndex++) {
			typeInfos[typeInfoIndex].writeToStream(dataOutputStream, writerMap);
		}
	}
}

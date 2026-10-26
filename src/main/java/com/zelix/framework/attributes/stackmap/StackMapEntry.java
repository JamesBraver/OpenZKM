package com.zelix.framework.attributes.stackmap;

import com.zelix.framework.StructureNode;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.attributes.base.AbstractStackMapAttribute;
import com.zelix.framework.attributes.code.StackMapAttribute;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StackMapEntry extends AbstractStackMapEntry {
	private VerificationTypeInfo[] localsTypes;
	private VerificationTypeInfo[] stackTypes;

	public StackMapEntry(
		StackMapAttribute stackMapAttribute,
		LabelNode labelNode,
		FrameNode frameNode,
		ConstantPoolManager constantPoolManager,
		Set<ClassInfoWriter> classInfos,
		List<TypeWriter> cpEntries,
		boolean maskUninitializedLocals,
		Map<Integer, VerificationTypeInfo> primitiveTypeCache,
		Map<String, VerificationTypeInfo> objectTypeCache
	) {
		super(stackMapAttribute);
		this.labelNode = labelNode;
		StackFrameType[] localFrameTypes;
		if (maskUninitializedLocals) {
			localFrameTypes = frameNode.getUninitializedLocals();
		} else {
			localFrameTypes = frameNode.getLocalVariables();
		}

		this.localsTypes = VerificationTypeInfo.createFromFrameTypes(stackMapAttribute, localFrameTypes, constantPoolManager, classInfos, cpEntries, true, primitiveTypeCache, objectTypeCache);
		this.stackTypes = VerificationTypeInfo.createFromFrameTypes(stackMapAttribute, frameNode.getOperandStack(), constantPoolManager, classInfos, cpEntries, false, primitiveTypeCache, objectTypeCache);
		this.isValid = true;
	}

	public StackMapEntry(
		StructureNode parent, ByteDataBuffer byteDataBuffer, MultiMap<Integer, IGraphElement> elementsByOffset, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences, PrintWriter printWriter
	) throws IOException {
		super(parent);
		HashMap<Integer, VerificationTypeInfo> primitiveTypesByTag = GlobalUtils.newHashMap();
		HashMap<Integer, VerificationTypeInfo> objectTypesByConstantIndex = GlobalUtils.newHashMap();
		IntegerCache integerCache = IntegerCache.getInstance();
		this.labelOffset = byteDataBuffer.readUnsignedShort();
		elementsByOffset.put(integerCache.valueOf(this.labelOffset), this);
		int localCount = byteDataBuffer.readUnsignedShort();
		this.localsTypes = new VerificationTypeInfo[localCount];

		for (int localIndex = 0; localIndex < localCount; localIndex++) {
			this.localsTypes[localIndex] = VerificationTypeInfo.readFromBuffer((AbstractStackMapAttribute)parent, byteDataBuffer, elementsByOffset, classReferences, printWriter, primitiveTypesByTag, objectTypesByConstantIndex, integerCache);
			if (!this.localsTypes[localIndex].isValid()) {
				this.isValid = false;
			}
		}

		int stackCount = byteDataBuffer.readUnsignedShort();
		this.stackTypes = new VerificationTypeInfo[stackCount];

		for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
			this.stackTypes[stackIndex] = VerificationTypeInfo.readFromBuffer((AbstractStackMapAttribute)parent, byteDataBuffer, elementsByOffset, classReferences, printWriter, primitiveTypesByTag, objectTypesByConstantIndex, integerCache);
			if (!this.stackTypes[stackIndex].isValid()) {
				this.isValid = false;
			}
		}
	}

	public void writeToStreamWithMapping(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		DebugUtils.assertTrue(this.isValid, new String[]{"Invalid StackMap Entry in " + this.getBinaryName()});
		if (this.labelNode != null) {
			dataOutputStream.writeShort(this.labelNode.getOffset());
		} else {
			dataOutputStream.writeShort(this.labelOffset);
		}

		int localCount = this.localsTypes.length;
		dataOutputStream.writeShort(localCount);

		for (int localIndex = 0; localIndex < localCount; localIndex++) {
			this.localsTypes[localIndex].writeToStream(dataOutputStream, writerMap);
		}

		int stackCount = this.stackTypes.length;
		dataOutputStream.writeShort(stackCount);

		for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
			this.stackTypes[stackIndex].writeToStream(dataOutputStream, writerMap);
		}
	}

	@Override
	public int getSerializedSize() {
		int serializedSize = 6;

		for (int localIndex = 0; localIndex < this.localsTypes.length; localIndex++) {
			serializedSize += this.localsTypes[localIndex].getSerializedSize();
		}

		for (int stackIndex = 0; stackIndex < this.stackTypes.length; stackIndex++) {
			serializedSize += this.stackTypes[stackIndex].getSerializedSize();
		}

		return serializedSize;
	}

	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		DebugUtils.assertTrue(this.isValid, new String[]{"Invalid StackMap Entry in " + this.getBinaryName()});
		if (this.labelNode != null) {
			dataOutputStream.writeShort(this.labelNode.getOffset());
		} else {
			dataOutputStream.writeShort(this.labelOffset);
		}

		int localCount = this.localsTypes.length;
		dataOutputStream.writeShort(localCount);

		for (int localIndex = 0; localIndex < localCount; localIndex++) {
			this.localsTypes[localIndex].writeToStream(dataOutputStream);
		}

		int stackCount = this.stackTypes.length;
		dataOutputStream.writeShort(stackCount);

		for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
			this.stackTypes[stackIndex].writeToStream(dataOutputStream);
		}
	}

	@Override
	public final void acceptVisitor(ConstantPoolVisitor visitor) {
		int localCount = this.localsTypes.length;

		for (int localIndex = 0; localIndex < localCount; localIndex++) {
			this.localsTypes[localIndex].acceptVisitor(visitor);
		}

		int stackCount = this.stackTypes.length;

		for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
			this.stackTypes[stackIndex].acceptVisitor(visitor);
		}
	}

	@Override
	public String getMnemonics() {
		return "StackMap";
	}
}

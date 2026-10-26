package com.zelix.framework.attributes.stackmap;

import com.zelix.framework.StructureNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.analyzer.UnresolvedNewType;
import com.zelix.framework.attributes.base.AbstractStackMapAttribute;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class VerificationTypeInfo extends StructureNode {
	public boolean isValid = true;
	public final int tag;

	protected VerificationTypeInfo(StructureNode parent, int tag) {
		super(parent);
		this.tag = tag;
	}

	public static VerificationTypeInfo[] createFromFrameTypes(
		AbstractStackMapAttribute stackMapAttribute,
		StackFrameType[] frameTypes,
		ConstantPoolManager constantPoolManager,
		Set<ClassInfoWriter> classInfoWriterPool,
		List<TypeWriter> typeWriters,
		boolean trimTrailingTopTypes,
		Map<Integer, VerificationTypeInfo> primitiveTypes,
		Map<String, VerificationTypeInfo> objectTypesByDescriptor
	) {
		IntegerCache integerCache = IntegerCache.getInstance();
		ArrayList<VerificationTypeInfo> typeInfoList = new ArrayList<>(frameTypes.length);

		for (int i = 0; i < frameTypes.length; i++) {
			StackFrameType stackFrameType = frameTypes[i];
			if (!stackFrameType.isUninitializedMarker()) {
				VerificationTypeInfo typeInfo;
				if (stackFrameType == StackFrameType.UNKNOWN) {
					typeInfo = getOrCreatePrimitiveType(stackMapAttribute, 0, primitiveTypes, integerCache);
				} else if (stackFrameType == StackFrameType.BYTE
					|| stackFrameType == StackFrameType.CHAR
					|| stackFrameType == StackFrameType.SHORT
					|| stackFrameType == StackFrameType.INT) {
					typeInfo = getOrCreatePrimitiveType(stackMapAttribute, 1, primitiveTypes, integerCache);
				} else if (stackFrameType == StackFrameType.FLOAT) {
					typeInfo = getOrCreatePrimitiveType(stackMapAttribute, 2, primitiveTypes, integerCache);
				} else if (stackFrameType == StackFrameType.DOUBLE) {
					typeInfo = getOrCreatePrimitiveType(stackMapAttribute, 3, primitiveTypes, integerCache);
				} else if (stackFrameType == StackFrameType.LONG) {
					typeInfo = getOrCreatePrimitiveType(stackMapAttribute, 4, primitiveTypes, integerCache);
				} else if (stackFrameType == StackFrameType.NULL) {
					typeInfo = getOrCreatePrimitiveType(stackMapAttribute, 5, primitiveTypes, integerCache);
				} else if (stackFrameType.isResolved()) {
					typeInfo = ObjectVariableInfo.getOrCreateObjectType(stackMapAttribute, 7, stackFrameType.getDescriptor(), constantPoolManager, classInfoWriterPool, typeWriters, objectTypesByDescriptor);
				} else if (stackFrameType.isUninitialized()) {
					typeInfo = getOrCreatePrimitiveType(stackMapAttribute, 6, primitiveTypes, integerCache);
				} else {
					typeInfo = new UninitializedVariableInfo(stackMapAttribute, 8, ((UnresolvedNewType)stackFrameType).getNewInstruction());
				}

				typeInfoList.add(typeInfo);
			}
		}

		if (trimTrailingTopTypes) {
			ListIterator<VerificationTypeInfo> typeInfoIterator = typeInfoList.listIterator();

			while (typeInfoIterator.hasNext()) {
				typeInfoIterator.next();
			}

			while (typeInfoIterator.hasPrevious() && typeInfoIterator.previous().isTop()) {
				typeInfoIterator.remove();
			}
		}

		VerificationTypeInfo[] typeInfoArray = new VerificationTypeInfo[typeInfoList.size()];
		typeInfoList.toArray(typeInfoArray);
		return typeInfoArray;
	}

	public static VerificationTypeInfo getOrCreatePrimitiveType(
		AbstractStackMapAttribute stackMapAttribute, int tag, Map<Integer, VerificationTypeInfo> primitiveTypes, IntegerCache integerCache
	) {
		Integer tagKey = integerCache.valueOf(tag);
		VerificationTypeInfo typeInfo;
		if (primitiveTypes.containsKey(tagKey)) {
			typeInfo = primitiveTypes.get(tagKey);
		} else {
			typeInfo = new VerificationTypeInfo(stackMapAttribute, tag);
			primitiveTypes.put(tagKey, typeInfo);
		}

		return typeInfo;
	}

	public static VerificationTypeInfo readFromBuffer(
		AbstractStackMapAttribute stackMapAttribute,
		ByteDataBuffer byteDataBuffer,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter,
		Map<Integer, VerificationTypeInfo> primitiveTypes,
		Map<Integer, VerificationTypeInfo> objectTypes,
		IntegerCache integerCache
	) throws IOException {
		int tagValue = byteDataBuffer.readUnsignedByte();
		Integer tagKey = integerCache.valueOf(tagValue);
		VerificationTypeInfo typeInfo = null;
		switch (tagValue) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
				if (primitiveTypes.containsKey(tagKey)) {
					typeInfo = primitiveTypes.get(tagKey);
				} else {
					typeInfo = new VerificationTypeInfo(stackMapAttribute, tagValue);
					primitiveTypes.put(tagKey, typeInfo);
				}
				break;
			case 7:
				int constantPoolIndex = byteDataBuffer.readUnsignedShort();
				Integer objectTypeKey = integerCache.valueOf(constantPoolIndex);
				if (objectTypes.containsKey(objectTypeKey)) {
					typeInfo = objectTypes.get(objectTypeKey);
				} else {
					typeInfo = new ObjectVariableInfo(stackMapAttribute, tagValue, constantPoolIndex, byteDataBuffer, classReferences, printWriter);
					objectTypes.put(objectTypeKey, typeInfo);
				}
				break;
			case 8:
				typeInfo = new UninitializedVariableInfo(stackMapAttribute, tagValue, byteDataBuffer, elementsByOffset, classReferences, printWriter);
				break;
			default:
				typeInfo = new VerificationTypeInfo(stackMapAttribute, tagValue);
				typeInfo.isValid = false;
				printWriter.println("ERROR: " + typeInfo.getRawSourcePath() + " : Invalid StackMap attribute '" + tagValue + "' (D)");
		}

		return typeInfo;
	}

	protected final void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		this.writeToStream(dataOutputStream, null);
	}

	public boolean isSameType(VerificationTypeInfo other) {
		return this.tag == other.tag;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	public final String getMnemonics() {
		return "StackMap Type";
	}

	protected void writeToStream(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		if (!this.isValid) {
			DebugUtils.assertTrue(false, new String[]{"Invalid StackMap Entry in " + this.getBinaryName()});
		}

		dataOutputStream.writeByte(this.tag);
		switch (this.tag) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
		}
	}

	public final boolean isTop() {
		return this.tag == 0;
	}

	public final int getSerializedSize() {
		switch (this.tag) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
				return 1;
			case 7:
				return 3;
			case 8:
				return 3;
			default:
				return 1;
		}
	}

	public final boolean isValid() {
		return this.isValid;
	}

	public boolean isCategory2() {
		switch (this.tag) {
			case 0:
			case 1:
			case 2:
			case 5:
			case 6:
			case 7:
			case 8:
				return false;
			case 3:
			case 4:
				return true;
			default:
				return false;
		}
	}
}

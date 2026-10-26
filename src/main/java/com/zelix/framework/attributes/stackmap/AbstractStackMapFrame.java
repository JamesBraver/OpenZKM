package com.zelix.framework.attributes.stackmap;

import com.zelix.framework.StructureNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.attributes.code.StackMapTableAttribute;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.references.AtomicReference;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStackMapFrame extends AbstractStackMapEntry {
	public AbstractStackMapFrame(StructureNode parent) {
		super(parent);
	}

	public static AbstractStackMapFrame createFrame(
		StackMapTableAttribute stackMapAttribute,
		LabelNode labelNode,
		StackFrameType[] stackFrameTypes,
		StackFrameType[] localFrameTypes,
		ConstantPoolManager constantPoolManager,
		Set<ClassInfoWriter> classInfoWriters,
		List<TypeWriter> utf8Strings,
		AtomicInt previousOffset,
		AtomicReference<VerificationTypeInfo[]> previousStack,
		AtomicReference<VerificationTypeInfo[]> previousLocals,
		Map<Integer, VerificationTypeInfo> primitiveTypeCache,
		Map<String, VerificationTypeInfo> objectTypeCache
	) {
		int currentOffset = labelNode.getOffset();
		int previousFrameOffset = previousOffset.get();
		int offsetDelta = previousFrameOffset == -1 ? currentOffset : currentOffset - previousFrameOffset - 1;
		previousOffset.set(currentOffset);
		VerificationTypeInfo[] previousLocalTypes = previousLocals.get();
		VerificationTypeInfo[] localTypes = VerificationTypeInfo.createFromFrameTypes(
			stackMapAttribute, localFrameTypes, constantPoolManager, classInfoWriters, utf8Strings, true, primitiveTypeCache, objectTypeCache
		);
		VerificationTypeInfo[] stackTypes = VerificationTypeInfo.createFromFrameTypes(
			stackMapAttribute, stackFrameTypes, constantPoolManager, classInfoWriters, utf8Strings, false, primitiveTypeCache, objectTypeCache
		);
		boolean localsUnchanged = areTypesEqual(previousLocalTypes, localTypes);
		AbstractStackMapFrame frame = null;
		int frameType = -1;
		if (Options.ZKM_DEBUG_FORCE_FULL_STACK_MAP) {
			frameType = 255;
			frame = new FullFrame(stackMapAttribute, offsetDelta, labelNode, stackTypes, localTypes);
		} else if (localsUnchanged && stackTypes.length == 0) {
			if (offsetDelta >= 0 && offsetDelta <= 63) {
				frameType = offsetDelta;
				frame = new SameFrame(stackMapAttribute, offsetDelta, labelNode);
			} else {
				frameType = 251;
				frame = new SameFrameExtended(stackMapAttribute, labelNode, offsetDelta);
			}
		} else if (localsUnchanged && stackTypes.length == 1) {
			if (offsetDelta >= 0 && offsetDelta <= 63) {
				frameType = offsetDelta + 64;
				frame = new SameLocals1StackItemFrame(stackMapAttribute, frameType, labelNode, stackTypes[0]);
			} else {
				frameType = 247;
				frame = new SameLocals1StackItemFrameExtended(stackMapAttribute, offsetDelta, labelNode, stackTypes[0]);
			}
		} else if (stackTypes.length == 0) {
			int choppedLocalsCount = previousLocalTypes.length - localTypes.length;
			if (choppedLocalsCount >= 1 && choppedLocalsCount <= 3) {
				int retainedLocalsMatch = 1;

				for (int chopLocalIndex = 0; chopLocalIndex < localTypes.length; chopLocalIndex++) {
					if (!previousLocalTypes[chopLocalIndex].isSameType(localTypes[chopLocalIndex])) {
						retainedLocalsMatch = 0;
						break;
					}
				}

				if (retainedLocalsMatch != 0) {
					frameType = 251 - choppedLocalsCount;
					frame = new ChopFrame(stackMapAttribute, frameType, offsetDelta, labelNode);
				}
			}

			int appendedLocalsCount;
			if (frameType == -1 && (appendedLocalsCount = localTypes.length - previousLocalTypes.length) >= 1 && appendedLocalsCount <= 3) {
				int appendedLocalsMatch = 1;

				for (int appendLocalIndex = 0; appendLocalIndex < previousLocalTypes.length; appendLocalIndex++) {
					if (!previousLocalTypes[appendLocalIndex].isSameType(localTypes[appendLocalIndex])) {
						appendedLocalsMatch = 0;
						break;
					}
				}

				if (appendedLocalsMatch != 0) {
					frameType = 251 + appendedLocalsCount;
					VerificationTypeInfo[] appendedLocalTypes = new VerificationTypeInfo[appendedLocalsCount];
					System.arraycopy(localTypes, localTypes.length - appendedLocalsCount, appendedLocalTypes, 0, appendedLocalsCount);
					frame = new AppendFrame(stackMapAttribute, frameType, offsetDelta, labelNode, appendedLocalTypes);
				}
			}
		}

		if (frameType == -1) {
			frame = new FullFrame(stackMapAttribute, offsetDelta, labelNode, stackTypes, localTypes);
		}

		previousLocals.set(localTypes);
		previousStack.set(stackTypes);
		return frame;
	}

	public static boolean areTypesEqual(VerificationTypeInfo[] previousLocals, VerificationTypeInfo[] currentLocals) {
		if (previousLocals.length == currentLocals.length) {
			for (int i = 0; i < previousLocals.length; i++) {
				if (!previousLocals[i].isSameType(currentLocals[i])) {
					return false;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	public static AbstractStackMapFrame parseFrame(
		StackMapTableAttribute stackMapAttribute,
		ByteDataBuffer buf,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter,
		AtomicInt previousOffset,
		Map<Integer, VerificationTypeInfo> primitiveTypeCache,
		Map<Integer, VerificationTypeInfo> objectTypeCache
	) throws IOException {
		AbstractStackMapFrame frame = null;
		int frameType = buf.readUnsignedByte();
		if (frameType == 255) {
			frame = new FullFrame(frameType, stackMapAttribute, buf, elementsByOffset, classReferences, printWriter, previousOffset, primitiveTypeCache, objectTypeCache);
		} else if (frameType == 251) {
			frame = new SameFrameExtended(frameType, stackMapAttribute, buf, elementsByOffset, classReferences, printWriter, previousOffset, primitiveTypeCache, objectTypeCache);
		} else if (frameType >= 0 && frameType <= 63) {
			frame = new SameFrame(frameType, stackMapAttribute, buf, elementsByOffset, classReferences, printWriter, previousOffset, primitiveTypeCache, objectTypeCache);
		} else if (frameType >= 64 && frameType <= 127) {
			frame = new SameLocals1StackItemFrame(frameType, stackMapAttribute, buf, elementsByOffset, classReferences, printWriter, previousOffset, primitiveTypeCache, objectTypeCache);
		} else if (frameType == 247) {
			frame = new SameLocals1StackItemFrameExtended(frameType, stackMapAttribute, buf, elementsByOffset, classReferences, printWriter, previousOffset, primitiveTypeCache, objectTypeCache);
		} else if (frameType >= 248 && frameType <= 250) {
			frame = new ChopFrame(frameType, stackMapAttribute, buf, elementsByOffset, classReferences, printWriter, previousOffset, primitiveTypeCache, objectTypeCache);
		} else if (frameType >= 252 && frameType <= 254) {
			frame = new AppendFrame(frameType, stackMapAttribute, buf, elementsByOffset, classReferences, printWriter, previousOffset, primitiveTypeCache, objectTypeCache);
		} else {
			DebugUtils.assertTrue(false, new String[]{"Invalid StackMapTable frame type '" + frameType + "' in '" + stackMapAttribute.getParentName() + "'"});
		}

		return frame;
	}

	public abstract int getFrameType();

	@Override
	public final String getMnemonics() {
		return "StackMapTable";
	}

	public final FrameTypes getFrameCategory() {
		int frameType = this.getFrameType();
		if (frameType >= 0 && frameType <= 63) {
			return FrameTypes.SAME;
		} else if (frameType >= 64 && frameType <= 127) {
			return FrameTypes.SAME_LOCALS_1_STACK_ITEM;
		} else if (frameType == 247) {
			return FrameTypes.SAME_LOCALS_1_STACK_ITEM_EXTENDED;
		} else if (frameType >= 248 && frameType <= 250) {
			return FrameTypes.CHOP;
		} else if (frameType == 251) {
			return FrameTypes.SAME_EXTENDED;
		} else if (frameType >= 252 && frameType <= 254) {
			return FrameTypes.APPEND;
		} else {
			return frameType == 255 ? FrameTypes.FULL : null;
		}
	}

	public final void write(DataOutputStream dataOutputStream, AtomicInt previousOffset) throws IOException {
		this.writeMapped(dataOutputStream, previousOffset, null);
	}

	public abstract void writeMapped(DataOutputStream dataOutputStream, AtomicInt lastOffset, Map<TypeWriter, TypeWriter> writerMap) throws IOException;
}

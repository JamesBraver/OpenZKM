package com.zelix.framework.analyzer;

import com.zelix.framework.attributes.code.StackMapTableAttribute;
import com.zelix.framework.attributes.stackmap.AbstractStackMapFrame;
import com.zelix.framework.attributes.stackmap.AppendFrame;
import com.zelix.framework.attributes.stackmap.FrameTypes;
import com.zelix.framework.attributes.stackmap.VerificationTypeInfo;
import com.zelix.framework.code.InsnList;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StackMapTableAnalyzer {
	private static boolean[][][] LOCAL_VARIANT_CACHE = new boolean[21][][];
	private final InsnList<AbstractInsnNode> insnList;
	private final InstructionListNode instructionListNode;
	private final MethodAnalyzer methodAnalyzer;
	private final FrameNode[] frames;
	private final String methodAnalyzing;

	public StackMapTableAnalyzer(InstructionListNode instructionListNode, MethodAnalyzer methodAnalyzer) {
		this.insnList = instructionListNode.getInstructionListCopy();
		this.instructionListNode = instructionListNode;
		this.methodAnalyzer = methodAnalyzer;
		if (Options.useSpecialStackMapFrames) {
			this.frames = methodAnalyzer.buildSpecialStackMapFrames();
		} else {
			this.frames = methodAnalyzer.getFrames();
		}

		this.methodAnalyzing = instructionListNode.getParentString();
	}

	private static boolean[][] filterLocalVariants(int localCount, boolean[] varyingLocals, boolean[][] variants) {
		boolean[][] filteredVariants = variants;

		for (int localIndex = 0; localIndex < varyingLocals.length; localIndex++) {
			if (varyingLocals[localIndex]) {
				boolean[][] reducedVariants = new boolean[filteredVariants.length / 2][localCount];
				int togglePeriod = (int)Math.pow(2.0, localIndex + 1);
				int runLength = variants.length / togglePeriod;
				boolean rowIncluded = true;
				int writeIndex = 0;
				int runPosition = 1;

				for (int rowIndex = 0; rowIndex < filteredVariants.length; rowIndex++) {
					if (rowIncluded) {
						System.arraycopy(filteredVariants[rowIndex], 0, reducedVariants[writeIndex], 0, localCount);
						writeIndex++;
					}

					if (++runPosition > runLength) {
						rowIncluded = !rowIncluded;
						runPosition = 1;
					}
				}

				filteredVariants = reducedVariants;
			}
		}

		return filteredVariants;
	}

	public static boolean[][] getLocalVariantMatrix(int localCount) {
		if (LOCAL_VARIANT_CACHE[localCount] == null) {
			int variantCount = (int)Math.pow(2.0, localCount);
			boolean[][] variants = new boolean[variantCount][localCount];

			for (int localIndex = 0; localIndex < localCount; localIndex++) {
				int togglePeriod = (int)Math.pow(2.0, localIndex + 1);
				int runLength = variantCount / togglePeriod;
				boolean localValue = true;
				int runPosition = 0;

				for (int variantIndex = 0; variantIndex < variantCount; variantIndex++) {
					if (++runPosition > runLength) {
						runPosition = 1;
						localValue = !localValue;
					}

					variants[variantIndex][localIndex] = localValue;
				}
			}

			LOCAL_VARIANT_CACHE[localCount] = variants;
		}

		return LOCAL_VARIANT_CACHE[localCount];
	}

	public AbstractStackMapFrame[] buildMinimalStackMapTables(int maxLocals, StackMapTableAttribute stackMapAttribute, ConstantPoolManager constantPoolManager, Set<ClassInfoWriter> classInfoWriters, List<TypeWriter> utf8Strings) {
		DebugUtils.assertTrue(maxLocals <= 20, new String[]{"Variable count : " + maxLocals + ">" + 20, this.instructionListNode.getSourcePath()});
		Integer[] labelIndexes = this.methodAnalyzer.getStackMapLabelIndexes();
		AbstractStackMapFrame[] frameTables = new AbstractStackMapFrame[labelIndexes.length];
		if (labelIndexes.length > 0 && this.frames.length > 0) {
			boolean[][] variants = getLocalVariantMatrix(maxLocals);
			variants = this.filterVariantsForLabels(maxLocals, labelIndexes, variants);
			if (variants.length > 128) {
				boolean[][] reducedVariants = new boolean[2][variants[0].length];
				System.arraycopy(variants[0], 0, reducedVariants[0], 0, variants[0].length);
				System.arraycopy(variants[variants.length - 1], 0, reducedVariants[1], 0, variants[1].length);
				variants = reducedVariants;
			}

			int minSerializedSize = Integer.MAX_VALUE;
			int bestVariantIndex = -1;
			ArrayList<TypeWriter> variantUtf8Strings = new ArrayList<>(utf8Strings);
			HashSet<ClassInfoWriter> variantClassInfoWriters = GlobalUtils.newHashSet(classInfoWriters);

			for (int variantIndex = 0; variantIndex < variants.length; variantIndex++) {
				HashMap<Integer, VerificationTypeInfo> primitiveTypeCache = GlobalUtils.newHashMap();
				HashMap<String, VerificationTypeInfo> objectTypeCache = GlobalUtils.newHashMap();
				AbstractStackMapFrame[] unusedFrameTables = new AbstractStackMapFrame[labelIndexes.length];
				AbstractStackMapFrame[] variantFrameTables = this.buildVariantStackMapTables(maxLocals, labelIndexes, variants[variantIndex], stackMapAttribute, constantPoolManager, variantClassInfoWriters, variantUtf8Strings, primitiveTypeCache, objectTypeCache);
				int serializedSize = 0;

				for (AbstractStackMapFrame frameTable : variantFrameTables) {
					serializedSize += frameTable.getSerializedSize();
				}

				if (serializedSize < minSerializedSize) {
					minSerializedSize = serializedSize;
					bestVariantIndex = variantIndex;
				}
			}

			HashMap<Integer, VerificationTypeInfo> finalPrimitiveTypeCache = GlobalUtils.newHashMap();
			HashMap<String, VerificationTypeInfo> finalObjectTypeCache = GlobalUtils.newHashMap();
			frameTables = this.buildVariantStackMapTables(maxLocals, labelIndexes, variants[bestVariantIndex], stackMapAttribute, constantPoolManager, classInfoWriters, utf8Strings, finalPrimitiveTypeCache, finalObjectTypeCache);
		}

		return frameTables;
	}

	private AbstractStackMapFrame[] buildVariantStackMapTables(
		int maxLocals,
		Integer[] labelIndexes,
		boolean[] useUninitializedLocals,
		StackMapTableAttribute stackMapAttribute,
		ConstantPoolManager constantPoolManager,
		Set<ClassInfoWriter> classInfoWriters,
		List<TypeWriter> utf8Strings,
		Map<Integer, VerificationTypeInfo> primitiveTypeCache,
		Map<String, VerificationTypeInfo> objectTypeCache
	) {
		AtomicInt previousOffset = new AtomicInt(-1);
		AtomicReference<VerificationTypeInfo[]> previousLocals = new AtomicReference<>();
		AtomicReference<VerificationTypeInfo[]> previousStack = new AtomicReference<>();
		previousLocals.set(
			VerificationTypeInfo.createFromFrameTypes(stackMapAttribute, this.frames[0].getLocalVariables(), constantPoolManager, classInfoWriters, utf8Strings, true, primitiveTypeCache, objectTypeCache)
		);
		previousStack.set(new VerificationTypeInfo[0]);
		AbstractStackMapFrame[] frameTables = new AbstractStackMapFrame[labelIndexes.length];

		for (int labelIndex = 0; labelIndex < labelIndexes.length; labelIndex++) {
			int labelInsnIndex = labelIndexes[labelIndex];
			LabelNode labelNode = (LabelNode)this.insnList.get(labelInsnIndex);
			FrameNode frame = this.frames[labelInsnIndex];
			StackFrameType[] localVariables = frame.getLocalVariables();
			StackFrameType[] uninitializedLocals = frame.getUninitializedLocals();
			StackFrameType[] frameLocalTypes = new StackFrameType[maxLocals];

			for (int localIndex = 0; localIndex < maxLocals; localIndex++) {
				if (useUninitializedLocals[localIndex]) {
					frameLocalTypes[localIndex] = localVariables[localIndex];
				} else {
					frameLocalTypes[localIndex] = uninitializedLocals[localIndex];
				}

				if (localIndex > 0) {
					if (frameLocalTypes[localIndex].isUninitializedMarker() && !frameLocalTypes[localIndex - 1].isCategory2()) {
						frameLocalTypes[localIndex] = StackFrameType.UNKNOWN;
					} else if (frameLocalTypes[localIndex - 1].isCategory2() && !frameLocalTypes[localIndex].isUninitializedMarker()) {
						frameLocalTypes[localIndex] = StackFrameType.UNINITIALIZED;
					}
				}
			}

			frameTables[labelIndex] = AbstractStackMapFrame.createFrame(
				stackMapAttribute, labelNode, frame.getOperandStack(), frameLocalTypes, constantPoolManager, classInfoWriters, utf8Strings, previousOffset, previousStack, previousLocals, primitiveTypeCache, objectTypeCache
			);
		}

		return frameTables;
	}

	public AbstractStackMapFrame[] buildFrameStackMapTables(
		StackMapTableAttribute stackMapAttribute,
		ConstantPoolManager constantPoolManager,
		Set<ClassInfoWriter> classInfoWriters,
		List<TypeWriter> utf8Strings,
		boolean useUninitializedLocals,
		boolean fixLastFrame
	) {
		Integer[] labelIndexes = this.methodAnalyzer.getStackMapLabelIndexes();
		int labelCount = labelIndexes.length;
		HashMap<Integer, VerificationTypeInfo> primitiveTypeCache = GlobalUtils.newHashMap();
		HashMap<String, VerificationTypeInfo> objectTypeCache = GlobalUtils.newHashMap();
		AbstractStackMapFrame[] frameTables = new AbstractStackMapFrame[labelCount];
		AtomicInt previousOffset = new AtomicInt(-1);
		AtomicReference<VerificationTypeInfo[]> previousLocals = new AtomicReference<>();
		AtomicReference<VerificationTypeInfo[]> previousStack = new AtomicReference<>();
		if (this.frames.length > 0) {
			previousLocals.set(
				VerificationTypeInfo.createFromFrameTypes(
					stackMapAttribute, this.frames[0].getLocalVariables(), constantPoolManager, classInfoWriters, utf8Strings, true, primitiveTypeCache, objectTypeCache
				)
			);
			previousStack.set(new VerificationTypeInfo[0]);
		}

		if (useUninitializedLocals || fixLastFrame) {
			for (int scanLabelIndex = 0; scanLabelIndex < labelCount; scanLabelIndex++) {
				if (this.frames[labelIndexes[scanLabelIndex]].hasCategory2Locals()) {
					useUninitializedLocals = false;
					fixLastFrame = false;
					break;
				}
			}
		}

		FrameNode previousFrame = this.frames[0];

		for (int labelIndex = 0; labelIndex < labelCount; labelIndex++) {
			int labelInsnIndex = labelIndexes[labelIndex];
			LabelNode labelNode = (LabelNode)this.insnList.get(labelInsnIndex);
			FrameNode frame = this.frames[labelInsnIndex];
			if (frame == null) {
				DebugUtils.assertNotNullString(
					frame,
					new String[]{
						"Dead instruction at ",
						String.valueOf(labelInsnIndex),
						" : '",
						this.methodAnalyzing,
						"' : ",
						Integer.toHexString(labelNode.getFlags()),
						" : ",
						String.valueOf(labelNode.getOffset())
					}
				);
			}

			if (fixLastFrame && labelIndex >= labelCount - 1) {
				frameTables[labelIndex] = this.createLastFrame(
					stackMapAttribute, labelNode, previousFrame, frame, constantPoolManager, classInfoWriters, utf8Strings, previousOffset, previousStack, previousLocals, primitiveTypeCache, objectTypeCache
				);
			} else {
				StackFrameType[] frameLocalTypes;
				if (useUninitializedLocals) {
					frameLocalTypes = frame.getUninitializedLocals();
				} else {
					frameLocalTypes = frame.getLocalVariables();
				}

				frameTables[labelIndex] = AbstractStackMapFrame.createFrame(
					stackMapAttribute,
					labelNode,
					frame.getOperandStack(),
					frameLocalTypes,
					constantPoolManager,
					classInfoWriters,
					utf8Strings,
					previousOffset,
					previousStack,
					previousLocals,
					primitiveTypeCache,
					objectTypeCache
				);
			}

			previousFrame = frame;
		}

		return frameTables;
	}

	private boolean[][] filterVariantsForLabels(int localCount, Integer[] labelIndexes, boolean[][] variants) {
		boolean[] identicalLocals = this.findVaryingLocals(localCount, labelIndexes);
		return filterLocalVariants(localCount, identicalLocals, variants);
	}

	private boolean[] findVaryingLocals(int localCount, Integer[] labelIndexes) {
		boolean[] identicalLocals = new boolean[localCount];

		for (int i = 0; i < identicalLocals.length; i++) {
			identicalLocals[i] = true;
		}

		StringBuilder[] localTypeBuffers = new StringBuilder[localCount];
		StringBuilder[] uninitializedTypeBuffers = new StringBuilder[localCount];

		for (int labelIndex = 0; labelIndex < labelIndexes.length; labelIndex++) {
			if (labelIndex == 0) {
				for (int localIndex = 0; localIndex < localCount; localIndex++) {
					localTypeBuffers[localIndex] = new StringBuilder();
					uninitializedTypeBuffers[localIndex] = new StringBuilder();
				}
			}

			FrameNode frame = this.frames[labelIndexes[labelIndex]];
			StackFrameType[] localVariables = frame.getLocalVariables();
			StackFrameType[] uninitializedLocals = frame.getUninitializedLocals();

			for (int slotIndex = 0; slotIndex < localCount; slotIndex++) {
				localTypeBuffers[slotIndex].append(localVariables[slotIndex] + ";");
				uninitializedTypeBuffers[slotIndex].append(uninitializedLocals[slotIndex] + ";");
				if (identicalLocals[slotIndex] && !localVariables[slotIndex].equals(uninitializedLocals[slotIndex])) {
					identicalLocals[slotIndex] = false;
				}
			}
		}

		return identicalLocals;
	}

	private AbstractStackMapFrame createLastFrame(
		StackMapTableAttribute stackMapAttribute,
		LabelNode labelNode,
		FrameNode previousFrame,
		FrameNode frame,
		ConstantPoolManager constantPoolManager,
		Set<ClassInfoWriter> classInfoWriters,
		List<TypeWriter> utf8Strings,
		AtomicInt previousOffset,
		AtomicReference<VerificationTypeInfo[]> previousStack,
		AtomicReference<VerificationTypeInfo[]> previousLocals,
		Map<Integer, VerificationTypeInfo> primitiveTypeCache,
		Map<String, VerificationTypeInfo> objectTypeCache
	) {
		StackFrameType[] localVariables = frame.getLocalVariables();
		StackFrameType[] uninitializedLocals = frame.getUninitializedLocals();
		StackFrameType[] operandStack = frame.getOperandStack();
		ArrayList<TypeWriter> localsUtf8Strings = new ArrayList<>(utf8Strings);
		ArrayList<TypeWriter> uninitializedUtf8Strings = new ArrayList<>(utf8Strings);
		AbstractStackMapFrame localsFrame = AbstractStackMapFrame.createFrame(
			stackMapAttribute,
			labelNode,
			operandStack,
			localVariables,
			constantPoolManager,
			GlobalUtils.newHashSet(classInfoWriters),
			localsUtf8Strings,
			new AtomicInt(previousOffset.get()),
			new AtomicReference<>(previousStack.get()),
			new AtomicReference<>(previousLocals.get()),
			GlobalUtils.newHashMap(primitiveTypeCache),
			GlobalUtils.newHashMap(objectTypeCache)
		);
		AbstractStackMapFrame.createFrame(
			stackMapAttribute,
			labelNode,
			operandStack,
			uninitializedLocals,
			constantPoolManager,
			GlobalUtils.newHashSet(classInfoWriters),
			uninitializedUtf8Strings,
			new AtomicInt(previousOffset.get()),
			new AtomicReference<>(previousStack.get()),
			new AtomicReference<>(previousLocals.get()),
			GlobalUtils.newHashMap(primitiveTypeCache),
			GlobalUtils.newHashMap(objectTypeCache)
		);
		int localsPatched = 0;
		StackFrameType[] patchedLocals = new StackFrameType[localVariables.length];
		System.arraycopy(localVariables, 0, patchedLocals, 0, localVariables.length);
		FrameTypes frameCategory = localsFrame.getFrameCategory();
		if (frameCategory == FrameTypes.APPEND) {
			int previousSlotCount = 0;
			VerificationTypeInfo[] previousLocalTypes = previousLocals.get();
			int localSlotCount = previousLocalTypes.length;

			for (int previousLocalIndex = 0; previousLocalIndex < localSlotCount; previousLocalIndex++) {
				previousSlotCount += previousLocalTypes[previousLocalIndex].isCategory2() ? 2 : 1;
			}

			localSlotCount = 0;
			VerificationTypeInfo[] appendedLocals = ((AppendFrame)localsFrame).getLocals();
			int appendedLocalsCount = appendedLocals.length;

			for (int appendedLocalIndex = 0; appendedLocalIndex < appendedLocalsCount; appendedLocalIndex++) {
				localSlotCount += appendedLocals[appendedLocalIndex].isCategory2() ? 2 : 1;
			}

			for (int slotIndex = previousSlotCount + localSlotCount - 1; slotIndex >= previousSlotCount && !frame.isLocalUsedUninitialized(slotIndex); slotIndex--) {
				patchedLocals[slotIndex] = uninitializedLocals[slotIndex];
				localsPatched = 1;
			}
		} else if (frameCategory == FrameTypes.FULL) {
			if (operandStack.length < 2) {
				VerificationTypeInfo[] unusedPreviousLocalTypes = previousLocals.get();
				StackFrameType[] previousLocalVariables = previousFrame.getLocalVariables();
				int previousLastLocalIndex = this.getLastKnownLocalIndex(previousLocalVariables);
				int lastLocalIndex = this.getLastKnownLocalIndex(localVariables);
				int lastCommonLocalIndex = -1;
				int scanIndex = 0;

				while (scanIndex <= previousLastLocalIndex && previousLocalVariables[scanIndex] == localVariables[scanIndex]) {
					lastCommonLocalIndex = scanIndex++;
				}

				if (previousLastLocalIndex < lastLocalIndex && lastCommonLocalIndex == previousLastLocalIndex) {
					scanIndex = lastLocalIndex;

					for (int uninitializedSlotIndex = lastLocalIndex; uninitializedSlotIndex >= lastCommonLocalIndex + 1 && uninitializedLocals[uninitializedSlotIndex].isUnknown(); uninitializedSlotIndex--) {
						patchedLocals[uninitializedSlotIndex] = uninitializedLocals[uninitializedSlotIndex];
						scanIndex--;
					}

					if (scanIndex != previousLastLocalIndex && (operandStack.length != 0 || scanIndex - previousLastLocalIndex > 3)) {
						System.arraycopy(localVariables, 0, patchedLocals, 0, localVariables.length);
					} else {
						localsPatched = 1;
					}
				}
			}

			if (localsPatched == 0) {
				for (int localIndex = 0; localIndex < localVariables.length; localIndex++) {
					StackFrameType localType = localVariables[localIndex];
					StackFrameType uninitializedLocalType = uninitializedLocals[localIndex];
					if (!localType.isUninitialized() && !localType.isUnknown() && uninitializedLocalType.isUnknown()) {
						patchedLocals[localIndex] = uninitializedLocalType;
						localsPatched = 1;
					}
				}
			}
		}

		return localsPatched != 0
			? AbstractStackMapFrame.createFrame(
				stackMapAttribute, labelNode, operandStack, patchedLocals, constantPoolManager, classInfoWriters, utf8Strings, previousOffset, previousStack, previousLocals, primitiveTypeCache, objectTypeCache
			)
			: AbstractStackMapFrame.createFrame(
				stackMapAttribute, labelNode, operandStack, localVariables, constantPoolManager, classInfoWriters, utf8Strings, previousOffset, previousStack, previousLocals, primitiveTypeCache, objectTypeCache
			);
	}

	private int getLastKnownLocalIndex(StackFrameType[] localTypes) {
		int lastKnownIndex = -1;

		for (int i = localTypes.length - 1; i >= 0; i--) {
			if (!localTypes[i].isUnknown()) {
				lastKnownIndex = i;
				break;
			}
		}

		return lastKnownIndex;
	}
}

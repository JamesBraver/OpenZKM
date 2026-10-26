package com.zelix.framework.variable;

import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.signature.MethodSignature;

import java.util.*;

public class LocalVariableTable implements LocalVariablesManager {
	private InstructionListNode ownerMethodBody;
	private List<LocalVariableSlot> slots;
	private int[] shufflePermutationMap;

	public LocalVariableTable(boolean isStaticMethod, String methodSignature, int initialCapacity) {
		this.slots = new ArrayList<>(initialCapacity);
		this.initializeFromSignature(isStaticMethod, methodSignature);
	}

	public LocalVariableTable(InstructionListNode ownerMethodBody, int initialCapacity) {
		this.ownerMethodBody = ownerMethodBody;
		this.slots = new ArrayList<>(initialCapacity);
		String methodDescriptor = this.ownerMethodBody.getMethodNode().getDescriptor();
		this.initializeFromSignature(this.ownerMethodBody.isStaticMethod(), methodDescriptor);
	}

	public LocalVariableSlot removeVariable(int index) {
		LocalVariableSlot removedSlot = this.slots.remove(index);
		if (removedSlot.isWideSlotStart()) {
			this.slots.remove(index);
		}

		return removedSlot;
	}

	public static boolean isWideType(String descriptor) {
		return descriptor.equals("J") || descriptor.equals("D");
	}

	public LocalVariableSlot getVariable(int index) {
		return !this.slots.isEmpty() && index <= this.slots.size() - 1 ? this.slots.get(index) : null;
	}

	public void resetWideFlagsFrom(int fromIndex) {
		for (int i = fromIndex; i < this.slots.size(); i++) {
			LocalVariableSlot slot = this.slots.get(i);
			slot.clearWideSlotStartFlag();
			slot.clearWideSlotContinuation();
		}
	}

	@Override
	public LocalVariableSlot getOrCreateVariable(int index, LocalVarOperationType opType, int flags) {
		LocalVariableSlot targetSlot;
		if (this.slots.size() > index) {
			targetSlot = this.slots.get(index);
			if (opType.isWide()) {
				targetSlot.clearWideSlotContinuation();
				targetSlot.setAsWideSlotStart();
				if (this.slots.size() > index + 1) {
					LocalVariableSlot continuationSlot = this.slots.get(index + 1);
					continuationSlot.clearWideSlotStartFlag();
					continuationSlot.setAsWideSlotContinuation();
				} else {
					LocalVariableSlot appendedContinuationSlot = new LocalVariableSlot(index + 1, false, true, flags);
					this.slots.add(appendedContinuationSlot);
				}
			} else {
				if (targetSlot.isWideSlotStart()) {
					LocalVariableSlot staleContinuationSlot = this.slots.get(index + 1);
					staleContinuationSlot.clearWideSlotContinuation();
					staleContinuationSlot.clearWideSlotStartFlag();
				}

				targetSlot.clearWideSlotStartFlag();
				targetSlot.clearWideSlotContinuation();
			}
		} else {
			for (int i = this.slots.size(); i < index; i++) {
				LocalVariableSlot fillerSlot = new LocalVariableSlot(i, flags);
				this.slots.add(fillerSlot);
			}

			if (opType.isWide()) {
				targetSlot = new LocalVariableSlot(index, true, false, flags);
				this.slots.add(targetSlot);
				LocalVariableSlot newContinuationSlot = new LocalVariableSlot(index + 1, false, true, flags);
				this.slots.add(newContinuationSlot);
			} else {
				targetSlot = new LocalVariableSlot(index, flags);
				this.slots.add(targetSlot);
			}
		}

		return targetSlot;
	}

	public int[] getShufflePermutationMap() {
		return this.shufflePermutationMap;
	}

	public void setOwnerMethodBody(InstructionListNode ownerMethodBody) {
		this.ownerMethodBody = ownerMethodBody;
	}

	private void initializeFromSignature(boolean isStatic, String methodDescriptor) {
		List<String> paramTypes = MethodSignature.parseParameterDescriptors(methodDescriptor);
		int slotIndex = 0;
		if (!isStatic) {
			slotIndex++;
			this.slots.add(new LocalVariableSlot(0));
		}

		for (int i = 0; i < paramTypes.size(); i++) {
			if (isWideType(paramTypes.get(i))) {
				this.slots.add(new LocalVariableSlot(slotIndex++, true, false));
				this.slots.add(new LocalVariableSlot(slotIndex++, false, true));
			} else {
				this.slots.add(new LocalVariableSlot(slotIndex++));
			}
		}
	}

	public LocalVariableSlot insertVariableAt(int index, boolean isWide, int flags) {
		LocalVariableSlot insertedSlot;
		if (isWide) {
			LocalVariableSlot continuationSlot = new LocalVariableSlot(index + 1, false, true, flags);
			this.slots.add(index, continuationSlot);
			insertedSlot = new LocalVariableSlot(index, true, false, flags);
			this.slots.add(index, insertedSlot);
		} else {
			insertedSlot = new LocalVariableSlot(index, flags);
			this.slots.add(index, insertedSlot);
		}

		return insertedSlot;
	}

	public void moveTailVariablesTo(int tailStartIndex, int targetIndex) {
		int slotCount = this.slots.size();
		int tailCount = slotCount - tailStartIndex;
		if (tailCount != 0) {
			ArrayList<LocalVariableSlot> tailSlots = new ArrayList<>(tailStartIndex);

			for (int i = tailCount; i < slotCount; i++) {
				LocalVariableSlot removedSlot = this.slots.remove(tailCount);
				tailSlots.add(removedSlot);
			}

			this.slots.addAll(targetIndex, tailSlots);
			this.recalculateSlotIndices();
		}
	}

	public int recalculateSlotIndices() {
		int currentIndex = 0;

		for (int i = 0; i < this.slots.size(); i++) {
			this.slots.get(i).setIndex(currentIndex);
			currentIndex++;
		}

		return this.slots.get(this.slots.size() - 1).getIndex();
	}

	public int getSlotCount() {
		return this.slots.size();
	}

	public boolean shuffleLocalVariables(int fromIndex, int toIndex, Random random) {
		ArrayList<List<LocalVariableSlot>> variableGroups = new ArrayList<>(this.slots.size());

		for (int slotIndex = fromIndex; slotIndex < toIndex; slotIndex++) {
			ArrayList<LocalVariableSlot> slotGroup = new ArrayList<>();
			variableGroups.add(slotGroup);
			LocalVariableSlot slot = this.slots.get(slotIndex);
			slotGroup.add(slot);
			int groupSlotIndex = slotIndex;

			while (slot.isWideSlotStart() || slot.isWideSlotContinuation() && groupSlotIndex < toIndex - 1) {
				slot = this.slots.get(++groupSlotIndex);
				if (!slot.isWideSlotStart() && !slot.isWideSlotContinuation()) {
					break;
				}

				slotGroup.add(slot);
			}

			slotIndex += slotGroup.size() - 1;
		}

		if (variableGroups.size() <= 1) {
			return false;
		}

		Collections.shuffle(variableGroups, random);
		this.shufflePermutationMap = new int[toIndex - fromIndex];
		int writeIndex = fromIndex;
		Iterator<List<LocalVariableSlot>> groupIterator = variableGroups.iterator();

		while (groupIterator.hasNext()) {
			for (LocalVariableSlot groupSlot : groupIterator.next()) {
				this.shufflePermutationMap[writeIndex - fromIndex] = groupSlot.getIndex();
				this.slots.set(writeIndex++, groupSlot);
			}
		}

		this.recalculateSlotIndices();
		return true;
	}

	public int getMaxSlotIndex() {
		return this.slots.isEmpty() ? -1 : this.slots.get(this.slots.size() - 1).getIndex();
	}

	public LocalVariableSlot getPenultimateVariable() {
		return this.slots.isEmpty() ? null : this.slots.get(this.slots.size() - 2);
	}
}

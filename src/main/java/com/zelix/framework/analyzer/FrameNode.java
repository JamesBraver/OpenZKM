package com.zelix.framework.analyzer;

import com.zelix.exceptions.InvalidBytecodeException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.instructions.MonitorEnterNode;
import com.zelix.framework.instructions.MonitorExitNode;
import com.zelix.utils.dataset.AtomicBool;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class FrameNode {
	private StackFrameType[] localVariables;
	private StackFrameType[] operandStack;
	private final LocalsInitializationState localsInitState;
	private Set<MonitorEnterNode> activeMonitors;
	private BitSet uninitializedUsageMask;

	public FrameNode(StackFrameType[] localVariables) {
		this(localVariables, (LocalsInitializationState)null, null);
	}

	public FrameNode(StackFrameType[] operandStack, StackFrameType[] localVariables, Set<MonitorEnterNode> activeMonitors) {
		this(operandStack, localVariables, null, activeMonitors);
	}

	public FrameNode(StackFrameType[] operandStack, StackFrameType[] localVariables, LocalsInitializationState localsInitState, Set<MonitorEnterNode> activeMonitors) {
		this.localVariables = localVariables;
		this.operandStack = operandStack;
		this.localsInitState = localsInitState;
		this.activeMonitors = activeMonitors;

		for (int i = 0; i < this.localVariables.length; i++) {
			if (this.localVariables[i] == null) {
				this.localVariables[i] = StackFrameType.UNKNOWN;
			}
		}
	}

	public FrameNode(StackFrameType[] localVariables, LocalsInitializationState localsInitState, Set<MonitorEnterNode> activeMonitors) {
		this(StackFrameType.createEmptyFrame(0), localVariables, localsInitState, activeMonitors);
	}

	public FrameNode(String typeDescriptor, StackFrameType[] localVariables, LocalsInitializationState localsInitState, Set<MonitorEnterNode> activeMonitors) {
		this.localVariables = localVariables;
		this.operandStack = StackFrameType.createEmptyFrame(1);
		this.localsInitState = localsInitState;
		this.activeMonitors = activeMonitors;
		this.operandStack[0] = StackFrameType.getResolved(typeDescriptor);

		for (int i = 0; i < this.localVariables.length; i++) {
			if (this.localVariables[i] == null) {
				this.localVariables[i] = StackFrameType.UNKNOWN;
			}
		}
	}

	public static boolean isUnknownType(StackFrameType type) {
		return type == null || type.isThatType("?");
	}

	public static boolean isCategory2Type(StackFrameType type) {
		return type.isThatType("J") || type.isThatType("D");
	}

	public static FrameNode mergeForExceptionHandler(
		TypeResolver typeResolver, FrameNode targetFrame, StackFrameType[] handlerLocals, LocalsInitializationState handlerLocalsInitState, Set<MonitorEnterNode> handlerActiveMonitors
	) {
		AtomicBool localsChangedFlag = new AtomicBool();
		StackFrameType[] mergedLocals = mergeLocalsWithMask(typeResolver, handlerLocals, targetFrame.localVariables, targetFrame.localsInitState, localsChangedFlag);
		LinkedHashSet<MonitorEnterNode> mergedMonitors = new LinkedHashSet<>();
		if (handlerActiveMonitors != null) {
			mergedMonitors.addAll(handlerActiveMonitors);
		}

		if (targetFrame.activeMonitors != null) {
			mergedMonitors.addAll(targetFrame.activeMonitors);
		}

		if (mergedMonitors.size() == 0) {
			mergedMonitors = null;
		}

		FrameNode mergedFrame;
		if (handlerLocalsInitState != null) {
			if (targetFrame.localsInitState.equals(handlerLocalsInitState)) {
				mergedFrame = new FrameNode(targetFrame.operandStack, mergedLocals, handlerLocalsInitState, mergedMonitors);
			} else {
				LocalsInitializationState mergedLocalsInitState = (LocalsInitializationState)handlerLocalsInitState.clone();
				mergedLocalsInitState.or(targetFrame.localsInitState);
				mergedFrame = new FrameNode(targetFrame.operandStack, mergedLocals, mergedLocalsInitState, mergedMonitors);
			}
		} else {
			mergedFrame = new FrameNode(targetFrame.operandStack, mergedLocals, mergedMonitors);
		}

		return mergedFrame;
	}

	public static StackFrameType[] mergeLocals(TypeResolver typeResolver, StackFrameType[] locals, StackFrameType[] otherLocals, boolean copy, AtomicBool changedFlag, String debugContext) {
		int localsLength = locals.length;
		StackFrameType[] mergedLocals;
		if (copy) {
			mergedLocals = (StackFrameType[])locals.clone();
		} else {
			mergedLocals = locals;
		}

		changedFlag.set(false);

		for (int localIndex = 0; localIndex < localsLength; localIndex++) {
			if (otherLocals[localIndex].equals(mergedLocals[localIndex])) {
				if (mergedLocals[localIndex].isThatType("~") && !mergedLocals[localIndex - 1].equals(StackFrameType.LONG) && !mergedLocals[localIndex - 1].equals(StackFrameType.DOUBLE)) {
					mergedLocals[localIndex] = StackFrameType.UNKNOWN;
				}
			} else {
				changedFlag.set(true);
				if (otherLocals[localIndex].isThatType("?") || mergedLocals[localIndex].isThatType("?")) {
					mergedLocals[localIndex] = StackFrameType.UNKNOWN;
				} else if (isReferenceType(otherLocals[localIndex]) && isReferenceType(mergedLocals[localIndex])) {
					if (mergedLocals[localIndex].isThatType("n")) {
						mergedLocals[localIndex] = otherLocals[localIndex];
					} else if (!otherLocals[localIndex].isThatType("n")) {
						mergedLocals[localIndex] = StackFrameType.getResolved(typeResolver.getCommonSupertype(otherLocals[localIndex], mergedLocals[localIndex], debugContext));
					}
				} else {
					mergedLocals[localIndex] = StackFrameType.UNKNOWN;
				}
			}
		}

		return mergedLocals;
	}

	private static StackFrameType[] mergeStack(TypeResolver typeResolver, StackFrameType[] stack, StackFrameType[] otherStack, boolean copy, AtomicBool changedFlag, String debugContext) {
		changedFlag.set(false);
		if (stack.length != otherStack.length) {
			throw new InvalidBytecodeException("Inconsistent stack heights " + stack.length + " " + otherStack.length + " : '" + debugContext + "'");
		} else {
			int stackLength = stack.length;
			StackFrameType[] mergedStack;
			if (copy) {
				mergedStack = (StackFrameType[])stack.clone();
			} else {
				mergedStack = stack;
			}

			for (int i = 0; i < stackLength; i++) {
				if (!otherStack[i].equals(mergedStack[i])) {
					changedFlag.set(true);
					if (!isReferenceType(otherStack[i]) || !isReferenceType(mergedStack[i])) {
						throw new InvalidBytecodeException("Stack mismatch with types: " + mergedStack[i].getDescriptor() + " " + otherStack[i].getDescriptor() + " : '" + debugContext + "'");
					}

					if (mergedStack[i].isThatType("n")) {
						mergedStack[i] = otherStack[i];
					} else if (!otherStack[i].isThatType("n")) {
						mergedStack[i] = StackFrameType.getResolved(typeResolver.getCommonSupertype(otherStack[i], mergedStack[i], debugContext));
					}
				}
			}

			return mergedStack;
		}
	}

	private static StackFrameType[] mergeLocalsWithMask(
		TypeResolver typeResolver, StackFrameType[] locals, StackFrameType[] newLocals, LocalsInitializationState mask, AtomicBool changedFlag
	) {
		int localsLength = locals.length;
		StackFrameType[] mergedLocals = (StackFrameType[])locals.clone();
		changedFlag.set(false);
		if (mask != null) {
			for (int i = 0; i < localsLength; i++) {
				if (!newLocals[i].equals(mergedLocals[i]) && mask.get(i)) {
					changedFlag.set(true);
					mergedLocals[i] = newLocals[i];
				}
			}
		}

		return mergedLocals;
	}

	public static boolean isInteger(StackFrameType type) {
		return type.isThatType("I");
	}

	public static boolean isAssignable(StackFrameType sourceType, StackFrameType targetType, TypeResolver typeResolver, String debugContext) {
		if (!isReferenceType(sourceType) || !isReferenceType(targetType)) {
			return isPrimitiveCompatible(sourceType, targetType);
		} else if (sourceType.isThatType("n")) {
			return true;
		} else {
			return typeResolver.isReferenceAssignable(sourceType, targetType, debugContext) ? true : typeResolver.isInterface(targetType, debugContext);
		}
	}

	private static boolean isPrimitiveCompatible(StackFrameType sourceType, StackFrameType targetType) {
		if (sourceType.equals(targetType)) {
			return true;
		} else if (isInteger(sourceType) && isInteger(targetType)) {
			return true;
		} else if (targetType.isThatType("F")) {
			return sourceType.isThatType("I");
		} else {
			return !targetType.isThatType("D") ? isReferenceType(sourceType) && isReferenceType(targetType) : sourceType.isThatType("F") || sourceType.isThatType("I");
		}
	}

	public static boolean isReferenceType(StackFrameType type) {
		return type.startsWith("[") || type.startsWith("L") && type.endsWith(";") || type.isThatType("n");
	}

	public MonitorEnterNode pushMonitor(MonitorEnterNode monitorEnter) {
		if (this.activeMonitors == null) {
			this.activeMonitors = new LinkedHashSet<>();
		}

		this.activeMonitors.add(monitorEnter);
		return monitorEnter;
	}

	public MonitorEnterNode popMonitor(MonitorExitNode monitorExit) {
		if (this.activeMonitors != null && this.activeMonitors.size() > 0) {
			int monitorCount = this.activeMonitors.size();
			int monitorIndex = -1;
			MonitorEnterNode poppedMonitor = null;
			Iterator<MonitorEnterNode> monitorIterator = this.activeMonitors.iterator();

			while (monitorIterator.hasNext()) {
				monitorIndex++;
				MonitorEnterNode activeMonitor = monitorIterator.next();
				if (activeMonitor.getMonitorType() == monitorExit.getMonitorType()) {
					poppedMonitor = activeMonitor;
					monitorIterator.remove();
					break;
				}

				if (monitorIndex == monitorCount - 1) {
					poppedMonitor = activeMonitor;
					monitorIterator.remove();
				}
			}

			if (this.activeMonitors.size() == 0) {
				this.activeMonitors = null;
			}

			return poppedMonitor;
		} else {
			return null;
		}
	}

	public BitSet getUninitializedUsageMask() {
		return (BitSet)this.uninitializedUsageMask.clone();
	}

	private boolean isMonitorStateCompatible(FrameNode otherFrame, Set<StackFrameType> monitorTypes) {
		for (StackFrameType stackType : this.getOperandStack()) {
			if (monitorTypes.contains(stackType)) {
				return false;
			}
		}

		for (StackFrameType otherStackType : otherFrame.getOperandStack()) {
			if (monitorTypes.contains(otherStackType)) {
				return false;
			}
		}

		if (this.activeMonitors == null && otherFrame.activeMonitors == null) {
			return true;
		} else if (this.activeMonitors != null && otherFrame.activeMonitors != null) {
			if (this.activeMonitors.size() != otherFrame.activeMonitors.size()) {
				return false;
			} else {
				label47:
				for (MonitorEnterNode monitor : this.activeMonitors) {
					for (MonitorEnterNode otherMonitor : otherFrame.activeMonitors) {
						if (monitor == otherMonitor) {
							continue label47;
						}
					}

					return false;
				}

				return true;
			}
		} else {
			return false;
		}
	}

	private boolean isLocalsCompatibleWith(TypeResolver typeResolver, FrameNode otherFrame, String debugContext) {
		int localsLength = this.localVariables.length;
		StackFrameType[] otherLocals = otherFrame.localVariables;
		if (localsLength != otherLocals.length) {
			throw new ProcessingException("Mismatched local variable counts: " + localsLength + " " + otherLocals.length);
		} else {
			for (int i = 0; i < localsLength; i++) {
				if (!this.localVariables[i].equals(otherLocals[i]) && !otherLocals[i].isThatType("?")) {
					if (this.localVariables[i].isThatType("?")) {
						return false;
					}

					if (!isReferenceType(this.localVariables[i]) || !isReferenceType(otherLocals[i])) {
						return false;
					}

					if (this.localVariables[i].isResolved() != otherLocals[i].isResolved()) {
						return false;
					}

					if (otherLocals[i].isThatType("n")) {
						return false;
					}

					if (this.localVariables[i].isThatType("n")) {
						return false;
					}

					if (!typeResolver.isReferenceAssignable(this.localVariables[i], otherLocals[i], debugContext)) {
						return false;
					}
				}
			}

			return true;
		}
	}

	public StackFrameType[] getOperandStack() {
		return (StackFrameType[])this.operandStack.clone();
	}

	public LocalsInitializationState getLocalsInitState() {
		return this.localsInitState;
	}

	public LabelNode getInitializationLabel() {
		return this.localsInitState != null ? this.localsInitState.getOwnerLabel() : null;
	}

	public FrameNode mergeWith(TypeResolver typeResolver, FrameNode otherFrame, AtomicBool changedFlag, String debugContext) {
		AtomicBool stackChangedFlag = new AtomicBool();
		AtomicBool localsChangedFlag = new AtomicBool();
		StackFrameType[] mergedStack = mergeStack(typeResolver, this.operandStack, otherFrame.operandStack, true, stackChangedFlag, debugContext);
		StackFrameType[] mergedLocals = mergeLocals(typeResolver, this.localVariables, otherFrame.localVariables, true, localsChangedFlag, debugContext);
		changedFlag.set(stackChangedFlag.get() || localsChangedFlag.get());
		LinkedHashSet<MonitorEnterNode> mergedMonitors = new LinkedHashSet<>();
		if (this.activeMonitors != null) {
			mergedMonitors.addAll(this.activeMonitors);
		}

		if (otherFrame.activeMonitors != null) {
			mergedMonitors.addAll(otherFrame.activeMonitors);
		}

		if (mergedMonitors.size() > 0) {
			if (this.activeMonitors == null || !mergedMonitors.equals(this.activeMonitors)) {
				changedFlag.set(true);
			}
		} else {
			mergedMonitors = null;
		}

		FrameNode mergedFrame;
		if (this.localsInitState == null || otherFrame.localsInitState == null) {
			mergedFrame = new FrameNode(mergedStack, mergedLocals, mergedMonitors);
		} else if (this.localsInitState.equals(otherFrame.localsInitState)) {
			mergedFrame = new FrameNode(mergedStack, mergedLocals, (LocalsInitializationState)this.localsInitState.clone(), mergedMonitors);
		} else {
			LocalsInitializationState mergedLocalsInitState = (LocalsInitializationState)this.localsInitState.clone();
			mergedLocalsInitState.or(otherFrame.localsInitState);
			mergedFrame = new FrameNode(mergedStack, mergedLocals, mergedLocalsInitState, mergedMonitors);
			changedFlag.set(true);
		}

		return mergedFrame;
	}

	public boolean isCompatibleWith(TypeResolver typeResolver, FrameNode otherFrame, Set<StackFrameType> monitorTypes, boolean checkMonitors, String debugContext, int checkMode) {
		return this.isStackCompatibleWith(typeResolver, otherFrame, debugContext) && this.isLocalsCompatibleWith(typeResolver, otherFrame, debugContext) && (!checkMonitors || this.isMonitorStateCompatible(otherFrame, monitorTypes))
			|| checkMode == -1;
	}

	public boolean hasUninitializedTypes() {
		StackFrameType[] stackTypes = this.operandStack;
		int length = stackTypes.length;

		for (int i = 0; i < length; i++) {
			if (stackTypes[i].isUninitialized()) {
				return true;
			}
		}

		for (StackFrameType localVariable : this.localVariables) {
			if (localVariable != null && localVariable.isUninitialized()) {
				return true;
			}
		}

		return false;
	}

	public StackFrameType[] getLocalVariables() {
		return (StackFrameType[])this.localVariables.clone();
	}

	public boolean isLocalUsedUninitialized(int localIndex) {
		return this.uninitializedUsageMask.get(localIndex);
	}

	public boolean hasUninitializedUsageMask() {
		return this.uninitializedUsageMask != null;
	}

	public String getLocalTypeDescriptor(int localIndex) {
		return !this.localVariables[localIndex].isUninitializedMarker()
				&& this.localVariables[localIndex] != StackFrameType.NULL
				&& this.localVariables[localIndex] != StackFrameType.RETURN_ADDRESS
				&& this.localVariables[localIndex] != StackFrameType.UNKNOWN
			? this.localVariables[localIndex].getDescriptor()
			: null;
	}

	public StackFrameType[] getUninitializedLocals() {
		if (this.uninitializedUsageMask == null) {
			return (StackFrameType[])this.localVariables.clone();
		} else {
			StackFrameType[] uninitializedLocals = new StackFrameType[this.localVariables.length];

			for (int i = 0; i < this.localVariables.length; i++) {
				StackFrameType localType;
				if (!this.uninitializedUsageMask.get(i)
					&& (this.localVariables[i] != StackFrameType.UNINITIALIZED || uninitializedLocals[i - 1] != StackFrameType.LONG && uninitializedLocals[i - 1] != StackFrameType.DOUBLE)) {
					localType = StackFrameType.UNKNOWN;
				} else {
					localType = this.localVariables[i];
				}

				uninitializedLocals[i] = localType;
			}

			return uninitializedLocals;
		}
	}

	private boolean isStackCompatibleWith(TypeResolver typeResolver, FrameNode otherFrame, String debugContext) {
		int length = this.operandStack.length;
		if (length != otherFrame.getStackSize()) {
			return false;
		} else {
			for (int i = 0; i < length; i++) {
				if (!this.operandStack[i].equals(otherFrame.operandStack[i])) {
					if (!isReferenceType(this.operandStack[i]) || !isReferenceType(otherFrame.operandStack[i])) {
						return false;
					}

					if (this.operandStack[i].isResolved() != otherFrame.operandStack[i].isResolved()) {
						return false;
					}

					if (this.operandStack[i].isThatType("n")) {
						return false;
					}

					if (otherFrame.operandStack[i].isThatType("n")) {
						return false;
					}

					if (!typeResolver.isReferenceAssignable(this.operandStack[i], otherFrame.operandStack[i], debugContext)) {
						return false;
					}
				}
			}

			return true;
		}
	}

	public boolean hasCategory2Locals() {
		for (int i = 0; i < this.localVariables.length; i++) {
			if (isCategory2Type(this.localVariables[i])) {
				return true;
			}
		}

		return false;
	}

	public boolean isLocalTypeValid(int localIndex) {
		return !this.localVariables[localIndex].isUninitializedMarker()
			&& this.localVariables[localIndex] != StackFrameType.RETURN_ADDRESS
			&& this.localVariables[localIndex] != StackFrameType.UNKNOWN;
	}

	public final int getStackSize() {
		return this.operandStack.length;
	}

	public Set<MonitorEnterNode> getActiveMonitors() {
		return this.activeMonitors != null ? new LinkedHashSet<>(this.activeMonitors) : null;
	}

	public AnalyzedFrame createAnalyzedFrame(boolean compact) {
		return (AnalyzedFrame)(compact ? new CompactFrame(this) : new ExpandedFrame(this));
	}

	public boolean isLocalInitialized(int localIndex) {
		return this.localVariables[localIndex] != null && !this.localVariables[localIndex].isThatType("?");
	}

	public int getStackDepth() {
		int depth = 0;
		StackFrameType[] stackTypes = this.operandStack;
		int length = stackTypes.length;

		for (int i = 0; i < length; i++) {
			depth += stackTypes[i].getStackSize();
		}

		return depth;
	}

	public boolean markUninitializedUsage(BitSet mask) {
		Object previousUsageMask = null;
		boolean maskCreated;
		if (this.uninitializedUsageMask == null) {
			this.uninitializedUsageMask = new BitSet(this.localVariables.length);
			maskCreated = true;
		} else {
			previousUsageMask = this.uninitializedUsageMask.clone();
			maskCreated = false;
		}

		this.uninitializedUsageMask.or(mask);
		return maskCreated || !this.uninitializedUsageMask.equals(previousUsageMask);
	}

	public boolean isFullyCompatibleWith(TypeResolver typeResolver, FrameNode otherFrame, Set<StackFrameType> monitorTypes, boolean checkMonitors, String debugContext) {
		return this.isStackCompatibleWith(typeResolver, otherFrame, debugContext) && this.isLocalsCompatibleWith(typeResolver, otherFrame, debugContext) && (!checkMonitors || this.isMonitorStateCompatible(otherFrame, monitorTypes));
	}

	public boolean isFullyResolved() {
		int length = this.operandStack.length;

		for (int i = 0; i < length; i++) {
			if (!this.operandStack[i].isResolved()) {
				return false;
			}
		}

		length = this.localVariables.length;

		for (int localIndex = 0; localIndex < length; localIndex++) {
			if (!this.localVariables[localIndex].isResolved()) {
				return false;
			}
		}

		return true;
	}
}

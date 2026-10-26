package com.zelix.framework.instructions;

import com.zelix.framework.ClearingFlag;
import com.zelix.framework.SettingFlag;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.utils.GlobalUtils;
import java.io.DataOutputStream;
import java.io.PrintWriter;

public class LabelNode extends AbstractInsnNode {
	private int offset = -1;
	private int labelId = -1;
	private int flags = 0;

	private LabelNode(int bytecodeOffset, boolean isVisible, boolean isFixed, int flags) {
		super(2147483646);
		this.addFlag(flags);
		this.offset = bytecodeOffset;
		if (isVisible) {
			this.addFlag(16384);
		}

		if (isFixed) {
			this.addFlag(32768);
		}
	}

	public LabelNode() {
		this(0, true, true, 2);
	}

	public LabelNode(int opcode) {
		this(-1, false, opcode);
	}

	public LabelNode(boolean isVisible, int flags) {
		this(-1, isVisible, flags);
	}

	public LabelNode(int bytecodeOffset, boolean isVisible, int flags) {
		this(bytecodeOffset, isVisible, false, flags);
	}

	public LabelNode(int bytecodeOffset, int flags) {
		this(bytecodeOffset, false, flags);
	}

	@Override
	public int getStackEffect() {
		return 0;
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		if (this.isVisible()) {
			printWriter.println(indent.toString() + this.getLabelName());
		}
	}

	public boolean isVisible() {
		return this.hasFlag(16384);
	}

	public int getOffset() {
		return this.offset;
	}

	@Override
	public final boolean isMethodExit() {
		return false;
	}

	@Override
	public boolean isLabel() {
		return true;
	}

	public String getLabelName() {
		StringBuilder labelName = new StringBuilder(10);
		labelName.append("label_");
		labelName.append(this.offset);
		if (this.isFixed()) {
			labelName.append('*');
		}

		return labelName.toString();
	}

	public boolean isJumpDestination() {
		return (GlobalUtils.toUnsignedShort(this.flags) & 4095) == 1;
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		return new FrameNode(frame.getOperandStack(), frame.getLocalVariables(), frame.getLocalsInitState(), frame.getActiveMonitors());
	}

	public void addFlag(int flag) {
		this.flags |= flag;
	}

	public void setVisible(boolean isVisible) {
		if (isVisible) {
			this.addFlag(16384);
		} else {
			this.removeFlag(ClearingFlag.CLEAR_USAGE_IS_VISIBLE);
		}
	}

	public void setLabelID(int labelId) {
		this.labelId = labelId;
	}

	public boolean hasFlag(int flag) {
		return (GlobalUtils.toUnsignedShort(this.flags) & flag) != 0;
	}

	public void addUsage(SettingFlag settingFlag) {
		this.flags = this.flags | settingFlag.getFlag();
	}

	public void removeFlag(ClearingFlag clearingFlag) {
		this.flags = this.flags & clearingFlag.getFlag();
	}

	@Override
	public boolean isStackProducer() {
		return false;
	}

	@Override
	public String getOpcodeMnemonics() {
		return this.getLabelName();
	}

	public boolean hasNoUsage() {
		return GlobalUtils.toUnsignedShort(this.flags) == 0;
	}

	@Override
	public void setOffset(int bytecodeOffset) {
		this.offset = bytecodeOffset;
	}

	@Override
	public final boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		return false;
	}

	public int getLabelID() {
		return this.labelId;
	}

	@Override
	public boolean isCategory2Producer() {
		return false;
	}

	@Override
	public int getBytecodeSize() {
		return 0;
	}

	@Override
	public boolean isStandaloneStackProducer() {
		return false;
	}

	@Override
	public final boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		return false;
	}

	@Override
	public final boolean canFallThrough() {
		return true;
	}

	public int getFlags() {
		return GlobalUtils.toUnsignedShort(this.flags);
	}

	public boolean isMarker() {
		return (GlobalUtils.toUnsignedShort(this.flags) & 8192) != 0;
	}

	public boolean isFixed() {
		return this.hasFlag(32768);
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) {
	}

	@Override
	public String getOpcodeText() {
		return this.toString();
	}
}

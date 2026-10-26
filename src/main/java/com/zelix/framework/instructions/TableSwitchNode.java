package com.zelix.framework.instructions;

import com.zelix.framework.reference.IGraphElement;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class TableSwitchNode extends AbstractSwitchNode {
	private int caseCount = -1;
	private final int lowValue;
	private final int highValue;

	public TableSwitchNode(ByteDataBuffer dataBuffer, int bytecodeOffset, MultiMap<Integer, IGraphElement> elementsByOffset) throws IOException {
		super(TABLESWITCH, dataBuffer, bytecodeOffset);
		int defaultOffset = dataBuffer.readInt();
		this.lowValue = dataBuffer.readInt();
		this.highValue = dataBuffer.readInt();
		this.caseCount = this.highValue - this.lowValue + 1;
		this.targetLabels = new LabelNode[this.caseCount + 1];
		this.targetOffsets = new ArrayList<>(this.targetLabels.length);

		for (int i = 0; i < this.caseCount; i++) {
			int caseOffset = dataBuffer.readInt();
			Integer caseTargetOffset = AbstractInsnNode.INTEGER_CACHE.valueOf(super.bytecodeOffset + caseOffset);
			elementsByOffset.put(caseTargetOffset, this);
			this.targetOffsets.add(caseTargetOffset);
		}

		Integer defaultTargetOffset = AbstractInsnNode.INTEGER_CACHE.valueOf(super.bytecodeOffset + defaultOffset);
		elementsByOffset.put(defaultTargetOffset, this);
		this.targetOffsets.add(defaultTargetOffset);
	}

	public TableSwitchNode(LabelNode defaultLabel, int lowValue, int highValue, LabelNode[] targetLabels) {
		super(TABLESWITCH);
		this.lowValue = lowValue;
		this.highValue = highValue;
		this.caseCount = this.highValue - this.lowValue + 1;
		this.targetLabels = new LabelNode[this.caseCount + 1];

		for (int i = 0; i < this.caseCount; i++) {
			this.targetLabels[i] = targetLabels[i];
		}

		this.targetLabels[this.targetLabels.length - 1] = defaultLabel;
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder();
		textBuffer.append(this.getOpcodeMnemonics());
		printWriter.println(indent.toString() + textBuffer.toString());
		Object defaultLabel = this.targetLabels[this.targetLabels.length - 1];
		printWriter.println(indent.toString() + "\tdefault: goto " + (defaultLabel != null ? ((LabelNode)defaultLabel).getLabelName() : defaultLabel));

		for (int i = 0; i < this.caseCount; i++) {
			Object caseLabel = this.targetLabels[i];
			int caseValue = this.lowValue + i;
			printWriter.println(indent.toString() + "\tcase " + caseValue + ": goto " + (caseLabel != null ? ((LabelNode)caseLabel).getLabelName() : caseLabel));
		}
	}

	@Override
	public int getBytecodeSize() {
		return 1 + super.padding + 12 + this.caseCount * 4;
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeInt(this.lowValue);
		dataOutputStream.writeInt(this.highValue);

		for (int i = 0; i < this.caseCount; i++) {
			LabelNode caseLabel = this.targetLabels[i];
			dataOutputStream.writeInt(caseLabel.getOffset() - super.bytecodeOffset);
		}
	}

	@Override
	public String getOpcodeText() {
		return this.getOpcodeMnemonics();
	}
}

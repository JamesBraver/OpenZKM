package com.zelix.framework.instructions;

import com.zelix.framework.reference.IGraphElement;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class LookupSwitchNode extends AbstractSwitchNode {
	private final int caseCount;
	private final int[] caseValues;

	public LookupSwitchNode(ByteDataBuffer dataBuffer, int bytecodeOffset, MultiMap<Integer, IGraphElement> elementsByOffset) throws IOException {
		super(LOOKUPSWITCH, dataBuffer, bytecodeOffset);
		int defaultOffset = dataBuffer.readInt();
		this.caseCount = dataBuffer.readInt();
		this.caseValues = new int[this.caseCount];
		this.targetLabels = new LabelNode[this.caseCount + 1];
		this.targetOffsets = new ArrayList<>(this.targetLabels.length);

		for (int i = 0; i < this.caseCount; i++) {
			int matchValue = dataBuffer.readInt();
			this.caseValues[i] = matchValue;
			int caseOffset = dataBuffer.readInt();
			Integer caseTargetOffset = AbstractInsnNode.INTEGER_CACHE.valueOf(super.bytecodeOffset + caseOffset);
			elementsByOffset.put(caseTargetOffset, this);
			this.targetOffsets.add(caseTargetOffset);
		}

		Integer defaultTargetOffset = AbstractInsnNode.INTEGER_CACHE.valueOf(super.bytecodeOffset + defaultOffset);
		elementsByOffset.put(defaultTargetOffset, this);
		this.targetOffsets.add(defaultTargetOffset);
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
			printWriter.println(indent.toString() + "\tcase " + this.caseValues[i] + ": goto " + (caseLabel != null ? ((LabelNode)caseLabel).getLabelName() : caseLabel));
		}
	}

	@Override
	public int getBytecodeSize() {
		return 1 + super.padding + 8 + this.caseCount * 8;
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeInt(this.caseCount);

		for (int i = 0; i < this.caseCount; i++) {
			dataOutputStream.writeInt(this.caseValues[i]);
			LabelNode caseLabel = this.targetLabels[i];
			dataOutputStream.writeInt(caseLabel.getOffset() - super.bytecodeOffset);
		}
	}

	@Override
	public String getOpcodeText() {
		return this.getOpcodeMnemonics();
	}
}

package com.zelix.framework.instructions;

import com.zelix.framework.SettingFlag;
import com.zelix.framework.analyzer.AnalyzingNode;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.reference.IControlFlowElement;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractSwitchNode extends AbstractInsnNode implements IControlFlowElement {
	public int bytecodeOffset;
	public int padding;
	public List<Integer> targetOffsets;
	public LabelNode[] targetLabels;

	public AbstractSwitchNode(int opcode) {
		super(opcode);
	}

	public AbstractSwitchNode(int opcode, ByteDataBuffer dataBuffer, int bytecodeOffset) throws IOException {
		super(opcode);
		this.bytecodeOffset = bytecodeOffset;
		this.padding = computePadding(bytecodeOffset);
		dataBuffer.skip(this.padding);
	}

	@Override
	public int getStackEffect() {
		return -1;
	}

	public static int computePadding(int bytecodeOffset) {
		return (4 - (bytecodeOffset + 1) % 4) % 4;
	}

	@Override
	public final boolean isMethodExit() {
		return false;
	}

	@Override
	public SettingFlag getSettingFlag() {
		return SettingFlag.SET_USAGE_JUMP_DESTINATION;
	}

	@Override
	public final FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		StackFrameType[] operandStack = frame.getOperandStack();
		StackFrameType[] localVariables = frame.getLocalVariables();
		int operandStackSize = operandStack.length;
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(operandStackSize - 1);
		System.arraycopy(operandStack, 0, newOperandStack, 0, operandStackSize - 1);
		return new FrameNode(newOperandStack, localVariables, frame.getLocalsInitState(), frame.getActiveMonitors());
	}

	@Override
	public void setLabel(Integer pos, LabelNode lbl) {
		int targetCount = this.targetLabels.length;
		lbl.setVisible(true);

		for (int i = 0; i < targetCount; i++) {
			if (this.targetOffsets.get(i).equals(pos) && this.targetLabels[i] == null) {
				this.targetLabels[i] = lbl;
			}
		}
	}

	@Override
	public String getMnemonics() {
		return this.getOpcodeMnemonics();
	}

	@Override
	public boolean isStackProducer() {
		return false;
	}

	@Override
	public void setOffset(int bytecodeOffset) {
		this.bytecodeOffset = bytecodeOffset;
		int paddingSize = computePadding(bytecodeOffset);
		this.padding = paddingSize;
	}

	@Override
	public final boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		return stackIndex >= stackSize;
	}

	@Override
	public boolean isCategory2Producer() {
		return false;
	}

	@Override
	public boolean isStandaloneStackProducer() {
		return false;
	}

	@Override
	public final boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		return stackIndex >= stackSize;
	}

	public List<LabelNode> getTargetLabels() {
		ArrayList<LabelNode> labels = new ArrayList<>(this.targetLabels.length);

		for (LabelNode label : this.targetLabels) {
			labels.add(label);
		}

		return labels;
	}

	@Override
	public boolean isJump() {
		return true;
	}

	@Override
	public final boolean canFallThrough() {
		return false;
	}

	@Override
	public final void registerTargetNodes(Map<LabelNode, AnalyzingNode> nodesByLabel, MultiMap<IControlFlowElement, AnalyzingNode> nodesByElement, List<AnalyzingNode> newNodes) {
		this.targetOffsets = null;

		for (int i = 0; i < this.targetLabels.length; i++) {
			LabelNode targetLabel = this.targetLabels[i];
			AnalyzingNode targetNode;
			if ((targetNode = nodesByLabel.get(targetLabel)) == null) {
				targetNode = new AnalyzingNode();
				newNodes.add(targetNode);
				nodesByLabel.put(targetLabel, targetNode);
			}

			nodesByElement.put(this, targetNode);
		}
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);

		for (int i = 0; i < this.padding; i++) {
			dataOutputStream.writeByte(0);
		}

		LabelNode defaultLabel = this.targetLabels[this.targetLabels.length - 1];
		dataOutputStream.writeInt(defaultLabel.getOffset() - this.bytecodeOffset);
	}

	@Override
	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		for (LabelNode targetLabel : this.targetLabels) {
			graph.addReference(targetLabel, this);
		}
	}
}

package com.zelix.framework.instructions;

import com.zelix.exceptions.LookupException;
import com.zelix.framework.SettingFlag;
import com.zelix.framework.analyzer.*;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.reference.IControlFlowElement;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class JumpInsnNode extends AbstractInsnNode implements IControlFlowElement {
	public int bytecodeOffset;
	public LabelNode targetLabel;

	public JumpInsnNode(int opcode, ByteDataBuffer dataBuffer, int bytecodeOffset, MultiMap<Integer, IGraphElement> elementsByOffset) throws IOException {
		super(opcode);
		this.bytecodeOffset = bytecodeOffset;
		int targetOffset = this.readBranchOffset(dataBuffer);
		if (targetOffset < 0) {
			throw new LookupException("Bad offset : " + opcode + " : " + bytecodeOffset + " : " + this.bytecodeOffset + " : " + targetOffset);
		} else {
			elementsByOffset.put(AbstractInsnNode.INTEGER_CACHE.valueOf(targetOffset), this);
		}
	}

	public JumpInsnNode(int opcode, LabelNode targetLabel) {
		super(opcode);
		this.bytecodeOffset = -1;
		this.targetLabel = targetLabel;
	}

	@Override
	public int getStackEffect() {
		switch (this.opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IFNULL:
			case IFNONNULL:
				return -1;
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
				return -2;
			default:
				return 0;
		}
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		String mnemonics = this.getOpcodeMnemonics();
		textBuffer.append(mnemonics + " " + this.targetLabel.getLabelName());
		String comment = AbstractInsnNode.fillCommentPlaceholder(AbstractInsnNode.getOpcodeComment(this.opcode), this.targetLabel.getLabelName());
		if (comment.length() > 0) {
			textBuffer.append("\t" + comment);
		}

		printWriter.println(indent.toString() + indent.toString() + textBuffer);
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
	public void setLabel(Integer pos, LabelNode lbl) {
		this.targetLabel = lbl;
		lbl.setVisible(true);
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		AnalyzeStackDebugContext debugContext = new AnalyzeStackDebugContext(methodSignature);
		StackFrameType[] operandStack = frame.getOperandStack();
		StackFrameType[] localVariables = frame.getLocalVariables();
		int operandStackSize = operandStack.length;
		LocalsInitializationState localsInitState = frame.getLocalsInitState();
		Set<MonitorEnterNode> activeMonitors = frame.getActiveMonitors();
		switch (this.opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
				StackFrameType[] intIfOperandStack = StackFrameType.createEmptyFrame(operandStackSize - 1);
				System.arraycopy(operandStack, 0, intIfOperandStack, 0, operandStackSize - 1);
				return new FrameNode(intIfOperandStack, localVariables, localsInitState, activeMonitors);
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
				StackFrameType[] intCmpOperandStack = StackFrameType.createEmptyFrame(operandStackSize - 2);
				System.arraycopy(operandStack, 0, intCmpOperandStack, 0, operandStackSize - 2);
				return new FrameNode(intCmpOperandStack, localVariables, localsInitState, activeMonitors);
			case IF_ACMPEQ:
			case IF_ACMPNE:
				StackFrameType[] refCmpOperandStack = StackFrameType.createEmptyFrame(operandStackSize - 2);
				System.arraycopy(operandStack, 0, refCmpOperandStack, 0, operandStackSize - 2);
				return new FrameNode(refCmpOperandStack, localVariables, localsInitState, activeMonitors);
			case GOTO:
			case JSR:
			case GOTO_W:
			case JSR_W:
				return null;
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
			case NEW:
			case NEWARRAY:
			case ANEWARRAY:
			case ARRAYLENGTH:
			case ATHROW:
			case CHECKCAST:
			case INSTANCEOF:
			case MONITORENTER:
			case MONITOREXIT:
			case WIDE:
			case MULTIANEWARRAY:
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode + " " + debugContext});
				return null;
			case IFNULL:
			case IFNONNULL:
				StackFrameType[] nullCheckOperandStack = StackFrameType.createEmptyFrame(operandStackSize - 1);
				System.arraycopy(operandStack, 0, nullCheckOperandStack, 0, operandStackSize - 1);
				return new FrameNode(nullCheckOperandStack, localVariables, localsInitState, activeMonitors);
		}
	}

	public int readBranchOffset(ByteDataBuffer dataBuffer) throws IOException {
		short relativeOffset = dataBuffer.readShort();
		return this.bytecodeOffset + relativeOffset;
	}

	@Override
	public String getMnemonics() {
		return this.getOpcodeMnemonics();
	}

	@Override
	public boolean isStackProducer() {
		switch (this.opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
			case GOTO:
			case IFNULL:
			case IFNONNULL:
			case GOTO_W:
				return false;
			case JSR:
			case JSR_W:
				return true;
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
			case NEW:
			case NEWARRAY:
			case ANEWARRAY:
			case ARRAYLENGTH:
			case ATHROW:
			case CHECKCAST:
			case INSTANCEOF:
			case MONITORENTER:
			case MONITOREXIT:
			case WIDE:
			case MULTIANEWARRAY:
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return false;
		}
	}

	public void setTargetLabel(LabelNode targetLabel) {
		this.targetLabel = targetLabel;
	}

	@Override
	public void setOffset(int bytecodeOffset) {
		this.bytecodeOffset = bytecodeOffset;
	}

	@Override
	public final boolean hasFallThroughOrIsJsr() {
		switch (this.opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
			case JSR:
			case IFNULL:
			case IFNONNULL:
			case JSR_W:
				return true;
			case GOTO:
			case GOTO_W:
				return false;
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
			case NEW:
			case NEWARRAY:
			case ANEWARRAY:
			case ARRAYLENGTH:
			case ATHROW:
			case CHECKCAST:
			case INSTANCEOF:
			case MONITORENTER:
			case MONITOREXIT:
			case WIDE:
			case MULTIANEWARRAY:
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return true;
		}
	}

	@Override
	public final boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		switch (this.opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
				return stackIndex >= stackSize;
			case IF_ACMPEQ:
			case IF_ACMPNE:
			case IFNULL:
			case IFNONNULL:
				return false;
			case GOTO:
			case JSR:
			case GOTO_W:
			case JSR_W:
				return false;
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
			case NEW:
			case NEWARRAY:
			case ANEWARRAY:
			case ARRAYLENGTH:
			case ATHROW:
			case CHECKCAST:
			case INSTANCEOF:
			case MONITORENTER:
			case MONITOREXIT:
			case WIDE:
			case MULTIANEWARRAY:
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return false;
		}
	}

	public LabelNode getTargetLabel() {
		return this.targetLabel;
	}

	@Override
	public boolean isCategory2Producer() {
		return false;
	}

	@Override
	public List<AbstractInsnNode> expandToWideIfNeeded() {
		int branchOffset = this.getBranchOffset();
		if (branchOffset >= -32768 && branchOffset <= 32767) {
			return null;
		} else {
			ArrayList<AbstractInsnNode> instructions = new ArrayList<>(5);
			LabelNode originalTarget = this.targetLabel;
			LabelNode wideJumpLabel = new LabelNode(true, 1);
			LabelNode skipLabel = new LabelNode(true, 1);
			this.setTargetLabel(wideJumpLabel);
			instructions.add(this);
			instructions.add(new GotoNode(skipLabel));
			instructions.add(wideJumpLabel);
			instructions.add(new WideJumpInsnNode(GOTO_W, originalTarget));
			instructions.add(skipLabel);
			return instructions;
		}
	}

	public int getBranchOffset() {
		return this.targetLabel.getOffset() - this.bytecodeOffset;
	}

	@Override
	public int getBytecodeSize() {
		return 3;
	}

	@Override
	public boolean isStandaloneStackProducer() {
		return this.isStackProducer();
	}

	@Override
	public boolean isJump() {
		return true;
	}

	@Override
	public final boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		switch (this.opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
			case IFNULL:
			case IFNONNULL:
				return stackIndex >= stackSize;
			case GOTO:
			case JSR:
			case GOTO_W:
			case JSR_W:
				return false;
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
			case NEW:
			case NEWARRAY:
			case ANEWARRAY:
			case ARRAYLENGTH:
			case ATHROW:
			case CHECKCAST:
			case INSTANCEOF:
			case MONITORENTER:
			case MONITOREXIT:
			case WIDE:
			case MULTIANEWARRAY:
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return false;
		}
	}

	public void writeBranchOffset(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.getBranchOffset());
	}

	@Override
	public final boolean canFallThrough() {
		switch (this.opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
			case IFNULL:
			case IFNONNULL:
				return true;
			case GOTO:
			case JSR:
			case GOTO_W:
			case JSR_W:
				return false;
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
			case NEW:
			case NEWARRAY:
			case ANEWARRAY:
			case ARRAYLENGTH:
			case ATHROW:
			case CHECKCAST:
			case INSTANCEOF:
			case MONITORENTER:
			case MONITOREXIT:
			case WIDE:
			case MULTIANEWARRAY:
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return true;
		}
	}

	@Override
	public final void registerTargetNodes(Map<LabelNode, AnalyzingNode> nodesByLabel, MultiMap<IControlFlowElement, AnalyzingNode> nodesByElement, List<AnalyzingNode> newNodes) {
		AnalyzingNode targetNode;
		if ((targetNode = nodesByLabel.get(this.targetLabel)) == null) {
			targetNode = new AnalyzingNode();
			nodesByLabel.put(this.targetLabel, targetNode);
			newNodes.add(targetNode);
		}

		nodesByElement.put(this, targetNode);
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		this.writeBranchOffset(dataOutputStream);
	}

	@Override
	public String getOpcodeText() {
		return this.getOpcodeMnemonics();
	}

	@Override
	public final void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		graph.addReference(this.targetLabel, this);
	}
}

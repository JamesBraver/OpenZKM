package com.zelix.framework.instructions;

import com.zelix.exceptions.MethodAnalyzerException;
import com.zelix.framework.analyzer.AnalyzeStackDebugContext;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.LocalsInitializationState;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.utils.DebugUtils;
import java.io.PrintWriter;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class InsnNode extends AbstractInsnNode {
	private static final InsnNode[] opcodeCache = new InsnNode[202];

	private InsnNode(int opcode) {
		super(opcode);
	}

	public static InsnNode of(int opcode) {
		return opcodeCache[opcode];
	}

	@Override
	public int getStackEffect() {
		switch (this.opcode) {
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
				return 1;
			case LCONST_0:
			case LCONST_1:
			case DCONST_0:
			case DCONST_1:
				return 2;
			case IALOAD:
			case FALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				return -1;
			case LALOAD:
			case DALOAD:
				return 0;
			case IASTORE:
			case FASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				return -3;
			case LASTORE:
			case DASTORE:
				return -4;
			case POP:
				return -1;
			case POP2:
				return -2;
			case DUP:
			case DUP_X1:
			case DUP_X2:
				return 1;
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
				return 2;
			case IADD:
			case ISUB:
			case IMUL:
			case IDIV:
			case IREM:
			case IAND:
			case IOR:
			case IXOR:
			case FADD:
			case FSUB:
			case FMUL:
			case FDIV:
			case FREM:
				return -1;
			case LADD:
			case LSUB:
			case LMUL:
			case LDIV:
			case LREM:
			case LAND:
			case LOR:
			case LXOR:
			case DADD:
			case DSUB:
			case DMUL:
			case DDIV:
			case DREM:
				return -2;
			case I2L:
			case I2D:
			case L2D:
			case F2L:
			case F2D:
				return 1;
			case L2I:
			case L2F:
			case D2I:
			case D2F:
				return -1;
			case ISHL:
			case ISHR:
			case IUSHR:
			case LSHL:
			case LSHR:
			case LUSHR:
				return -1;
			case LCMP:
			case DCMPL:
			case DCMPG:
				return -3;
			case FCMPL:
			case FCMPG:
				return -1;
			case IRETURN:
			case FRETURN:
			case ARETURN:
				return -1;
			case LRETURN:
			case DRETURN:
				return -2;
			default:
				return 0;
		}
	}

	@Override
	public boolean isProducedByStackOp(StackFrameType[] stackBefore, int newStackIndex) {
		int topIndex = stackBefore.length - 1;
		switch (this.opcode) {
			case POP:
			case POP2:
				return false;
			case DUP:
				return newStackIndex >= topIndex - 1;
			case DUP_X1:
				return newStackIndex >= topIndex - 2;
			case DUP_X2:
				if (stackBefore[topIndex - 1].isCategory2()) {
					return newStackIndex >= topIndex - 2;
				}

				return newStackIndex >= topIndex - 3;
			case DUP2:
				if (stackBefore[topIndex].isCategory2()) {
					return newStackIndex >= topIndex - 1;
				}

				return newStackIndex >= topIndex - 3;
			case DUP2_X1:
				if (stackBefore[topIndex].isCategory2()) {
					return newStackIndex >= topIndex - 2;
				}

				return newStackIndex >= topIndex - 4;
			case DUP2_X2:
				if (stackBefore[topIndex].isCategory2()) {
					if (stackBefore[topIndex - 1].isCategory2()) {
						return newStackIndex >= topIndex - 2;
					}

					return newStackIndex >= topIndex - 3;
				} else {
					if (stackBefore[topIndex - 2].isCategory2()) {
						return newStackIndex >= topIndex - 4;
					}

					return newStackIndex >= topIndex - 5;
				}
			case SWAP:
				return newStackIndex >= topIndex - 1;
			default:
				DebugUtils.assertTrue(false, new String[]{this.opcode + " " + this.getOpcodeMnemonics()});
				return false;
		}
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder insnText = new StringBuilder(100);
		String mnemonics = this.getOpcodeMnemonics();
		insnText.append(mnemonics);
		String descriptions = this.getOpcodeComment();
		if (descriptions.length() > 0) {
			insnText.append("\t" + descriptions);
		}

		printWriter.println(indent.toString() + indent.toString() + insnText);
	}

	@Override
	public final boolean isMethodExit() {
		switch (this.opcode) {
			case NOP:
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
			case IALOAD:
			case LALOAD:
			case FALOAD:
			case DALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
			case POP:
			case POP2:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
			case IADD:
			case LADD:
			case FADD:
			case DADD:
			case ISUB:
			case LSUB:
			case FSUB:
			case DSUB:
			case IMUL:
			case LMUL:
			case FMUL:
			case DMUL:
			case IDIV:
			case LDIV:
			case FDIV:
			case DDIV:
			case IREM:
			case LREM:
			case FREM:
			case DREM:
			case INEG:
			case LNEG:
			case FNEG:
			case DNEG:
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
			case IAND:
			case LAND:
			case IOR:
			case LOR:
			case IXOR:
			case LXOR:
			case I2L:
			case I2F:
			case I2D:
			case L2I:
			case L2F:
			case L2D:
			case F2I:
			case F2L:
			case F2D:
			case D2I:
			case D2L:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case ARRAYLENGTH:
				return false;
			case BIPUSH:
			case SIPUSH:
			case LDC:
			case LDC_W:
			case LDC2_W:
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IINC:
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
			case JSR:
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return false;
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case ATHROW:
				return true;
		}
	}

	@Override
	public final boolean isIntegerOpcode() {
		switch (this.opcode) {
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean isPureStackOpcode() {
		switch (this.opcode) {
			case POP:
			case POP2:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
				return true;
			default:
				return false;
		}
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		new AnalyzeStackDebugContext(methodSignature);
		StackFrameType[] localVariables = frame.getLocalVariables();
		StackFrameType[] operandStack = frame.getOperandStack();
		Set<MonitorEnterNode> activeMonitors = frame.getActiveMonitors();
		int stackSize = operandStack.length;
		LocalsInitializationState localsInitState = frame.getLocalsInitState();
		switch (this.opcode) {
			case NOP:
				return new FrameNode(operandStack, localVariables, localsInitState, activeMonitors);
			case ACONST_NULL:
				StackFrameType[] pushNullStack = StackFrameType.createEmptyFrame(stackSize + 1);
				System.arraycopy(operandStack, 0, pushNullStack, 0, stackSize);
				pushNullStack[stackSize] = StackFrameType.NULL;
				return new FrameNode(pushNullStack, localVariables, localsInitState, activeMonitors);
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
				return this.createPushedValueFrame(stackSize, operandStack, localVariables, StackFrameType.INT, localsInitState, activeMonitors);
			case LCONST_0:
			case LCONST_1:
				return this.createPushedValueFrame(stackSize, operandStack, localVariables, StackFrameType.LONG, localsInitState, activeMonitors);
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
				return this.createPushedValueFrame(stackSize, operandStack, localVariables, StackFrameType.FLOAT, localsInitState, activeMonitors);
			case DCONST_0:
			case DCONST_1:
				return this.createPushedValueFrame(stackSize, operandStack, localVariables, StackFrameType.DOUBLE, localsInitState, activeMonitors);
			case BIPUSH:
			case SIPUSH:
			case LDC:
			case LDC_W:
			case LDC2_W:
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IINC:
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
			case JSR:
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return null;
			case IALOAD:
				return this.createArrayLoadFrame(stackSize, operandStack, localVariables, StackFrameType.INT_ARRAY, StackFrameType.INT, localsInitState, activeMonitors);
			case LALOAD:
				return this.createArrayLoadFrame(stackSize, operandStack, localVariables, StackFrameType.LONG_ARRAY, StackFrameType.LONG, localsInitState, activeMonitors);
			case FALOAD:
				return this.createArrayLoadFrame(stackSize, operandStack, localVariables, StackFrameType.FLOAT_ARRAY, StackFrameType.FLOAT, localsInitState, activeMonitors);
			case DALOAD:
				return this.createArrayLoadFrame(stackSize, operandStack, localVariables, StackFrameType.DOUBLE_ARRAY, StackFrameType.DOUBLE, localsInitState, activeMonitors);
			case AALOAD:
				StackFrameType[] arrayLoadStack = StackFrameType.createEmptyFrame(stackSize - 1);
				System.arraycopy(operandStack, 0, arrayLoadStack, 0, stackSize - 2);
				arrayLoadStack[stackSize - 2] = operandStack[stackSize - 2].getElementType();
				return new FrameNode(arrayLoadStack, localVariables, localsInitState, activeMonitors);
			case BALOAD:
				return this.createArrayLoadFrame(stackSize, operandStack, localVariables, StackFrameType.BYTE_ARRAY, StackFrameType.INT, localsInitState, activeMonitors);
			case CALOAD:
				return this.createArrayLoadFrame(stackSize, operandStack, localVariables, StackFrameType.CHAR_ARRAY, StackFrameType.INT, localsInitState, activeMonitors);
			case SALOAD:
				return this.createArrayLoadFrame(stackSize, operandStack, localVariables, StackFrameType.SHORT_ARRAY, StackFrameType.INT, localsInitState, activeMonitors);
			case IASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				StackFrameType[] intArrayStoreStack = StackFrameType.createEmptyFrame(stackSize - 3);
				System.arraycopy(operandStack, 0, intArrayStoreStack, 0, stackSize - 3);
				return new FrameNode(intArrayStoreStack, localVariables, localsInitState, activeMonitors);
			case LASTORE:
				StackFrameType[] longArrayStoreStack = StackFrameType.createEmptyFrame(stackSize - 3);
				System.arraycopy(operandStack, 0, longArrayStoreStack, 0, stackSize - 3);
				return new FrameNode(longArrayStoreStack, localVariables, localsInitState, activeMonitors);
			case FASTORE:
				StackFrameType[] floatArrayStoreStack = StackFrameType.createEmptyFrame(stackSize - 3);
				System.arraycopy(operandStack, 0, floatArrayStoreStack, 0, stackSize - 3);
				return new FrameNode(floatArrayStoreStack, localVariables, localsInitState, activeMonitors);
			case DASTORE:
				StackFrameType[] doubleArrayStoreStack = StackFrameType.createEmptyFrame(stackSize - 3);
				System.arraycopy(operandStack, 0, doubleArrayStoreStack, 0, stackSize - 3);
				return new FrameNode(doubleArrayStoreStack, localVariables, localsInitState, activeMonitors);
			case AASTORE:
				StackFrameType[] referenceArrayStoreStack = StackFrameType.createEmptyFrame(stackSize - 3);
				System.arraycopy(operandStack, 0, referenceArrayStoreStack, 0, stackSize - 3);
				return new FrameNode(referenceArrayStoreStack, localVariables, localsInitState, activeMonitors);
			case POP:
				if (stackSize - 1 < 0) {
					throw new MethodAnalyzerException("Method may be corrupt (A)", "Unknown");
				}

				StackFrameType[] popStack = StackFrameType.createEmptyFrame(stackSize - 1);
				System.arraycopy(operandStack, 0, popStack, 0, stackSize - 1);
				return new FrameNode(popStack, localVariables, localsInitState, activeMonitors);
			case POP2:
				if (stackSize - 1 < 0) {
					throw new MethodAnalyzerException("Method may be corrupt (B)", "Unknown");
				} else if (FrameNode.isCategory2Type(operandStack[stackSize - 1])) {
					StackFrameType[] pop2Category2Stack = StackFrameType.createEmptyFrame(stackSize - 1);
					System.arraycopy(operandStack, 0, pop2Category2Stack, 0, stackSize - 1);
					return new FrameNode(pop2Category2Stack, localVariables, localsInitState, activeMonitors);
				} else {
					if (stackSize - 2 < 0) {
						throw new MethodAnalyzerException("Method may be corrupt (C)", "Unknown");
					}

					StackFrameType[] pop2Category1Stack = StackFrameType.createEmptyFrame(stackSize - 2);
					System.arraycopy(operandStack, 0, pop2Category1Stack, 0, stackSize - 2);
					return new FrameNode(pop2Category1Stack, localVariables, localsInitState, activeMonitors);
				}
			case DUP:
				StackFrameType[] dupStack = StackFrameType.createEmptyFrame(stackSize + 1);
				System.arraycopy(operandStack, 0, dupStack, 0, stackSize);
				dupStack[stackSize] = operandStack[stackSize - 1];
				return new FrameNode(dupStack, localVariables, localsInitState, activeMonitors);
			case DUP_X1:
				StackFrameType[] dupX1Stack = StackFrameType.createEmptyFrame(stackSize + 1);
				int dupX1StackSize = dupX1Stack.length;
				System.arraycopy(operandStack, 0, dupX1Stack, 0, stackSize - 2);
				dupX1Stack[dupX1StackSize - 3] = operandStack[stackSize - 1];
				dupX1Stack[dupX1StackSize - 2] = operandStack[stackSize - 2];
				dupX1Stack[dupX1StackSize - 1] = operandStack[stackSize - 1];
				return new FrameNode(dupX1Stack, localVariables, localsInitState, activeMonitors);
			case DUP_X2:
				StackFrameType[] dupX2Stack = StackFrameType.createEmptyFrame(stackSize + 1);
				int dupX2StackSize = dupX2Stack.length;
				if (FrameNode.isCategory2Type(operandStack[stackSize - 2])) {
					System.arraycopy(operandStack, 0, dupX2Stack, 0, stackSize - 2);
					dupX2Stack[dupX2StackSize - 3] = operandStack[stackSize - 1];
					dupX2Stack[dupX2StackSize - 2] = operandStack[stackSize - 2];
					dupX2Stack[dupX2StackSize - 1] = operandStack[stackSize - 1];
					return new FrameNode(dupX2Stack, localVariables, localsInitState, activeMonitors);
				}

				System.arraycopy(operandStack, 0, dupX2Stack, 0, stackSize - 3);
				dupX2Stack[dupX2StackSize - 4] = operandStack[stackSize - 1];
				dupX2Stack[dupX2StackSize - 3] = operandStack[stackSize - 3];
				dupX2Stack[dupX2StackSize - 2] = operandStack[stackSize - 2];
				dupX2Stack[dupX2StackSize - 1] = operandStack[stackSize - 1];
				return new FrameNode(dupX2Stack, localVariables, localsInitState, activeMonitors);
			case DUP2:
				if (FrameNode.isCategory2Type(operandStack[stackSize - 1])) {
					StackFrameType[] dup2Category2Stack = StackFrameType.createEmptyFrame(stackSize + 1);
					int dup2Category2StackSize = dup2Category2Stack.length;
					System.arraycopy(operandStack, 0, dup2Category2Stack, 0, stackSize);
					dup2Category2Stack[dup2Category2StackSize - 1] = operandStack[stackSize - 1];
					return new FrameNode(dup2Category2Stack, localVariables, localsInitState, activeMonitors);
				}

				StackFrameType[] dup2Category1Stack = StackFrameType.createEmptyFrame(stackSize + 2);
				int dup2Category1StackSize = dup2Category1Stack.length;
				System.arraycopy(operandStack, 0, dup2Category1Stack, 0, stackSize);
				dup2Category1Stack[dup2Category1StackSize - 2] = operandStack[stackSize - 2];
				dup2Category1Stack[dup2Category1StackSize - 1] = operandStack[stackSize - 1];
				return new FrameNode(dup2Category1Stack, localVariables, localsInitState, activeMonitors);
			case DUP2_X1:
				if (FrameNode.isCategory2Type(operandStack[stackSize - 1])) {
					StackFrameType[] dup2X1Category2Stack = StackFrameType.createEmptyFrame(stackSize + 1);
					int dup2X1Category2StackSize = dup2X1Category2Stack.length;
					System.arraycopy(operandStack, 0, dup2X1Category2Stack, 0, stackSize - 2);
					dup2X1Category2Stack[dup2X1Category2StackSize - 3] = operandStack[stackSize - 1];
					dup2X1Category2Stack[dup2X1Category2StackSize - 2] = operandStack[stackSize - 2];
					dup2X1Category2Stack[dup2X1Category2StackSize - 1] = operandStack[stackSize - 1];
					return new FrameNode(dup2X1Category2Stack, localVariables, localsInitState, activeMonitors);
				}

				StackFrameType[] dup2X1Category1Stack = StackFrameType.createEmptyFrame(stackSize + 2);
				int dup2X1Category1StackSize = dup2X1Category1Stack.length;
				System.arraycopy(operandStack, 0, dup2X1Category1Stack, 0, stackSize - 3);
				dup2X1Category1Stack[dup2X1Category1StackSize - 5] = operandStack[stackSize - 2];
				dup2X1Category1Stack[dup2X1Category1StackSize - 4] = operandStack[stackSize - 1];
				dup2X1Category1Stack[dup2X1Category1StackSize - 3] = operandStack[stackSize - 3];
				dup2X1Category1Stack[dup2X1Category1StackSize - 2] = operandStack[stackSize - 2];
				dup2X1Category1Stack[dup2X1Category1StackSize - 1] = operandStack[stackSize - 1];
				return new FrameNode(dup2X1Category1Stack, localVariables, localsInitState, activeMonitors);
			case DUP2_X2:
				if (FrameNode.isCategory2Type(operandStack[stackSize - 1])) {
					if (FrameNode.isCategory2Type(operandStack[stackSize - 2])) {
						StackFrameType[] dup2X2BothCategory2Stack = StackFrameType.createEmptyFrame(stackSize + 1);
						int dup2X2BothCategory2StackSize = dup2X2BothCategory2Stack.length;
						System.arraycopy(operandStack, 0, dup2X2BothCategory2Stack, 0, stackSize - 2);
						dup2X2BothCategory2Stack[dup2X2BothCategory2StackSize - 3] = operandStack[stackSize - 1];
						dup2X2BothCategory2Stack[dup2X2BothCategory2StackSize - 2] = operandStack[stackSize - 2];
						dup2X2BothCategory2Stack[dup2X2BothCategory2StackSize - 1] = operandStack[stackSize - 1];
						return new FrameNode(dup2X2BothCategory2Stack, localVariables, localsInitState, activeMonitors);
					}

					StackFrameType[] dup2X2TopCategory2Stack = StackFrameType.createEmptyFrame(stackSize + 1);
					int dup2X2TopCategory2StackSize = dup2X2TopCategory2Stack.length;
					System.arraycopy(operandStack, 0, dup2X2TopCategory2Stack, 0, stackSize - 3);
					dup2X2TopCategory2Stack[dup2X2TopCategory2StackSize - 4] = operandStack[stackSize - 1];
					dup2X2TopCategory2Stack[dup2X2TopCategory2StackSize - 3] = operandStack[stackSize - 3];
					dup2X2TopCategory2Stack[dup2X2TopCategory2StackSize - 2] = operandStack[stackSize - 2];
					dup2X2TopCategory2Stack[dup2X2TopCategory2StackSize - 1] = operandStack[stackSize - 1];
					return new FrameNode(dup2X2TopCategory2Stack, localVariables, localsInitState, activeMonitors);
				} else {
					if (FrameNode.isCategory2Type(operandStack[stackSize - 3])) {
						StackFrameType[] dup2X2ThirdCategory2Stack = StackFrameType.createEmptyFrame(stackSize + 2);
						int dup2X2ThirdCategory2StackSize = dup2X2ThirdCategory2Stack.length;
						System.arraycopy(operandStack, 0, dup2X2ThirdCategory2Stack, 0, stackSize - 3);
						dup2X2ThirdCategory2Stack[dup2X2ThirdCategory2StackSize - 5] = operandStack[stackSize - 2];
						dup2X2ThirdCategory2Stack[dup2X2ThirdCategory2StackSize - 4] = operandStack[stackSize - 1];
						dup2X2ThirdCategory2Stack[dup2X2ThirdCategory2StackSize - 3] = operandStack[stackSize - 3];
						dup2X2ThirdCategory2Stack[dup2X2ThirdCategory2StackSize - 2] = operandStack[stackSize - 2];
						dup2X2ThirdCategory2Stack[dup2X2ThirdCategory2StackSize - 1] = operandStack[stackSize - 1];
						return new FrameNode(dup2X2ThirdCategory2Stack, localVariables, localsInitState, activeMonitors);
					}

					StackFrameType[] dup2X2AllCategory1Stack = StackFrameType.createEmptyFrame(stackSize + 2);
					int dup2X2AllCategory1StackSize = dup2X2AllCategory1Stack.length;
					System.arraycopy(operandStack, 0, dup2X2AllCategory1Stack, 0, stackSize - 4);
					dup2X2AllCategory1Stack[dup2X2AllCategory1StackSize - 6] = operandStack[stackSize - 2];
					dup2X2AllCategory1Stack[dup2X2AllCategory1StackSize - 5] = operandStack[stackSize - 1];
					dup2X2AllCategory1Stack[dup2X2AllCategory1StackSize - 4] = operandStack[stackSize - 4];
					dup2X2AllCategory1Stack[dup2X2AllCategory1StackSize - 3] = operandStack[stackSize - 3];
					dup2X2AllCategory1Stack[dup2X2AllCategory1StackSize - 2] = operandStack[stackSize - 2];
					dup2X2AllCategory1Stack[dup2X2AllCategory1StackSize - 1] = operandStack[stackSize - 1];
					return new FrameNode(dup2X2AllCategory1Stack, localVariables, localsInitState, activeMonitors);
				}
			case SWAP:
				StackFrameType[] swapStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, swapStack, 0, stackSize - 2);
				swapStack[stackSize - 2] = operandStack[stackSize - 1];
				swapStack[stackSize - 1] = operandStack[stackSize - 2];
				return new FrameNode(swapStack, localVariables, localsInitState, activeMonitors);
			case IADD:
			case ISUB:
			case IMUL:
			case IDIV:
			case IREM:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IOR:
			case IXOR:
				StackFrameType[] intArithStack = StackFrameType.createEmptyFrame(stackSize - 1);
				System.arraycopy(operandStack, 0, intArithStack, 0, stackSize - 2);
				intArithStack[stackSize - 2] = StackFrameType.INT;
				return new FrameNode(intArithStack, localVariables, localsInitState, activeMonitors);
			case LADD:
			case LSUB:
			case LMUL:
			case LDIV:
			case LREM:
			case LAND:
			case LOR:
			case LXOR:
				StackFrameType[] longArithStack = StackFrameType.createEmptyFrame(stackSize - 1);
				System.arraycopy(operandStack, 0, longArithStack, 0, stackSize - 2);
				longArithStack[stackSize - 2] = StackFrameType.LONG;
				return new FrameNode(longArithStack, localVariables, localsInitState, activeMonitors);
			case FADD:
			case FSUB:
			case FMUL:
			case FDIV:
			case FREM:
				StackFrameType[] floatArithStack = StackFrameType.createEmptyFrame(stackSize - 1);
				System.arraycopy(operandStack, 0, floatArithStack, 0, stackSize - 1);
				return new FrameNode(floatArithStack, localVariables, localsInitState, activeMonitors);
			case DADD:
			case DSUB:
			case DMUL:
			case DDIV:
			case DREM:
				StackFrameType[] doubleArithStack = StackFrameType.createEmptyFrame(stackSize - 1);
				System.arraycopy(operandStack, 0, doubleArithStack, 0, stackSize - 1);
				return new FrameNode(doubleArithStack, localVariables, localsInitState, activeMonitors);
			case INEG:
			case I2B:
			case I2C:
			case I2S:
				return new FrameNode(operandStack, localVariables, localsInitState, activeMonitors);
			case LNEG:
				return new FrameNode(operandStack, localVariables, localsInitState, activeMonitors);
			case FNEG:
				return new FrameNode(operandStack, localVariables, localsInitState, activeMonitors);
			case DNEG:
				return new FrameNode(operandStack, localVariables, localsInitState, activeMonitors);
			case LSHL:
			case LSHR:
			case LUSHR:
				StackFrameType[] longShiftStack = StackFrameType.createEmptyFrame(stackSize - 1);
				System.arraycopy(operandStack, 0, longShiftStack, 0, stackSize - 2);
				longShiftStack[stackSize - 2] = StackFrameType.LONG;
				return new FrameNode(longShiftStack, localVariables, localsInitState, activeMonitors);
			case I2L:
				StackFrameType[] i2lStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, i2lStack, 0, stackSize - 1);
				i2lStack[stackSize - 1] = StackFrameType.LONG;
				return new FrameNode(i2lStack, localVariables, localsInitState, activeMonitors);
			case I2F:
				StackFrameType[] i2fStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, i2fStack, 0, stackSize - 1);
				i2fStack[stackSize - 1] = StackFrameType.FLOAT;
				return new FrameNode(i2fStack, localVariables, localsInitState, activeMonitors);
			case I2D:
				StackFrameType[] i2dStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, i2dStack, 0, stackSize - 1);
				i2dStack[stackSize - 1] = StackFrameType.DOUBLE;
				return new FrameNode(i2dStack, localVariables, localsInitState, activeMonitors);
			case L2I:
				StackFrameType[] l2iStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, l2iStack, 0, stackSize - 1);
				l2iStack[stackSize - 1] = StackFrameType.INT;
				return new FrameNode(l2iStack, localVariables, localsInitState, activeMonitors);
			case L2F:
				StackFrameType[] l2fStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, l2fStack, 0, stackSize - 1);
				l2fStack[stackSize - 1] = StackFrameType.FLOAT;
				return new FrameNode(l2fStack, localVariables, localsInitState, activeMonitors);
			case L2D:
				StackFrameType[] l2dStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, l2dStack, 0, stackSize - 1);
				l2dStack[stackSize - 1] = StackFrameType.DOUBLE;
				return new FrameNode(l2dStack, localVariables, localsInitState, activeMonitors);
			case F2I:
				StackFrameType[] f2iStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, f2iStack, 0, stackSize - 1);
				f2iStack[stackSize - 1] = StackFrameType.INT;
				return new FrameNode(f2iStack, localVariables, localsInitState, activeMonitors);
			case F2L:
				StackFrameType[] f2lStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, f2lStack, 0, stackSize - 1);
				f2lStack[stackSize - 1] = StackFrameType.LONG;
				return new FrameNode(f2lStack, localVariables, localsInitState, activeMonitors);
			case F2D:
				StackFrameType[] f2dStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, f2dStack, 0, stackSize - 1);
				f2dStack[stackSize - 1] = StackFrameType.DOUBLE;
				return new FrameNode(f2dStack, localVariables, localsInitState, activeMonitors);
			case D2I:
				StackFrameType[] d2iStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, d2iStack, 0, stackSize - 1);
				d2iStack[stackSize - 1] = StackFrameType.INT;
				return new FrameNode(d2iStack, localVariables, localsInitState, activeMonitors);
			case D2L:
				StackFrameType[] d2lStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, d2lStack, 0, stackSize - 1);
				d2lStack[stackSize - 1] = StackFrameType.LONG;
				return new FrameNode(d2lStack, localVariables, localsInitState, activeMonitors);
			case D2F:
				StackFrameType[] d2fStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, d2fStack, 0, stackSize - 1);
				d2fStack[stackSize - 1] = StackFrameType.FLOAT;
				return new FrameNode(d2fStack, localVariables, localsInitState, activeMonitors);
			case LCMP:
				StackFrameType[] lcmpStack = StackFrameType.createEmptyFrame(stackSize - 1);
				int lcmpStackSize = lcmpStack.length;
				System.arraycopy(operandStack, 0, lcmpStack, 0, stackSize - 2);
				lcmpStack[lcmpStackSize - 1] = StackFrameType.INT;
				return new FrameNode(lcmpStack, localVariables, localsInitState, activeMonitors);
			case FCMPL:
			case FCMPG:
				StackFrameType[] fcmpStack = StackFrameType.createEmptyFrame(stackSize - 1);
				int fcmpStackSize = fcmpStack.length;
				System.arraycopy(operandStack, 0, fcmpStack, 0, stackSize - 2);
				fcmpStack[fcmpStackSize - 1] = StackFrameType.INT;
				return new FrameNode(fcmpStack, localVariables, localsInitState, activeMonitors);
			case DCMPL:
			case DCMPG:
				StackFrameType[] dcmpStack = StackFrameType.createEmptyFrame(stackSize - 1);
				int dcmpStackSize = dcmpStack.length;
				System.arraycopy(operandStack, 0, dcmpStack, 0, stackSize - 2);
				dcmpStack[dcmpStackSize - 1] = StackFrameType.INT;
				return new FrameNode(dcmpStack, localVariables, localsInitState, activeMonitors);
			case IRETURN:
				return new FrameNode(localVariables, localsInitState, activeMonitors);
			case LRETURN:
				return new FrameNode(localVariables, localsInitState, activeMonitors);
			case FRETURN:
				return new FrameNode(localVariables, localsInitState, activeMonitors);
			case DRETURN:
				return new FrameNode(localVariables, localsInitState, activeMonitors);
			case ARETURN:
				return new FrameNode(localVariables, localsInitState, activeMonitors);
			case RETURN:
				return new FrameNode(localVariables, localsInitState, activeMonitors);
			case ARRAYLENGTH:
				StackFrameType[] arrayLengthStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(operandStack, 0, arrayLengthStack, 0, stackSize - 1);
				arrayLengthStack[stackSize - 1] = StackFrameType.INT;
				return new FrameNode(arrayLengthStack, localVariables, localsInitState, activeMonitors);
			case ATHROW:
				return new FrameNode(operandStack, localVariables, localsInitState, activeMonitors);
		}
	}

	@Override
	public boolean isLConst() {
		switch (this.opcode) {
			case LCONST_0:
			case LCONST_1:
				return true;
			default:
				return false;
		}
	}

	@Override
	public int[] mapOldStackIndexToNew(StackFrameType[] oldStack, StackFrameType[] newStack, int oldStackIndex) {
		int oldStackTop = oldStack.length - 1;
		int newStackTop = newStack.length - 1;
		switch (this.opcode) {
			case POP:
			case POP2:
				if (oldStackIndex > newStackTop) {
					return new int[0];
				}
				break;
			case DUP:
				if (oldStackIndex == oldStackTop) {
					return new int[]{oldStackTop, oldStackTop + 1};
				}
				break;
			case DUP_X1:
				if (oldStackIndex == oldStackTop) {
					return new int[]{oldStackTop - 1, oldStackTop + 1};
				}

				if (oldStackIndex == oldStackTop - 1) {
					return new int[]{oldStackTop};
				}
				break;
			case DUP_X2:
				if (oldStack[oldStackTop - 1].isCategory2()) {
					if (oldStackIndex == oldStackTop) {
						return new int[]{oldStackTop - 1, oldStackTop + 1};
					}

					if (oldStackIndex == oldStackTop - 1) {
						return new int[]{oldStackTop};
					}
				} else {
					if (oldStackIndex == oldStackTop) {
						return new int[]{oldStackTop - 2, oldStackTop + 1};
					}

					if (oldStackIndex == oldStackTop - 1 || oldStackIndex == oldStackTop - 2) {
						return new int[]{oldStackIndex + 1};
					}
				}
				break;
			case DUP2:
				if (oldStack[oldStackTop].isCategory2()) {
					if (oldStackIndex == oldStackTop) {
						return new int[]{oldStackIndex, oldStackIndex + 1};
					}
				} else if (oldStackIndex == oldStackTop || oldStackIndex == oldStackTop - 1) {
					return new int[]{oldStackIndex, oldStackIndex + 2};
				}
				break;
			case DUP2_X1:
				if (oldStack[oldStackTop].isCategory2()) {
					if (oldStackIndex == oldStackTop) {
						return new int[]{oldStackIndex - 1, oldStackIndex + 1};
					}

					if (oldStackIndex == oldStackTop - 1) {
						return new int[]{oldStackIndex + 1};
					}
				} else {
					if (oldStackIndex == oldStackTop || oldStackIndex == oldStackTop - 1) {
						return new int[]{oldStackIndex - 1, oldStackIndex + 2};
					}

					if (oldStackIndex == oldStackTop - 2) {
						return new int[]{oldStackIndex + 2};
					}
				}
				break;
			case DUP2_X2:
				if (oldStack[oldStackTop].isCategory2()) {
					if (oldStack[oldStackTop - 1].isCategory2()) {
						if (oldStackIndex == oldStackTop) {
							return new int[]{oldStackIndex - 1, oldStackIndex + 1};
						}

						if (oldStackIndex == oldStackTop - 1) {
							return new int[]{oldStackIndex + 1};
						}
					} else {
						if (oldStackIndex == oldStackTop) {
							return new int[]{oldStackIndex - 2, oldStackIndex + 1};
						}

						if (oldStackIndex == oldStackTop - 1 || oldStackIndex == oldStackTop - 2) {
							return new int[]{oldStackIndex + 1};
						}
					}
				} else if (oldStack[oldStackTop - 2].isCategory2()) {
					if (oldStackIndex == oldStackTop || oldStackIndex == oldStackTop - 1) {
						return new int[]{oldStackIndex - 1, oldStackIndex + 2};
					}

					if (oldStackIndex == oldStackTop - 2) {
						return new int[]{oldStackTop};
					}
				} else {
					if (oldStackIndex == oldStackTop || oldStackIndex == oldStackTop - 1) {
						return new int[]{oldStackIndex - 2, oldStackIndex + 2};
					}

					if (oldStackIndex == oldStackTop - 2 || oldStackIndex == oldStackTop - 3) {
						return new int[]{oldStackIndex + 2};
					}
				}
				break;
			case SWAP:
				if (oldStackIndex == oldStackTop) {
					return new int[]{oldStackTop - 1};
				}

				if (oldStackIndex == oldStackTop - 1) {
					return new int[]{oldStackTop};
				}
				break;
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return null;
		}

		return new int[]{oldStackIndex};
	}

	@Override
	public boolean isStackProducer() {
		switch (this.opcode) {
			case NOP:
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
			case POP:
			case POP2:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case ATHROW:
				return false;
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
			case IALOAD:
			case LALOAD:
			case FALOAD:
			case DALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case IADD:
			case LADD:
			case FADD:
			case DADD:
			case ISUB:
			case LSUB:
			case FSUB:
			case DSUB:
			case IMUL:
			case LMUL:
			case FMUL:
			case DMUL:
			case IDIV:
			case LDIV:
			case FDIV:
			case DDIV:
			case IREM:
			case LREM:
			case FREM:
			case DREM:
			case INEG:
			case LNEG:
			case FNEG:
			case DNEG:
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
			case IAND:
			case LAND:
			case IOR:
			case LOR:
			case IXOR:
			case LXOR:
			case I2L:
			case I2F:
			case I2D:
			case L2I:
			case L2F:
			case L2D:
			case F2I:
			case F2L:
			case F2D:
			case D2I:
			case D2L:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case ARRAYLENGTH:
				return true;
			case BIPUSH:
			case SIPUSH:
			case LDC:
			case LDC_W:
			case LDC2_W:
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IINC:
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
			case JSR:
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return false;
		}
	}

	@Override
	public final boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		switch (this.opcode) {
			case NOP:
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
			case DLOAD_0:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
				return false;
			case BIPUSH:
			case SIPUSH:
			case LDC:
			case LDC_W:
			case LDC2_W:
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IINC:
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
			case JSR:
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return false;
			case IALOAD:
			case FALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case IADD:
			case FADD:
			case ISUB:
			case FSUB:
			case IMUL:
			case FMUL:
			case IDIV:
			case FDIV:
			case IREM:
			case FREM:
			case INEG:
			case FNEG:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IOR:
			case IXOR:
			case I2F:
			case L2I:
			case L2F:
			case F2I:
			case D2I:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case ARRAYLENGTH:
				return stackIndex >= stackSize - 1;
			case LALOAD:
			case DALOAD:
			case LADD:
			case DADD:
			case LSUB:
			case DSUB:
			case LMUL:
			case DMUL:
			case LDIV:
			case DDIV:
			case LREM:
			case DREM:
			case LNEG:
			case DNEG:
			case LSHL:
			case LSHR:
			case LUSHR:
			case LAND:
			case LOR:
			case LXOR:
			case I2L:
			case I2D:
			case L2D:
			case F2L:
			case F2D:
			case D2L:
				return stackIndex >= stackSize - 2;
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				return stackIndex >= stackSize;
			case POP:
			case POP2:
				return stackIndex >= stackSize;
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case ATHROW:
				return true;
			case RETURN:
				return false;
		}
	}

	private FrameNode createPushedValueFrame(
		int stackSize, StackFrameType[] operandStack, StackFrameType[] localVariables, StackFrameType valueType, LocalsInitializationState localsInitState, Set<MonitorEnterNode> activeMonitors
	) {
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
		System.arraycopy(operandStack, 0, newOperandStack, 0, stackSize);
		newOperandStack[stackSize] = valueType;
		return new FrameNode(newOperandStack, localVariables, localsInitState, activeMonitors);
	}

	@Override
	public final int getStackIndexOrigin(StackFrameType[] stackBefore, int newStackIndex) {
		int topIndex = stackBefore.length - 1;
		switch (this.opcode) {
			case POP:
			case POP2:
				return newStackIndex;
			case DUP:
				if (newStackIndex == topIndex) {
					return newStackIndex - 1;
				}

				return newStackIndex;
			case DUP_X1:
				if (newStackIndex != topIndex && newStackIndex != topIndex - 1) {
					if (newStackIndex == topIndex - 2) {
						return newStackIndex + 1;
					}

					return newStackIndex;
				}

				return newStackIndex - 1;
			case DUP_X2:
				if (stackBefore[topIndex - 1].isCategory2()) {
					if (newStackIndex != topIndex && newStackIndex != topIndex - 1) {
						if (newStackIndex == topIndex - 2) {
							return newStackIndex + 1;
						}

						return newStackIndex;
					}

					return newStackIndex - 1;
				} else {
					if (newStackIndex != topIndex && newStackIndex != topIndex - 1 && newStackIndex != topIndex - 2) {
						if (newStackIndex == topIndex - 3) {
							return newStackIndex + 2;
						}

						return newStackIndex;
					}

					return newStackIndex - 1;
				}
			case DUP2:
				if (stackBefore[topIndex].isCategory2()) {
					if (newStackIndex == topIndex) {
						return newStackIndex - 1;
					}

					return newStackIndex;
				} else {
					if (newStackIndex != topIndex && newStackIndex != topIndex - 1) {
						return newStackIndex;
					}

					return newStackIndex - 2;
				}
			case DUP2_X1:
				if (stackBefore[topIndex].isCategory2()) {
					if (newStackIndex != topIndex && newStackIndex != topIndex - 1) {
						if (newStackIndex == topIndex - 2) {
							return newStackIndex + 1;
						}

						return newStackIndex;
					}

					return newStackIndex - 1;
				} else {
					if (newStackIndex != topIndex && newStackIndex != topIndex - 1 && newStackIndex != topIndex - 2) {
						if (newStackIndex != topIndex - 3 && newStackIndex != topIndex - 4) {
							return newStackIndex;
						}

						return newStackIndex + 1;
					}

					return newStackIndex - 2;
				}
			case DUP2_X2:
				if (stackBefore[topIndex].isCategory2()) {
					if (stackBefore[topIndex - 1].isCategory2()) {
						if (newStackIndex != topIndex && newStackIndex != topIndex - 1) {
							if (newStackIndex == topIndex - 2) {
								return newStackIndex + 1;
							}

							return newStackIndex;
						}

						return newStackIndex - 1;
					} else {
						if (newStackIndex != topIndex && newStackIndex != topIndex - 1 && newStackIndex != topIndex - 2) {
							if (newStackIndex == topIndex - 3) {
								return newStackIndex + 2;
							}

							return newStackIndex;
						}

						return newStackIndex - 1;
					}
				} else if (stackBefore[topIndex - 2].isCategory2()) {
					if (newStackIndex != topIndex && newStackIndex != topIndex - 1 && newStackIndex != topIndex - 2) {
						if (newStackIndex != topIndex - 3 && newStackIndex != topIndex - 4) {
							return newStackIndex;
						}

						return newStackIndex + 1;
					}

					return newStackIndex - 2;
				} else {
					if (newStackIndex != topIndex && newStackIndex != topIndex - 1 && newStackIndex != topIndex - 2 && newStackIndex != topIndex - 3) {
						if (newStackIndex != topIndex - 4 && newStackIndex != topIndex - 5) {
							return newStackIndex;
						}

						return newStackIndex + 2;
					}

					return newStackIndex - 2;
				}
			case SWAP:
				if (newStackIndex == topIndex) {
					return topIndex - 1;
				} else {
					if (newStackIndex == topIndex - 1) {
						return topIndex;
					}

					return newStackIndex;
				}
			default:
				DebugUtils.assertTrue(false, new String[]{this.opcode + " " + this.getOpcodeMnemonics()});
				return newStackIndex;
		}
	}

	@Override
	public boolean isCategory2Producer() {
		switch (this.opcode) {
			case NOP:
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case IALOAD:
			case FALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
			case POP:
			case POP2:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
			case IADD:
			case FADD:
			case ISUB:
			case FSUB:
			case IMUL:
			case FMUL:
			case IDIV:
			case FDIV:
			case IREM:
			case FREM:
			case INEG:
			case FNEG:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IOR:
			case IXOR:
			case I2F:
			case L2I:
			case L2F:
			case F2I:
			case D2I:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case ARRAYLENGTH:
			case ATHROW:
				return false;
			case LCONST_0:
			case LCONST_1:
			case DCONST_0:
			case DCONST_1:
			case LALOAD:
			case DALOAD:
			case LADD:
			case DADD:
			case LSUB:
			case DSUB:
			case LMUL:
			case DMUL:
			case LDIV:
			case DDIV:
			case LREM:
			case DREM:
			case LNEG:
			case DNEG:
			case LSHL:
			case LSHR:
			case LUSHR:
			case LAND:
			case LOR:
			case LXOR:
			case I2L:
			case I2D:
			case L2D:
			case F2L:
			case F2D:
			case D2L:
				return true;
			case BIPUSH:
			case SIPUSH:
			case LDC:
			case LDC_W:
			case LDC2_W:
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IINC:
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
			case JSR:
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return false;
		}
	}

	private FrameNode createArrayLoadFrame(
		int stackSize, StackFrameType[] operandStack, StackFrameType[] localVariables, StackFrameType arrayType, StackFrameType elementType, LocalsInitializationState localsInitState, Set<MonitorEnterNode> activeMonitors
	) {
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(stackSize - 1);
		System.arraycopy(operandStack, 0, newOperandStack, 0, stackSize - 2);
		newOperandStack[stackSize - 2] = elementType;
		return new FrameNode(newOperandStack, localVariables, localsInitState, activeMonitors);
	}

	@Override
	public boolean isStandaloneStackProducer() {
		switch (this.opcode) {
			case NOP:
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
			case POP:
			case POP2:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case ATHROW:
				return false;
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
				return true;
			case BIPUSH:
			case SIPUSH:
			case LDC:
			case LDC_W:
			case LDC2_W:
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IINC:
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
			case JSR:
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return false;
			case IALOAD:
			case LALOAD:
			case FALOAD:
			case DALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case IADD:
			case LADD:
			case FADD:
			case DADD:
			case ISUB:
			case LSUB:
			case FSUB:
			case DSUB:
			case IMUL:
			case LMUL:
			case FMUL:
			case DMUL:
			case IDIV:
			case LDIV:
			case FDIV:
			case DDIV:
			case IREM:
			case LREM:
			case FREM:
			case DREM:
			case INEG:
			case LNEG:
			case FNEG:
			case DNEG:
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
			case IAND:
			case LAND:
			case IOR:
			case LOR:
			case IXOR:
			case LXOR:
			case I2L:
			case I2F:
			case I2D:
			case L2I:
			case L2F:
			case L2D:
			case F2I:
			case F2L:
			case F2D:
			case D2I:
			case D2L:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case ARRAYLENGTH:
				return false;
		}
	}

	@Override
	public boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		switch (this.opcode) {
			case NOP:
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
				return false;
			case BIPUSH:
			case SIPUSH:
			case LDC:
			case LDC_W:
			case LDC2_W:
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IINC:
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
			case JSR:
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return false;
			case IALOAD:
			case FALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case IADD:
			case FADD:
			case ISUB:
			case FSUB:
			case IMUL:
			case FMUL:
			case IDIV:
			case FDIV:
			case IREM:
			case FREM:
			case INEG:
			case FNEG:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IOR:
			case IXOR:
			case I2F:
			case L2I:
			case L2F:
			case F2I:
			case D2I:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case ARRAYLENGTH:
				return stackIndex >= stackSize - 1;
			case LALOAD:
			case DALOAD:
			case LADD:
			case DADD:
			case LSUB:
			case DSUB:
			case LMUL:
			case DMUL:
			case LDIV:
			case DDIV:
			case LREM:
			case DREM:
			case LNEG:
			case DNEG:
			case LSHL:
			case LSHR:
			case LUSHR:
			case LAND:
			case LOR:
			case LXOR:
			case I2L:
			case I2D:
			case L2D:
			case F2L:
			case F2D:
			case D2L:
				return stackIndex >= stackSize - 2;
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
			case POP:
			case POP2:
				return stackIndex >= stackSize;
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case ATHROW:
				return true;
		}
	}

	@Override
	public final boolean canFallThrough() {
		switch (this.opcode) {
			case NOP:
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
			case IALOAD:
			case LALOAD:
			case FALOAD:
			case DALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
			case POP:
			case POP2:
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
			case IADD:
			case LADD:
			case FADD:
			case DADD:
			case ISUB:
			case LSUB:
			case FSUB:
			case DSUB:
			case IMUL:
			case LMUL:
			case FMUL:
			case DMUL:
			case IDIV:
			case LDIV:
			case FDIV:
			case DDIV:
			case IREM:
			case LREM:
			case FREM:
			case DREM:
			case INEG:
			case LNEG:
			case FNEG:
			case DNEG:
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
			case IAND:
			case LAND:
			case IOR:
			case LOR:
			case IXOR:
			case LXOR:
			case I2L:
			case I2F:
			case I2D:
			case L2I:
			case L2F:
			case L2D:
			case F2I:
			case F2L:
			case F2D:
			case D2I:
			case D2L:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
			case ARRAYLENGTH:
				return true;
			case BIPUSH:
			case SIPUSH:
			case LDC:
			case LDC_W:
			case LDC2_W:
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
			case IINC:
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
			case JSR:
			case RET:
			case TABLESWITCH:
			case LOOKUPSWITCH:
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
			default:
				DebugUtils.assertTrue(false, this.opcode);
				return true;
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case ATHROW:
				return false;
		}
	}

	@Override
	public String getOpcodeText() {
		return this.getOpcodeMnemonics();
	}

	@Override
	public boolean isReturn() {
		switch (this.opcode) {
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
				return true;
			default:
				return false;
		}
	}

	static {
		opcodeCache[NOP] = new InsnNode(NOP);
		opcodeCache[ACONST_NULL] = new InsnNode(ACONST_NULL);
		opcodeCache[ICONST_M1] = new InsnNode(ICONST_M1);
		opcodeCache[ICONST_0] = new InsnNode(ICONST_0);
		opcodeCache[ICONST_1] = new InsnNode(ICONST_1);
		opcodeCache[ICONST_2] = new InsnNode(ICONST_2);
		opcodeCache[ICONST_3] = new InsnNode(ICONST_3);
		opcodeCache[ICONST_4] = new InsnNode(ICONST_4);
		opcodeCache[ICONST_5] = new InsnNode(ICONST_5);
		opcodeCache[LCONST_0] = new InsnNode(LCONST_0);
		opcodeCache[LCONST_1] = new InsnNode(LCONST_1);
		opcodeCache[FCONST_0] = new InsnNode(FCONST_0);
		opcodeCache[FCONST_1] = new InsnNode(FCONST_1);
		opcodeCache[FCONST_2] = new InsnNode(FCONST_2);
		opcodeCache[DCONST_0] = new InsnNode(DCONST_0);
		opcodeCache[DCONST_1] = new InsnNode(DCONST_1);
		opcodeCache[IALOAD] = new InsnNode(IALOAD);
		opcodeCache[LALOAD] = new InsnNode(LALOAD);
		opcodeCache[FALOAD] = new InsnNode(FALOAD);
		opcodeCache[DALOAD] = new InsnNode(DALOAD);
		opcodeCache[AALOAD] = new InsnNode(AALOAD);
		opcodeCache[BALOAD] = new InsnNode(BALOAD);
		opcodeCache[CALOAD] = new InsnNode(CALOAD);
		opcodeCache[SALOAD] = new InsnNode(SALOAD);
		opcodeCache[IASTORE] = new InsnNode(IASTORE);
		opcodeCache[LASTORE] = new InsnNode(LASTORE);
		opcodeCache[FASTORE] = new InsnNode(FASTORE);
		opcodeCache[DASTORE] = new InsnNode(DASTORE);
		opcodeCache[AASTORE] = new InsnNode(AASTORE);
		opcodeCache[BASTORE] = new InsnNode(BASTORE);
		opcodeCache[CASTORE] = new InsnNode(CASTORE);
		opcodeCache[SASTORE] = new InsnNode(SASTORE);
		opcodeCache[POP] = new InsnNode(POP);
		opcodeCache[POP2] = new InsnNode(POP2);
		opcodeCache[DUP] = new InsnNode(DUP);
		opcodeCache[DUP_X1] = new InsnNode(DUP_X1);
		opcodeCache[DUP_X2] = new InsnNode(DUP_X2);
		opcodeCache[DUP2] = new InsnNode(DUP2);
		opcodeCache[DUP2_X1] = new InsnNode(DUP2_X1);
		opcodeCache[DUP2_X2] = new InsnNode(DUP2_X2);
		opcodeCache[SWAP] = new InsnNode(SWAP);
		opcodeCache[IADD] = new InsnNode(IADD);
		opcodeCache[LADD] = new InsnNode(LADD);
		opcodeCache[FADD] = new InsnNode(FADD);
		opcodeCache[DADD] = new InsnNode(DADD);
		opcodeCache[ISUB] = new InsnNode(ISUB);
		opcodeCache[LSUB] = new InsnNode(LSUB);
		opcodeCache[FSUB] = new InsnNode(FSUB);
		opcodeCache[DSUB] = new InsnNode(DSUB);
		opcodeCache[IMUL] = new InsnNode(IMUL);
		opcodeCache[LMUL] = new InsnNode(LMUL);
		opcodeCache[FMUL] = new InsnNode(FMUL);
		opcodeCache[DMUL] = new InsnNode(DMUL);
		opcodeCache[IDIV] = new InsnNode(IDIV);
		opcodeCache[LDIV] = new InsnNode(LDIV);
		opcodeCache[FDIV] = new InsnNode(FDIV);
		opcodeCache[DDIV] = new InsnNode(DDIV);
		opcodeCache[IREM] = new InsnNode(IREM);
		opcodeCache[LREM] = new InsnNode(LREM);
		opcodeCache[FREM] = new InsnNode(FREM);
		opcodeCache[DREM] = new InsnNode(DREM);
		opcodeCache[INEG] = new InsnNode(INEG);
		opcodeCache[LNEG] = new InsnNode(LNEG);
		opcodeCache[FNEG] = new InsnNode(FNEG);
		opcodeCache[DNEG] = new InsnNode(DNEG);
		opcodeCache[ISHL] = new InsnNode(ISHL);
		opcodeCache[LSHL] = new InsnNode(LSHL);
		opcodeCache[ISHR] = new InsnNode(ISHR);
		opcodeCache[LSHR] = new InsnNode(LSHR);
		opcodeCache[IUSHR] = new InsnNode(IUSHR);
		opcodeCache[LUSHR] = new InsnNode(LUSHR);
		opcodeCache[IAND] = new InsnNode(IAND);
		opcodeCache[LAND] = new InsnNode(LAND);
		opcodeCache[IOR] = new InsnNode(IOR);
		opcodeCache[LOR] = new InsnNode(LOR);
		opcodeCache[IXOR] = new InsnNode(IXOR);
		opcodeCache[LXOR] = new InsnNode(LXOR);
		opcodeCache[I2L] = new InsnNode(I2L);
		opcodeCache[I2F] = new InsnNode(I2F);
		opcodeCache[I2D] = new InsnNode(I2D);
		opcodeCache[L2I] = new InsnNode(L2I);
		opcodeCache[L2F] = new InsnNode(L2F);
		opcodeCache[L2D] = new InsnNode(L2D);
		opcodeCache[F2I] = new InsnNode(F2I);
		opcodeCache[F2L] = new InsnNode(F2L);
		opcodeCache[F2D] = new InsnNode(F2D);
		opcodeCache[D2I] = new InsnNode(D2I);
		opcodeCache[D2L] = new InsnNode(D2L);
		opcodeCache[D2F] = new InsnNode(D2F);
		opcodeCache[I2B] = new InsnNode(I2B);
		opcodeCache[I2C] = new InsnNode(I2C);
		opcodeCache[I2S] = new InsnNode(I2S);
		opcodeCache[LCMP] = new InsnNode(LCMP);
		opcodeCache[FCMPL] = new InsnNode(FCMPL);
		opcodeCache[FCMPG] = new InsnNode(FCMPG);
		opcodeCache[DCMPL] = new InsnNode(DCMPL);
		opcodeCache[DCMPG] = new InsnNode(DCMPG);
		opcodeCache[IRETURN] = new InsnNode(IRETURN);
		opcodeCache[LRETURN] = new InsnNode(LRETURN);
		opcodeCache[FRETURN] = new InsnNode(FRETURN);
		opcodeCache[DRETURN] = new InsnNode(DRETURN);
		opcodeCache[ARETURN] = new InsnNode(ARETURN);
		opcodeCache[RETURN] = new InsnNode(RETURN);
		opcodeCache[ARRAYLENGTH] = new InsnNode(ARRAYLENGTH);
		opcodeCache[ATHROW] = new InsnNode(ATHROW);
	}
}

package com.zelix.framework.instructions;

import com.zelix.framework.analyzer.AnalyzeStackDebugContext;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.LocalsInitializationState;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.variable.LocalVarOperationType;
import com.zelix.framework.variable.LocalVariableSlot;
import com.zelix.framework.variable.LocalVariablesManager;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class VarInsnNode extends AbstractInsnNode {
	protected final LocalVariableSlot targetVariable;
	protected final LocalVarOperationType operationType;
	private final int incrementAmount;

	public VarInsnNode(int localVarIndex, LocalVarOperationType operationType, LocalVariablesManager localVariablesManager, int flags) {
		this(localVarIndex, operationType, localVariablesManager, 0, flags);
	}

	public VarInsnNode(LocalVariableSlot localVariableSlot, LocalVarOperationType operationType) {
		super(255);
		this.targetVariable = localVariableSlot;
		this.operationType = operationType;
		this.incrementAmount = 0;
	}

	public VarInsnNode(int opcode, int wideWrapOpcode, int localVarIndex, ByteDataBuffer dataBuffer, LocalVariablesManager localVariablesManager) throws IOException {
		super(255);
		if (wideWrapOpcode == IINC) {
			if (opcode == WIDE) {
				this.incrementAmount = dataBuffer.readShort();
			} else {
				this.incrementAmount = dataBuffer.readByte();
			}
		} else {
			this.incrementAmount = 0;
		}

		this.operationType = mapOpcodeToOperationType(wideWrapOpcode);
		this.targetVariable = localVariablesManager.getOrCreateVariable(localVarIndex, this.operationType, 0);
	}

	public VarInsnNode(int localVarIndex, LocalVarOperationType operationType, LocalVariablesManager localVariablesManager, int incrementAmount, int flags) {
		super(255);
		this.operationType = operationType;
		this.targetVariable = localVariablesManager.getOrCreateVariable(localVarIndex, operationType, flags);
		this.incrementAmount = incrementAmount;
	}

	@Override
	public int getStackEffect() {
		switch (this.operationType) {
			case INT_LOAD:
			case FLOAT_LOAD:
			case OBJECT_LOAD:
				return 1;
			case LONG_LOAD:
			case DOUBLE_LOAD:
				return 2;
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
				return -1;
			case LONG_STORE:
			case DOUBLE_STORE:
				return -2;
			default:
				return 0;
		}
	}

	private static LocalVarOperationType getDescriptorOperationType(String typeDescriptor, boolean isLoad) {
		if (typeDescriptor.length() == 1) {
			switch (typeDescriptor.charAt(0)) {
				case 'B':
				case 'C':
				case 'I':
				case 'S':
				case 'Z':
					if (isLoad) {
						return LocalVarOperationType.INT_LOAD;
					}

					return LocalVarOperationType.INT_STORE;
				case 'D':
					if (isLoad) {
						return LocalVarOperationType.DOUBLE_LOAD;
					}

					return LocalVarOperationType.DOUBLE_STORE;
				case 'E':
				case 'G':
				case 'H':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				default:
					return null;
				case 'F':
					if (isLoad) {
						return LocalVarOperationType.FLOAT_LOAD;
					}

					return LocalVarOperationType.FLOAT_STORE;
				case 'J':
					return isLoad ? LocalVarOperationType.LONG_LOAD : LocalVarOperationType.LONG_STORE;
			}
		} else {
			return isLoad ? LocalVarOperationType.OBJECT_LOAD : LocalVarOperationType.OBJECT_STORE;
		}
	}

	public static VarInsnNode readFromBuffer(int opcode, ByteDataBuffer dataBuffer, LocalVariablesManager localVariablesManager) throws IOException {
		int wideWrapOpcode;
		if (opcode == WIDE) {
			wideWrapOpcode = dataBuffer.read();
		} else {
			wideWrapOpcode = opcode;
		}

		int variableIndex;
		switch (wideWrapOpcode) {
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case IINC:
			case RET:
				if (opcode == WIDE) {
					variableIndex = dataBuffer.readUnsignedShort();
				} else {
					variableIndex = dataBuffer.readUnsignedByte();
				}
				break;
			case ILOAD_0:
			case LLOAD_0:
			case FLOAD_0:
			case DLOAD_0:
			case ALOAD_0:
			case ISTORE_0:
			case LSTORE_0:
			case FSTORE_0:
			case DSTORE_0:
			case ASTORE_0:
				variableIndex = 0;
				break;
			case ILOAD_1:
			case LLOAD_1:
			case FLOAD_1:
			case DLOAD_1:
			case ALOAD_1:
			case ISTORE_1:
			case LSTORE_1:
			case FSTORE_1:
			case DSTORE_1:
			case ASTORE_1:
				variableIndex = 1;
				break;
			case ILOAD_2:
			case LLOAD_2:
			case FLOAD_2:
			case DLOAD_2:
			case ALOAD_2:
			case ISTORE_2:
			case LSTORE_2:
			case FSTORE_2:
			case DSTORE_2:
			case ASTORE_2:
				variableIndex = 2;
				break;
			case ILOAD_3:
			case LLOAD_3:
			case FLOAD_3:
			case DLOAD_3:
			case ALOAD_3:
			case ISTORE_3:
			case LSTORE_3:
			case FSTORE_3:
			case DSTORE_3:
			case ASTORE_3:
				variableIndex = 3;
				break;
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
			default:
				variableIndex = -1;
		}

		return (VarInsnNode)(variableIndex > 3
				|| wideWrapOpcode != ILOAD
					&& wideWrapOpcode != ISTORE
					&& wideWrapOpcode != LLOAD
					&& wideWrapOpcode != LSTORE
					&& wideWrapOpcode != FLOAD
					&& wideWrapOpcode != FSTORE
					&& wideWrapOpcode != DLOAD
					&& wideWrapOpcode != DSTORE
					&& wideWrapOpcode != ALOAD
					&& wideWrapOpcode != ASTORE
			? new VarInsnNode(opcode, wideWrapOpcode, variableIndex, dataBuffer, localVariablesManager)
			: new AutoVarInsnNode(opcode, wideWrapOpcode, variableIndex, dataBuffer, localVariablesManager));
	}

	public static VarInsnNode createByTypeDescriptor(String typeDescriptor, boolean isLoad, LocalVariableSlot localVariableSlot) {
		LocalVarOperationType operationType = getDescriptorOperationType(typeDescriptor, isLoad);
		return new VarInsnNode(localVariableSlot, operationType);
	}

	private static LocalVarOperationType mapOpcodeToOperationType(int wideWrapOpcode) {
		switch (wideWrapOpcode) {
			case ILOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
				return LocalVarOperationType.INT_LOAD;
			case LLOAD:
			case LLOAD_0:
			case LLOAD_1:
			case LLOAD_2:
			case LLOAD_3:
				return LocalVarOperationType.LONG_LOAD;
			case FLOAD:
			case FLOAD_0:
			case FLOAD_1:
			case FLOAD_2:
			case FLOAD_3:
				return LocalVarOperationType.FLOAT_LOAD;
			case DLOAD:
			case DLOAD_0:
			case DLOAD_1:
			case DLOAD_2:
			case DLOAD_3:
				return LocalVarOperationType.DOUBLE_LOAD;
			case ALOAD:
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
				return LocalVarOperationType.OBJECT_LOAD;
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
			default:
				return null;
			case ISTORE:
			case ISTORE_0:
			case ISTORE_1:
			case ISTORE_2:
			case ISTORE_3:
				return LocalVarOperationType.INT_STORE;
			case LSTORE:
			case LSTORE_0:
			case LSTORE_1:
			case LSTORE_2:
			case LSTORE_3:
				return LocalVarOperationType.LONG_STORE;
			case FSTORE:
			case FSTORE_0:
			case FSTORE_1:
			case FSTORE_2:
			case FSTORE_3:
				return LocalVarOperationType.FLOAT_STORE;
			case DSTORE:
			case DSTORE_0:
			case DSTORE_1:
			case DSTORE_2:
			case DSTORE_3:
				return LocalVarOperationType.DOUBLE_STORE;
			case ASTORE:
			case ASTORE_0:
			case ASTORE_1:
			case ASTORE_2:
			case ASTORE_3:
				return LocalVarOperationType.OBJECT_STORE;
			case IINC:
				return LocalVarOperationType.INT_INC;
			case RET:
				return LocalVarOperationType.ADDRESS;
		}
	}

	public int getIncrementAmount() {
		return this.incrementAmount;
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		textBuffer.append(this.getOpcodeMnemonics());
		textBuffer.append(' ');
		int specificOpcode = this.calculateSpecificOpcode();
		if (this.targetVariable.needsWidePrefix()) {
			textBuffer.append(AbstractInsnNode.OPCODE_MNEMONICS[this.calculateSpecificOpcode()]);
			textBuffer.append(' ');
		}

		textBuffer.append(this.targetVariable.getIndex());
		String comment = AbstractInsnNode.fillCommentPlaceholder(AbstractInsnNode.getOpcodeComment(specificOpcode), String.valueOf(this.targetVariable.getIndex()));
		if (specificOpcode == IINC) {
			comment = AbstractInsnNode.fillCommentPlaceholder(comment, String.valueOf(this.incrementAmount));
		}

		if (comment.length() > 0) {
			textBuffer.append("\t" + comment);
		}

		printWriter.println(indent.toString() + indent.toString() + textBuffer);
	}

	@Override
	public final boolean isMethodExit() {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
			case INT_INC:
				return false;
			case ADDRESS:
				return true;
			default:
				return false;
		}
	}

	public int getHeadOpcode() {
		return this.requiresWidePrefix() ? 196 : this.calculateSpecificOpcode();
	}

	public int calculateSpecificOpcode() {
		int variableIndex = this.targetVariable.getIndex();
		switch (this.operationType) {
			case INT_LOAD:
				switch (variableIndex) {
					case 0:
						return ILOAD_0;
					case 1:
						return ILOAD_1;
					case 2:
						return ILOAD_2;
					case 3:
						return ILOAD_3;
					default:
						return ILOAD;
				}
			case LONG_LOAD:
				switch (variableIndex) {
					case 0:
						return LLOAD_0;
					case 1:
						return LLOAD_1;
					case 2:
						return LLOAD_2;
					case 3:
						return LLOAD_3;
					default:
						return LLOAD;
				}
			case FLOAT_LOAD:
				switch (variableIndex) {
					case 0:
						return FLOAD_0;
					case 1:
						return FLOAD_1;
					case 2:
						return FLOAD_2;
					case 3:
						return FLOAD_3;
					default:
						return FLOAD;
				}
			case DOUBLE_LOAD:
				switch (variableIndex) {
					case 0:
						return DLOAD_0;
					case 1:
						return DLOAD_1;
					case 2:
						return DLOAD_2;
					case 3:
						return DLOAD_3;
					default:
						return DLOAD;
				}
			case OBJECT_LOAD:
				switch (variableIndex) {
					case 0:
						return ALOAD_0;
					case 1:
						return ALOAD_1;
					case 2:
						return ALOAD_2;
					case 3:
						return ALOAD_3;
					default:
						return ALOAD;
				}
			case INT_STORE:
				switch (variableIndex) {
					case 0:
						return 59;
					case 1:
						return 60;
					case 2:
						return 61;
					case 3:
						return 62;
					default:
						return 54;
				}
			case FLOAT_STORE:
				switch (variableIndex) {
					case 0:
						return 67;
					case 1:
						return 68;
					case 2:
						return 69;
					case 3:
						return 70;
					default:
						return 56;
				}
			case OBJECT_STORE:
				switch (variableIndex) {
					case 0:
						return 75;
					case 1:
						return 76;
					case 2:
						return 77;
					case 3:
						return 78;
					default:
						return 58;
				}
			case LONG_STORE:
				switch (variableIndex) {
					case 0:
						return 63;
					case 1:
						return 64;
					case 2:
						return 65;
					case 3:
						return 66;
					default:
						return 55;
				}
			case DOUBLE_STORE:
				switch (variableIndex) {
					case 0:
						return 71;
					case 1:
						return 72;
					case 2:
						return 73;
					case 3:
						return 74;
					default:
						return 57;
				}
			case INT_INC:
				return IINC;
			case ADDRESS:
				return RET;
			default:
				return -1;
		}
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		new AnalyzeStackDebugContext(methodSignature);
		StackFrameType[] localVariables = frame.getLocalVariables();
		StackFrameType[] operandStack = frame.getOperandStack();
		int operandStackSize = operandStack.length;
		LocalsInitializationState localsInitState = frame.getLocalsInitState();
		Set<MonitorEnterNode> activeMonitors = frame.getActiveMonitors();
		int variableIndex = this.targetVariable.getIndex();
		LocalsInitializationState updatedLocalsInitState;
		if (localsInitState != null) {
			switch (this.operationType) {
				case INT_LOAD:
				case FLOAT_LOAD:
				case OBJECT_LOAD:
				case INT_STORE:
				case FLOAT_STORE:
				case OBJECT_STORE:
				case INT_INC:
				case ADDRESS:
					if (localsInitState.get(variableIndex)) {
						updatedLocalsInitState = localsInitState;
					} else {
						updatedLocalsInitState = (LocalsInitializationState)localsInitState.clone();
						updatedLocalsInitState.set(variableIndex);
					}
					break;
				case LONG_LOAD:
				case DOUBLE_LOAD:
				case LONG_STORE:
				case DOUBLE_STORE:
					if (localsInitState.get(variableIndex) && localsInitState.get(variableIndex + 1)) {
						updatedLocalsInitState = localsInitState;
					} else {
						updatedLocalsInitState = (LocalsInitializationState)localsInitState.clone();
						updatedLocalsInitState.set(variableIndex);
						updatedLocalsInitState.set(variableIndex + 1);
					}
					break;
				default:
					DebugUtils.assertTrue(false, new String[]{this.operationType.toString()});
					updatedLocalsInitState = null;
			}
		} else {
			updatedLocalsInitState = null;
		}

		FrameNode updatedFrame;
		switch (this.operationType) {
			case INT_LOAD:
				StackFrameType[] intLoadStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
				System.arraycopy(operandStack, 0, intLoadStack, 0, operandStackSize);
				intLoadStack[operandStackSize] = StackFrameType.INT;
				updatedFrame = new FrameNode(intLoadStack, localVariables, updatedLocalsInitState, activeMonitors);
				break;
			case LONG_LOAD:
				StackFrameType[] longLoadStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
				System.arraycopy(operandStack, 0, longLoadStack, 0, operandStackSize);
				longLoadStack[operandStackSize] = StackFrameType.LONG;
				updatedFrame = new FrameNode(longLoadStack, localVariables, updatedLocalsInitState, activeMonitors);
				break;
			case FLOAT_LOAD:
				StackFrameType[] floatLoadStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
				System.arraycopy(operandStack, 0, floatLoadStack, 0, operandStackSize);
				floatLoadStack[operandStackSize] = StackFrameType.FLOAT;
				updatedFrame = new FrameNode(floatLoadStack, localVariables, updatedLocalsInitState, activeMonitors);
				break;
			case DOUBLE_LOAD:
				StackFrameType[] doubleLoadStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
				System.arraycopy(operandStack, 0, doubleLoadStack, 0, operandStackSize);
				doubleLoadStack[operandStackSize] = StackFrameType.DOUBLE;
				updatedFrame = new FrameNode(doubleLoadStack, localVariables, updatedLocalsInitState, activeMonitors);
				break;
			case OBJECT_LOAD:
				StackFrameType[] objectLoadStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
				System.arraycopy(operandStack, 0, objectLoadStack, 0, operandStackSize);
				objectLoadStack[operandStackSize] = localVariables[variableIndex];
				updatedFrame = new FrameNode(objectLoadStack, localVariables, updatedLocalsInitState, activeMonitors);
				break;
			case INT_STORE:
				updatedFrame = this.updateFrameForCategory1Store(operandStackSize, operandStack, localVariables, StackFrameType.INT, updatedLocalsInitState, activeMonitors);
				break;
			case FLOAT_STORE:
				updatedFrame = this.updateFrameForCategory1Store(operandStackSize, operandStack, localVariables, StackFrameType.FLOAT, updatedLocalsInitState, activeMonitors);
				break;
			case OBJECT_STORE:
				int localVariablesSize = localVariables.length;
				StackFrameType[] newLocalVariables = StackFrameType.createEmptyFrame(localVariablesSize);
				System.arraycopy(localVariables, 0, newLocalVariables, 0, localVariablesSize);
				if (newLocalVariables[variableIndex].equals(StackFrameType.UNINITIALIZED) && (newLocalVariables[variableIndex - 1].equals(StackFrameType.LONG) || newLocalVariables[variableIndex - 1].equals(StackFrameType.DOUBLE))) {
					newLocalVariables[variableIndex - 1] = StackFrameType.UNKNOWN;
					if (updatedLocalsInitState != null) {
						updatedLocalsInitState.set(variableIndex - 1);
					}
				}

				if (newLocalVariables[variableIndex].equals(StackFrameType.LONG) || newLocalVariables[variableIndex].equals(StackFrameType.DOUBLE)) {
					newLocalVariables[variableIndex + 1] = StackFrameType.UNKNOWN;
					if (updatedLocalsInitState != null) {
						updatedLocalsInitState.set(variableIndex + 1);
					}
				}

				newLocalVariables[variableIndex] = operandStack[operandStackSize - 1];
				StackFrameType[] objectStoreStack = StackFrameType.createEmptyFrame(operandStackSize - 1);
				System.arraycopy(operandStack, 0, objectStoreStack, 0, operandStackSize - 1);
				updatedFrame = new FrameNode(objectStoreStack, newLocalVariables, updatedLocalsInitState, activeMonitors);
				break;
			case LONG_STORE:
				updatedFrame = this.updateFrameForCategory2Store(operandStackSize, operandStack, localVariables, StackFrameType.LONG, updatedLocalsInitState, activeMonitors);
				break;
			case DOUBLE_STORE:
				updatedFrame = this.updateFrameForCategory2Store(operandStackSize, operandStack, localVariables, StackFrameType.DOUBLE, updatedLocalsInitState, activeMonitors);
				break;
			case INT_INC:
				if (updatedLocalsInitState != null && updatedLocalsInitState != localsInitState) {
					updatedFrame = new FrameNode(frame.getOperandStack(), frame.getLocalVariables(), updatedLocalsInitState, activeMonitors);
				} else {
					updatedFrame = new FrameNode(operandStack, localVariables, localsInitState, activeMonitors);
				}
				break;
			case ADDRESS:
				if (updatedLocalsInitState != null && updatedLocalsInitState != localsInitState) {
					updatedFrame = new FrameNode(operandStack, localVariables, updatedLocalsInitState, activeMonitors);
				} else {
					updatedFrame = new FrameNode(operandStack, localVariables, localsInitState, activeMonitors);
				}
				break;
			default:
				DebugUtils.assertTrue(false, new String[]{"Invalid variable type and action " + this.operationType});
				updatedFrame = null;
		}

		return updatedFrame;
	}

	public int getVariableIndex() {
		return this.targetVariable.getIndex();
	}

	@Override
	public int getOpcode() {
		return super.getOpcode();
	}

	@Override
	public boolean isVarLoad() {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
				return true;
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
			case INT_INC:
			case ADDRESS:
				return false;
			default:
				return false;
		}
	}

	@Override
	public final boolean isStackProducer() {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
				return true;
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
			case INT_INC:
			case ADDRESS:
				return false;
			default:
				return false;
		}
	}

	@Override
	public final String getOpcodeMnemonics() {
		return AbstractInsnNode.OPCODE_MNEMONICS[this.getHeadOpcode()];
	}

	public LocalVarOperationType getOperationType() {
		return this.operationType;
	}

	@Override
	public final boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
			case INT_INC:
			case ADDRESS:
				return false;
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
				return stackIndex >= stackSize;
			default:
				return false;
		}
	}

	@Override
	public boolean isThatVarLoad(int localSlotIndex) {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
				return this.targetVariable.getIndex() == localSlotIndex;
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
			case INT_INC:
			case ADDRESS:
				return false;
			default:
				return false;
		}
	}

	public LocalVarOperationType getLoadOperationType() {
		switch (this.operationType) {
			case INT_STORE:
				return LocalVarOperationType.INT_LOAD;
			case FLOAT_STORE:
				return LocalVarOperationType.FLOAT_LOAD;
			case OBJECT_STORE:
				return LocalVarOperationType.OBJECT_LOAD;
			case LONG_STORE:
				return LocalVarOperationType.LONG_LOAD;
			case DOUBLE_STORE:
				return LocalVarOperationType.DOUBLE_LOAD;
			default:
				return null;
		}
	}

	private FrameNode updateFrameForCategory2Store(
		int stackSize, StackFrameType[] operandStack, StackFrameType[] localVariables, StackFrameType storedType, LocalsInitializationState localsInitState, Set<MonitorEnterNode> activeMonitors
	) {
		int variableIndex = this.targetVariable.getIndex();
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(stackSize - 1);
		System.arraycopy(operandStack, 0, newOperandStack, 0, stackSize - 1);
		int localVariablesSize = localVariables.length;
		StackFrameType[] newLocalVariables = StackFrameType.createEmptyFrame(localVariablesSize);
		System.arraycopy(localVariables, 0, newLocalVariables, 0, localVariablesSize);
		if (newLocalVariables[variableIndex].equals(StackFrameType.UNINITIALIZED) && (newLocalVariables[variableIndex - 1].equals(StackFrameType.LONG) || newLocalVariables[variableIndex - 1].equals(StackFrameType.DOUBLE))) {
			newLocalVariables[variableIndex - 1] = StackFrameType.UNKNOWN;
		}

		newLocalVariables[variableIndex] = storedType;
		StackFrameType secondSlotType = newLocalVariables[variableIndex + 1];
		newLocalVariables[variableIndex + 1] = StackFrameType.UNINITIALIZED;
		if (secondSlotType.equals(StackFrameType.LONG) || secondSlotType.equals(StackFrameType.DOUBLE)) {
			newLocalVariables[variableIndex + 2] = StackFrameType.UNKNOWN;
			if (localsInitState != null) {
				localsInitState.set(variableIndex + 2);
			}
		}

		return new FrameNode(newOperandStack, newLocalVariables, localsInitState, activeMonitors);
	}

	@Override
	public boolean isThatVar(int localSlotIndex) {
		if (!this.isThatVarStore(localSlotIndex) && !this.isThatVarLoad(localSlotIndex)) {
			int specificOpcode = this.calculateSpecificOpcode();
			return specificOpcode != IINC && specificOpcode != RET ? false : this.targetVariable.getIndex() == localSlotIndex;
		} else {
			return true;
		}
	}

	@Override
	public final boolean isCategory2Producer() {
		switch (this.operationType) {
			case INT_LOAD:
			case FLOAT_LOAD:
			case OBJECT_LOAD:
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
			case INT_INC:
			case ADDRESS:
				return false;
			case LONG_LOAD:
			case DOUBLE_LOAD:
				return true;
			default:
				return false;
		}
	}

	private FrameNode updateFrameForCategory1Store(
		int stackSize, StackFrameType[] operandStack, StackFrameType[] localVariables, StackFrameType storedType, LocalsInitializationState localsInitState, Set<MonitorEnterNode> activeMonitors
	) {
		int variableIndex = this.targetVariable.getIndex();
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(stackSize - 1);
		System.arraycopy(operandStack, 0, newOperandStack, 0, stackSize - 1);
		int localVariablesSize = localVariables.length;
		StackFrameType[] newLocalVariables = StackFrameType.createEmptyFrame(localVariablesSize);
		System.arraycopy(localVariables, 0, newLocalVariables, 0, localVariablesSize);
		if (newLocalVariables[variableIndex].equals(StackFrameType.UNINITIALIZED) && (newLocalVariables[variableIndex - 1].equals(StackFrameType.LONG) || newLocalVariables[variableIndex - 1].equals(StackFrameType.DOUBLE))) {
			newLocalVariables[variableIndex - 1] = StackFrameType.UNKNOWN;
			if (localsInitState != null) {
				localsInitState.set(variableIndex - 1);
			}
		}

		if (newLocalVariables[variableIndex].equals(StackFrameType.LONG) || newLocalVariables[variableIndex].equals(StackFrameType.DOUBLE)) {
			newLocalVariables[variableIndex + 1] = StackFrameType.UNKNOWN;
			if (localsInitState != null) {
				localsInitState.set(variableIndex + 1);
			}
		}

		newLocalVariables[variableIndex] = storedType;
		return new FrameNode(newOperandStack, newLocalVariables, localsInitState, activeMonitors);
	}

	public boolean requiresWidePrefix() {
		return this.targetVariable.needsWidePrefix()
			|| this.operationType == LocalVarOperationType.INT_INC && (this.incrementAmount < -128 || this.incrementAmount > 127);
	}

	@Override
	public int getBytecodeSize() {
		int bytecodeSize = 0;
		switch (this.calculateSpecificOpcode()) {
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
			case RET:
				bytecodeSize = 2 + (this.requiresWidePrefix() ? 2 : 0);
				break;
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
				bytecodeSize = 1;
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
			default:
				break;
			case IINC:
				bytecodeSize = 3 + (this.requiresWidePrefix() ? 3 : 0);
		}

		return bytecodeSize;
	}

	@Override
	public boolean isStandaloneStackProducer() {
		return this.isStackProducer();
	}

	@Override
	public final boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
			case INT_INC:
			case ADDRESS:
				return false;
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
				return stackIndex >= stackSize;
			default:
				return false;
		}
	}

	@Override
	public final boolean canFallThrough() {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
			case INT_INC:
				return true;
			case ADDRESS:
				return false;
			default:
				return true;
		}
	}

	@Override
	public final boolean isThatVarStore(int localSlotIndex) {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
			case INT_INC:
			case ADDRESS:
				return false;
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
				return this.targetVariable.getIndex() == localSlotIndex;
			default:
				return false;
		}
	}

	@Override
	public final boolean isVarStore() {
		switch (this.operationType) {
			case INT_LOAD:
			case LONG_LOAD:
			case FLOAT_LOAD:
			case DOUBLE_LOAD:
			case OBJECT_LOAD:
			case INT_INC:
			case ADDRESS:
				return false;
			case INT_STORE:
			case FLOAT_STORE:
			case OBJECT_STORE:
			case LONG_STORE:
			case DOUBLE_STORE:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		if (this.requiresWidePrefix()) {
			dataOutputStream.writeByte(196);
		}

		int variableIndex = this.targetVariable.getIndex();
		switch (this.operationType) {
			case INT_LOAD:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(ILOAD_0);
						return;
					case 1:
						dataOutputStream.writeByte(ILOAD_1);
						return;
					case 2:
						dataOutputStream.writeByte(ILOAD_2);
						return;
					case 3:
						dataOutputStream.writeByte(ILOAD_3);
						return;
					default:
						dataOutputStream.writeByte(ILOAD);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case LONG_LOAD:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(LLOAD_0);
						return;
					case 1:
						dataOutputStream.writeByte(LLOAD_1);
						return;
					case 2:
						dataOutputStream.writeByte(LLOAD_2);
						return;
					case 3:
						dataOutputStream.writeByte(LLOAD_3);
						return;
					default:
						dataOutputStream.writeByte(LLOAD);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case FLOAT_LOAD:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(34);
						return;
					case 1:
						dataOutputStream.writeByte(35);
						return;
					case 2:
						dataOutputStream.writeByte(36);
						return;
					case 3:
						dataOutputStream.writeByte(37);
						return;
					default:
						dataOutputStream.writeByte(23);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case DOUBLE_LOAD:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(38);
						return;
					case 1:
						dataOutputStream.writeByte(39);
						return;
					case 2:
						dataOutputStream.writeByte(40);
						return;
					case 3:
						dataOutputStream.writeByte(41);
						return;
					default:
						dataOutputStream.writeByte(24);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case OBJECT_LOAD:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(42);
						return;
					case 1:
						dataOutputStream.writeByte(43);
						return;
					case 2:
						dataOutputStream.writeByte(44);
						return;
					case 3:
						dataOutputStream.writeByte(45);
						return;
					default:
						dataOutputStream.writeByte(25);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case INT_STORE:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(59);
						return;
					case 1:
						dataOutputStream.writeByte(60);
						return;
					case 2:
						dataOutputStream.writeByte(61);
						return;
					case 3:
						dataOutputStream.writeByte(62);
						return;
					default:
						dataOutputStream.writeByte(54);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case FLOAT_STORE:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(67);
						return;
					case 1:
						dataOutputStream.writeByte(68);
						return;
					case 2:
						dataOutputStream.writeByte(69);
						return;
					case 3:
						dataOutputStream.writeByte(70);
						return;
					default:
						dataOutputStream.writeByte(56);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case OBJECT_STORE:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(75);
						return;
					case 1:
						dataOutputStream.writeByte(76);
						return;
					case 2:
						dataOutputStream.writeByte(77);
						return;
					case 3:
						dataOutputStream.writeByte(78);
						return;
					default:
						dataOutputStream.writeByte(58);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case LONG_STORE:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(63);
						return;
					case 1:
						dataOutputStream.writeByte(64);
						return;
					case 2:
						dataOutputStream.writeByte(65);
						return;
					case 3:
						dataOutputStream.writeByte(66);
						return;
					default:
						dataOutputStream.writeByte(55);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case DOUBLE_STORE:
				switch (variableIndex) {
					case 0:
						dataOutputStream.writeByte(71);
						return;
					case 1:
						dataOutputStream.writeByte(72);
						return;
					case 2:
						dataOutputStream.writeByte(73);
						return;
					case 3:
						dataOutputStream.writeByte(74);
						return;
					default:
						dataOutputStream.writeByte(57);
						if (this.requiresWidePrefix()) {
							dataOutputStream.writeShort(variableIndex);
						} else {
							dataOutputStream.writeByte(variableIndex);
						}

						return;
				}
			case INT_INC:
				dataOutputStream.writeByte(IINC);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
					dataOutputStream.writeShort(this.getIncrementAmount());
				} else {
					dataOutputStream.writeByte(variableIndex);
					dataOutputStream.writeByte(this.getIncrementAmount());
				}
				break;
			case ADDRESS:
				dataOutputStream.writeByte(RET);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
		}
	}

	@Override
	public String getOpcodeText() {
		StringBuilder textBuffer = new StringBuilder();
		textBuffer.append(this.getOpcodeMnemonics());
		textBuffer.append(' ');
		int specificOpcode = this.calculateSpecificOpcode();
		if (this.requiresWidePrefix()) {
			textBuffer.append(AbstractInsnNode.OPCODE_MNEMONICS[specificOpcode]);
			textBuffer.append(' ');
		}

		textBuffer.append(this.targetVariable.getIndex());
		if (specificOpcode == IINC) {
			textBuffer.append(' ');
			textBuffer.append(this.incrementAmount);
		}

		return textBuffer.toString();
	}

	@Override
	public boolean isRet() {
		return this.operationType == LocalVarOperationType.ADDRESS;
	}
}

package com.zelix.framework.instructions;

import com.zelix.exceptions.UnknownOpcodeException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.WritableType;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.constant.*;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.listener.*;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.variable.LocalVarOperationType;
import com.zelix.framework.variable.LocalVariableSlot;
import com.zelix.framework.variable.LocalVariablesManager;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.NestedMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.dataset.PriorityEntry;
import com.zelix.utils.references.AtomicReference;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public abstract class AbstractInsnNode implements IOpcode {
	public static final String[] OPCODE_MNEMONICS = new String[202];
	public static final String[] OPCODE_DESCRIPTIONS = new String[202];
	public static IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
	public int opcode = -1;

	public AbstractInsnNode(int opcode) {
		this.opcode = opcode;
	}

	public static AbstractInsnNode storeFloat(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.FLOAT_STORE, localVariablesManager, flags);
	}

	public static AbstractInsnNode IINC(int localVarIndex, int incrementAmount, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.INT_INC, localVariablesManager, incrementAmount, flags);
	}

	public static String getOpcodeComment(int opcode) {
		String description = OPCODE_DESCRIPTIONS[opcode];
		return description != null && description.length() > 0 ? "//" + description : "";
	}

	public static AbstractInsnNode ldcClass(String className, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		ClassInfoWriter classInfoWriter = constantPoolManager.getOrCreateClassInfo(className, typeWriters);
		return new WideConstantPoolInsnNode(LDC_W, classInfoWriter);
	}

	public static AbstractInsnNode ldcInt(int intValue, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		return (AbstractInsnNode)(intValue >= -32768 && intValue <= 32767 ? newInt(intValue) : new WideConstantPoolInsnNode(LDC_W, constantPoolManager.createInteger(intValue, typeWriters)));
	}

	public static AbstractInsnNode ldcLong(long longValue, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		return new WideConstantPoolInsnNode(LDC2_W, constantPoolManager.getOrCreateLong(longValue, typeWriters));
	}

	public static AbstractInsnNode loadLong(LocalVariableSlot localVariableSlot) {
		return new VarInsnNode(localVariableSlot, LocalVarOperationType.LONG_LOAD);
	}

	public static AbstractInsnNode loadLong(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.LONG_LOAD, localVariablesManager, flags);
	}

	public static int appendLongConst(long longValue, List<AbstractInsnNode> instructions, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		AbstractInsnNode longConstantNode = ldcLong(longValue, constantPoolManager, typeWriters);
		instructions.add(longConstantNode);
		return longConstantNode.getBytecodeSize();
	}

	public static AbstractInsnNode storeInt(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.INT_STORE, localVariablesManager, flags);
	}

	public static AbstractInsnNode ldcString(String value, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, boolean needStringObf) {
		StringNode stringNode = constantPoolManager.createStringNode(value, typeWriters, needStringObf);
		return new WideConstantPoolInsnNode(LDC_W, stringNode);
	}

	public static AbstractInsnNode loadObject(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.OBJECT_LOAD, localVariablesManager, flags);
	}

	public static AbstractInsnNode loadFloat(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.FLOAT_LOAD, localVariablesManager, flags);
	}

	public static AbstractInsnNode storeLong(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.LONG_STORE, localVariablesManager, flags);
	}

	public static AbstractInsnNode storeObject(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.OBJECT_STORE, localVariablesManager, flags);
	}

	public static String fillCommentPlaceholder(String description, String replacement) {
		int placeholderIndex = description.indexOf(37);
		if (placeholderIndex != -1) {
			StringBuilder commentBuffer = new StringBuilder();
			commentBuffer.append(description.substring(0, placeholderIndex));
			if (replacement.length() > 200) {
				commentBuffer.append(replacement.substring(0, 50));
				commentBuffer.append("...<");
				commentBuffer.append(replacement.length() - 100);
				commentBuffer.append(" CHARS>...");
				commentBuffer.append(replacement.substring(replacement.length() - 50));
			} else {
				commentBuffer.append(replacement);
			}

			commentBuffer.append(description.substring(placeholderIndex + 1));
			return commentBuffer.toString();
		} else {
			return description;
		}
	}

	public static AbstractInsnNode loadInt(LocalVariableSlot localVariableSlot) {
		return new VarInsnNode(localVariableSlot, LocalVarOperationType.INT_LOAD);
	}

	public static AbstractInsnNode loadRetAddress(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.ADDRESS, localVariablesManager, flags);
	}

	public static AbstractInsnNode loadDouble(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.DOUBLE_LOAD, localVariablesManager, flags);
	}

	public static AbstractInsnNode readInstruction(
		ByteDataBuffer dataBuffer,
		int bytecodeOffset,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences,
		ConstantPool constantPool,
		LocalVariablesManager localVariablesManager
	) throws UnknownOpcodeException, IOException {
		int opcode = dataBuffer.read();
		switch (opcode) {
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
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
			case ARRAYLENGTH:
			case ATHROW:
				return InsnNode.of(opcode);
			case BIPUSH:
				return new BiPushNode(dataBuffer);
			case SIPUSH:
				return new SiPushNode(dataBuffer);
			case LDC:
				return new LdcNode(dataBuffer, constantPool, stringReferences, integerReferences, classReferences);
			case LDC_W:
			case LDC2_W:
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case ANEWARRAY:
			case CHECKCAST:
			case INSTANCEOF:
				return new WideConstantPoolInsnNode(opcode, dataBuffer, constantPool, stringReferences, integerReferences, longReferences, classReferences, memberReferences);
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
			case RET:
			case WIDE:
				return VarInsnNode.readFromBuffer(opcode, dataBuffer, localVariablesManager);
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
				return new JumpInsnNode(opcode, dataBuffer, bytecodeOffset, elementsByOffset);
			case GOTO:
				return new GotoNode(dataBuffer, bytecodeOffset, elementsByOffset);
			case JSR:
				return new JSRNode(dataBuffer, bytecodeOffset, elementsByOffset);
			case TABLESWITCH:
				return new TableSwitchNode(dataBuffer, bytecodeOffset, elementsByOffset);
			case LOOKUPSWITCH:
				return new LookupSwitchNode(dataBuffer, bytecodeOffset, elementsByOffset);
			case INVOKEINTERFACE:
				return new InvokeInterfaceNode(dataBuffer, constantPool, stringReferences, integerReferences, longReferences, classReferences, memberReferences);
			case INVOKEDYNAMIC:
				return new InvokeDynamicNode(dataBuffer, constantPool, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences);
			case NEW:
				return new NewNode(dataBuffer, constantPool, bytecodeOffset, stringReferences, integerReferences, longReferences, classReferences, memberReferences);
			case NEWARRAY:
				return new NewArrayNode(dataBuffer);
			case MONITORENTER:
				return new MonitorEnterNode();
			case MONITOREXIT:
				return new MonitorExitNode();
			case MULTIANEWARRAY:
				return new MultiANewArrayInsnNode(dataBuffer, constantPool, stringReferences, integerReferences, longReferences, classReferences, memberReferences);
			case GOTO_W:
			case JSR_W:
				return new WideJumpInsnNode(opcode, dataBuffer, bytecodeOffset, elementsByOffset);
			default:
				throw new UnknownOpcodeException("Error: unknown opcode=" + opcode + " (A)");
		}
	}

	public static AbstractInsnNode newInt(int intValue) {
		Object intConstNode = null;
		switch (intValue) {
			case -1:
				intConstNode = InsnNode.of(ICONST_M1);
				break;
			case 0:
				intConstNode = InsnNode.of(ICONST_0);
				break;
			case 1:
				intConstNode = InsnNode.of(ICONST_1);
				break;
			case 2:
				intConstNode = InsnNode.of(ICONST_2);
				break;
			case 3:
				intConstNode = InsnNode.of(ICONST_3);
				break;
			case 4:
				intConstNode = InsnNode.of(ICONST_4);
				break;
			case 5:
				intConstNode = InsnNode.of(ICONST_5);
				break;
			default:
				if (intValue >= -128 && intValue <= 127) {
					intConstNode = new BiPushNode(intValue);
				} else if (intValue >= -32768 && intValue <= 32767) {
					intConstNode = new SiPushNode(intValue);
				}
		}

		return (AbstractInsnNode)intConstNode;
	}

	public static AbstractInsnNode loadInt(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.INT_LOAD, localVariablesManager, flags);
	}

	public static int encryptLongConstant(long longValue, List<AbstractInsnNode> instructions, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters, AtomicReference<LongNode> maskConstantRef) {
		int bytecodeSize = 0;
		int highChunk = (int)(longValue >>> 48);
		int upperChunk = (int)(longValue << 16 >>> 48);
		int lowerChunk = (int)(longValue << 32 >>> 48);
		int lowChunk = (int)(longValue << 48 >>> 48);
		ArrayList<AbstractInsnNode> constantInstructions = new ArrayList<>(17);
		boolean valueOnStack = false;
		if (highChunk != 0) {
			constantInstructions.add(newInt((short)highChunk));
			constantInstructions.add(InsnNode.of(I2L));
			constantInstructions.add(new BiPushNode(48));
			constantInstructions.add(InsnNode.of(LSHL));
			valueOnStack = true;
		}

		LongNode maskConstant = maskConstantRef.get();
		if (upperChunk != 0) {
			short upperChunkShort = (short)upperChunk;
			constantInstructions.add(newInt(upperChunkShort));
			constantInstructions.add(InsnNode.of(I2L));
			if (upperChunkShort < 0) {
				if (maskConstant == null) {
					maskConstant = constantPoolManager.getOrCreateLong(65535L, typeWriters);
					maskConstantRef.set(maskConstant);
				}

				constantInstructions.add(new WideConstantPoolInsnNode(LDC2_W, maskConstant));
				constantInstructions.add(InsnNode.of(LAND));
			}

			constantInstructions.add(new BiPushNode(32));
			constantInstructions.add(InsnNode.of(LSHL));
			if (valueOnStack) {
				constantInstructions.add(InsnNode.of(LOR));
			} else {
				valueOnStack = true;
			}
		}

		if (lowerChunk != 0) {
			short lowerChunkShort = (short)lowerChunk;
			constantInstructions.add(newInt(lowerChunkShort));
			constantInstructions.add(InsnNode.of(I2L));
			if (lowerChunkShort < 0) {
				if (maskConstant == null) {
					maskConstant = constantPoolManager.getOrCreateLong(65535L, typeWriters);
					maskConstantRef.set(maskConstant);
				}

				constantInstructions.add(new WideConstantPoolInsnNode(LDC2_W, maskConstant));
				constantInstructions.add(InsnNode.of(LAND));
			}

			constantInstructions.add(new BiPushNode(16));
			constantInstructions.add(InsnNode.of(LSHL));
			if (valueOnStack) {
				constantInstructions.add(InsnNode.of(LOR));
			} else {
				valueOnStack = true;
			}
		}

		if (lowChunk != 0) {
			short lowChunkShort = (short)lowChunk;
			constantInstructions.add(newInt(lowChunkShort));
			constantInstructions.add(InsnNode.of(I2L));
			if (lowChunkShort < 0) {
				if (maskConstant == null) {
					maskConstant = constantPoolManager.getOrCreateLong(65535L, typeWriters);
					maskConstantRef.set(maskConstant);
				}

				constantInstructions.add(new WideConstantPoolInsnNode(LDC2_W, maskConstant));
				constantInstructions.add(InsnNode.of(LAND));
			}

			if (valueOnStack) {
				constantInstructions.add(InsnNode.of(LOR));
			}
		}

		for (AbstractInsnNode instruction : constantInstructions) {
			bytecodeSize += instruction.getBytecodeSize();
		}

		instructions.addAll(constantInstructions);
		return bytecodeSize;
	}

	public static AbstractInsnNode createLoadByTypeDesc(String typeDescriptor, int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		if (typeDescriptor.length() == 1) {
			switch (typeDescriptor.charAt(0)) {
				case 'B':
				case 'C':
				case 'I':
				case 'S':
				case 'Z':
					return loadInt(localVarIndex, localVariablesManager, flags);
				case 'D':
					return loadDouble(localVarIndex, localVariablesManager, flags);
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
					return loadFloat(localVarIndex, localVariablesManager, flags);
				case 'J':
					return loadLong(localVarIndex, localVariablesManager, flags);
			}
		} else {
			return loadObject(localVarIndex, localVariablesManager, flags);
		}
	}

	public static int encryptIntConstant(int intValue, List<AbstractInsnNode> instructions, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		int bytecodeSize = 0;
		if (intValue >= -32768 && intValue <= 32767) {
			AbstractInsnNode intConstNode = newInt(intValue);
			instructions.add(intConstNode);
			bytecodeSize = intConstNode.getBytecodeSize();
		} else {
			int remainder = intValue % 131070;
			int quotient = intValue / 131070;
			boolean combinedByAddition = true;
			int highPart;
			int lowPart;
			if (Math.abs(remainder) > 65535) {
				if (quotient == 0) {
					highPart = 131070 * (remainder > 0 ? 1 : -1);
				} else if (quotient > 0) {
					highPart = 131070 * (quotient + 1);
				} else {
					highPart = 131070 * (quotient - 1);
				}

				combinedByAddition = false;
				lowPart = remainder > 0 ? 131070 - remainder : (131070 + remainder) * -1;
			} else {
				highPart = 131070 * quotient;
				lowPart = remainder;
			}

			ArrayList<AbstractInsnNode> constantInstructions = new ArrayList<>();
			if (highPart != 0) {
				WideConstantPoolInsnNode highPartNode = new WideConstantPoolInsnNode(LDC_W, constantPoolManager.createInteger(highPart, typeWriters));
				constantInstructions.add(highPartNode);
				bytecodeSize = 0 + highPartNode.getBytecodeSize();
			}

			if (lowPart >= -32768 && lowPart <= 32767) {
				AbstractInsnNode lowPartNode = newInt(lowPart);
				bytecodeSize += lowPartNode.getBytecodeSize();
				constantInstructions.add(lowPartNode);
			} else {
				AbstractInsnNode absoluteLowPartNode = newInt((short)(Math.abs(lowPart) & 65535));
				constantInstructions.add(absoluteLowPartNode);
				bytecodeSize += absoluteLowPartNode.getBytecodeSize();
				constantInstructions.add(InsnNode.of(I2C));
				bytecodeSize++;
				if (lowPart < 0) {
					constantInstructions.add(InsnNode.of(INEG));
					bytecodeSize++;
				}
			}

			if (highPart != 0) {
				if (combinedByAddition) {
					constantInstructions.add(InsnNode.of(IADD));
					bytecodeSize++;
				} else {
					constantInstructions.add(InsnNode.of(ISUB));
					bytecodeSize++;
				}
			}

			instructions.addAll(constantInstructions);
			if (combinedByAddition) {
				int recomposedSum = highPart + lowPart;
			} else {
				int recomposedDifference = highPart - lowPart;
			}
		}

		return bytecodeSize;
	}

	public static AbstractInsnNode storeLong(LocalVariableSlot localVariableSlot) {
		return new VarInsnNode(localVariableSlot, LocalVarOperationType.LONG_STORE);
	}

	public static AbstractInsnNode createStoreInsnByTypeDesc(String typeDescriptor, int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		if (typeDescriptor.length() == 1) {
			switch (typeDescriptor.charAt(0)) {
				case 'B':
				case 'C':
				case 'I':
				case 'S':
				case 'Z':
					return storeInt(localVarIndex, localVariablesManager, flags);
				case 'D':
					return storeDouble(localVarIndex, localVariablesManager, flags);
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
					return storeFloat(localVarIndex, localVariablesManager, flags);
				case 'J':
					return storeLong(localVarIndex, localVariablesManager, flags);
			}
		} else {
			return storeObject(localVarIndex, localVariablesManager, flags);
		}
	}

	public static AbstractInsnNode storeDouble(int localVarIndex, LocalVariablesManager localVariablesManager, int flags) {
		return new VarInsnNode(localVarIndex, LocalVarOperationType.DOUBLE_STORE, localVariablesManager, flags);
	}

	public boolean isFieldOpcode() {
		return false;
	}

	public abstract void printInsn(PrintWriter printWriter, StringBuilder indent) throws UnknownOpcodeException;

	public boolean isProducedByStackOp(StackFrameType[] stackBefore, int newStackIndex) {
		return false;
	}

	public abstract boolean isMethodExit();

	public boolean isLabel() {
		return false;
	}

	public boolean isIntegerOpcode() {
		return false;
	}

	public abstract FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature);

	public boolean isPureStackOpcode() {
		return false;
	}

	public boolean isIntegerConstant2() {
		return false;
	}

	public int getOpcode() {
		return this.opcode;
	}

	public abstract int getStackEffect();

	public int[] mapOldStackIndexToNew(StackFrameType[] oldStack, StackFrameType[] newStack, int oldStackIndex) {
		return new int[]{oldStackIndex};
	}

	public boolean isLConst() {
		return false;
	}

	public boolean isVarLoad() {
		return false;
	}

	public AbstractInsnNode optimizeToShortLdc(Map<TypeWriter, TypeWriter> typeWriterMapping) {
		return null;
	}

	public abstract boolean isStackProducer();

	public String getOpcodeMnemonics() {
		return OPCODE_MNEMONICS[this.getOpcode()];
	}

	public void setOffset(int bytecodeOffset) {
	}

	public boolean hasFallThroughOrIsJsr() {
		return this.canFallThrough();
	}

	public boolean isPutField() {
		return false;
	}

	public abstract boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize);

	public boolean isThatVarLoad(int localSlotIndex) {
		return false;
	}

	public boolean isInvoke() {
		return false;
	}

	public int getStackIndexOrigin(StackFrameType[] stackBefore, int newStackIndex) {
		return newStackIndex;
	}

	public boolean isThatVar(int localSlotIndex) {
		return this.isThatVarStore(localSlotIndex) || this.isThatVarLoad(localSlotIndex);
	}

	public boolean isJSR() {
		return false;
	}

	public abstract boolean isCategory2Producer();

	public List<AbstractInsnNode> expandToWideIfNeeded() {
		return null;
	}

	public boolean isLong() {
		return false;
	}

	public int getBytecodeSize() {
		return 1;
	}

	public boolean isIntegerConstant() {
		return false;
	}

	public boolean collectStringObfCandidate(
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries, Set<StringNode> excludedStrings, MethodNode methodNode, int instructionIndex
	) {
		return false;
	}

	public abstract boolean isStandaloneStackProducer();

	public abstract boolean consumesStackFromIndex(int stackIndex, int stackSize);

	public boolean isJump() {
		return false;
	}

	public void writeWithConstantMap(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> typeWriterMapping) throws IOException {
		this.writeToStream(dataOutputStream);
	}

	public abstract boolean canFallThrough();

	public boolean isString() {
		return false;
	}

	public boolean collectLongObfCandidate(
		NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longConstantEntries, Set<LongNode> excludedLongs, MethodNode methodNode, int instructionIndex
	) {
		return false;
	}

	public boolean isVarStore() {
		return false;
	}

	public boolean isThatVarStore(int localSlotIndex) {
		return false;
	}

	public boolean isMonitor() {
		return false;
	}

	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeByte(this.opcode);
	}

	public boolean collectIntObfCandidate(
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries, Set<IntegerNode> excludedIntegers, MethodNode methodNode, int instructionIndex
	) {
		return false;
	}

	public boolean isRet() {
		return false;
	}

	public abstract String getOpcodeText();

	public final String getOpcodeComment() {
		String description = OPCODE_DESCRIPTIONS[this.getOpcode()];
		return description != null && description.length() > 0 ? "//" + description : "";
	}

	public boolean isReturn() {
		return false;
	}

	static {
		OPCODE_DESCRIPTIONS[50] = "push reference from array";
		OPCODE_DESCRIPTIONS[83] = "pop reference into array";
		OPCODE_DESCRIPTIONS[1] = "push null object reference";
		OPCODE_DESCRIPTIONS[25] = "push reference from local%";
		OPCODE_DESCRIPTIONS[42] = "push reference from local0";
		OPCODE_DESCRIPTIONS[43] = "push reference from local1";
		OPCODE_DESCRIPTIONS[44] = "push reference from local2";
		OPCODE_DESCRIPTIONS[45] = "push reference from local3";
		OPCODE_DESCRIPTIONS[189] = "push a reference to a new array of %";
		OPCODE_DESCRIPTIONS[176] = "return reference";
		OPCODE_DESCRIPTIONS[190] = "push length of array";
		OPCODE_DESCRIPTIONS[58] = "pop reference into local%";
		OPCODE_DESCRIPTIONS[75] = "pop reference into local0";
		OPCODE_DESCRIPTIONS[76] = "pop reference into local1";
		OPCODE_DESCRIPTIONS[77] = "pop reference into local2";
		OPCODE_DESCRIPTIONS[78] = "pop reference into local3";
		OPCODE_DESCRIPTIONS[191] = "throw reference currently on stack";
		OPCODE_DESCRIPTIONS[51] = "push byte or boolean from array";
		OPCODE_DESCRIPTIONS[84] = "pop into byte or boolean array";
		OPCODE_DESCRIPTIONS[16] = "push % onto stack";
		OPCODE_DESCRIPTIONS[52] = "push char from array";
		OPCODE_DESCRIPTIONS[85] = "pop into char array";
		OPCODE_DESCRIPTIONS[192] = "cast to %";
		OPCODE_DESCRIPTIONS[144] = "convert double to float";
		OPCODE_DESCRIPTIONS[142] = "convert double to int";
		OPCODE_DESCRIPTIONS[143] = "convert double to long";
		OPCODE_DESCRIPTIONS[99] = "add the two doubles on the stack";
		OPCODE_DESCRIPTIONS[49] = "push double from array";
		OPCODE_DESCRIPTIONS[82] = "pop into double array";
		OPCODE_DESCRIPTIONS[151] = "compare two doubles on stack";
		OPCODE_DESCRIPTIONS[152] = "compare two doubles on stack";
		OPCODE_DESCRIPTIONS[14] = "push double 0.0";
		OPCODE_DESCRIPTIONS[15] = "push double 1.0";
		OPCODE_DESCRIPTIONS[111] = "divide two doubles";
		OPCODE_DESCRIPTIONS[24] = "push double from local%";
		OPCODE_DESCRIPTIONS[38] = "push double from local0";
		OPCODE_DESCRIPTIONS[39] = "push double from local1";
		OPCODE_DESCRIPTIONS[40] = "push double from local2";
		OPCODE_DESCRIPTIONS[41] = "push double from local3";
		OPCODE_DESCRIPTIONS[107] = "multiply two doubles";
		OPCODE_DESCRIPTIONS[119] = "negate a double";
		OPCODE_DESCRIPTIONS[115] = "push remainder of two doubles";
		OPCODE_DESCRIPTIONS[175] = "return double";
		OPCODE_DESCRIPTIONS[57] = "pop double into local%";
		OPCODE_DESCRIPTIONS[71] = "pop double into local0";
		OPCODE_DESCRIPTIONS[72] = "pop double into local1";
		OPCODE_DESCRIPTIONS[73] = "pop double into local2";
		OPCODE_DESCRIPTIONS[74] = "pop double into local3";
		OPCODE_DESCRIPTIONS[103] = "subtract double at stack top from double below it";
		OPCODE_DESCRIPTIONS[89] = "duplicate top stack word";
		OPCODE_DESCRIPTIONS[90] = "duplicate top stack word & put 2 down";
		OPCODE_DESCRIPTIONS[91] = "duplicate top stack word & put 3 down";
		OPCODE_DESCRIPTIONS[92] = "duplicate top 2 stack words";
		OPCODE_DESCRIPTIONS[93] = "duplicate top 2 stack words & put 3 down";
		OPCODE_DESCRIPTIONS[94] = "duplicate top 2 stack words & put 4 down";
		OPCODE_DESCRIPTIONS[141] = "convert float to double";
		OPCODE_DESCRIPTIONS[139] = "convert float to int";
		OPCODE_DESCRIPTIONS[140] = "convert float to long";
		OPCODE_DESCRIPTIONS[98] = "add the two floats on the stack";
		OPCODE_DESCRIPTIONS[48] = "push float from array";
		OPCODE_DESCRIPTIONS[81] = "pop into float array";
		OPCODE_DESCRIPTIONS[149] = "compare two floats on stack";
		OPCODE_DESCRIPTIONS[150] = "compare two floats on stack";
		OPCODE_DESCRIPTIONS[11] = "push float 0.0";
		OPCODE_DESCRIPTIONS[12] = "push float 1.0";
		OPCODE_DESCRIPTIONS[13] = "push float 2.0";
		OPCODE_DESCRIPTIONS[110] = "divide two floats";
		OPCODE_DESCRIPTIONS[23] = "push float from local%";
		OPCODE_DESCRIPTIONS[34] = "push float from local0";
		OPCODE_DESCRIPTIONS[35] = "push float from local1";
		OPCODE_DESCRIPTIONS[36] = "push float from local2";
		OPCODE_DESCRIPTIONS[37] = "push float from local3";
		OPCODE_DESCRIPTIONS[106] = "multiply two floats";
		OPCODE_DESCRIPTIONS[118] = "negate a float";
		OPCODE_DESCRIPTIONS[114] = "push remainder of two floats";
		OPCODE_DESCRIPTIONS[174] = "return float";
		OPCODE_DESCRIPTIONS[56] = "pop float into local%";
		OPCODE_DESCRIPTIONS[67] = "pop float into local0";
		OPCODE_DESCRIPTIONS[68] = "pop float into local1";
		OPCODE_DESCRIPTIONS[69] = "pop float into local2";
		OPCODE_DESCRIPTIONS[70] = "pop float into local3";
		OPCODE_DESCRIPTIONS[102] = "subtract float at stack top from float below it";
		OPCODE_DESCRIPTIONS[180] = "push contents of field %";
		OPCODE_DESCRIPTIONS[178] = "push contents of field %";
		OPCODE_DESCRIPTIONS[167] = "goto %";
		OPCODE_DESCRIPTIONS[200] = "goto %";
		OPCODE_DESCRIPTIONS[145] = "convert int to byte";
		OPCODE_DESCRIPTIONS[146] = "convert int to char";
		OPCODE_DESCRIPTIONS[135] = "convert int to double";
		OPCODE_DESCRIPTIONS[133] = "convert int to long";
		OPCODE_DESCRIPTIONS[147] = "convert int to short";
		OPCODE_DESCRIPTIONS[96] = "add the two ints on the stack";
		OPCODE_DESCRIPTIONS[46] = "push int from array";
		OPCODE_DESCRIPTIONS[126] = "bitwise AND of two ints";
		OPCODE_DESCRIPTIONS[79] = "pop into int array";
		OPCODE_DESCRIPTIONS[2] = "push -1 onto stack";
		OPCODE_DESCRIPTIONS[3] = "push 0 onto stack";
		OPCODE_DESCRIPTIONS[4] = "push 1 onto stack";
		OPCODE_DESCRIPTIONS[5] = "push 2 onto stack";
		OPCODE_DESCRIPTIONS[6] = "push 3 onto stack";
		OPCODE_DESCRIPTIONS[7] = "push 4 onto stack";
		OPCODE_DESCRIPTIONS[8] = "push 5 onto stack";
		OPCODE_DESCRIPTIONS[108] = "divide two ints";
		OPCODE_DESCRIPTIONS[165] = "goto % if ref1 == ref2";
		OPCODE_DESCRIPTIONS[166] = "goto % if ref1 != ref2";
		OPCODE_DESCRIPTIONS[159] = "goto % if int1 == int2";
		OPCODE_DESCRIPTIONS[160] = "goto % if int1 != int2";
		OPCODE_DESCRIPTIONS[161] = "goto % if int1 < int2";
		OPCODE_DESCRIPTIONS[162] = "goto % if int1 >= int2";
		OPCODE_DESCRIPTIONS[163] = "goto % if int1 > int2";
		OPCODE_DESCRIPTIONS[164] = "goto % if int1 <= int2";
		OPCODE_DESCRIPTIONS[153] = "goto % if int == 0";
		OPCODE_DESCRIPTIONS[154] = "goto % if int != 0";
		OPCODE_DESCRIPTIONS[155] = "goto % if int < 0";
		OPCODE_DESCRIPTIONS[156] = "goto % if int >= 0";
		OPCODE_DESCRIPTIONS[157] = "goto % if int > 0";
		OPCODE_DESCRIPTIONS[158] = "goto % if int <= 0";
		OPCODE_DESCRIPTIONS[199] = "goto % if objectref != null";
		OPCODE_DESCRIPTIONS[198] = "goto % if objectref == null";
		OPCODE_DESCRIPTIONS[132] = "increment local% by %";
		OPCODE_DESCRIPTIONS[21] = "push int from local%";
		OPCODE_DESCRIPTIONS[26] = "push int from local0";
		OPCODE_DESCRIPTIONS[27] = "push int from local1";
		OPCODE_DESCRIPTIONS[28] = "push int from local2";
		OPCODE_DESCRIPTIONS[29] = "push int from local3";
		OPCODE_DESCRIPTIONS[104] = "multiply two ints";
		OPCODE_DESCRIPTIONS[116] = "negate an int";
		OPCODE_DESCRIPTIONS[193] = "is objectref of class %?";
		OPCODE_DESCRIPTIONS[185] = "invoke % with % operands";
		OPCODE_DESCRIPTIONS[186] = "invoke % dynamically using %";
		OPCODE_DESCRIPTIONS[183] = "invoke %";
		OPCODE_DESCRIPTIONS[184] = "invoke %";
		OPCODE_DESCRIPTIONS[182] = "invoke %";
		OPCODE_DESCRIPTIONS[128] = "bitwise OR of two ints";
		OPCODE_DESCRIPTIONS[112] = "push remainder of two ints";
		OPCODE_DESCRIPTIONS[172] = "return int";
		OPCODE_DESCRIPTIONS[120] = "shift int1 left by low 5 bits of int2";
		OPCODE_DESCRIPTIONS[122] = "shift int1 right by low 5 bits of int2";
		OPCODE_DESCRIPTIONS[54] = "pop int from stack into local%";
		OPCODE_DESCRIPTIONS[59] = "pop int from stack into local0";
		OPCODE_DESCRIPTIONS[60] = "pop int from stack into local1";
		OPCODE_DESCRIPTIONS[61] = "pop int from stack into local2";
		OPCODE_DESCRIPTIONS[62] = "pop int from stack into local3";
		OPCODE_DESCRIPTIONS[100] = "subtract int at stack top from int below it";
		OPCODE_DESCRIPTIONS[124] = "logical shift int1 right by low 5 bits of int2";
		OPCODE_DESCRIPTIONS[130] = "bitwise XOR of two ints";
		OPCODE_DESCRIPTIONS[168] = "push address of next opcode then goto %";
		OPCODE_DESCRIPTIONS[201] = "push address of next opcode then goto %";
		OPCODE_DESCRIPTIONS[138] = "convert long to double";
		OPCODE_DESCRIPTIONS[137] = "convert long to float";
		OPCODE_DESCRIPTIONS[136] = "convert long to int";
		OPCODE_DESCRIPTIONS[97] = "add the two longs on the stack";
		OPCODE_DESCRIPTIONS[47] = "push long from array";
		OPCODE_DESCRIPTIONS[127] = "bitwise AND of two longs";
		OPCODE_DESCRIPTIONS[80] = "pop into long array";
		OPCODE_DESCRIPTIONS[148] = "compare two longs on stack";
		OPCODE_DESCRIPTIONS[9] = "push long 0";
		OPCODE_DESCRIPTIONS[10] = "push long 1";
		OPCODE_DESCRIPTIONS[18] = "push % from constant pool";
		OPCODE_DESCRIPTIONS[19] = "push % from constant pool";
		OPCODE_DESCRIPTIONS[20] = "push long or double % from constant pool";
		OPCODE_DESCRIPTIONS[109] = "divide two longs";
		OPCODE_DESCRIPTIONS[22] = "push long from local%";
		OPCODE_DESCRIPTIONS[30] = "push long from local0";
		OPCODE_DESCRIPTIONS[31] = "push long from local1";
		OPCODE_DESCRIPTIONS[32] = "push long from local2";
		OPCODE_DESCRIPTIONS[33] = "push long from local3";
		OPCODE_DESCRIPTIONS[105] = "multiply two longs";
		OPCODE_DESCRIPTIONS[117] = "negate a long";
		OPCODE_DESCRIPTIONS[129] = "bitwise OR of two longs";
		OPCODE_DESCRIPTIONS[113] = "push remainder of two longs";
		OPCODE_DESCRIPTIONS[173] = "return long";
		OPCODE_DESCRIPTIONS[121] = "shift long left by low 6 bits of int";
		OPCODE_DESCRIPTIONS[123] = "shift long right by low 6 bits of int";
		OPCODE_DESCRIPTIONS[55] = "pop long from stack into local%";
		OPCODE_DESCRIPTIONS[63] = "pop long from stack into local0";
		OPCODE_DESCRIPTIONS[64] = "pop long from stack into local1";
		OPCODE_DESCRIPTIONS[65] = "pop long from stack into local2";
		OPCODE_DESCRIPTIONS[66] = "pop long from stack into local3";
		OPCODE_DESCRIPTIONS[101] = "subtract long at stack top from long below it";
		OPCODE_DESCRIPTIONS[125] = "logical shift long right by low 6 bits of int";
		OPCODE_DESCRIPTIONS[131] = "bitwise XOR of two longs";
		OPCODE_DESCRIPTIONS[194] = "enter monitor for objectref";
		OPCODE_DESCRIPTIONS[195] = "exit monitor for objectref";
		OPCODE_DESCRIPTIONS[197] = "new array of % with % dimensions";
		OPCODE_DESCRIPTIONS[187] = "new %";
		OPCODE_DESCRIPTIONS[188] = "new array of %";
		OPCODE_DESCRIPTIONS[0] = "do nothing";
		OPCODE_DESCRIPTIONS[87] = "pop 1 word";
		OPCODE_DESCRIPTIONS[88] = "pop 2 words";
		OPCODE_DESCRIPTIONS[181] = "set field %";
		OPCODE_DESCRIPTIONS[179] = "set field %";
		OPCODE_DESCRIPTIONS[169] = "return from subroutine. Use address in local%";
		OPCODE_DESCRIPTIONS[177] = "return void from method";
		OPCODE_DESCRIPTIONS[53] = "push a short from an array";
		OPCODE_DESCRIPTIONS[86] = "pop a short into an array";
		OPCODE_DESCRIPTIONS[17] = "push % onto stack";
		OPCODE_DESCRIPTIONS[95] = "swap two stack words";
		OPCODE_DESCRIPTIONS[196] = "modifies behaviour of next opcode. NB: has 2 formats";
		OPCODE_MNEMONICS[0] = "nop";
		OPCODE_MNEMONICS[1] = "aconst_null";
		OPCODE_MNEMONICS[2] = "iconst_m1";
		OPCODE_MNEMONICS[3] = "iconst_0";
		OPCODE_MNEMONICS[4] = "iconst_1";
		OPCODE_MNEMONICS[5] = "iconst_2";
		OPCODE_MNEMONICS[6] = "iconst_3";
		OPCODE_MNEMONICS[7] = "iconst_4";
		OPCODE_MNEMONICS[8] = "iconst_5";
		OPCODE_MNEMONICS[9] = "lconst_0";
		OPCODE_MNEMONICS[10] = "lconst_1";
		OPCODE_MNEMONICS[11] = "fconst_0";
		OPCODE_MNEMONICS[12] = "fconst_1";
		OPCODE_MNEMONICS[13] = "fconst_2";
		OPCODE_MNEMONICS[14] = "dconst_0";
		OPCODE_MNEMONICS[15] = "dconst_1";
		OPCODE_MNEMONICS[16] = "bipush";
		OPCODE_MNEMONICS[17] = "sipush";
		OPCODE_MNEMONICS[18] = "ldc";
		OPCODE_MNEMONICS[19] = "ldc_w";
		OPCODE_MNEMONICS[20] = "ldc2_w";
		OPCODE_MNEMONICS[21] = "iload";
		OPCODE_MNEMONICS[22] = "lload";
		OPCODE_MNEMONICS[23] = "fload";
		OPCODE_MNEMONICS[24] = "dload";
		OPCODE_MNEMONICS[25] = "aload";
		OPCODE_MNEMONICS[26] = "iload_0";
		OPCODE_MNEMONICS[27] = "iload_1";
		OPCODE_MNEMONICS[28] = "iload_2";
		OPCODE_MNEMONICS[29] = "iload_3";
		OPCODE_MNEMONICS[30] = "lload_0";
		OPCODE_MNEMONICS[31] = "lload_1";
		OPCODE_MNEMONICS[32] = "lload_2";
		OPCODE_MNEMONICS[33] = "lload_3";
		OPCODE_MNEMONICS[34] = "fload_0";
		OPCODE_MNEMONICS[35] = "fload_1";
		OPCODE_MNEMONICS[36] = "fload_2";
		OPCODE_MNEMONICS[37] = "fload_3";
		OPCODE_MNEMONICS[38] = "dload_0";
		OPCODE_MNEMONICS[39] = "dload_1";
		OPCODE_MNEMONICS[40] = "dload_2";
		OPCODE_MNEMONICS[41] = "dload_3";
		OPCODE_MNEMONICS[42] = "aload_0";
		OPCODE_MNEMONICS[43] = "aload_1";
		OPCODE_MNEMONICS[44] = "aload_2";
		OPCODE_MNEMONICS[45] = "aload_3";
		OPCODE_MNEMONICS[46] = "iaload";
		OPCODE_MNEMONICS[47] = "laload";
		OPCODE_MNEMONICS[48] = "faload";
		OPCODE_MNEMONICS[49] = "daload";
		OPCODE_MNEMONICS[50] = "aaload";
		OPCODE_MNEMONICS[51] = "baload";
		OPCODE_MNEMONICS[52] = "caload";
		OPCODE_MNEMONICS[53] = "saload";
		OPCODE_MNEMONICS[54] = "istore";
		OPCODE_MNEMONICS[55] = "lstore";
		OPCODE_MNEMONICS[56] = "fstore";
		OPCODE_MNEMONICS[57] = "dstore";
		OPCODE_MNEMONICS[58] = "astore";
		OPCODE_MNEMONICS[59] = "istore_0";
		OPCODE_MNEMONICS[60] = "istore_1";
		OPCODE_MNEMONICS[61] = "istore_2";
		OPCODE_MNEMONICS[62] = "istore_3";
		OPCODE_MNEMONICS[63] = "lstore_0";
		OPCODE_MNEMONICS[64] = "lstore_1";
		OPCODE_MNEMONICS[65] = "lstore_2";
		OPCODE_MNEMONICS[66] = "lstore_3";
		OPCODE_MNEMONICS[67] = "fstore_0";
		OPCODE_MNEMONICS[68] = "fstore_1";
		OPCODE_MNEMONICS[69] = "fstore_2";
		OPCODE_MNEMONICS[70] = "fstore_3";
		OPCODE_MNEMONICS[71] = "dstore_0";
		OPCODE_MNEMONICS[72] = "dstore_1";
		OPCODE_MNEMONICS[73] = "dstore_2";
		OPCODE_MNEMONICS[74] = "dstore_3";
		OPCODE_MNEMONICS[75] = "astore_0";
		OPCODE_MNEMONICS[76] = "astore_1";
		OPCODE_MNEMONICS[77] = "astore_2";
		OPCODE_MNEMONICS[78] = "astore_3";
		OPCODE_MNEMONICS[79] = "iastore";
		OPCODE_MNEMONICS[80] = "lastore";
		OPCODE_MNEMONICS[81] = "fastore";
		OPCODE_MNEMONICS[82] = "dastore";
		OPCODE_MNEMONICS[83] = "aastore";
		OPCODE_MNEMONICS[84] = "bastore";
		OPCODE_MNEMONICS[85] = "castore";
		OPCODE_MNEMONICS[86] = "sastore";
		OPCODE_MNEMONICS[87] = "pop";
		OPCODE_MNEMONICS[88] = "pop2";
		OPCODE_MNEMONICS[89] = "dup";
		OPCODE_MNEMONICS[90] = "dup_x1";
		OPCODE_MNEMONICS[91] = "dup_x2";
		OPCODE_MNEMONICS[92] = "dup2";
		OPCODE_MNEMONICS[93] = "dup2_x1";
		OPCODE_MNEMONICS[94] = "dup2_x2";
		OPCODE_MNEMONICS[95] = "swap";
		OPCODE_MNEMONICS[96] = "iadd";
		OPCODE_MNEMONICS[97] = "ladd";
		OPCODE_MNEMONICS[98] = "fadd";
		OPCODE_MNEMONICS[99] = "dadd";
		OPCODE_MNEMONICS[100] = "isub";
		OPCODE_MNEMONICS[101] = "lsub";
		OPCODE_MNEMONICS[102] = "fsub";
		OPCODE_MNEMONICS[103] = "dsub";
		OPCODE_MNEMONICS[104] = "imul";
		OPCODE_MNEMONICS[105] = "lmul";
		OPCODE_MNEMONICS[106] = "fmul";
		OPCODE_MNEMONICS[107] = "dmul";
		OPCODE_MNEMONICS[108] = "idiv";
		OPCODE_MNEMONICS[109] = "ldiv";
		OPCODE_MNEMONICS[110] = "fdiv";
		OPCODE_MNEMONICS[111] = "ddiv";
		OPCODE_MNEMONICS[112] = "irem";
		OPCODE_MNEMONICS[113] = "lrem";
		OPCODE_MNEMONICS[114] = "frem";
		OPCODE_MNEMONICS[115] = "drem";
		OPCODE_MNEMONICS[116] = "ineg";
		OPCODE_MNEMONICS[117] = "lneg";
		OPCODE_MNEMONICS[118] = "fneg";
		OPCODE_MNEMONICS[119] = "dneg";
		OPCODE_MNEMONICS[120] = "ishl";
		OPCODE_MNEMONICS[121] = "lshl";
		OPCODE_MNEMONICS[122] = "ishr";
		OPCODE_MNEMONICS[123] = "lshr";
		OPCODE_MNEMONICS[124] = "iushr";
		OPCODE_MNEMONICS[125] = "lushr";
		OPCODE_MNEMONICS[126] = "iand";
		OPCODE_MNEMONICS[127] = "land";
		OPCODE_MNEMONICS[128] = "ior";
		OPCODE_MNEMONICS[129] = "lor";
		OPCODE_MNEMONICS[130] = "ixor";
		OPCODE_MNEMONICS[131] = "lxor";
		OPCODE_MNEMONICS[132] = "iinc";
		OPCODE_MNEMONICS[133] = "i2l";
		OPCODE_MNEMONICS[134] = "i2f";
		OPCODE_MNEMONICS[135] = "i2d";
		OPCODE_MNEMONICS[136] = "l2i";
		OPCODE_MNEMONICS[137] = "l2f";
		OPCODE_MNEMONICS[138] = "l2d";
		OPCODE_MNEMONICS[139] = "f2i";
		OPCODE_MNEMONICS[140] = "f2l";
		OPCODE_MNEMONICS[141] = "f2d";
		OPCODE_MNEMONICS[142] = "d2i";
		OPCODE_MNEMONICS[143] = "d2l";
		OPCODE_MNEMONICS[144] = "d2f";
		OPCODE_MNEMONICS[145] = "i2b";
		OPCODE_MNEMONICS[146] = "i2c";
		OPCODE_MNEMONICS[147] = "i2s";
		OPCODE_MNEMONICS[148] = "lcmp";
		OPCODE_MNEMONICS[149] = "fcmpl";
		OPCODE_MNEMONICS[150] = "fcmpg";
		OPCODE_MNEMONICS[151] = "dcmpl";
		OPCODE_MNEMONICS[152] = "dcmpg";
		OPCODE_MNEMONICS[153] = "ifeq";
		OPCODE_MNEMONICS[154] = "ifne";
		OPCODE_MNEMONICS[155] = "iflt";
		OPCODE_MNEMONICS[156] = "ifge";
		OPCODE_MNEMONICS[157] = "ifgt";
		OPCODE_MNEMONICS[158] = "ifle";
		OPCODE_MNEMONICS[159] = "if_icmpeq";
		OPCODE_MNEMONICS[160] = "if_icmpne";
		OPCODE_MNEMONICS[161] = "if_icmplt";
		OPCODE_MNEMONICS[162] = "if_icmpge";
		OPCODE_MNEMONICS[163] = "if_icmpgt";
		OPCODE_MNEMONICS[164] = "if_icmple";
		OPCODE_MNEMONICS[165] = "if_acmpeq";
		OPCODE_MNEMONICS[166] = "if_acmpne";
		OPCODE_MNEMONICS[167] = "goto";
		OPCODE_MNEMONICS[168] = "jsr";
		OPCODE_MNEMONICS[169] = "ret";
		OPCODE_MNEMONICS[170] = "tableswitch";
		OPCODE_MNEMONICS[171] = "lookupswitch";
		OPCODE_MNEMONICS[172] = "ireturn";
		OPCODE_MNEMONICS[173] = "lreturn";
		OPCODE_MNEMONICS[174] = "freturn";
		OPCODE_MNEMONICS[175] = "dreturn";
		OPCODE_MNEMONICS[176] = "areturn";
		OPCODE_MNEMONICS[177] = "return";
		OPCODE_MNEMONICS[178] = "getstatic";
		OPCODE_MNEMONICS[179] = "putstatic";
		OPCODE_MNEMONICS[180] = "getfield";
		OPCODE_MNEMONICS[181] = "putfield";
		OPCODE_MNEMONICS[182] = "invokevirtual";
		OPCODE_MNEMONICS[183] = "invokespecial";
		OPCODE_MNEMONICS[184] = "invokestatic";
		OPCODE_MNEMONICS[185] = "invokeinterface";
		OPCODE_MNEMONICS[186] = "invokedynamic";
		OPCODE_MNEMONICS[187] = "new";
		OPCODE_MNEMONICS[188] = "newarray";
		OPCODE_MNEMONICS[189] = "anewarray";
		OPCODE_MNEMONICS[190] = "arraylength";
		OPCODE_MNEMONICS[191] = "athrow";
		OPCODE_MNEMONICS[192] = "checkcast";
		OPCODE_MNEMONICS[193] = "instanceof";
		OPCODE_MNEMONICS[194] = "monitorenter";
		OPCODE_MNEMONICS[195] = "monitorexit";
		OPCODE_MNEMONICS[196] = "wide";
		OPCODE_MNEMONICS[197] = "multianewarray";
		OPCODE_MNEMONICS[198] = "ifnull";
		OPCODE_MNEMONICS[199] = "ifnonnull";
		OPCODE_MNEMONICS[200] = "goto_w";
		OPCODE_MNEMONICS[201] = "jsr_w";
	}
}

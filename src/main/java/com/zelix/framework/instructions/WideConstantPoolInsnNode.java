package com.zelix.framework.instructions;

import com.zelix.exceptions.FileException;
import com.zelix.exceptions.InvalidBytecodeException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.WritableType;
import com.zelix.framework.analyzer.AnalyzeStackDebugContext;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.LocalsInitializationState;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.constant.*;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.listener.*;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.NestedMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.dataset.PriorityEntry;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class WideConstantPoolInsnNode
	extends AbstractInsnNode
	implements LongConstants,
	IntegerConstants,
	WritableType,
	StringConstantUpdateListener,
	ReferenceUpdateListener,
	IDynamicCallUpdatable,
	TypeNode {
	public TypeWriter constantOperand;

	public WideConstantPoolInsnNode(
		int opcode,
		ByteDataBuffer dataBuffer,
		ConstantPool constantPool,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences
	) throws IOException {
		super(opcode);
		int constantPoolIndex = dataBuffer.readUnsignedShort();
		this.constantOperand = constantPool.getConstantPoolEntry(constantPoolIndex);
		switch (this.constantOperand.getTypeTag()) {
			case CLASS:
				classReferences.put((ClassInfoWriter)this.constantOperand, this);
				break;
			case STRING:
				stringReferences.put((StringNode)this.constantOperand, this);
				break;
			case INTEGER:
				integerReferences.put((IntegerNode)this.constantOperand, this);
				break;
			case LONG:
				longReferences.put((LongNode)this.constantOperand, this);
				break;
			case FIELDREF:
			case METHODREF:
			case INTERFACE_METHODREF:
				memberReferences.put((MemberTypeWriter)this.constantOperand, this);
		}
	}

	public WideConstantPoolInsnNode(int opcode, TypeWriter constantOperand) {
		super(opcode);
		this.constantOperand = constantOperand;
	}

	@Override
	public int getStackEffect() {
		switch (this.opcode) {
			case LDC_W:
				return 1;
			case LDC2_W:
				return 2;
			case GETSTATIC:
				return TypeWriter.isDoubleSlotType(this.getFieldStackType()) ? 2 : 1;
			case PUTSTATIC:
				return TypeWriter.isDoubleSlotType(this.getFieldStackType()) ? -2 : -1;
			case GETFIELD:
				return (TypeWriter.isDoubleSlotType(this.getFieldStackType()) ? 2 : 1) - 1;
			case PUTFIELD:
				return (TypeWriter.isDoubleSlotType(this.getFieldStackType()) ? -2 : -1) - 1;
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
				return this.getInvokeStackEffect();
			case INVOKEDYNAMIC:
				return this.getInvokeDynamicStackEffect();
			case NEW:
				return 1;
			default:
				return 0;
		}
	}

	private String getFieldStackType() {
		return ((FieldTypeWriter)this.constantOperand).getStackType();
	}

	private int getInvokeStackEffect() {
		AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)this.constantOperand;
		int stackEffect = this.opcode == INVOKESTATIC ? 0 : -1;
		String returnType = methodCallWriter.getStackReturnType();
		stackEffect += returnType == null ? 0 : (TypeWriter.isDoubleSlotType(returnType) ? 2 : 1);

		for (String parameterType : methodCallWriter.getStackParameterTypes()) {
			stackEffect += TypeWriter.isDoubleSlotType(parameterType) ? -2 : -1;
		}

		return stackEffect;
	}

	private int getInvokeDynamicStackEffect() {
		NameAndTypeWriter target = ((InvokeDynamicCallWriter)this.constantOperand).getTarget();
		String returnType = TypeWriter.getStackReturnType(target);
		int stackEffect = returnType == null ? 0 : (TypeWriter.isDoubleSlotType(returnType) ? 2 : 1);

		for (String parameterType : TypeWriter.getStackParameterTypes(target)) {
			stackEffect += TypeWriter.isDoubleSlotType(parameterType) ? -2 : -1;
		}

		return stackEffect;
	}

	@Override
	public boolean isFieldOpcode() {
		switch (this.opcode) {
			case GETSTATIC:
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder lineBuffer = new StringBuilder(100);
		lineBuffer.append((CharSequence)indent);
		lineBuffer.append((CharSequence)indent);
		lineBuffer.append(this.getOpcodeMnemonics());
		lineBuffer.append(' ');
		lineBuffer.append(this.constantOperand.toLoggableString());
		String commentText = AbstractInsnNode.fillCommentPlaceholder(this.getOpcodeComment(), this.constantOperand.toLoggableString());
		if (commentText.length() > 0) {
			lineBuffer.append('\t');
			lineBuffer.append(commentText);
		}

		printWriter.println(lineBuffer);
	}

	@Override
	public final boolean isMethodExit() {
		return false;
	}

	@Override
	public final void updateIntegerConstant(IntegerNode oldReference, IntegerNode newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		new AnalyzeStackDebugContext(methodSignature);
		StackFrameType[] frameLocalVariables = frame.getLocalVariables();
		StackFrameType[] frameOperandStack = frame.getOperandStack();
		int stackSize = frameOperandStack.length;
		LocalsInitializationState localsInitState = frame.getLocalsInitState();
		Set<MonitorEnterNode> activeMonitors = frame.getActiveMonitors();
		switch (this.opcode) {
			case LDC_W:
				StackFrameType[] ldcOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
				System.arraycopy(frameOperandStack, 0, ldcOperandStack, 0, stackSize);
				if (this.constantOperand instanceof ConstantNode) {
					String constantTypeName = ((ConstantNode)this.constantOperand).getTypeName();
					if (constantTypeName.equals("integer")) {
						ldcOperandStack[stackSize] = StackFrameType.INT;
					} else if (constantTypeName.equals("string")) {
						ldcOperandStack[stackSize] = StackFrameType.STRING;
					} else if (constantTypeName.equals("float")) {
						ldcOperandStack[stackSize] = StackFrameType.FLOAT;
					}
				} else if (this.constantOperand instanceof ClassInfoWriter) {
					ldcOperandStack[stackSize] = StackFrameType.CLASS;
				} else if (this.constantOperand instanceof MethodDescriptorWriter) {
					ldcOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
					System.arraycopy(frameOperandStack, 0, ldcOperandStack, 0, stackSize);
					ldcOperandStack[stackSize] = StackFrameType.getResolved("Ljava/lang/invoke/MethodType;");
				} else if (this.constantOperand instanceof ResolvedMethodHandleWriter) {
					ldcOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
					System.arraycopy(frameOperandStack, 0, ldcOperandStack, 0, stackSize);
					ldcOperandStack[stackSize] = StackFrameType.getResolved("Ljava/lang/invoke/MethodHandle;");
				} else if (this.constantOperand instanceof DynamicConstantWriter) {
					ldcOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
					System.arraycopy(frameOperandStack, 0, ldcOperandStack, 0, stackSize);
					String dynamicReturnType = ((DynamicConstantWriter)this.constantOperand).getStackReturnType();
					ldcOperandStack[stackSize] = StackFrameType.getResolved(dynamicReturnType);
				}

				return new FrameNode(ldcOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case LDC2_W:
				StackFrameType[] ldc2OperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
				System.arraycopy(frameOperandStack, 0, ldc2OperandStack, 0, stackSize);
				String wideConstantTypeName = ((ConstantNode)this.constantOperand).getTypeName();
				if (wideConstantTypeName.equals("long")) {
					ldc2OperandStack[stackSize] = StackFrameType.LONG;
				} else if (wideConstantTypeName.equals("double")) {
					ldc2OperandStack[stackSize] = StackFrameType.DOUBLE;
				}

				return new FrameNode(ldc2OperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case GETSTATIC:
				StackFrameType[] getstaticOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
				System.arraycopy(frameOperandStack, 0, getstaticOperandStack, 0, stackSize);
				FieldTypeWriter staticFieldWriter = (FieldTypeWriter)this.constantOperand;
				getstaticOperandStack[stackSize] = StackFrameType.getResolved(staticFieldWriter.getStackType());
				return new FrameNode(getstaticOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case PUTSTATIC:
				StackFrameType[] putstaticOperandStack = StackFrameType.createEmptyFrame(stackSize - 1);
				System.arraycopy(frameOperandStack, 0, putstaticOperandStack, 0, stackSize - 1);
				return new FrameNode(putstaticOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case GETFIELD:
				StackFrameType[] getfieldOperandStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(frameOperandStack, 0, getfieldOperandStack, 0, stackSize - 1);
				FieldTypeWriter instanceFieldWriter = (FieldTypeWriter)this.constantOperand;
				getfieldOperandStack[stackSize - 1] = StackFrameType.getResolved(instanceFieldWriter.getStackType());
				return new FrameNode(getfieldOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case PUTFIELD:
				StackFrameType[] putfieldOperandStack = StackFrameType.createEmptyFrame(stackSize - 2);
				System.arraycopy(frameOperandStack, 0, putfieldOperandStack, 0, stackSize - 2);
				if (checkTypes) {
					FieldTypeWriter assignedFieldWriter = (FieldTypeWriter)this.constantOperand;
					if (!FrameNode.isAssignable(frameOperandStack[stackSize - 1], StackFrameType.getResolved(assignedFieldWriter.getStackType()), typeResolver, methodSignature)) {
					}
				}

				return new FrameNode(putfieldOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case INVOKEVIRTUAL:
			case INVOKEINTERFACE:
				return this.analyzeInvokeStack(frameOperandStack, stackSize, frameLocalVariables, localsInitState, activeMonitors, true, false, checkTypes, unused, typeResolver, methodSignature);
			case INVOKESPECIAL:
				return this.analyzeInvokeStack(frameOperandStack, stackSize, frameLocalVariables, localsInitState, activeMonitors, true, true, checkTypes, unused, typeResolver, methodSignature);
			case INVOKESTATIC:
				return this.analyzeInvokeStack(frameOperandStack, stackSize, frameLocalVariables, localsInitState, activeMonitors, false, false, checkTypes, unused, typeResolver, methodSignature);
			case INVOKEDYNAMIC:
				return this.analyzeInvokeDynamicStack(frameOperandStack, stackSize, frameLocalVariables, localsInitState, activeMonitors, checkTypes, unused, typeResolver, methodSignature);
			case NEW:
				StackFrameType[] newObjectOperandStack = StackFrameType.createEmptyFrame(stackSize + 1);
				System.arraycopy(frameOperandStack, 0, newObjectOperandStack, 0, stackSize);
				ClassInfoWriter instantiatedClassWriter = (ClassInfoWriter)this.constantOperand;
				newObjectOperandStack[stackSize] = StackFrameType.getInstance(instantiatedClassWriter.getDescriptor(), false, (NewNode)this);
				return new FrameNode(newObjectOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case ANEWARRAY:
				StackFrameType[] arrayOperandStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(frameOperandStack, 0, arrayOperandStack, 0, stackSize - 1);
				ClassInfoWriter elementClassWriter = (ClassInfoWriter)this.constantOperand;
				arrayOperandStack[stackSize - 1] = StackFrameType.getResolved("[" + elementClassWriter.getDescriptor());
				return new FrameNode(arrayOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case CHECKCAST:
				StackFrameType[] checkcastOperandStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(frameOperandStack, 0, checkcastOperandStack, 0, stackSize - 1);
				ClassInfoWriter castTargetClassWriter = (ClassInfoWriter)this.constantOperand;
				checkcastOperandStack[stackSize - 1] = StackFrameType.getResolved(castTargetClassWriter.getDescriptor());
				return new FrameNode(checkcastOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			case INSTANCEOF:
				StackFrameType[] instanceofOperandStack = StackFrameType.createEmptyFrame(stackSize);
				System.arraycopy(frameOperandStack, 0, instanceofOperandStack, 0, stackSize - 1);
				instanceofOperandStack[stackSize - 1] = StackFrameType.INT;
				return new FrameNode(instanceofOperandStack, frameLocalVariables, localsInitState, activeMonitors);
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return null;
		}
	}

	private FrameNode analyzeInvokeStack(
		StackFrameType[] operandStack,
		int stackSize,
		StackFrameType[] localVariables,
		LocalsInitializationState localsInitState,
		Set<MonitorEnterNode> activeMonitors,
		boolean hasReceiver,
		boolean isInvokeSpecial,
		boolean checkTypes,
		boolean unused,
		TypeResolver typeResolver,
		String methodSignature
	) {
		AnalyzeStackDebugContext debugContext = new AnalyzeStackDebugContext(methodSignature);
		AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)this.constantOperand;
		List<String> parameterTypes = methodCallWriter.getStackParameterTypes();
		int parameterCount = parameterTypes.size();
		String returnType = methodCallWriter.getStackReturnType();
		int returnSize = returnType == null ? 0 : 1;
		if (checkTypes) {
			for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
				StackFrameType actualParameterType = operandStack[stackSize - parameterCount + parameterIndex];
				StackFrameType expectedParameterType = StackFrameType.getResolved(parameterTypes.get(parameterIndex));

				try {
					if (!FrameNode.isAssignable(actualParameterType, expectedParameterType, typeResolver, methodSignature)) {
						throw new InvalidBytecodeException(
							"Type mismatch at method invocation : "
								+ actualParameterType.getDescriptor()
								+ " not assignment compatible with "
								+ expectedParameterType.getDescriptor()
								+ " :  parameter "
								+ parameterIndex
								+ " calling "
								+ methodCallWriter.toDisplayString()
								+ " in class "
								+ methodCallWriter.getOwnerRawSourcePath()
								+ " while "
								+ debugContext
						);
					}
				} catch (FileException ignored) {
				}
			}
		}

		int newStackSize = stackSize - parameterCount - (hasReceiver ? 1 : 0) + returnSize;
		if (newStackSize - returnSize < 0) {
			throw new InvalidBytecodeException("Stack underflow : " + newStackSize + " in class " + methodCallWriter.getOwnerRawSourcePath() + " while " + debugContext);
		} else {
			StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(newStackSize);
			System.arraycopy(operandStack, 0, newOperandStack, 0, newStackSize - returnSize);
			if (returnSize == 1) {
				newOperandStack[newOperandStack.length - 1] = StackFrameType.getResolved(returnType);
			}

			if (isInvokeSpecial && methodCallWriter.isConstructorCall()) {
				StackFrameType uninitializedObject = operandStack[stackSize - parameterCount - 1];
				StackFrameType initializedObjectType = uninitializedObject.toResolved();
				int localVariableCount = localVariables.length;
				StackFrameType[] updatedLocalVariables = StackFrameType.createEmptyFrame(localVariableCount);
				System.arraycopy(localVariables, 0, updatedLocalVariables, 0, localVariableCount);

				for (int stackSlotIndex = 0; stackSlotIndex < newStackSize; stackSlotIndex++) {
					if (newOperandStack[stackSlotIndex] == uninitializedObject) {
						newOperandStack[stackSlotIndex] = initializedObjectType;
					}
				}

				for (int localSlotIndex = 0; localSlotIndex < localVariableCount; localSlotIndex++) {
					if (updatedLocalVariables[localSlotIndex] == uninitializedObject) {
						updatedLocalVariables[localSlotIndex] = initializedObjectType;
					}
				}

				return new FrameNode(newOperandStack, updatedLocalVariables, localsInitState, activeMonitors);
			} else {
				return new FrameNode(newOperandStack, localVariables, localsInitState, activeMonitors);
			}
		}
	}

	@Override
	public boolean isIntegerConstant2() {
		return this.opcode == LDC_W && this.constantOperand instanceof IntegerNode;
	}

	@Override
	public void updateLongConstant(LongNode oldReference, LongNode newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public final void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public AbstractInsnNode optimizeToShortLdc(Map<TypeWriter, TypeWriter> typeWriterMapping) {
		if (this.opcode == LDC_W) {
			TypeWriter mappedWriter = GlobalUtils.getMappedOrDefault(this.constantOperand, typeWriterMapping);
			TypeWriter effectiveWriter;
			if (mappedWriter != null) {
				effectiveWriter = mappedWriter;
			} else {
				effectiveWriter = this.constantOperand;
			}

			return effectiveWriter.getTag() <= 255 ? new LdcNode(this.constantOperand) : null;
		} else {
			return null;
		}
	}

	@Override
	public final boolean isStackProducer() {
		switch (this.opcode) {
			case LDC_W:
			case LDC2_W:
			case GETSTATIC:
			case GETFIELD:
			case NEW:
			case ANEWARRAY:
			case INSTANCEOF:
			case MULTIANEWARRAY:
				return true;
			case PUTSTATIC:
			case PUTFIELD:
			case CHECKCAST:
				return false;
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
				return ((AbstractMethodCallWriter)this.constantOperand).getStackReturnType() != null;
			case INVOKEDYNAMIC:
				return ((InvokeDynamicCallWriter)this.constantOperand).getStackReturnType() != null;
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return false;
		}
	}

	@Override
	public final void updateDynamicCall(InvokeDynamicCallWriter oldReference, InvokeDynamicCallWriter newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public boolean isPutField() {
		switch (this.opcode) {
			case PUTSTATIC:
			case PUTFIELD:
				return true;
			default:
				return false;
		}
	}

	@Override
	public TypeWriter getTypeWriter() {
		return this.constantOperand;
	}

	@Override
	public boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		switch (this.opcode) {
			case LDC_W:
			case LDC2_W:
			case GETSTATIC:
			case NEW:
				return false;
			case PUTSTATIC:
			case PUTFIELD:
				return stackIndex >= stackSize;
			case GETFIELD:
			case ANEWARRAY:
				return stackIndex >= stackSize - 1;
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
				int returnStackSize;
				if (this.opcode == INVOKEDYNAMIC) {
					returnStackSize = ((InvokeDynamicCallWriter)this.constantOperand).getReturnStackSize();
				} else {
					returnStackSize = ((AbstractMethodCallWriter)this.constantOperand).getReturnStackSize();
				}

				if (stackIndex >= stackSize - returnStackSize) {
					return !stackType.getDescriptor().equals("Ljava/lang/Object;");
				}

				return false;
			case CHECKCAST:
			case INSTANCEOF:
				return false;
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return false;
		}
	}

	@Override
	public final boolean isInvoke() {
		switch (this.opcode) {
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
				return true;
			default:
				return false;
		}
	}

	@Override
	public final boolean isCategory2Producer() {
		switch (this.opcode) {
			case LDC_W:
			case PUTSTATIC:
			case PUTFIELD:
			case NEW:
			case ANEWARRAY:
			case CHECKCAST:
			case INSTANCEOF:
			case MULTIANEWARRAY:
				return false;
			case LDC2_W:
				return true;
			case GETSTATIC:
			case GETFIELD:
				String fieldStackType = ((FieldTypeWriter)this.constantOperand).getStackType();
				return fieldStackType != null && (fieldStackType.equals("J") || fieldStackType.equals("D"));
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
				String methodReturnType = ((AbstractMethodCallWriter)this.constantOperand).getStackReturnType();
				return methodReturnType != null && (methodReturnType.equals("J") || methodReturnType.equals("D"));
			case INVOKEDYNAMIC:
				String dynamicReturnType = ((InvokeDynamicCallWriter)this.constantOperand).getStackReturnType();
				return dynamicReturnType != null && (dynamicReturnType.equals("J") || dynamicReturnType.equals("D"));
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return false;
		}
	}

	@Override
	public boolean isLong() {
		return this.opcode == LDC2_W && this.constantOperand instanceof LongNode;
	}

	@Override
	public int getBytecodeSize() {
		return 3;
	}

	@Override
	public boolean isIntegerConstant() {
		return this.opcode == LDC_W && this.constantOperand instanceof IntegerNode;
	}

	@Override
	public boolean isStandaloneStackProducer() {
		switch (this.opcode) {
			case LDC_W:
			case LDC2_W:
			case GETSTATIC:
			case NEW:
				return true;
			case PUTSTATIC:
			case GETFIELD:
			case PUTFIELD:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKEINTERFACE:
			case INVOKEDYNAMIC:
			case ANEWARRAY:
			case CHECKCAST:
			case INSTANCEOF:
			case MULTIANEWARRAY:
				return false;
			case INVOKESTATIC:
				AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)this.constantOperand;
				return methodCallWriter.getStackReturnType() != null && methodCallWriter.getStackParameterTypes().size() == 0;
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return false;
		}
	}

	@Override
	public boolean collectStringObfCandidate(
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries, Set<StringNode> excludedStrings, MethodNode methodNode, int instructionIndex
	) {
		if (this.opcode == LDC_W && this.constantOperand instanceof StringNode) {
			StringNode stringConstant = (StringNode)this.constantOperand;
			if (stringConstant.getUtf8StringWriter().getLength() >= 2 && stringConstant.isNeedStringObf() && !excludedStrings.contains(stringConstant)) {
				stringConstantEntries.put(methodNode.getMethodOwner(), methodNode, stringConstant, new PriorityEntry<>(instructionIndex, this));
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public void writeWithConstantMap(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> typeWriterMapping) throws IOException {
		super.writeToStream(dataOutputStream);
		TypeWriter mappedWriter = typeWriterMapping.get(this.constantOperand);
		if (mappedWriter != null) {
			dataOutputStream.writeShort(mappedWriter.getTag());
		} else {
			dataOutputStream.writeShort(this.constantOperand.getTag());
		}
	}

	@Override
	public boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		switch (this.opcode) {
			case LDC_W:
			case LDC2_W:
			case GETSTATIC:
			case NEW:
				return false;
			case PUTSTATIC:
			case PUTFIELD:
				return stackIndex >= stackSize;
			case GETFIELD:
			case ANEWARRAY:
			case CHECKCAST:
			case INSTANCEOF:
				return stackIndex >= stackSize - 1;
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKESTATIC:
			case INVOKEINTERFACE:
				return stackIndex >= stackSize - ((AbstractMethodCallWriter)this.constantOperand).getReturnStackSize();
			case INVOKEDYNAMIC:
				return stackIndex >= stackSize - ((InvokeDynamicCallWriter)this.constantOperand).getReturnStackSize();
			default:
				DebugUtils.assertTrue(false, new String[]{"invalid opcode " + this.opcode});
				return false;
		}
	}

	@Override
	public final boolean canFallThrough() {
		return true;
	}

	@Override
	public boolean isString() {
		return this.opcode == LDC_W && this.constantOperand instanceof StringNode;
	}

	@Override
	public final void updateStringConstant(StringNode oldReference, StringNode newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public boolean collectLongObfCandidate(
		NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longConstantEntries, Set<LongNode> excludedLongs, MethodNode methodNode, int instructionIndex
	) {
		if (this.opcode == LDC2_W && this.constantOperand instanceof LongNode) {
			LongNode longConstant = (LongNode)this.constantOperand;
			if (longConstant.isNeedLongObf() && !excludedLongs.contains(longConstant)) {
				longConstantEntries.put(methodNode.getMethodOwner(), methodNode, longConstant, new PriorityEntry<>(instructionIndex, this));
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public final void updateMemberReference(MemberTypeWriter oldReference, MemberTypeWriter newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeShort(this.constantOperand.getTag());
	}

	@Override
	public boolean collectIntObfCandidate(
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries, Set<IntegerNode> excludedIntegers, MethodNode methodNode, int instructionIndex
	) {
		if (this.opcode == LDC_W && this.constantOperand instanceof IntegerNode) {
			IntegerNode intConstant = (IntegerNode)this.constantOperand;
			if (intConstant.isNeedIntObf() && !excludedIntegers.contains(intConstant)) {
				integerConstantEntries.put(methodNode.getMethodOwner(), methodNode, intConstant, new PriorityEntry<>(instructionIndex, this));
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public String getOpcodeText() {
		StringBuilder textBuffer = new StringBuilder();
		textBuffer.append(this.getOpcodeMnemonics());
		textBuffer.append(' ');
		textBuffer.append(this.constantOperand.toLoggableString());
		return textBuffer.toString();
	}

	private FrameNode analyzeInvokeDynamicStack(
		StackFrameType[] operandStack,
		int stackSize,
		StackFrameType[] localVariables,
		LocalsInitializationState localsInitState,
		Set<MonitorEnterNode> activeMonitors,
		boolean checkTypes,
		boolean unused,
		TypeResolver typeResolver,
		String methodSignature
	) {
		AnalyzeStackDebugContext debugContext = new AnalyzeStackDebugContext(methodSignature);
		InvokeDynamicCallWriter invokeDynamicWriter = (InvokeDynamicCallWriter)this.constantOperand;
		ResolvedMethodHandleWriter methodHandleWriter = invokeDynamicWriter.getBootstrapMethodHandle();
		if (!methodHandleWriter.getKind().equals(MethodHandleKind.REF_INVOKE_STATIC)) {
			throw new InvalidBytecodeException(this.getOpcodeMnemonics() + " " + methodHandleWriter.getKind().toString() + " (A)");
		} else {
			invokeDynamicWriter.getBootstrapArguments();
			NameAndTypeWriter nameAndTypeWriter = invokeDynamicWriter.getTarget();
			List<String> parameterTypes = TypeWriter.getStackParameterTypes(nameAndTypeWriter);
			int parameterCount = parameterTypes.size();
			String returnType = TypeWriter.getStackReturnType(nameAndTypeWriter);
			int returnSize = returnType == null ? 0 : 1;
			if (checkTypes) {
				for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
					StackFrameType actualParameterType = operandStack[stackSize - parameterCount + parameterIndex];
					StackFrameType expectedParameterType = StackFrameType.getResolved(parameterTypes.get(parameterIndex));

					try {
						if (!FrameNode.isAssignable(actualParameterType, expectedParameterType, typeResolver, methodSignature)) {
							throw new InvalidBytecodeException(
								"Type mismatch at dynamic method invocation : "
									+ actualParameterType.getDescriptor()
									+ " not assignment compatible with "
									+ expectedParameterType.getDescriptor()
									+ " :  parameter "
									+ parameterIndex
									+ " calling "
									+ nameAndTypeWriter.toDisplayString()
									+ " while "
									+ debugContext
							);
						}
					} catch (FileException ignored) {
					}
				}
			}

			int newStackSize = stackSize - parameterCount + returnSize;
			if (newStackSize - returnSize < 0) {
				throw new InvalidBytecodeException("Stack underflow in dynamic method invocation : " + newStackSize + " while " + debugContext);
			} else {
				StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(newStackSize);
				System.arraycopy(operandStack, 0, newOperandStack, 0, newStackSize - returnSize);
				if (returnSize == 1) {
					newOperandStack[newOperandStack.length - 1] = StackFrameType.getResolved(returnType);
				}

				return new FrameNode(newOperandStack, localVariables, localsInitState, activeMonitors);
			}
		}
	}
}

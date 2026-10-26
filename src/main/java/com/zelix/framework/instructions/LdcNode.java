package com.zelix.framework.instructions;

import com.zelix.framework.WritableType;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.StackFrameType;
import com.zelix.framework.constant.ConstantNode;
import com.zelix.framework.constant.ConstantPool;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.DynamicConstantWriter;
import com.zelix.framework.writer.MethodDescriptorWriter;
import com.zelix.framework.writer.ResolvedMethodHandleWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.NestedMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.dataset.PriorityEntry;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.LDC;
import static com.zelix.framework.instructions.OpcodeConstants.LDC_W;

public class LdcNode extends AbstractInsnNode implements IntegerConstants, WritableType, StringConstantUpdateListener, ReferenceUpdateListener {
	private TypeWriter constantOperand;

	public LdcNode(TypeWriter constantOperand) {
		super(LDC);
		this.constantOperand = constantOperand;
	}

	public LdcNode(
		ByteDataBuffer dataBuffer,
		ConstantPool constantPool,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) throws IOException {
		super(LDC);
		int constantPoolIndex = dataBuffer.read();
		this.constantOperand = constantPool.getConstantPoolEntry(constantPoolIndex);
		if (this.constantOperand instanceof StringNode) {
			stringReferences.put((StringNode)this.constantOperand, this);
		} else if (this.constantOperand instanceof IntegerNode) {
			integerReferences.put((IntegerNode)this.constantOperand, this);
		} else if (this.constantOperand instanceof ClassInfoWriter) {
			classReferences.put((ClassInfoWriter)this.constantOperand, this);
		}
	}

	@Override
	public int getStackEffect() {
		return 1;
	}

	@Override
	public void printInsn(PrintWriter printWriter, StringBuilder indent) {
		StringBuilder textBuffer = new StringBuilder(100);
		String mnemonics = this.getOpcodeMnemonics();
		textBuffer.append(mnemonics + " " + this.constantOperand.toLoggableString());
		String comment = AbstractInsnNode.fillCommentPlaceholder(AbstractInsnNode.getOpcodeComment(this.opcode), this.constantOperand.toLoggableString());
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
	public void updateIntegerConstant(IntegerNode oldReference, IntegerNode newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public FrameNode analyzeStack(FrameNode frame, boolean checkTypes, boolean unused, TypeResolver typeResolver, String methodSignature) {
		StackFrameType[] operandStack = frame.getOperandStack();
		int operandStackSize = operandStack.length;
		StackFrameType[] newOperandStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
		StackFrameType[] localVariables = frame.getLocalVariables();
		System.arraycopy(operandStack, 0, newOperandStack, 0, operandStackSize);
		if (this.constantOperand instanceof ConstantNode) {
			String typeName = ((ConstantNode)this.constantOperand).getTypeName();
			if (typeName.equals("integer")) {
				newOperandStack[operandStackSize] = StackFrameType.INT;
			} else if (typeName.equals("string")) {
				newOperandStack[operandStackSize] = StackFrameType.STRING;
			} else if (typeName.equals("float")) {
				newOperandStack[operandStackSize] = StackFrameType.FLOAT;
			}
		} else if (this.constantOperand instanceof ClassInfoWriter) {
			newOperandStack[operandStackSize] = StackFrameType.CLASS;
		} else if (this.constantOperand instanceof MethodDescriptorWriter) {
			newOperandStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
			System.arraycopy(operandStack, 0, newOperandStack, 0, operandStackSize);
			newOperandStack[operandStackSize] = StackFrameType.getResolved("Ljava/lang/invoke/MethodType;");
		} else if (this.constantOperand instanceof ResolvedMethodHandleWriter) {
			newOperandStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
			System.arraycopy(operandStack, 0, newOperandStack, 0, operandStackSize);
			newOperandStack[operandStackSize] = StackFrameType.getResolved("Ljava/lang/invoke/MethodHandle;");
		} else if (this.constantOperand instanceof DynamicConstantWriter) {
			newOperandStack = StackFrameType.createEmptyFrame(operandStackSize + 1);
			System.arraycopy(operandStack, 0, newOperandStack, 0, operandStackSize);
			String returnTypeDescriptor = ((DynamicConstantWriter)this.constantOperand).getStackReturnType();
			newOperandStack[operandStackSize] = StackFrameType.getResolved(returnTypeDescriptor);
		}

		return new FrameNode(newOperandStack, localVariables, frame.getLocalsInitState(), frame.getActiveMonitors());
	}

	@Override
	public boolean isIntegerConstant2() {
		return this.constantOperand instanceof IntegerNode;
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public AbstractInsnNode optimizeToShortLdc(Map<TypeWriter, TypeWriter> typeWriterMapping) {
		TypeWriter mappedTypeWriter = GlobalUtils.getMappedOrDefault(this.constantOperand, typeWriterMapping);
		TypeWriter resolvedTypeWriter;
		if (mappedTypeWriter != null) {
			resolvedTypeWriter = mappedTypeWriter;
		} else {
			resolvedTypeWriter = this.constantOperand;
		}

		return resolvedTypeWriter.getTag() > 255 ? new WideConstantPoolInsnNode(LDC_W, this.constantOperand) : null;
	}

	@Override
	public boolean isStackProducer() {
		return true;
	}

	@Override
	public TypeWriter getTypeWriter() {
		return this.constantOperand;
	}

	@Override
	public final boolean isStackSlotConsumed(int stackIndex, StackFrameType stackType, int stackSize) {
		return false;
	}

	@Override
	public boolean isCategory2Producer() {
		return false;
	}

	@Override
	public int getBytecodeSize() {
		return 2;
	}

	@Override
	public boolean isIntegerConstant() {
		return this.constantOperand instanceof IntegerNode;
	}

	@Override
	public boolean collectStringObfCandidate(
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries, Set<StringNode> excludedStrings, MethodNode methodNode, int instructionIndex
	) {
		if (this.constantOperand instanceof StringNode) {
			StringNode stringNode = (StringNode)this.constantOperand;
			if (stringNode.getUtf8StringWriter().getLength() >= 2 && stringNode.isNeedStringObf() && !excludedStrings.contains(stringNode)) {
				stringConstantEntries.put(methodNode.getMethodOwner(), methodNode, stringNode, new PriorityEntry<>(instructionIndex, this));
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isStandaloneStackProducer() {
		return true;
	}

	@Override
	public final boolean consumesStackFromIndex(int stackIndex, int stackSize) {
		return false;
	}

	@Override
	public void writeWithConstantMap(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> typeWriterMapping) throws IOException {
		super.writeToStream(dataOutputStream);
		TypeWriter mappedTypeWriter = typeWriterMapping.get(this.constantOperand);
		if (mappedTypeWriter != null) {
			dataOutputStream.writeByte(mappedTypeWriter.getTag());
		} else {
			dataOutputStream.writeByte(this.constantOperand.getTag());
		}
	}

	@Override
	public final boolean canFallThrough() {
		return true;
	}

	@Override
	public boolean isString() {
		return this.constantOperand instanceof StringNode;
	}

	@Override
	public void updateStringConstant(StringNode oldReference, StringNode newReference) {
		if (this.constantOperand == oldReference) {
			this.constantOperand = newReference;
		}
	}

	@Override
	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		super.writeToStream(dataOutputStream);
		dataOutputStream.writeByte(this.constantOperand.getTag());
	}

	@Override
	public boolean collectIntObfCandidate(
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries, Set<IntegerNode> excludedIntegers, MethodNode methodNode, int instructionIndex
	) {
		if (this.constantOperand instanceof IntegerNode) {
			IntegerNode integerNode = (IntegerNode)this.constantOperand;
			if (integerNode.isNeedIntObf() && !excludedIntegers.contains(integerNode)) {
				integerConstantEntries.put(methodNode.getMethodOwner(), methodNode, integerNode, new PriorityEntry<>(instructionIndex, this));
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
}

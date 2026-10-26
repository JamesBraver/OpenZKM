package com.zelix.tools.parameterChanger;

import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.variable.LocalVariableSlot;
import com.zelix.framework.variable.VariableNode;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.parameterChanger.extra.ExtraParameter;
import com.zelix.utils.GlobalUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParameterPattern {
	private final String descriptor;
	private final ExtraParameter[] extraParameters;
	private Map<InstructionListNode, VariableNode[]> variablesByInstruction;

	public ParameterPattern(String descriptor, ExtraParameter[] extraParameters) {
		String returnDescriptor = MethodSignature.extractReturnDescriptor(descriptor);
		List<String> parameterDescriptors = MethodSignature.parseParameterDescriptors(descriptor);
		int parameterCount = parameterDescriptors.size() + extraParameters.length;
		int extraParameterCount = extraParameters.length;
		int extraParameterIndex = 0;
		Iterator<String> parameterDescriptorIterator = parameterDescriptors.iterator();
		StringBuilder descriptorBuffer = new StringBuilder();
		descriptorBuffer.append('(');

		for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
			while (extraParameterIndex < extraParameterCount && extraParameters[extraParameterIndex].getParameterIndex() == parameterIndex) {
				descriptorBuffer.append(extraParameters[extraParameterIndex].getTypeChar());
				extraParameterIndex++;
				parameterIndex++;
			}

			if (parameterDescriptorIterator.hasNext()) {
				String parameterDescriptor = parameterDescriptorIterator.next();
				descriptorBuffer.append(parameterDescriptor);
			}
		}

		descriptorBuffer.append(')');
		descriptorBuffer.append(returnDescriptor);
		this.descriptor = descriptorBuffer.toString();
		this.extraParameters = extraParameters;
	}

	public ParameterPattern(List<String> parameterDescriptors, String returnDescriptor, char[] extraParameterTypes, int[] extraParameterIndexes) {
		this.extraParameters = new ExtraParameter[extraParameterTypes.length];

		for (int i = 0; i < extraParameterTypes.length; i++) {
			this.extraParameters[i] = new ExtraParameter(extraParameterIndexes[i], extraParameterTypes[i]);
		}

		int extraParameterCount = extraParameterTypes.length;
		int parameterCount = parameterDescriptors.size() + extraParameterTypes.length;
		StringBuilder descriptorBuffer = new StringBuilder();
		descriptorBuffer.append('(');
		int extraParameterIndex = 0;
		Iterator<String> parameterDescriptorIterator = parameterDescriptors.iterator();

		for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
			while (extraParameterIndex < extraParameterCount && extraParameterIndexes[extraParameterIndex] == parameterIndex) {
				descriptorBuffer.append(extraParameterTypes[extraParameterIndex]);
				extraParameterIndex++;
				parameterIndex++;
			}

			if (parameterDescriptorIterator.hasNext()) {
				String parameterDescriptor = parameterDescriptorIterator.next();
				descriptorBuffer.append(parameterDescriptor);
			}
		}

		descriptorBuffer.append(')');
		descriptorBuffer.append(returnDescriptor);
		this.descriptor = descriptorBuffer.toString();
	}

	public static int[] computeExtraParameterSlots(ParameterPattern parameterPattern, boolean hasThisParameter) {
		List<String> parameterDescriptors = TypeWriter.parseParameterDescriptors(parameterPattern.descriptor);
		int parameterCount = parameterDescriptors.size();
		int extraParameterCount = parameterPattern.extraParameters.length;
		int[] extraParameterSlots = new int[extraParameterCount];
		int slotIndex = hasThisParameter ? 0 : 1;
		int extraParameterIndex = 0;
		int extraParameterPosition = parameterPattern.extraParameters[0].getParameterIndex();

		for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
			if (parameterIndex == extraParameterPosition) {
				extraParameterSlots[extraParameterIndex] = slotIndex;
				if (++extraParameterIndex < extraParameterCount) {
					extraParameterPosition = parameterPattern.extraParameters[extraParameterIndex].getParameterIndex();
				} else {
					extraParameterPosition = -1;
				}
			}

			String parameterDescriptor = parameterDescriptors.get(parameterIndex);
			slotIndex++;
			if (parameterDescriptor.equals("J") || parameterDescriptor.equals("D")) {
				slotIndex++;
			}
		}

		return extraParameterSlots;
	}

	public static String buildParameterDescriptor(List<String> parameterDescriptors, ExtraParameter[] extraParameters) {
		int parameterCount = parameterDescriptors.size() + extraParameters.length;
		int extraParameterCount = extraParameters.length;
		StringBuilder descriptorBuffer = new StringBuilder();
		int parameterDescriptorIndex = 0;
		int extraParameterIndex = 0;
		ExtraParameter extraParameter;
		if (extraParameters.length == 0) {
			extraParameter = null;
		} else {
			extraParameter = extraParameters[0];
		}

		descriptorBuffer.append('(');

		for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
			if (extraParameter != null && parameterIndex == extraParameter.getParameterIndex()) {
				descriptorBuffer.append(extraParameter.getTypeChar());
				if (++extraParameterIndex < extraParameterCount) {
					extraParameter = extraParameters[extraParameterIndex];
				} else {
					extraParameter = null;
				}
			} else {
				String parameterDescriptor = parameterDescriptors.get(parameterDescriptorIndex);
				descriptorBuffer.append(parameterDescriptor);
				parameterDescriptorIndex++;
			}
		}

		descriptorBuffer.append(')');
		return descriptorBuffer.toString();
	}

	public ExtraParameter getLastExtraParameter() {
		return this.extraParameters[this.extraParameters.length - 1];
	}

	public ExtraParameter getFirstExtraParameter() {
		return this.extraParameters[0];
	}

	public int getCumulativeBitShift(int extraParameterIndex) {
		int cumulativeBitShift;
		if (extraParameterIndex == 0) {
			cumulativeBitShift = 0;
		} else {
			int firstParameterBitSize = this.getParameterBitSize(0);
			if (extraParameterIndex == 1) {
				cumulativeBitShift = firstParameterBitSize;
			} else {
				int secondParameterBitSize = this.getParameterBitSize(1);
				cumulativeBitShift = firstParameterBitSize + secondParameterBitSize;
			}
		}

		return cumulativeBitShift;
	}

	private int getByteExtraParameterCount() {
		int byteParameterCount = 0;
		ExtraParameter[] extraParameters = this.extraParameters;
		int extraParameterCount = extraParameters.length;

		for (int i = 0; i < extraParameterCount; i++) {
			if (extraParameters[i].getTypeChar() == 'B') {
				byteParameterCount++;
			}
		}

		return byteParameterCount;
	}

	public String getDescriptor() {
		return this.descriptor;
	}

	public int getExtraParameterIndex(int extraParameterIndex) {
		return this.extraParameters[extraParameterIndex].getParameterIndex();
	}

	private int getParameterBitSize(int extraParameterIndex) {
		char typeChar = this.extraParameters[extraParameterIndex].getTypeChar();
		int bitSize;
		if (typeChar == 'B') {
			bitSize = 8;
		} else if (typeChar == 'S' || typeChar == 'C') {
			bitSize = 16;
		} else if (typeChar == 'J') {
			bitSize = 64 - (extraParameterIndex == 0 ? this.extraParameters[1].getBitSize() : this.extraParameters[0].getBitSize());
		} else if (this.extraParameters.length == 2) {
			bitSize = 32;
		} else if (extraParameterIndex == 0) {
			bitSize = 32;
		} else {
			int byteParameterCount = this.getByteExtraParameterCount();
			int intParameterCount = this.getIntExtraParameterCount();
			int intParameterIndex = this.getIntExtraParameterIndex();
			if (extraParameterIndex == intParameterIndex) {
				bitSize = 32;
			} else if (intParameterCount == 3) {
				bitSize = 16;
			} else if (byteParameterCount == 1) {
				bitSize = 24;
			} else {
				bitSize = 16;
			}
		}

		return bitSize;
	}

	public boolean hasNoExtraParameters() {
		return this.extraParameters.length == 0;
	}

	public int getLeftShiftAmount(int extraParameterIndex) {
		return 64 - this.getParameterBitSize(extraParameterIndex);
	}

	public boolean hasMultipleExtraParameters() {
		return this.extraParameters.length > 1;
	}

	public int[] getExtraParameterIndexes() {
		int extraParameterCount = this.extraParameters.length;
		int[] extraParameterIndexes = new int[extraParameterCount];

		for (int i = 0; i < extraParameterCount; i++) {
			extraParameterIndexes[i] = this.extraParameters[i].getParameterIndex();
		}

		return extraParameterIndexes;
	}

	public void setParameterVariable(InstructionListNode instructionListNode, int extraParameterIndex, LocalVariableSlot localVariableSlot) {
		if (this.variablesByInstruction == null) {
			this.variablesByInstruction = Options.ZKM_USE_PARALLEL ? new ConcurrentHashMap<>(13) : GlobalUtils.newHashMap(13);
		}

		VariableNode[] variableNodes = this.variablesByInstruction.get(instructionListNode);
		if (variableNodes == null) {
			variableNodes = new VariableNode[this.extraParameters.length];
			this.variablesByInstruction.put(instructionListNode, variableNodes);
		}

		variableNodes[extraParameterIndex] = new VariableNode(localVariableSlot, extraParameterIndex);
	}

	public VariableNode[] getParameterVariables(InstructionListNode instructionListNode) {
		return this.variablesByInstruction.get(instructionListNode);
	}

	public char getExtraParameterTypeChar(int extraParameterIndex) {
		return this.extraParameters[extraParameterIndex].getTypeChar();
	}

	private int getIntExtraParameterIndex() {
		int extraParameterIndex = 0;
		ExtraParameter[] extraParameters = this.extraParameters;
		int extraParameterCount = extraParameters.length;

		for (int i = 0; i < extraParameterCount; i++) {
			if (extraParameters[i].getTypeChar() == 'I') {
				return extraParameterIndex;
			}

			extraParameterIndex++;
		}

		return -1;
	}

	public boolean isLongExtraParameter(int extraParameterIndex) {
		return this.extraParameters[extraParameterIndex].getTypeChar() == 'J';
	}

	public int getRightShiftAmount(int extraParameterIndex) {
		return 64 - this.getParameterBitSize(extraParameterIndex);
	}

	public int getParameterCount() {
		return TypeWriter.parseParameterDescriptors(this.descriptor).size();
	}

	private int getIntExtraParameterCount() {
		int intParameterCount = 0;
		ExtraParameter[] extraParameters = this.extraParameters;
		int extraParameterCount = extraParameters.length;

		for (int i = 0; i < extraParameterCount; i++) {
			if (extraParameters[i].getTypeChar() == 'I') {
				intParameterCount++;
			}
		}

		return intParameterCount;
	}

	public ExtraParameter[] getExtraParameters() {
		return (ExtraParameter[])this.extraParameters.clone();
	}

	public int getPrecedingBitShift(int extraParameterIndex) {
		int precedingBitShift = 0;
		int firstParameterBitSize = this.getParameterBitSize(0);
		if (extraParameterIndex == 1) {
			precedingBitShift = firstParameterBitSize;
		} else if (extraParameterIndex == 2) {
			int secondParameterBitSize = this.getParameterBitSize(1);
			precedingBitShift = firstParameterBitSize + secondParameterBitSize;
		}

		return precedingBitShift;
	}

	public int getExtraParameterCount() {
		return this.extraParameters.length;
	}
}

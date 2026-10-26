package com.zelix.framework.instructions;

import com.zelix.framework.variable.LocalVariablesManager;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public class AutoVarInsnNode extends VarInsnNode {
	public AutoVarInsnNode(int opcode, int wideWrapOpcode, int localVarIndex, ByteDataBuffer dataBuffer, LocalVariablesManager localVariablesManager) throws IOException {
		super(opcode, wideWrapOpcode, localVarIndex, dataBuffer, localVariablesManager);
	}

	@Override
	public int calculateSpecificOpcode() {
		this.targetVariable.getIndex();
		switch (this.operationType) {
			case INT_LOAD:
				return 21;
			case INT_STORE:
				return 54;
			case LONG_LOAD:
				return 22;
			case LONG_STORE:
				return 55;
			case FLOAT_LOAD:
				return 23;
			case FLOAT_STORE:
				return 56;
			case DOUBLE_LOAD:
				return 24;
			case DOUBLE_STORE:
				return 57;
			case OBJECT_LOAD:
				return 25;
			case OBJECT_STORE:
				return 58;
			case INT_INC:
				return 132;
			case ADDRESS:
				return 169;
			default:
				return -1;
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
				dataOutputStream.writeByte(21);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case INT_STORE:
				dataOutputStream.writeByte(54);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case LONG_LOAD:
				dataOutputStream.writeByte(22);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case LONG_STORE:
				dataOutputStream.writeByte(55);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case FLOAT_LOAD:
				dataOutputStream.writeByte(23);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case FLOAT_STORE:
				dataOutputStream.writeByte(56);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case DOUBLE_LOAD:
				dataOutputStream.writeByte(24);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case DOUBLE_STORE:
				dataOutputStream.writeByte(57);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case OBJECT_LOAD:
				dataOutputStream.writeByte(25);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case OBJECT_STORE:
				dataOutputStream.writeByte(58);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
				break;
			case INT_INC:
				dataOutputStream.writeByte(132);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
					dataOutputStream.writeShort(this.getIncrementAmount());
				} else {
					dataOutputStream.writeByte(variableIndex);
					dataOutputStream.writeByte(this.getIncrementAmount());
				}
				break;
			case ADDRESS:
				dataOutputStream.writeByte(169);
				if (this.requiresWidePrefix()) {
					dataOutputStream.writeShort(variableIndex);
				} else {
					dataOutputStream.writeByte(variableIndex);
				}
		}
	}
}

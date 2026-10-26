package com.zelix.framework.analyzer.reference;

import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class ErrorReference implements Reference {
	private AbstractInsnNode insnNode;
	private String errorMessage;

	public ErrorReference(AbstractInsnNode insnNode, String errorMessage) {
		this.insnNode = insnNode;
		this.errorMessage = errorMessage;
	}

	@Override
	public boolean isSingleValue() {
		return false;
	}

	@Override
	public boolean isResolved() {
		return false;
	}

	@Override
	public String getTypeName() {
		return this.getValue();
	}

	@Override
	public int getInstructionIndex() {
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ErrorReference) {
			ErrorReference other = (ErrorReference)obj;
			boolean insnNodeEqual;
			if (this.insnNode != null) {
				if (other.insnNode != null) {
					insnNodeEqual = this.insnNode.equals(other.insnNode);
				} else {
					insnNodeEqual = false;
				}
			} else {
				insnNodeEqual = other.insnNode == null;
			}

			if (insnNodeEqual) {
				if (this.errorMessage != null) {
					return other.errorMessage != null ? this.errorMessage.equals(other.errorMessage) : false;
				} else {
					return other.errorMessage == null;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = 0;
		if (this.insnNode != null) {
			hash = this.insnNode.hashCode();
		}

		if (this.errorMessage != null) {
			hash ^= this.errorMessage.hashCode();
		}

		return hash;
	}

	@Override
	public String getValue() {
		return null;
	}

	@Override
	public String getKey() {
		return this.getValue();
	}

	public String getErrorMessage() {
		return this.errorMessage;
	}

	@Override
	public boolean isPending() {
		return false;
	}

	@Override
	public String getDisplayString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<");
		if (this.insnNode != null) {
			switch (this.insnNode.getOpcode()) {
				case INVOKEVIRTUAL:
				case INVOKESPECIAL:
				case INVOKEINTERFACE:
					WideConstantPoolInsnNode methodCallInsn = (WideConstantPoolInsnNode)this.insnNode;
					AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)methodCallInsn.getTypeWriter();
					buffer.append(methodCallInsn.getOpcodeMnemonics() + " " + methodCallWriter.getOwnerName() + "." + methodCallWriter.getName() + " ");
					break;
				case INVOKESTATIC:
				default:
					buffer.append(this.insnNode.getOpcodeMnemonics() + " ");
			}
		}

		buffer.append("'" + this.errorMessage + "'>");
		return buffer.toString();
	}
}

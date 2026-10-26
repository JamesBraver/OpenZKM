package com.zelix.framework.analyzer.reference;

import com.zelix.framework.instructions.AbstractInsnNode;

public class InstructionTypeReference implements Reference {
	private AbstractInsnNode insnNode;
	private String internalName;

	public InstructionTypeReference(AbstractInsnNode insnNode, String internalName) {
		this.insnNode = insnNode;
		if (internalName.startsWith("[")) {
			this.internalName = "java/lang/Object";
		} else {
			this.internalName = internalName;
		}
	}

	@Override
	public boolean isSingleValue() {
		return true;
	}

	@Override
	public boolean isResolved() {
		return true;
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
		if (obj instanceof InstructionTypeReference) {
			InstructionTypeReference other = (InstructionTypeReference)obj;
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
				if (this.internalName != null) {
					return other.internalName != null ? this.internalName.equals(other.internalName) : false;
				} else {
					return other.internalName == null;
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

		if (this.internalName != null) {
			hash ^= this.internalName.hashCode();
		}

		return hash;
	}

	@Override
	public String getValue() {
		return this.internalName;
	}

	@Override
	public String getKey() {
		return this.getValue();
	}

	@Override
	public boolean isPending() {
		return false;
	}

	@Override
	public String getDisplayString() {
		return this.internalName;
	}
}

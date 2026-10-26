package com.zelix.framework.analyzer.reference;

public class PrimitiveTypeReference implements Reference {
	private final String descriptor;

	public PrimitiveTypeReference(String descriptor) {
		this.descriptor = descriptor;
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
		if (obj instanceof PrimitiveTypeReference) {
			PrimitiveTypeReference other = (PrimitiveTypeReference)obj;
			if (this.descriptor != null) {
				return other.descriptor != null ? this.descriptor.equals(other.descriptor) : false;
			} else {
				return other.descriptor == null;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.descriptor != null ? this.descriptor.hashCode() : 0;
	}

	@Override
	public String getValue() {
		return this.descriptor;
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
		return this.descriptor;
	}
}

package com.zelix.framework.analyzer.reference;

public class NullReference implements Reference {
	private int cachedHashCode = this.getClass().getName().hashCode();

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
		return obj instanceof NullReference;
	}

	@Override
	public int hashCode() {
		return this.cachedHashCode;
	}

	@Override
	public String getValue() {
		return null;
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
		return "<null>";
	}
}

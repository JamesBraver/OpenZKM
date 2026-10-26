package com.zelix.framework.analyzer.reference;

public class ConstructorReference implements Reference {
	private final String value;

	public ConstructorReference(String value) {
		this.value = value;
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
		return this.value;
	}

	@Override
	public int getInstructionIndex() {
		return -1;
	}

	@Override
	public String getValue() {
		return this.value;
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
		return this.value;
	}
}

package com.zelix.enums;

public enum LoadTypes {
	TOP_LEVEL(""),
	SKIP("-"),
	UNSKIP("+");

	private final String typePrefix;

	private LoadTypes(String prefix) {
		this.typePrefix = prefix;
	}

	public String getPrefix() {
		return this.typePrefix;
	}
}

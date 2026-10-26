package com.zelix.framework.analyzer;

import com.zelix.framework.constant.StringNode;

public class StringConcatPart {
	private StringNode constantStringNode;
	private final int partIndex;
	private final String value;

	public StringConcatPart(int partIndex, String value) {
		this.partIndex = partIndex;
		this.value = value;
	}

	public StringConcatPart(int partIndex, String value, StringNode constantStringNode) {
		this(partIndex, value);
		this.constantStringNode = constantStringNode;
	}

	public int getPartIndex() {
		return this.partIndex;
	}

	public StringNode getConstantStringNode() {
		return this.constantStringNode;
	}

	public void clearConstantNode() {
		this.constantStringNode = null;
	}

	public String getValue() {
		return this.value;
	}
}

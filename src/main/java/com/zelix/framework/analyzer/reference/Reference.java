package com.zelix.framework.analyzer.reference;

public interface Reference {
	String[] EMPTY_STRING_ARRAY = new String[0];

	boolean isSingleValue();

	boolean isResolved();

	String getTypeName();

	int getInstructionIndex();

	String getValue();

	String getKey();

	boolean isPending();

	String getDisplayString();
}

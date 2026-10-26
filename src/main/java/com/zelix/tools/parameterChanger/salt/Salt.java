package com.zelix.tools.parameterChanger.salt;

public interface Salt {
	void appendToChain(Salt salt);

	long advanceChain(long mixValue);

	long getCurrentValue();

	boolean isLessThanOrEqual(Salt other);

	long getMidBits();

	boolean isSaltEntry();
}

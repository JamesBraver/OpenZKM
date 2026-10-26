package com.zelix.tools.retrace;

public interface StacktraceLine {
	boolean isValid();

	String getLine();

	boolean hasLineNumber();

	int getLineNumber();

	String getClassName();
}

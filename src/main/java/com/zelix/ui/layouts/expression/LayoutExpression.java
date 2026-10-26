package com.zelix.ui.layouts.expression;

public interface LayoutExpression {
	boolean isContainerTopOrLeft();

	String getParseError();

	boolean isContainerSizeOrCenter();

	void invalidate();

	boolean resolve(boolean containerSizeKnown);

	boolean isContainerBottomOrRight();

	int getValue();

	boolean isContainerReference();
}

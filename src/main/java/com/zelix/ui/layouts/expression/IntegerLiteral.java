package com.zelix.ui.layouts.expression;

public class IntegerLiteral implements LayoutExpression {
	private int value;

	public IntegerLiteral(int value) {
		this.value = value;
	}

	@Override
	public boolean isContainerTopOrLeft() {
		return false;
	}

	@Override
	public String getParseError() {
		return null;
	}

	@Override
	public boolean isContainerSizeOrCenter() {
		return false;
	}

	@Override
	public void invalidate() {
	}

	@Override
	public boolean resolve(boolean containerSizeKnown) {
		return true;
	}

	@Override
	public boolean isContainerBottomOrRight() {
		return false;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public boolean isContainerReference() {
		return false;
	}
}

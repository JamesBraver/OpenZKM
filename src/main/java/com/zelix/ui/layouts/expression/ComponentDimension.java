package com.zelix.ui.layouts.expression;

public class ComponentDimension implements LayoutExpression {
	private ComponentConstraint constraint;
	private int propertyIndex;

	public ComponentDimension(ComponentConstraint constraint, int propertyIndex) {
		this.constraint = constraint;
		this.propertyIndex = propertyIndex;
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
		return this.constraint.isResolved(this.propertyIndex);
	}

	@Override
	public boolean isContainerBottomOrRight() {
		return false;
	}

	@Override
	public int getValue() {
		return this.constraint.getValue(this.propertyIndex);
	}

	@Override
	public boolean isContainerReference() {
		return false;
	}
}

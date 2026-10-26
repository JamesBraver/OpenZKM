package com.zelix.ui.layouts.expression;

import com.zelix.ui.layouts.PowerLayout;

public class ContainerDimension implements LayoutExpression {
	private PowerLayout layout;
	private int propertyIndex;

	public ContainerDimension(PowerLayout layout, int propertyIndex) {
		this.layout = layout;
		this.propertyIndex = propertyIndex;
	}

	@Override
	public boolean isContainerTopOrLeft() {
		return this.propertyIndex == 3 || this.propertyIndex == 2;
	}

	@Override
	public String getParseError() {
		return null;
	}

	@Override
	public boolean isContainerSizeOrCenter() {
		return this.propertyIndex == 0 || this.propertyIndex == 1 || this.propertyIndex == 6 || this.propertyIndex == 7;
	}

	@Override
	public void invalidate() {
	}

	@Override
	public boolean resolve(boolean containerSizeKnown) {
		switch (this.propertyIndex) {
			case 2:
			case 3:
				return true;
			default:
				return containerSizeKnown;
		}
	}

	@Override
	public boolean isContainerBottomOrRight() {
		return this.propertyIndex == 5 || this.propertyIndex == 4;
	}

	@Override
	public int getValue() {
		return this.layout.getContainerExtent(this.propertyIndex);
	}

	public int getPropertyIndex() {
		return this.propertyIndex;
	}

	@Override
	public boolean isContainerReference() {
		return true;
	}
}

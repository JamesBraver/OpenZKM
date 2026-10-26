package com.zelix.ui.layouts;

import com.zelix.ui.components.panels.DraggablePanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Point;

public class SplitLayout implements LayoutManager2 {
	public int splitType = 0;
	public float splitRatio;
	protected int dividerThickness;
	protected boolean dragEnabled;
	protected Dimension preferredSize;
	protected Component firstComponent;
	protected Container container;
	protected DraggablePanel divider;
	protected Component secondComponent;

	public SplitLayout(int splitType, int splitPercentage, boolean dragEnabled, int dividerThickness) {
		if (splitType == 0) {
			this.splitType = 0;
		} else {
			if (splitType != 1) {
				throw new IllegalArgumentException("Split type must be SplitLayout.VERTICAL or SplitLayout.HORIZONTAL");
			}

			this.splitType = 1;
		}

		if (splitPercentage >= 0 && splitPercentage <= 100) {
			this.splitRatio = splitPercentage / 100.0F;
			if (dividerThickness >= 3 && dividerThickness <= 7) {
				this.dividerThickness = dividerThickness;
				this.dragEnabled = dragEnabled;
			} else {
				throw new IllegalArgumentException("Divider thickness must be >= 3 && <= 7");
			}
		} else {
			throw new IllegalArgumentException("Initial split percentage must be >= 0 && <= 100");
		}
	}

	public void updateSplitRatio(Point dragPoint) {
		if (this.splitType == 0) {
			this.splitRatio = (float)(this.firstComponent.getSize().width + dragPoint.x) / this.container.getSize().width;
		} else {
			this.splitRatio = (float)(this.firstComponent.getSize().height + dragPoint.y) / this.container.getSize().height;
		}

		this.splitRatio = Math.max(0.0F, Math.min(1.0F, this.splitRatio));
		this.container.invalidate();
		this.container.validate();
	}

	private void addComponent(Component component) {
		if (this.firstComponent == null) {
			this.firstComponent = component;
		} else if (this.firstComponent != component && this.secondComponent == null) {
			this.secondComponent = component;
		} else if (this.secondComponent == component) {
			throw new IllegalArgumentException("SplitLayout Error - More than two Components added");
		}
	}

	@Override
	public void addLayoutComponent(Component comp, Object constraints) {
		if (constraints != this) {
			this.addComponent(comp);
		}
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
		this.addComponent(comp);
	}

	public void endDividerDrag(Point dragPoint) {
		this.updateSplitRatio(dragPoint);
	}

	private Dimension computeContentSize(Container container, Insets insets) {
		Dimension contentSize = container.getSize();
		contentSize.width = contentSize.width - (insets.left + insets.left);
		contentSize.height = contentSize.height - (insets.top + insets.bottom);
		if (this.preferredSize != null) {
			int maxWidth = Math.max(contentSize.width, this.preferredSize.width);
			int maxHeight = Math.max(contentSize.height, this.preferredSize.height);
			return new Dimension(maxWidth, maxHeight);
		} else {
			return contentSize;
		}
	}

	@Override
	public float getLayoutAlignmentX(Container target) {
		return 0.5F;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
		return 0.5F;
	}

	@Override
	public void invalidateLayout(Container target) {
	}

	@Override
	public void layoutContainer(Container parent) {
		if (this.container == null) {
			this.container = parent;
		}

		if (this.divider == null) {
			this.divider = new DraggablePanel(this);
			this.container.add(this.divider, this);
		}

		Insets insets = parent.getInsets();
		Dimension contentSize = this.computeContentSize(parent, insets);
		if (this.splitType == 0) {
			int firstWidth = Math.max(0, Math.round(contentSize.width * this.splitRatio - this.dividerThickness));
			int secondWidth = contentSize.width - this.dividerThickness - firstWidth;
			this.firstComponent.setBounds(0, 0, firstWidth, contentSize.height);
			this.divider.setBounds(firstWidth, 0, this.dividerThickness, contentSize.height);
			this.secondComponent.setBounds(firstWidth + this.dividerThickness, 0, secondWidth, contentSize.height);
		} else {
			int firstHeight = Math.max(0, Math.round(contentSize.height * this.splitRatio - this.dividerThickness));
			int secondHeight = contentSize.height - this.dividerThickness - firstHeight;
			this.firstComponent.setBounds(0, 0, contentSize.width, firstHeight);
			this.divider.setBounds(0, firstHeight, contentSize.width, this.dividerThickness);
			this.secondComponent.setBounds(0, firstHeight + this.dividerThickness, contentSize.width, secondHeight);
		}
	}

	@Override
	public Dimension maximumLayoutSize(Container target) {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return this.preferredLayoutSize(parent);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return this.preferredSize != null ? this.preferredSize : new Dimension(100, 100);
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		if (this.firstComponent == comp) {
			this.firstComponent = null;
		} else {
			if (this.secondComponent != comp) {
				throw new IllegalArgumentException("SplitLayout Error - Unknown Component");
			}

			this.secondComponent = null;
		}
	}

	public void dragDivider(Point dragPoint) {
		if (this.dragEnabled) {
			this.updateSplitRatio(dragPoint);
		}
	}
}

package com.zelix.ui.layouts;

import com.zelix.ui.layouts.expression.ComponentConstraint;
import com.zelix.ui.layouts.expression.StylesExpression;
import com.zelix.utils.GlobalUtils;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class PowerLayout implements LayoutManager2 {
	public static final String lineSeparator = GlobalUtils.LINE_SEPARATOR;
	private Map<String, Component> componentsByName = GlobalUtils.newHashMap();
	private Map<Component, ComponentConstraint> constraintsByComponent = GlobalUtils.newHashMap();
	private StylesExpression[] sizeExpressions = new StylesExpression[4];
	private Container container;
	private Integer width;
	private Integer defaultRightPad;
	private boolean constraintsResolved;
	private Integer height;
	private boolean sizesResolved;
	private Integer minHeight;
	private String[] properties;
	private Integer minWidth;
	private Integer defaultBottomPad;
	private boolean resolvingConstraints;
	private Map<String, StylesExpression> stylesByName;

	public PowerLayout(Container container) {
		this.container = container;
	}

	public static String propertyName(int propertyIndex) {
		switch (propertyIndex) {
			case 0:
				return "width";
			case 1:
				return "height";
			case 2:
				return "top";
			case 3:
				return "left";
			case 4:
				return "bottom";
			case 5:
				return "right";
			case 6:
				return "centerX";
			case 7:
				return "centerY";
			case 8:
				return "minWidth";
			case 9:
				return "minHeight";
			case 10:
				return "defaultWidth";
			case 11:
				return "defaultHeight";
			default:
				return null;
		}
	}

	public static int propertyIndex(String propertyName) {
		if (propertyName.equals("width")) {
			return 0;
		} else if (propertyName.equals("height")) {
			return 1;
		} else if (propertyName.equals("top")) {
			return 2;
		} else if (propertyName.equals("bottom")) {
			return 4;
		} else if (propertyName.equals("left")) {
			return 3;
		} else if (propertyName.equals("right")) {
			return 5;
		} else if (propertyName.equals("centerX")) {
			return 6;
		} else if (propertyName.equals("centerY")) {
			return 7;
		} else if (propertyName.equals("minWidth")) {
			return 8;
		} else if (propertyName.equals("minHeight")) {
			return 9;
		} else if (propertyName.equals("defaultWidth")) {
			return 10;
		} else {
			return propertyName.equals("defaultHeight") ? 11 : -1;
		}
	}

	public String getVersion() {
		return "1.1";
	}

	private void resolveSizeExpressions() {
		if (!this.sizesResolved) {
			for (int expressionIndex = 0; expressionIndex < this.sizeExpressions.length; expressionIndex++) {
				if (this.sizeExpressions[expressionIndex] != null && this.sizeExpressions[expressionIndex].resolve(false)) {
					switch (expressionIndex) {
						case 0:
							this.width = this.sizeExpressions[expressionIndex].getValue();
							break;
						case 1:
							this.height = this.sizeExpressions[expressionIndex].getValue();
							break;
						case 2:
							this.minWidth = this.sizeExpressions[expressionIndex].getValue();
							break;
						case 3:
							this.minHeight = this.sizeExpressions[expressionIndex].getValue();
					}
				}
			}

			this.sizesResolved = true;
		}
	}

	private void resolveConstraints(Container container, ArrayList<Component> unresolved, boolean applyBounds) {
		this.constraintsResolved = true;
		this.resolvingConstraints = true;
		Insets insets = container.getInsets();
		int componentCount = container.getComponentCount();

		for (int i = 0; i < componentCount; i++) {
			unresolved.add(container.getComponent(i));
		}

		boolean changed = true;

		while (changed) {
			changed = false;
			ArrayList<Component> unresolvedSnapshot = new ArrayList<>(unresolved);

			for (int snapshotIndex = 0; snapshotIndex < unresolvedSnapshot.size(); snapshotIndex++) {
				Component component = unresolvedSnapshot.get(snapshotIndex);
				ComponentConstraint constraint = this.constraintsByComponent.get(component);
				if (constraint != null) {
					if (constraint.resolveConstraints(applyBounds)) {
						changed = true;
					}

					if (constraint.isResolvable(applyBounds) && constraint.isResolved()) {
						unresolved.remove(component);
						if (applyBounds) {
							Dimension size = constraint.getSize();
							Point location = constraint.getLocation();
							component.setBounds(location.x + insets.left, location.y + insets.top, size.width, size.height);
						}
					}
				} else {
					unresolved.remove(component);
				}
			}
		}

		this.resolvingConstraints = false;
	}

	public void setProperties(String[] styles) {
		this.properties = styles;
		this.stylesByName = GlobalUtils.newHashMap();
		this.width = null;
		this.height = null;
		this.minWidth = null;
		this.minHeight = null;
		this.defaultRightPad = null;
		this.defaultBottomPad = null;

		for (int propertyIndex = 0; propertyIndex < this.properties.length; propertyIndex++) {
			StringTokenizer propertyTokenizer = new StringTokenizer(this.properties[propertyIndex], "=");
			int tokenCount = propertyTokenizer.countTokens();
			String[] tokens = new String[tokenCount];

			for (int tokenIndex = 0; tokenIndex < tokenCount; tokenIndex++) {
				tokens[tokenIndex] = propertyTokenizer.nextToken().trim();
			}

			String propertyValue = tokens[tokenCount - 1];

			for (int nameIndex = 0; nameIndex < tokenCount - 1; nameIndex++) {
				String namePath = tokens[nameIndex];
				int dotIndex = namePath.indexOf(".");
				if (dotIndex == -1) {
					this.stylesByName.put(namePath, new StylesExpression(this, propertyValue));
				} else {
					if (dotIndex == namePath.length() - 1 || namePath.lastIndexOf(".") != dotIndex) {
						this.fatalError("Property '" + namePath + "' is invalid. : '" + this.properties[propertyIndex] + "'");
					}

					String ownerName = namePath.substring(0, dotIndex).trim();
					String propertyName = namePath.substring(dotIndex + 1).trim();
					if (ownerName.equals("container")) {
						this.setContainerProperty(propertyName, propertyValue);
					} else if (ownerName.equals("layout")) {
						this.setLayoutProperty(propertyName, propertyValue);
					} else {
						Component component = this.componentsByName.get(ownerName);
						ComponentConstraint constraint = this.constraintsByComponent.get(component);
						constraint.setExpression(propertyName, propertyValue);
					}
				}
			}
		}

		for (int expressionIndex = 0; expressionIndex < this.sizeExpressions.length; expressionIndex++) {
			if (this.sizeExpressions[expressionIndex] != null) {
				this.sizeExpressions[expressionIndex].getParseError();
			}
		}

		Iterator<ComponentConstraint> constraintIterator = this.constraintsByComponent.values().iterator();

		while (constraintIterator.hasNext()) {
			constraintIterator.next().getExpressionError();
		}
	}

	public ComponentConstraint getConstraint(String componentName) {
		Component component = this.componentsByName.get(componentName);
		if (component == null) {
			this.fatalError("Component '" + componentName + "' has not been added to the container.");
		}

		return this.constraintsByComponent.get(component);
	}

	private String setContainerProperty(String property, String value) {
		int padValue;
		try {
			padValue = Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			return "Only an integer can be assigned to 'container." + property + "'";
		}

		if (property.equals("defaultRightPad")) {
			this.defaultRightPad = padValue;
		} else {
			if (!property.equals("defaultBottomPad")) {
				return "Property '" + property + "' is not valid for the 'container' object.";
			}

			this.defaultBottomPad = padValue;
		}

		return null;
	}

	public int getContainerExtent(int propertyIndex) {
		Dimension availableSize = this.getAvailableSize();
		switch (propertyIndex) {
			case 0:
			case 5:
				return availableSize.width;
			case 1:
			case 4:
				return availableSize.height;
			case 2:
			case 3:
				return 0;
			case 6:
				return availableSize.width / 2;
			case 7:
				return availableSize.height / 2;
			default:
				this.fatalError("'container." + propertyName(propertyIndex) + "' is not a valid RHS property.");
				return -1;
		}
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
		synchronized (comp.getTreeLock()) {
			Component existingComponent = this.componentsByName.put(name, comp);
			if (existingComponent != null) {
				this.fatalError("Multiple components with the name '" + name + "': " + existingComponent + " : " + comp);
			}

			this.constraintsByComponent.put(comp, new ComponentConstraint(name, comp, this));
		}
	}

	@Override
	public void addLayoutComponent(Component comp, Object constraints) {
		synchronized (comp.getTreeLock()) {
			if (!(constraints instanceof String)) {
				throw new IllegalArgumentException("'addLayoutComponent' constraint must be a name String");
			} else {
				String name = (String)constraints;
				Component existingComponent = this.componentsByName.put(name, comp);
				if (existingComponent != null) {
					this.fatalError("Multiple components with the name '" + name + "': " + existingComponent + " : " + comp);
				}

				this.constraintsByComponent.put(comp, new ComponentConstraint(name, comp, this));
			}
		}
	}

	public void fatalError(String message) {
		String errorMessage = "PowerLayout " + this.getVersion() + " ERROR: " + lineSeparator + message;
		System.err.println(lineSeparator + errorMessage);
		System.exit(1);
		throw new RuntimeException(errorMessage);
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
		this.resetConstraints();
	}

	public StylesExpression getStyle(String name) {
		return this.stylesByName.get(name);
	}

	@Override
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			this.resolveSizeExpressions();
			if (this.properties == null) {
			}

			this.resetConstraints();
			ArrayList<Component> unresolvedComponents = new ArrayList<>(parent.getComponentCount());
			this.resolveConstraints(parent, unresolvedComponents, true);
			if (unresolvedComponents.size() > 0) {
				StringBuffer messageBuffer = new StringBuffer();

				for (int i = 0; i < unresolvedComponents.size(); i++) {
					ComponentConstraint constraint = this.constraintsByComponent.get(unresolvedComponents.get(i));
					messageBuffer.append(lineSeparator + constraint.toDebugString());
				}

				this.fatalError("Constraints could not be resolved: " + messageBuffer.toString());
			}
		}
	}

	@Override
	public Dimension maximumLayoutSize(Container target) {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			this.resolveSizeExpressions();
			Integer effectiveWidth = this.minWidth != null ? this.minWidth : this.width;
			Integer effectiveHeight = this.minHeight != null ? this.minHeight : this.height;
			return this.getLayoutSize(parent, effectiveWidth, effectiveHeight);
		}
	}

	public void applyStyles(String styles) {
		StringTokenizer styleTokenizer = new StringTokenizer(styles, ";");
		String[] styleTokens = new String[styleTokenizer.countTokens()];

		for (int i = 0; i < styleTokens.length; i++) {
			styleTokens[i] = styleTokenizer.nextToken().trim();
		}

		this.setProperties(styleTokens);
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			this.resolveSizeExpressions();
			return this.getLayoutSize(parent, this.width, this.height);
		}
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		synchronized (comp.getTreeLock()) {
			ComponentConstraint removedConstraint = this.constraintsByComponent.remove(comp);
			if (removedConstraint != null) {
				this.componentsByName.remove(removedConstraint.getName());
			}
		}
	}

	public Dimension getAvailableSize() {
		Insets insets = this.container.getInsets();
		Dimension size = this.container.getSize();
		size.width = size.width - (insets.left + insets.left);
		size.height = size.height - (insets.top + insets.bottom);
		if (this.minWidth != null) {
			size.width = Math.max(size.width, this.minWidth);
		}

		if (this.minHeight != null) {
			size.height = Math.max(size.height, this.minHeight);
		}

		return size;
	}

	private String setLayoutProperty(String property, String value) {
		int sizeValue;
		try {
			sizeValue = Integer.parseInt(value);
		} catch (NumberFormatException ignored) {
			if (property.equals("width")) {
				this.sizeExpressions[0] = new StylesExpression(this, value);
			} else if (property.equals("height")) {
				this.sizeExpressions[1] = new StylesExpression(this, value);
			} else if (property.equals("minWidth")) {
				this.sizeExpressions[2] = new StylesExpression(this, value);
			} else {
				if (!property.equals("minHeight")) {
					return "Property '" + property + "' is not valid for the 'layout' object.";
				}

				this.sizeExpressions[3] = new StylesExpression(this, value);
			}

			return null;
		}

		if (property.equals("width")) {
			this.width = sizeValue;
		} else if (property.equals("height")) {
			this.height = sizeValue;
		} else if (property.equals("minWidth")) {
			this.minWidth = sizeValue;
		} else {
			if (!property.equals("minHeight")) {
				return "Property '" + property + "' is not valid for the 'layout' object.";
			}

			this.minHeight = sizeValue;
		}

		return null;
	}

	private void resetConstraints() {
		Iterator<ComponentConstraint> constraintIterator = this.constraintsByComponent.values().iterator();

		while (constraintIterator.hasNext()) {
			constraintIterator.next().invalidate();
		}

		this.constraintsResolved = false;
	}

	public Dimension getLayoutSize(Container container, Integer width, Integer height) {
		if (this.properties == null) {
		}

		this.resetConstraints();
		Insets insets = container.getInsets();
		int layoutWidth = -1;
		int layoutHeight = -1;
		if (width != null) {
			layoutWidth = width;
		}

		if (height != null) {
			layoutHeight = height;
		}

		if (layoutWidth == -1 || layoutHeight == -1) {
			int componentCount = container.getComponentCount();
			ArrayList<Component> unresolvedComponents = new ArrayList<>(componentCount);
			this.resolveConstraints(container, unresolvedComponents, false);

			boolean pendingChanged;
			do {
				pendingChanged = false;

				for (int i = 0; i < unresolvedComponents.size(); i++) {
					Component pendingComponent = unresolvedComponents.get(i);
					pendingChanged = this.constraintsByComponent.get(pendingComponent).resolvePendingExpressions() ? true : pendingChanged;
				}
			} while (pendingChanged);

			int rightPad = 0;
			if (this.defaultRightPad != null) {
				rightPad = this.defaultRightPad;
			}

			int bottomPad = 0;
			if (this.defaultBottomPad != null) {
				bottomPad = this.defaultBottomPad;
			}

			int maxRight = 0;
			int maxBottom = 0;

			for (int componentIndex = 0; componentIndex < componentCount; componentIndex++) {
				Component component = container.getComponent(componentIndex);
				ComponentConstraint constraint = this.constraintsByComponent.get(component);
				if (!constraint.isResolved()) {
					this.fatalError("Constraints could not be resolved while determining preferred size. '" + constraint + "'");
				}

				Point bottomRight = constraint.getBottomRight();
				bottomRight.x = bottomRight.x + Math.max(constraint.getRightOffset(), rightPad);
				bottomRight.y = bottomRight.y + Math.max(constraint.getBottomOffset(), bottomPad);
				if (bottomRight.x > maxRight) {
					maxRight = bottomRight.x;
				}

				if (bottomRight.y > maxBottom) {
					maxBottom = bottomRight.y;
				}
			}

			if (layoutWidth == -1) {
				layoutWidth = maxRight;
			}

			if (layoutHeight == -1) {
				layoutHeight = maxBottom;
			}

			if (this.minWidth != null) {
				layoutWidth = Math.max(layoutWidth, this.minWidth);
			}

			if (this.minHeight != null) {
				layoutHeight = Math.max(layoutHeight, this.minHeight);
			}
		}

		return new Dimension(layoutWidth + insets.left + insets.right, layoutHeight + insets.top + insets.bottom);
	}
}

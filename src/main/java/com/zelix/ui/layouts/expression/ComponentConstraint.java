package com.zelix.ui.layouts.expression;

import com.zelix.ui.layouts.PowerLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

public class ComponentConstraint {
	private StylesExpression[] expressions = new StylesExpression[10];
	private Integer[] values = new Integer[10];
	private String name;
	private Component component;
	private PowerLayout layout;
	private int rightOffset;
	private Dimension cachedSize;
	private int bottomOffset;
	private StylesExpression[] pendingExpressions;
	private Point cachedLocation;

	public ComponentConstraint(String name, Component component, PowerLayout layout) {
		this.name = name;
		this.component = component;
		this.layout = layout;
	}

	public Point getLocation() {
		return new Point(this.values[3], this.values[2]);
	}

	private void resolveValueFromExpression(int propertyIndex, StylesExpression[] expressions, boolean[] changed, boolean containerSizeKnown) {
		if (this.values[propertyIndex] == null) {
			StylesExpression expression = expressions[propertyIndex];
			if (expression != null && expression.resolve(containerSizeKnown)) {
				this.values[propertyIndex] = expression.getValue();
				changed[0] = true;
			}
		}
	}

	public boolean resolveExpressions(boolean containerSizeKnown, StylesExpression[] expressions) {
		boolean[] valueResolved = new boolean[]{false};

		for (int propertyIndex = 0; propertyIndex < 10; propertyIndex++) {
			this.resolveValueFromExpression(propertyIndex, expressions, valueResolved, containerSizeKnown);
		}

		boolean anyDependentResolved = false;

		for (boolean progressMade = true; progressMade; anyDependentResolved = progressMade ? true : anyDependentResolved) {
			boolean[] dependentResolved = new boolean[]{false};

			for (int dependentPropertyIndex = 0; dependentPropertyIndex < 8; dependentPropertyIndex++) {
				this.resolveDependentValue(dependentPropertyIndex, expressions, dependentResolved, containerSizeKnown);
			}

			progressMade = dependentResolved[0];
		}

		return valueResolved[0] || anyDependentResolved;
	}

	public String getExpressionError() {
		this.cachedSize = null;
		this.cachedLocation = null;

		for (int i = 0; i < 10; i++) {
			StylesExpression expression = this.expressions[i];
			if (expression != null) {
				String parseError = expression.getParseError();
				if (parseError != null) {
					return parseError;
				}
			}
		}

		return null;
	}

	public String setExpression(String property, String expression) {
		int propertyIndex = PowerLayout.propertyIndex(property);
		this.expressions[propertyIndex] = new StylesExpression(this.layout, expression);
		return null;
	}

	public int getRightOffset() {
		return this.rightOffset;
	}

	public Point getBottomRight() {
		return new Point(this.values[5], this.values[4]);
	}

	private void resolveDependentValue(int propertyIndex, StylesExpression[] expressions, boolean[] changed, boolean containerSizeKnown) {
		if (expressions[propertyIndex] == null && this.values[propertyIndex] == null) {
			int resolvedValue = Integer.MIN_VALUE;
			switch (propertyIndex) {
				case 0:
					if (expressions[3] != null && expressions[5] != null) {
						if (this.values[3] != null && this.values[5] != null) {
							resolvedValue = this.values[5] - this.values[3];
						}
					} else if (expressions[3] != null && expressions[6] != null) {
						if (this.values[3] != null && this.values[6] != null) {
							resolvedValue = (this.values[6] - this.values[3]) * 2;
						}
					} else if (expressions[6] != null && expressions[5] != null) {
						if (this.values[6] != null && this.values[5] != null) {
							resolvedValue = (this.values[5] - this.values[6]) * 2;
						}
					} else {
						resolvedValue = this.component.getPreferredSize().width;
						StylesExpression minWidthExpression = this.expressions[8];
						if (minWidthExpression != null) {
							resolvedValue = Math.max(resolvedValue, minWidthExpression.getValue());
						}
					}
					break;
				case 1:
					if (expressions[4] != null && expressions[2] != null) {
						if (this.values[4] != null && this.values[2] != null) {
							resolvedValue = this.values[4] - this.values[2];
						}
					} else if (expressions[2] != null && expressions[7] != null) {
						if (this.values[7] != null && this.values[2] != null) {
							resolvedValue = (this.values[7] - this.values[2]) * 2;
						}
					} else if (expressions[7] != null && expressions[4] != null) {
						if (this.values[7] != null && this.values[4] != null) {
							resolvedValue = (this.values[4] - this.values[7]) * 2;
						}
					} else {
						resolvedValue = this.component.getPreferredSize().height;
						StylesExpression minHeightExpression = this.expressions[9];
						if (minHeightExpression != null) {
							resolvedValue = Math.max(resolvedValue, minHeightExpression.getValue());
						}
					}
					break;
				case 2:
					if (expressions[7] == null && expressions[4] == null) {
						resolvedValue = 0;
					} else {
						if (this.values[1] == null) {
							return;
						}

						int heightForTop = this.values[1];
						if (this.values[4] != null) {
							resolvedValue = this.values[4] - heightForTop;
						} else if (this.values[7] != null) {
							resolvedValue = this.values[7] - heightForTop / 2;
						}
					}
					break;
				case 3:
					if (expressions[6] == null && expressions[5] == null) {
						resolvedValue = 0;
					} else {
						if (this.values[0] == null) {
							return;
						}

						int widthForLeft = this.values[0];
						if (this.values[5] != null) {
							resolvedValue = this.values[5] - widthForLeft;
						} else if (this.values[6] != null) {
							resolvedValue = this.values[6] - widthForLeft / 2;
						}
					}
					break;
				case 4:
					if (this.values[1] == null) {
						return;
					}

					int heightForBottom = this.values[1];
					if (this.values[2] != null) {
						resolvedValue = this.values[2] + heightForBottom;
					} else if (this.values[7] != null) {
						resolvedValue = this.values[7] + heightForBottom / 2;
					}
					break;
				case 5:
					if (this.values[0] == null) {
						return;
					}

					int widthForRight = this.values[0];
					if (this.values[3] != null) {
						resolvedValue = this.values[3] + widthForRight;
					} else if (this.values[6] != null) {
						resolvedValue = this.values[6] + widthForRight / 2;
					}
					break;
				case 6:
					if (this.values[0] == null) {
						return;
					}

					int widthForCenterX = this.values[0];
					if (this.values[3] != null) {
						resolvedValue = this.values[3] + widthForCenterX / 2;
					} else if (this.values[5] != null) {
						resolvedValue = this.values[5] - widthForCenterX / 2;
					}
					break;
				case 7:
					if (this.values[1] == null) {
						return;
					}

					int heightForCenterY = this.values[1];
					if (this.values[2] != null) {
						resolvedValue = this.values[2] + heightForCenterY / 2;
					} else if (this.values[4] != null) {
						resolvedValue = this.values[4] - heightForCenterY / 2;
					}
			}

			if (resolvedValue != Integer.MIN_VALUE) {
				changed[0] = true;
				this.values[propertyIndex] = resolvedValue;
			}
		}
	}

	public boolean isResolved() {
		return this.values[0] != null && this.values[1] != null && this.values[3] != null && this.values[2] != null;
	}

	public boolean resolveConstraints(boolean containerSizeKnown) {
		return this.resolveExpressions(containerSizeKnown, this.expressions);
	}

	public String getName() {
		return this.name;
	}

	public String toDebugString() {
		StringBuffer buffer = new StringBuffer(this.name + " : {");

		for (int expressionIndex = 0; expressionIndex < 10; expressionIndex++) {
			StylesExpression expression = this.expressions[expressionIndex];
			if (expression != null) {
				buffer.append(" " + PowerLayout.propertyName(expressionIndex) + "=" + expression.getExpression());
			}
		}

		buffer.append("} {");

		for (int valueIndex = 0; valueIndex < 10; valueIndex++) {
			buffer.append(" " + PowerLayout.propertyName(valueIndex) + "=" + this.values[valueIndex]);
		}

		buffer.append("}");
		return buffer.toString();
	}

	public boolean isResolvable(boolean containerSizeKnown) {
		for (int i = 0; i < 10; i++) {
			StylesExpression expression = this.expressions[i];
			if (expression != null && !expression.resolve(containerSizeKnown)) {
				return false;
			}
		}

		return true;
	}

	public int getValue(int propertyIndex) {
		if (propertyIndex == 10) {
			return this.component.getPreferredSize().width;
		} else {
			return propertyIndex == 11 ? this.component.getPreferredSize().height : this.values[propertyIndex];
		}
	}

	public void invalidate() {
		this.cachedSize = null;
		this.cachedLocation = null;

		for (int valueIndex = 0; valueIndex < 10; valueIndex++) {
			this.values[valueIndex] = null;
		}

		for (int expressionIndex = 0; expressionIndex < 10; expressionIndex++) {
			StylesExpression expression = this.expressions[expressionIndex];
			if (expression != null) {
				expression.invalidate();
			}
		}
	}

	public boolean resolvePendingExpressions() {
		if (this.pendingExpressions == null) {
			this.pendingExpressions = new StylesExpression[10];
			System.arraycopy(this.expressions, 0, this.pendingExpressions, 0, 10);

			for (int i = 0; i < 10; i++) {
				StylesExpression pendingExpression = this.pendingExpressions[i];
				if (pendingExpression != null && !pendingExpression.isResolved()) {
					if (pendingExpression.isContainerSizeOrCenter()) {
						this.pendingExpressions[i] = null;
					} else if (pendingExpression.isContainerBottomOrRight()) {
						if (pendingExpression.getPropertyIndex() == 5) {
							this.rightOffset = pendingExpression.getTrailingOffset();
						} else {
							this.bottomOffset = pendingExpression.getTrailingOffset();
						}

						this.pendingExpressions[i] = null;
					} else if (pendingExpression.isContainerTopOrLeft()) {
						pendingExpression.resolve(true);
					}
				}
			}
		}

		boolean progressMade = false;

		while (this.resolveExpressions(false, this.pendingExpressions)) {
			progressMade = true;
		}

		return progressMade;
	}

	public boolean isResolved(int propertyIndex) {
		return propertyIndex != 10 && propertyIndex != 11 ? this.values[propertyIndex] != null : true;
	}

	public Dimension getSize() {
		return new Dimension(this.values[0], this.values[1]);
	}

	public int getBottomOffset() {
		return this.bottomOffset;
	}
}

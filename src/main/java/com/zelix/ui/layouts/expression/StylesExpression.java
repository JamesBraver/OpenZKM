package com.zelix.ui.layouts.expression;

import com.zelix.ui.layouts.PowerLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StylesExpression implements LayoutExpression {
	private List<LayoutExpression> operands = new ArrayList<>();
	private List<String> operators = new ArrayList<>();
	private PowerLayout layout;
	private String expression;
	private int value;
	private boolean parsed;
	private boolean resolved;

	public StylesExpression(PowerLayout layout, String expression) {
		this.layout = layout;
		this.expression = expression;
	}

	@Override
	public boolean isContainerTopOrLeft() {
		return this.operands.get(0).isContainerTopOrLeft();
	}

	@Override
	public String getParseError() {
		this.invalidate();
		return !this.parsed ? this.parse() : null;
	}

	public boolean isResolved() {
		return this.resolved;
	}

	@Override
	public boolean isContainerSizeOrCenter() {
		return this.operands.get(0).isContainerSizeOrCenter();
	}

	@Override
	public void invalidate() {
		for (int i = 0; i < this.operands.size(); i++) {
			this.operands.get(i).invalidate();
		}

		this.resolved = false;
		this.value = Integer.MIN_VALUE;
	}

	public int getPropertyIndex() {
		LayoutExpression operand = this.operands.get(0);
		return operand instanceof ContainerDimension ? ((ContainerDimension)operand).getPropertyIndex() : 0;
	}

	@Override
	public boolean resolve(boolean containerSizeKnown) {
		if (!this.resolved) {
			this.resolved = true;

			for (int operandIndex = 0; operandIndex < this.operands.size(); operandIndex++) {
				int operandValue = 0;
				LayoutExpression operand = this.operands.get(operandIndex);
				if (operand.resolve(containerSizeKnown)) {
					operandValue = operand.getValue();
				} else {
					this.resolved = false;
				}

				if (this.resolved) {
					if (operandIndex == 0) {
						this.value = operandValue;
					} else {
						String operator = this.operators.get(operandIndex - 1);
						this.value = this.applyOperator(this.value, operandValue, operator);
					}
				}
			}
		}

		return this.resolved;
	}

	@Override
	public boolean isContainerBottomOrRight() {
		return this.operands.get(0).isContainerBottomOrRight();
	}

	@Override
	public int getValue() {
		return this.value;
	}

	public int getTrailingOffset() {
		int offset = 0;

		for (int operandIndex = 1; operandIndex < this.operands.size(); operandIndex++) {
			LayoutExpression operand = this.operands.get(operandIndex);
			if (operand.resolve(false)) {
				int operandValue = operand.getValue();
				String operator = this.operators.get(operandIndex - 1);
				offset = this.applyOperator(offset, operandValue, operator);
			}
		}

		return offset * -1;
	}

	private int applyOperator(int leftValue, int rightValue, String operator) {
		if (operator.equals("+")) {
			return leftValue + rightValue;
		} else if (operator.equals("-")) {
			return leftValue - rightValue;
		} else {
			return operator.equals("*") ? leftValue * rightValue : leftValue / rightValue;
		}
	}

	@Override
	public boolean isContainerReference() {
		return this.operands.get(0).isContainerReference();
	}

	private boolean isOperator(String token) {
		return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/");
	}

	private String parse() {
		this.parsed = true;
		StringTokenizer tokenizer = new StringTokenizer(this.expression, "+-*/", true);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken().trim();
			if (this.isOperator(token)) {
				this.operators.add(token);
			} else {
				int dotIndex = token.indexOf(".");
				LayoutExpression operand;
				if (FunctionExpression.isFunction(token)) {
					FunctionExpression functionExpression = new FunctionExpression(this.layout, token);
					String functionParseError = functionExpression.getParseError();
					if (functionParseError != null) {
						return functionParseError;
					}

					operand = functionExpression;
				} else if (dotIndex != -1) {
					String componentName = token.substring(0, dotIndex).trim();
					int propertyIndex = PowerLayout.propertyIndex(token.substring(dotIndex + 1).trim());
					if (componentName.equals("container")) {
						operand = new ContainerDimension(this.layout, propertyIndex);
					} else {
						ComponentConstraint constraint = this.layout.getConstraint(componentName);
						operand = new ComponentDimension(constraint, propertyIndex);
					}
				} else if (Character.isDigit(token.charAt(0))) {
					try {
						int literalValue = Integer.parseInt(token);
						operand = new IntegerLiteral(literalValue);
					} catch (NumberFormatException ignored) {
						return "Parse error '" + this.expression + "'";
					}
				} else {
					StylesExpression style = this.layout.getStyle(token);
					String styleParseError = style.getParseError();
					if (styleParseError != null) {
						return styleParseError;
					}

					operand = style;
				}

				this.operands.add(operand);
				if (this.operands.size() > 1 && operand.isContainerReference()) {
					return "Parse error. Only the first operand can be container relative : '" + token + "' in '" + this.expression + "'";
				}
			}
		}

		return null;
	}

	public String getExpression() {
		return this.expression;
	}
}

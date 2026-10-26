package com.zelix.ui.layouts.expression;

import com.zelix.ui.layouts.PowerLayout;
import java.util.StringTokenizer;
import java.util.Vector;

public class FunctionExpression implements LayoutExpression {
	public Vector<StylesExpression> operands = new Vector<>();
	public PowerLayout layout;
	public String expression;
	public boolean resolved;
	public String function;
	public boolean parsed;
	public int value;

	public FunctionExpression(PowerLayout layout, String expression) {
		this.layout = layout;
		this.expression = expression.trim();
	}

	public static boolean isFunction(String token) {
		if (!token.startsWith("max") && !token.startsWith("min")) {
			return false;
		} else {
			char followingChar = token.charAt(3);
			return followingChar == '(' || followingChar == ' ';
		}
	}

	@Override
	public boolean isContainerTopOrLeft() {
		return false;
	}

	public String parseFunction() {
		this.parsed = true;
		StringTokenizer tokenizer = new StringTokenizer(this.expression, "()");
		this.function = tokenizer.nextToken().trim();
		String argumentList = tokenizer.nextToken();
		StringTokenizer argumentTokenizer = new StringTokenizer(argumentList, ",");

		while (argumentTokenizer.hasMoreTokens()) {
			String argument = argumentTokenizer.nextToken();
			StylesExpression argumentExpression = new StylesExpression(this.layout, argument);
			String parseError = argumentExpression.getParseError();
			if (parseError != null) {
				return parseError;
			}

			if (argumentExpression.isContainerReference()) {
				return "Parse Error: Functions cannot take container properties as operands '" + this.expression + "'";
			}

			this.operands.addElement(argumentExpression);
		}

		return null;
	}

	@Override
	public String getParseError() {
		this.invalidate();
		String parseError = null;
		if (!this.parsed) {
			parseError = this.parseFunction();
		}

		if (parseError != null) {
			return parseError;
		} else {
			if (this.function.equals("max")) {
				this.value = Integer.MIN_VALUE;
			} else {
				this.value = Integer.MAX_VALUE;
			}

			return null;
		}
	}

	@Override
	public boolean isContainerSizeOrCenter() {
		return false;
	}

	@Override
	public void invalidate() {
		this.resolved = false;

		for (int i = 0; i < this.operands.size(); i++) {
			this.operands.elementAt(i).invalidate();
		}
	}

	@Override
	public boolean resolve(boolean containerSizeKnown) {
		if (this.resolved) {
			return true;
		} else {
			for (int operandIndex = 0; operandIndex < this.operands.size(); operandIndex++) {
				if (!this.operands.elementAt(operandIndex).resolve(containerSizeKnown)) {
					return false;
				}
			}

			for (int valueIndex = 0; valueIndex < this.operands.size(); valueIndex++) {
				int operandValue = this.operands.elementAt(valueIndex).getValue();
				if (this.function.equals("max")) {
					this.value = Math.max(operandValue, this.value);
				} else {
					this.value = Math.min(operandValue, this.value);
				}
			}

			this.resolved = true;
			return true;
		}
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

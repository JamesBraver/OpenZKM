package com.zelix.tools.parameterChanger.extra;

public class ExtraParameter {
	public static final ExtraParameter[] EMPTY_ARRAY = new ExtraParameter[0];
	private final int parameterIndex;
	private final char typeChar;

	public ExtraParameter(int parameterIndex, char typeChar) {
		this.parameterIndex = parameterIndex;
		this.typeChar = typeChar;
	}

	public char getTypeChar() {
		return this.typeChar;
	}

	public int getBitSize() {
		switch (this.typeChar) {
			case 'B':
				return 8;
			case 'C':
			case 'S':
				return 16;
			case 'I':
				return 32;
			default:
				return 0;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExtraParameter)) {
			return false;
		} else {
			ExtraParameter otherParameter = (ExtraParameter)obj;
			return this.parameterIndex == otherParameter.parameterIndex && this.typeChar == otherParameter.typeChar;
		}
	}

	@Override
	public int hashCode() {
		return this.parameterIndex + this.typeChar;
	}

	public int getParameterIndex() {
		return this.parameterIndex;
	}
}

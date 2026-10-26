package com.zelix.exceptions;

public class MethodAnalyzerException extends ProcessingException {
	private final String methodName;

	public MethodAnalyzerException(String message, String methodName) {
		super(message);
		this.methodName = methodName;
	}

	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " : '" + this.getMethodName() + "'";
	}
}

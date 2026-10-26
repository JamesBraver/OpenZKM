package com.zelix.exceptions;

public class ClassResolveNotFoundException extends ClassResolveException {
	private String className;

	public ClassResolveNotFoundException(String className, String message) {
		super(message);
		this.className = className.replace('/', '.');
	}

	public String getClassName() {
		return this.className;
	}
}

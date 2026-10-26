package com.zelix.exceptions;

public class FileException extends ProcessingException {
	protected String className;

	public FileException(String className, String message) {
		super(message);
		this.className = className.replace('/', '.');
	}

	public FileException(String message) {
		super(message);
	}

	public String getClassName() {
		return this.className;
	}
}

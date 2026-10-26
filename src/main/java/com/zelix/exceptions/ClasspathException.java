package com.zelix.exceptions;

public class ClasspathException extends FileException {
	public ClasspathException(String className, String message) {
		super(className, message);
	}
}

package com.zelix.exceptions;

public class LookupException extends RuntimeException {
	public LookupException(String message) {
		super(message);
	}

	public LookupException(String message, Throwable cause) {
		super(message, cause);
	}
}

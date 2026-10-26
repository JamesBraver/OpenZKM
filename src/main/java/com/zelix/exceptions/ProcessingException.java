package com.zelix.exceptions;

public class ProcessingException extends ZKMException {
	public ProcessingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProcessingException(String message) {
		super(message);
	}
}

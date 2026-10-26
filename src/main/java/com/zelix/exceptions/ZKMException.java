package com.zelix.exceptions;

public class ZKMException extends RuntimeException {
	public ZKMException(String message, Throwable cause) {
		super(message, cause);
	}

	public ZKMException(String message) {
		super(message);
	}
}

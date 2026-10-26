package com.zelix.exceptions;

public class DebugAssertFailedException extends LookupException {
	public DebugAssertFailedException(String message) {
		super(message);
	}
}

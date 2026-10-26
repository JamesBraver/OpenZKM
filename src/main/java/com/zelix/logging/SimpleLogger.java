package com.zelix.logging;

public class SimpleLogger extends AbstractLogger {
	@Override
	public void info(String prefix, String message) {
		this.warn(prefix, message);
	}

	@Override
	public void errorWithDetails(String prefix, String message, String details) {
		this.fatalErrorWithDetails(prefix, message, details);
	}

	@Override
	public void warn(String prefix, String message) {
		System.out.println();
		System.out.println(prefix + this.separator(prefix) + message);
	}

	@Override
	public void fatal(String prefix, String message) {
		System.err.println();
		System.err.println(prefix + this.separator(prefix) + message);
		System.exit(1);
	}

	@Override
	public void fatalErrorWithDetails(String prefix, String message, String details) {
		System.err.println();
		System.err.println(prefix + this.separator(prefix) + message);
		System.err.println(details);
		System.exit(1);
	}

	@Override
	public void error(String prefix, String message) {
		this.fatal(prefix, message);
	}

	@Override
	public void message(String prefix, String message) {
		System.out.println();
		System.out.println(prefix + this.separator(prefix) + message);
	}
}

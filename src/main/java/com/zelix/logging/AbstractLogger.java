package com.zelix.logging;

public abstract class AbstractLogger {
	private boolean exitOnFatalError = true;

	public abstract void info(String prefix, String message);

	public abstract void errorWithDetails(String prefix, String message, String details);

	public abstract void warn(String prefix, String message);

	public abstract void fatal(String prefix, String message);

	protected final String separator(String prefix) {
		return prefix != null && prefix.trim().endsWith(":") ? " " : " : ";
	}

	public void specialError(String prefix, String message) {
		this.error(prefix, message);
	}

	public abstract void fatalErrorWithDetails(String prefix, String message, String details);

	protected boolean isExitOnFatalError() {
		return this.exitOnFatalError;
	}

	public abstract void error(String prefix, String message);

	protected void setExitOnFatalError(boolean exitOnFatalError) {
		this.exitOnFatalError = exitOnFatalError;
	}

	public abstract void message(String prefix, String message);
}

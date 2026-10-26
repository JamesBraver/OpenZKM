package com.zelix.logging;

import com.zelix.klassmaster.script.ScriptContext;

public class LogContextLogger extends AbstractLogger {
	private ScriptContext scriptContext;
	private int timestampPrefixLength;

	public LogContextLogger(ScriptContext scriptContext, int timestampPrefixLength) {
		this.scriptContext = scriptContext;
		this.timestampPrefixLength = timestampPrefixLength;
		this.setExitOnFatalError(!scriptContext.isThrowOnFatalError());
	}

	@Override
	public void info(String prefix, String message) {
		this.scriptContext.logWarning(message);
	}

	@Override
	public void errorWithDetails(String prefix, String message, String details) {
		String messageWithDetails = message + this.separator(message) + details;
		this.scriptContext.logError(messageWithDetails);
	}

	@Override
	public void warn(String prefix, String message) {
		this.scriptContext.logWarning(message);
	}

	@Override
	public void fatal(String prefix, String message) {
		this.scriptContext.logFatalError(message);
	}

	@Override
	public void specialError(String prefix, String message) {
		this.scriptContext.logSpecialError(message);
	}

	@Override
	public void fatalErrorWithDetails(String prefix, String message, String details) {
		String messageWithDetails = message + this.separator(message) + details;
		this.scriptContext.logFatalError(messageWithDetails);
	}

	@Override
	public void error(String prefix, String message) {
		this.scriptContext.logError(message);
	}

	@Override
	public void message(String prefix, String message) {
		this.scriptContext.logMessage(message);
	}
}

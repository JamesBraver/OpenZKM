package com.zelix.logging;

import com.zelix.exceptions.LookupException;
import com.zelix.klassmaster.script.ScriptContext;
import java.io.PrintWriter;

public class FileConsoleLogger extends AbstractLogger {
	private final PrintWriter writer;
	private final int timestampPrefixLength;
	private final ScriptContext scriptContext;

	public FileConsoleLogger(PrintWriter writer, int timestampPrefixLength, boolean throwOnFatalError, ScriptContext scriptContext) {
		this.writer = writer;
		this.timestampPrefixLength = timestampPrefixLength;
		this.setExitOnFatalError(!throwOnFatalError);
		this.scriptContext = scriptContext;
	}

	@Override
	public void info(String prefix, String message) {
		this.fatal(prefix, message);
	}

	@Override
	public void errorWithDetails(String prefix, String message, String details) {
		this.fatalErrorWithDetails(prefix, message, details);
	}

	@Override
	public void warn(String prefix, String message) {
		String outputLine = prefix + this.separator(prefix) + message + this.scriptContext.getIndentationSuffix();
		System.out.println();
		System.out.println(outputLine);
		this.writer.println(outputLine);
	}

	@Override
	public void fatal(String prefix, String message) {
		String outputLine = prefix + this.separator(prefix) + message + this.scriptContext.getIndentationSuffix();
		System.err.println();
		System.err.println(outputLine);
		this.writer.println(outputLine);
		this.writer.close();
		if (this.isExitOnFatalError()) {
			System.exit(1);
		} else {
			throw new LookupException(outputLine);
		}
	}

	@Override
	public void fatalErrorWithDetails(String prefix, String message, String details) {
		String outputLine = prefix + this.separator(prefix) + message + this.scriptContext.getIndentationSuffix();
		System.err.println();
		System.err.println(outputLine);
		System.err.println(details);
		this.writer.println(outputLine);
		this.writer.println(details);
		this.writer.close();
		if (this.isExitOnFatalError()) {
			System.exit(1);
		} else {
			throw new LookupException(outputLine);
		}
	}

	@Override
	public void error(String prefix, String message) {
		this.fatal(prefix, message);
	}

	@Override
	public void message(String prefix, String message) {
		String outputLine = prefix + this.separator(prefix) + message + this.scriptContext.getIndentationSuffix();
		System.out.println();
		System.out.println(outputLine);
		this.writer.println(outputLine);
	}
}

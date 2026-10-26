package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.klassmaster.script.statements.token.StringLiteral;
import com.zelix.utils.StringUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ExecuteStatement extends ScriptCommand {
	public String commandLine;

	public ExecuteStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "execute";
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		this.getChildrenCount();
		StringLiteral commandLineLiteral = (StringLiteral)this.getChild(0);
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();
		commandLineLiteral.parseArguments(this, scriptContext);
		this.commandLine = commandLineLiteral.getText();
		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		PrintWriter writer = scriptContext.getWriter();
		String timestampPrefix = AbstractScriptNode.getTimestampPrefix();
		String logMessage = timestampPrefix + " Executing command...";
		writer.println(logMessage + " [" + this.commandLine + "]");
		System.out.println(logMessage);
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		String shellPrefix = null;

		try {
			if (SystemConstants.osName.startsWith("Windows 9")) {
				shellPrefix = "command.com /c ";
			} else if (SystemConstants.osName.startsWith("Windows NT")
				|| SystemConstants.osName.startsWith("Windows 200")
				|| SystemConstants.osName.startsWith("Windows XP")
				|| SystemConstants.osName.startsWith("OS/2")) {
				shellPrefix = "cmd /c ";
			} else if (SystemConstants.osName.startsWith("Windows")) {
				shellPrefix = "cmd /c ";
			} else {
				shellPrefix = "";
			}

			process = runtime.exec(shellPrefix + this.commandLine);
			ExecuteStatement.StreamPumpThread errorStreamPump = new ExecuteStatement.StreamPumpThread(process.getErrorStream(), writer, timestampPrefix.length() + 1, true);
			ExecuteStatement.StreamPumpThread outputStreamPump = new ExecuteStatement.StreamPumpThread(process.getInputStream(), writer, timestampPrefix.length() + 1, false);
			errorStreamPump.start();
			outputStreamPump.start();
			if (!SystemConstants.javaVendor.startsWith("Microsoft")) {
				try {
					errorStreamPump.join();
					outputStreamPump.join();
				} catch (InterruptedException ignoredPumpJoinInterrupt) {
				}
			}
		} catch (IOException ioException) {
			scriptContext.logFatalError(this.getCommandName() + " failed: '" + shellPrefix + this.commandLine + "' : '" + ioException.toString() + "'");
		}

		try {
			if (process != null) {
				process.waitFor();
			}
		} catch (InterruptedException ignoredProcessWaitInterrupt) {
		}
	}

	public static class StreamPumpThread extends Thread {
		public InputStream inputStream;
		public PrintWriter writer;
		public String linePrefix;
		public boolean isErrorStream;

		public StreamPumpThread(InputStream inputStream, PrintWriter writer, int prefixWidth, boolean isErrorStream) {
			this.inputStream = inputStream;
			this.writer = writer;
			if (isErrorStream) {
				this.linePrefix = StringUtils.justificateString("ERROR: ", 82, prefixWidth, 32);
			} else {
				this.linePrefix = StringUtils.justificateString("", 76, prefixWidth, 32);
			}

			this.isErrorStream = isErrorStream;
		}

		@Override
		public void run() {
			boolean anyLineRead = false;

			try {
				InputStreamReader inputStreamReader = new InputStreamReader(this.inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String line;
				while ((line = bufferedReader.readLine()) != null) {
					anyLineRead = true;
					this.writer.println(this.linePrefix + line);
				}
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}

			if (anyLineRead) {
				System.out.println(this.linePrefix + "Output generated and written to log.");
			}
		}
	}
}

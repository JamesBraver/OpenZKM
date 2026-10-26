package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.klassmaster.script.statements.token.IntegerLiteral;
import com.zelix.klassmaster.script.statements.token.StringLiteral;
import com.zelix.logging.LogContextLogger;
import com.zelix.repository.ClasspathManager;
import com.zelix.utils.StringUtils;
import com.zelix.utils.references.AtomicReference;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ClasspathStatement extends ScriptCommand {
	private static char pathSeparator = File.pathSeparatorChar;
	private List<String> classpathEntries = new ArrayList<>();
	private Integer multiReleaseVersion;

	public ClasspathStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public final void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		int childCount = this.getChildrenCount();
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();

		for (int i = 0; i < childCount; i++) {
			ScriptNode child = this.getChild(i);
			if (child instanceof IntegerLiteral) {
				String versionText = ((IntegerLiteral)child).getText();
				if (i == 0) {
					try {
						int parsedVersion = Integer.parseInt(versionText);
						if (parsedVersion < 9) {
							scriptContext.logFatalError(
								"'"
									+ this.getCommandName()
									+ "' statement at line "
									+ this.getLineNumber()
									+ " has a multi-release version integer '"
									+ versionText
									+ "' which is not 9 or greater."
							);
						} else {
							this.multiReleaseVersion = parsedVersion;
						}
					} catch (NumberFormatException ignored) {
						scriptContext.logFatalError(
							"'" + this.getCommandName() + "' statement at line " + this.getLineNumber() + " has an invalid multi-release version integer : '" + versionText + "'d."
						);
					}
				} else {
					scriptContext.logFatalError(
						"'"
							+ this.getCommandName()
							+ "' statement at line "
							+ this.getLineNumber()
							+ " had a multi-release version integer '"
							+ versionText
							+ "' that was not the first parameter of the statement : "
							+ i
							+ "."
					);
				}
			} else if (child instanceof StringLiteral) {
				String classpathEntry = ((StringLiteral)child).getText();
				if (this.classpathEntries.contains(classpathEntry)) {
					scriptContext.logWarning(
						"\""
							+ classpathEntry
							+ "\" appears more than once in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ". First occurrence will be used."
					);
				} else {
					this.classpathEntries.add(classpathEntry);
				}
			} else {
				scriptContext.logFatalError(this.getClass().getName() + " had " + child.getClass().getName() + " as child " + i + " at line " + this.getLineNumber() + ".");
			}
		}

		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	@Override
	public String getCommandName() {
		return "classpath";
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		PrintWriter writer = scriptContext.getWriter();
		LogContextLogger logger = new LogContextLogger(scriptContext, AbstractScriptNode.getTimestampPrefix().length());
		ClasspathManager classpathManager = scriptContext.getVersionedClasspathManager(this.multiReleaseVersion);
		StringBuilder classpathBuffer = new StringBuilder();

		for (int i = 0; i < this.classpathEntries.size(); i++) {
			String classpathEntry = this.classpathEntries.get(i);
			if (classpathBuffer.length() > 0 && classpathBuffer.charAt(classpathBuffer.length() - 1) != pathSeparator) {
				classpathBuffer.append(pathSeparator);
			}

			classpathBuffer.append(classpathEntry);
		}

		String classpath = classpathBuffer.toString();
		String logMessage = AbstractScriptNode.getTimestampPrefix() + " Setting classpath" + (this.multiReleaseVersion == null ? "" : " for multi-release version " + this.multiReleaseVersion);
		System.out.println(logMessage + "...");
		AtomicReference<String> normalizedClasspathRef = new AtomicReference<>();
		AtomicReference<String> invalidPathMessageRef = new AtomicReference<>();
		boolean classpathNormalized = ClasspathManager.normalizeAndExpandClasspath(classpath, scriptContext.getDefaultDirectory(), normalizedClasspathRef, logger, invalidPathMessageRef);
		classpath = normalizedClasspathRef.get();
		if (!classpathNormalized) {
			scriptContext.logError("Invalid classpath in \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " : " + invalidPathMessageRef.get());
			System.err.println(StringUtils.repeat(AbstractScriptNode.getTimestampPrefix().length() + 1, 32) + "Error detected while setting classpath.");
		}

		String oldClasspath = classpathManager.getRawClasspath();
		classpathManager.setClasspath(classpath);
		AtomicReference<String> legacyBootCpRef = new AtomicReference<>();
		AtomicReference<String> jrtFsRef = new AtomicReference<>();
		if (classpathManager.initializeBootClasspath(jrtFsRef, legacyBootCpRef)) {
			if (!jrtFsRef.isNull()) {
				writer.println(AbstractScriptNode.getTimestampPrefix() + " Using \"" + jrtFsRef.get() + "\" as path to java.lang.Object (E)");
			} else if (!legacyBootCpRef.isNull()) {
				writer.println(AbstractScriptNode.getTimestampPrefix() + " Using \"" + legacyBootCpRef.get() + "\" as path to java.lang.Object (F)");
			}
		}

		String newClasspath = classpathManager.getRawClasspath();
		writer.println(logMessage + " to \"" + ClasspathManager.formatClasspathTempFileNames(newClasspath) + "\"");
		jrtFsRef.setNull();
		legacyBootCpRef.setNull();
		if (!classpathManager.initializeBootClasspath(jrtFsRef, legacyBootCpRef)) {
			scriptContext.logFatalError(
				"Could not find java.lang.Object : '"
					+ newClasspath
					+ "' : java.home='"
					+ SystemConstants.javaHome
					+ "' '"
					+ SystemConstants.javaVendor
					+ "' '"
					+ SystemConstants.javaVMVersion
					+ "' '"
					+ SystemConstants.osName
					+ "' '"
					+ SystemConstants.osVersion
					+ "'"
			);
		} else if (!legacyBootCpRef.isNull()) {
			String legacyBootCp = legacyBootCpRef.get();
			writer.println("\tAdding '" + legacyBootCp + "' to classpath. Must be able to find java.lang.Object.");
		}

		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
		if (scriptContext.isVerbose()) {
			writer.println("\tChanging classpath" + (this.multiReleaseVersion == null ? "" : " for multi-release version " + this.multiReleaseVersion) + "...");
			writer.println("\tfrom\t\"" + oldClasspath + "\"");
			writer.println("\tto\t\"" + classpathManager.getResolvedClasspath() + "\"");
		}
	}
}

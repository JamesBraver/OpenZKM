package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ProcessStepStatement;
import com.zelix.klassmaster.script.statements.exclusion.ExistingSerializedClassesStatement;
import com.zelix.klassmaster.script.statements.exclusion.FixedClassesStatement;
import com.zelix.klassmaster.script.statements.exclusion.TrimExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.TrimUnexcludeStatement;
import com.zelix.logging.LogContextLogger;
import com.zelix.utils.NoDuplicatePrintWriter;
import com.zelix.utils.function.EmptyConsumer;
import com.zelix.utils.io.DirectoryScanner;
import com.zelix.workspace.Workspace;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class TrimStatement extends ProcessStepStatement {
	public TrimStatement(int nodeType) {
		super(nodeType);
	}

	protected void applyDeleteDebugExtensionAttributes(TrimConfig config, ScriptContext scriptContext) {
		config.deleteDebugExtensionAttributes = true;
		List<String> argumentValues = super.parameterValues.get("deleteDebugExtensionAttributes");
		if (argumentValues != null && argumentValues.size() > 0) {
			String argumentValue = argumentValues.get(0);
			if (argumentValue != null && argumentValue.equals("false")) {
				config.deleteDebugExtensionAttributes = false;
			}
		}
	}

	@Override
	public String getCommandName() {
		return "trim";
	}

	protected void applyDeleteAnnotationAttributes(TrimConfig config, ScriptContext scriptContext) {
		config.deleteAnnotationAttributes = false;
		List<String> argumentValues = super.parameterValues.get("deleteAnnotationAttributes");
		if (argumentValues != null && argumentValues.size() > 0) {
			String argumentValue = argumentValues.get(0);
			if (argumentValue != null && argumentValue.equals("true")) {
				config.deleteAnnotationAttributes = true;
			}
		}
	}

	protected void applyDeleteSourceFileAttributes(TrimConfig config, ScriptContext scriptContext) {
		config.deleteSourceFileAttributes = false;
		List<String> argumentValues = super.parameterValues.get("deleteSourceFileAttributes");
		if (argumentValues != null && argumentValues.size() > 0) {
			String argumentValue = argumentValues.get(0);
			if (argumentValue != null && argumentValue.equals("true")) {
				config.deleteSourceFileAttributes = true;
			}
		}
	}

	protected String getStepLabel() {
		return "Trimming";
	}

	protected void applyDeleteExceptionAttributes(TrimConfig config, ScriptContext scriptContext) {
		config.deleteExceptionAttributes = false;
		List<String> argumentValues = super.parameterValues.get("deleteExceptionAttributes");
		if (argumentValues != null && argumentValues.size() > 0) {
			String argumentValue = argumentValues.get(0);
			if (argumentValue != null && argumentValue.equals("true")) {
				config.deleteExceptionAttributes = true;
			}
		}
	}

	protected void applyDeleteDeprecatedAttributes(TrimConfig config, ScriptContext scriptContext) {
		config.deleteDeprecatedAttributes = true;
		List<String> argumentValues = super.parameterValues.get("deleteDeprecatedAttributes");
		if (argumentValues != null && argumentValues.size() > 0) {
			String argumentValue = argumentValues.get(0);
			if (argumentValue != null && argumentValue.equals("false")) {
				config.deleteDeprecatedAttributes = false;
			}
		}
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) throws IOException {
		int initialSpecialErrorCount = scriptContext.getSpecialErrorCount();
		Workspace workspace = scriptContext.getWorkspace();
		if (!workspace.hasWorkspaceClasses()) {
			scriptContext.logFatalError("Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " before opening classes");
		} else if (workspace.isEmpty()) {
			scriptContext.logFatalError("Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " with no classes opened");
		} else if (!workspace.isErrorEmpty()) {
			scriptContext.logFatalError("Attempt to use \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " with unusable classes : " + workspace.getError());
		} else if (!workspace.hasClasses()) {
			scriptContext.logMessage(
				"Only module-info.class classes opened. Statement \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + " will have no effect."
			);
			return;
		}

		PrintWriter writer = scriptContext.getWriter();
		LogContextLogger logger = new LogContextLogger(scriptContext, AbstractScriptNode.getTimestampPrefix().length());
		EmptyConsumer emptyConsumer = EmptyConsumer.create();
		NoDuplicatePrintWriter trimLogWriter = null;

		try {
			File trimLogFile = new File(scriptContext.getTrimLogFilePath());
			File trimLogDirectory = trimLogFile.getParentFile();
			if (!trimLogDirectory.exists()) {
				DirectoryScanner.ensureDirectory(trimLogDirectory.getAbsolutePath());
			}

			trimLogWriter = new NoDuplicatePrintWriter(new FileWriter(trimLogFile), true);
		} catch (IOException ioException) {
			logger.error("ERROR:", "Couldn't open " + scriptContext.getTrimLogFilePath() + " : " + ioException.getClass().getName());
		}

		String logMessage = AbstractScriptNode.getTimestampPrefix() + " " + this.getStepLabel() + "...";
		writer.println(logMessage);
		System.out.println(logMessage);
		TrimConfig config = new TrimConfig();
		this.applyDeleteSourceFileAttributes(config, scriptContext);
		this.applyDeleteDeprecatedAttributes(config, scriptContext);
		this.applyDeleteAnnotationAttributes(config, scriptContext);
		this.applyDeleteUnknownAttributes(config, scriptContext);
		this.applyDeleteExceptionAttributes(config, scriptContext);
		this.applyDeleteDebugExtensionAttributes(config, scriptContext);
		if (scriptContext.isVerbose()) {
			writer.println("\tdeleteSourceFileAttributes=" + config.deleteSourceFileAttributes);
			writer.println("\tdeleteDeprecatedAttributes=" + config.deleteDeprecatedAttributes);
			writer.println("\tdeleteAnnotationAttributes=" + config.deleteAnnotationAttributes);
			writer.println("\tdeleteUnknownAttributes=" + config.deleteUnknownAttributes);
			writer.println("\tdeleteExceptionAttributes=" + config.deleteExceptionAttributes);
			writer.println("\tdeleteDebugExtensionAttributes=" + config.deleteDebugExtensionAttributes);
		}

		List<TrimExcludeStatement> trimExcludeStatements = scriptContext.getTrimExcludeStatements();
		List<TrimUnexcludeStatement> trimUnexcludeStatements = scriptContext.getTrimUnexcludeStatements();
		List<ExistingSerializedClassesStatement> existingSerializedClassesStatements = scriptContext.getExistingSerializedClassesStatements();
		List<FixedClassesStatement> fixedClassesStatements = scriptContext.getFixedClassesStatements();
		workspace.trim(config, trimExcludeStatements, trimUnexcludeStatements, existingSerializedClassesStatements, fixedClassesStatements, trimLogWriter, logger, emptyConsumer, null, scriptContext);
		if (trimLogWriter != null) {
			trimLogWriter.close();
		}

		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
		if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
			logger.fatalErrorWithDetails("FATAL ERROR:", "Serious Errors detected during " + this.getCommandName(), scriptContext.getSpecialErrorsAsString());
		}
	}

	protected void applyDeleteUnknownAttributes(TrimConfig config, ScriptContext scriptContext) {
		config.deleteUnknownAttributes = false;
		List<String> argumentValues = super.parameterValues.get("deleteUnknownAttributes");
		if (argumentValues != null && argumentValues.size() > 0) {
			String argumentValue = argumentValues.get(0);
			if (argumentValue != null && argumentValue.equals("true")) {
				config.deleteUnknownAttributes = true;
			}
		}
	}
}

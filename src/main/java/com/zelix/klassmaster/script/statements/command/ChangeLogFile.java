package com.zelix.klassmaster.script.statements.command;

import com.zelix.exceptions.MappingParserException;
import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.mapping.MappingParser;
import com.zelix.klassmaster.mapping.entry.MappingInputNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ActionParameter;
import com.zelix.klassmaster.script.statements.base.ProcessStepStatement;
import com.zelix.klassmaster.script.statements.exclusion.ExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.UnexcludeStatement;
import com.zelix.logging.AbstractLogger;
import com.zelix.logging.LogContextLogger;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;
import com.zelix.utils.FileUtils;
import com.zelix.utils.dataset.ChangeLogFileData;
import com.zelix.utils.function.EmptyConsumer;
import com.zelix.utils.io.DirectoryScanner;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.Workspace;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class ChangeLogFile extends ProcessStepStatement {
	public ChangeLogFile(int nodeType) {
		super(nodeType);
	}

	protected void applyAllClassesOpened(ObfuscateConfig config, ScriptContext scriptContext) {
		config.allClassesOpened = true;
		List<String> parameterValues = super.parameterValues.get("allClassesOpened");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("false")) {
				config.allClassesOpened = false;
			}
		}
	}

	protected void applyChangeLogFileIn(ObfuscateConfig config, ScriptContext scriptContext) {
		config.changeLogFileIn = null;
		List<String> changeLogFileInValues = null;
		if (super.parameterValues.containsKey("changeLogFileIn")) {
			changeLogFileInValues = super.parameterValues.get("changeLogFileIn");
		} else if (super.parameterValues.containsKey("looseChangeLogFileIn")) {
			changeLogFileInValues = super.parameterValues.get("looseChangeLogFileIn");
			config.isLooseChangeLogFile = true;
		}

		ArrayList<ChangeLogFileData> changeLogFileDataList = new ArrayList<>();
		if (changeLogFileInValues != null) {
			for (int i = 0; i < changeLogFileInValues.size(); i++) {
				String changeLogFilePath = changeLogFileInValues.get(i);
				if (changeLogFilePath != null && changeLogFilePath.length() > 0) {
					String normalizedChangeLogFilePath = FileUtils.normalizePathSeparators(changeLogFilePath);
					File changeLogInFile;
					if (FileUtils.isRelativePath(normalizedChangeLogFilePath)) {
						changeLogInFile = new File(scriptContext.getDefaultDirectory(), normalizedChangeLogFilePath);
					} else {
						changeLogInFile = new File(normalizedChangeLogFilePath);
					}

					if (changeLogInFile.isDirectory()) {
						scriptContext.logFatalError(
							"Input change log file cannot be a directory : \""
								+ this.getCommandName()
								+ "\" statement at line "
								+ this.getLineNumber()
								+ " : \""
								+ changeLogInFile.getAbsolutePath()
								+ "\""
						);
					}

					String changeLogInAbsolutePath = changeLogInFile.getAbsolutePath();
					String zkmLogFilePath = scriptContext.getLogFilePath();
					this.checkFileNameClash(changeLogInAbsolutePath, zkmLogFilePath, "ZKM log", scriptContext);
					String trimLogFilePath = scriptContext.getTrimLogFilePath();
					this.checkFileNameClash(changeLogInAbsolutePath, trimLogFilePath, "trim log", scriptContext);
					ChangeLogFileData changeLogFileData;
					if (scriptContext.isImplementsClauseMatching() && this.isProguardMappingFile(changeLogInAbsolutePath, scriptContext)) {
						String translatedFilePath = this.translateMappingFile(changeLogInAbsolutePath, scriptContext);
						changeLogFileData = new ChangeLogFileData(translatedFilePath);
						changeLogInFile = new File(translatedFilePath);
						changeLogInAbsolutePath = changeLogInFile.getAbsolutePath();
					} else {
						changeLogFileData = new ChangeLogFileData(changeLogFilePath);
					}

					try {
						changeLogFileData.setReader(this.createChangeLogReader(changeLogInFile));
						changeLogFileDataList.add(changeLogFileData);
					} catch (IOException ioException) {
						scriptContext.logFatalError(
							"Change log file \""
								+ changeLogInAbsolutePath
								+ "\" could not be opened in \""
								+ this.getCommandName()
								+ "\" statement at line "
								+ this.getLineNumber()
								+ " : "
								+ ioException
								+ " : (1)"
						);
					}
				}
			}

			if (changeLogFileDataList.size() > 0) {
				config.changeLogFileIn = changeLogFileDataList.toArray(new ChangeLogFileData[changeLogFileDataList.size()]);
			}
		}
	}

	private boolean isProguardMappingFile(String fileName, ScriptContext scriptContext) {
		try {
			String fileContent = FileUtils.readToString(new File(fileName));
			return fileContent.indexOf("->") != -1 && fileContent.indexOf("=>") == -1;
		} catch (IOException ignored) {
			scriptContext.logSpecialError("Error reading file '" + fileName + "'");
			return false;
		}
	}

	protected Reader createChangeLogReader(File changeLogFile) throws IOException {
		String encoding = ChangeLogContext.getLogFileEncoding(changeLogFile);
		BufferedReader reader;
		if (encoding != null) {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(changeLogFile), encoding));
		} else {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(changeLogFile)));
		}

		StringBuffer contentBuffer = new StringBuffer();

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				contentBuffer.append(line);
				contentBuffer.append(Options.lineSeparator);
			}
		} finally {
			reader.close();
		}
		return new StringReader(contentBuffer.toString());
	}

	protected void applyKeepInnerClassInfo(ObfuscateConfig config, ScriptContext scriptContext) {
		config.keepInnerClassInfo = 1;
		List<String> parameterValues = super.parameterValues.get("keepInnerClassInfo");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("true")) {
					config.keepInnerClassInfo = 0;
				} else if (parameterValue.equals("ifNameNotObfuscated")) {
					config.keepInnerClassInfo = 2;
				}
			}
		}
	}

	private void checkFileNameClash(String changeLogFileName, String otherFileName, String otherFileDescription, ScriptContext scriptContext) {
		if (changeLogFileName.equals(otherFileName) || !FileUtils.IS_UNIX && changeLogFileName.equalsIgnoreCase(otherFileName)) {
			scriptContext.logFatalError(
				"Change log file \"" + changeLogFileName + "\" clashes with " + otherFileDescription + " file name in \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber()
			);
		}
	}

	@Override
	public boolean validateParameter(ActionParameter parameter, ScriptContext scriptContext) {
		String parameterName = parameter.getParameterName();
		if (super.parameterValues.containsKey("changeLogFileIn") && parameterName.equals("looseChangeLogFileIn")) {
			scriptContext.logError(
				"\"changeLogFileIn\" and \"looseChangeLogFileIn\" appear in \""
					+ this.getCommandName()
					+ "\" statement at line "
					+ this.getLineNumber()
					+ ". They are mutually exclusive. Only \"changeLogFileIn\" will be used. (3)"
			);
			return false;
		} else if (super.parameterValues.containsKey("looseChangeLogFileIn") && parameterName.equals("changeLogFileIn")) {
			scriptContext.logError(
				"\"looseChangeLogFileIn\" and \"changeLogFileIn\" appear in \""
					+ this.getCommandName()
					+ "\" statement at line "
					+ this.getLineNumber()
					+ ". They are mutually exclusive. Only \"looseChangeLogFileIn\" will be used. (4)"
			);
			return false;
		} else {
			return true;
		}
	}

	public String getParameterValue(String parameterName) {
		List<String> parameterValues = super.parameterValues.get(parameterName);
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			return parameterValue != null && parameterValue.length() > 0 ? parameterValue : null;
		} else {
			return null;
		}
	}

	protected void applyRandomizeObfuscation(ObfuscateConfig config, ScriptContext scriptContext) {
		config.randomizeObfuscation = false;
		List<String> parameterValues = super.parameterValues.get("randomize");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("true")) {
				config.randomizeObfuscation = true;
			}
		}
	}

	@Override
	protected final void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) throws IOException {
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
		EmptyConsumer completionConsumer = EmptyConsumer.create();
		String startMessage = AbstractScriptNode.getTimestampPrefix() + " " + this.getActionName() + " classes...";
		writer.println(startMessage);
		System.out.println(startMessage);
		ObfuscateConfig config = new ObfuscateConfig();
		this.applyChangeLogFileIn(config, scriptContext);
		this.applyChangeLogFileOut(config, scriptContext);
		this.applyAggressiveRenaming(config, scriptContext);
		this.applyRandomizeObfuscation(config, scriptContext);
		this.applyAllClassesOpened(config, scriptContext);
		this.applyKeepInnerClassInfo(config, scriptContext);
		List<ExcludeStatement> excludeStatements = scriptContext.getExcludeStatements();
		List<UnexcludeStatement> unexcludeStatements = scriptContext.getUnexcludeStatements();
		this.applyStatementConfig(config, scriptContext);
		if (scriptContext.isVerbose()) {
			StringBuffer changeLogFileInBuffer = new StringBuffer();
			if (config.isLooseChangeLogFile) {
				changeLogFileInBuffer.append("\tlooseChangeLogFileIn=");
			} else {
				changeLogFileInBuffer.append("\tchangeLogFileIn=");
			}

			if (config.changeLogFileIn != null && config.changeLogFileIn.length != 0) {
				for (int i = 0; i < config.changeLogFileIn.length; i++) {
					if (i > 0) {
						changeLogFileInBuffer.append(", ");
					}

					changeLogFileInBuffer.append("\"" + config.changeLogFileIn[i].getPath() + "\"");
				}
			} else {
				changeLogFileInBuffer.append("\"\"");
			}

			writer.println(changeLogFileInBuffer.toString());
			writer.println("\tchangeLogFileOut=\"" + config.changeLogFileOut + "\"");
			writer.println("\taggressiveMethodRenaming=" + config.aggressiveMethodRenaming);
			writer.println("\trandomize=" + config.randomizeObfuscation);
			writer.println("\tallClassesOpened=" + config.allClassesOpened);
			String keepInnerClassInfoText = null;
			switch (config.keepInnerClassInfo) {
				case 0:
					keepInnerClassInfoText = "true";
					break;
				case 1:
					keepInnerClassInfoText = "false";
					break;
				case 2:
					keepInnerClassInfoText = "ifNameNotObfuscated";
			}

			writer.println("\tkeepInnerClassInfo=" + keepInnerClassInfoText);
		}

		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "in parse of");
		initialMessageCount = scriptContext.getMessageCount();
		initialWarningCount = scriptContext.getWarningCount();
		initialErrorCount = scriptContext.getErrorCount();
		int initialSpecialErrorCount = scriptContext.getSpecialErrorCount();
		this.executeStatement(excludeStatements, unexcludeStatements, config, scriptContext, completionConsumer, logger);
		if (config.changeLogPrintWriter != null) {
			config.changeLogPrintWriter.close();
			config.changeLogPrintWriter = null;
		}

		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
		if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
			logger.fatalErrorWithDetails(
				"FATAL ERROR:", "Serious Errors detected during execution of '" + this.getCommandName() + "' statement.", scriptContext.getSpecialErrorsAsString()
			);
		}
	}

	protected abstract void applyStatementConfig(ObfuscateConfig config, ScriptContext scriptContext);

	private String translateMappingFile(String fileName, ScriptContext scriptContext) {
		BufferedReader mappingFileReader = null;

		String translatedFilePath;
		try {
			mappingFileReader = FileUtils.createBufferedReader(fileName);
			if (MappingParser.instance == null) {
				new MappingParser(mappingFileReader);
			} else {
				MappingParser.reInit(mappingFileReader);
			}

			MappingInputNode mappingInputNode = (MappingInputNode)MappingParser.parseInput();
			ProguardTranslateWriter translateWriter = new ProguardTranslateWriter(fileName);
			mappingInputNode.interpret(null, translateWriter);
			String translation = translateWriter.buildTranslation();
			AtomicReference<String> errorMessageReference = new AtomicReference<>();
			File tempFile = FileUtils.writeToTempFile(translation, errorMessageReference);
			if (tempFile != null) {
				scriptContext.logMessage("Mapping file '" + fileName + "' into " + CopyrightConstants.NAME + " format : '" + errorMessageReference.get() + "'");
				return tempFile.getAbsolutePath();
			}

			scriptContext.logSpecialError("Error creating temporary file while translating '" + fileName + "' into " + CopyrightConstants.NAME + " format : '" + errorMessageReference.get() + "'");
			translatedFilePath = fileName;
		} catch (IOException ioException) {
			scriptContext.logSpecialError("Error translating '" + fileName + "' into " + CopyrightConstants.NAME + " format : '" + ioException.getMessage() + "' (A)");
			return fileName;
		} catch (MappingParserException mappingParserException) {
			scriptContext.logSpecialError("Error translating '" + fileName + "' into " + CopyrightConstants.NAME + " format : '" + mappingParserException.getMessage() + "' (B)");
			return fileName;
		} finally {
			try {
				if (mappingFileReader != null) {
					mappingFileReader.close();
				}
			} catch (IOException ignored) {
			}
		}

		return translatedFilePath;
	}

	protected abstract void executeStatement(
		List<ExcludeStatement> excludeStatements,
		List<UnexcludeStatement> unexcludeStatements,
		ObfuscateConfig config,
		ScriptContext scriptContext,
		EmptyConsumer completionConsumer,
		AbstractLogger logger
	) throws IOException;

	protected void applyAggressiveRenaming(ObfuscateConfig config, ScriptContext scriptContext) {
		config.aggressiveMethodRenaming = false;
		List<String> parameterValues = super.parameterValues.get("aggressiveMethodRenaming");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("true")) {
				config.aggressiveMethodRenaming = true;
			}
		}
	}

	protected void applyChangeLogFileOut(ObfuscateConfig config, ScriptContext scriptContext) {
		List<String> changeLogFileOutValues = super.parameterValues.get("changeLogFileOut");
		if (changeLogFileOutValues == null) {
			changeLogFileOutValues = super.parameterValues.get("changeLogFile");
		}

		config.changeLogPrintWriter = null;
		if (changeLogFileOutValues != null && changeLogFileOutValues.size() > 0) {
			config.changeLogFileOut = changeLogFileOutValues.get(0).trim();
		} else {
			config.changeLogFileOut = "ChangeLog.txt";
		}

		if (config.changeLogFileOut.length() > 0) {
			String normalizedChangeLogOutPath = FileUtils.normalizePathSeparators(config.changeLogFileOut);
			File changeLogOutFile = null;

			try {
				if (FileUtils.isRelativePath(normalizedChangeLogOutPath)) {
					changeLogOutFile = new File(scriptContext.getDefaultDirectory(), normalizedChangeLogOutPath);
				} else {
					changeLogOutFile = new File(normalizedChangeLogOutPath);
				}

				if (changeLogOutFile.isDirectory()) {
					scriptContext.logFatalError(
						"Change log file cannot be a directory : \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ " : \""
							+ changeLogOutFile.getAbsolutePath()
							+ "\""
					);
				}

				int lastSeparatorIndex = normalizedChangeLogOutPath.lastIndexOf(SystemConstants.fileSeparator);
				if (lastSeparatorIndex > 0) {
					DirectoryScanner.ensureDirectory(normalizedChangeLogOutPath.substring(0, lastSeparatorIndex));
				}

				String changeLogOutAbsolutePath = changeLogOutFile.getAbsolutePath();
				String zkmLogFilePath = scriptContext.getLogFilePath();
				this.checkFileNameClash(changeLogOutAbsolutePath, zkmLogFilePath, "ZKM log", scriptContext);
				String trimLogFilePath = scriptContext.getTrimLogFilePath();
				this.checkFileNameClash(changeLogOutAbsolutePath, trimLogFilePath, "trim log", scriptContext);
				String encoding = AbstractChangeLogContext.getEncoding();
				if (encoding != null) {
					config.changeLogPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(changeLogOutFile), encoding), 2048));
					config.useFileEncoding = encoding;
				} else {
					OutputStreamWriter utf8Writer = new OutputStreamWriter(new FileOutputStream(changeLogOutFile), "UTF-8");
					config.changeLogPrintWriter = new PrintWriter(new BufferedWriter(utf8Writer, 2048));
					config.useFileEncoding = "UTF-8";
				}
			} catch (IOException ioException) {
				scriptContext.logFatalError(
					"Change log file \""
						+ changeLogOutFile.getAbsolutePath()
						+ "\" could not be opened in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ " : "
						+ ioException
						+ " : (2)"
				);
			}
		} else {
			config.changeLogFileOut = null;
		}
	}

	protected abstract String getActionName();
}

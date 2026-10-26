package com.zelix.tools.proguardConfigurationTranslate;

import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.exceptions.ProguardConfigSyntaxException;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.tools.proguardConfigurationTranslate.parser.ProguardConfigRootNode;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.OrderedMap;
import com.zelix.utils.collections.Stack;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.references.AtomicReference;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ProguardConfigUtils {
	private static int findNextOption(String configText, int fromIndex, AtomicReference<String> optionRef) {
		int[] commandIndexes = new int[ProguardCommandsConstants.PROGUARD_COMMANDS.length];

		for (int commandIndex = 0; commandIndex < commandIndexes.length; commandIndex++) {
			commandIndexes[commandIndex] = configText.indexOf(ProguardCommandsConstants.PROGUARD_COMMANDS[commandIndex], fromIndex);
		}

		int optionStartIndex = -1;

		for (int candidateCommandIndex = 0; candidateCommandIndex < commandIndexes.length; candidateCommandIndex++) {
			if (commandIndexes[candidateCommandIndex] > -1 && (optionStartIndex == -1 || commandIndexes[candidateCommandIndex] < optionStartIndex)) {
				optionStartIndex = commandIndexes[candidateCommandIndex];
				optionRef.set(ProguardCommandsConstants.PROGUARD_COMMANDS[candidateCommandIndex]);
			}
		}

		return optionStartIndex > -1 ? optionStartIndex + optionRef.get().length() : optionStartIndex;
	}

	private static String substituteProperties(String text, Properties userProperties, Properties systemProperties) {
		StringBuilder outputBuffer = new StringBuilder((int)(text.length() * 1.5));
		int scanFrom = 0;

		int placeholderStart;
		while ((placeholderStart = text.indexOf("<", scanFrom)) > -1) {
			outputBuffer.append(text.substring(scanFrom, placeholderStart));
			int placeholderEnd = text.indexOf(">", placeholderStart + 1);
			if (placeholderEnd > -1 && placeholderEnd > placeholderStart + 1) {
				String propertyName = text.substring(placeholderStart + 1, placeholderEnd);
				String propertyValue = null;
				if (userProperties != null) {
					propertyValue = userProperties.getProperty(propertyName);
				}

				if (propertyValue == null) {
					propertyValue = systemProperties.getProperty(propertyName);
				}

				if (propertyValue != null) {
					outputBuffer.append(propertyValue);
					scanFrom = placeholderEnd + 1;
				} else {
					outputBuffer.append("<");
					scanFrom = placeholderStart + 1;
				}
			} else {
				outputBuffer.append("<");
				scanFrom = placeholderStart + 1;
			}
		}

		outputBuffer.append(text.substring(scanFrom));
		return outputBuffer.toString();
	}

	public static String translateProGuardConfig(String configText, Properties properties) {
		AtomicReference<Reader> readerRef = new AtomicReference<>();
		StringWriter logBuffer = new StringWriter();
		AtomicReference<String> tempFileErrorRef = new AtomicReference<>();
		File tempInputFile = FileUtils.writeToTempFile(configText, tempFileErrorRef);
		if (!tempFileErrorRef.isNull()) {
			return tempFileErrorRef.get();
		} else {
			String errorText;
			try {
				PrintWriter logWriter = new PrintWriter(logBuffer);
				ProguardScriptTranslator scriptTranslator = buildScriptTranslator(tempInputFile, logWriter, readerRef, properties);
				ArrayList<String> reportedMessages = new ArrayList<>();
				return scriptTranslator.generateScript("ProGuardInputTranslator", true, true, reportedMessages);
			} catch (DebugAssertFailedException debugAssertFailure) {
				throw debugAssertFailure;
			} catch (Throwable throwable) {
				Throwable logThrowable = throwable;
				String errorMessage;
				if (logBuffer != null) {
					logBuffer.flush();
					errorText = logBuffer.toString();
					File errorLogFile = new File("ZKM_PG_log.txt");
					PrintWriter errorLogWriter = null;

					try {
						errorMessage = "ERROR: Error while translating input from ProGuard to " + CopyrightConstants.NAME + " format : '" + logThrowable.getMessage() + "' (B)";
						errorLogWriter = new PrintWriter(new FileWriter(errorLogFile));
						errorLogWriter.println(errorText);
						errorLogWriter.println(errorMessage);
						errorMessage = errorMessage
							+ DebugUtils.LINE_SEPARATOR
							+ "See '"
							+ errorLogFile.getAbsolutePath()
							+ "' for more detail. Any error message line numbers will refer to the content appearing two lines after the 'Expanded and preprocessed ProGuard style input' line.";
						errorLogWriter.flush();
					} catch (IOException ignoredLogWrite) {
						errorMessage = "ERROR: Error while translating input from ProGuard to " + CopyrightConstants.NAME + " format : '" + logThrowable.getMessage() + "' (C)";
					} finally {
						if (errorLogWriter != null) {
							errorLogWriter.close();
						}
					}
				} else {
					errorMessage = "ERROR: Error while translating input from ProGuard to " + CopyrightConstants.NAME + " format : '" + throwable.getMessage() + "' (D)";
				}

				errorText = errorMessage;
			} finally {
				try {
					if (logBuffer != null) {
						logBuffer.close();
					}
				} catch (IOException ignoredLogBufferClose) {
				}

				try {
					if (readerRef.get() != null) {
						readerRef.get().close();
					}
				} catch (IOException ignoredReaderClose) {
				}
			}

			return errorText;
		}
	}

	private static String expandIncludes(String configText, Properties userProperties, Properties systemProperties, Stack<File> includeStack, Map<File, String> fileContents, PrintWriter logWriter) {
		StringBuilder outputBuffer = new StringBuilder();
		int scanFrom = 0;

		for (int includeIndex = configText.indexOf("-include"); includeIndex > -1; includeIndex = configText.indexOf("-include", scanFrom)) {
			if (includeIndex - 1 > scanFrom) {
				String textBeforeInclude = configText.substring(scanFrom, includeIndex);
				outputBuffer.append(textBeforeInclude);
				outputBuffer.append(Options.lineSeparator);
			}

			int fileNameStart = StringUtils.skipWhitespace(configText, includeIndex + 8);
			if (fileNameStart == -1) {
				throw new ProguardConfigSyntaxException("No contents for '-include' in file '" + includeStack.peek().getAbsolutePath() + "'");
			}

			int fileNameEnd = fileNameStart;
			char currentChar = configText.charAt(fileNameStart);

			for (boolean inQuotes = false; !Character.isWhitespace(currentChar) || currentChar == ' ' && inQuotes; currentChar = configText.charAt(++fileNameEnd)) {
				if (currentChar == '"') {
					inQuotes = !inQuotes;
				}
			}

			String includeFileName = configText.substring(fileNameStart, fileNameEnd);
			if (includeFileName.startsWith("\"") && includeFileName.endsWith("\"")) {
				includeFileName = includeFileName.substring(1, includeFileName.length() - 1);
			}

			if (includeFileName.indexOf("\"") > -1) {
				throw new ProguardConfigSyntaxException("Broken string literal in '-include' in file '" + includeStack.peek().getAbsolutePath() + "' : '" + includeFileName + "'");
			}

			String includedContent = readAndExpandIncludes(includeFileName, userProperties, systemProperties, includeStack, fileContents, logWriter);
			outputBuffer.append(includedContent);
			if (!includedContent.endsWith(Options.lineSeparator)) {
				outputBuffer.append(Options.lineSeparator);
			}

			scanFrom = fileNameEnd + 1;
		}

		outputBuffer.append(configText.substring(scanFrom, configText.length()));
		return outputBuffer.toString();
	}

	private static ProguardScriptTranslator buildScriptTranslator(File inputFile, PrintWriter logWriter, AtomicReference<Reader> readerRef, Properties userProperties) throws IOException {
		OrderedMap<File, String> fileContents = new OrderedMap<>();
		logWriter.println("");
		logWriter.println("Temporary input file '" + inputFile.getAbsolutePath() + "' contents.");
		logWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
		logWriter.println(FileUtils.readToString(inputFile));
		logWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
		logWriter.println("");
		String expandedConfigText = quoteOptionArguments(readAndExpandIncludes(inputFile.getAbsolutePath(), userProperties, System.getProperties(), new Stack<>(), fileContents, logWriter));
		logWriter.println("");
		logWriter.println("Expanded and preprocessed ProGuard style input. Any line numbers in error messages will refer to this content.");
		logWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
		logWriter.println(expandedConfigText.replace('~', FileUtils.PATH_SEPARATOR_CHAR));
		logWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
		StringReader configReader = new StringReader(expandedConfigText);
		readerRef.set(configReader);
		ProguardConfigRootNode configRootNode = (ProguardConfigRootNode)new ProguardConfigParser(configReader).parseConfiguration();
		ProguardScriptTranslator scriptTranslator = new ProguardScriptTranslator(logWriter, true);
		configRootNode.parseArguments(null, scriptTranslator);
		return scriptTranslator;
	}

	private static boolean isArgumentDelimiter(int ch) {
		return ch == 40 || ch == 13 || ch == 10 || ch == SystemConstants.pathSep;
	}

	private static String findAndReadScript(AtomicReference<String> scriptFileRef, String inputDirectory, Set<String> searchedDirs) throws IOException {
		String scriptEncoding = Options.ZKM_SCRIPT_ENCODING != null ? Options.ZKM_SCRIPT_ENCODING : FileUtils.DEFAULT_ENCODING;
		if (Options.ZKM_DEFAULT_DIR != null) {
			searchedDirs.add(Options.ZKM_DEFAULT_DIR);
			String defaultDirScript = readScriptInDir(Options.ZKM_DEFAULT_DIR, scriptEncoding, scriptFileRef);
			if (defaultDirScript != null) {
				return defaultDirScript;
			}
		}

		if (inputDirectory != null) {
			searchedDirs.add(inputDirectory);
			String inputDirScript = readScriptInDir(inputDirectory, scriptEncoding, scriptFileRef);
			if (inputDirScript != null) {
				return inputDirScript;
			}
		}

		searchedDirs.add(FileUtils.USER_DIRECTORY_PATH);
		String userDirScript = readScriptInDir(FileUtils.USER_DIRECTORY_PATH, scriptEncoding, scriptFileRef);
		return userDirScript != null ? userDirScript : null;
	}

	private static String readScriptInDir(String directory, String encoding, AtomicReference<String> scriptFileRef) throws IOException {
		File scriptFile = new File(directory, "ZKMScript.txt");
		if (scriptFile.exists() && !scriptFile.isDirectory()) {
			scriptFileRef.set(scriptFile.getAbsolutePath());
			CommentStripper commentStripper = new CommentStripper();
			String scriptText = FileUtils.readToString(scriptFile, encoding, commentStripper);
			int commentStart = scriptText.indexOf("/*");
			if (commentStart > -1) {
				StringBuilder outputBuffer = new StringBuilder();

				while (commentStart > -1) {
					outputBuffer.append(scriptText.substring(0, commentStart));
					int commentEnd = scriptText.indexOf("*/", commentStart + 2);
					if (commentEnd > -1) {
						scriptText = scriptText.substring(commentEnd + 2);
						commentStart = scriptText.indexOf("/*");
					} else {
						commentStart = -1;
					}
				}

				outputBuffer.append(scriptText);
				return outputBuffer.toString();
			} else {
				return scriptText;
			}
		} else {
			return null;
		}
	}

	public static String translateProGuardConfig(
		String[] args,
		Properties properties,
		AtomicReference<String> logFilePathRef,
		AtomicReference<String> trimLogFilePathRef,
		AtomicReference<String> defaultDirRef,
		AtomicBool verboseFlagRef,
		String logFileName,
		PrintWriter externalWriter,
		boolean shouldExitOnError
	) {
		externalWriter.println("");
		externalWriter.println("Raw input");
		externalWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

		for (int i = 0; i < args.length; i++) {
			externalWriter.println(args[i]);
		}

		externalWriter.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
		StringBuilder configTextBuffer = new StringBuilder();

		for (int argIndex = 0; argIndex < args.length; argIndex++) {
			if (configTextBuffer.length() > 0) {
				configTextBuffer.append(" ");
			}

			if (args[argIndex].length() > 0 && args[argIndex].charAt(0) == '@') {
				configTextBuffer.append("-include");
				configTextBuffer.append(" ");
				configTextBuffer.append(args[argIndex].substring(1));
			} else {
				configTextBuffer.append(args[argIndex]);
			}
		}

		AtomicReference<String> tempFileErrorRef = new AtomicReference<>();
		File tempInputFile = FileUtils.writeToTempFile(configTextBuffer.toString(), tempFileErrorRef);
		if (tempInputFile == null) {
			String tempInputCreateError = "ERROR: Error while attempting to create temporary file (A) : '" + tempFileErrorRef.get() + "'.";
			if (!shouldExitOnError) {
				throw new RuntimeException(tempInputCreateError);
			}

			logErrorAndExit(tempInputCreateError, logFileName, externalWriter);
		}

		AtomicReference<Reader> readerRef = new AtomicReference<>();
		ProguardScriptTranslator scriptTranslator = null;

		try {
			scriptTranslator = buildScriptTranslator(tempInputFile, externalWriter, readerRef, properties);
		} catch (DebugAssertFailedException debugAssertFailure) {
			throw debugAssertFailure;
		} catch (Throwable translationException) {
			String translationError = "ERROR: Error while translating input from ProGuard to " + CopyrightConstants.NAME + " format : '"
				+ (translationException.getMessage() != null ? translationException.getMessage() : translationException)
				+ "' (A)'. See '"
				+ logFileName
				+ "' for more detail.";
			if (!shouldExitOnError) {
				throw new RuntimeException(translationError);
			}

			logErrorAndExit(translationError, logFileName, externalWriter);
		} finally {
			try {
				if (readerRef.get() != null) {
					readerRef.get().close();
				}
			} catch (IOException ignoredReaderClose) {
			}
		}

		String scriptText = null;
		AtomicReference<String> scriptFileRef = new AtomicReference<>();
		HashSet<String> searchedDirs = GlobalUtils.newHashSet(13);
		String scriptDirectory = scriptTranslator.getScriptDirectory(searchedDirs);
		String logDirectory = ProguardScriptTranslator.resolveLogDirectory(scriptDirectory);
		System.setProperty("ZKM_LOG_DIR", logDirectory);
		System.setProperty("PROGUARD_LOG_DIR", scriptTranslator.getLogDirectory());
		System.setProperty("PROGUARD_SAVE_DIR", scriptTranslator.getSaveDirectory());

		try {
			scriptText = findAndReadScript(scriptFileRef, scriptDirectory, searchedDirs);
		} catch (IOException scriptReadException) {
			String scriptReadError = "FILE ERROR: Error while attempting to open and read : '"
				+ (scriptFileRef.isNull() ? "ZKMScript.txt" : new File(scriptFileRef.get(), "ZKMScript.txt").getAbsolutePath())
				+ "' : '"
				+ scriptReadException.getMessage();
			if (!shouldExitOnError) {
				throw new RuntimeException(scriptReadError);
			}

			logErrorAndExit(scriptReadError, logFileName, externalWriter);
		}

		String searchedDirsMessage = "Searched for 'ZKMScript.txt' in : " + GlobalUtils.toQuotedString(searchedDirs);
		scriptTranslator.logMessage(searchedDirsMessage);
		if (scriptText != null && scriptText.length() > 0) {
			scriptTranslator.extractZKMStatements(scriptText, scriptFileRef.get());
		}

		String generatedScript = scriptTranslator.generateScript("ProGuardInputTranslator", true, true, new ArrayList<>());
		logFilePathRef.set(scriptTranslator.getPrintSeedsFile());
		trimLogFilePathRef.set(scriptTranslator.getPrintUsageFile());
		defaultDirRef.set(scriptTranslator.getBaseDirectory());
		verboseFlagRef.set(scriptTranslator.isVerbose());
		tempFileErrorRef.set(null);
		File tempOutputFile = FileUtils.writeToTempFile(generatedScript, tempFileErrorRef);
		if (tempOutputFile == null) {
			String tempOutputCreateError = "ERROR: Error while attempting to create temporary file (B) : '" + tempFileErrorRef.get() + "'.";
			if (!shouldExitOnError) {
				throw new RuntimeException(tempOutputCreateError);
			}

			logErrorAndExit(tempOutputCreateError, logFileName, externalWriter);
		}

		return tempOutputFile.getAbsolutePath();
	}

	public static String readAndExpandIncludes(String fileName, Properties userProperties, Properties systemProperties, Stack<File> includeStack, Map<File, String> fileContents, PrintWriter logWriter) {
		StringBuilder rawContentBuffer = new StringBuilder();
		StringBuilder strippedContentBuffer = new StringBuilder();
		BufferedReader fileReader = null;
		File includeFile = new File(fileName);
		logWriter.println("Expanding '-include' '" + includeFile.getAbsolutePath() + "'");

		try {
			if (includeStack.search(includeFile) > -1) {
				throw new ProguardConfigSyntaxException(
					"Recursive '-include' of file '" + includeFile.getAbsolutePath() + "' in file '" + includeStack.peek().getAbsolutePath() + "'"
				);
			}

			includeStack.push(includeFile);

			try {
				fileReader = new BufferedReader(new FileReader(includeFile));
			} catch (IOException fileOpenException) {
				throw new ProguardConfigSyntaxException(
					"File error while processing '-include' of file '"
						+ includeFile.getAbsolutePath()
						+ "'"
						+ (includeStack.size() > 1 ? " in file '" + includeStack.peek(1).getAbsolutePath() + "'" : "")
						+ " : '"
						+ fileOpenException.getMessage()
						+ "'"
				);
			}

			String line;
			while ((line = fileReader.readLine()) != null) {
				rawContentBuffer.append(line);
				rawContentBuffer.append(Options.lineSeparator);
				int commentStart = line.indexOf("#");
				if (commentStart > -1) {
					line = line.substring(0, commentStart);
				}

				line = line.trim();
				if (line.length() > 0) {
					strippedContentBuffer.append(line);
					strippedContentBuffer.append(Options.lineSeparator);
				}
			}
		} catch (IOException includeReadException) {
			throw new RuntimeException(includeReadException);
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException ignored) {
				}
			}
		}

		fileContents.put(includeFile, rawContentBuffer.toString());
		String expandedContent = expandIncludes(substituteProperties(strippedContentBuffer.toString(), userProperties, systemProperties), userProperties, systemProperties, includeStack, fileContents, logWriter);
		File poppedFile = includeStack.pop();
		return expandedContent;
	}

	private static int skipConsecutiveOptions(String configText, int fromIndex, AtomicReference<String> optionRef) {
		int optionsEnd = findNextOption(configText, fromIndex, optionRef);
		boolean moreOptions = true;

		while (optionsEnd > 0 && moreOptions) {
			int nextNonWhitespaceIndex = StringUtils.skipWhitespace(configText, optionsEnd);
			if (nextNonWhitespaceIndex != -1 && configText.charAt(nextNonWhitespaceIndex) == '-') {
				optionsEnd = findNextOption(configText, optionsEnd, optionRef);
			} else {
				moreOptions = false;
			}
		}

		return optionsEnd;
	}

	private static void logErrorAndExit(String message, String logFileName, PrintWriter logWriter) {
		if (logWriter != null) {
			logWriter.println(message);
			logWriter.close();
		}

		System.err.println(message);
		System.err.println("See '" + logFileName + "' for more detail.");
		System.exit(1);
	}

	public static String quoteOptionArguments(String configText) {
		int configLength = configText.length();
		StringBuilder outputBuffer = new StringBuilder();
		int segmentStart = 0;
		AtomicReference<String> optionRef = new AtomicReference<>();
		int optionsEnd = skipConsecutiveOptions(configText, 0, optionRef);

		while (optionsEnd > -1) {
			String optionText = configText.substring(segmentStart, optionsEnd).trim();
			outputBuffer.append(configText.substring(segmentStart, optionsEnd));
			int argStart = StringUtils.skipWhitespace(configText, optionsEnd);
			if (argStart == -1) {
				segmentStart = configLength;
				break;
			}

			outputBuffer.append(configText.substring(optionsEnd, argStart));
			int argIndex = argStart;
			char currentChar = configText.charAt(argStart);
			char nextChar = 0;
			if (configText.length() > argStart + 1) {
				nextChar = configText.charAt(argStart + 1);
			}

			if (currentChar == '\'') {
				currentChar = '"';
			}

			boolean inQuotes = false;
			boolean atArgStart = true;

			while (argIndex < configLength && (!Character.isWhitespace(currentChar) || currentChar == ' ' && inQuotes)) {
				if (currentChar == '"' && !isArgumentDelimiter(nextChar)) {
					inQuotes = !inQuotes;
				}

				if (atArgStart) {
					char lastOutputChar = outputBuffer.charAt(outputBuffer.length() - 1);
					if (currentChar != '"' && lastOutputChar != '"' && lastOutputChar != ')') {
						outputBuffer.append("\"");
						inQuotes = true;
					}

					atArgStart = false;
				}

				if (currentChar != SystemConstants.pathSep && currentChar != ',' && currentChar != '(' && currentChar != ')') {
					if (currentChar == '"' && !inQuotes) {
						atArgStart = true;
					}

					if (currentChar != '"' || !isArgumentDelimiter(nextChar) || optionText.equals("-flattenpackagehierarchy") && outputBuffer.charAt(outputBuffer.length() - 1) == '"') {
						outputBuffer.append(currentChar);
					}
				} else {
					if (outputBuffer.charAt(outputBuffer.length() - 1) != '"' && inQuotes) {
						outputBuffer.append("\"");
					}

					inQuotes = false;
					outputBuffer.append(currentChar == SystemConstants.pathSep ? "~" : currentChar);
					atArgStart = true;
				}

				if (++argIndex < configLength) {
					currentChar = configText.charAt(argIndex);
					if (currentChar == '\'') {
						currentChar = '"';
					}
				}

				if (configText.length() > argIndex + 1) {
					nextChar = configText.charAt(argIndex + 1);
				}
			}

			if (outputBuffer.charAt(outputBuffer.length() - 1) != '"' && outputBuffer.charAt(outputBuffer.length() - 1) != ')') {
				outputBuffer.append("\"");
			}

			segmentStart = argIndex;
			optionsEnd = skipConsecutiveOptions(configText, argIndex, optionRef);
		}

		outputBuffer.append(configText.substring(segmentStart, configLength));
		return outputBuffer.toString();
	}
}

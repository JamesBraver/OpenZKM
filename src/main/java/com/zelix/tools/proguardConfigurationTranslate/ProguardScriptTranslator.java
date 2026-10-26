package com.zelix.tools.proguardConfigurationTranslate;

import com.zelix.framework.AttributesNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ZKMCommands;
import com.zelix.klassmaster.script.log.AbstractScriptLogger;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.WildcardMaskPatternUtils;
import com.zelix.utils.collections.OrderedSet;
import com.zelix.utils.references.AtomicReference;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class ProguardScriptTranslator extends AbstractScriptLogger {
	private static final int LINE_NUMBER_TABLE_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("LineNumberTable");
	private static final int SOURCE_FILE_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("SourceFile");
	private static final int LOCAL_VARIABLE_TABLE_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("LocalVariableTable");
	private static final int ANNOTATION_FLAGS = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("RuntimeInvisibleAnnotations")
		| AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("RuntimeInvisibleParameterAnnotations")
		| AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("RuntimeInvisibleTypeAnnotations")
		| AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("RuntimeInvisibleTypeAnnotations")
		| AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("RuntimeVisibleAnnotations")
		| AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("RuntimeVisibleParameterAnnotations")
		| AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("RuntimeVisibleTypeAnnotations")
		| AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("AnnotationDefault");
	private static final int STACK_MAP_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("StackMap");
	private static final int STACK_MAP_TABLE_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("StackMapTable");
	private static final int SIGNATURE_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("Signature");
	private static final int BRIDGE_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("Bridge");
	private static final int LOCAL_VAR_TYPE_TABLE_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("LocalVariableTypeTable");
	private static final int GENERICS_INFO_FLAGS = SIGNATURE_FLAG | BRIDGE_FLAG | LOCAL_VAR_TYPE_TABLE_FLAG;
	private static final int DEPRECATED_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("Deprecated");
	private static final int EXCEPTIONS_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("Exceptions");
	private static final int INNER_CLASSES_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("InnerClasses");
	private static final int ENCLOSING_METHOD_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("EnclosingMethod");
	private static final int METHOD_PARAMETERS_FLAG = AttributesNode.ATTRIBUTE_FLAG_BY_NAME.get("MethodParameters");
	private static final int INNER_CLASS_INFO_FLAGS = INNER_CLASSES_FLAG | ENCLOSING_METHOD_FLAG;
	private OrderedSet<String> classpathEntries = new OrderedSet<>();
	private OrderedSet<String> ignoreMissingRefPatterns = new OrderedSet<>();
	private LinkedHashMap<String, String> openEntries = new LinkedHashMap<>();
	private OrderedSet<String> methodCallsIncludePatterns = new OrderedSet<>();
	private OrderedSet<String> methodCallsExcludePatterns = new OrderedSet<>();
	private OrderedSet<String> trimExcludePatterns = new OrderedSet<>();
	private OrderedSet<String> trimUnexcludePatterns = new OrderedSet<>();
	private OrderedSet<String> trimOptions = new OrderedSet<>();
	private OrderedSet<String> excludePatterns = new OrderedSet<>();
	private OrderedSet<String> unexcludePatterns = new OrderedSet<>();
	private OrderedSet<String> obfuscateOptions = new OrderedSet<>();
	private OrderedSet<String> outputArchives = new OrderedSet<>();
	private Set<String> attributePatterns = GlobalUtils.newHashSet();
	private int retainedAttributeFlags = 0;
	private String obfuscationDictionary;
	private String obfuscateReferencesInclude;
	private String stringEncryptionExclude;
	private boolean dontObfuscate;
	private String sourceFileName;
	private String methodParameterChangesInclude;
	private String removeMethodCallsExclude;
	private String trim;
	private boolean aggressiveMethodRenaming;
	private boolean dontUseMixedCaseClassNames;
	private String trimExclude;
	private String accessedByReflectionExclude;
	private String accessedByReflection;
	private String ignoreMissingReferences;
	private String stringEncryptionUnexclude;
	private String applyMappingFile;
	private boolean dontPreVerify;
	private String classpath;
	private String printUsageFile;
	private boolean keepParameterNames;
	private String obfuscateReferencesExclude;
	private String trimUnexclude;
	private String exclude;
	private boolean dontShrink;
	private String obfuscate;
	private String packageNameDictionary;
	private String obfuscateFlowUnexclude;
	private String collapsePackagesWithDefault;
	private String methodParameterChangesExclude;
	private boolean dontOptimize;
	private String obfuscateFlowExclude;
	private String printSeedsFile;
	private String fixedClasses;
	private String removeMethodCallsInclude;
	private String printMappingFile;
	private String baseDirectory;
	private String unexclude;
	private String classNameDictionary;

	public ProguardScriptTranslator(PrintWriter writer, boolean verbose) {
		super(verbose);
		super.writer = writer;
	}

	private static void appendAndJoinedValues(String statementName, int width, OrderedSet<String> values, StringBuilder builder, boolean emitWhenEmpty) {
		width = Math.max(width, statementName.length() + 2);
		if (values.size() > 0 || emitWhenEmpty) {
			builder.append(StringUtils.justificateString(statementName, 76, width - 1, 32));

			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				if (i == 0) {
					builder.append(value);
				} else {
					builder.append(StringUtils.justificateString(" ", 82, width - 1, 32));
					builder.append(value);
				}

				if (i < values.size() - 1) {
					builder.append(' ');
					builder.append("and");
					builder.append(Options.lineSeparator);
				}
			}

			builder.append(Options.lineSeparator);
			builder.append(StringUtils.justificateString(";", 82, width, 32));
			builder.append(Options.lineSeparator);
			builder.append(Options.lineSeparator);
		}
	}

	private static void appendQuotedValueLines(String statementName, String optionText, int width, OrderedSet<String> values, StringBuilder builder) {
		width = Math.max(width, statementName.length() + 2);
		if (values.size() > 0) {
			builder.append(StringUtils.justificateString(statementName, 76, width - 1, 32));
			if (optionText != null) {
				builder.append(optionText);
				builder.append(Options.lineSeparator);
			}

			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				if (i == 0 && optionText == null) {
					builder.append("\"");
				} else {
					builder.append(StringUtils.justificateString("\"", 82, width, 32));
				}

				builder.append(value);
				builder.append("\"");
				builder.append(Options.lineSeparator);
			}

			builder.append(StringUtils.justificateString(";", 82, width, 32));
			builder.append(Options.lineSeparator);
			builder.append(Options.lineSeparator);
		}
	}

	private static void appendQuotedMapValues(String statementName, int width, Map<String, String> entries, StringBuilder builder) {
		width = Math.max(width, statementName.length() + 2);
		if (entries.size() > 0) {
			builder.append(StringUtils.justificateString(statementName, 76, width - 1, 32));
			int entryIndex = 0;

			for (Entry<String, String> entry : entries.entrySet()) {
				String key = entry.getKey();
				if (entryIndex == 0) {
					builder.append("\"");
				} else {
					builder.append(StringUtils.justificateString("\"", 82, width, 32));
				}

				builder.append(key);
				builder.append("\"");
				String value = entry.getValue();
				if (value != null && value.length() > 0) {
					builder.append(" ");
					builder.append(value);
				}

				builder.append(Options.lineSeparator);
				entryIndex++;
			}

			builder.append(StringUtils.justificateString(";", 82, width, 32));
			builder.append(Options.lineSeparator);
			builder.append(Options.lineSeparator);
		}
	}

	public static String resolveBaseDirectory(String[] args) {
		for (int basedirectoryArgIndex = 0; basedirectoryArgIndex < args.length; basedirectoryArgIndex++) {
			if (args[basedirectoryArgIndex].equals("-basedirectory") && basedirectoryArgIndex < args.length - 1 && args[basedirectoryArgIndex + 1].length() > 0 && args[basedirectoryArgIndex + 1].charAt(0) != '-') {
				return args[basedirectoryArgIndex + 1];
			}
		}

		for (int printOptionIndex = 0; printOptionIndex < args.length; printOptionIndex++) {
			String printOption = args[printOptionIndex];
			if ((printOption.equals("-printseeds") || printOption.equals("-printmapping") || printOption.equals("-printusage"))
				&& printOptionIndex < args.length - 1
				&& args[printOptionIndex + 1].length() > 0
				&& args[printOptionIndex + 1].charAt(0) != '-') {
				String printFilePath = args[printOptionIndex + 1];
				String printFileParent = new File(printFilePath).getParent();
				if (printFileParent != null) {
					return printFileParent;
				}
			}
		}

		return FileUtils.USER_DIRECTORY_PATH;
	}

	public static String resolveLogDirectory(String fallbackDirectory) {
		if (Options.ZKM_DEFAULT_DIR != null) {
			File defaultDirectoryFile = new File(Options.ZKM_DEFAULT_DIR);
			if (defaultDirectoryFile.exists() && defaultDirectoryFile.isDirectory()) {
				return defaultDirectoryFile.getAbsolutePath();
			}
		}

		if (fallbackDirectory != null) {
			File fallbackDirectoryFile = new File(fallbackDirectory);
			if (fallbackDirectoryFile.exists() && fallbackDirectoryFile.isDirectory()) {
				return fallbackDirectoryFile.getAbsolutePath();
			}
		}

		return new File(FileUtils.USER_DIRECTORY_PATH).getAbsolutePath();
	}

	private static void appendQuotedValues(String statementName, int width, OrderedSet<String> values, StringBuilder builder) {
		appendQuotedValueLines(statementName, null, width, values, builder);
	}

	private static void appendAndJoinedValuesIfAny(String statementName, int width, OrderedSet<String> values, StringBuilder builder) {
		appendAndJoinedValues(statementName, width, values, builder, false);
	}

	private static void appendValueLines(String statementName, int width, OrderedSet<String> values, StringBuilder builder) {
		width = Math.max(width, statementName.length() + 2);
		builder.append(StringUtils.justificateString(statementName, 76, width - 1, 32));

		for (int i = 0; i < values.size(); i++) {
			String value = values.get(i);
			if (i == 0) {
				builder.append(value);
			} else {
				builder.append(StringUtils.justificateString(" ", 82, width - 1, 32));
				builder.append(value);
			}

			if (i < values.size() - 1) {
				builder.append(Options.lineSeparator);
			}
		}

		if (values.size() > 0) {
			builder.append(Options.lineSeparator);
			builder.append(StringUtils.justificateString(";", 82, width, 32));
		} else {
			builder.append(";");
		}

		builder.append(Options.lineSeparator);
		builder.append(Options.lineSeparator);
	}

	public static boolean createDefaultDirAndLogFile(String[] args, AtomicReference<File> logFileRef, AtomicReference<String> errorRef) {
		String defaultDirectoryPath = Options.ZKM_DEFAULT_DIR != null ? Options.ZKM_DEFAULT_DIR : resolveBaseDirectory(args);
		File defaultDirectory = new File(FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(defaultDirectoryPath), null));
		if (!defaultDirectory.exists()) {
			if (!defaultDirectory.mkdirs()) {
				errorRef.set("ERROR: Could not create default directory '" + defaultDirectory.getAbsolutePath() + "'.");
				return false;
			}
		} else if (!defaultDirectory.isDirectory()) {
			errorRef.set("ERROR: Specified default directory '" + defaultDirectory.getAbsolutePath() + "' is not a directory.");
			return false;
		}

		File logFile = new File(defaultDirectory, "ZKM_PG_log.txt");
		PrintWriter logWriter = null;

		label69: {
			try {
				logWriter = new PrintWriter(new FileWriter(logFile));
				break label69;
			} catch (IOException ioException) {
				errorRef.set("ERROR: Couldn't create preliminary log file '" + defaultDirectory.getAbsolutePath() + "' : '" + ioException.getMessage() + "'");
			} finally {
				if (logWriter != null) {
					logWriter.close();
				}
			}

			return false;
		}

		logFileRef.set(logFile);
		return true;
	}

	public void addMethodCallsIncludePattern(String pattern) {
		this.methodCallsIncludePatterns.add(pattern);
	}

	private void appendUntranslatedStatement(String statementName, String statementText, StringBuilder builder) {
		builder.append(Options.lineSeparator);
		builder.append("//Using '" + statementName + "' statement found in '" + this.sourceFileName + "'");
		builder.append(Options.lineSeparator);
		builder.append(statementText);
		builder.append(Options.lineSeparator);
	}

	public void addTrimExcludePattern(String pattern) {
		if (pattern.indexOf(".^") > -1) {
			pattern = pattern.replace(".^", ".");
		}

		this.trimExcludePatterns.add(pattern);
	}

	public void setFlattenedPackageName(String packageName) {
		this.collapsePackagesWithDefault = packageName;
	}

	public void setVerbose() {
		this.verbose = true;
	}

	private void parseClasspathStatement(StringBuilder builder) {
		boolean entriesAlreadyPresent = this.classpathEntries.size() > 0;
		OrderedSet<String> statementEntries = new OrderedSet<>();
		builder.append(Options.lineSeparator);
		builder.append("//" + (entriesAlreadyPresent ? (Options.prependClasspathEntries ? "PREPENDING" : "Appending") : "Using") + " 'classpath' statement found in '" + this.sourceFileName + "'");
		builder.append(Options.lineSeparator);
		int quoteIndex = this.classpath.indexOf(34, 0);

		while (quoteIndex > -1) {
			int closingQuoteIndex = this.classpath.indexOf(34, quoteIndex + 1);
			if (closingQuoteIndex <= -1) {
				this.logMessage("Error in using 'classpath' statement found in '" + this.sourceFileName + "'. Unbalanced '\"' characters.");
				break;
			}

			statementEntries.add(this.classpath.substring(quoteIndex + 1, closingQuoteIndex));
			quoteIndex = this.classpath.indexOf(34, closingQuoteIndex + 1);
		}

		if (!Options.prependClasspathEntries) {
			this.classpathEntries.addAll(statementEntries);
		} else {
			statementEntries.addAll(this.classpathEntries);
			this.classpathEntries = statementEntries;
		}
	}

	public void setPrintSeeds(String fileName) {
		fileName = fileName.trim();
		if (this.printSeedsFile != null && !this.printSeedsFile.equals(fileName)) {
			this.logWarning("More than one value for ProGuard '-printseeds'. '" + fileName + "' replacing '" + this.printSeedsFile + "'.");
		}

		if (fileName != null && fileName.length() > 0) {
			this.printSeedsFile = fileName;
		}

		this.setVerbose();
	}

	public void addClasspathEntries(Set<String> entries) {
		this.classpathEntries.addAll(entries);
	}

	public void addOpenEntry(String fileName, String filterExpression) {
		this.openEntries.put(fileName, filterExpression);
	}

	private String extractStatement(String statementName, String contents) {
		int statementIndex = contents.indexOf(statementName);

		while (statementIndex > -1 && !this.isStatementBoundary(statementIndex, statementName, contents)) {
			statementIndex = contents.indexOf(statementName, statementIndex + 1);
		}

		if (statementIndex > -1) {
			int semicolonIndex = contents.indexOf(";", statementIndex);
			if (semicolonIndex > -1) {
				return contents.substring(statementIndex, semicolonIndex + 1);
			}
		}

		return null;
	}

	public void setAggressiveMethodRenaming() {
		this.aggressiveMethodRenaming = true;
	}

	public String getSaveDirectory() {
		File baseDirectoryFile;
		if (this.baseDirectory != null) {
			baseDirectoryFile = new File(this.baseDirectory);
		} else {
			baseDirectoryFile = FileUtils.USER_DIRECTORY;
		}

		return this.outputArchives != null && this.outputArchives.size() > 0 ? this.resolveParentDirectory(baseDirectoryFile, this.outputArchives.get(0)) : baseDirectoryFile.getAbsolutePath();
	}

	public void extractZKMStatements(String contents, String fileName) {
		this.sourceFileName = fileName;
		StringBuilder commandNamesBuffer = new StringBuilder();

		for (int commandIndex = 0; commandIndex < ZKMCommands.commandNames.length; commandIndex++) {
			commandNamesBuffer.append("'");
			commandNamesBuffer.append(ZKMCommands.commandNames[commandIndex]);
			commandNamesBuffer.append("'");
			if (commandIndex < ZKMCommands.commandNames.length - 2) {
				commandNamesBuffer.append(", ");
			} else if (commandIndex < ZKMCommands.commandNames.length - 1) {
				commandNamesBuffer.append(" or ");
			}
		}

		this.logMessage("Will examine contents of file '" + fileName + "' for possible " + commandNamesBuffer.toString() + " statements.");

		for (String commandName : ZKMCommands.commandNames) {
			String statementText = this.extractStatement(commandName, contents);
			if (statementText != null) {
				this.logMessage("Using '" + commandName + "' statement found in '" + fileName + "'");
				if (commandName.equals("classpath")) {
					this.classpath = statementText;
				} else if (commandName.equals("ignoreMissingReferences")) {
					this.ignoreMissingReferences = statementText;
				} else if (commandName.equals("removeMethodCallsInclude")) {
					this.removeMethodCallsInclude = statementText;
				} else if (commandName.equals("removeMethodCallsExclude")) {
					this.removeMethodCallsExclude = statementText;
				} else if (commandName.equals("trimExclude")) {
					this.trimExclude = statementText;
				} else if (commandName.equals("trimUnexclude")) {
					this.trimUnexclude = statementText;
				} else if (commandName.equals("trim")) {
					this.trim = statementText;
				} else if (commandName.equals("obfuscateFlowExclude")) {
					this.obfuscateFlowExclude = statementText;
				} else if (commandName.equals("obfuscateFlowUnexclude")) {
					this.obfuscateFlowUnexclude = statementText;
				} else if (commandName.equals("stringEncryptionExclude")) {
					this.stringEncryptionExclude = statementText;
				} else if (commandName.equals("stringEncryptionUnexclude")) {
					this.stringEncryptionUnexclude = statementText;
				} else if (commandName.equals("exclude")) {
					this.exclude = statementText;
				} else if (commandName.equals("unexclude")) {
					this.unexclude = statementText;
				} else if (commandName.equals("accessedByReflection")) {
					this.accessedByReflection = statementText;
				} else if (commandName.equals("accessedByReflectionExclude")) {
					this.accessedByReflectionExclude = statementText;
				} else if (commandName.equals("obfuscateReferencesInclude")) {
					this.obfuscateReferencesInclude = statementText;
				} else if (commandName.equals("obfuscateReferencesExclude")) {
					this.obfuscateReferencesExclude = statementText;
				} else if (commandName.equals("fixedClasses")) {
					this.fixedClasses = statementText;
				} else if (commandName.equals("methodParameterChangesInclude")) {
					this.methodParameterChangesInclude = statementText;
				} else if (commandName.equals("methodParameterChangesExclude")) {
					this.methodParameterChangesExclude = statementText;
				} else if (commandName.equals("obfuscate")) {
					this.obfuscate = statementText;
				}
			}
		}
	}

	public void disableMixedCaseClassNames() {
		this.dontUseMixedCaseClassNames = true;
	}

	private boolean isStatementBoundary(int index, String statementName, String contents) {
		int statementEndIndex = index + statementName.length();
		int contentsLength = contents.length();
		return contentsLength > statementEndIndex && !Character.isWhitespace(contents.charAt(statementEndIndex)) && contents.charAt(statementEndIndex) != ';'
			? false
			: index <= 0 || Character.isWhitespace(contents.charAt(index - 1)) || contents.charAt(index - 1) == ';';
	}

	public void addExcludePattern(String pattern) {
		this.excludePatterns.add(pattern);
	}

	public String getLogDirectory() {
		File baseDirectoryFile;
		if (this.baseDirectory != null) {
			baseDirectoryFile = new File(this.baseDirectory);
		} else {
			baseDirectoryFile = FileUtils.USER_DIRECTORY;
		}

		if (this.printSeedsFile != null) {
			return this.resolveParentDirectory(baseDirectoryFile, this.printSeedsFile);
		} else if (this.printMappingFile != null && this.printMappingFile.trim().length() > 0) {
			return this.resolveParentDirectory(baseDirectoryFile, this.printMappingFile);
		} else {
			return this.printUsageFile != null ? this.resolveParentDirectory(baseDirectoryFile, this.printUsageFile) : baseDirectoryFile.getAbsolutePath();
		}
	}

	public void setPrintUsage(String fileName) {
		fileName = fileName.trim();
		if (this.printUsageFile != null && !this.printUsageFile.equals(fileName)) {
			this.logWarning("More than one value for ProGuard '-printusage'. '" + fileName + "' replacing '" + this.printUsageFile + "'.");
		}

		if (fileName != null && fileName.length() > 0) {
			this.printUsageFile = fileName;
		}
	}

	public String getScriptDirectory(Set<String> searchedDirectories) {
		if (this.baseDirectory != null) {
			searchedDirectories.add(this.getBaseDirectory());
			return this.getBaseDirectory();
		} else if (this.printSeedsFile != null) {
			String printSeedsParent = new File(this.printSeedsFile).getParent();
			if (printSeedsParent != null && printSeedsParent.length() > 0) {
				searchedDirectories.add(printSeedsParent);
			}

			return printSeedsParent;
		} else if (this.printMappingFile != null && this.printMappingFile.trim().length() > 0) {
			String printMappingParent = new File(this.printMappingFile).getParent();
			if (printMappingParent != null && printMappingParent.length() > 0) {
				searchedDirectories.add(printMappingParent);
			}

			return printMappingParent;
		} else if (this.printUsageFile != null) {
			String printUsageParent = new File(this.printUsageFile).getParent();
			if (printUsageParent != null && printUsageParent.length() > 0) {
				searchedDirectories.add(printUsageParent);
			}

			return printUsageParent;
		} else {
			return null;
		}
	}

	public void setDontObfuscate() {
		this.dontObfuscate = true;
	}

	public void setObfuscationDictionary(String fileName) {
		fileName = fileName.trim();
		if (this.obfuscationDictionary != null && !this.obfuscationDictionary.equals(fileName)) {
			this.logWarning("More than one value for ProGuard '-classobfuscationdictionary'. '" + fileName + "' replacing '" + this.obfuscationDictionary + "'.");
		}

		if (fileName != null && fileName.length() > 0) {
			this.obfuscationDictionary = fileName;
		}
	}

	public String getBaseDirectory() {
		return this.baseDirectory;
	}

	public void setRepackagedPackageName(String packageName) {
		this.collapsePackagesWithDefault = packageName;
	}

	public void setPrintMapping(String fileName) {
		fileName = fileName.trim();
		if (this.printMappingFile != null && !this.printMappingFile.equals(fileName)) {
			this.logWarning("More than one value for ProGuard '-printmapping'. '" + fileName + "' replacing '" + this.printMappingFile + "'.");
		}

		this.printMappingFile = fileName;
	}

	private int getAlignmentWidth() {
		if (this.methodCallsIncludePatterns != null && !this.methodCallsIncludePatterns.isEmpty()) {
			return 26;
		} else if (this.methodCallsExcludePatterns != null && !this.methodCallsExcludePatterns.isEmpty()) {
			return 26;
		} else {
			return this.ignoreMissingRefPatterns != null && !this.ignoreMissingRefPatterns.isEmpty() ? 25 : 15;
		}
	}

	public void addAttributePattern(String pattern) {
		this.attributePatterns.add(pattern);
	}

	public void setKeepParameterNames() {
		this.keepParameterNames = true;
	}

	public void setDontPreVerify() {
		this.dontPreVerify = true;
	}

	public void addUnexcludePattern(String pattern) {
		this.unexcludePatterns.add(pattern);
	}

	public void setDontOptimize() {
		this.dontOptimize = true;
	}

	public String generateScript(String generatorName, boolean includeCommandLine, boolean includeMessages, List<String> messages) {
		this.buildAttributeOptions();
		StringBuilder scriptBuilder = new StringBuilder(1000);
		scriptBuilder.append(AbstractScriptLogger.generateHeaderComment(generatorName));
		scriptBuilder.append(Options.lineSeparator);
		StringBuilder classpathCommentBuilder = new StringBuilder();
		if (this.classpath != null) {
			this.parseClasspathStatement(classpathCommentBuilder);
		}

		List<String> messageList = this.getMessagesAsList();
		if (messageList.size() > 0) {
			StringBuilder summaryBuilder = new StringBuilder();
			if (this.getMessageCount() > 0) {
				summaryBuilder.append(this.getMessageCount() + " message" + (this.getMessageCount() > 1 ? "s" : ""));
			}

			if (this.getWarningCount() > 0) {
				if (summaryBuilder.length() > 0) {
					summaryBuilder.append(" and ");
				}

				summaryBuilder.append(this.getWarningCount() + " warning" + (this.getWarningCount() > 1 ? "s" : ""));
			}

			if (this.getErrorCount() > 0) {
				if (summaryBuilder.length() > 0) {
					summaryBuilder.append(" and ");
				}

				summaryBuilder.append(this.getErrorCount() + " error" + (this.getErrorCount() > 1 ? "s" : ""));
			}

			messages.add(summaryBuilder.toString() + " reported during ProGuard command translation.");
			messages.add("");
			messages.addAll(messageList);
			if (includeMessages) {
				String[] messageArray = new String[messages.size()];
				messages.toArray(messageArray);
				scriptBuilder.append(GlobalUtils.generateComment(false, messageArray));
				scriptBuilder.append(Options.lineSeparator);
			}
		}

		if (includeCommandLine && (this.verbose || this.baseDirectory != null || this.printUsageFile != null || this.printSeedsFile != null)) {
			StringBuilder explanationBuilder = new StringBuilder(100);
			StringBuilder commandLineBuilder = new StringBuilder(100);
			commandLineBuilder.append("java -jar ZKM.jar ");
			explanationBuilder.append("Some ProGuard options must appear as " + CopyrightConstants.NAME + " command line options as shown below.");
			if (this.verbose) {
				commandLineBuilder.append("-v");
				commandLineBuilder.append(' ');
			}

			if (this.baseDirectory != null) {
				commandLineBuilder.append("-dd " + WildcardMaskPatternUtils.quoteIfNeeded(this.baseDirectory) + " ");
			}

			if (this.printUsageFile != null) {
				commandLineBuilder.append("-tl " + (this.printUsageFile.length() > 0 ? WildcardMaskPatternUtils.quoteIfNeeded(this.printUsageFile) : "ZKM_TrimLog.txt") + " ");
			}

			if (this.printSeedsFile != null) {
				commandLineBuilder.append("-l " + (this.printSeedsFile.length() > 0 ? WildcardMaskPatternUtils.quoteIfNeeded(this.printSeedsFile) : "ZKM_log.txt") + " ");
			}

			commandLineBuilder.append("MyZKMScript.txt");
			String commandLine = commandLineBuilder.toString();
			String explanation = explanationBuilder.toString();
			scriptBuilder.append(GlobalUtils.generateComment(new String[]{explanation, commandLine}));
			scriptBuilder.append(Options.lineSeparator);
		}

		int alignmentWidth = this.getAlignmentWidth();
		if (classpathCommentBuilder.length() > 0) {
			scriptBuilder.append((CharSequence)classpathCommentBuilder);
		}

		appendQuotedValues("classpath", alignmentWidth, this.classpathEntries, scriptBuilder);
		if (this.ignoreMissingReferences != null) {
			this.parseAndAppendStatement("ignoreMissingReferences", this.ignoreMissingReferences, this.ignoreMissingRefPatterns, scriptBuilder);
		}

		if (!this.ignoreMissingRefPatterns.isEmpty() || this.ignoreMissingReferences != null) {
			appendAndJoinedValues("ignoreMissingReferences", alignmentWidth, this.ignoreMissingRefPatterns, scriptBuilder, true);
		}

		appendQuotedMapValues("open", alignmentWidth, this.openEntries, scriptBuilder);
		if (this.removeMethodCallsInclude != null || this.removeMethodCallsExclude != null || !this.methodCallsIncludePatterns.isEmpty() || !this.methodCallsExcludePatterns.isEmpty()) {
			if (this.removeMethodCallsInclude != null) {
				this.parseAndAppendStatement("removeMethodCallsInclude", this.removeMethodCallsInclude, this.methodCallsIncludePatterns, scriptBuilder);
			}

			if (!this.methodCallsIncludePatterns.isEmpty()) {
				appendAndJoinedValuesIfAny("removeMethodCallsInclude", alignmentWidth, this.methodCallsIncludePatterns, scriptBuilder);
			}

			if (this.removeMethodCallsExclude != null) {
				this.appendUntranslatedStatement("removeMethodCallsExclude", this.removeMethodCallsExclude, scriptBuilder);
			}

			if (!this.methodCallsExcludePatterns.isEmpty()) {
				this.parseAndAppendStatement("removeMethodCallsExclude", this.removeMethodCallsExclude, this.methodCallsExcludePatterns, scriptBuilder);
			}

			if (!this.dontOptimize) {
				appendValueLines("removeMethodCalls", alignmentWidth, new OrderedSet<>(), scriptBuilder);
			}
		}

		if (Options.dontAppendProguardStatements && this.trimExclude != null) {
			this.appendUntranslatedStatement("trimExclude", this.trimExclude, scriptBuilder);
		} else {
			if (!Options.dontAppendProguardStatements && this.trimExclude != null) {
				this.parseAndAppendStatement("trimExclude", this.trimExclude, this.trimExcludePatterns, scriptBuilder);
			}

			if (this.trimExcludePatterns.size() > 0 && this.dontShrink) {
				this.trimExcludePatterns.add("*.* + /*Exclude everything*/");
			}

			if (this.trimExcludePatterns.size() > 0) {
				appendAndJoinedValuesIfAny("trimExclude", alignmentWidth, this.trimExcludePatterns, scriptBuilder);
			}
		}

		if (Options.dontAppendProguardStatements && this.trimUnexclude != null) {
			this.appendUntranslatedStatement("trimUnexclude", this.trimUnexclude, scriptBuilder);
		} else if (!this.dontShrink) {
			if (!Options.dontAppendProguardStatements && this.trimUnexclude != null) {
				this.parseAndAppendStatement("trimUnexclude", this.trimUnexclude, this.trimUnexcludePatterns, scriptBuilder);
			}

			if (this.trimUnexcludePatterns.size() > 0) {
				appendAndJoinedValuesIfAny("trimUnexclude", alignmentWidth, this.trimUnexcludePatterns, scriptBuilder);
			}
		} else if (this.trimUnexclude != null) {
			scriptBuilder.append(Options.lineSeparator);
			scriptBuilder.append("//Ignoring 'trimUnexclude' statement found in '" + this.sourceFileName + "' because '-dontshrink' specified.");
			scriptBuilder.append(Options.lineSeparator);
		}

		if (this.trim != null) {
			this.appendUntranslatedStatement("trim", this.trim, scriptBuilder);
		} else if (this.trimExcludePatterns.size() > 0 || !this.dontShrink) {
			appendValueLines("trim", alignmentWidth, this.trimOptions, scriptBuilder);
		}

		if (this.stringEncryptionExclude != null) {
			this.appendUntranslatedStatement("stringEncryptionExclude", this.stringEncryptionExclude, scriptBuilder);
		}

		if (this.stringEncryptionUnexclude != null) {
			this.appendUntranslatedStatement("stringEncryptionUnexclude", this.stringEncryptionUnexclude, scriptBuilder);
		}

		if (this.obfuscateFlowExclude != null) {
			this.appendUntranslatedStatement("obfuscateFlowExclude", this.obfuscateFlowExclude, scriptBuilder);
		}

		if (this.obfuscateFlowUnexclude != null) {
			this.appendUntranslatedStatement("obfuscateFlowUnexclude", this.obfuscateFlowUnexclude, scriptBuilder);
		}

		if (Options.dontAppendProguardStatements && this.exclude != null) {
			this.appendUntranslatedStatement("exclude", this.exclude, scriptBuilder);
		} else {
			if (this.dontObfuscate) {
				this.excludePatterns.add("*.^* + /*Exclude everything*/");
			}

			if (!Options.dontAppendProguardStatements && this.exclude != null) {
				this.parseAndAppendStatement("exclude", this.exclude, this.excludePatterns, scriptBuilder);
			}

			if (this.excludePatterns.size() > 0) {
				appendAndJoinedValuesIfAny("exclude", alignmentWidth, this.excludePatterns, scriptBuilder);
			}
		}

		if (Options.dontAppendProguardStatements && this.unexclude != null) {
			this.appendUntranslatedStatement("unexclude", this.unexclude, scriptBuilder);
		} else if (!this.dontObfuscate) {
			if (!Options.dontAppendProguardStatements && this.unexclude != null) {
				this.parseAndAppendStatement("unexclude", this.unexclude, this.unexcludePatterns, scriptBuilder);
			}

			if (this.unexcludePatterns.size() > 0) {
				appendAndJoinedValuesIfAny("unexclude", alignmentWidth, this.unexcludePatterns, scriptBuilder);
			}
		}

		if (this.accessedByReflection != null) {
			this.appendUntranslatedStatement("accessedByReflection", this.accessedByReflection, scriptBuilder);
		}

		if (this.accessedByReflectionExclude != null) {
			this.appendUntranslatedStatement("accessedByReflectionExclude", this.accessedByReflectionExclude, scriptBuilder);
		}

		if (this.obfuscateReferencesInclude != null) {
			this.appendUntranslatedStatement("obfuscateReferencesInclude", this.obfuscateReferencesInclude, scriptBuilder);
		}

		if (this.obfuscateReferencesExclude != null) {
			this.appendUntranslatedStatement("obfuscateReferencesExclude", this.obfuscateReferencesExclude, scriptBuilder);
		}

		if (this.fixedClasses != null) {
			this.appendUntranslatedStatement("fixedClasses", this.fixedClasses, scriptBuilder);
		}

		if (this.methodParameterChangesInclude != null) {
			this.appendUntranslatedStatement("methodParameterChangesInclude", this.methodParameterChangesInclude, scriptBuilder);
		}

		if (this.methodParameterChangesExclude != null) {
			this.appendUntranslatedStatement("methodParameterChangesExclude", this.methodParameterChangesExclude, scriptBuilder);
		}

		if (this.obfuscate != null) {
			this.appendUntranslatedStatement("obfuscate", this.obfuscate, scriptBuilder);
		} else {
			if (this.applyMappingFile != null) {
				this.obfuscateOptions.add("changeLogFileIn=\"" + this.applyMappingFile + "\"");
			}

			if (this.printMappingFile != null) {
				this.obfuscateOptions.add("changeLogFileOut=\"" + this.printMappingFile + "\"");
			}

			this.obfuscateOptions.add("obfuscateFlow=none //can change from none to light, normal, aggressive or extraAggressive");
			this.obfuscateOptions.add("obfuscateParameters=none //can change from none normal");
			this.obfuscateOptions.add("encryptStringLiterals=none //can change from none to normal, aggressive, flowObfuscate or enhanced");
			this.obfuscateOptions.add("exceptionObfuscation=none //can change from none to light or heavy");
			this.obfuscateOptions.add("autoReflectionHandling=none //can change from none to normal");
			this.obfuscateOptions.add("obfuscateReferences=none //can change from none to normal");
			this.obfuscateOptions.add("randomize=false //can change from false to true");
			if (this.collapsePackagesWithDefault != null) {
				this.obfuscateOptions.add("collapsePackagesWithDefault=\"" + this.collapsePackagesWithDefault + "\"");
			}

			if (this.aggressiveMethodRenaming) {
				this.obfuscateOptions.add("aggressiveMethodRenaming=true");
			}

			if (this.dontPreVerify) {
				this.obfuscateOptions.add("preverify=false");
			}

			if (this.dontUseMixedCaseClassNames) {
				this.obfuscateOptions.add("mixedCaseClassNames=false");
			} else {
				this.obfuscateOptions.add("mixedCaseClassNames=ifInArchive");
			}

			this.obfuscateOptions.add("keepBalancedLocks=true");
			if (this.packageNameDictionary != null && this.packageNameDictionary.length() > 0) {
				this.obfuscateOptions.add("newPackageNameFile=\"" + this.packageNameDictionary + '"');
			}

			if (this.classNameDictionary != null && this.classNameDictionary.length() > 0) {
				this.obfuscateOptions.add("newClassNameFile=\"" + this.classNameDictionary + '"');
			}

			if (this.obfuscationDictionary != null && this.obfuscationDictionary.length() > 0) {
				this.obfuscateOptions.add("newFieldNameFile=\"" + this.obfuscationDictionary + '"');
				this.obfuscateOptions.add("newMethodNameFile=\"" + this.obfuscationDictionary + '"');
			}

			appendValueLines("obfuscate", alignmentWidth, this.obfuscateOptions, scriptBuilder);
		}

		appendQuotedValueLines("saveAll", "archiveCompression=all", alignmentWidth, this.outputArchives, scriptBuilder);
		return scriptBuilder.toString();
	}

	public void addOutputArchives(Set<String> archives) {
		this.outputArchives.addAll(archives);
	}

	public String getPrintUsageFile() {
		return this.printUsageFile;
	}

	private void parseAndAppendStatement(String statementName, String statementText, OrderedSet<String> values, StringBuilder builder) {
		builder.append(Options.lineSeparator);
		builder.append("//Appending '" + statementName + "' statement found in '" + this.sourceFileName + "'");
		builder.append(Options.lineSeparator);
		StringTokenizer tokenizer = new StringTokenizer(statementText);
		if (tokenizer.hasMoreTokens() && tokenizer.nextToken().equals(statementName)) {
			StringBuilder valueBuffer = new StringBuilder();

			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.equals("and")) {
					if (valueBuffer.length() > 0) {
						values.add(valueBuffer.toString());
						valueBuffer.setLength(0);
					}
				} else {
					if (token.endsWith(";")) {
						if (token.length() > 1) {
							if (valueBuffer.length() > 0) {
								valueBuffer.append(' ');
							}

							valueBuffer.append(token.substring(0, token.length() - 1));
						}

						values.add(valueBuffer.toString());
						break;
					}

					if (valueBuffer.length() > 0) {
						valueBuffer.append(' ');
					}

					valueBuffer.append(token);
				}
			}
		}
	}

	private String resolveParentDirectory(File baseDirectory, String filePath) {
		return FileUtils.isRelativePath(filePath) ? new File(baseDirectory, filePath).getParentFile().getAbsolutePath() : new File(filePath).getParentFile().getAbsolutePath();
	}

	public void addIgnoreMissingRefPattern(String pattern) {
		this.ignoreMissingRefPatterns.add(pattern);
	}

	public void setPackageNameDictionary(String fileName) {
		fileName = fileName.trim();
		if (this.packageNameDictionary != null && !this.packageNameDictionary.equals(fileName)) {
			this.logWarning("More than one value for ProGuard '-packageobfuscationdictionary'. '" + fileName + "' replacing '" + this.packageNameDictionary + "'.");
		}

		if (fileName != null && fileName.length() > 0) {
			this.packageNameDictionary = fileName;
		}
	}

	public void setClassNameDictionary(String fileName) {
		fileName = fileName.trim();
		if (this.classNameDictionary != null && !this.classNameDictionary.equals(fileName)) {
			this.logWarning("More than one value for ProGuard '-classobfuscationdictionary'. '" + fileName + "' replacing '" + this.classNameDictionary + "'.");
		}

		if (fileName != null && fileName.length() > 0) {
			this.classNameDictionary = fileName;
		}
	}

	public void addMethodCallsExcludePattern(String pattern) {
		this.methodCallsExcludePatterns.add(pattern);
	}

	public String getPrintSeedsFile() {
		return this.printSeedsFile;
	}

	private void buildAttributeOptions() {
		for (String attributePattern : this.attributePatterns) {
			for (Entry<String, Integer> attributeEntry : AttributesNode.ATTRIBUTE_FLAG_BY_NAME.entrySet()) {
				if (attributeEntry.getKey().matches(attributePattern)) {
					this.retainedAttributeFlags = this.retainedAttributeFlags | attributeEntry.getValue();
				}
			}
		}

		if ((this.retainedAttributeFlags & DEPRECATED_FLAG) == DEPRECATED_FLAG) {
			this.trimOptions.add("deleteDeprecatedAttributes=false");
		} else {
			this.trimOptions.add("deleteDeprecatedAttributes=true");
		}

		if ((this.retainedAttributeFlags & SOURCE_FILE_FLAG) == 0) {
			this.trimOptions.add("deleteSourceFileAttributes=true");
		} else {
			this.trimOptions.add("deleteSourceFileAttributes=false //defaults to false");
		}

		if ((this.retainedAttributeFlags & ANNOTATION_FLAGS) != ANNOTATION_FLAGS) {
			this.trimOptions.add("deleteAnnotationAttributes=true");
		} else {
			this.trimOptions.add("deleteAnnotationAttributes=false");
		}

		if ((this.retainedAttributeFlags & EXCEPTIONS_FLAG) == 0) {
			this.trimOptions.add("deleteExceptionAttributes=true");
		} else {
			this.trimOptions.add("deleteExceptionAttributes=false //defaults to false");
		}

		this.trimOptions.add("//deleteUnknownAttributes=true");
		if ((this.retainedAttributeFlags & LINE_NUMBER_TABLE_FLAG) == LINE_NUMBER_TABLE_FLAG) {
			this.obfuscateOptions.add("lineNumbers=keep //can specify delete, scramble or keep");
		}

		if ((this.retainedAttributeFlags & SIGNATURE_FLAG) == SIGNATURE_FLAG) {
			if ((this.retainedAttributeFlags & GENERICS_INFO_FLAGS) != GENERICS_INFO_FLAGS) {
				this.logMessage(
					"'keepGenericsInfo' set to default to 'true' however both 'LocalVariableTypeTable' and 'Bridge' attributes were not specified to be retained."
				);
			}
		} else {
			this.obfuscateOptions.add("keepGenericsInfo=false");
		}

		if ((this.retainedAttributeFlags & INNER_CLASS_INFO_FLAGS) == INNER_CLASS_INFO_FLAGS) {
			this.obfuscateOptions.add("keepInnerClassInfo=true");
		} else {
			if ((this.retainedAttributeFlags & INNER_CLASSES_FLAG) == INNER_CLASSES_FLAG && (this.retainedAttributeFlags & ENCLOSING_METHOD_FLAG) != ENCLOSING_METHOD_FLAG) {
				this.logWarning("'keepInnerClassInfo' set to 'false' because 'EnclosingMethod' attribute not retained.");
			}

			this.obfuscateOptions.add("keepInnerClassInfo=false");
		}

		if ((this.retainedAttributeFlags & LOCAL_VARIABLE_TABLE_FLAG) == LOCAL_VARIABLE_TABLE_FLAG) {
			if (this.keepParameterNames) {
				this.obfuscateOptions
					.add(
						"localVariables=keepMethodParametersIfNotObfuscated //can specify delete, keepVisibleMethodParameters, keepVisibleMethodParametersIfNotObfuscated, keepMethodParametersIfNotObfuscated, obfuscate or keep"
					);
			} else {
				this.obfuscateOptions
					.add(
						"localVariables=keep //can specify delete, keepVisibleMethodParameters, keepVisibleMethodParametersIfNotObfuscated, keepMethodParametersIfNotObfuscated, obfuscate or keep"
					);
			}
		} else if (this.keepParameterNames) {
			this.obfuscateOptions.add("localVariables=keepMethodParametersIfNotObfuscated");
		}

		if ((this.retainedAttributeFlags & METHOD_PARAMETERS_FLAG) == 0) {
			if (this.keepParameterNames) {
				this.obfuscateOptions.add("methodParameters=keepIfNotObfuscated //can specify delete, keepVisible, keepVisibleIfNotObfuscated, keepIfNotObfuscated, obfuscate or keep");
			} else {
				this.obfuscateOptions
					.add("methodParameters=keepVisibleIfNotObfuscated //can specify delete, keepVisible, keepVisibleIfNotObfuscated, keepIfNotObfuscated, obfuscate or keep");
			}
		} else {
			this.obfuscateOptions.add("methodParameters=keep");
		}
	}

	public void setDontShrink() {
		this.dontShrink = true;
	}

	public void setApplyMapping(String fileName) {
		fileName = fileName.trim();
		if (this.applyMappingFile != null && !this.applyMappingFile.equals(fileName)) {
			this.logWarning("More than one value for ProGuard '-applymapping'. '" + fileName + "' replacing '" + this.applyMappingFile + "'.");
		}

		this.applyMappingFile = fileName;
	}

	public void addTrimUnexcludePattern(String pattern) {
		if (pattern.indexOf(".^") > -1) {
			pattern = pattern.replace(".^", ".");
		}

		this.trimUnexcludePatterns.add(pattern);
	}

	public void setBaseDirectory(String directory) {
		this.baseDirectory = directory;
	}
}

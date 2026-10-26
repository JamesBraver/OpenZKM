package com.zelix.klassmaster;

import com.zelix.exceptions.*;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ScriptInputNode;
import com.zelix.klassmaster.script.ScriptParser;
import com.zelix.klassmaster.script.ScriptPreprocessor;
import com.zelix.logging.FileConsoleLogger;
import com.zelix.logging.SimpleLogger;
import com.zelix.repository.ClasspathManager;
import com.zelix.tools.proguardConfigurationTranslate.ProguardCommandsConstants;
import com.zelix.tools.proguardConfigurationTranslate.ProguardConfigUtils;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.ui.GUI;
import com.zelix.ui.dialogs.StacktraceTranslatorDialog;
import com.zelix.ui.prompts.EvaluationAgreementPrompt;
import com.zelix.ui.splash.SplashScreen;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.*;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.function.EmptyConsumer;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.Workspace;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class KlassMaster extends CopyrightConstants {
	public static String SAVE_PERMISSION_MARKER = "a%78 _$";
	private static String DEFAULT_MIDLET_SCRIPT = "trimExclude public *.* extends javax.microedition.midlet.MIDlet;"
		+ Options.lineSeparator
		+ Options.lineSeparator
		+ "trim        deleteSourceFileAttributes=true"
		+ Options.lineSeparator
		+ "            deleteAnnotationAttributes=true"
		+ Options.lineSeparator
		+ "            deleteDeprecatedAttributes=true"
		+ Options.lineSeparator
		+ "            deleteUnknownAttributes=false;"
		+ Options.lineSeparator
		+ Options.lineSeparator
		+ "exclude     public *.^* extends javax.microedition.midlet.MIDlet;"
		+ Options.lineSeparator
		+ Options.lineSeparator
		+ "obfuscate   changeLogFileOut=\"ChangeLog.txt\""
		+ Options.lineSeparator
		+ "            obfuscateFlow=aggressive"
		+ Options.lineSeparator
		+ "            encryptStringLiterals=none"
		+ Options.lineSeparator
		+ "            collapsePackagesWithDefault=\"\""
		+ Options.lineSeparator
		+ "            exceptionObfuscation=light"
		+ Options.lineSeparator
		+ "            lineNumbers=delete;";
	private static long currentThreadId = -1L;
	public static final String SUFFIX = CopyrightConstants.SUFFIX;
	private static PrintStream originalSystemOut;
	private static Font defaultFont;
	private String firstLog;
	private String[] licenseInfos;
	public static ScriptContext theScriptContextRef;

	public KlassMaster(
		File jadFile, String inputJarFileName, File outputJarFile, String classpath, String scriptDirectory, boolean verbose, AtomicReference<PrintWriter> logPrinterRef
	) {
		checkThread();
		boolean hasSavePermission = this.getLicenseInfoLine(3).contains(SAVE_PERMISSION_MARKER);
		AtomicReference<ScriptContext> scriptContextRef = new AtomicReference<>();
		AtomicReference<BufferedReader> scriptReaderRef = new AtomicReference<>();
		Workspace workspace = null;

		try {
			String scriptPath = this.generateMidletScript(scriptDirectory, classpath, jadFile, inputJarFileName);
			workspace = this.createWorkspace(
				scriptPath,
				"ZKM_log.txt",
				"ZKM_TrimLog.txt",
				null,
				null,
				null,
				null,
				scriptDirectory,
				verbose,
				false,
				null,
				scriptContextRef,
				scriptReaderRef,
				true,
				hasSavePermission,
				true,
				logPrinterRef,
				false,
				null
			);
			ScriptContext scriptContext = scriptContextRef.get();
			theScriptContextRef = scriptContext;
			PrintWriter writer = scriptContext.getWriter();
			String currentDate = GlobalUtils.getTimestampPrefix();
			FileConsoleLogger fileConsoleLogger = new FileConsoleLogger(writer, currentDate.length(), true, scriptContext);
			EmptyConsumer consumer = EmptyConsumer.create();
			writer.println(currentDate + " Saving all classes into \"" + outputJarFile + "\"");
			System.out.println(currentDate + " Saving " + workspace.getClassesCount() + " classes...");
			workspace.writeWorkspace(3, false, false, null, outputJarFile.getParentFile(), fileConsoleLogger, scriptContext, consumer);
		} catch (IOException ioException) {
			ScriptContext ioScriptContext = scriptContextRef.get();
			if (ioScriptContext != null) {
				ioScriptContext.getWriter().println(ioException.getMessage());
			}

			throw new LookupException(ioException.toString());
		} catch (ProcessingException | ParsingException | LexicalException scriptException) {
			ScriptContext scriptErrorContext = scriptContextRef.get();
			if (scriptErrorContext != null) {
				scriptErrorContext.getWriter().println(scriptException.getMessage());
			}

			throw new LookupException(scriptException.getMessage());
		} finally {
			try {
				BufferedReader bufferedReader = scriptReaderRef.get();
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch (IOException ignored) {
			}

			if (workspace != null) {
				workspace.cleanup();
			}

			releaseThreadLock();
		}

		ScriptContext closingScriptContext = scriptContextRef.get();
		if (closingScriptContext != null) {
			closingScriptContext.close();
		}
	}

	public KlassMaster(String[] args, AtomicReference<PrintWriter> logPrinterRef) throws IOException {
		checkThread();
		AtomicReference<String> zkmTreatAsProguardReasonRef = new AtomicReference<>();
		if (args.length == 0) {
			this.initUI(logPrinterRef);
		} else if (!Options.ZKM_PROGUARD_STYLE && !this.parseProguardCommands(args, zkmTreatAsProguardReasonRef)) {
			String zkmScriptFilePath = null;
			String logFilePath = null;
			String trimLogFilePath = null;
			String defaultExcludeFilePath = null;
			String defaultTrimExcludeFilePath = null;
			String defaultMethodParameterChangesExcludeFilePath = null;
			String defaultMethodParameterObfuscationExcludeFilePath = null;
			String defaultDirectory = null;
			boolean parseScriptFileOnly = false;
			boolean verboseMode = false;
			String redirectStdOutFileName = null;

			for (int argIndex = 0; argIndex < args.length; argIndex++) {
				if (argIndex == args.length - 1) {
					if (args[argIndex].equalsIgnoreCase("-?")) {
						this.printHelpAndExit();
					} else if (args[argIndex].startsWith("-")) {
						this.printError("\"" + args[argIndex] + "\" is not a valid ZKM Script file name");
					}

					zkmScriptFilePath = args[argIndex];
				} else if (args[argIndex].equalsIgnoreCase("-p")) {
					parseScriptFileOnly = true;
				} else if (args[argIndex].equalsIgnoreCase("-v")) {
					verboseMode = true;
				} else if (args[argIndex].equalsIgnoreCase("-?")) {
					this.printHelpAndExit();
				} else if (args[argIndex].toLowerCase().startsWith("-l")) {
					if (++argIndex < args.length) {
						logFilePath = args[argIndex];
						if (logFilePath.startsWith("-")) {
							this.printError("\"" + logFilePath + "\" is not a valid log file name");
						}
					} else {
						this.printError("Missing log file name");
					}
				} else if (args[argIndex].toLowerCase().startsWith("-tl")) {
					if (++argIndex < args.length) {
						trimLogFilePath = args[argIndex];
						if (trimLogFilePath.startsWith("-")) {
							this.printError("\"" + trimLogFilePath + "\" is not a valid trim log file name");
						}
					} else {
						this.printError("Missing trim log file name");
					}
				} else if (args[argIndex].toLowerCase().startsWith("-de")) {
					if (++argIndex < args.length) {
						defaultExcludeFilePath = args[argIndex];
						if (defaultExcludeFilePath.startsWith("-")) {
							this.printError("\"" + defaultExcludeFilePath + "\" is not a valid default exclude file name");
						}
					} else {
						this.printError("Missing default exclude file name");
					}
				} else if (args[argIndex].toLowerCase().startsWith("-dte")) {
					if (++argIndex < args.length) {
						defaultTrimExcludeFilePath = args[argIndex];
						if (defaultTrimExcludeFilePath.startsWith("-")) {
							this.printError("\"" + defaultTrimExcludeFilePath + "\" is not a valid default trim exclude file name");
						}
					} else {
						this.printError("Missing default trim exclude file name");
					}
				} else if (args[argIndex].toLowerCase().startsWith("-dpe")) {
					if (++argIndex < args.length) {
						defaultMethodParameterChangesExcludeFilePath = args[argIndex];
						if (defaultMethodParameterChangesExcludeFilePath.startsWith("-")) {
							this.printError("\"" + defaultMethodParameterChangesExcludeFilePath + "\" is not a valid default method parameter changes exclude file name");
						}
					} else {
						this.printError("Missing default method parameter changes exclude file name");
					}
				} else if (args[argIndex].toLowerCase().startsWith("-dpo")) {
					if (++argIndex < args.length) {
						defaultMethodParameterObfuscationExcludeFilePath = args[argIndex];
						if (defaultMethodParameterObfuscationExcludeFilePath.startsWith("-")) {
							this.printError("\"" + defaultMethodParameterObfuscationExcludeFilePath + "\" is not a valid default method parameter obfuscation exclude file name");
						}
					} else {
						this.printError("Missing default method parameter changes exclude file name");
					}
				} else if (args[argIndex].toLowerCase().startsWith("-dd")) {
					if (++argIndex < args.length) {
						defaultDirectory = args[argIndex];
						if (defaultDirectory.startsWith("-")) {
							this.printError("\"" + defaultDirectory + "\" is not a valid 'default directory' name");
						}
					} else {
						this.printError("Missing 'files directory' name");
					}
				} else if (args[argIndex].toLowerCase().startsWith("-ro")) {
					if (++argIndex < args.length) {
						redirectStdOutFileName = args[argIndex];
						if (redirectStdOutFileName.startsWith("-")) {
							this.printError("\"" + redirectStdOutFileName + "\" is not a valid stdout redirect file name");
						}
					} else {
						this.printError("Missing stdout redirect file name");
					}
				} else {
					this.printError("\"" + args[argIndex] + "\" is not a valid parameter");
				}
			}

			if (zkmScriptFilePath == null) {
				this.printError("Missing ZKM Script file name");
			}

			boolean hasSavePermission = this.getLicenseInfoLine(3).contains(SAVE_PERMISSION_MARKER);
			AtomicReference<ScriptContext> scriptContextReference = new AtomicReference<>();
			AtomicReference<BufferedReader> scriptReaderReference = new AtomicReference<>();

			try {
				if (redirectStdOutFileName != null) {
					redirectStdout(redirectStdOutFileName);
				}

				this.createWorkspace(
					zkmScriptFilePath,
					logFilePath,
					trimLogFilePath,
					defaultExcludeFilePath,
					defaultTrimExcludeFilePath,
					defaultMethodParameterChangesExcludeFilePath,
					defaultMethodParameterObfuscationExcludeFilePath,
					defaultDirectory,
					verboseMode,
					parseScriptFileOnly,
					null,
					scriptContextReference,
					scriptReaderReference,
					false,
					hasSavePermission,
					false,
					logPrinterRef,
					false,
					null
				);
			} catch (LexicalException lexicalException) {
				System.err.println(lexicalException.getMessage());
				ScriptContext lexicalScriptContext = scriptContextReference.get();
				if (lexicalScriptContext != null) {
					lexicalScriptContext.getWriter().println(lexicalException.getMessage());
				}

				System.exit(1);
			} catch (ParsingException parsingException) {
				System.err.println(parsingException.getMessage());
				ScriptContext parsingScriptContext = scriptContextReference.get();
				if (parsingScriptContext != null) {
					parsingScriptContext.getWriter().println(parsingException.getMessage());
				}

				System.exit(1);
			} catch (ProcessingException processingException) {
				System.err.println(processingException.getMessage());
				System.exit(1);
			} finally {
				if (originalSystemOut != null && originalSystemOut != System.out) {
					System.setOut(originalSystemOut);
				}

				try {
					BufferedReader reader = scriptReaderReference.get();
					if (reader != null) {
						reader.close();
					}
				} catch (IOException ignored) {
				}

				releaseThreadLock();
			}

			ScriptContext closingScriptContext = scriptContextReference.get();
			if (closingScriptContext != null) {
				closingScriptContext.close();
			}

			System.exit(0);
		} else {
			String reason;
			if (zkmTreatAsProguardReasonRef.isNull()) {
				reason = "System property 'ZKM_PROGUARD_STYLE' is set to 'true'.";
			} else {
				reason = zkmTreatAsProguardReasonRef.get();
			}

			System.out.println(GlobalUtils.getTimestampPrefix() + " Treating input as ProGuard style input because " + reason);
			this.executeProguardStyleScript(args, null, logPrinterRef, true);
		}
	}

	public KlassMaster(String proguardConfigPath, Properties properties, AtomicReference<PrintWriter> logPrinterRef) {
		String[] proguardArgs = new String[]{"-include", proguardConfigPath};
		this.executeProguardStyleScript(proguardArgs, properties, logPrinterRef, true);
	}

	public KlassMaster() {
	}

	public KlassMaster(
		String scriptPath,
		String logFilePath,
		String trimLogFilePath,
		String defaultExcludeFilePath,
		String defaultTrimExcludeFilePath,
		String defaultMethodParameterChangesExcludeFilePath,
		String defaultMethodParameterObfuscationExcludeFilePath,
		String defaultDirectory,
		boolean verbose,
		boolean parseScriptOnly,
		Properties configProperties,
		AtomicReference<PrintWriter> logPrinterRef
	) throws IOException {
		checkThread();
		boolean hasSavePermission = this.getLicenseInfoLine(3).indexOf(SAVE_PERMISSION_MARKER) != -1;
		AtomicReference<ScriptContext> scriptContextRef = new AtomicReference<>();
		AtomicReference<BufferedReader> scriptReaderRef = new AtomicReference<>();
		Workspace workspace = null;

		try {
			workspace = this.createWorkspace(
				scriptPath,
				logFilePath,
				trimLogFilePath,
				defaultExcludeFilePath,
				defaultTrimExcludeFilePath,
				defaultMethodParameterChangesExcludeFilePath,
				defaultMethodParameterObfuscationExcludeFilePath,
				defaultDirectory,
				verbose,
				parseScriptOnly,
				configProperties,
				scriptContextRef,
				scriptReaderRef,
				true,
				hasSavePermission,
				false,
				logPrinterRef,
				false,
				null
			);
		} catch (LexicalException lexicalException) {
			ScriptContext lexicalScriptContext = scriptContextRef.get();
			if (lexicalScriptContext != null) {
				lexicalScriptContext.getWriter().println(lexicalException.getMessage());
			}

			throw new LookupException(lexicalException.getMessage());
		} catch (ParsingException parsingException) {
			ScriptContext parsingScriptContext = scriptContextRef.get();
			if (parsingScriptContext != null) {
				parsingScriptContext.getWriter().println(parsingException.getMessage());
			}

			throw new LookupException(parsingException.getMessage());
		} catch (ProcessingException processingException) {
			ScriptContext processingScriptContext = scriptContextRef.get();
			if (processingScriptContext != null) {
				processingScriptContext.getWriter().println(processingException.getMessage());
			}

			throw new LookupException(processingException.getMessage());
		} finally {
			try {
				BufferedReader reader = scriptReaderRef.get();
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ignored) {
			}

			if (workspace != null) {
				workspace.cleanup();
			}

			releaseThreadLock();
		}

		ScriptContext closingScriptContext = scriptContextRef.get();
		if (closingScriptContext != null) {
			closingScriptContext.close();
		}
	}

	public static synchronized void releaseThreadLock() {
		long threadId = GlobalUtils.getThreadId();
		if (currentThreadId != -1L && threadId == currentThreadId) {
			currentThreadId = -1L;
		}
	}

	public static String filterExistingPaths(String classpath) {
		StringBuffer pathBuffer = new StringBuffer();
		StringTokenizer tokenizer = new StringTokenizer(classpath, SystemConstants.pathSeparator);

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (new File(token).exists()) {
				if (pathBuffer.length() > 0) {
					pathBuffer.append(SystemConstants.pathSeparator);
				}

				pathBuffer.append(token);
			}
		}

		return pathBuffer.toString();
	}

	public static Image getIcon(Component component) {
		String iconFileName;
		if (!SystemConstants.osName.equals("SunOS") && !SystemConstants.osName.equals("Solaris") && !SystemConstants.osName.equals("HP_UX")) {
			iconFileName = "2.gif";
		} else {
			iconFileName = "3.gif";
		}

		return component.getToolkit().getImage(Options.uiConfig.getClass().getResource(iconFileName));
	}

	public static Font getFont() {
		if (defaultFont == null) {
			defaultFont = new Font("Dialog", 0, 12);
		}

		return defaultFont;
	}

	private static String getDefaultClasspath() {
		String classpath = Options.uiConfig.getLastClassPath();
		if (classpath != null && classpath.length() > 0) {
			classpath = filterExistingPaths(classpath);
		} else {
			classpath = System.getProperty("java.class.path");
		}

		return classpath;
	}

	private static void redirectStdout(String redirectFileName) {
		if (redirectFileName != null && redirectFileName.trim().length() > 0) {
			try {
				PrintStream printStream = new PrintStream(new FileOutputStream(redirectFileName), true);
				originalSystemOut = System.out;
				System.setOut(printStream);
			} catch (IOException ioException) {
				throw new ProcessingException(ioException.toString());
			}
		}
	}

	public static synchronized void checkThread() {
		long threadId = GlobalUtils.getThreadId();
		if (currentThreadId != -1L && threadId != currentThreadId) {
			throw new LookupException(
				CopyrightConstants.NAME + " is not thread safe. If you need to run more than once instance concurrently then each instance must run in a separate JVM with different 'java.io.tmpdir' System property values."
			);
		} else {
			currentThreadId = threadId;
		}
	}

	public static void printSystemDiagnostics(PrintWriter writer, Properties extraProperties) {
		if (extraProperties != null) {
			GlobalUtils.listProperties("extra", extraProperties, writer);
			writer.println();
		}

		Properties systemProperties = System.getProperties();
		GlobalUtils.listProperties("system", systemProperties, writer);
		writer.println();
		writer.println("defaultCharset=" + Charset.defaultCharset());
		writer.println();
		long maxMemory = Runtime.getRuntime().maxMemory();
		if (maxMemory > -1L) {
			writer.println("Maximum memory is " + maxMemory / 1024L + "K.");
			writer.println();
		}

		try {
			List<String> jvmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
			if (!jvmArguments.isEmpty()) {
				writer.println("JVM options: " + GlobalUtils.toQuotedString(jvmArguments));
				writer.println();
			}
		} catch (Throwable ignored) {
		}

		if (Options.ZKM_USE_PARALLEL) {
			if (Options.threadCount >= 2) {
				writer.println("Parallel processing active. " + Options.threadCount + " processors available.");
				writer.println();
			} else {
				writer.println("Only " + Options.threadCount + " processors available.");
				writer.println();
			}
		}
	}

	public void showStackTraceTranslate() {
		SplashDoneCallback splashDoneCallback = new SplashDoneCallback(this);
		String versionTitle = CopyrightConstants.NAME + SUFFIX + " " + CopyrightConstants.VERSION;
		String[] licenseDisplayLines = null;
		String licenseNumber = this.getLicenseInfoLine(1);
		String headerLine = GlobalUtils.getTimestampPrefix()
			+ " "
			+ versionTitle
			+ (licenseNumber != null && !licenseNumber.equals("none") ? " - License #" + licenseNumber : "");
		String licenseType = this.getLicenseInfoLine(3);
		String licensee = this.getLicenseInfoLine(5);
		String licenseLocation = this.getLicenseInfoLine(7);
		if (licensee.length() > 57) {
			licensee = licensee.substring(0, 57);
		}

		if (licenseLocation.length() > 57) {
			licenseLocation = licenseLocation.substring(0, 57);
		}

		if (licenseType != null) {
			int parenIndex = licenseType.indexOf("(");
			if (parenIndex == -1) {
				licenseDisplayLines = new String[]{licenseType, licensee, licenseLocation, CopyrightConstants.COPYRIGHT, CopyrightConstants.COPYRIGHT2};
			} else {
				licenseDisplayLines = new String[]{
					licenseType.substring(0, parenIndex),
					licenseType.substring(parenIndex),
					licensee,
					licenseLocation,
					CopyrightConstants.COPYRIGHT,
					CopyrightConstants.COPYRIGHT2
				};
			}
		}

		System.out.println(headerLine);
		if (licenseDisplayLines != null) {
			for (int i = 0; i < licenseDisplayLines.length; i++) {
				System.out
					.println(StringUtils.justificateString(licenseDisplayLines[i], 82, licenseDisplayLines[i].length() + GlobalUtils.getTimestampPrefix().length() + 1, 32));
			}
		}

		StacktraceTranslatorDialog stacktraceDialog = new StacktraceTranslatorDialog(CopyrightConstants.NAME + " - Stand Alone Stack Trace Translate Tool", Options.uiConfig);
		stacktraceDialog.setEnabled(false);
		GuiUtils.setVisible(stacktraceDialog, false);
		new SplashScreen(stacktraceDialog, this, splashDoneCallback);
	}

	private boolean parseProguardCommands(String[] args, AtomicReference<String> reasonRef) {
		for (String arg : args) {
			if (ProguardCommandsConstants.PROGUARD_CLI_OPTION_SET.contains(arg)) {
				reasonRef.set("the command line option '" + arg + "' is present.");
				return true;
			}
		}

		for (int argIndex = 0; argIndex < args.length; argIndex++) {
			if (!args[argIndex].isEmpty() && args[argIndex].charAt(0) != '-') {
				boolean atPrefixed = false;
				if (args[argIndex].charAt(0) == '@') {
					atPrefixed = true;
					args[argIndex] = args[argIndex].substring(1);
				}

				try {
					File argFile = new File(args[argIndex]);
					String fileContent = FileUtils.readToString(argFile, FileUtils.DEFAULT_ENCODING);
					if (this.containsProguardCommand("-injars", fileContent)) {
						reasonRef.set("file '" + argFile.getAbsolutePath() + "' contains ProGuard command '-injars'");
						if (args[argIndex].charAt(0) != '@') {
							if (!atPrefixed) {
								System.out
									.println(
										GlobalUtils.getTimestampPrefix()
											+ " WARNING: Prepending '@' to front of command line argument '"
											+ args[argIndex]
											+ "' to make it valid ProGuard style input."
									);
							}

							args[argIndex] = '@' + args[argIndex];
						}

						return true;
					}
				} catch (DebugAssertFailedException debugAssertException) {
					throw debugAssertException;
				} catch (Throwable ignored) {
				}
			}
		}

		return false;
	}

	private void printErrorAndExit(String message, boolean shouldExit) {
		System.err.println("ERROR: " + message);
		if (shouldExit) {
			System.exit(1);
		} else {
			throw new RuntimeException("ERROR: " + message);
		}
	}

	public void executeProguardStyleScript(String[] args, Properties properties, AtomicReference<PrintWriter> logPrinterRef, boolean shouldExitOnError) {
		checkThread();
		boolean hasSavePermission = this.getLicenseInfoLine(3).indexOf(SAVE_PERMISSION_MARKER) != -1;
		AtomicReference<ScriptContext> scriptContextRef = new AtomicReference<>();
		AtomicReference<BufferedReader> scriptReaderRef = new AtomicReference<>();
		Workspace workspace = null;
		AtomicReference<File> logFileRef = new AtomicReference<>();
		AtomicReference<String> errorRef = new AtomicReference<>();
		if (!ProguardScriptTranslator.createDefaultDirAndLogFile(args, logFileRef, errorRef)) {
			this.printErrorAndExit(errorRef.get(), shouldExitOnError);
		}

		File logFile = logFileRef.get();
		System.out.println(GlobalUtils.getTimestampPrefix() + " Preliminary log file name is '" + logFile.getAbsolutePath() + "'");
		PrintWriter externalWriter = null;

		try {
			AtomicReference<String> logFilePathRef = new AtomicReference<>();
			AtomicReference<String> trimLogFilePathRef = new AtomicReference<>();
			AtomicReference<String> defaultDirRef = new AtomicReference<>();
			AtomicBool verboseFlagRef = new AtomicBool();
			externalWriter = new PrintWriter(new FileWriter(logFile), true);
			printSystemDiagnostics(externalWriter, null);
			this.initLicenseInfos();
			this.printLicenseInfo(externalWriter);
			String translatedScriptPath = ProguardConfigUtils.translateProGuardConfig(
				args, properties, logFilePathRef, trimLogFilePathRef, defaultDirRef, verboseFlagRef, logFile.getAbsolutePath(), externalWriter, shouldExitOnError
			);
			String logFilePath = logFilePathRef.isNull() ? "ZKM_log.txt" : logFilePathRef.get();
			String trimLogFilePath = trimLogFilePathRef.isNull() ? "ZKM_TrimLog.txt" : trimLogFilePathRef.get();
			String defaultExcludeFilePath = null;
			String defaultTrimExcludeFilePath = null;
			String defaultMethodParameterChangesExcludeFilePath = null;
			String defaultMethodParameterObfuscationExcludeFilePath = null;
			String defaultDir = defaultDirRef.get();
			boolean verbose = verboseFlagRef.get();
			Properties nullProperties = null;
			boolean shouldNotExitOnError = !shouldExitOnError;
			workspace = this.createWorkspace(
				translatedScriptPath,
				logFilePath,
				trimLogFilePath,
				defaultExcludeFilePath,
				defaultTrimExcludeFilePath,
				defaultMethodParameterChangesExcludeFilePath,
				defaultMethodParameterObfuscationExcludeFilePath,
				defaultDir,
				verbose,
				false,
				nullProperties,
				scriptContextRef,
				scriptReaderRef,
				shouldNotExitOnError,
				hasSavePermission,
				false,
				logPrinterRef,
				true,
				externalWriter
			);
		} catch (IOException ioException) {
			ScriptContext ioScriptContext = scriptContextRef.get();
			if (ioScriptContext != null) {
				ioScriptContext.getWriter().println(ioException.getMessage());
			}

			if (!shouldExitOnError) {
				throw new LookupException(ioException.toString());
			}

			System.err.println(ioException.toString());
			System.exit(1);
		} catch (LexicalException lexicalException) {
			ScriptContext lexicalScriptContext = scriptContextRef.get();
			if (lexicalScriptContext != null) {
				lexicalScriptContext.getWriter().println(lexicalException.getMessage());
			}

			if (!shouldExitOnError) {
				throw new LookupException(lexicalException.toString());
			}

			System.err.println(lexicalException.toString());
			System.exit(1);
		} catch (ParsingException parsingException) {
			ScriptContext parsingScriptContext = scriptContextRef.get();
			if (parsingScriptContext != null) {
				parsingScriptContext.getWriter().println(parsingException.getMessage());
			}

			if (!shouldExitOnError) {
				throw new LookupException(parsingException.toString());
			}

			System.err.println(parsingException.toString());
			System.exit(1);
		} catch (ProcessingException processingException) {
			ScriptContext processingScriptContext = scriptContextRef.get();
			if (processingScriptContext != null) {
				processingScriptContext.getWriter().println(processingException.getMessage());
			}

			if (!shouldExitOnError) {
				throw new LookupException(processingException.toString());
			}

			System.err.println(processingException.toString());
			System.exit(1);
		} finally {
			try {
				BufferedReader reader = scriptReaderRef.get();
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ignored) {
			}

			if (workspace != null) {
				workspace.cleanup();
			}

			ScriptContext closingScriptContext = scriptContextRef.get();
			if (closingScriptContext != null) {
				closingScriptContext.close();
			}

			if (externalWriter != null) {
				externalWriter.close();
			}

			releaseThreadLock();
		}
	}

	private void printLicenseInfo(PrintWriter writer) {
		writer.println(this.firstLog);
		if (this.licenseInfos != null) {
			for (int i = 0; i < this.licenseInfos.length; i++) {
				writer.println(StringUtils.justificateString(this.licenseInfos[i], 82, this.licenseInfos[i].length() + GlobalUtils.getTimestampPrefix().length() + 1, 32));
			}
		}
	}

	private boolean containsProguardCommand(String command, String content) {
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new StringReader(content));

			String line;
			while ((line = reader.readLine()) != null) {
				int commentIndex = line.indexOf("//");
				if (commentIndex > -1) {
					line = line.substring(0, commentIndex);
				}

				if (line.indexOf(command) > -1) {
					return true;
				}
			}

			return false;
		} catch (IOException ignored) {
			return false;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException closeIgnored) {
				}
			}
		}
	}

	private void setupStream(PrintStream stream) {
		stream.println(this.firstLog);
		if (this.licenseInfos != null) {
			for (int i = 0; i < this.licenseInfos.length; i++) {
				String truncatedLine;
				if (this.licenseInfos[i].length() > 57) {
					truncatedLine = this.licenseInfos[i].substring(0, 57);
				} else {
					truncatedLine = this.licenseInfos[i];
				}

				stream.println(StringUtils.justificateString(truncatedLine, 82, truncatedLine.length() + GlobalUtils.getTimestampPrefix().length() + 1, 32));
			}
		}
	}

	private void printHelp(PrintStream stream) {
		stream.println();
		stream.println("Usage: java -jar ZKM.jar [[-options] <scriptFileName>]");
		stream.println();
		stream.println("Where <scriptFileName> is name of ZKM Script file to execute (non-GUI mode).");
		stream.println();
		stream.println("   If all options and <scriptFileName> are missing then start in GUI mode.");
		stream.println();
		stream.println("Where the options are:");
		stream.println("   -?\tDisplay usage");
		stream.println("   -p\tOnly parse the ZKM Script file (Don't execute it)");
		stream.println("   -v\tTurn on verbose mode");
		stream.println("   -l <logFileName>");
		stream.println("     \tUse specified log file. Default name is 'ZKM_log.txt'");
		stream.println("   -tl <trimLogFileName>");
		stream.println("     \tUse specified trim log file.");
		stream.println("     \tDefault name is 'ZKM_TrimLog.txt'");
		stream.println("   -de <defaultExcludeFileName>");
		stream.println("     \tUse specified 'default exclusions' file.");
		stream.println("     \tDefault name is 'defaultExclude.txt'");
		stream.println("   -dte <defaultTrimExcludeFileName>");
		stream.println("     \tUse specified 'default trim exclusions' file.");
		stream.println("     \tDefault name is 'defaultTrimExclude.txt'");
		stream.println("   -dpe <defaultMethodParameterChangesExcludeFileName>");
		stream.println("     \tUse specified 'default method parameter changes exclusions' file.");
		stream.println("     \tDefault name is 'defaultMethodParameterChangesExclude.txt'");
		stream.println("   -dpo <defaultMethodParameterObfuscationExcludeFileName>");
		stream.println("     \tUse specified 'default method parameter obfuscation exclusions' file.");
		stream.println("     \tDefault name is 'defaultMethodParameterChangesExclude.txt'");
		stream.println("   -dd <defaultDirectoryName>");
		stream.println("     \tUse specified directory for reading and writing files");
		stream.println("     \tthat have not be specified with an absolute path.");
		stream.println("     \tDefaults to current working directory.");
		stream.println("   -ro <fileName>");
		stream.println("     \tRedirect stdout to the specified file. Redirects messages and warnings.");
		stream.println("     \tErrors will still be written to stderr.");
		stream.println();
	}

	private void enableAndShowDialog(Object dialog) {
		StacktraceTranslatorDialog stacktraceDialog = (StacktraceTranslatorDialog)dialog;
		stacktraceDialog.setEnabled(true);
		stacktraceDialog.positionDialog(0, 0, false);
		stacktraceDialog.focusBrowseButton();
	}

	private void printHelpOnErrorAndExit() {
		this.printHelp(System.err);
		System.exit(1);
	}

	private void printError(String message) {
		System.err.println();
		System.err.println("ERROR: " + message);
		this.printHelpOnErrorAndExit();
	}

	private void initUI(AtomicReference<PrintWriter> logPrinterRef) {
		String licenseNumber = this.getLicenseInfoLine(1);
		if (licenseNumber != null) {
			new StringBuilder().append(" - License #").append(licenseNumber).append(".").toString();
		} else {
			String emptyString = "";
		}

		boolean hasSavePermission = this.getLicenseInfoLine(3).contains(SAVE_PERMISSION_MARKER);
		boolean lafSetSuccess = false;
		if (Options.ZKM_LAF_NAME != null) {
			try {
				UIManager.setLookAndFeel(Options.ZKM_LAF_NAME);
				lafSetSuccess = true;
			} catch (Exception lafIgnored) {
			}
		}

		if (!lafSetSuccess) {
			String lafClassName;
			if (Options.uiConfig.getUseCrossPlatformLaf()) {
				lafClassName = UIManager.getCrossPlatformLookAndFeelClassName();
			} else {
				lafClassName = UIManager.getSystemLookAndFeelClassName();
			}

			if (lafClassName != null) {
				try {
					UIManager.setLookAndFeel(lafClassName);
				} catch (Exception fallbackLafIgnored) {
				}
			}
		}

		AtomicReference<String> statusRef = new AtomicReference<>();
		String defaultClasspath = getDefaultClasspath();
		ClasspathManager classpathManager = new ClasspathManager(defaultClasspath);
		Workspace workspace = new Workspace(statusRef, classpathManager, false, hasSavePermission, FileUtils.IS_UNIX, false, this);
		AtomicBool bootClasspathInitialized = new AtomicBool(false);
		AtomicReference<String> legacyBootCpRef = new AtomicReference<>();
		AtomicReference<String> jrtFsRef = new AtomicReference<>();
		boolean initSuccess = classpathManager.initializeBootClasspath(jrtFsRef, legacyBootCpRef);
		if (initSuccess) {
			Options.uiConfig.setLastClassPath(classpathManager.getRawClasspath());
			Options.uiConfig.saveConfig();
			workspace.setupObjectMethods();
		}

		bootClasspathInitialized.set(initSuccess);
		SimpleLogger logger = new SimpleLogger();
		EmptyConsumer consumer = EmptyConsumer.create();
		AtomicReference<String> errorRef = new AtomicReference<>();
		if (!FileUtils.testFileSystemWriteRead("ZKM_log.txt", errorRef)) {
			logger.error("ERROR:", "Log file \"ZKM_log.txt\" could not be created, modified and read. : '" + errorRef.get() + "' (A)");
		}

		PrintWriter out = null;

		try {
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream("ZKM_log.txt"), "UTF-8"), true);
		} catch (IOException ioException) {
			logger.error("ERROR:", "Couldn't open ZKM_log.txt : " + ioException.getClass().getName());
		}

		PrintWriter sout = out;
		logPrinterRef.set(out);
		Properties systemProperties = System.getProperties();
		GlobalUtils.listProperties(systemProperties, out);
		out.println();
		long maxMemory = Runtime.getRuntime().maxMemory();
		if (maxMemory > -1L) {
			out.println("Maximum memory is " + maxMemory / 1024L + "K.");
			out.println();
		}

		try {
			List<String> jvmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
			if (!jvmArguments.isEmpty()) {
				sout.println("JVM options: " + GlobalUtils.toQuotedString(jvmArguments));
				sout.println();
			}
		} catch (Throwable ignored) {
		}

		if (Options.ZKM_USE_PARALLEL) {
			if (Options.threadCount >= 2) {
				out.println("Parallel processing active. " + Options.threadCount + " processors available.");
				out.println();
			} else {
				out.println("Only " + Options.threadCount + " processors available.");
				out.println();
			}
		}

		this.initLicenseInfos();
		this.setupStream(System.out);
		this.printLicenseInfo(out);
		if (initSuccess) {
			if (!jrtFsRef.isNull()) {
				out.println(GlobalUtils.getTimestampPrefix() + " Using \"" + jrtFsRef.get() + "\" as path to java.lang.Object (A)");
			} else if (!legacyBootCpRef.isNull()) {
				out.println(GlobalUtils.getTimestampPrefix() + " Using \"" + legacyBootCpRef.get() + "\" as path to java.lang.Object (B)");
			}
		}

		LoadLogThread loadLogThread = new LoadLogThread(this, logger, workspace, consumer, out);
		new Thread(loadLogThread).start();
		GUI gui = new GUI(CopyrightConstants.NAME + SUFFIX, workspace, statusRef, classpathManager, "ZKM_log.txt", out, Options.uiConfig);
		workspace.subscribe(gui);
		Dimension size = gui.getSize();
		size.width--;
		gui.setSize(size);
		GUIDisplayCallback guiDisplayCallback = new GUIDisplayCallback(this);
		gui.setEnabled(false);
		GuiUtils.setVisible(gui, true);
		new SplashScreen(gui, this, guiDisplayCallback);
		if (false) {
			new EvaluationAgreementPrompt(gui, this, guiDisplayCallback);
		}
	}

	private Workspace createWorkspace(
		String scriptPath,
		String logFilePath,
		String trimLogFilePath,
		String defaultExcludeFilePath,
		String defaultTrimExcludeFilePath,
		String defaultMethodParameterChangesExcludeFilePath,
		String defaultMethodParameterObfuscationExcludeFilePath,
		String defaultDirectory,
		boolean verbose,
		boolean skipExecution,
		Properties configProperties,
		AtomicReference<ScriptContext> outScriptContext,
		AtomicReference<BufferedReader> outScriptReader,
		boolean enableLogs,
		boolean allowSave,
		boolean pluginMode,
		AtomicReference<PrintWriter> outPrintWriter,
		boolean enableImplementsClauseMatching,
		PrintWriter externalLogger
	) throws IOException {
		AtomicReference<String> statusReference = new AtomicReference<>();
		ClasspathManager classpathManager = new ClasspathManager(System.getProperty("java.class.path"));
		Workspace workspace = new Workspace(statusReference, classpathManager, verbose, allowSave, FileUtils.IS_UNIX, enableImplementsClauseMatching, this);
		ScriptContext scriptContext = new ScriptContext(
			workspace,
			classpathManager,
			verbose,
			logFilePath,
			trimLogFilePath,
			defaultExcludeFilePath,
			defaultTrimExcludeFilePath,
			defaultMethodParameterChangesExcludeFilePath,
			defaultMethodParameterObfuscationExcludeFilePath,
			defaultDirectory,
			enableLogs,
			pluginMode,
			enableImplementsClauseMatching
		);
		outScriptContext.set(scriptContext);
		String logFile = scriptContext.getLogFilePath();
		File defDirPath = scriptContext.getDefaultDirectory();
		String normalizedDefDirPath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(scriptPath), defDirPath);
		File defDirectory;
		if (FileUtils.isRelativePath(normalizedDefDirPath)) {
			defDirectory = new File(defDirPath, normalizedDefDirPath);
		} else {
			defDirectory = new File(normalizedDefDirPath);
		}

		AtomicReference<String> legacyBootCpRef = new AtomicReference<>();
		AtomicReference<String> jrtFsRef = new AtomicReference<>();
		boolean initializedBootClasspath = classpathManager.initializeBootClasspath(jrtFsRef, legacyBootCpRef);
		if (initializedBootClasspath) {
			workspace.setupObjectMethods();
		}

		PrintWriter writer = scriptContext.getWriter();
		outPrintWriter.set(writer);
		if (externalLogger != null) {
			externalLogger.println();
			externalLogger.println(GlobalUtils.getTimestampPrefix() + " Default directory set to '" + scriptContext.getDefaultDirectory().getAbsolutePath() + "'");
			externalLogger.println(GlobalUtils.getTimestampPrefix() + ' ' + CopyrightConstants.NAME + " log file is '" + logFile + "'");
		}

		if (verbose) {
			printSystemDiagnostics(writer, configProperties);
		}

		if (initializedBootClasspath) {
			if (!jrtFsRef.isNull()) {
				writer.println(GlobalUtils.getTimestampPrefix() + " Using \"" + jrtFsRef.get() + "\" as path to java.lang.Object (C)");
			} else if (!legacyBootCpRef.isNull()) {
				writer.println(GlobalUtils.getTimestampPrefix() + " Using \"" + legacyBootCpRef.get() + "\" as path to java.lang.Object (D)");
			}
		}

		this.initLicenseInfos();
		this.setupStream(System.out);
		this.printLicenseInfo(writer);
		System.out.println(GlobalUtils.getTimestampPrefix() + " Preprocessing ZKM Script file...");
		writer.println(
			GlobalUtils.getTimestampPrefix() + " Preprocessing ZKM Script file \"" + TempFileManager.resolveTempPathInString(defDirectory.getAbsolutePath()) + "\""
		);
		ScriptPreprocessor scriptPreprocessor = new ScriptPreprocessor(defDirectory.getAbsolutePath(), configProperties);
		BufferedReader scriptReader = scriptPreprocessor.getScriptReader();
		outScriptReader.set(scriptReader);
		if (verbose) {
			writer.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
			writer.println(scriptPreprocessor.getPreprocessedScript());
			writer.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
			writer.println();
		}

		System.out.println(GlobalUtils.getTimestampPrefix() + " Parsing ZKM Script file...");
		writer.println(GlobalUtils.getTimestampPrefix() + " Parsing ZKM Script file \"" + defDirectory.getAbsolutePath() + "\"");

		try {
			FileUtils.writeStreamToManagedTempFile(new BufferedInputStream(new FileInputStream(new File(logFile))), logFile);
		} catch (IOException ignored) {
		}

		ScriptInputNode parsedScript = (ScriptInputNode)new ScriptParser(scriptReader).parseScript();
		if (pluginMode) {
			File scriptFile = new File(defaultDirectory, "script.txt");
			if (parsedScript.getOutputStatementCount() > 0) {
				System.out
					.println(
						StringUtils.repeat(GlobalUtils.getTimestampPrefix().length(), 32)
							+ "WARNING: Detected a save statement in '"
							+ scriptFile
							+ "'. It is unnecessary when using the plugin."
					);
				writer.println(
					StringUtils.repeat(GlobalUtils.getTimestampPrefix().length(), 32)
						+ "WARNING: Detected a save statement in '"
						+ scriptFile
						+ "'. It is unnecessary when using the plugin."
				);
			}

			if (parsedScript.getOpenStatementCount() > 1) {
				System.err
					.println(
						StringUtils.repeat(GlobalUtils.getTimestampPrefix().length(), 32)
							+ "ERROR: Detected an 'open' statement in '"
							+ scriptFile
							+ "'. The plugin MUST specify the JAR to be opened."
					);
				writer.println(
					StringUtils.repeat(GlobalUtils.getTimestampPrefix().length(), 32)
						+ "ERROR: Detected an 'open' statement in '"
						+ scriptFile
						+ "'. The plugin MUST specify the JAR to be opened."
				);
			}
		}

		writer.flush();
		if (!skipExecution && !Options.ZKM_PARSE_ONLY) {
			parsedScript.parseArguments(null, scriptContext);
		}

		String message = GlobalUtils.getTimestampPrefix() + " Terminating normally.";
		message = message + scriptContext.getIndentationSuffix();
		System.out.println(message + " See \"" + logFile + "\" for more detail.");
		writer.println(message);
		return workspace;
	}

	private String generateMidletScript(String scriptDirectory, String classpath, File jadFile, String inputJarFileName) throws IOException {
		File tmpFile = TempFileManager.createTempFile();
		PrintWriter writer = null;

		try {
			writer = new PrintWriter(new FileWriter(tmpFile));
			writer.println("classpath   \"" + classpath + "\"; //added automatically");
			writer.println("open        \"" + jadFile + "\" //added automatically");
			writer.println("            \"" + inputJarFileName + "\"; //added automatically");
			File scriptFile = new File(scriptDirectory, "script.txt");
			if (scriptFile.exists()) {
				writer.println("//Reading from '" + scriptFile.getAbsoluteFile() + "'");
				BufferedReader scriptReader = null;

				try {
					scriptReader = FileUtils.createBufferedReader(scriptFile, Options.ZKM_SCRIPT_ENCODING);

					String line;
					while ((line = scriptReader.readLine()) != null) {
						writer.println(line);
					}
				} finally {
					if (scriptReader != null) {
						try {
							scriptReader.close();
						} catch (IOException ignored) {
						}
					}
				}
			} else {
				writer.println("//Using internal default script. '" + scriptFile.getAbsolutePath() + "' not found");
				writer.println(DEFAULT_MIDLET_SCRIPT);
			}
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}

		return tmpFile.getAbsolutePath();
	}

	public void setVisible(Object gui) {
		GUI guiInstance = (GUI)gui;
		guiInstance.setEnabled(true);
		if (!guiInstance.isVisible()) {
			GuiUtils.setVisible(guiInstance, true);
		}

		guiInstance.init();
	}

	private void printHelpAndExit() {
		this.printHelp(System.out);
		System.exit(0);
	}

	public String getLicenseInfoLine(int line) {
		String licenseStr = this.getLicenseStr();
		return "58#\\u0001\\u00031*\\u0001<\\u000erm".equals(licenseStr) ? this.getLicenseInfo(line).trim() : null;
	}

	private void initLicenseInfos() {
		if (this.firstLog == null || this.licenseInfos == null) {
			String title = CopyrightConstants.NAME + SUFFIX + " " + CopyrightConstants.VERSION;
			String licenseNumber = this.getLicenseInfoLine(1);
			this.firstLog = GlobalUtils.getTimestampPrefix() + " " + title + (licenseNumber != null && !licenseNumber.equals("none") ? " - License #" + licenseNumber : "");
			String licenseType = this.getLicenseInfoLine(3);
			String licensee = this.getLicenseInfoLine(5);
			String licenseLocation = this.getLicenseInfoLine(7);
			if (licenseType != null) {
				int parenIndex = licenseType.indexOf("(");
				if (parenIndex == -1) {
					this.licenseInfos = new String[5];
					this.licenseInfos[0] = licenseType;
					this.licenseInfos[1] = licensee;
					this.licenseInfos[2] = licenseLocation;
					this.licenseInfos[3] = CopyrightConstants.COPYRIGHT;
					this.licenseInfos[4] = CopyrightConstants.COPYRIGHT2;
				} else {
					this.licenseInfos = new String[6];
					this.licenseInfos[0] = licenseType.substring(0, parenIndex);
					this.licenseInfos[1] = licenseType.substring(parenIndex);
					this.licenseInfos[2] = licensee;
					this.licenseInfos[3] = licenseLocation;
					this.licenseInfos[4] = CopyrightConstants.COPYRIGHT;
					this.licenseInfos[5] = CopyrightConstants.COPYRIGHT2;
				}
			}
		}
	}

	public static void invokeEnableAndShowDialog(KlassMaster klassMaster, Object dialog) {
		klassMaster.enableAndShowDialog(dialog);
	}

	public static class GUIDisplayCallback extends EmptyConsumer {
		public final KlassMaster klassMaster;

		public GUIDisplayCallback(KlassMaster klassMaster) {
			this.klassMaster = klassMaster;
		}

		public void onSplashComplete(Frame frame) {
			this.klassMaster.setVisible(frame);
		}

		@Override
		public void accept(Object arg) {
			this.onSplashComplete((Frame)arg);
		}

		@Override
		public void accept() {
			System.exit(0);
		}
	}

	public static class LoadLogThread implements Runnable {
		public final KlassMaster klassMaster;
		public final SimpleLogger logger;
		public final Workspace workspace;
		public final EmptyConsumer consumer;
		public final PrintWriter writer;

		public LoadLogThread(KlassMaster klassMaster, SimpleLogger logger, Workspace workspace, EmptyConsumer consumer, PrintWriter writer) {
			this.klassMaster = klassMaster;
			this.logger = logger;
			this.workspace = workspace;
			this.consumer = consumer;
			this.writer = writer;
		}

		@SneakyThrows
		@Override
		public void run() {
			NoDuplicatePrintWriter loadLogWriter = null;

			try {
				loadLogWriter = new NoDuplicatePrintWriter(new FileWriter("ZKM_LoadLog.txt"), true);
			} catch (IOException loadLogException) {
				this.logger.error("ERROR:", "Couldn't open ZKM_LoadLog.txt : " + loadLogException.getClass().getName());
			}

			PrintWriter warningLogWriter = null;

			try {
				warningLogWriter = new PrintWriter(new FileWriter("ZKM_WarningLog.txt"), true);
			} catch (IOException warningLogException) {
				this.logger.error("ERROR:", "Couldn't open ZKM_WarningLog.txt : " + warningLogException.getClass().getName());
			}

			this.workspace
				.loadClasses(
					new ClasspathResource[0],
					new ArchiveNode[0],
					null,
					null,
					null,
					null,
					null,
					null,
					(String)null,
					new ClasspathResource[0],
					new ClasspathResource[0],
					new ClasspathResource[0],
					new ClasspathResource[0],
					true,
					this.logger,
					this.consumer,
					null,
					(ScriptContext)null,
					loadLogWriter,
					warningLogWriter,
					new AtomicInt(0),
					this.writer
				);
			if (loadLogWriter != null) {
				loadLogWriter.close();
			}

			if (warningLogWriter != null) {
				warningLogWriter.close();
			}		}
	}

	public static class SplashDoneCallback extends OptionalConsumer {
		public final KlassMaster klassMaster;

		public SplashDoneCallback(KlassMaster klassMaster) {
			this.klassMaster = klassMaster;
		}

		public void onSplashComplete(Frame frame) {
			KlassMaster.invokeEnableAndShowDialog(this.klassMaster, frame);
		}

		@Override
		public void accept(Object arg) {
			this.onSplashComplete((Frame)arg);
		}
	}
}

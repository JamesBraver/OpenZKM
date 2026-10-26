package com.zelix;

import com.zelix.exceptions.ZKMException;
import com.zelix.klassmaster.AbstractBootstrap;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.message.ErrorMessages;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.ClasspathManager;
import com.zelix.tools.retrace.lines.StacktraceAnalyzer;
import com.zelix.utils.*;
import com.zelix.utils.references.AtomicReference;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;

public class ZKMStackTraceTranslate extends ErrorMessages {
	public static final int NO_PARAM_TYPES = 1;
	private ArrayList<String> changeLogFileNames;
	private StacktraceAnalyzer stacktraceAnalyzer;
	private Set<String> classpathErrorPaths;

	public ZKMStackTraceTranslate(String changeLogFileName) {
		this(changeLogFileName, null);
	}

	public ZKMStackTraceTranslate(String changeLogFileName, String classpath) {
		if (changeLogFileName == null) {
			throw new IllegalArgumentException("Change log file name must not be null");
		} else {
			String[] changeLogFileNameArray = new String[]{changeLogFileName};
			this.mandatoryContruction(changeLogFileNameArray, classpath);
		}
	}

	public ZKMStackTraceTranslate(String[] changeLogFileNames) {
		this(changeLogFileNames, null);
	}

	public ZKMStackTraceTranslate(String[] changeLogFileNames, String classpath) {
		this.mandatoryContruction(changeLogFileNames, classpath);
	}

	private static void launchTranslateTool() {
		if (Options.ZKM_TEST_JVM_VERSION && EnvironmentUtils.getCurrentClassVersion() < 52) {
			System.err.println(ErrorMessages.lineSeparator + ErrorMessages.requireJava8 + ErrorMessages.lineSeparator);
			System.exit(1);
		}

		try {
			AbstractBootstrap.showStackTraceTranslate();
		} catch (IllegalAccessException ignored) {
			System.err.println(ErrorMessages.lineSeparator + "Unexpected Error [B]. Please report the problem to bugs2" + CopyrightConstants.EMAIL + ErrorMessages.lineSeparator);
		} catch (NoClassDefFoundError noClassDefFoundError) {
			String classpath = System.getProperty("java.class.path");
			if (classpath != null) {
				if (classpath.indexOf("ZKMTranslate.jar") == -1) {
					System.err.println("Startup Error. (Check that ZKMTranslate.jar is in classpath) (1)");
				} else if (isTranslateJarInClasspath(classpath)) {
					System.err.println(ErrorMessages.lineSeparator + "Unexpected Error [C]. Please report the problem to bugs2" + CopyrightConstants.EMAIL + ErrorMessages.lineSeparator);
				} else {
					System.err.println("Startup Error. (ZKMTranslate.jar not found where specified by classpath)");
				}
			} else {
				System.err.println("Startup Error. (Check that ZKMTranslate.jar is in classpath) (2)");
			}

			System.err.println("Classpath is: \"" + classpath + "\"");
		} catch (InvocationTargetException invocationTargetException) {
			Throwable targetException = invocationTargetException.getTargetException();
			System.err.println(ErrorMessages.lineSeparator + "Unexpected Error [D]. Please report the problem to bugs2" + CopyrightConstants.EMAIL + ErrorMessages.lineSeparator);
			targetException.printStackTrace(System.err);
		} catch (NoSuchMethodException noSuchMethodException) {
			System.err.println(ErrorMessages.lineSeparator + "Unexpected Error [E]. Please report the problem to bugs2" + CopyrightConstants.EMAIL + ErrorMessages.lineSeparator);
		} catch (Throwable throwable) {
			System.err.println(ErrorMessages.lineSeparator + "Unexpected Error [G]. Please report the problem to bugs2" + CopyrightConstants.EMAIL + ErrorMessages.lineSeparator);
			throwable.printStackTrace(System.err);
		}
	}

	private static boolean isTranslateJarInClasspath(String classpath) {
		StringTokenizer classpathTokenizer = new StringTokenizer(classpath, ErrorMessages.pathSeparator);

		while (classpathTokenizer.hasMoreTokens()) {
			String classpathEntry = classpathTokenizer.nextToken();
			if (classpathEntry.endsWith("ZKMTranslate.jar") && new File(classpathEntry).exists()) {
				return true;
			}
		}

		return false;
	}

	public static void main(String[] args) {
		startTranslateTool();
	}

	public static void startTranslateTool() {
		TempFileManager.cleanupOldTempFiles();
		launchTranslateTool();
	}

	public void close() {
		if (this.stacktraceAnalyzer != null) {
			this.stacktraceAnalyzer.close();
		}

		TempFileManager.cleanupOldTempFiles();
	}

	public String getOldClassName(String className) {
		String translatedClassName;
		try {
			translatedClassName = this.stacktraceAnalyzer.translateClassName(className);
		} catch (ZKMException zkmException) {
			translatedClassName = "ZKM ERROR (2): \"" + zkmException.getMessage() + "\"" + ErrorMessages.lineSeparator;
		}

		return translatedClassName;
	}

	public String getOldMethodName(String className, String methodName, String[] parameterTypes, String returnType) {
		try {
			return this.stacktraceAnalyzer.translateMethodName(className, methodName, parameterTypes, returnType);
		} catch (ZKMException zkmException) {
			return "ZKM ERROR (4): \"" + zkmException.getMessage() + "\"" + ErrorMessages.lineSeparator;
		}
	}

	public String getOldMethodSignatures(String className, String signature) {
		try {
			signature = signature.trim();
			int spaceIndex = signature.indexOf(" ");
			if (spaceIndex <= -1) {
				return "'" + signature + "' is not a valid method signature (3)";
			} else {
				String returnType = signature.substring(0, spaceIndex);
				String signatureBody = signature.substring(spaceIndex + 1).trim();
				int openParenIndex = signatureBody.indexOf("(");
				if (openParenIndex <= 0) {
					return "'" + signature + "' is not a valid method signature (2)";
				} else {
					String methodName = signatureBody.substring(0, openParenIndex);
					int closeParenIndex = signatureBody.indexOf(")", openParenIndex);
					if (closeParenIndex <= -1) {
						return "'" + signature + "' is not a valid method signature (1)";
					} else {
						String parameterList = signatureBody.substring(openParenIndex + 1, closeParenIndex);
						ArrayList<String> parameterTypes = new ArrayList<>();
						StringTokenizer parameterTokenizer = new StringTokenizer(parameterList, ",");

						while (parameterTokenizer.hasMoreTokens()) {
							parameterTypes.add(parameterTokenizer.nextToken().trim());
						}

						String[] parameterTypesArray = new String[parameterTypes.size()];
						parameterTypesArray = parameterTypes.toArray(parameterTypesArray);
						return this.stacktraceAnalyzer.translateMethodName(className, methodName, parameterTypesArray, returnType);
					}
				}
			}
		} catch (ZKMException zkmException) {
			return "ZKM ERROR (5): \"" + zkmException.getMessage() + "\"" + ErrorMessages.lineSeparator;
		}
	}

	public String getOldMethodName(String className, String methodName) {
		String possibleMethodsText;
		try {
			String[] possibleMethods = this.stacktraceAnalyzer.listPossibleMethods(className, methodName);
			StringBuffer methodsBuffer = new StringBuffer();
			methodsBuffer.append("[");

			for (int i = 0; i < possibleMethods.length; i++) {
				methodsBuffer.append(possibleMethods[i]);
				if (i < possibleMethods.length - 2) {
					methodsBuffer.append(", ");
				} else if (i < possibleMethods.length - 1) {
					methodsBuffer.append(" or ");
				}
			}

			methodsBuffer.append("]");
			possibleMethodsText = methodsBuffer.toString();
		} catch (ZKMException zkmException) {
			possibleMethodsText = "ZKM ERROR (3): \"" + zkmException.getMessage() + "\"" + ErrorMessages.lineSeparator;
		}

		return possibleMethodsText;
	}

	public String getTranslatedStackTrace(String stackTrace) {
		return this.getTranslatedStackTrace(stackTrace, true, 2);
	}

	public String getTranslatedStackTrace(String stackTrace, boolean analyzeBytecode, int parameterTypeFormat) {
		String translatedStackTrace;
		try {
			translatedStackTrace = this.stacktraceAnalyzer.translateStackTrace(stackTrace, analyzeBytecode, parameterTypeFormat);
		} catch (ZKMException zkmException) {
			String classpathErrorText = null;
			if (this.classpathErrorPaths != null && this.classpathErrorPaths.size() > 0) {
				classpathErrorText = GlobalUtils.toQuotedString(GlobalUtils.formatTempFileNames(this.classpathErrorPaths));
			}

			translatedStackTrace = "ZKM ERROR (1): \""
				+ zkmException.getMessage()
				+ "\""
				+ ErrorMessages.lineSeparator
				+ (classpathErrorText != null ? "Initial classpath paths in error : " + classpathErrorText + DebugUtils.LINE_SEPARATOR : "");
		}

		return translatedStackTrace;
	}

	public String getTranslatedStackTrace(String stackTrace, boolean analyzeBytecode) {
		return this.getTranslatedStackTrace(stackTrace, analyzeBytecode, 2);
	}

	public void mandatoryContruction(String[] changeLogFileNames, String classpath) {
		AbstractBootstrap.isZKMJarInClasspath(classpath);
		this.changeLogFileNames = new ArrayList<>(changeLogFileNames.length);
		int fileIndex = 0;

		for (String changeLogFileName : changeLogFileNames) {
			if (changeLogFileName == null) {
				throw new IllegalArgumentException("Change log file name " + fileIndex + " must not be null");
			}

			this.changeLogFileNames.add(changeLogFileName);
			fileIndex++;
		}

		ClasspathManager classpathManager = null;
		if (classpath != null && classpath.length() > 0) {
			this.classpathErrorPaths = GlobalUtils.newHashSet();
			classpathManager = new ClasspathManager(classpath, this.classpathErrorPaths);
			classpathManager.setClassRepository(new ClassRepository(classpathManager, FileUtils.IS_UNIX));
			classpathManager.initializeBootClasspath(new AtomicReference<>(), new AtomicReference<>());
		}

		this.stacktraceAnalyzer = new StacktraceAnalyzer(this.changeLogFileNames, classpathManager);
	}
}

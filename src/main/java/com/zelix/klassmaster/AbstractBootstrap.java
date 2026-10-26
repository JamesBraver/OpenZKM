package com.zelix.klassmaster;

import com.zelix.exceptions.EvaluationExpiredException;
import com.zelix.exceptions.LookupException;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.utils.EnvironmentUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PropertyUtils;
import com.zelix.utils.references.AtomicReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

public abstract class AbstractBootstrap extends Bootstrap {
	public static PrintStream USED_ERR_STREAM;
	public static PrintStream USED_OUT_STREAM;

	public static void run(String scriptPath, String logFilePath, boolean verbose, boolean parseScriptOnly, Hashtable<?, ?> properties) throws Exception {
		Properties convertedProperties = convertMapToProperties(properties);
		executeStandard(
			scriptPath, logFilePath, (String)null, (String)null, (String)null, (String)null, (String)null, (String)null, verbose, parseScriptOnly, convertedProperties
		);
	}

	public static void run(
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
		Properties properties
	) throws Exception {
		executeWithAllParams(
			scriptPath,
			logFilePath,
			trimLogFilePath,
			defaultExcludeFilePath,
			defaultTrimExcludeFilePath,
			(String)null,
			(String)null,
			defaultDirectory,
			(String)null,
			(String)null,
			verbose,
			parseScriptOnly,
			properties
		);
	}

	public static void run(
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
		Hashtable<?, ?> properties
	) throws Exception {
		Properties convertedProperties = convertMapToProperties(properties);
		executeStandard(
			scriptPath, logFilePath, trimLogFilePath, defaultExcludeFilePath, defaultTrimExcludeFilePath, (String)null, (String)null, defaultDirectory, verbose, parseScriptOnly, convertedProperties
		);
	}

	public static void run(
		String scriptPath,
		String logFilePath,
		String trimLogFilePath,
		String defaultExcludeFilePath,
		String defaultTrimExcludeFilePath,
		String defaultMethodParameterChangesExcludeFilePath,
		String defaultMethodParameterObfuscationExcludeFilePath,
		String defaultDirectory,
		boolean verbose,
		boolean parseScriptOnly
	) throws Exception {
		executeStandard(
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
			null
		);
	}

	public static void run(String scriptPath, String logFilePath, boolean verbose, boolean parseScriptOnly) throws Exception {
		run(scriptPath, logFilePath, verbose, parseScriptOnly, (Hashtable<?, ?>)null);
	}

	public static boolean isZKMJarInClasspath(String classpath) {
		if (classpath == null) {
			return false;
		} else {
			StringTokenizer tokenizer = new StringTokenizer(classpath, Bootstrap.pathSeparator);

			while (tokenizer.hasMoreTokens()) {
				String classpathEntry = tokenizer.nextToken();
				if (classpathEntry.endsWith("ZKM.jar") && new File(classpathEntry).exists()) {
					return true;
				}
			}

			return false;
		}
	}

	public static void showStackTraceTranslate() throws Throwable {
		Lookup lookup = MethodHandles.lookup();
		Class<?> klassMasterClass = Class.forName(GlobalUtils.getKlassMasterClassName());
		Object klassMasterInstance = klassMasterClass.newInstance();
		Class<?>[] parameterTypes = new Class<?>[0];
		MethodType methodType = MethodType.methodType(void.class, parameterTypes);
		MethodHandle methodHandle = lookup.findVirtual(klassMasterClass, "showStackTraceTranslate", methodType);
		methodHandle.invoke((Object)klassMasterInstance);
	}

	public static void getStackTrace(Throwable throwable, PrintWriter writer) {
		String stackTrace = GlobalUtils.getStackTrace(throwable);
		StringTokenizer lineTokenizer = new StringTokenizer(stackTrace, Bootstrap.lineSeparator);
		int lineCount = lineTokenizer.countTokens();
		int linesToPrint;
		if (lineCount > 5) {
			linesToPrint = lineCount - 2;
		} else {
			linesToPrint = lineCount - 1;
		}

		if (lineCount == 1) {
			String singleLine = lineTokenizer.nextToken();
			System.err.println(singleLine);
			if (writer != null) {
				writer.println(singleLine);
			}
		} else {
			for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
				String line = lineTokenizer.nextToken();
				if (lineIndex < linesToPrint) {
					System.err.println(line);
					if (writer != null) {
						writer.println(line);
					}
				}
			}
		}
	}

	public static void startKlassMaster(
		String scriptPath,
		String logFilePath,
		String trimLogFilePath,
		String defaultExcludeFilePath,
		String defaultTrimExcludeFilePath,
		String defaultMethodParameterChangesExcludeFilePath,
		String defaultMethodParameterObfuscationExcludeFilePath,
		String defaultDirectory,
		String stdoutRedirectFileName,
		String stderrRedirectFileName,
		boolean verbose,
		boolean parseScriptOnly,
		Properties properties,
		boolean proguardStyle
	) throws Exception {
		PropertyUtils.loadSystemProperties();
		if (Options.ZKM_TEST_JVM_VERSION && EnvironmentUtils.getCurrentClassVersion() < 52) {
			System.err.println(Bootstrap.lineSeparator + Bootstrap.requiresJava8 + Bootstrap.lineSeparator);
			System.exit(1);
		}

		AtomicReference<PrintWriter> logPrinterReference = new AtomicReference<>();

		try {
			if (stdoutRedirectFileName != null && stdoutRedirectFileName.trim().length() > 0) {
				PrintStream stdoutPrintStream = new PrintStream(new FileOutputStream(stdoutRedirectFileName), true);
				USED_OUT_STREAM = System.out;
				System.setOut(stdoutPrintStream);
			}

			if (stderrRedirectFileName != null && stderrRedirectFileName.trim().length() > 0 && USED_OUT_STREAM != null) {
				PrintStream stderrPrintStream;
				if (!new File(stdoutRedirectFileName).getAbsolutePath().equals(new File(stderrRedirectFileName).getAbsolutePath())) {
					stderrPrintStream = new PrintStream(new FileOutputStream(stderrRedirectFileName), true);
				} else {
					stderrPrintStream = System.out;
				}

				USED_ERR_STREAM = System.err;
				System.setErr(stderrPrintStream);
			}

			Class<?>[] parameterTypes;
			Object[] arguments;
			if (proguardStyle) {
				parameterTypes = new Class<?>[]{String.class, Properties.class, AtomicReference.class};
				arguments = new Object[]{scriptPath, properties, logPrinterReference};
			} else {
				parameterTypes = new Class<?>[]{
					String.class,
					String.class,
					String.class,
					String.class,
					String.class,
					String.class,
					String.class,
					String.class,
					boolean.class,
					boolean.class,
					Properties.class,
					AtomicReference.class
				};
				arguments = new Object[]{
					scriptPath, logFilePath, trimLogFilePath, defaultExcludeFilePath, defaultTrimExcludeFilePath, defaultMethodParameterChangesExcludeFilePath, defaultMethodParameterObfuscationExcludeFilePath, defaultDirectory, verbose ? Boolean.TRUE : Boolean.FALSE, parseScriptOnly ? Boolean.TRUE : Boolean.FALSE, properties, logPrinterReference
				};
			}

			Class.forName(GlobalUtils.getKlassMasterClassName()).getConstructor(parameterTypes).newInstance(arguments);
		} catch (InstantiationException ignoredInstantiation) {
			throw new Exception("ZKM: Unexpected Error (A). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
		} catch (IllegalAccessException ignoredIllegalAccess) {
			throw new Exception("ZKM: Unexpected Error (B). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
		} catch (NoClassDefFoundError ignoredNoClassDefFound) {
			String classpath = System.getProperty("java.class.path");
			System.err.println("ZKM: Classpath is: \"" + classpath + "\"");
			if (classpath != null) {
				if (classpath.indexOf("ZKM.jar") == -1) {
					throw new Exception("ZKM: Startup Error. (Check that ZKM.jar is in classpath) (1)");
				}

				if (isZKMJarInClasspath(classpath)) {
					throw new Exception("ZKM: Unexpected Error (C). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
				}

				throw new Exception("ZKM: Startup Error. (ZKM.jar not found where specified by classpath)");
			}

			throw new Exception("ZKM: Startup Error. (Check that ZKM.jar is in classpath) (2)");
		} catch (InvocationTargetException invocationTargetException) {
			Throwable targetException = invocationTargetException.getTargetException();
			if (targetException instanceof EvaluationExpiredException) {
				throw new Exception("ZKM Evaluation has expired: " + targetException.getMessage());
			}

			if (targetException instanceof LookupException) {
				throw new Exception("ZKM: " + targetException.getMessage());
			}

			getStackTrace(targetException, logPrinterReference.get());
			throw new Exception("ZKM: Unexpected Error (D). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
		} catch (NoSuchMethodException ignoredNoSuchMethod) {
			throw new Exception("ZKM: Unexpected Error (E). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
		} catch (Throwable throwable) {
			getStackTrace(throwable, logPrinterReference.get());
			throw new Exception("ZKM: Unexpected Error (F). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
		} finally {
			if (USED_ERR_STREAM != null && USED_ERR_STREAM != System.err) {
				System.setErr(USED_ERR_STREAM);
			}

			if (USED_OUT_STREAM != null && USED_OUT_STREAM != System.out) {
				System.setOut(USED_OUT_STREAM);
			}
		}
	}

	public static void executeWithHashtableProperties(
		String scriptPath, String logFilePath, String trimLogFilePath, String defaultExcludeFilePath, String defaultTrimExcludeFilePath, String defaultMethodParameterChangesExcludeFilePath, String defaultMethodParameterObfuscationExcludeFilePath, String defaultDirectory, boolean verbose, boolean parseScriptOnly, Hashtable<?, ?> properties
	) throws Exception {
		Properties convertedProperties = convertMapToProperties(properties);
		executeStandard(scriptPath, logFilePath, trimLogFilePath, defaultExcludeFilePath, defaultTrimExcludeFilePath, defaultMethodParameterChangesExcludeFilePath, defaultMethodParameterObfuscationExcludeFilePath, defaultDirectory, verbose, parseScriptOnly, convertedProperties);
	}

	public static void executeWithAllParams(
		String scriptPath,
		String logFilePath,
		String trimLogFilePath,
		String defaultExcludeFilePath,
		String defaultTrimExcludeFilePath,
		String defaultMethodParameterChangesExcludeFilePath,
		String defaultMethodParameterObfuscationExcludeFilePath,
		String defaultDirectory,
		String stdoutRedirectFileName,
		String stderrRedirectFileName,
		boolean verbose,
		boolean parseScriptOnly,
		Properties properties
	) throws Exception {
		startKlassMaster(scriptPath, logFilePath, trimLogFilePath, defaultExcludeFilePath, defaultTrimExcludeFilePath, defaultMethodParameterChangesExcludeFilePath, defaultMethodParameterObfuscationExcludeFilePath, defaultDirectory, stdoutRedirectFileName, stderrRedirectFileName, verbose, parseScriptOnly, properties, false);
	}

	public static void executeProguardStyle(String proguardConfigPath, Map<?, ?> properties) throws Exception {
		Properties convertedProperties = convertMapToProperties(properties);
		startKlassMaster(
			proguardConfigPath, (String)null, (String)null, (String)null, (String)null, (String)null, (String)null, (String)null, (String)null, (String)null, true, false, convertedProperties, true
		);
	}

	public static void executeStandard(
		String scriptPath, String logFilePath, String trimLogFilePath, String defaultExcludeFilePath, String defaultTrimExcludeFilePath, String defaultMethodParameterChangesExcludeFilePath, String defaultMethodParameterObfuscationExcludeFilePath, String defaultDirectory, boolean verbose, boolean parseScriptOnly, Properties properties
	) throws Exception {
		executeWithAllParams(scriptPath, logFilePath, trimLogFilePath, defaultExcludeFilePath, defaultTrimExcludeFilePath, defaultMethodParameterChangesExcludeFilePath, defaultMethodParameterObfuscationExcludeFilePath, defaultDirectory, (String)null, (String)null, verbose, parseScriptOnly, properties);
	}

	public static void processMIDlet(File jadFile, String inputJarFileName, File outputJarFile, String classpath, String projectDirectory) {
		if (outputJarFile == null) {
			throw new IllegalArgumentException("ZKM: Null output JAR file.");
		} else if (inputJarFileName == null || inputJarFileName.length() <= 4) {
			throw new IllegalArgumentException("ZKM: Invalid input JAR file name: '" + inputJarFileName + "'");
		} else if (projectDirectory == null || projectDirectory.length() < 1) {
			throw new IllegalArgumentException("ZKM: Invalid project directory name: '" + projectDirectory + "'");
		} else if (classpath != null && classpath.length() != 0) {
			PropertyUtils.loadSystemProperties();
			AtomicReference<PrintWriter> logPrinterReference = new AtomicReference<>();

			try {
				Class<?>[] parameterTypes = new Class<?>[]{File.class, String.class, File.class, String.class, String.class, boolean.class, AtomicReference.class};
				Constructor<?> constructor = Class.forName(GlobalUtils.getKlassMasterClassName()).getConstructor(parameterTypes);
				Object[] arguments = new Object[]{jadFile, inputJarFileName, outputJarFile, classpath, projectDirectory, Boolean.TRUE, logPrinterReference};
				constructor.newInstance(arguments);
			} catch (InstantiationException ignoredInstantiation) {
				System.err.println("ZKM: Unexpected Error (A). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
			} catch (IllegalAccessException ignoredIllegalAccess) {
				System.err.println("ZKM: Unexpected Error (B). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
			} catch (NoClassDefFoundError ignoredNoClassDefFound) {
				System.err.println("ZKM: Classpath is: \"" + classpath + "\"");
				System.err.println("ZKM: Unexpected Error (C). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
			} catch (InvocationTargetException invocationTargetException) {
				Throwable targetException = invocationTargetException.getTargetException();
				if (targetException instanceof EvaluationExpiredException) {
					System.err.println("ERROR: ZKM Evaluation has expired: " + targetException.getMessage());
				} else if (targetException instanceof LookupException) {
					System.err.println("ZKM: " + targetException.getMessage());
				} else {
					getStackTrace(targetException, logPrinterReference.get());
					System.err.println("ZKM: Unexpected Error (D). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
				}
			} catch (NoSuchMethodException ignoredNoSuchMethod) {
				System.err.println("ZKM: Unexpected Error (E). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
			} catch (Throwable throwable) {
				getStackTrace(throwable, logPrinterReference.get());
				System.err.println("ZKM: Unexpected Error (F). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
			}
		} else {
			throw new IllegalArgumentException("ZKM: Invalid classpath: '" + classpath + "'");
		}
	}

	public static Properties convertMapToProperties(Map<?, ?> map) {
		Properties properties = null;
		if (map != null) {
			properties = new Properties();
			properties.putAll(map);
		}

		return properties;
	}
}

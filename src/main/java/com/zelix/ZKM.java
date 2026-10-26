package com.zelix;

import com.zelix.exceptions.EvaluationExpiredException;
import com.zelix.exceptions.ZKMUnexpectedException;
import com.zelix.klassmaster.AbstractBootstrap;
import com.zelix.klassmaster.Bootstrap;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.utils.EnvironmentUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PropertyUtils;
import com.zelix.utils.references.AtomicReference;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ZKM extends AbstractBootstrap {
	public static void main(String[] args) {
		PropertyUtils.loadSystemProperties();
		if (Options.ZKM_DEBUG_REDIRECT_OUT) {
			PrintStream redirectedOutputStream = null;

			try {
				File redirectedOutFile = new File(FileUtils.USER_DIRECTORY, "RedirectedOut_" + System.currentTimeMillis() + ".txt");
				System.out.println("REDIRECTING System.out to '" + redirectedOutFile.getAbsolutePath() + "'" + '\u0007');
				redirectedOutputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(redirectedOutFile), 4096), true);
				AbstractBootstrap.USED_OUT_STREAM = System.out;
				System.setOut(redirectedOutputStream);
			} catch (IOException ioException) {
				System.setOut(AbstractBootstrap.USED_OUT_STREAM);
				ioException.printStackTrace();
				System.exit(1);
			} finally {
				if (redirectedOutputStream != null) {
					redirectedOutputStream.close();
				}
			}
		}

		if (Options.ZKM_TEST_JVM_VERSION && EnvironmentUtils.getCurrentClassVersion() < 52) {
			System.err.println(Bootstrap.lineSeparator + Bootstrap.requiresJava8 + Bootstrap.lineSeparator);
			System.exit(1);
		}

		try {
			Class.forName("java.security.MessageDigest");
		} catch (ClassNotFoundException ignored) {
			System.err.println(Bootstrap.lineSeparator + Bootstrap.noDigest + Bootstrap.lineSeparator);
			System.exit(1);
		}

		AtomicReference<PrintWriter> printWriterReference = new AtomicReference<>();

		try {
			Class<?>[] constructorParameterTypes = new Class[]{String[].class, AtomicReference.class};
			Constructor<?> constructor = Class.forName(GlobalUtils.getKlassMasterClassName()).getConstructor(constructorParameterTypes);
			Object[] constructorArguments = new Object[]{args, printWriterReference};
			constructor.newInstance(constructorArguments);
		} catch (InstantiationException instantiationException) {
			System.err.println(Bootstrap.lineSeparator + "Unexpected Error (A). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
			System.exit(1);
		} catch (IllegalAccessException illegalAccessException) {
			System.err.println(Bootstrap.lineSeparator + "Unexpected Error (B). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
			System.exit(1);
		} catch (NoClassDefFoundError noClassDefFoundError) {
			String classpath = System.getProperty("java.class.path");
			if (classpath != null) {
				if (classpath.indexOf("ZKM.jar") == -1) {
					System.err.println("Startup Error. (Check that ZKM.jar is in classpath) (1)");
				} else if (AbstractBootstrap.isZKMJarInClasspath(classpath)) {
					System.err.println(Bootstrap.lineSeparator + "Unexpected Error (C). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
				} else {
					System.err.println("Startup Error. (ZKM.jar not found where specified by classpath)");
				}
			} else {
				System.err.println("Startup Error. (Check that ZKM.jar is in classpath) (2)");
			}

			System.err.println("Classpath is: \"" + classpath + "\"");
			System.exit(1);
		} catch (InvocationTargetException invocationTargetException) {
			Throwable targetException = invocationTargetException.getTargetException();
			if (targetException instanceof EvaluationExpiredException) {
				System.err.println(Bootstrap.lineSeparator + "Evaluation has expired : " + targetException.getMessage() + Bootstrap.lineSeparator);
			} else if (targetException instanceof ZKMUnexpectedException) {
				PrintWriter printWriter = printWriterReference.get();
				System.err.println(Bootstrap.lineSeparator + "Unexpected Error (D). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
				System.err.println(Bootstrap.lineSeparator + targetException.getMessage() + Bootstrap.lineSeparator);
				printWriter.println(Bootstrap.lineSeparator + "Unexpected Error (D). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
				printWriter.println(Bootstrap.lineSeparator + targetException.getMessage() + Bootstrap.lineSeparator);
				Throwable cause = targetException.getCause();
				if (cause != null) {
					cause.printStackTrace(printWriter);
				}
			} else {
				System.err.println(Bootstrap.lineSeparator + "Unexpected Error (D). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
				AbstractBootstrap.getStackTrace(targetException, printWriterReference.get());
			}

			System.exit(1);
		} catch (NoSuchMethodException noSuchMethodException) {
			System.err.println(Bootstrap.lineSeparator + "Unexpected Error (E). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
			System.exit(1);
		} catch (Throwable throwable) {
			System.err.println(Bootstrap.lineSeparator + "Unexpected Error (G). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
			AbstractBootstrap.getStackTrace(throwable, printWriterReference.get());
			System.exit(1);
		}
	}
}

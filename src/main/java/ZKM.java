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
import java.nio.file.Files;

public class ZKM extends AbstractBootstrap {
	public static void main(String[] args) {
		PropertyUtils.loadSystemProperties();
		if (Options.ZKM_DEBUG_REDIRECT_OUT) {
			PrintStream stream = null;

			try {
				File file = new File(FileUtils.USER_DIRECTORY, "RedirectedOut_" + System.currentTimeMillis() + ".txt");
				System.out.println("REDIRECTING System.out to '" + file.getAbsolutePath() + "'" + '\u0007');
				stream = new PrintStream(new BufferedOutputStream(Files.newOutputStream(file.toPath()), 4096), true);
				AbstractBootstrap.USED_OUT_STREAM = System.out;
				System.setOut(stream);
			} catch (IOException ioException) {
				System.setOut(AbstractBootstrap.USED_OUT_STREAM);
				ioException.printStackTrace();
				System.exit(1);
			}
		}

		if (Options.ZKM_TEST_JVM_VERSION && EnvironmentUtils.getCurrentClassVersion() < 52) {
			System.err.println(Bootstrap.lineSeparator + Bootstrap.requiresJava8 + Bootstrap.lineSeparator);
			System.exit(1);
		}

		try {
			Class.forName("java.security.MessageDigest");
		} catch (ClassNotFoundException ignoredMessageDigestLookup) {
			System.err.println(Bootstrap.lineSeparator + Bootstrap.noDigest + Bootstrap.lineSeparator);
			System.exit(1);
		}

		AtomicReference<PrintWriter> logPrinterRef = new AtomicReference<>();

		try {
			Class<?>[] constructorParameterTypes = new Class[]{String[].class, AtomicReference.class};
			Constructor<?> klassMasterConstructor = Class.forName(GlobalUtils.getKlassMasterClassName()).getConstructor(constructorParameterTypes);
			Object[] constructorArguments = new Object[]{args, logPrinterRef};
			klassMasterConstructor.newInstance(constructorArguments);
		} catch (InstantiationException ignoredInstantiation) {
			System.err.println(Bootstrap.lineSeparator + "Unexpected Error (A). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
			System.exit(1);
		} catch (IllegalAccessException ignoredIllegalAccess) {
			System.err.println(Bootstrap.lineSeparator + "Unexpected Error (B). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
			System.exit(1);
		} catch (NoClassDefFoundError ignoredNoClassDefFound) {
			String classPath = System.getProperty("java.class.path");
			if (classPath != null) {
				if (!classPath.contains("ZKM.jar")) {
					System.err.println("Startup Error. (Check that ZKM.jar is in classpath) (1)");
				} else if (AbstractBootstrap.isZKMJarInClasspath(classPath)) {
					System.err.println(Bootstrap.lineSeparator + "Unexpected Error (C). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
				} else {
					System.err.println("Startup Error. (ZKM.jar not found where specified by classpath)");
				}
			} else {
				System.err.println("Startup Error. (Check that ZKM.jar is in classpath) (2)");
			}

			System.err.println("Classpath is: \"" + classPath + "\"");
			System.exit(1);
		} catch (InvocationTargetException invocationTargetException) {
			Throwable targetException = invocationTargetException.getTargetException();
			if (targetException instanceof EvaluationExpiredException) {
				System.err.println(Bootstrap.lineSeparator + "Evaluation has expired : " + targetException.getMessage() + Bootstrap.lineSeparator);
			} else if (targetException instanceof ZKMUnexpectedException) {
				PrintWriter logPrinter = logPrinterRef.get();
				System.err.println(Bootstrap.lineSeparator + "Unexpected Error (D). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
				System.err.println(Bootstrap.lineSeparator + targetException.getMessage() + Bootstrap.lineSeparator);
				logPrinter.println(Bootstrap.lineSeparator + "Unexpected Error (D). Please report the problem to bugs2" + CopyrightConstants.EMAIL);
				logPrinter.println(Bootstrap.lineSeparator + targetException.getMessage() + Bootstrap.lineSeparator);
				Throwable cause = targetException.getCause();
				if (cause != null) {
					cause.printStackTrace(logPrinter);
				}
			} else {
				System.err.println(Bootstrap.lineSeparator + "Unexpected Error (D). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
				AbstractBootstrap.getStackTrace(targetException, logPrinterRef.get());
			}

			System.exit(1);
		} catch (NoSuchMethodException ignoredNoSuchMethod) {
			System.err.println(Bootstrap.lineSeparator + "Unexpected Error (E). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
			System.exit(1);
		} catch (Throwable startupFailure) {
			System.err.println(Bootstrap.lineSeparator + "Unexpected Error (G). Please report the problem to bugs2" + CopyrightConstants.EMAIL + Bootstrap.lineSeparator);
			AbstractBootstrap.getStackTrace(startupFailure, logPrinterRef.get());
			System.exit(1);
		}
	}
}

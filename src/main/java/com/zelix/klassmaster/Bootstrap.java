package com.zelix.klassmaster;

import com.zelix.klassmaster.constant.CopyrightConstants;

public abstract class Bootstrap {
	public static final String lineSeparator = System.getProperty("line.separator", "\n");
	public static final String pathSeparator = System.getProperty("path.separator");
	public static final String requiresJava8 = "ERROR: " + CopyrightConstants.NAME + " " + CopyrightConstants.VERSION + " can process earlier bytecode but it requires a Java 8 (JDK 1.8) or better JVM to run. Your JVM is version '"
		+ System.getProperty("java.vm.version")
		+ "'.";
	public static final String noDigest = "ERROR: " + CopyrightConstants.NAME + " " + CopyrightConstants.VERSION + " requires java.security.MessageDigest to be in its runtime classpath. : \""
		+ System.getProperty("java.class.path")
		+ "\"";
}

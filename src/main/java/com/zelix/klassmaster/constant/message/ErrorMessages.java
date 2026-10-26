package com.zelix.klassmaster.constant.message;

import com.zelix.klassmaster.constant.CopyrightConstants;

public class ErrorMessages {
	public static final String lineSeparator = System.getProperty("line.separator", "\n");
	public static final String pathSeparator = System.getProperty("path.separator");
	public static final String requireJava8 = "ERROR: " + CopyrightConstants.NAME + " " + CopyrightConstants.VERSION + " can process earlier bytecode but it requires a Java 8 (JDK 1.8) or better JVM to run. Your JVM is version '"
		+ System.getProperty("java.vm.version")
		+ "'.";
}

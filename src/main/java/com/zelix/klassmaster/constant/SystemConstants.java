package com.zelix.klassmaster.constant;

import java.io.File;

public class SystemConstants {
	public static final String userDirectory = System.getProperty("user.dir");
	public static final String fileSeparator = System.getProperty("file.separator");
	public static final char fileSep = fileSeparator.charAt(0);
	public static final String pathSeparator = System.getProperty("path.separator");
	public static final char pathSep = pathSeparator.charAt(0);
	public static final String lineSeparator = System.getProperty("line.separator", "\n");
	public static final File userDirectoryFile = new File(userDirectory);
	public static final String javaVendor = System.getProperty("java.vm.vendor", "Sun");
	public static final String javaVMVersion = System.getProperty("java.vm.version");
	public static final String javaHome = System.getProperty("java.home", userDirectory);
	public static final String bootClasspath = System.getProperty("sun.boot.class.path");
	public static final String osName = System.getProperty("os.name");
	public static final String osVersion = System.getProperty("os.version");
}

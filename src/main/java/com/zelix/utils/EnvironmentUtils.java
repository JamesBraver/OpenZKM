package com.zelix.utils;

public class EnvironmentUtils {
	private static final String javaClassVersionString = System.getProperty("java.class.version");
	private static final int javaClassVersion = Integer.parseInt(javaClassVersionString.split("\\.")[0]);

	public static int getCurrentClassVersion() {
		return javaClassVersion;
	}

	public static boolean isJava1_4OrNewer(int classVersion) {
		return classVersion >= 48;
	}

	public static int getClassVersionForRuntime(int javaVersion) {
		if (javaVersion < 1) {
			throw new IllegalArgumentException("Invalid Java runtime version '" + javaVersion + "'");
		} else {
			switch (javaVersion) {
				case 1:
					return 45;
				case 2:
					return 46;
				case 3:
					return 47;
				case 4:
					return 48;
				case 5:
					return 49;
				case 6:
					return 50;
				case 7:
					return 51;
				case 8:
					return 52;
				case 9:
					return 53;
				case 10:
					return 54;
				case 11:
					return 55;
				case 12:
					return 56;
				case 13:
					return 57;
				case 14:
					return 58;
				case 15:
					return 59;
				case 16:
					return 60;
				case 17:
					return 61;
				case 18:
					return 62;
				case 19:
					return 63;
				case 20:
					return 64;
				case 21:
					return 65;
				case 22:
					return 66;
				case 23:
					return 67;
				case 24:
					return 68;
				case 25:
					return 69;
				case 26:
					return 70;
				case 27:
					return 71;
				default:
					return 70;
			}
		}
	}

	public static boolean isJava9OrNewer(int classVersion) {
		return classVersion >= 53;
	}

	public static boolean isRuntimeJava19OrNewer() {
		return javaClassVersion >= 63;
	}

	public static boolean isJava5OrNewer(int classVersion) {
		return classVersion >= 49;
	}

	public static boolean isJava19OrNewer(int classVersion) {
		return classVersion >= 63;
	}

	public static boolean isRuntimeJava8OrNewer() {
		return javaClassVersion >= 52;
	}

	public static boolean isJava8OrNewer(int classVersion) {
		return classVersion >= 52;
	}

	public static boolean isJava6OrNewer(int classVersion) {
		return classVersion >= 50;
	}

	public static boolean isRuntimeJava9OrNewer() {
		return javaClassVersion >= 53;
	}

	public static boolean isJava7OrNewer(int classVersion) {
		return classVersion >= 51;
	}
}

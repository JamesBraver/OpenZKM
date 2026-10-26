package com.zelix.klassmaster.constant;

public class LicenseCheckConstant {
	private static String scrambledLicenseNameHash = AntiStringerConstants.LICENSE_NAME_HASHCODE_SCRAMBLED;

	public static String getMethodNameXorKey() {
		return "PMGwlFcwZ0FwGAmpggn0hD";
	}

	public static String getScrambledLicenseNameHash() {
		return scrambledLicenseNameHash;
	}

	public static String getScrambledMethodName() {
		return " ,5\u0004\t\n\f\u0019=";
	}

	public long unscrambleNumber(String scrambledNumber) {
		return 8964L;
	}
}

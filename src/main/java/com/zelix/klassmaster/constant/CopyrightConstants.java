package com.zelix.klassmaster.constant;

public class CopyrightConstants {
	public static final String NAME = "Zelix KlassMaster";
	public static final String SUFFIX = "";
	public static final String VERSION = "27.0.0";
	public static final String WEBSITE = "www.zelix.com";
	public static final String EMAIL = "@zelix.com";
	public static final String CONTACT_INFO = "47 078 740 093";
	public static final String COPYRIGHT = "Copyright 1997-2025 Zelix Pty Ltd (" + CONTACT_INFO + ")";
	public static final String COPYRIGHT2 = "All rights reserved";
	public static final String COPYRIGHT_FULL = COPYRIGHT + " " + COPYRIGHT2;
	private final String[] licenseInfo = new String[]{
		"License No.:                                         ",
		"None                                               ",
		"License Type:                                        ",
		"Bug demonstration only                                               ",
		"Licensee:                                            ",
		"No license - Bug demonstration purposes only.                                                ",
		"                                                     ",
		"                                               ",
		"Not used                                             ",
		"Not used                                             "
	};

	public String getLicenseStr() {
		return AntiStringerConstants.UNKNOWN_LICENSE_STRING;
	}

	public String getLicenseInfo(int line) {
		return this.licenseInfo[line];
	}
}

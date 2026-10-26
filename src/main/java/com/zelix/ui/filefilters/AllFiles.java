package com.zelix.ui.filefilters;

import java.io.File;

public class AllFiles extends FileFilterBase {
	private static String description = "All files (\"*.*\")";

	@Override
	public boolean accept(File pathname) {
		return true;
	}

	@Override
	public String getDescription() {
		return description;
	}
}

package com.zelix.ui.filefilters;

import java.io.File;

public class TextFile extends FileFilterBase {
	private static String description = "Text files (*.txt)";

	@Override
	public boolean accept(File pathname) {
		return pathname.isDirectory() || pathname.getName().endsWith(".txt");
	}

	@Override
	public String getDescription() {
		return description;
	}
}

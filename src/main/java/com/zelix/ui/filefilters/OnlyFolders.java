package com.zelix.ui.filefilters;

import java.io.File;

public class OnlyFolders extends FileFilterBase {
	private static String description = "Only Folders";

	@Override
	public boolean accept(File pathname) {
		return pathname.isDirectory();
	}

	@Override
	public String getDescription() {
		return description;
	}
}

package com.zelix.ui.filefilters;

import com.zelix.utils.FileUtils;
import java.io.File;

public class FoldersAndArchives extends FileFilterBase {
	private static String description = "Folders and Java archive files";

	@Override
	public boolean accept(File pathname) {
		return pathname.isDirectory() ? true : FileUtils.isArchiveExtension(pathname.getName());
	}

	@Override
	public String getDescription() {
		return description;
	}
}

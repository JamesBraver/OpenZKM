package com.zelix.ui.filefilters;

import com.zelix.utils.FileUtils;
import java.io.File;

public class ClassesAndDescriptor extends FileFilterBase {
	private static String description = "Folders, archives, 'jad' & 'xml' files";

	@Override
	public boolean accept(File pathname) {
		String lowerCaseName = pathname.getName().toLowerCase();
		if (pathname.isDirectory()) {
			return true;
		} else if (FileUtils.isArchiveExtension(pathname.getName())) {
			return true;
		} else {
			return lowerCaseName.endsWith(".jad") ? true : FileUtils.isXmlFile(lowerCaseName);
		}
	}

	@Override
	public String getDescription() {
		return description;
	}
}

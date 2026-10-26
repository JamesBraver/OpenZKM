package com.zelix.ui.filefilters;

import java.io.File;
import java.io.FileFilter;

public abstract class FileFilterBase implements FileFilter {
	@Override
	public abstract boolean accept(File pathname);

	@Override
	public String toString() {
		return this.getDescription();
	}

	public abstract String getDescription();
}

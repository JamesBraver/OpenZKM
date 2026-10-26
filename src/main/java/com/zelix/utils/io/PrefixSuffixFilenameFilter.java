package com.zelix.utils.io;

import java.io.File;
import java.io.FilenameFilter;

public class PrefixSuffixFilenameFilter implements FilenameFilter {
	private String prefix;
	private String suffix;

	public PrefixSuffixFilenameFilter(String prefix, String suffix) {
		this.prefix = prefix;
		this.suffix = suffix;
	}

	@Override
	public boolean accept(File dir, String name) {
		return new File(dir, name).isDirectory() ? false : name.startsWith(this.prefix) && name.endsWith(this.suffix);
	}
}

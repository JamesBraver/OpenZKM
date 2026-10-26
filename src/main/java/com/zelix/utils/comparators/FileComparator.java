package com.zelix.utils.comparators;

import java.io.File;
import java.util.Comparator;

public class FileComparator implements Comparator<File> {
	private static final FileComparator INSTANCE = new FileComparator();

	private FileComparator() {
	}

	public static FileComparator getInstance() {
		return INSTANCE;
	}

	public int compareFiles(File file1, File file2) {
		String file1Name = file1.getName().toLowerCase();
		String file2Name = file2.getName().toLowerCase();
		if (file1.isDirectory()) {
			return !file2.isDirectory() ? -1 : file1Name.compareTo(file2Name);
		} else {
			return file2.isDirectory() ? 1 : file1Name.compareTo(file2Name);
		}
	}

	@Override
	public int compare(File o1, File o2) {
		return this.compareFiles(o1, o2);
	}
}

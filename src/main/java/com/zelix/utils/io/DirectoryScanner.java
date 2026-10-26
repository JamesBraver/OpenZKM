package com.zelix.utils.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirectoryScanner {
	protected File rootDirectory;

	public DirectoryScanner(File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public static void ensureDirectory(String directoryPath) throws IOException {
		if (directoryPath != null) {
			File directoryFile = new File(directoryPath);
			if (!directoryFile.exists() && !directoryFile.mkdirs()) {
				throw new IOException("Couldn't create directory '" + directoryPath + "'");
			}
		}
	}

	protected void collectSubdirectories(File directory, List<String> subdirectories) {
		String[] subdirectoryNames = directory.list(new DirectoryFilter());
		int subdirectoryCount = subdirectoryNames != null ? subdirectoryNames.length : 0;

		for (int i = 0; i < subdirectoryCount; i++) {
			File childDirectory = new File(directory, subdirectoryNames[i]);
			subdirectories.add(childDirectory.getAbsolutePath());
			this.collectSubdirectories(childDirectory, subdirectories);
		}
	}

	public List<String> getSubdirectories() {
		ArrayList<String> subdirectories = new ArrayList<>();
		this.collectSubdirectories(this.rootDirectory, subdirectories);
		return subdirectories;
	}
}

package com.zelix.utils;

import com.zelix.utils.io.PrefixSuffixFilenameFilter;
import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Map;

public class TempFileManager {
	public static final String TEMP_DIRECTORY_PATH = System.getProperty("java.io.tmpdir", System.getProperty("user.dir"));
	private static File TEMP_DIRECTORY = new File(TEMP_DIRECTORY_PATH);
	private static Map<String, String> tempFileToOriginalNameMap = GlobalUtils.newHashMap();

	public static String getOriginalName(String tempFilePath) {
		return tempFileToOriginalNameMap.get(tempFilePath);
	}

	public static File createTempFile(String originalName) throws IOException {
		File tempFile = File.createTempFile("ZKM", ".tmp", TEMP_DIRECTORY);
		tempFile.deleteOnExit();
		if (originalName != null) {
			tempFileToOriginalNameMap.put(tempFile.getAbsolutePath(), originalName);
		}

		return tempFile;
	}

	public static boolean isManagedTempFile(File file) {
		String fileName = file.getName();
		String parentPath = file.getParent();
		return fileName.startsWith("ZKM") && fileName.endsWith(".tmp") && parentPath != null && new File(parentPath).equals(new File(TEMP_DIRECTORY_PATH));
	}

	public static String findTempFileByOriginalName(String originalName) {
		for (Entry<String, String> entry : tempFileToOriginalNameMap.entrySet()) {
			if (entry.getValue().equals(originalName)) {
				return entry.getKey();
			}
		}

		return null;
	}

	public static void cleanupOldTempFiles() {
		try {
			long nowMillis = System.currentTimeMillis();
			String[] tempFileNames = TEMP_DIRECTORY.list(new PrefixSuffixFilenameFilter("ZKM", ".tmp"));
			if (tempFileNames != null) {
				for (int i = 0; i < tempFileNames.length; i++) {
					File tempFile = new File(TEMP_DIRECTORY, tempFileNames[i]);
					long lastModified = tempFile.lastModified();
					if (nowMillis - lastModified > 172800000L) {
						tempFile.delete();
					}
				}
			}
		} catch (Throwable exception) {
			exception.printStackTrace();
		}
	}

	public static boolean isManagedTempFile(String filePath) {
		return isManagedTempFile(new File(filePath));
	}

	public static File createTempFile() throws IOException {
		return createTempFile(null);
	}

	public static String resolveTempPathInString(String text) {
		if (tempFileToOriginalNameMap.size() != 0 && text.indexOf(".tmp") != -1) {
			for (Entry<String, String> entry : tempFileToOriginalNameMap.entrySet()) {
				String tempFilePath = entry.getKey();
				String originalName = entry.getValue();
				int tempPathIndex = text.indexOf(tempFilePath);
				if (tempPathIndex > -1) {
					StringBuilder builder = new StringBuilder();
					builder.append(text.substring(0, tempPathIndex));
					builder.append(originalName);
					builder.append(text.substring(tempPathIndex + tempFilePath.length()));
					return builder.toString();
				}
			}

			return text;
		} else {
			return text;
		}
	}
}

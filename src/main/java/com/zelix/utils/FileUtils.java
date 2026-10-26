package com.zelix.utils;

import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.proguardConfigurationTranslate.Stripper;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtils {
	public static final String USER_DIRECTORY_PATH = System.getProperty("user.dir");
	public static final File USER_DIRECTORY = new File(USER_DIRECTORY_PATH);
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final char FILE_SEPARATOR_CHAR = FILE_SEPARATOR.charAt(0);
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
	public static final char PATH_SEPARATOR_CHAR = PATH_SEPARATOR.charAt(0);
	public static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");
	public static final String DEFAULT_ENCODING = System.getProperty("file.encoding", "UTF-8");
	public static boolean IS_UNIX = true;
	public static final String OS_NAME = System.getProperty("os.name");
	public static int BOM_BUFFER_SIZE = 4;
	private static List<String> EXTRA_XML_FILE_EXTENSIONS;

	private static boolean isZipStream(InputStream inputStream) throws IOException {
		if (inputStream.available() < 4) {
			return false;
		} else {
			byte[] headerBytes = readBytes(inputStream, 4);
			return (headerBytes[0] & 255) == 80 && (headerBytes[1] & 255) == 75 && (headerBytes[2] & 255) == 3 && (headerBytes[3] & 255) == 4;
		}
	}

	public static BufferedReader createBufferedReader(File file, AtomicReference<String> encodingRef) throws IOException {
		return createReaderWithBomDetection(new FileInputStream(file), (String)null, encodingRef);
	}

	public static BufferedReader createBufferedReader(InputStream inputStream) throws IOException {
		return createReaderWithBomDetection(inputStream, (String)null, null);
	}

	public static String splitPath(String path, AtomicReference<String> parentPathRef, AtomicReference<String> fileNameRef) {
		int lastSeparatorIndex;
		if ((lastSeparatorIndex = path.lastIndexOf(FILE_SEPARATOR)) <= -1 && (lastSeparatorIndex = path.lastIndexOf("\\")) <= -1 && (lastSeparatorIndex = path.lastIndexOf("/")) <= -1) {
			parentPathRef.set("");
			fileNameRef.set(path);
			return "";
		} else {
			parentPathRef.set(path.substring(0, lastSeparatorIndex));
			fileNameRef.set(path.substring(lastSeparatorIndex + 1));
			return path.substring(lastSeparatorIndex, lastSeparatorIndex + 1);
		}
	}

	public static File writeToTempFile(String content, AtomicReference<String> errMessageRef) {
		try {
			return writeStringToFile(content, TempFileManager.createTempFile(), errMessageRef);
		} catch (IOException ioException) {
			errMessageRef.set(ioException.getMessage());
			return null;
		}
	}

	public static boolean isXmlFile(String fileName) {
		int lastDotIndex = fileName.lastIndexOf(46);
		if (lastDotIndex > -1) {
			String extension = fileName.substring(lastDotIndex, fileName.length());
			if (extension.equalsIgnoreCase(".xml") || extension.equalsIgnoreCase(".tld") || extension.equalsIgnoreCase(".fxml") || extension.equalsIgnoreCase(".e4xmi")) {
				return true;
			}
		}

		if (EXTRA_XML_FILE_EXTENSIONS != null) {
			for (String extraExtension : EXTRA_XML_FILE_EXTENSIONS) {
				if (extraExtension.indexOf("*") > -1 && StringUtils.matchesWildcard(fileName, extraExtension)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isRelativePath(String path) {
		if (path != null && path.trim().length() != 0) {
			path = path.trim();
			return path.charAt(0) == File.separatorChar ? false : path.length() <= 1 || !Character.isLetter(path.charAt(0)) || path.charAt(1) != ':';
		} else {
			return false;
		}
	}

	public static boolean isNestedArchive(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
		return isArchiveExtensionLenient(zipEntry.getName()) && isZipEntryContentZip(zipFile, zipEntry);
	}

	public static String getRelativePath(String fromPath, String toPath) {
		if (fromPath.equals(toPath)) {
			return ".";
		} else {
			char separatorChar = '/';
			int separatorIndex;
			if (((separatorIndex = fromPath.lastIndexOf(FILE_SEPARATOR)) > -1 || (separatorIndex = fromPath.lastIndexOf("\\")) > -1 || (separatorIndex = fromPath.lastIndexOf("/")) > -1) && separatorIndex > -1) {
				separatorChar = fromPath.charAt(separatorIndex);
			}

			if (separatorChar == 0
				&& ((separatorIndex = toPath.lastIndexOf(FILE_SEPARATOR)) > -1 || (separatorIndex = toPath.lastIndexOf("\\")) > -1 || (separatorIndex = toPath.lastIndexOf("/")) > -1)
				&& separatorIndex > -1) {
				separatorChar = toPath.charAt(separatorIndex);
			}

			if (fromPath.trim().length() == 0) {
				return toPath.charAt(0) == separatorChar ? toPath.substring(1) : toPath;
			} else {
				String separatorString = String.valueOf(separatorChar);
				StringTokenizer fromPathTokens = new StringTokenizer(fromPath, separatorString, true);
				StringTokenizer toPathTokens = new StringTokenizer(toPath, separatorString, true);
				int fromTokenCount = fromPathTokens.countTokens();
				int toTokenCount = toPathTokens.countTokens();
				StringBuilder commonPrefixBuilder = new StringBuilder();

				for (int commonTokenIndex = 0; commonTokenIndex < fromTokenCount && commonTokenIndex < toTokenCount; commonTokenIndex++) {
					String fromToken = fromPathTokens.nextToken();
					String toToken = toPathTokens.nextToken();
					if (!fromToken.equals(toToken)) {
						break;
					}

					commonPrefixBuilder.append(fromToken);
				}

				String commonPrefix = commonPrefixBuilder.toString();
				if (commonPrefix.length() == 0) {
					int fromLevelCount = new StringTokenizer(fromPath, separatorString).countTokens();
					StringBuilder upPathBuilder = new StringBuilder();

					for (int upLevelIndex = 0; upLevelIndex < fromLevelCount; upLevelIndex++) {
						upPathBuilder.append("..");
						if (upLevelIndex < fromLevelCount - 1) {
							upPathBuilder.append(separatorString);
						}
					}

					if (!toPath.startsWith(separatorString)) {
						upPathBuilder.append(separatorString);
					}

					upPathBuilder.append(toPath);
					return upPathBuilder.toString();
				} else {
					int commonPrefixTokenCount = new StringTokenizer(commonPrefix, separatorString, true).countTokens();
					String remainingPath = toPath.substring(commonPrefix.length());
					StringBuilder relativePathBuilder = new StringBuilder();
					if (fromTokenCount == commonPrefixTokenCount) {
						relativePathBuilder.append(".");
					} else {
						int upLevelCount = fromTokenCount - commonPrefixTokenCount;
						relativePathBuilder.append("..");
						upLevelCount--;

						for (int parentLevelIndex = 0; parentLevelIndex < upLevelCount / 2; parentLevelIndex++) {
							relativePathBuilder.append(separatorString);
							relativePathBuilder.append("..");
						}
					}

					if (remainingPath.length() > 0) {
						if (relativePathBuilder.length() == 1 && relativePathBuilder.toString().equals(".")) {
							relativePathBuilder.setLength(0);
							if (remainingPath.charAt(0) == separatorChar) {
								remainingPath = remainingPath.substring(1);
							}
						} else if (relativePathBuilder.charAt(relativePathBuilder.length() - 1) == separatorChar) {
							if (remainingPath.charAt(0) == separatorChar) {
								relativePathBuilder.setLength(relativePathBuilder.length() - 1);
							}
						} else if (remainingPath.charAt(0) != separatorChar) {
							relativePathBuilder.append(separatorString);
						}

						relativePathBuilder.append(remainingPath);
					}

					return relativePathBuilder.toString();
				}
			}
		}
	}

	public static boolean isPropertiesFile(String fileName) {
		if (!Options.ZKM_PROCESS_PROPERTIES) {
			return false;
		} else {
			int lastDotIndex = fileName.lastIndexOf(46);
			return lastDotIndex > -1 && fileName.substring(lastDotIndex, fileName.length()).equalsIgnoreCase(".properties");
		}
	}

	public static String decodeURLString(String path) {
		String decodedPath = null;

		try {
			decodedPath = URLDecoder.decode(path, DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException ignoredDefaultEncoding) {
			try {
				decodedPath = URLDecoder.decode(path, "UTF-8");
			} catch (UnsupportedEncodingException ignoredUtf8Fallback) {
			}
		}

		return decodedPath != null ? decodedPath : path;
	}

	public static String getZipEntryPath(ZipFile zipFile, ZipEntry zipEntry) {
		return zipFile.getName() + "!" + zipEntry.getName();
	}

	public static String resolveRelativePath(String path, File baseDirectory) {
		if (path == null) {
			return null;
		} else {
			String baseAbsolutePath;
			if (baseDirectory == null) {
				baseAbsolutePath = USER_DIRECTORY_PATH;
				baseDirectory = USER_DIRECTORY;
			} else {
				baseAbsolutePath = baseDirectory.getAbsolutePath();
			}

			path = path.trim();
			StringBuilder resolvedPathBuilder = new StringBuilder();
			if (path.startsWith(".." + FILE_SEPARATOR_CHAR)) {
				File parentDirectory = baseDirectory.getParentFile();
				if (parentDirectory == null) {
					resolvedPathBuilder.append(baseAbsolutePath);
				} else {
					resolvedPathBuilder.append(parentDirectory.getAbsolutePath());
				}

				if (path.length() > 2) {
					if (resolvedPathBuilder.charAt(resolvedPathBuilder.length() - 1) == FILE_SEPARATOR_CHAR && path.charAt(2) == FILE_SEPARATOR_CHAR) {
						resolvedPathBuilder.deleteCharAt(resolvedPathBuilder.length() - 1);
					}

					resolvedPathBuilder.append(path.substring(2));
				}
			} else {
				if (!path.startsWith("." + FILE_SEPARATOR_CHAR)) {
					return path;
				}

				resolvedPathBuilder.append(baseAbsolutePath);
				if (path.length() > 1) {
					if (resolvedPathBuilder.charAt(resolvedPathBuilder.length() - 1) == FILE_SEPARATOR_CHAR && path.charAt(1) == FILE_SEPARATOR_CHAR) {
						resolvedPathBuilder.deleteCharAt(resolvedPathBuilder.length() - 1);
					}

					resolvedPathBuilder.append(path.substring(1));
				}
			}

			return resolvedPathBuilder.toString();
		}
	}

	public static List<String> findFilesByPath(String path, File baseDirectory, AtomicReference<String> errMessageRef, boolean allowDirectoryWildcard) {
		errMessageRef.setNull();
		Vector<String> matchedPaths = new Vector<>();
		if (path != null && path.trim().length() != 0) {
			String resolvedPath = resolveRelativePath(normalizePathSeparators(path.trim()), baseDirectory);
			if (containsWildcard(resolvedPath)) {
				int lastSeparatorIndex = resolvedPath.lastIndexOf(FILE_SEPARATOR);
				File searchDirectory;
				String fileNamePattern;
				if (lastSeparatorIndex > 0) {
					fileNamePattern = resolvedPath.substring(lastSeparatorIndex + 1, resolvedPath.length());
					String directoryPath = resolvedPath.substring(0, lastSeparatorIndex);
					if (allowDirectoryWildcard && containsWildcard(directoryPath)) {
						int wildcardIndex = directoryPath.indexOf("*");
						int directorySeparatorIndex = directoryPath.lastIndexOf(FILE_SEPARATOR);
						if (wildcardIndex < directorySeparatorIndex) {
							errMessageRef.set("Wildcard not in final directory qualifier : '" + directoryPath + "'");
							return matchedPaths;
						}

						if (true) {
							fileNamePattern = fileNamePattern.toUpperCase();
						}
						return findFilesInWildcardDirectory(directoryPath, fileNamePattern, baseDirectory, errMessageRef);
					}

					if (isRelativePath(directoryPath)) {
						searchDirectory = new File(baseDirectory, directoryPath);
					} else {
						searchDirectory = new File(directoryPath);
					}
				} else {
					searchDirectory = baseDirectory;
					fileNamePattern = resolvedPath;
				}

				if (true) {
					fileNamePattern = fileNamePattern.toUpperCase();
				}
				if (searchDirectory.isDirectory() && searchDirectory.exists()) {
					File[] files = searchDirectory.listFiles();
					if (files != null) {
						for (int i = 0; i < files.length; i++) {
							String fileName = files[i].getName();
							if (true) {
								fileName = fileName.toUpperCase();
							}
							if (StringUtils.matchesWildcard(fileName, fileNamePattern)) {
								matchedPaths.add(files[i].getAbsolutePath());
							}
						}
					} else {
						errMessageRef.set("' File error trying to access " + searchDirectory.getAbsolutePath() + "' : '" + resolvedPath + "'");
					}
				} else {
					errMessageRef.set("'" + searchDirectory.getAbsolutePath() + "' is not a directory or does not exist : '" + resolvedPath + "'");
				}
			} else {
				File resolvedFile = new File(resolvedPath);
				matchedPaths.add(resolvedFile.getAbsolutePath());
			}

			return matchedPaths;
		} else {
			errMessageRef.set("No file path specified");
			return matchedPaths;
		}
	}

	public static boolean isArchiveExtension(String fileName) {
		int nameLength = fileName.length();
		if (nameLength > 4) {
			String lastFourChars = fileName.substring(nameLength - 4);
			if (lastFourChars.equalsIgnoreCase(".zip")
				|| lastFourChars.equalsIgnoreCase(".jar")
				|| lastFourChars.equalsIgnoreCase(".ear")
				|| lastFourChars.equalsIgnoreCase(".war")
				|| lastFourChars.equalsIgnoreCase(".rar")
				|| lastFourChars.equalsIgnoreCase(".sar")
				|| lastFourChars.equalsIgnoreCase(".par")
				|| lastFourChars.equalsIgnoreCase(".aar")) {
				return true;
			}

			if (nameLength > 5 && fileName.substring(nameLength - 5).equalsIgnoreCase(".jmod")) {
				return true;
			}
		}

		return false;
	}

	public static File copyToManagedTempFile(File sourceFile) throws IOException {
		BufferedInputStream inputStream = null;

		File tempFile;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(sourceFile));
			tempFile = writeStreamToManagedTempFile(inputStream, sourceFile.getAbsolutePath());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ignored) {
				}
			}
		}

		return tempFile;
	}

	public static BufferedReader createBufferedReader(File file, String encoding) throws IOException {
		return createReaderWithBomDetection(new FileInputStream(file), encoding, null);
	}

	public static String normalizePathSeparators(String path) {
		path = path.trim();
		if (path.length() > 1 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
			return File.separatorChar == '\\' ? path.replace('/', File.separatorChar) : path;
		} else {
			return path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
		}
	}

	public static byte[] peekBytes(PushbackInputStream inputStream, int byteCount) {
		try {
			byte[] peekedBytes = new byte[byteCount];
			int bytesRead = readFully(inputStream, peekedBytes);
			if (bytesRead == -1) {
				bytesRead = 0;
			}

			inputStream.unread(peekedBytes, 0, bytesRead);
			return peekedBytes;
		} catch (IOException ignored) {
			return null;
		}
	}

	public static String readToString(File file, String encoding) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		return readToString(fileInputStream, encoding, fileInputStream.available() + 10, null);
	}

	public static String resolveRelativeResourcePath(String path, String baseDirectory) {
		if (path == null) {
			return null;
		} else if (baseDirectory == null) {
			throw new IllegalArgumentException("Must specify a base directory. Cannot translate '" + path + "'.");
		} else {
			path = path.trim();
			StringBuilder resolvedPathBuilder = new StringBuilder();
			if (path.startsWith("..")) {
				String remainingPath = path;
				String currentBaseDirectory = baseDirectory;

				while (remainingPath.startsWith("..")) {
					String parentDirectory = getParentDirectory(currentBaseDirectory);
					if (parentDirectory == null) {
						throw new IllegalArgumentException("Base directory '" + baseDirectory + "' has inadequate parent. Cannot translate '" + path + "'.");
					}

					currentBaseDirectory = parentDirectory;
					if (remainingPath.length() > 2) {
						remainingPath = remainingPath.substring(3);
					} else {
						remainingPath = "";
					}
				}

				resolvedPathBuilder.append(currentBaseDirectory);
				if (resolvedPathBuilder.length() > 0 && resolvedPathBuilder.charAt(resolvedPathBuilder.length() - 1) != '/' && remainingPath.length() > 0) {
					resolvedPathBuilder.append('/');
				}

				resolvedPathBuilder.append(remainingPath);
			} else if (path.startsWith(".")) {
				if (baseDirectory.length() > 0) {
					resolvedPathBuilder.append(baseDirectory);
					if (path.length() > 1) {
						if (resolvedPathBuilder.charAt(resolvedPathBuilder.length() - 1) == '/' && path.charAt(1) == '/') {
							resolvedPathBuilder.deleteCharAt(resolvedPathBuilder.length() - 1);
						}

						resolvedPathBuilder.append(path.substring(1));
					}
				} else {
					String relativeSubPath = path.substring(1);
					if (relativeSubPath.length() > 0 && relativeSubPath.charAt(0) == '/') {
						relativeSubPath = relativeSubPath.substring(1);
						resolvedPathBuilder.append(relativeSubPath);
					}
				}
			} else {
				if (path.charAt(0) == '/') {
					return path;
				}

				if (baseDirectory.length() > 0) {
					resolvedPathBuilder.append(baseDirectory);
					if (resolvedPathBuilder.charAt(resolvedPathBuilder.length() - 1) != '/') {
						resolvedPathBuilder.append('/');
					}
				}

				resolvedPathBuilder.append(path);
			}

			return resolvedPathBuilder.toString();
		}
	}

	public static List<String> getExtraXmlFileExtensions() {
		return EXTRA_XML_FILE_EXTENSIONS;
	}

	public static void copyFile(String sourcePath, String destinationPath) throws IOException {
		copyFile(sourcePath, destinationPath, (Integer)null, (Integer)null);
	}

	public static String readToString(ZipFile zipFile, ZipEntry zipEntry, String encoding) throws IOException {
		return readToString(zipFile.getInputStream(zipEntry), encoding, zipEntry.getSize(), null);
	}

	public static boolean isArchive(File file) throws IOException {
		String fileName = file.getName();
		return isArchiveExtension(fileName) || isArchiveExtensionLenient(fileName) && isZipFile(file);
	}

	public static BufferedReader createBufferedReader(String filePath, String encoding) throws IOException {
		return createReaderWithBomDetection(new FileInputStream(filePath.trim()), encoding, null);
	}

	public static String getEncodingFromBOM(byte[] bomBytes) {
		return detectEncodingFromBOM(bomBytes, new AtomicInt(), new AtomicInt(), new AtomicInt());
	}

	public static boolean testFileSystemWriteRead(String filePath, AtomicReference<String> errMessageRef) {
		PrintWriter writer = null;

		label175: {
			try {
				writer = new PrintWriter(new FileWriter(filePath));
				writer.println("xXyYzZ");
				break label175;
			} catch (IOException writeException) {
				if (errMessageRef != null) {
					errMessageRef.set(writeException.toString());
				}
			} finally {
				if (writer != null) {
					writer.close();
				}
			}

			return false;
		}

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(filePath));
			String line = reader.readLine();
			return line != null && "xXyYzZ".equals(line);
		} catch (IOException readException) {
			if (errMessageRef != null) {
				errMessageRef.set(readException.toString());
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {
				}
			}
		}

		return false;
	}

	public static int readFully(InputStream inputStream, byte[] buffer) throws IOException {
		int totalRead = 0;

		do {
			int bytesRead = inputStream.read(buffer, totalRead, buffer.length - totalRead);
			if (bytesRead == -1) {
				if (totalRead == 0) {
					totalRead = -1;
				}
				break;
			}

			totalRead += bytesRead;
		} while (totalRead < buffer.length);

		return totalRead;
	}

	public static String getFileName(String path) {
		if (path == null) {
			return null;
		} else {
			String trimmedPath = path.trim();
			if (FILE_SEPARATOR_CHAR == '\\') {
				trimmedPath = trimmedPath.replace('/', FILE_SEPARATOR_CHAR);
			} else if (FILE_SEPARATOR_CHAR == '/') {
				trimmedPath = trimmedPath.replace('\\', FILE_SEPARATOR_CHAR);
			}

			while (trimmedPath.endsWith(FILE_SEPARATOR)) {
				trimmedPath = trimmedPath.substring(0, trimmedPath.length() - FILE_SEPARATOR.length());
			}

			int lastSeparatorIndex = trimmedPath.lastIndexOf(FILE_SEPARATOR_CHAR);
			if (lastSeparatorIndex > -1) {
				return lastSeparatorIndex == trimmedPath.length() - 1 ? trimmedPath.substring(0, trimmedPath.length() - 1) : trimmedPath.substring(lastSeparatorIndex + 1);
			} else {
				return trimmedPath;
			}
		}
	}

	public static boolean isZipFile(File file) throws IOException {
		FileInputStream fileInputStream = null;

		boolean zipDetected;
		try {
			fileInputStream = new FileInputStream(file);
			zipDetected = isZipStream(fileInputStream);
		} finally {
			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (DebugAssertFailedException debugAssertFailure) {
				throw debugAssertFailure;
			} catch (Exception ignored) {
			}
		}

		return zipDetected;
	}

	public static File writeStreamToManagedTempFile(BufferedInputStream inputStream, String originalName) throws IOException {
		File tempFile = TempFileManager.createTempFile(originalName);
		BufferedOutputStream outputStream = null;

		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
			inputStream.available();
			byte[] buffer = new byte[2048];
			int totalBytesWritten = 0;

			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				totalBytesWritten += bytesRead;
				outputStream.write(buffer, 0, bytesRead);
			}
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException ignored) {
				}
			}
		}

		return tempFile;
	}

	public static String getResolvedZipEntryPath(ZipFile zipFile, ZipEntry zipEntry) {
		StringBuilder resolvedPathBuilder = new StringBuilder();
		resolvedPathBuilder.append(zipFile.getName());
		File zipFileHandle = new File(zipFile.getName());
		if (TempFileManager.isManagedTempFile(zipFileHandle)) {
			String originalName = TempFileManager.getOriginalName(zipFileHandle.getAbsolutePath());
			if (originalName != null) {
				resolvedPathBuilder.append('(');
				resolvedPathBuilder.append(originalName);
				resolvedPathBuilder.append(')');
			}
		}

		resolvedPathBuilder.append("!");
		resolvedPathBuilder.append(zipEntry.getName());
		return resolvedPathBuilder.toString();
	}

	public static String readToString(File file, String encoding, Stripper stripper) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		return readToString(fileInputStream, encoding, fileInputStream.available() + 10, stripper);
	}

	public static String getParentDirectory(String path) {
		if (path.length() > 1) {
			char lastChar = path.charAt(path.length() - 1);
			if (lastChar == FILE_SEPARATOR.charAt(0) || lastChar == "/".charAt(0) || lastChar == "\\".charAt(0)) {
				path = path.substring(0, path.length() - 1);
			}
		}

		int lastSeparatorIndex;
		if ((lastSeparatorIndex = path.lastIndexOf(FILE_SEPARATOR)) <= -1 && (lastSeparatorIndex = path.lastIndexOf("/")) <= -1 && (lastSeparatorIndex = path.lastIndexOf("\\")) <= -1) {
			return path.length() > 0 ? "" : null;
		} else if (lastSeparatorIndex == 0) {
			return path.length() == 1 ? null : path.substring(0, lastSeparatorIndex + 1);
		} else {
			return path.substring(0, lastSeparatorIndex);
		}
	}

	public static BufferedReader createReaderWithBomDetection(InputStream inputStream, String encoding, AtomicReference<String> encodingRef) throws UnsupportedEncodingException, IOException {
		PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, BOM_BUFFER_SIZE);
		String resolvedEncoding = null;
		if (encoding != null && encoding.trim().length() > 0) {
			resolvedEncoding = encoding.trim();
		}

		boolean utf8BomDetected = false;
		if (resolvedEncoding == null || resolvedEncoding.length() == 0 || resolvedEncoding.equals("UTF-8")) {
			String bomEncoding = getEncodingFromBOM(peekBytes(pushbackStream, BOM_BUFFER_SIZE));
			utf8BomDetected = bomEncoding != null && bomEncoding.equals("UTF-8");
			if (bomEncoding != null && bomEncoding.length() > 0 && (resolvedEncoding == null || resolvedEncoding.length() == 0)) {
				resolvedEncoding = bomEncoding;
			}
		}

		BufferedReader reader;
		if (resolvedEncoding != null && resolvedEncoding.length() != 0) {
			if (utf8BomDetected) {
				readBytes(pushbackStream, 3);
			}

			reader = new BufferedReader(new InputStreamReader(pushbackStream, resolvedEncoding));
		} else {
			reader = new BufferedReader(new InputStreamReader(pushbackStream));
		}

		if (encodingRef != null) {
			encodingRef.set(resolvedEncoding);
		}

		return reader;
	}

	public static void copyFile(String sourcePath, String destinationPath, Integer inputBufferSize, Integer outputBufferSize) throws IOException {
		if (new File(sourcePath).getAbsolutePath().equals(new File(destinationPath).getAbsolutePath())) {
			throw new IllegalArgumentException("Source and destination files are the same : '" + new File(sourcePath).getAbsolutePath() + "'");
		} else {
			BufferedOutputStream outputStream = null;
			BufferedInputStream inputStream = null;

			try {
				if (inputBufferSize != null) {
					inputStream = new BufferedInputStream(new FileInputStream(sourcePath), inputBufferSize);
				} else {
					inputStream = new BufferedInputStream(new FileInputStream(sourcePath));
				}

				if (outputBufferSize != null) {
					outputStream = new BufferedOutputStream(new FileOutputStream(destinationPath), outputBufferSize);
				} else {
					outputStream = new BufferedOutputStream(new FileOutputStream(destinationPath));
				}

				int availableBytes = inputStream.available();
				byte[] buffer = new byte[availableBytes];
				int offset = 0;

				int bytesRead;
				while (offset < availableBytes && (bytesRead = inputStream.read(buffer, offset, Math.min(8192, availableBytes - offset))) != -1) {
					offset += bytesRead;
				}

				outputStream.write(buffer);
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException ignoredOutputClose) {
					}
				}

				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (IOException ignoredInputClose) {
					}
				}
			}
		}
	}

	public static boolean isYamlFile(String fileName) {
		if (!Options.ZKM_PROCESS_YAML) {
			return false;
		} else {
			int lastDotIndex = fileName.lastIndexOf(46);
			if (lastDotIndex > -1) {
				String extension = fileName.substring(lastDotIndex, fileName.length());
				if (extension.equalsIgnoreCase(".yaml") || extension.equalsIgnoreCase(".yml")) {
					return true;
				}
			}

			return false;
		}
	}

	public static File writeStringToFile(String string, File dest, AtomicReference<String> errMessageRef) throws IOException {
		PrintWriter writer = null;

		try {
			writer = new PrintWriter(new BufferedWriter(new FileWriter(dest)));
			writer.println(string);
			writer.flush();
			return dest;
		} catch (IOException ioException) {
			errMessageRef.set(ioException.getMessage());
		} finally {
			if (writer != null) {
				writer.close();
			}
		}

		return null;
	}

	private static String readToString(InputStream inputStream, String encoding, long estimatedLength, Stripper stripper) throws IOException {
		BufferedReader reader = null;
		StringWriter stringWriter;
		if (estimatedLength > -1L && estimatedLength < 2147483647L) {
			stringWriter = new StringWriter((int)estimatedLength + 10);
		} else {
			stringWriter = new StringWriter();
		}

		try {
			reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
			boolean firstLine = true;

			String line;
			while ((line = reader.readLine()) != null) {
				if (stripper != null) {
					line = stripper.stripComment(line);
				}

				if (firstLine) {
					firstLine = false;
				} else {
					stringWriter.append(LINE_SEPARATOR);
				}

				stringWriter.write(line);
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {
				}
			} else {
				try {
					inputStream.close();
				} catch (Exception ignoredOnClose) {
				}
			}
		}

		return stringWriter.toString();
	}

	public static File extractToManagedTempFile(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
		BufferedInputStream inputStream = null;

		File tempFile;
		try {
			inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
			tempFile = writeStreamToManagedTempFile(inputStream, zipFile.getName() + "!" + zipEntry.getName());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ignored) {
				}
			}
		}

		return tempFile;
	}

	private static List<String> findFilesInWildcardDirectory(String wildcardDirectory, String fileNamePattern, File baseDirectory, AtomicReference<String> errMessageRef) {
		int lastSeparatorIndex = wildcardDirectory.lastIndexOf(FILE_SEPARATOR);
		String directoryPath;
		String wildcardPattern;
		if (lastSeparatorIndex > -1) {
			directoryPath = wildcardDirectory.substring(0, lastSeparatorIndex);
			wildcardPattern = wildcardDirectory.substring(lastSeparatorIndex + 1);
		} else {
			directoryPath = null;
			wildcardPattern = wildcardDirectory;
		}

		File searchDirectory;
		if (directoryPath == null) {
			searchDirectory = baseDirectory;
		} else if (isRelativePath(directoryPath)) {
			searchDirectory = new File(baseDirectory, directoryPath);
		} else {
			searchDirectory = new File(directoryPath);
		}

		Vector<String> resultPaths = new Vector<>();
		if (!searchDirectory.exists()) {
			errMessageRef.set("Directory '" + searchDirectory.getAbsolutePath() + "' does not exist.");
			return resultPaths;
		} else if (!searchDirectory.isDirectory()) {
			errMessageRef.set("Path '" + searchDirectory.getAbsolutePath() + "' is not a directory.");
			return resultPaths;
		} else {
			File[] files = searchDirectory.listFiles();
			if (files == null) {
				errMessageRef.set("Unknown error reading '" + searchDirectory.getAbsolutePath() + "'.");
				return resultPaths;
			} else {
				for (File file : files) {
					if (file.isDirectory() && StringUtils.matchesWildcard(file.getName(), wildcardPattern)) {
						findFilesRecursively(file, fileNamePattern, resultPaths);
					}
				}

				return resultPaths;
			}
		}
	}

	private static void findFilesRecursively(File directory, String fileNamePattern, List<String> resultPaths) {
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) {
				findFilesRecursively(file, fileNamePattern, resultPaths);
			} else {
				String fileName = file.getName();
				if (true) {
					fileName = fileName.toUpperCase();
				}
				if (StringUtils.matchesWildcard(fileName, fileNamePattern)) {
					resultPaths.add(file.getAbsolutePath());
				}
			}
		}
	}

	public static BufferedReader createBufferedReader(File file) throws IOException {
		return createReaderWithBomDetection(new FileInputStream(file), (String)null, null);
	}

	public static boolean isArchiveExtensionLenient(String fileName) {
		int nameLength = fileName.length();
		int lastDotIndex = fileName.lastIndexOf(46);
		if (lastDotIndex > 0 && (lastDotIndex == nameLength - 4 || lastDotIndex == nameLength - 5)) {
			String extension = fileName.substring(lastDotIndex).toLowerCase();
			if (extension.equals(".zip") || extension.equals(".jmod") || extension.endsWith("ar")) {
				return true;
			}
		}

		return false;
	}

	public static boolean isZipEntryContentZip(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
		InputStream inputStream = null;

		boolean zipDetected;
		try {
			inputStream = zipFile.getInputStream(zipEntry);
			zipDetected = isZipStream(inputStream);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (DebugAssertFailedException debugAssertFailure) {
				throw debugAssertFailure;
			} catch (Exception ignored) {
			}
		}

		return zipDetected;
	}

	public static byte[] readBytes(InputStream inputStream, int byteCount) throws IOException {
		byte[] bytes = null;
		if (byteCount > 0) {
			bytes = new byte[byteCount];
			readFully(inputStream, bytes);
		}

		return bytes;
	}

	public static boolean containsWildcard(String path) {
		return path.indexOf("*") > -1;
	}

	public static String detectEncodingFromBOM(byte[] bomBytes, AtomicInt bomSkipLength, AtomicInt bomLength, AtomicInt byteOrder) {
		if (bomBytes == null) {
			return null;
		} else if (bomBytes.length != BOM_BUFFER_SIZE) {
			throw new IllegalArgumentException("Byte Order Mark byte array length not" + BOM_BUFFER_SIZE + " : " + bomBytes.length);
		} else {
			int firstByte = bomBytes[0] & 255;
			int secondByte = bomBytes[1] & 255;
			int thirdByte = bomBytes[2] & 255;
			if (firstByte == 239 && secondByte == 187 && thirdByte == 191) {
				bomSkipLength.set(3);
				bomLength.set(3);
				return "UTF-8";
			} else if (firstByte == 255 && secondByte == 254) {
				bomLength.set(2);
				byteOrder.set(0);
				return "UTF-16";
			} else if (firstByte == 254 && secondByte == 255) {
				bomLength.set(2);
				byteOrder.set(1);
				return "UTF-16";
			} else {
				return null;
			}
		}
	}

	public static String readToString(File file) throws IOException {
		return readToString(file, DEFAULT_ENCODING);
	}

	public static BufferedReader createBufferedReader(String filePath) throws IOException {
		return createReaderWithBomDetection(new FileInputStream(filePath.trim()), (String)null, null);
	}

	static {
		String upperCaseProbeName = "aAzZyY".toUpperCase();
		int probeNumber = 0;

		File probeFile;
		File upperCaseProbeFile;
		do {
			probeFile = new File("aAzZyY" + probeNumber + ".txt");
			upperCaseProbeFile = new File(upperCaseProbeName + probeNumber++ + ".txt");
		} while (probeFile.exists() || upperCaseProbeFile.exists());

		try {
			PrintWriter probeWriter = new PrintWriter(new FileWriter(probeFile), true);
			probeWriter.println("aAzZyY");
			probeWriter.flush();
			probeWriter.close();
			IS_UNIX = !upperCaseProbeFile.exists();
		} catch (IOException ignored) {
			IS_UNIX = !OS_NAME.startsWith("Windows") && !OS_NAME.startsWith("OS/2") && !OS_NAME.startsWith("Mac OS");
		}

		probeFile.delete();
		if (Options.ZKM_EXTRA_XML_FILE_TYPES != null) {
			EXTRA_XML_FILE_EXTENSIONS = new ArrayList<>();
			StringTokenizer extraTypesTokenizer = new StringTokenizer(Options.ZKM_EXTRA_XML_FILE_TYPES, ";");

			while (extraTypesTokenizer.hasMoreTokens()) {
				String extension = extraTypesTokenizer.nextToken();
				if (true) {
					extension = extension.toLowerCase();
				}
				EXTRA_XML_FILE_EXTENSIONS.add(extension);
			}
		}
	}
}

package com.zelix.utils;

import com.zelix.exceptions.ZKMException;
import com.zelix.framework.manifest.ManifestFile;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.klassmaster.script.statements.command.PathConfig;
import com.zelix.klassmaster.script.statements.javaccInterface.PathFilter;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.dataset.ArchiveEntry;
import com.zelix.utils.references.AtomicReference;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

public class ArchiveUtils {
	private static boolean isPathMatchedByFilter(String path, PathFilter filter) {
		return filter == PathFilter.MATCH_ALL ? true : filter.matches(path);
	}

	private static String getFileExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf(46);
		if (dotIndex > -1 && dotIndex < fileName.length()) {
			return fileName.substring(dotIndex + 1);
		} else {
			return fileName.equals("*") ? "*" : "";
		}
	}

	private static boolean checkIfPathIsSkipped(String path, List<String> skipPatterns, List<String> unskipPatterns, AtomicReference<String> outUsedSkipPattern, AtomicReference<String> outUsedUnskipPattern) {
		outUsedSkipPattern.set(null);
		String normalizedPath;
		if (FileUtils.IS_UNIX) {
			normalizedPath = path;
		} else {
			normalizedPath = path.toUpperCase();
		}

		normalizedPath = normalizedPath.replace('\\', '/');
		int lastSeparatorIndex = findLastPathSeparatorIndex(normalizedPath);
		String pathFileName = normalizedPath.substring(lastSeparatorIndex + 1);
		String pathDirPrefix = normalizedPath.substring(0, lastSeparatorIndex + 1);
		StringBuilder patternBuffer = new StringBuilder();

		for (int i = 0; i < skipPatterns.size(); i++) {
			String skipPattern = skipPatterns.get(i).trim();
			if (skipPattern.length() > 0) {
				patternBuffer.setLength(0);
				patternBuffer.append("*");
				patternBuffer.append(skipPattern);
				String wildcardPattern = patternBuffer.toString();
				if (!FileUtils.IS_UNIX) {
					wildcardPattern = wildcardPattern.toUpperCase();
				}

				int patternSeparatorIndex = findLastPathSeparatorIndex(wildcardPattern);
				String patternFileName = wildcardPattern.substring(patternSeparatorIndex + 1);
				String patternDirPrefix = wildcardPattern.substring(0, patternSeparatorIndex + 1);
				if (StringUtils.matchesWildcard(pathDirPrefix, patternDirPrefix) && StringUtils.matchesWildcard(pathFileName, patternFileName)) {
					outUsedSkipPattern.set(skipPattern);
					if (unskipPatterns != null && checkIfPathIsUnskipped(pathDirPrefix, pathFileName, patternDirPrefix, patternFileName, unskipPatterns, outUsedUnskipPattern)) {
						return false;
					}

					return true;
				}
			}
		}

		return false;
	}

	private static boolean checkIfPathIsUnskipped(String pathPrefix, String fileName, String patternPrefix, String patternFileName, List<String> unskipPatterns, AtomicReference<String> outUsedUnskipPattern) {
		outUsedUnskipPattern.set(null);
		int patternPrefixBangCount = StringUtils.countOccurrences(patternPrefix, '!');
		String skipPatternExtension = getFileExtension(patternFileName);
		StringBuilder patternBuffer = new StringBuilder();

		for (int i = 0; i < unskipPatterns.size(); i++) {
			String unskipPattern = unskipPatterns.get(i).trim();
			if (unskipPattern.length() > 0 && StringUtils.countOccurrences(unskipPattern, '!') == patternPrefixBangCount) {
				patternBuffer.setLength(0);
				patternBuffer.append("*");
				patternBuffer.append(unskipPattern);
				String wildcardPattern = patternBuffer.toString();
				if (!FileUtils.IS_UNIX) {
					wildcardPattern = wildcardPattern.toUpperCase();
				}

				int patternSeparatorIndex = findLastPathSeparatorIndex(wildcardPattern);
				String unskipPatternFileName = wildcardPattern.substring(patternSeparatorIndex + 1);
				String unskipPatternDirPrefix = wildcardPattern.substring(0, patternSeparatorIndex + 1);
				String unskipPatternExtension = getFileExtension(unskipPatternFileName);
				if ((unskipPatternExtension.equals(skipPatternExtension) || StringUtils.matchesWildcard(unskipPatternExtension, skipPatternExtension) || StringUtils.matchesWildcard(skipPatternExtension, unskipPatternExtension))
					&& StringUtils.matchesWildcard(pathPrefix, unskipPatternDirPrefix)
					&& StringUtils.matchesWildcard(fileName, unskipPatternFileName)) {
					outUsedUnskipPattern.set(unskipPattern);
					return true;
				}
			}
		}

		return false;
	}

	private static int findLastPathSeparatorIndex(String path) {
		int slashIndex = path.lastIndexOf(47);
		int bangIndex = path.lastIndexOf("!");
		return Math.max(slashIndex, bangIndex);
	}

	public static void processArchive(
		File archiveFile,
		Set<ClasspathResource> outAllClassResources,
		Map<String, ArchiveNode> archiveCache,
		PathConfig fileConfig,
		boolean processNestedArchivesLater,
		List<String> skipPatterns,
		List<String> unskipPatterns,
		Set<String> outUsedSkipPatterns,
		Set<String> outUsedUnskipPatterns,
		Set<ArchiveNode> outNestedArchives,
		Set<ClasspathResource> outSkippedClassResources,
		Set<ClasspathResource> outSkippedXmlResources,
		Set<ClasspathResource> outSkippedYamlResources,
		Set<ClasspathResource> outSkippedPropertiesResources,
		Set<ClasspathResource> outFilteredPathResources,
		String entryPath,
		ArchiveNode parentArchiveNode,
		TemporaryZipWriter skippedClassesZipWriter,
		Set<String> skippedClassNamesCache,
		AbstractLogger logger
	) {
		String archivePath;
		if (parentArchiveNode != null) {
			archivePath = parentArchiveNode.getFullPathForChild(entryPath);
		} else {
			archivePath = archiveFile.getAbsolutePath();
		}

		Vector<ArchiveEntry> nestedArchiveEntries = new Vector<>();

		try {
			ArchiveNode node = archiveCache.get(archivePath);
			if (node == null) {
				if (parentArchiveNode != null) {
					node = new ArchiveNode(archiveFile.getAbsolutePath(), archiveFile.length(), entryPath, parentArchiveNode);
				} else {
					node = new ArchiveNode(
						archiveFile.getAbsolutePath(), archiveFile.length(), entryPath, fileConfig.getExpectedInitialHash(), fileConfig.getExpectedFinalHash()
					);
				}

				if (archivePath.contains("SnakeGame.jar")) {
					node.setModuleName("sb");
				}

				if (false) {
					System.out.println("Create ArchiveNode for: " + archivePath);
					System.out.println(" - parent: " + (parentArchiveNode != null ? parentArchiveNode.getFullPath() : "null"));
					System.out.println(" - tempFilePath: " + node.getFilePath()
							+ ", relativePath: " + node.getRelativePath()
							+ ", fullPath: " + node.getFullPath()
							+ ", expectedInitialHash: " + node.getExpectedFinalHash()
							+ ", expectedFinalHash: " + node.getExpectedFinalHash()
							+ ", sizeInBytes: " + node.getSizeInBytes()
							+ ", manifestFile: " + (node.getManifest() != null ? "present" : "null"));
				}

				archiveCache.put(archivePath, node);
			}

			Object pathFilter;
			if (fileConfig != null) {
				pathFilter = fileConfig.getFilter();
			} else {
				pathFilter = PathFilter.MATCH_ALL;
			}

			if (!skipPatterns.isEmpty() && parentArchiveNode != null) {
				if (!isPathMatchedByFilter(archivePath, (PathFilter)pathFilter)) {
					logger.message(
						"INFO:", "Filtering out nested archive '" + archivePath + "' because it does not match specified filter '" + ((PathFilter)pathFilter).getDefinition() + "' (C)"
					);
					return;
				}

				AtomicReference<String> archiveSkipPatternRef = new AtomicReference<>();
				AtomicReference<String> archiveUnskipPatternRef = new AtomicReference<>();
				boolean archiveSkipped = checkIfPathIsSkipped(archivePath, skipPatterns, unskipPatterns, archiveSkipPatternRef, archiveUnskipPatternRef);
				if (!archiveSkipPatternRef.isNull()) {
					String archiveSkipPattern = archiveSkipPatternRef.get();
					logger.message("INFO:", "Skipping nested archive '" + archivePath + "' because of parameter -\"" + archiveSkipPattern + "\"");
					outUsedSkipPatterns.add(archiveSkipPattern);
				}

				if (!archiveUnskipPatternRef.isNull()) {
					String archiveUnskipPattern = archiveUnskipPatternRef.get();
					logger.message("INFO:", "Unskipping nested archive '" + archivePath + "' because of parameter +\"" + archiveUnskipPattern + "\"");
					outUsedUnskipPatterns.add(archiveUnskipPattern);
				}

				if (archiveSkipped) {
					outNestedArchives.add(node);
					return;
				}
			}

			boolean classResourceAdded = false;
			ZipEntry manifestEntry = null;
			ZipFileWrapper zipFile = new ZipFileWrapper(archiveFile);
			Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

			while (zipEntries.hasMoreElements()) {
				ZipEntry ze = zipEntries.nextElement();
				if (!ze.isDirectory()) {
					String name = ze.getName();
					String entryFullPath = (parentArchiveNode != null ? archivePath : entryPath) + "!" + name;
					if (isPathMatchedByFilter(entryFullPath, (PathFilter)pathFilter)) {
						if (name.endsWith(".class")) {
							AtomicReference<String> classSkipPatternRef = new AtomicReference<>();
							AtomicReference<String> classUnskipPatternRef = new AtomicReference<>();
							boolean classSkipped = !skipPatterns.isEmpty() && checkIfPathIsSkipped(entryFullPath, skipPatterns, unskipPatterns, classSkipPatternRef, classUnskipPatternRef);
							if (!classSkipPatternRef.isNull()) {
								String classSkipPattern = classSkipPatternRef.get();
								outUsedSkipPatterns.add(classSkipPattern);
								logger.message("INFO:", "Skipping class file '" + entryFullPath + "' because of parameter -\"" + classSkipPattern + "\"");
							}

							if (!classUnskipPatternRef.isNull()) {
								String classUnskipPattern = classUnskipPatternRef.get();
								logger.message("INFO:", "Unskipping class file '" + entryFullPath + "' because of parameter +\"" + classUnskipPattern + "\"");
								outUsedUnskipPatterns.add(classUnskipPattern);
							}

							if (classSkipped) {
								outSkippedClassResources.add(new ClasspathResource(zipFile, ze, node));
								if (skippedClassNamesCache.add(ze.getName())) {
									try {
										new ZipEntryWriter(skippedClassesZipWriter.getStream(), ze.getName(), false, zipFile.getInputStream(ze), (int)ze.getSize()).writeEntry();
									} catch (IOException skippedClassWriteException) {
										StringBuffer writeErrorMessage = new StringBuffer();
										writeErrorMessage.append("Could not write to \"" + skippedClassesZipWriter.getTempFilePath() + "\" : ");
										writeErrorMessage.append(skippedClassWriteException.getMessage());
										writeErrorMessage.append(" (1)");
										logger.fatal("FILE ERROR:", writeErrorMessage.toString());
									}
								} else {
									logger.warn(
										"WARNING:",
										"Skipped class file '"
											+ ze.getName()
											+ "' skipped in more than one archive. Class '"
											+ entryFullPath
											+ "' will not be automatically added to the effective classpath."
									);
								}
							} else {
								ClasspathResource classpathResource = new ClasspathResource(zipFile, ze, node);
								outAllClassResources.add(classpathResource);
								classResourceAdded = true;
							}
						} else if (FileUtils.isXmlFile(name)) {
							AtomicReference<String> xmlSkipPatternRef = new AtomicReference<>();
							AtomicReference<String> xmlUnskipPatternRef = new AtomicReference<>();
							boolean xmlSkipped = !skipPatterns.isEmpty() && checkIfPathIsSkipped(entryFullPath, skipPatterns, unskipPatterns, xmlSkipPatternRef, xmlUnskipPatternRef);
							if (!xmlSkipPatternRef.isNull()) {
								String xmlSkipPattern = xmlSkipPatternRef.get();
								if (outUsedSkipPatterns != null) {
									outUsedSkipPatterns.add(xmlSkipPattern);
								}

								logger.message("INFO:", "Skipping file '" + entryFullPath + "' because of parameter -\"" + xmlSkipPattern + "\"");
							}

							if (!xmlUnskipPatternRef.isNull()) {
								String xmlUnskipPattern = xmlUnskipPatternRef.get();
								logger.message("INFO:", "Unskipping file '" + entryFullPath + "' because of parameter +\"" + xmlUnskipPattern + "\"");
								outUsedUnskipPatterns.add(xmlUnskipPattern);
							}

							if (xmlSkipped) {
								outSkippedXmlResources.add(new ClasspathResource(zipFile, ze, node));
							}
						} else if (!FileUtils.isYamlFile(name) && !FileUtils.isPropertiesFile(name)) {
							boolean nestedArchive = false;

							try {
								nestedArchive = FileUtils.isNestedArchive(zipFile, ze);
							} catch (IOException nestedArchiveCheckException) {
								logger.error("FILE ERROR:", "File Error : " + nestedArchiveCheckException);
							}

							if (nestedArchive) {
								File tempFile = FileUtils.extractToManagedTempFile(zipFile, ze);
								if (processNestedArchivesLater) {
									nestedArchiveEntries.addElement(new ArchiveEntry(node, name, tempFile));
								} else {
									ArchiveNode nestedArchiveNode = new ArchiveNode(tempFile.getAbsolutePath(), tempFile.length(), name, node);
									archiveCache.put(node.getFullPathForChild(name), nestedArchiveNode);
									outNestedArchives.add(nestedArchiveNode);
								}
							} else if (name.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
								if (manifestEntry == null) {
									manifestEntry = ze;
								} else {
									logger.error("FILE ERROR:", "More than one MANIFEST in '" + entryPath + "' : '" + manifestEntry.getName() + "' '" + ze.getName() + "'");
								}
							}
						} else {
							AtomicReference<String> resourceSkipPatternRef = new AtomicReference<>();
							AtomicReference<String> resourceUnskipPatternRef = new AtomicReference<>();
							boolean resourceSkipped = !skipPatterns.isEmpty() && checkIfPathIsSkipped(entryFullPath, skipPatterns, unskipPatterns, resourceSkipPatternRef, resourceUnskipPatternRef);
							if (!resourceSkipPatternRef.isNull()) {
								String resourceSkipPattern = resourceSkipPatternRef.get();
								if (outUsedSkipPatterns != null) {
									outUsedSkipPatterns.add(resourceSkipPattern);
								}

								logger.message("INFO:", "Skipping file '" + entryFullPath + "' because of parameter -\"" + resourceSkipPattern + "\"");
							}

							if (!resourceUnskipPatternRef.isNull()) {
								String resourceUnskipPattern = resourceUnskipPatternRef.get();
								logger.message("INFO:", "Unskipping file '" + entryFullPath + "' because of parameter +\"" + resourceUnskipPattern + "\"");
								outUsedUnskipPatterns.add(resourceUnskipPattern);
							}

							if (resourceSkipped) {
								if (FileUtils.isYamlFile(name)) {
									outSkippedYamlResources.add(new ClasspathResource(zipFile, ze, node));
								} else {
									outSkippedPropertiesResources.add(new ClasspathResource(zipFile, ze, node));
								}
							}
						}
					} else {
						outFilteredPathResources.add(new ClasspathResource(zipFile, ze, node));
						logger.message(
							"INFO:", "Filtering out path '" + entryFullPath + "' because it does not match specified filter '" + ((PathFilter)pathFilter).getDefinition() + "' (D)"
						);
					}
				}
			}

			if (manifestEntry != null) {
				try {
					ManifestFile manifestFile = new ManifestFile(zipFile, manifestEntry);
					node.setManifest(manifestFile);
				} catch (ZKMException manifestException) {
					logger.fatal("FILE ERROR:", "Invalid MANIFEST in '" + entryPath + "' : " + manifestException.getMessage());
				} catch (IOException manifestReadException) {
					logger.fatal("FILE ERROR:", "Error reading MANIFEST in '" + entryPath + "' : " + manifestReadException);
				}
			}

			if (!classResourceAdded) {
				zipFile.close();
			}
		} catch (ZipException zipException) {
			archiveCache.remove(archivePath);
			StringBuffer zipErrorMessage = new StringBuffer();
			zipErrorMessage.append("\"" + archivePath + "\" couldn't be written or opened as an archive file : ");
			if (!archivePath.equals(entryPath)) {
				zipErrorMessage.append(" \"" + entryPath + "\" : ");
			}

			zipErrorMessage.append(zipException.getMessage());
			logger.warn("ARCHIVE FILE ERROR:", zipErrorMessage.toString());
		} catch (IOException archiveIoException) {
			archiveCache.remove(archivePath);
			StringBuffer ioErrorMessage = new StringBuffer();
			ioErrorMessage.append("\"" + archivePath + "\" couldn't be opened or written : ");
			if (!archivePath.equals(entryPath)) {
				ioErrorMessage.append("\"" + entryPath + "\" : ");
			}

			ioErrorMessage.append(archiveIoException.getMessage());
			logger.fatal("FILE ERROR:", ioErrorMessage.toString());
		}

		for (int i = 0; i < nestedArchiveEntries.size(); i++) {
			ArchiveEntry archiveEntry = nestedArchiveEntries.elementAt(i);
			processArchive(
				archiveEntry.file,
				outAllClassResources,
				archiveCache,
				fileConfig,
				processNestedArchivesLater,
				skipPatterns,
				unskipPatterns,
				outUsedSkipPatterns,
				outUsedUnskipPatterns,
				outNestedArchives,
				outSkippedClassResources,
				outSkippedXmlResources,
				outSkippedYamlResources,
				outSkippedPropertiesResources,
				outFilteredPathResources,
				archiveEntry.name,
				archiveEntry.node,
				skippedClassesZipWriter,
				skippedClassNamesCache,
				logger
			);
		}
	}
}

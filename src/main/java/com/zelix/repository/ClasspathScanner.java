package com.zelix.repository;

import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.framework.manifest.ManifestFile;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.TempFileManager;
import com.zelix.utils.ZipFileWrapper;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.references.AtomicReference;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ClasspathScanner {
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final char FILE_SEPARATOR_CHAR = FILE_SEPARATOR.charAt(0);
	private static final String PATH_SEPARATOR = System.getProperty("path.separator");
	private static final String[] SPRING_BOOT_CLASS_DIRS = new String[]{"WEB-INF/classes/", "BOOT-INF/classes/"};
	private static Object NOT_FOUND_MARKER = new Object();
	public static char MANIFEST_PATH_MARKER_CHAR = '#';
	public static String MANIFEST_PATH_MARKER = String.valueOf(MANIFEST_PATH_MARKER_CHAR);
	private Set<String> managedTempFiles = GlobalUtils.newHashSet();
	private Map<String, ZipFileWrapper> systemJarCache = GlobalUtils.newHashMap();
	private boolean enableCaching;
	private Map<String, Object> resourceCache;
	private Set<String> invalidPaths;
	private List<Object> classpathRoots;
	private String rawClasspathString;

	public ClasspathScanner(String classpath, boolean searchSystem) {
		this(classpath, searchSystem, false, (AbstractLogger)null);
	}

	private ClasspathScanner(String classpath, boolean searchSystem, boolean enableCaching, AbstractLogger logger) {
		this.enableCaching = enableCaching;
		this.initialize(classpath, logger);
	}

	private static void readManifestClassPath(ZipFile archive, List<String> resolvedPaths, Set<String> invalidPaths) {
		ZipEntry manifestEntry = archive.getEntry("META-INF/MANIFEST.MF");
		if (manifestEntry != null) {
			ManifestFile manifestFile;
			try {
				manifestFile = new ManifestFile(archive, manifestEntry);
			} catch (Exception ignored) {
				return;
			}

			resolveManifestPaths(archive, resolvedPaths, invalidPaths, manifestFile.getMainAttributeValue("Class-Path"));
			resolveManifestPaths(archive, resolvedPaths, invalidPaths, manifestFile.getMainAttributeValue("Rsrc-Class-Path"));
		}
	}

	public static String internalNameToClassFileName(String internalName, boolean useFileSeparator) {
		return useFileSeparator ? internalName.replace('/', FILE_SEPARATOR_CHAR) + ".class" : internalName + ".class";
	}

	public static List<Object> parseAndExpandClasspath(String rawClasspath, Set<String> invalidPaths, File defaultDirectory, AbstractLogger logger) {
		return expandClasspathInternal(rawClasspath, invalidPaths, null, defaultDirectory, logger);
	}

	private static boolean addAndCheckDuplicates(String path, Set<String> seenPaths) {
		if (!FileUtils.IS_UNIX) {
			path = path.toUpperCase();
		}

		return seenPaths.add(path);
	}

	private static void extractNestedArchives(ZipFile archive, List<Object> expandedArchives, Set<String> invalidPaths, Set<String> managedTempFiles, AbstractLogger logger) {
		Enumeration<? extends ZipEntry> entryEnumeration = archive.entries();

		while (entryEnumeration.hasMoreElements()) {
			ZipEntry entry = entryEnumeration.nextElement();
			String entryPath = FileUtils.getZipEntryPath(archive, entry);
			if (!entry.isDirectory()) {
				boolean nestedArchive = false;

				try {
					nestedArchive = FileUtils.isNestedArchive(archive, entry);
				} catch (IOException ignored) {
					invalidPaths.add(entryPath);
				}

				if (nestedArchive) {
					File nestedArchiveFile = null;
					boolean extractionFailed = false;

					try {
						nestedArchiveFile = FileUtils.extractToManagedTempFile(archive, entry);
						if (managedTempFiles != null) {
							managedTempFiles.add(nestedArchiveFile.getAbsolutePath());
						}
					} catch (IOException tempFileException) {
						extractionFailed = true;
						if (logger != null) {
							logger.fatal("FATAL ERROR:", "Could not create temporary file in '" + TempFileManager.TEMP_DIRECTORY_PATH + "' : '" + entryPath + "' : " + tempFileException + " (4)");
						}
					}

					if (!extractionFailed && nestedArchiveFile != null) {
						try {
							ZipFileWrapper nestedArchiveWrapper = new ZipFileWrapper(nestedArchiveFile);
							expandedArchives.add(nestedArchiveWrapper);
							extractNestedArchives(nestedArchiveWrapper, expandedArchives, invalidPaths, managedTempFiles, logger);
						} catch (IOException zipFileException) {
							if (logger != null) {
								logger.fatal(
									"FATAL ERROR:",
									"Could not open ZipFile '" + nestedArchiveFile.getAbsolutePath() + "' in '" + TempFileManager.TEMP_DIRECTORY_PATH + "' : '" + entryPath + "' : " + zipFileException
								);
							}
						}
					}
				}
			}
		}
	}

	private static void resolveManifestPaths(ZipFile archive, List<String> resolvedPaths, Set<String> invalidPaths, String classPathAttribute) {
		if (classPathAttribute != null) {
			StringTokenizer pathTokenizer = new StringTokenizer(classPathAttribute, " ");
			File archiveParentDirectory = new File(archive.getName()).getParentFile();

			while (pathTokenizer.hasMoreTokens()) {
				String resolvedPath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(pathTokenizer.nextToken().trim()), archiveParentDirectory);
				File resolvedFile;
				if (FileUtils.isRelativePath(resolvedPath)) {
					resolvedFile = new File(archiveParentDirectory, resolvedPath);
				} else {
					resolvedFile = new File(resolvedPath);
				}

				if (resolvedFile.exists()) {
					resolvedPaths.add(resolvedFile.getAbsolutePath());
				} else {
					invalidPaths.add(resolvedFile.getAbsolutePath() + MANIFEST_PATH_MARKER_CHAR);
				}
			}
		}
	}

	public static List<Object> expandClasspathInternal(String rawClasspath, Set<String> invalidPaths, Set<String> managedTempFiles, File defaultDirectory, AbstractLogger logger) {
		if (defaultDirectory == null) {
			defaultDirectory = FileUtils.USER_DIRECTORY;
		}

		Vector<String> classpathPaths = new Vector<>();
		StringTokenizer pathTokenizer = new StringTokenizer(rawClasspath, PATH_SEPARATOR);

		while (pathTokenizer.hasMoreTokens()) {
			String pathToken = pathTokenizer.nextToken();
			if (pathToken.trim().length() > 0) {
				String resolvedPath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(pathToken), defaultDirectory);
				if (FileUtils.containsWildcard(resolvedPath)) {
					AtomicReference<String> wildcardError = new AtomicReference<>();
					List<String> wildcardMatches = FileUtils.findFilesByPath(resolvedPath, defaultDirectory, wildcardError, true);
					if (wildcardError.isNull()) {
						classpathPaths.addAll(wildcardMatches);
					} else {
						invalidPaths.add(resolvedPath);
					}
				} else {
					classpathPaths.add(resolvedPath);
				}
			} else {
				invalidPaths.add(pathToken);
			}
		}

		ArrayList<Object> expandedRoots = new ArrayList<>(classpathPaths.size());
		HashSet<String> seenPaths = GlobalUtils.newHashSet();

		for (int i = 0; i < classpathPaths.size(); i++) {
			String classpathPath = classpathPaths.get(i);
			if (addAndCheckDuplicates(classpathPath, seenPaths)) {
				File pathFile;
				if (FileUtils.isRelativePath(classpathPath)) {
					pathFile = new File(defaultDirectory, classpathPath);
				} else {
					pathFile = new File(classpathPath);
				}

				if (pathFile.exists()) {
					if (pathFile.isDirectory()) {
						expandedRoots.add(pathFile);
					} else {
						try {
							ZipFileWrapper archiveWrapper = new ZipFileWrapper(pathFile);
							expandedRoots.add(archiveWrapper);
							if (Options.ZKM_ADD_MANIFEST_CLASSPATHS) {
								readManifestClassPath(archiveWrapper, classpathPaths, invalidPaths);
							}

							extractNestedArchives(archiveWrapper, expandedRoots, invalidPaths, managedTempFiles, logger);
						} catch (ZipException ignoredZipException) {
							invalidPaths.add(pathFile.getAbsolutePath());
						} catch (IOException ignoredIoException) {
							invalidPaths.add(pathFile.getAbsolutePath());
						}
					}
				} else {
					invalidPaths.add(pathFile.getAbsolutePath());
				}
			}
		}

		return expandedRoots;
	}

	public static boolean archiveContainsClass(String archivePath, String internalName) {
		ZipFileWrapper archiveWrapper = null;
		File archiveFile = new File(archivePath);

		try {
			archiveWrapper = new ZipFileWrapper(archiveFile);
		} catch (IOException ignoredOpenFailure) {
			if (archiveWrapper != null) {
				try {
					archiveWrapper.close();
				} catch (IOException ignoredCloseFailure) {
				}
			}

			return false;
		}

		String classFileName = internalNameToClassFileName(internalName, false);
		ZipEntry classEntry = archiveWrapper.getEntry(classFileName);
		boolean classFound;
		if (classEntry != null && !classEntry.isDirectory()) {
			classFound = true;
		} else {
			classFound = false;
		}

		if (archiveWrapper != null) {
			try {
				archiveWrapper.close();
			} catch (IOException ignoredFinalCloseFailure) {
			}
		}

		return classFound;
	}

	public static List<Object> parseAndExpandClasspath(String rawClasspath, Set<String> invalidPaths) {
		return parseAndExpandClasspath(rawClasspath, invalidPaths, null, null);
	}

	private ClasspathResource findClassInZip(ZipFile archive, String internalName) {
		String classFileName = internalNameToClassFileName(internalName, false);
		ZipEntry classEntry = archive.getEntry(classFileName);
		if (classEntry == null) {
			for (String springBootClassDir : SPRING_BOOT_CLASS_DIRS) {
				String candidateEntryName = springBootClassDir + classFileName;
				classEntry = archive.getEntry(candidateEntryName);
				if (classEntry != null) {
					break;
				}
			}
		}

		if (classEntry != null && !classEntry.isDirectory()) {
			File archiveFile = new File(archive.getName());
			if (TempFileManager.isManagedTempFile(archiveFile)) {
				String originalArchiveName = TempFileManager.getOriginalName(archive.getName());
				if (originalArchiveName != null) {
					ArchiveNode archiveNode = new ArchiveNode(archiveFile.getAbsolutePath(), archiveFile.length(), originalArchiveName);
					return new ClasspathResource(archive, classEntry, archiveNode);
				}
			}

			return new ClasspathResource(archive, classEntry);
		} else {
			return null;
		}
	}

	public boolean packageExists(String packageName) {
		String packagePath = packageName.replace('/', FILE_SEPARATOR_CHAR);

		for (int i = 0; i < this.classpathRoots.size(); i++) {
			Object classpathRoot = this.classpathRoots.get(i);
			if (classpathRoot instanceof File) {
				File classpathRootDirectory = (File)classpathRoot;
				String packageDirectoryPath = classpathRootDirectory.getAbsolutePath() + FILE_SEPARATOR + packagePath;
				File packageDirectory = new File(packageDirectoryPath);
				if (packageDirectory.exists() && packageDirectory.isDirectory()) {
					return true;
				}
			} else if (classpathRoot instanceof ZipFile) {
				ZipEntry packageEntry = ((ZipFile)classpathRoot).getEntry(packageName);
				if (packageEntry != null && packageEntry.isDirectory()) {
					return true;
				}
			}
		}

		return false;
	}

	public void close() {
		if (this.classpathRoots != null) {
			for (int i = 0; i < this.classpathRoots.size(); i++) {
				Object classpathRoot = this.classpathRoots.get(i);
				if (classpathRoot instanceof ZipFile) {
					ZipFile classpathRootArchive = (ZipFile)classpathRoot;

					try {
						classpathRootArchive.close();
					} catch (IOException ignoredRootArchiveClose) {
					}
				}
			}
		}

		for (ZipFile systemJar : this.systemJarCache.values()) {
			try {
				systemJar.close();
			} catch (IOException ignoredSystemJarClose) {
			}
		}

		if (this.managedTempFiles != null) {
			for (String managedTempFile : this.managedTempFiles) {
				try {
					new File(managedTempFile).delete();
				} catch (DebugAssertFailedException debugAssertFailure) {
					throw debugAssertFailure;
				} catch (Throwable ignoredTempFileDelete) {
				}
			}
		}
	}

	public ClasspathResource findResource(String internalName, boolean searchSystem) {
		Object cachedResource = this.resourceCache.get(internalName);
		if (cachedResource != null) {
			if (cachedResource == NOT_FOUND_MARKER) {
				return null;
			}

			if (cachedResource instanceof File) {
				return new ClasspathResource((File)cachedResource);
			}

			if (cachedResource instanceof KeyValuePair) {
				KeyValuePair<?, ?> cachedArchiveEntry = (KeyValuePair<?, ?>)cachedResource;
				ZipFile cachedArchive = (ZipFile)cachedArchiveEntry.getKey();
				ZipEntry cachedEntry = (ZipEntry)cachedArchiveEntry.getValue();
				return new ClasspathResource(cachedArchive, cachedEntry);
			}
		}

		ClasspathResource resource = null;

		for (int i = 0; i < this.classpathRoots.size(); i++) {
			Object classpathRoot = this.classpathRoots.get(i);
			if (classpathRoot instanceof File) {
				resource = this.findClassInDirectory((File)classpathRoot, internalName);
				if (resource != null) {
					break;
				}
			} else if (classpathRoot instanceof ZipFile) {
				resource = this.findClassInZip((ZipFile)classpathRoot, internalName);
				if (resource != null) {
					break;
				}
			}
		}

		if (searchSystem && resource == null) {
			URL classUrl = this.getClass().getResource("/" + internalName.replace('.', '/') + ".class");
			if (classUrl != null) {
				String protocol = classUrl.getProtocol();
				String urlPath = FileUtils.decodeURLString(classUrl.getPath());
				if (protocol.equalsIgnoreCase("jar")) {
					File archiveFile;
					if (urlPath.substring(0, 5).equalsIgnoreCase("file:")) {
						String pathWithoutScheme = urlPath.substring(5, urlPath.length());
						int jarSeparatorIndex = pathWithoutScheme.indexOf(33);
						DebugUtils.assertTrue(jarSeparatorIndex > 0, new String[]{classUrl.toString()});
						DebugUtils.assertTrue(jarSeparatorIndex == pathWithoutScheme.lastIndexOf(33), new String[]{classUrl.toString()});
						String jarPath = pathWithoutScheme.substring(0, jarSeparatorIndex);
						archiveFile = new File(jarPath);
					} else {
						archiveFile = new File(urlPath);
					}

					if (archiveFile.exists() && !archiveFile.isDirectory()) {
						try {
							String archivePath = archiveFile.getAbsolutePath();
							ZipFileWrapper archiveWrapper;
							if (this.systemJarCache.containsKey(archivePath)) {
								archiveWrapper = this.systemJarCache.get(archivePath);
							} else {
								archiveWrapper = new ZipFileWrapper(archiveFile);
								this.systemJarCache.put(archivePath, archiveWrapper);
							}

							resource = this.findClassInZip(archiveWrapper, internalName);
						} catch (IOException ignored) {
						}
					}
				} else if (protocol.equalsIgnoreCase("file")) {
					File resourceFile = new File(urlPath);
					if (resourceFile.exists() && !resourceFile.isDirectory()) {
						resource = new ClasspathResource(resourceFile);
					}
				}
			}
		}

		if (this.enableCaching && resource == null) {
			this.resourceCache.put(internalName, NOT_FOUND_MARKER);
		}

		return resource;
	}

	private ClasspathResource findClassInDirectory(File directory, String internalName) {
		String classFilePath = directory.getAbsolutePath() + FILE_SEPARATOR + internalNameToClassFileName(internalName, true);
		File classFile = new File(classFilePath);
		return classFile.exists() && !classFile.isDirectory() ? new ClasspathResource(classFile) : null;
	}

	private void initialize(String rawClasspath, AbstractLogger logger) {
		this.rawClasspathString = rawClasspath;
		this.invalidPaths = new LinkedHashSet<>();
		this.classpathRoots = expandClasspathInternal(rawClasspath, this.invalidPaths, this.managedTempFiles, null, logger);
		this.resourceCache = Options.ZKM_USE_PARALLEL ? new ConcurrentHashMap<>() : GlobalUtils.newHashMap();
	}

	public ClasspathResource findResourceInSelfJar(String internalName) {
		CodeSource codeSource = this.getClass().getProtectionDomain().getCodeSource();
		if (codeSource != null) {
			URL locationUrl = codeSource.getLocation();
			if (locationUrl != null) {
				String protocol = locationUrl.getProtocol();
				String urlPath = FileUtils.decodeURLString(locationUrl.getPath());
				if (protocol.equalsIgnoreCase("file")) {
					File selfFile = new File(urlPath);
					if (selfFile.exists()) {
						ClasspathResource fileProtocolResource = null;
						if (selfFile.isDirectory()) {
							fileProtocolResource = this.findClassInDirectory(selfFile, internalName);
						} else {
							try {
								ZipFileWrapper selfArchiveWrapper = new ZipFileWrapper(selfFile);
								fileProtocolResource = this.findClassInZip(selfArchiveWrapper, internalName);
							} catch (IOException ignoredSelfArchiveOpen) {
							}
						}

						return fileProtocolResource;
					}
				} else if (protocol.equalsIgnoreCase("jar")) {
					File archiveFile;
					if (urlPath.substring(0, 5).equalsIgnoreCase("file:")) {
						String pathWithoutScheme = urlPath.substring(5, urlPath.length());
						int jarSeparatorIndex = pathWithoutScheme.indexOf(33);
						DebugUtils.assertTrue(jarSeparatorIndex > 0, new String[]{locationUrl.toString()});
						DebugUtils.assertTrue(jarSeparatorIndex == pathWithoutScheme.lastIndexOf(33), new String[]{locationUrl.toString()});
						String jarPath = pathWithoutScheme.substring(0, jarSeparatorIndex);
						archiveFile = new File(jarPath);
					} else {
						archiveFile = new File(urlPath);
					}

					if (archiveFile.exists()) {
						ClasspathResource jarProtocolResource = null;

						try {
							ZipFileWrapper jarArchiveWrapper = new ZipFileWrapper(archiveFile);
							jarProtocolResource = this.findClassInZip(jarArchiveWrapper, internalName);
						} catch (IOException ignoredJarArchiveOpen) {
						}

						return jarProtocolResource;
					}
				} else {
					DebugUtils.assertTrue(false, new String[]{protocol});
				}
			}
		}

		return null;
	}
}

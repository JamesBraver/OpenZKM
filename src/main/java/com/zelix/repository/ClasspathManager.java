package com.zelix.repository;

import com.zelix.event.ChangeTrackingNotifier;
import com.zelix.exceptions.FileException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.EnvironmentUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.TempFileManager;
import com.zelix.utils.ZipFileWrapper;
import com.zelix.utils.references.AtomicReference;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class ClasspathManager extends ChangeTrackingNotifier {
	public static String OBJECT_CLASS_FILE_PATH = "java/lang/Object".replace('/', SystemConstants.fileSep) + ".class";
	private static final String JRT_FS_JAR_NAME = "lib" + SystemConstants.fileSeparator + "jrt-fs.jar";
	private String rawClasspath;
	private Integer releaseVersion;
	private FileSystem jrtFileSystem;
	private ReferenceGraph<String, String> jrtPackageGraph;
	private boolean ownsJrtFileSystem;
	private ClassRepository classRepository;
	private boolean isJrtInitialized;
	private boolean isBootClasspathInitialized;

	public ClasspathManager(String rawClasspath, Set<String> invalidPaths) {
		this(rawClasspath, null, invalidPaths);
	}

	public ClasspathManager(String rawClasspath, Integer releaseVersion) {
		this(rawClasspath, releaseVersion, GlobalUtils.newHashSet());
	}

	public ClasspathManager(String rawClasspath) {
		this(rawClasspath, null, GlobalUtils.newHashSet());
	}

	public ClasspathManager(String rawClasspath, Integer releaseVersion, Set<String> invalidPaths) {
		this.rawClasspath = normalizeAndExpandClasspath(rawClasspath, invalidPaths);
		this.releaseVersion = releaseVersion;
	}

	private static String formatInvalidPathMessage(Set<String> invalidPaths) {
		StringBuilder messageBuilder = new StringBuilder();
		int pathCount = invalidPaths.size();
		int pathIndex = 0;

		for (String invalidPath : invalidPaths) {
			messageBuilder.append("\"");
			messageBuilder.append(invalidPath);
			messageBuilder.append("\"");
			if (pathIndex + 2 < pathCount) {
				messageBuilder.append(", ");
			} else if (pathIndex + 1 < pathCount) {
				messageBuilder.append(" and ");
			}

			pathIndex++;
		}

		if (invalidPaths.size() > 1) {
			messageBuilder.append(" are not valid paths.");
		} else {
			messageBuilder.append(" is not a valid path.");
		}

		if (hasManifestDerivedPaths(invalidPaths)) {
			messageBuilder.append(" Any path marked with '");
			messageBuilder.append(ClasspathScanner.MANIFEST_PATH_MARKER);
			messageBuilder.append("' was derived from the \"Class-Path\" attribute contained in the manifest of one or more JARs.");
		}

		return messageBuilder.toString();
	}

	private static FileSystem getSystemJrtFileSystem() {
		FileSystem systemJrtFileSystem = null;

		try {
			systemJrtFileSystem = FileSystems.getFileSystem(URI.create("jrt:/"));
		} catch (ProviderNotFoundException ignored) {
		}

		return systemJrtFileSystem;
	}

	public static String formatClasspathTempFileNames(String classpath) {
		StringBuilder resolvedClasspathBuilder = new StringBuilder();
		StringTokenizer classpathTokenizer = new StringTokenizer(classpath, SystemConstants.pathSeparator);

		while (classpathTokenizer.hasMoreTokens()) {
			String classpathElement = classpathTokenizer.nextToken();
			resolvedClasspathBuilder.append(classpathElement);
			if (TempFileManager.isManagedTempFile(classpathElement)) {
				resolvedClasspathBuilder.append('(');
				resolvedClasspathBuilder.append(TempFileManager.getOriginalName(classpathElement));
				resolvedClasspathBuilder.append(')');
			}

			if (classpathTokenizer.hasMoreTokens()) {
				resolvedClasspathBuilder.append(SystemConstants.pathSep);
			}
		}

		return resolvedClasspathBuilder.toString();
	}

	public static boolean archiveContainsObject(String archivePath) {
		return archiveContainsObject(new File(archivePath));
	}

	public static String filterExistingPaths(String classpath) {
		StringBuffer filteredClasspathBuilder = new StringBuffer();
		StringTokenizer classpathTokenizer = new StringTokenizer(classpath, SystemConstants.pathSeparator);

		while (classpathTokenizer.hasMoreTokens()) {
			String classpathElement = classpathTokenizer.nextToken();
			if (new File(classpathElement).exists()) {
				if (filteredClasspathBuilder.length() > 0) {
					filteredClasspathBuilder.append(SystemConstants.pathSeparator);
				}

				filteredClasspathBuilder.append(classpathElement);
			}
		}

		return filteredClasspathBuilder.toString();
	}

	private static String normalizeAndExpandClasspath(String rawClasspath) {
		return normalizeAndExpandClasspath(rawClasspath, GlobalUtils.newHashSet());
	}

	public static boolean archiveContainsObject(File archiveFile) {
		return archiveFile.exists() && ClasspathScanner.archiveContainsClass(archiveFile.getAbsolutePath(), "java/lang/Object");
	}

	private static FileSystem createJrtFileSystem(String javaHome) {
		FileSystem createdFileSystem = null;

		try {
			if (EnvironmentUtils.isRuntimeJava9OrNewer()) {
				HashMap<String, String> environmentMap = GlobalUtils.newHashMap(13);
				environmentMap.put("java.home", javaHome);
				createdFileSystem = FileSystems.newFileSystem(URI.create("jrt:/"), environmentMap);
			} else {
				URL jrtFsJarUrl = Paths.get(javaHome, "lib", "jrt-fs.jar").toUri().toURL();
				URLClassLoader jrtFsClassLoader = new URLClassLoader(new URL[]{jrtFsJarUrl});
				createdFileSystem = FileSystems.newFileSystem(URI.create("jrt:/"), Collections.emptyMap(), jrtFsClassLoader);
			}
		} catch (IOException ignored) {
		}

		return createdFileSystem;
	}

	public static boolean hasManifestDerivedPaths(Set<String> invalidPaths) {
		if (invalidPaths.size() > 0) {
			Iterator<String> invalidPathIterator = invalidPaths.iterator();

			while (invalidPathIterator.hasNext()) {
				if (invalidPathIterator.next().endsWith(ClasspathScanner.MANIFEST_PATH_MARKER)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean normalizeAndExpandClasspath(String rawClasspath, File defaultDirectory, AtomicReference<String> resolvedClasspathRef, AbstractLogger logger, AtomicReference<String> errorMessageRef) {
		HashSet<String> invalidPaths = GlobalUtils.newHashSet();
		List<?> classpathEntries = ClasspathScanner.parseAndExpandClasspath(rawClasspath, invalidPaths, defaultDirectory, logger);
		StringBuilder classpathBuilder = new StringBuilder();

		for (int entryIndex = 0; entryIndex < classpathEntries.size(); entryIndex++) {
			Object classpathEntry = classpathEntries.get(entryIndex);
			boolean entryAppended = false;
			if (classpathEntry instanceof File) {
				classpathBuilder.append(((File)classpathEntries.get(entryIndex)).getAbsolutePath());
				entryAppended = true;
			} else {
				ZipFileWrapper zipWrapper = (ZipFileWrapper)classpathEntries.get(entryIndex);
				if (!TempFileManager.isManagedTempFile(zipWrapper.getName())) {
					classpathBuilder.append(zipWrapper.getName());
					entryAppended = true;
				}
			}

			if (entryIndex + 1 < classpathEntries.size() && entryAppended) {
				classpathBuilder.append(SystemConstants.pathSeparator);
			}
		}

		for (int wrapperIndex = 0; wrapperIndex < classpathEntries.size(); wrapperIndex++) {
			if (classpathEntries.get(wrapperIndex) instanceof ZipFileWrapper) {
				try {
					((ZipFileWrapper)classpathEntries.get(wrapperIndex)).close();
				} catch (IOException ignored) {
				}
			}
		}

		resolvedClasspathRef.set(classpathBuilder.toString());
		if (invalidPaths.size() > 0) {
			errorMessageRef.set(formatInvalidPathMessage(invalidPaths));
			return false;
		} else {
			return true;
		}
	}

	public static boolean isValidBootClasspath(String bootClasspath) {
		StringTokenizer bootClasspathTokenizer = new StringTokenizer(bootClasspath, SystemConstants.pathSeparator);

		while (bootClasspathTokenizer.hasMoreTokens()) {
			String classpathElement = bootClasspathTokenizer.nextToken();
			File elementFile = new File(classpathElement);
			if (elementFile.exists()) {
				if (elementFile.isDirectory()) {
					String objectClassFilePath = (classpathElement.endsWith(SystemConstants.fileSeparator) ? classpathElement : classpathElement + SystemConstants.fileSeparator) + OBJECT_CLASS_FILE_PATH;
					if (new File(objectClassFilePath).exists()) {
						return true;
					}
				} else if (FileUtils.isArchiveExtension(classpathElement) && archiveContainsObject(classpathElement)) {
					return true;
				}
			}
		}

		return false;
	}

	private static String findLegacyBootClasspath() {
		String legacyBootClasspath = null;
		if (SystemConstants.bootClasspath != null && SystemConstants.bootClasspath.length() > 0 && isValidBootClasspath(SystemConstants.bootClasspath)) {
			legacyBootClasspath = filterExistingPaths(SystemConstants.bootClasspath);
		}

		if (legacyBootClasspath == null && SystemConstants.javaHome != null && SystemConstants.javaHome.length() > 0) {
			File javaHomeDirectory = new File(SystemConstants.javaHome);
			if (javaHomeDirectory.exists() && javaHomeDirectory.isDirectory()) {
				String libDirectoryPath = javaHomeDirectory.getAbsolutePath() + SystemConstants.fileSeparator + "lib";
				File libDirectory = new File(libDirectoryPath);
				if (libDirectory.exists() && libDirectory.isDirectory()) {
					File rtJarFile = new File(libDirectory, "rt.jar");
					if (archiveContainsObject(rtJarFile)) {
						legacyBootClasspath = rtJarFile.getAbsolutePath();
					} else {
						File classesZipFile = new File(libDirectory, "classes.zip");
						if (archiveContainsObject(classesZipFile)) {
							legacyBootClasspath = classesZipFile.getAbsolutePath();
						}
					}
				}
			}
		}

		return legacyBootClasspath;
	}

	private static String normalizeAndExpandClasspath(String rawClasspath, Set<String> invalidPaths) {
		List<?> classpathEntries = ClasspathScanner.parseAndExpandClasspath(rawClasspath, invalidPaths);
		StringBuilder classpathBuilder = new StringBuilder();

		for (int entryIndex = 0; entryIndex < classpathEntries.size(); entryIndex++) {
			Object classpathEntry = classpathEntries.get(entryIndex);
			boolean entryAppended = false;
			if (classpathEntry instanceof File) {
				classpathBuilder.append(((File)classpathEntries.get(entryIndex)).getAbsolutePath());
				entryAppended = true;
			} else {
				ZipFileWrapper zipWrapper = (ZipFileWrapper)classpathEntries.get(entryIndex);
				if (Options.ZKM_ADD_NESTED_CLASSPATH_ARCHIVES || !TempFileManager.isManagedTempFile(zipWrapper.getName())) {
					classpathBuilder.append(zipWrapper.getName());
					entryAppended = true;
				}
			}

			if (entryIndex + 1 < classpathEntries.size() && entryAppended) {
				classpathBuilder.append(SystemConstants.pathSep);
			}
		}

		for (int wrapperIndex = 0; wrapperIndex < classpathEntries.size(); wrapperIndex++) {
			if (classpathEntries.get(wrapperIndex) instanceof ZipFileWrapper) {
				try {
					((ZipFileWrapper)classpathEntries.get(wrapperIndex)).close();
				} catch (IOException ignored) {
				}
			}
		}

		return classpathBuilder.toString();
	}

	private boolean buildJrtPackageGraph() {
		Path packagesPath = this.jrtFileSystem.getPath("packages");
		this.jrtPackageGraph = new ReferenceGraph<>();

		try {
			try (DirectoryStream<Path> packageDirectoryStream = Files.newDirectoryStream(packagesPath)) {
				for (Path packageDirectory : packageDirectoryStream) {
					for (Path moduleDirectory : Files.newDirectoryStream(packageDirectory)) {
						String packageName = packageDirectory.getFileName().toString().replace('.', '/');
						String modulePath = "/modules/" + moduleDirectory.getFileName().toString();
						this.jrtPackageGraph.addReference(packageName, modulePath);
					}
				}
			}

			return true;
		} catch (IOException | DirectoryIteratorException ignored) {
			return false;
		}
	}

	public String setClasspath(String classpath) {
		String previousClasspath = this.rawClasspath;
		this.rawClasspath = normalizeAndExpandClasspath(classpath);
		this.markChanged();
		this.notify(this.rawClasspath, null, null);
		return previousClasspath;
	}

	public ClassRepository getClassRepository() {
		return this.classRepository;
	}

	public void closeJrtFileSystem() {
		if (this.jrtFileSystem != null) {
			if (this.ownsJrtFileSystem) {
				try {
					this.jrtFileSystem.close();
				} catch (IOException ignored) {
				}
			}

			this.jrtFileSystem = null;
			this.jrtPackageGraph = null;
			this.isJrtInitialized = false;
			this.ownsJrtFileSystem = false;
			this.isBootClasspathInitialized = false;
		}
	}

	public String appendClasspath(String classpath) {
		String previousClasspath = this.rawClasspath;
		StringBuilder classpathBuilder = new StringBuilder(this.rawClasspath);
		if (classpathBuilder.length() > 0 && classpathBuilder.charAt(classpathBuilder.length() - 1) != SystemConstants.pathSep) {
			classpathBuilder.append(SystemConstants.pathSep);
		}

		classpathBuilder.append(normalizeAndExpandClasspath(classpath));
		this.rawClasspath = classpathBuilder.toString();
		this.markChanged();
		this.notify(this.rawClasspath, null, null);
		return previousClasspath;
	}

	public boolean isJrtInitialized() {
		return this.isJrtInitialized;
	}

	public String getResolvedClasspath() {
		return formatClasspathTempFileNames(this.rawClasspath);
	}

	public byte[] readFromJrtFileSystem(String internalName, AtomicReference<Path> pathRef) throws IOException {
		pathRef.setNull();
		if (!this.isJrtInitialized) {
			return null;
		} else {
			String packageName = AbstractClassNode.getPackageName(internalName);
			if (packageName != null && packageName.length() > 0) {
				Set<String> modulePaths = this.jrtPackageGraph.getReferences(packageName);
				if (modulePaths == null) {
					return null;
				} else {
					StringBuilder classFilePathBuilder = new StringBuilder();
					Path resolvedPath = null;

					for (String modulePath : modulePaths) {
						classFilePathBuilder.setLength(0);
						classFilePathBuilder.append(internalName);
						classFilePathBuilder.append(".class");
						Path candidatePath = this.jrtFileSystem.getPath(modulePath, classFilePathBuilder.toString());
						if (Files.exists(candidatePath)) {
							resolvedPath = candidatePath;
							break;
						}
					}

					if (resolvedPath != null) {
						pathRef.set(resolvedPath);
						return Files.readAllBytes(resolvedPath);
					} else {
						return null;
					}
				}
			} else {
				return null;
			}
		}
	}

	private String findJavaHomeFromClasspath() {
		String javaHome = null;
		String jrtFsJarName = JRT_FS_JAR_NAME;

		for (String classpathElement : this.rawClasspath.split(SystemConstants.pathSeparator)) {
			if (FileUtils.IS_UNIX ? classpathElement.endsWith(jrtFsJarName) : classpathElement.toLowerCase().endsWith(jrtFsJarName)) {
				File jrtFsJarFile = new File(classpathElement);
				if (jrtFsJarFile.exists() && !jrtFsJarFile.isDirectory()) {
					File javaHomeDirectory = jrtFsJarFile.getParentFile().getParentFile();
					if (javaHomeDirectory != null) {
						javaHome = javaHomeDirectory.getAbsolutePath();
						break;
					}
				}
			}
		}

		return javaHome;
	}

	public Integer getReleaseVersion() {
		return this.releaseVersion;
	}

	public boolean isBootClasspathInitialized() {
		return this.isBootClasspathInitialized;
	}

	public String prependClasspath(String classpath) {
		String previousClasspath = this.rawClasspath;
		StringBuilder classpathBuilder = new StringBuilder(normalizeAndExpandClasspath(classpath));
		if (classpathBuilder.length() > 0 && classpathBuilder.charAt(classpathBuilder.length() - 1) != SystemConstants.pathSep) {
			classpathBuilder.append(SystemConstants.pathSep);
		}

		classpathBuilder.append(this.rawClasspath);
		this.rawClasspath = classpathBuilder.toString();
		this.markChanged();
		this.notify(this.rawClasspath, null, null);
		return previousClasspath;
	}

	public String getRawClasspath() {
		return this.rawClasspath;
	}

	public boolean initializeBootClasspath(AtomicReference<String> jrtFsRef, AtomicReference<String> legacyBootCpRef) {
		this.isBootClasspathInitialized = false;
		this.isJrtInitialized = false;
		boolean bootClasspathResolved = false;

		try {
			this.classRepository.loadClassInternalWithSystem("java/lang/Object", this.releaseVersion, true, null, false);
			bootClasspathResolved = true;
		} catch (FileException ignored) {
		}

		if (!bootClasspathResolved && EnvironmentUtils.isRuntimeJava8OrNewer()) {
			String javaHome = this.findJavaHomeFromClasspath();
			if (javaHome != null) {
				this.jrtFileSystem = createJrtFileSystem(javaHome);
				if (this.jrtFileSystem != null) {
					bootClasspathResolved = this.buildJrtPackageGraph();
					this.isJrtInitialized = bootClasspathResolved;
					this.ownsJrtFileSystem = true;
					if (this.isJrtInitialized) {
						jrtFsRef.set(javaHome + SystemConstants.fileSeparator + JRT_FS_JAR_NAME);
					}
				}
			}
		}

		if (!bootClasspathResolved) {
			String legacyBootClasspath = findLegacyBootClasspath();
			if (legacyBootClasspath != null) {
				this.appendClasspath(legacyBootClasspath);
				legacyBootCpRef.set(legacyBootClasspath);
				bootClasspathResolved = true;
			}
		}

		if (!bootClasspathResolved && EnvironmentUtils.isRuntimeJava9OrNewer()) {
			this.jrtFileSystem = getSystemJrtFileSystem();
			if (this.jrtFileSystem != null) {
				bootClasspathResolved = this.buildJrtPackageGraph();
				this.isJrtInitialized = bootClasspathResolved;
				this.ownsJrtFileSystem = false;
				if (this.isJrtInitialized) {
					legacyBootCpRef.set("jrt:/");
				}
			}
		}

		this.isBootClasspathInitialized = bootClasspathResolved;
		return bootClasspathResolved;
	}

	public List<String> getClasspathElements() {
		ArrayList<String> classpathElements = new ArrayList<>();
		StringTokenizer classpathTokenizer = new StringTokenizer(this.rawClasspath, SystemConstants.pathSeparator);

		while (classpathTokenizer.hasMoreTokens()) {
			String classpathElement = classpathTokenizer.nextToken();
			if (!TempFileManager.isManagedTempFile(classpathElement)) {
				classpathElements.add(classpathElement);
			}
		}

		return classpathElements;
	}

	public void setClassRepository(ClassRepository classRepository) {
		this.classRepository = classRepository;
	}
}

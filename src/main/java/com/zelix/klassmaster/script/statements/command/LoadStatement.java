package com.zelix.klassmaster.script.statements.command;

import com.zelix.enums.LoadTypes;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.filter.FileFilter;
import com.zelix.klassmaster.script.statements.javaccInterface.PathFilter;
import com.zelix.klassmaster.script.statements.parameter.LoadParameter;
import com.zelix.klassmaster.script.statements.parameter.OpenNestedArchivesParameter;
import com.zelix.klassmaster.script.statements.token.SkipArchivePath;
import com.zelix.klassmaster.script.statements.token.StringLiteral;
import com.zelix.klassmaster.script.statements.token.UnSkipArchivePath;
import com.zelix.logging.AbstractLogger;
import com.zelix.logging.LogContextLogger;
import com.zelix.utils.*;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.function.EmptyConsumer;
import com.zelix.utils.io.ClassFilenameFilter;
import com.zelix.utils.io.DirectoryScanner;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.Workspace;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;

public class LoadStatement extends ScriptCommand {
	private boolean processNestedArchives = true;
	private List<PathConfig> archiveConfigs;
	private List<String> simpleSkipPatterns;
	private List<String> complexUnskipPatterns;
	private List<String> complexSkipPatterns;
	private List<String> simpleUnskipPatterns;

	public LoadStatement(int nodeType) {
		super(nodeType);
	}

	private void resolvePathWithWildcards(String path, Map<String, String> resolvedPaths, LoadTypes loadType, ScriptContext scriptContext) {
		String normalizedPath = FileUtils.normalizePathSeparators(path);
		File pathFile;
		if (FileUtils.isRelativePath(normalizedPath)) {
			pathFile = new File(scriptContext.getDefaultDirectory(), normalizedPath);
		} else {
			pathFile = new File(normalizedPath);
		}

		if (pathFile.exists()) {
			resolvedPaths.put(pathFile.getAbsolutePath(), normalizedPath);
		} else if (normalizedPath.indexOf("*") > -1
			&& (
				FileUtils.isArchiveExtensionLenient(normalizedPath)
					|| FileUtils.isXmlFile(normalizedPath)
					|| FileUtils.isYamlFile(normalizedPath)
					|| FileUtils.isPropertiesFile(normalizedPath)
					|| normalizedPath.toLowerCase().endsWith(".class")
			)) {
			AtomicReference<String> errorMessage = new AtomicReference<>();
			List<String> matchedPaths = FileUtils.findFilesByPath(normalizedPath, scriptContext.getDefaultDirectory(), errorMessage, true);
			if (!errorMessage.isNull()) {
				scriptContext.logWarning(errorMessage.get() + " : Specified path was " + loadType.getPrefix() + "\"" + path + "\". Default directory was '" + scriptContext.getDefaultDirectory() + "'");
			}

			if (matchedPaths.size() > 0) {
				for (String matchedPath : matchedPaths) {
					resolvedPaths.put(matchedPath, normalizedPath);
				}
			} else {
				scriptContext.logWarning("No paths matched " + loadType.getPrefix() + "\"" + path + "\". Default directory was '" + scriptContext.getDefaultDirectory() + "'");
			}
		} else if (normalizedPath.endsWith("*")) {
			String pathPrefix = normalizedPath.substring(0, normalizedPath.length() - 1);
			if (FileUtils.isRelativePath(pathPrefix)) {
				pathFile = new File(scriptContext.getDefaultDirectory(), pathPrefix);
			} else {
				pathFile = new File(pathPrefix);
			}

			if (pathFile.exists() && pathFile.isDirectory()) {
				resolvedPaths.put(pathFile.getAbsolutePath(), normalizedPath);
				List<String> subdirectories = new DirectoryScanner(pathFile).getSubdirectories();
				int subdirectoryCount = subdirectories.size();

				for (int i = 0; i < subdirectoryCount; i++) {
					String subdirectory = subdirectories.get(i);
					resolvedPaths.put(subdirectory, normalizedPath);
				}
			} else if (pathFile.exists()) {
				scriptContext.logFatalError("\"" + pathFile.getAbsolutePath() + "\" is not a directory");
			} else {
				scriptContext.logFatalError(
					"\""
						+ pathFile.getAbsolutePath()
						+ "\" does not exist"
						+ (normalizedPath.endsWith("**") ? ". '**' not supported." : "")
						+ " Specified path was '"
						+ path
						+ "' Default directory was '"
						+ scriptContext.getDefaultDirectory()
						+ "'. (1)"
				);
			}
		} else {
			switch (loadType) {
				case TOP_LEVEL:
					scriptContext.logFatalError(
						"\"" + pathFile.getAbsolutePath() + "\" does not exist. Specified path was '" + path + "' Default directory was '" + scriptContext.getDefaultDirectory() + "'. (2)"
					);
					break;
				case SKIP:
				case UNSKIP:
					scriptContext.logWarning(
						"\""
							+ pathFile.getAbsolutePath()
							+ "\" does not exist. Specified path was "
							+ loadType.getPrefix()
							+ "\""
							+ path
							+ "\". Default directory was '"
							+ scriptContext.getDefaultDirectory()
							+ "'. (2)"
					);
			}
		}
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		scriptContext.buildPhantomClassManager();
		int childCount = this.getChildrenCount();
		int initialMessageCount = scriptContext.getMessageCount();
		int initialWarningCount = scriptContext.getWarningCount();
		int initialErrorCount = scriptContext.getErrorCount();
		LinkedHashSet<String> archivePaths = new LinkedHashSet<>();
		LinkedHashSet<String> simpleSkipPaths = new LinkedHashSet<>();
		LinkedHashSet<String> simpleUnskipPaths = new LinkedHashSet<>();
		LinkedHashSet<String> complexSkipPaths = new LinkedHashSet<>();
		LinkedHashSet<String> complexUnskipPaths = new LinkedHashSet<>();
		ArrayList<PathFilter> pathFilters = new ArrayList<>();
		ArrayList<String> expectedInitialHashes = new ArrayList<>();
		ArrayList<String> expectedFinalHashes = new ArrayList<>();

		for (int i = 0; i < childCount; i++) {
			ScriptNode child = this.getChild(i);
			child.parseArguments(this, scriptContext);
			if (child instanceof TokenNode) {
				String pathText = ((TokenNode)child).getText().trim();
				if (child instanceof StringLiteral) {
					this.addPathToSet(pathText, archivePaths, scriptContext);
					pathFilters.add(PathFilter.MATCH_ALL);
					expectedInitialHashes.add(null);
					expectedFinalHashes.add(null);
				} else if (child instanceof SkipArchivePath) {
					if (pathText.indexOf("!") == -1) {
						this.addPathToSet(pathText, simpleSkipPaths, scriptContext);
					} else {
						String normalizedSkipPath = StringUtils.replace(pathText.replace('\\', '/'), "!/", "!");
						if (!normalizedSkipPath.equals(pathText)) {
							scriptContext.logMessage("-\"" + pathText + "\" changed to -\"" + normalizedSkipPath + "\" in \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + ".");
						}

						this.addPathToSet(normalizedSkipPath, complexSkipPaths, scriptContext);
					}
				} else if (child instanceof UnSkipArchivePath) {
					if (pathText.indexOf("!") == -1) {
						this.addPathToSet(pathText, simpleUnskipPaths, scriptContext);
					} else {
						String normalizedUnskipPath = StringUtils.replace(pathText.replace('\\', '/'), "!/", "!");
						if (!normalizedUnskipPath.equals(pathText)) {
							scriptContext.logMessage("+\"" + pathText + "\" changed to +\"" + normalizedUnskipPath + "\" in \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + ".");
						}

						this.addPathToSet(normalizedUnskipPath, complexUnskipPaths, scriptContext);
					}
				} else {
					scriptContext.logFatalError(
						"'" + this.getCommandName() + "' statement had " + child.getClass().getName() + " as child " + i + " at line " + this.getLineNumber() + ". (1)"
					);
				}
			} else if (child instanceof FileFilter) {
				pathFilters.set(pathFilters.size() - 1, (PathFilter)child);
			} else if (child instanceof LoadParameter) {
				LoadParameter loadParameter = (LoadParameter)child;
				if (loadParameter.getParameterName().equals("expectedInitial_SHA256")) {
					String existingInitialHash = expectedInitialHashes.get(expectedInitialHashes.size() - 1);
					if (existingInitialHash != null) {
						scriptContext.logWarning(
							"Extra 'expectedInitial_SHA256' value '"
								+ loadParameter.getValue(0)
								+ "' in '"
								+ this.getCommandName()
								+ "' statement. First value '"
								+ existingInitialHash
								+ "' will be used."
						);
					} else {
						expectedInitialHashes.set(expectedInitialHashes.size() - 1, loadParameter.getValue(0));
					}
				} else {
					String existingFinalHash = expectedFinalHashes.get(expectedFinalHashes.size() - 1);
					if (existingFinalHash != null) {
						scriptContext.logWarning(
							"Extra 'expectedFinal_SHA256' value '"
								+ loadParameter.getValue(0)
								+ "' in '"
								+ this.getCommandName()
								+ "' statement. First value '"
								+ existingFinalHash
								+ "' will be used."
						);
					} else {
						expectedFinalHashes.set(expectedFinalHashes.size() - 1, loadParameter.getValue(0));
					}
				}
			} else if (child instanceof OpenNestedArchivesParameter) {
				if (((OpenNestedArchivesParameter)child).getValue(0).equals("false")) {
					this.processNestedArchives = false;
				}
			} else {
				scriptContext.logFatalError(
					"'" + this.getCommandName() + "' statement had " + child.getClass().getName() + " as child " + i + " at line " + this.getLineNumber() + ". (2)"
				);
			}
		}

		this.archiveConfigs = new ArrayList<>(archivePaths.size());
		int configIndex = 0;

		for (String archivePath : archivePaths) {
			this.archiveConfigs.add(new PathConfig(archivePath, pathFilters.get(configIndex), expectedInitialHashes.get(configIndex), expectedFinalHashes.get(configIndex)));
			configIndex++;
		}

		this.simpleSkipPatterns = new ArrayList<>(simpleSkipPaths);
		this.simpleUnskipPatterns = new ArrayList<>(simpleUnskipPaths);
		this.complexSkipPatterns = new ArrayList<>(complexSkipPaths);
		this.complexUnskipPatterns = new ArrayList<>(complexUnskipPaths);
		this.executeCommand(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount);
	}

	@Override
	public String getCommandName() {
		return "open";
	}

	private void logClasspathResources(ClasspathResource[] resources, PrintWriter writer) {
		for (ClasspathResource resource : resources) {
			writer.println("\t" + resource.getResourceName() + (resource.isZipEntry() ? " opened from " + resource.getContainerPath() : ""));
		}
	}

	private void verifyFileSha256Hash(File file, String expectedHash, ScriptContext scriptContext) {
		try {
			String actualHash = new SHA256Utils(file.getAbsolutePath()).getHexDigest().toLowerCase();
			if (!expectedHash.equals(actualHash)) {
				scriptContext.logFatalError("File '" + file.getAbsolutePath() + "' was expected to have the SHA256 hash '" + expectedHash + "' but its actual hash is '" + actualHash + "'");
			}
		} catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			scriptContext.logFatalError("'" + file.getAbsolutePath() + "' couldn't be analyzed : '" + noSuchAlgorithmException + "' (3)");
		} catch (IOException ioException) {
			scriptContext.logFatalError("'" + file.getAbsolutePath() + "' couldn't be analyzed : '" + ioException + "' (4)");
		}
	}

	private void addPathToSet(String path, Set<String> pathSet, ScriptContext scriptContext) {
		if (path.length() == 0) {
			scriptContext.logFatalError("Empty file path \"" + path + "\" in \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + ".");
		} else if (!pathSet.add(path)) {
			scriptContext.logWarning(
				"\"" + path + "\" appears more than once in \"" + this.getCommandName() + "\" statement at line " + this.getLineNumber() + ". First occurrence will be used."
			);
		}
	}

	private void logUnusedPatterns(Set<String> usedSkipPatterns, Set<String> usedUnskipPatterns, AbstractLogger logger) {
		if (this.complexSkipPatterns.size() + this.simpleSkipPatterns.size() > usedSkipPatterns.size()) {
			for (int simpleSkipIndex = 0; simpleSkipIndex < this.simpleSkipPatterns.size(); simpleSkipIndex++) {
				String simpleSkipPattern = this.simpleSkipPatterns.get(simpleSkipIndex);
				if (!usedSkipPatterns.contains(simpleSkipPattern)) {
					this.logUnmatchedPatternWarning(simpleSkipPattern, "Skip", logger);
				}
			}

			for (int complexSkipIndex = 0; complexSkipIndex < this.complexSkipPatterns.size(); complexSkipIndex++) {
				String complexSkipPattern = this.complexSkipPatterns.get(complexSkipIndex);
				if (!usedSkipPatterns.contains(complexSkipPattern)) {
					this.logUnmatchedPatternWarning(complexSkipPattern, "Skip", logger);
				}
			}
		}

		if (this.complexUnskipPatterns.size() + this.simpleUnskipPatterns.size() > usedUnskipPatterns.size()) {
			for (int simpleUnskipIndex = 0; simpleUnskipIndex < this.simpleUnskipPatterns.size(); simpleUnskipIndex++) {
				String simpleUnskipPattern = this.simpleUnskipPatterns.get(simpleUnskipIndex);
				if (!usedUnskipPatterns.contains(simpleUnskipPattern)) {
					this.logUnmatchedPatternWarning(simpleUnskipPattern, "Unskip", logger);
				}
			}

			for (int complexUnskipIndex = 0; complexUnskipIndex < this.complexUnskipPatterns.size(); complexUnskipIndex++) {
				String complexUnskipPattern = this.complexUnskipPatterns.get(complexUnskipIndex);
				if (!usedUnskipPatterns.contains(complexUnskipPattern)) {
					this.logUnmatchedPatternWarning(complexUnskipPattern, "Unskip", logger);
				}
			}
		}
	}

	private void logFileSystemResources(ClasspathResource[] resources, String message, PrintWriter writer) {
		if (resources != null && resources.length > 0) {
			writer.println(message);

			for (ClasspathResource resource : resources) {
				writer.println("\t" + resource.getFullPath() + " opened");
			}
		}
	}

	private String calculateSha256(File file, ScriptContext scriptContext) {
		try {
			return new SHA256Utils(file.getAbsolutePath()).getHexDigest().toLowerCase();
		} catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			scriptContext.logFatalError("'" + file.getAbsolutePath() + "' couldn't be analyzed : '" + noSuchAlgorithmException + "' (1)");
		} catch (IOException ioException) {
			scriptContext.logFatalError("'" + file.getAbsolutePath() + "' couldn't be analyzed : '" + ioException + "' (2)");
		}

		return null;
	}

	@Override
	protected void executeCommand(ScriptContext scriptContext, int initialMessageCount, int initialWarningCount, int initialErrorCount) throws IOException {
		Workspace workspace = scriptContext.getWorkspace();
		PrintWriter writer = scriptContext.getWriter();
		LogContextLogger logger = new LogContextLogger(scriptContext, AbstractScriptNode.getTimestampPrefix().length());
		EmptyConsumer emptyConsumer = EmptyConsumer.create();
		ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream warningBuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream infoBuffer = new ByteArrayOutputStream();
		NoDuplicatePrintWriter errorWriter = new NoDuplicatePrintWriter(errorBuffer);
		PrintWriter warningWriter = new PrintWriter(warningBuffer);
		PrintWriter infoWriter = new PrintWriter(infoBuffer);
		String timestampPrefix = AbstractScriptNode.getTimestampPrefix();
		String openingMessage = timestampPrefix + " Opening classes...";
		writer.println(openingMessage);
		System.out.println(openingMessage);
		HashMap<String, ArchiveNode> archiveCache = GlobalUtils.newHashMap(27);
		ArrayList<ClasspathResource> jadResources = new ArrayList<>();
		ArrayList<ClasspathResource> xmlResources = new ArrayList<>();
		ArrayList<ClasspathResource> yamlResources = new ArrayList<>();
		ArrayList<ClasspathResource> propertiesResources = new ArrayList<>();
		HashSet<String> usedSkipPatterns = GlobalUtils.newHashSet(13);
		HashSet<String> usedUnskipPatterns = GlobalUtils.newHashSet(13);
		LinkedHashSet<ArchiveNode> nestedArchives = new LinkedHashSet<>(13);
		LinkedHashSet<ClasspathResource> skippedClassResources = new LinkedHashSet<>(13);
		HashSet<ClasspathResource> skippedXmlResources = GlobalUtils.newHashSet(13);
		HashSet<ClasspathResource> skippedYamlResources = GlobalUtils.newHashSet(13);
		HashSet<ClasspathResource> skippedPropertiesResources = GlobalUtils.newHashSet(13);
		HashSet<ClasspathResource> filteredPathResources = GlobalUtils.newHashSet(13);
		TemporaryZipWriter skippedClassesZipWriter = new TemporaryZipWriter();
		HashSet<String> skippedClassNamesCache = GlobalUtils.newHashSet(13);
		scriptContext.setClassesLoaded(false);
		ClasspathResource[] allClassResources = this.collectAndFilterAllResources(
			archiveCache, jadResources, xmlResources, yamlResources, propertiesResources, usedSkipPatterns, usedUnskipPatterns, nestedArchives, skippedClassResources, skippedXmlResources, skippedYamlResources, skippedPropertiesResources, filteredPathResources, skippedClassesZipWriter, skippedClassNamesCache, scriptContext, logger
		);
		ArchiveNode[] archiveNodes = new ArchiveNode[archiveCache.size()];
		int archiveIndex = 0;

		for (Entry<String, ArchiveNode> archiveEntry : archiveCache.entrySet()) {
			archiveNodes[archiveIndex++] = archiveEntry.getValue();
		}

		if (scriptContext.isVerbose()) {
			this.logClasspathResources(allClassResources, writer);
		}

		ClasspathResource[] jadResourceArray = new ClasspathResource[jadResources.size()];
		jadResources.toArray(jadResourceArray);
		ClasspathResource[] xmlResourceArray = new ClasspathResource[xmlResources.size()];
		xmlResources.toArray(xmlResourceArray);
		ClasspathResource[] yamlResourceArray = new ClasspathResource[yamlResources.size()];
		yamlResources.toArray(yamlResourceArray);
		ClasspathResource[] propertiesResourceArray = new ClasspathResource[propertiesResources.size()];
		propertiesResources.toArray(propertiesResourceArray);
		this.logUnusedPatterns(usedSkipPatterns, usedUnskipPatterns, logger);
		if (scriptContext.isVerbose()) {
			this.logFileSystemResources(jadResourceArray, timestampPrefix + " Opening JAD files from file system...", writer);
			this.logFileSystemResources(xmlResourceArray, timestampPrefix + " Opening XML files from file system...", writer);
			if (yamlResourceArray.length > 0) {
				this.logFileSystemResources(yamlResourceArray, timestampPrefix + " Opening YAML files from file system...", writer);
			}

			if (propertiesResourceArray.length > 0) {
				this.logFileSystemResources(propertiesResourceArray, timestampPrefix + " Opening PROPERTIES files from file system...", writer);
			}
		}

		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "in parse of");
		initialMessageCount = scriptContext.getMessageCount();
		initialWarningCount = scriptContext.getWarningCount();
		initialErrorCount = scriptContext.getErrorCount();
		AtomicInt apiWarningCounter = new AtomicInt(0);
		scriptContext.resetTrimExcludedMemberSet();
		scriptContext.setMicroEditionClassesFound(false);
		int openedClassCount = workspace.loadClasses(
			allClassResources,
			archiveNodes,
			nestedArchives,
			skippedClassResources,
			skippedXmlResources,
			skippedYamlResources,
			skippedPropertiesResources,
			filteredPathResources,
			skippedClassesZipWriter.getTempFilePath(),
			jadResourceArray,
			xmlResourceArray,
			yamlResourceArray,
			propertiesResourceArray,
			this.processNestedArchives,
			logger,
			emptyConsumer,
			null,
			scriptContext,
			errorWriter,
			warningWriter,
			apiWarningCounter,
			infoWriter
		);
		errorWriter.close();
		warningWriter.close();
		infoWriter.close();
		this.reportCountChanges(scriptContext, initialMessageCount, initialWarningCount, initialErrorCount, "while executing");
		String warningText = warningBuffer.toString();
		String errorText = errorBuffer.toString();
		String infoText = infoBuffer.toString();
		System.out.println(StringUtils.repeat(timestampPrefix.length() + 1, 32) + "Opened " + openedClassCount + (openedClassCount > 1 ? " classes" : " class"));
		if (errorText.length() > 0) {
			int errorLineCount = StringUtils.countOccurrences(errorText, Options.lineSeparator);
			writer.println("Errors detected during open...");
			writer.println(errorText);
			System.err.println(StringUtils.repeat(timestampPrefix.length() + 1, 32) + errorLineCount + (errorLineCount > 1 ? " errors" : " error") + " detected during open!");
		}

		if (apiWarningCounter.get() > 0) {
			if (Options.ZKM_REFLECTION_WARNINGS) {
				int apiWarningCount = apiWarningCounter.get();
				writer.println("API calls detected that may not be handled automatically...");
				writer.println(warningText);
				System.out.println(StringUtils.repeat(timestampPrefix.length() + 1, 32) + apiWarningCount + (apiWarningCount > 1 ? " API warnings" : " warning") + " detected during open.");
			} else {
				writer.println("API call warnings disabled.");
				System.out.println(StringUtils.repeat(timestampPrefix.length() + 1, 32) + "API call warnings disabled.");
			}
		}

		writer.println(infoText);
	}

	private void processPath(
		String path,
		File file,
		PathConfig fileConfig,
		Set<ClasspathResource> allClassResources,
		List<ClasspathResource> jadResources,
		List<ClasspathResource> xmlResources,
		List<ClasspathResource> yamlResources,
		List<ClasspathResource> propertiesResources,
		Map<String, ArchiveNode> archiveCache,
		Set<String> usedSkipPatterns,
		Set<String> usedUnskipPatterns,
		Set<ArchiveNode> nestedArchives,
		Set<ClasspathResource> skippedClassResources,
		Set<ClasspathResource> skippedXmlResources,
		Set<ClasspathResource> skippedYamlResources,
		Set<ClasspathResource> skippedPropertiesResources,
		Set<ClasspathResource> filteredPathResources,
		ScriptContext scriptContext,
		TemporaryZipWriter skippedClassesZipWriter,
		Set<String> skippedClassNamesCache,
		AbstractLogger logger
	) {
		if (!file.exists()) {
			scriptContext.logFatalError("'" + file.getAbsolutePath() + "' does not exist. (3)");
		} else if (file.isFile() && !file.isDirectory()) {
			String expectedInitialHash = fileConfig.getExpectedInitialHash();
			if (expectedInitialHash != null) {
				this.verifyFileSha256Hash(file, expectedInitialHash, scriptContext);
			}

			String expectedFinalHash = fileConfig.getExpectedFinalHash();
			if (path.endsWith(".class")) {
				allClassResources.add(new ClasspathResource(fileConfig.getExpectedFinalHash(), file));
				scriptContext.setClassesLoaded(true);
			} else {
				if (Options.ZKM_REPRODUCIBLE_RANDOM && !Options.ZKM_USE_PARALLEL && scriptContext.isVerbose()) {
					String sha256Hash = this.calculateSha256(file, scriptContext);
					scriptContext.getWriter().println("\tFile '" + file.getAbsolutePath() + "' has SHA256 hash '" + sha256Hash + "'");
				}

				boolean archiveDetected = false;

				try {
					archiveDetected = FileUtils.isArchive(file);
				} catch (IOException archiveCheckException) {
					scriptContext.logFatalError("FILE ERROR '" + file.getAbsolutePath() + "' : " + archiveCheckException);
				}

				if (archiveDetected) {
					File tempFile = null;

					try {
						tempFile = FileUtils.copyToManagedTempFile(file);
					} catch (IOException tempFileException) {
						scriptContext.logFatalError("Could not create temporary file in '" + TempFileManager.TEMP_DIRECTORY_PATH + "' : " + tempFileException + " (1)");
					}

					DebugUtils.assertNotNull(tempFile, "Null temporary file : '" + TempFileManager.TEMP_DIRECTORY_PATH + "'");
					ArchiveUtils.processArchive(
						tempFile,
						allClassResources,
						archiveCache,
						fileConfig,
						this.processNestedArchives,
						this.complexSkipPatterns,
						this.complexUnskipPatterns,
						usedSkipPatterns,
						usedUnskipPatterns,
						nestedArchives,
						skippedClassResources,
						skippedXmlResources,
						skippedYamlResources,
						skippedPropertiesResources,
						filteredPathResources,
						file.getAbsolutePath(),
						null,
						skippedClassesZipWriter,
						skippedClassNamesCache,
						logger
					);
				} else if (path.endsWith(".jad")) {
					jadResources.add(new ClasspathResource(expectedFinalHash, file));
				} else if (FileUtils.isXmlFile(path)) {
					xmlResources.add(new ClasspathResource(expectedFinalHash, file));
				} else if (FileUtils.isYamlFile(path)) {
					yamlResources.add(new ClasspathResource(expectedFinalHash, file));
				} else if (FileUtils.isPropertiesFile(path)) {
					propertiesResources.add(new ClasspathResource(expectedFinalHash, file));
				} else {
					scriptContext.logFatalError("File '" + file.getAbsolutePath() + "' not a recognized type.");
				}
			}
		} else if (!file.isFile() && file.isDirectory()) {
			PathFilter pathFilter = fileConfig.getFilter();
			String[] classFileNames = file.list(new ClassFilenameFilter());
			if (classFileNames != null) {
				for (int i = 0; i < classFileNames.length; i++) {
					File classFile = new File(file, classFileNames[i]);
					if (pathFilter.matches(classFile.getAbsolutePath())) {
						allClassResources.add(new ClasspathResource(classFile));
						scriptContext.setClassesLoaded(true);
					} else {
						logger.message(
							"INFO:", "Filtering out path '" + classFile.getAbsolutePath() + "' because it does not match specified filter '" + pathFilter.getDefinition() + "' (B)"
						);
					}
				}
			}
		} else {
			scriptContext.logFatalError("'" + file.getPath() + "' couldn't be recognized : " + file.isDirectory() + " : " + file.isFile());
		}
	}

	public ClasspathResource[] collectAndFilterAllResources(
		Map<String, ArchiveNode> archiveCache,
		ArrayList<ClasspathResource> jadResources,
		ArrayList<ClasspathResource> xmlResources,
		List<ClasspathResource> yamlResources,
		List<ClasspathResource> propertiesResources,
		Set<String> usedSkipPatterns,
		Set<String> usedUnskipPatterns,
		Set<ArchiveNode> nestedArchives,
		Set<ClasspathResource> skippedClassResources,
		Set<ClasspathResource> skippedXmlResources,
		Set<ClasspathResource> skippedYamlResources,
		Set<ClasspathResource> skippedPropertiesResources,
		Set<ClasspathResource> filteredPathResources,
		TemporaryZipWriter skippedClassesZipWriter,
		Set<String> skippedClassNamesCache,
		ScriptContext scriptContext,
		AbstractLogger logger
	) {
		LinkedHashSet<ClasspathResource> collectedResources = new LinkedHashSet<>();
		LinkedHashMap<String, PathConfig> configsByPath = new LinkedHashMap<>();

		for (PathConfig archiveConfig : this.archiveConfigs) {
			archiveConfig.getFilter();
			LinkedHashMap<String, String> patternsByPath = new LinkedHashMap<>();
			this.resolvePathWithWildcards(archiveConfig.getPath(), patternsByPath, LoadTypes.TOP_LEVEL, scriptContext);

			for (Entry<String, String> pathEntry : patternsByPath.entrySet()) {
				configsByPath.put(pathEntry.getKey(), archiveConfig);
			}
		}

		LinkedHashMap<String, String> skipPatternsByPath = new LinkedHashMap<>();

		for (String simpleSkipPattern : this.simpleSkipPatterns) {
			this.resolvePathWithWildcards(simpleSkipPattern, skipPatternsByPath, LoadTypes.SKIP, scriptContext);
		}

		LinkedHashMap<String, String> unskipPatternsByPath = new LinkedHashMap<>();

		for (String simpleUnskipPattern : this.simpleUnskipPatterns) {
			this.resolvePathWithWildcards(simpleUnskipPattern, unskipPatternsByPath, LoadTypes.UNSKIP, scriptContext);
		}

		for (Entry<String, PathConfig> configEntry : configsByPath.entrySet()) {
			String path = configEntry.getKey();
			File file = new File(path);
			PathConfig pathConfig = configEntry.getValue();
			PathFilter pathFilter = pathConfig.getFilter();
			if (!FileUtils.isArchiveExtension(path) && !file.isDirectory() && !pathFilter.matches(path)) {
				logger.message("INFO:", "Filtering out path '" + path + "' because it does not match specified filter '" + pathFilter.getDefinition() + "' (A)");
			} else {
				boolean pathSkipped = false;
				if (skipPatternsByPath.containsKey(path)) {
					logger.message("INFO:", "Skipping path '" + path + "' because of parameter -\"" + skipPatternsByPath.get(path) + "\" (A)");
					usedSkipPatterns.add(path);
					if (unskipPatternsByPath.containsKey(path)) {
						logger.message("INFO:", "Unskipping path '" + path + "' because of parameter +\"" + unskipPatternsByPath.get(path) + "\" (A)");
						usedUnskipPatterns.add(path);
					} else {
						pathSkipped = true;
					}
				}

				if (!pathSkipped) {
					this.processPath(path, file, pathConfig, collectedResources, jadResources, xmlResources, yamlResources, propertiesResources, archiveCache, usedSkipPatterns, usedUnskipPatterns, nestedArchives, skippedClassResources, skippedXmlResources, skippedYamlResources, skippedPropertiesResources, filteredPathResources, scriptContext, skippedClassesZipWriter, skippedClassNamesCache, logger);
				}
			}
		}

		ClasspathResource[] resourceArray = new ClasspathResource[collectedResources.size()];
		resourceArray = collectedResources.toArray(resourceArray);
		if (!skippedClassesZipWriter.isStreamUninitialized()) {
			try {
				skippedClassesZipWriter.close();
			} catch (IOException closeException) {
				logger.fatal("FILE ERROR:", closeException.toString());
			}
		}

		return resourceArray;
	}

	private void logUnmatchedPatternWarning(String pattern, String clauseName, AbstractLogger logger) {
		if (pattern.toLowerCase().endsWith(".class")) {
			logger.warn("WARNING:", clauseName + " clause '" + pattern + "' not matched to any class");
		} else {
			logger.warn("WARNING:", clauseName + " clause '" + pattern + "' not matched to any file");
		}
	}
}

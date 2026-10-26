package com.zelix.workspace.writer;

import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.exceptions.MethodAnalyzerException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.exceptions.ZKMException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.manifest.ManifestFile;
import com.zelix.framework.modules.ModuleInfoNode;
import com.zelix.framework.reference.IReferenceGraph;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.reference.ReferenceGraphMap;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.logging.AbstractLogger;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.utils.*;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.index.IndexListProcessor;
import com.zelix.workspace.resources.ResourceIndex;
import com.zelix.workspace.resources.xml.AbstractFrameworkXmlHandler;
import com.zelix.workspace.resources.xml.XmlHandler;
import com.zelix.workspace.resources.xml.XmlParser;
import com.zelix.workspace.resources.xml.XmlResourceProcessor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class WorkspaceWriter {
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private final List<ArchiveNode> rootArchives = new ArrayList<>();
	private final Map<ArchiveNode, ZipFile> inputArchiveFileCache = new TreeMap<>();
	private final Map<ArchiveNode, File> outputFileHandleMap = GlobalUtils.newHashMap();
	private final Map<ZipOutputStream, String> outputStreamPathMap = GlobalUtils.newHashMap();
	private final Map<ArchiveNode, ZipOutputStream> outputStreamMap = GlobalUtils.newHashMap();
	private final ReferenceGraph<ZipOutputStream, String> writtenEntryTracker = new ReferenceGraph<>();
	private final ClassNode[] allClassNodes;
	private final ArchiveNode[] allArchiveNodes;
	private final Set<ArchiveNode> skippedArchives;
	private final DualKeyMap<ArchiveNode, String, ClasspathResource> skippedClassResources;
	private final DualKeyMap<ArchiveNode, String, ClasspathResource> skippedXmlResources;
	private final DualKeyMap<ArchiveNode, String, ClasspathResource> skippedYamlResources;
	private final DualKeyMap<ArchiveNode, String, ClasspathResource> skippedPropertiesResources;
	private final DualKeyMap<ArchiveNode, String, ClasspathResource> deletedResources;
	private final HashMap<JadFileProcessor, ArchiveNode> jadFileMap;
	private final ClasspathResource[] xmlFiles;
	private final ClasspathResource[] yamlFileResources;
	private final ClasspathResource[] propertiesFileResources;
	private final HashMap<ArchiveNode, File> customArchiveOutputPaths;
	private final HashMap<JadFileProcessor, File> customJadOutputPaths;
	private final HashMap<ClasspathResource, File> customXmlOutputPaths;
	private final HashMap<ClasspathResource, File> customYamlOutputPaths;
	private final ReadOnlyMap<String, String> packageRenameMap;
	private final ReadOnlyMap<String, String> classRenameMap;
	private final ReadOnlyMap<String, String> fullRemappingMap;
	private final ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldRemappingRegistry;
	private final ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodRemappingRegistry;
	private final AbstractWorkspace workspace;
	private final TypeResolver typeResolver;
	private final int compressionFlag;
	private final boolean deleteEmptyDirectories;
	private final boolean deleteXmlComments;
	private final AbstractLogger logger;
	private final ScriptContext scriptContext;
	private final FixedClassManager fixedClassManager;
	private final ResourceIndex fileAdaptor;
	private final AtomicReference<String> statusReporter;
	private final File outputTarget;
	private final Long globalTimestamp;
	private final boolean isOutputToArchive;
	private ZipOutputStream mainOutputZipStream;
	private final PathRemapper pathRemapper;

	public WorkspaceWriter(
		ClassNode[] workspaceClasses,
		ModuleInfoNode[] moduleInfoNodes,
		ArchiveNode[] allArchiveNodes,
		Set<ArchiveNode> skippedArchives,
		DualKeyMap<ArchiveNode, String, ClasspathResource> skippedClasses,
		DualKeyMap<ArchiveNode, String, ClasspathResource> unobfuscatableResources,
		DualKeyMap<ArchiveNode, String, ClasspathResource> keepTimestampResources,
		DualKeyMap<ArchiveNode, String, ClasspathResource> keepTimestampResources2,
		DualKeyMap<ArchiveNode, String, ClasspathResource> deletedResources,
		HashMap<JadFileProcessor, ArchiveNode> jadFileMap,
		ClasspathResource[] xmlFiles,
		ClasspathResource[] yamlFileResources,
		ClasspathResource[] propertiesFileResources,
		HashMap<ArchiveNode, File> customArchiveOutputPaths,
		HashMap<JadFileProcessor, File> customJadOutputPaths,
		HashMap<ClasspathResource, File> customXmlOutputPaths,
		HashMap<ClasspathResource, File> customYamlOutputPaths,
		HashMap<ClasspathResource, File> customPropertiesOutputPaths,
		ReadOnlyMap<String, String> packageRenameMap,
		ReadOnlyMap<String, String> classRenameMap,
		ReadOnlyMap<String, String> fullRemappingMap,
		ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldRemappingRegistry,
		ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodRemappingRegistry,
		AbstractWorkspace workspace,
		File outputTarget,
		int compressionFlag,
		boolean deleteEmptyDirectories,
		boolean useUtf8Bom,
		String timestampString,
		AbstractLogger logger,
		ScriptContext scriptContext,
		TypeResolver typeResolver,
		FixedClassManager fixedClassManager,
		IReferenceGraph<ClassNode, ClassNode> generatedClassGraph,
		ResourceIndex fileAdaptor,
		AtomicReference<String> statusReporter
	) {
		if (moduleInfoNodes.length > 0) {
			this.allClassNodes = new ClassNode[workspaceClasses.length + moduleInfoNodes.length];
			System.arraycopy(workspaceClasses, 0, this.allClassNodes, 0, workspaceClasses.length);
			System.arraycopy(moduleInfoNodes, 0, this.allClassNodes, workspaceClasses.length, moduleInfoNodes.length);
		} else {
			this.allClassNodes = new ClassNode[workspaceClasses.length];
			System.arraycopy(workspaceClasses, 0, this.allClassNodes, 0, workspaceClasses.length);
		}

		this.allArchiveNodes = allArchiveNodes;
		this.skippedArchives = skippedArchives;
		this.skippedClassResources = skippedClasses;
		this.skippedXmlResources = unobfuscatableResources;
		this.skippedYamlResources = keepTimestampResources;
		this.skippedPropertiesResources = keepTimestampResources2;
		this.deletedResources = deletedResources;
		this.jadFileMap = jadFileMap;
		this.xmlFiles = xmlFiles;
		this.yamlFileResources = yamlFileResources;
		this.propertiesFileResources = propertiesFileResources;
		this.customArchiveOutputPaths = customArchiveOutputPaths;
		this.customJadOutputPaths = customJadOutputPaths;
		this.customXmlOutputPaths = customXmlOutputPaths;
		this.customYamlOutputPaths = customYamlOutputPaths;
		this.packageRenameMap = packageRenameMap;
		this.classRenameMap = classRenameMap;
		this.fullRemappingMap = fullRemappingMap;
		this.fieldRemappingRegistry = fieldRemappingRegistry;
		this.methodRemappingRegistry = methodRemappingRegistry;
		this.workspace = workspace;
		this.typeResolver = typeResolver;
		this.compressionFlag = compressionFlag;
		this.deleteEmptyDirectories = deleteEmptyDirectories;
		this.deleteXmlComments = useUtf8Bom;
		this.logger = logger;
		this.scriptContext = scriptContext;
		this.fixedClassManager = fixedClassManager;
		this.fileAdaptor = fileAdaptor;
		this.statusReporter = statusReporter;
		this.outputTarget = outputTarget;
		if (timestampString != null) {
			this.globalTimestamp = this.parseGlobalTimestamp(timestampString);
		} else {
			this.globalTimestamp = null;
		}

		if (FileUtils.isArchiveExtensionLenient(outputTarget.getAbsolutePath())) {
			if (jadFileMap != null && jadFileMap.size() > 0) {
				this.isOutputToArchive = false;
				logger.error("ERROR:", "Must specify a base save directory when processing JAD files. Cannot save to a base archive : '" + outputTarget.getAbsolutePath() + "'");
			} else {
				this.isOutputToArchive = true;
				if (outputTarget.exists()) {
					this.backupExistingOutputFile(outputTarget);
				}

				try {
					this.mainOutputZipStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputTarget), 2048));
					this.outputStreamPathMap.put(this.mainOutputZipStream, outputTarget.getAbsolutePath());
				} catch (IOException ioException) {
					logger.error("FILE ERROR:", "Couldn't create '" + outputTarget.getAbsolutePath() + "' : " + ioException.getMessage() + " : (1)");
				}
			}
		} else {
			this.isOutputToArchive = false;
		}

		try {
			this.validateFilesystemConflicts();
			this.initializeOutputStreams();
			HashMap<ClasspathResource, String> classEntryPaths = GlobalUtils.newHashMap();
			HashMap<ClasspathResource, String> classDirPaths = GlobalUtils.newHashMap();
			Set<String> packagePrefixes = this.buildClassResourcePathMaps(classEntryPaths, classDirPaths);
			this.pathRemapper = new PathRemapper(packageRenameMap, GlobalUtils.newHashSet(packagePrefixes));
			ReferenceGraph<ArchiveNode, String> rootEntryGraph = new ReferenceGraph<>(allArchiveNodes.length);
			ReferenceGraph<ArchiveNode, String> directoryEntryGraph = new ReferenceGraph<>(allArchiveNodes.length);
			this.buildResourceGraphs(classEntryPaths, this.pathRemapper, rootEntryGraph, directoryEntryGraph);
			ReferenceGraph<ZipOutputStream, String> writtenEntryGraph = new ReferenceGraph<>();
			this.processManifestFiles();
			this.processServiceFiles();
			this.processAndSaveAllClasses(generatedClassGraph, classEntryPaths, writtenEntryGraph, classDirPaths, rootEntryGraph, directoryEntryGraph);
			this.processAndCopyAllResources(writtenEntryGraph, rootEntryGraph, directoryEntryGraph);
			this.processExternalXmlFiles();
			this.processExternalTextFiles(yamlFileResources, customYamlOutputPaths, false);
			this.processExternalTextFiles(propertiesFileResources, customPropertiesOutputPaths, true);
			this.writeNestedArchives();
		} finally {
			this.closeAllStreams();
		}

		if (!this.isOutputToArchive) {
			this.processJadFiles();
		}
	}

	private static String getSha256Hash(File file, ScriptContext scriptContext) {
		String sha256Hash = null;

		try {
			sha256Hash = new SHA256Utils(file.getAbsolutePath()).getHexDigest().toLowerCase();
		} catch (NoSuchAlgorithmException algorithmException) {
			scriptContext.logFatalError("'" + file.getAbsolutePath() + "' couldn't be analyzed : '" + algorithmException + "' (5)");
		} catch (IOException ioException) {
			scriptContext.logFatalError("'" + file.getAbsolutePath() + "' couldn't be analyzed : '" + ioException + "' (6)");
		}

		return sha256Hash;
	}

	private static Set<String> findEmptyDirectories(Enumeration<? extends ZipEntry> entries, Set<String> writtenEntries) {
		HashSet<String> emptyDirectories = GlobalUtils.newHashSet();
		HashSet<String> directoryNames = GlobalUtils.newHashSet();
		HashMap<String, String> fileNameByDirectoryPath = GlobalUtils.newHashMap();

		while (entries.hasMoreElements()) {
			ZipEntry zipEntry = entries.nextElement();
			String entryName = zipEntry.getName();
			if (zipEntry.isDirectory()) {
				directoryNames.add(entryName);
			} else {
				int lastSeparator = entryName.lastIndexOf("/");
				if (lastSeparator > -1) {
					String entryDirectoryPath = entryName.substring(0, lastSeparator + 1);
					if (entryName.toLowerCase().endsWith(".class")) {
						if (writtenEntries != null && writtenEntries.contains(entryName)) {
							fileNameByDirectoryPath.put(entryDirectoryPath, entryName);
						}
					} else {
						fileNameByDirectoryPath.put(entryDirectoryPath, entryName);
					}
				}
			}
		}

		HashSet<String> implicitDirectories = GlobalUtils.newHashSet();

		for (String directoryPath : fileNameByDirectoryPath.keySet()) {
			StringTokenizer pathTokenizer = new StringTokenizer(directoryPath, "/");
			StringBuilder pathBuffer = new StringBuilder();

			while (pathTokenizer.hasMoreTokens()) {
				String pathSegment = pathTokenizer.nextToken();
				pathBuffer.append(pathSegment);
				pathBuffer.append("/");
				implicitDirectories.add(pathBuffer.toString());
			}
		}

		for (String directoryName : directoryNames) {
			if (!implicitDirectories.contains(directoryName)) {
				emptyDirectories.add(directoryName);
			}
		}

		return emptyDirectories;
	}

	private void trackResourcePath(String entryPath, ArchiveNode archiveNode, ReferenceGraph<ArchiveNode, String> rootEntryGraph, ReferenceGraph<ArchiveNode, String> directoryEntryGraph) {
		int lastSeparator = entryPath.lastIndexOf("/");
		if (lastSeparator == -1) {
			rootEntryGraph.addReference(archiveNode, entryPath);
		} else {
			String directoryPath = entryPath.substring(0, lastSeparator);
			directoryEntryGraph.addReference(archiveNode, directoryPath);
		}
	}

	private void registerOutputArchive(String outputPath, ArchiveNode archiveNode, Map<String, ArchiveNode> outputPathToArchiveMap, String contextTag) {
		if (!outputPathToArchiveMap.containsKey(outputPath)) {
			outputPathToArchiveMap.put(outputPath, archiveNode);
		} else {
			ArchiveNode existingArchive = outputPathToArchiveMap.get(outputPath);
			ZipOutputStream existingOutputStream = this.outputStreamMap.get(existingArchive);
			File existingOutputFile = this.outputFileHandleMap.get(existingArchive);

			try {
				existingOutputStream.close();
				Files.deleteIfExists(existingOutputFile.toPath());
			} catch (Throwable cleanupException) {
				DebugUtils.assertTrue(false, new String[]{cleanupException.toString()});
			}

			this.logger
				.fatal(
					"ERROR:",
					"Duplicate unqualified archive names while attempting to save '"
						+ archiveNode.getDisplayPath()
						+ "' to '"
						+ outputPath
						+ "'. Clash with '"
						+ existingArchive.getDisplayPath()
						+ "' ("
						+ contextTag
						+ ")"
				);
		}
	}

	private final void saveClassToDirectory(ClassNode classNode, byte[] classBytes) {
		DataOutputStream dataOutputStream = null;
		String classFilePath = this.getClassFilePathInDirectory(classNode);

		try {
			if (this.scriptContext.isVerbose()) {
				this.scriptContext.getWriter().println("\tClass file '" + classNode.getRawSourcePath() + "' saved as '" + classFilePath + "'.");
			}

			FileOutputStream fileOutputStream = new FileOutputStream(classFilePath);
			dataOutputStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream, 2048));
			if (classBytes != null) {
				dataOutputStream.write(classBytes);
			} else {
				classNode.write(dataOutputStream, this.typeResolver, this.workspace, this.scriptContext);
			}
		} catch (IOException ioException) {
			this.logger.error("FILE ERROR:", "Saving " + ioException.getMessage());
		} catch (MethodAnalyzerException methodAnalyzerException) {
			this.logger.error("ERROR:", "Method '" + methodAnalyzerException.getMethodName() + "' could not be analyzed (H). Consider excluding it from Flow and Exception Obfuscation.");
		} catch (ProcessingException processingException) {
			this.logger.error("ERROR:", processingException.getMessage());
		} finally {
			if (dataOutputStream != null) {
				try {
					dataOutputStream.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	private void copyZipEntry(ZipFile zipFile, ZipEntry zipEntry, ZipOutputStream zipOutputStream, String sourceEntryName, String targetEntryName, boolean isDeflated, boolean keepTimestamp) {
		try {
			InputStream entryInputStream = null;

			try {
				entryInputStream = zipFile.getInputStream(zipEntry);
				if (entryInputStream != null) {
					ZipEntryWriter entryWriter = new ZipEntryWriter(zipOutputStream, targetEntryName, isDeflated, entryInputStream, (int)zipEntry.getSize());
					if (Options.ZKM_KEEP_SKIPPED_LAST_MODIFIED_DATE && keepTimestamp) {
						entryWriter.writeEntry(zipEntry.getTime());
					} else {
						entryWriter.writeEntry(this.globalTimestamp);
					}

					this.writtenEntryTracker.addReference(zipOutputStream, targetEntryName);
				} else {
					this.scriptContext
						.logWarning("File Error: Couldn't copy '" + sourceEntryName + "' from '" + zipFile.getName() + "' to new archive because the entry appears to be corrupt (B)");
				}
			} finally {
				if (entryInputStream != null) {
					try {
						entryInputStream.close();
					} catch (IOException ignored) {
					}
				}
			}
		} catch (IOException ioException) {
			this.logger.error("FILE ERROR:", "Couldn't copy '" + sourceEntryName + "' from '" + zipFile.getName() + "' to new archive (B) : " + ioException.getMessage());
		}
	}

	public void initializeOutputStreams() {
		for (int i = 0; i < this.allArchiveNodes.length; i++) {
			if (this.allArchiveNodes[i].isRoot()) {
				this.rootArchives.add(this.allArchiveNodes[i]);
			}

			if (this.skippedArchives == null || !this.skippedArchives.contains(this.allArchiveNodes[i])) {
				String archiveFilePath = this.allArchiveNodes[i].getFilePath();

				try {
					ZipFileWrapper zipFileWrapper = new ZipFileWrapper(archiveFilePath);
					this.inputArchiveFileCache.put(this.allArchiveNodes[i], zipFileWrapper);
				} catch (IOException archiveOpenException) {
					if (archiveFilePath.equals(this.allArchiveNodes[i].getFullPath())) {
						this.scriptContext.logSpecialError("Couldn't open '" + archiveFilePath + "' as an archive file. : " + archiveOpenException.getMessage() + " : Please check if it is corrupt.");
					} else {
						this.scriptContext
							.logSpecialError(
								"Couldn't open '"
									+ archiveFilePath
									+ "' as an archive file. : "
									+ archiveOpenException.getMessage()
									+ " : Temporary copy of "
									+ this.allArchiveNodes[i].getFullPath()
									+ " : Please check if the original is corrupt."
							);
					}
				}
			}
		}

		HashMap<String, ArchiveNode> outputPathToArchiveMap = GlobalUtils.newHashMap();

		for (ArchiveNode archiveNode : this.inputArchiveFileCache.keySet()) {
			File outputFile;
			String outputPath;
			if (archiveNode.isRoot()) {
				String relativePath = archiveNode.getRelativePath();
				if (this.hasCustomOutputPath(archiveNode)) {
					outputFile = this.customArchiveOutputPaths.get(archiveNode);
					String absoluteOutputPath = outputFile.getAbsolutePath();
					outputPath = outputFile.getName();
					if (this.scriptContext.isVerbose()) {
						this.scriptContext.getWriter().println("\tArchive file '" + archiveNode.getRelativePath() + "' saved to '" + outputFile.getAbsolutePath() + "'.");
					}

					this.registerOutputArchive(absoluteOutputPath, archiveNode, outputPathToArchiveMap, "A");
				} else if (this.isOutputToArchive) {
					outputFile = this.outputTarget;
					this.outputTarget.getAbsolutePath();
					outputPath = this.outputTarget.getName();
				} else {
					String outputFilePath = this.outputTarget.getAbsolutePath() + FILE_SEPARATOR + relativePath.substring(relativePath.lastIndexOf(FILE_SEPARATOR) + 1);
					outputFile = new File(outputFilePath);
					outputPath = outputFile.getName();
					this.registerOutputArchive(outputFilePath, archiveNode, outputPathToArchiveMap, "B");
				}

				this.outputFileHandleMap.put(archiveNode, outputFile);
				if (outputFile.exists() && (!this.isOutputToArchive || !outputFile.getAbsolutePath().equals(this.outputTarget.getAbsolutePath()))) {
					this.backupExistingOutputFile(outputFile);
				}
			} else {
				try {
					outputFile = TempFileManager.createTempFile(archiveNode.getShortPath());
					this.outputFileHandleMap.put(archiveNode, outputFile);
					ArchiveNode rootArchive = archiveNode.getRootArchive();
					if (this.hasCustomOutputPath(rootArchive)) {
						outputPath = archiveNode.getShortPath();
					} else if (this.isOutputToArchive) {
						String shortPath = archiveNode.getShortPath();
						int nestedSeparatorIndex = shortPath.indexOf("!");
						outputPath = this.outputTarget.getName() + shortPath.substring(nestedSeparatorIndex);
					} else {
						outputPath = archiveNode.getShortPath();
					}
				} catch (IOException tempFileCreationException) {
					this.logger.fatal("FILE ERROR:", "Could not create temporary file in '" + TempFileManager.TEMP_DIRECTORY_PATH + "' : " + tempFileCreationException + " (3)");
					return;
				}
			}

			ZipOutputStream zipOutputStream = null;
			if (!archiveNode.isRoot() || !this.isOutputToArchive || this.customArchiveOutputPaths != null && this.customArchiveOutputPaths.containsKey(archiveNode)) {
				try {
					zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile), 2048));
					this.outputStreamPathMap.put(zipOutputStream, outputPath);
				} catch (IOException outputStreamCreationException) {
					this.logger.fatal("FILE ERROR:", "Couldn't create '" + outputFile.getAbsolutePath() + "' : " + outputStreamCreationException.getMessage() + " : (2)");
				}
			} else {
				zipOutputStream = this.mainOutputZipStream;
			}

			DebugUtils.assertNotNull(zipOutputStream, archiveNode.getRelativePath());
			this.outputStreamMap.put(archiveNode, zipOutputStream);
		}
	}

	private void copyZipEntry(ZipFile zipFile, ZipEntry zipEntry, ZipOutputStream zipOutputStream, String sourceEntryName, String targetEntryName, boolean isDeflated) {
		this.copyZipEntry(zipFile, zipEntry, zipOutputStream, sourceEntryName, targetEntryName, isDeflated, false);
	}

	private void validateOutputFileHash(File sourceFile, File outputFile, String expectedHash, ScriptContext scriptContext) {
		try {
			String actualHash = new SHA256Utils(outputFile.getAbsolutePath()).getHexDigest().toLowerCase();
			if (!expectedHash.equals(actualHash)) {
				scriptContext.logFatalError(
					"File '"
						+ sourceFile.getAbsolutePath()
						+ "' was expected to be saved as "
						+ outputFile.getAbsolutePath()
						+ " with the SHA256 hash '"
						+ expectedHash
						+ "' but the actual hash was '"
						+ actualHash
						+ "'"
				);
			}
		} catch (NoSuchAlgorithmException algorithmException) {
			scriptContext.logFatalError("'" + outputFile.getAbsolutePath() + "' couldn't be analyzed : '" + algorithmException + "' (7)");
		} catch (IOException ioException) {
			scriptContext.logFatalError("'" + outputFile.getAbsolutePath() + "' couldn't be analyzed : '" + ioException + "' (8)");
		}
	}

	private Long parseGlobalTimestamp(String timestampString) {
		try {
			long epochMillis = Long.parseLong(timestampString);
			FileTime fileTimeFromMillis = FileTime.fromMillis(epochMillis);
			this.scriptContext.getWriter().println("All archive entries will have their last modified dates set to '" + fileTimeFromMillis + "' : " + timestampString);
			return epochMillis;
		} catch (NumberFormatException ignored) {
			try {
				FileTime fileTimeFromInstant = FileTime.from(Instant.parse(timestampString));
				this.scriptContext.getWriter().println("All archive entries will have their last modified dates set to '" + fileTimeFromInstant + "' : '" + timestampString + "'");
				return fileTimeFromInstant.toMillis();
			} catch (DateTimeParseException dateTimeParseException) {
				this.scriptContext.logFatalError("Could not successfully parse '" + timestampString + "' : '" + dateTimeParseException + "'");
				return null;
			}
		}
	}

	private void copyUnchangedClassEntry(ZipFile zipFile, ZipEntry zipEntry, ZipOutputStream zipOutputStream, boolean isDeflated) {
		try {
			String entryName = zipEntry.getName();
			InputStream entryInputStream = zipFile.getInputStream(zipEntry);
			new ZipEntryWriter(zipOutputStream, entryName, isDeflated, entryInputStream, (int)zipEntry.getSize()).writeEntry(this.globalTimestamp);
			this.writtenEntryTracker.addReference(zipOutputStream, entryName);
		} catch (IOException ioException) {
			this.logger.error("FILE ERROR:", "Couldn't save '" + zipEntry.getName() + "' from '" + zipFile.getName() + "' to new archive (D) : " + ioException.getMessage());
		}
	}

	private boolean isEntryAlreadyWritten(ZipOutputStream zipOutputStream, String entryName, boolean warnOnDuplicate) {
		if (this.writtenEntryTracker.isReferencedBy(zipOutputStream, entryName)) {
			if (warnOnDuplicate) {
				this.logger
					.warn("WARNING:", "Duplicate entry '" + entryName + "' while writing to '" + this.outputStreamPathMap.get(zipOutputStream) + "'. Using only the first entry encountered.");
			}

			return true;
		} else {
			return false;
		}
	}

	private final void copyUnchangedClassFile(ClassNode classNode, String sourceFilePath) {
		String classFilePath = this.getClassFilePathInDirectory(classNode);

		try {
			if (this.scriptContext.isVerbose()) {
				this.scriptContext.getWriter().println("\tClass file '" + sourceFilePath + "' saved as '" + classFilePath + "'.");
			}

			FileUtils.copyFile(sourceFilePath, classFilePath);
		} catch (IOException ioException) {
			this.logger.error("FILE ERROR:", "Copying " + ioException.getMessage());
		}
	}

	private void processExternalXmlFiles() {
		if (this.xmlFiles != null) {
			for (ClasspathResource xmlFileResource : this.xmlFiles) {
				String xmlFilePath = xmlFileResource.getFullPath();
				File xmlFile = xmlFileResource.getAsFile();
				AtomicReference<String> detectedCharset = new AtomicReference<>();
				AtomicInt bomBytesToSkip = new AtomicInt(0);
				AtomicInt bomType = new AtomicInt(0);
				AtomicInt byteOrder = new AtomicInt(Integer.MAX_VALUE);

				try {
					String xmlText = XmlParser.readFile(xmlFile, detectedCharset, bomBytesToSkip, bomType, byteOrder);
					XmlHandler xmlHandler = AbstractFrameworkXmlHandler.createHandler(
						xmlFilePath,
						xmlText,
						this.classRenameMap,
						this.fullRemappingMap,
						this.fieldRemappingRegistry,
						this.methodRemappingRegistry,
						this.pathRemapper,
						this.fileAdaptor,
						this.workspace,
						this.typeResolver.getClassProvider(),
						this.logger
					);
					if (this.isOutputToArchive) {
						ZipOutputStream outputZipStream = this.mainOutputZipStream;
						if (!this.isEntryAlreadyWritten(outputZipStream, xmlFileResource.getResourceName())) {
							new XmlResourceProcessor(xmlFile, bomBytesToSkip, bomType, byteOrder, detectedCharset.get(), outputZipStream, xmlHandler, this.compressionFlag == 1, this.deleteXmlComments);
							this.writtenEntryTracker.addReference(outputZipStream, xmlFileResource.getResourceName());
						}
					} else {
						File outputFile;
						if (this.customXmlOutputPaths != null && this.customXmlOutputPaths.containsKey(xmlFileResource)) {
							outputFile = this.customXmlOutputPaths.get(xmlFileResource);
						} else {
							outputFile = new File(this.outputTarget, xmlFile.getName());
						}

						new XmlResourceProcessor(xmlFile, bomBytesToSkip, bomType, byteOrder, detectedCharset.get(), outputFile, xmlHandler, this.deleteXmlComments);
						if (xmlFileResource.getExpectedFinalHash() != null) {
							String expectedFinalHash = xmlFileResource.getExpectedFinalHash();
							this.validateOutputFileHash(xmlFile, outputFile, expectedFinalHash, this.scriptContext);
						}

						if (this.scriptContext.isVerbose()) {
							this.scriptContext
								.getWriter()
								.println(
									"\tXML file '"
										+ xmlFile.getAbsolutePath()
										+ "' saved to '"
										+ outputFile.getAbsolutePath()
										+ "'"
										+ (!Options.ZKM_USE_PARALLEL && Options.ZKM_REPRODUCIBLE_RANDOM ? " with SHA256 hash '" + getSha256Hash(outputFile, this.scriptContext) + "'" : "")
										+ "."
								);
						}
					}
				} catch (ZKMException invalidXmlException) {
					this.logger.error("FILE ERROR:", "Invalid XML file '" + xmlFilePath + "' : " + invalidXmlException.getMessage());
				} catch (IOException ioException) {
					this.logger.error("FILE ERROR:", "Error reading or writing XML file '" + xmlFilePath + "' : " + ioException);
				}
			}
		}
	}

	private void validateFilesystemConflicts() {
		if (!this.isOutputToArchive) {
			ReferenceGraph<String, String> packageClashGraph = new ReferenceGraph<>();
			MultiMap<String, ClassNode> classesByLowercaseName = new MultiMap<>();

			for (ClassNode classNode : this.allClassNodes) {
				String packageName = classNode.getPackageName();
				if (!classNode.isFromFilesystem()) {
					packageClashGraph.addReference(packageName.toLowerCase(), packageName);
				}

				classesByLowercaseName.put(classNode.getInternalName().toLowerCase(), classNode);
			}

			boolean classClashDetected = false;
			Iterator<Entry<String, List<ClassNode>>> classEntryIterator = classesByLowercaseName.entrySet().iterator();

			while (classEntryIterator.hasNext()) {
				List<ClassNode> clashingClasses = classEntryIterator.next().getValue();
				if (clashingClasses.size() > 1) {
					ArrayList<ClassNode> nonFilesystemClasses = new ArrayList<>(clashingClasses.size());
					ArrayList<ClassNode> filesystemClasses = new ArrayList<>(clashingClasses.size());

					for (ClassNode clashingClass : clashingClasses) {
						if (!clashingClass.isFromFilesystem()) {
							nonFilesystemClasses.add(clashingClass);
						} else {
							filesystemClasses.add(clashingClass);
						}
					}

					if (nonFilesystemClasses.size() > 0) {
						StringBuilder warningBuffer = new StringBuilder();
						this.buildCaseClashWarning(warningBuffer, nonFilesystemClasses, "the file system");
						if (filesystemClasses.size() > 0) {
							this.buildCaseClashWarning(warningBuffer, filesystemClasses, "archive files");
						}

						if (nonFilesystemClasses.size() + filesystemClasses.size() > 2) {
							warningBuffer.append("Some of these class names");
						} else {
							warningBuffer.append("These class names");
						}

						if (FileUtils.IS_UNIX) {
							warningBuffer.append(" would clash in a case insensitive environment.");
							this.logger.warn("WARNING:", warningBuffer.toString());
						} else {
							warningBuffer.append(" will clash in the current case insensitive environment. : ");
							warningBuffer.append(FileUtils.OS_NAME);
							this.logger.warn("WARNING:", warningBuffer.toString());
							classClashDetected = true;
						}
					}
				}
			}

			if (classClashDetected) {
				this.logger.fatal("FATAL ERROR:", "Classes would be saved to the file system with clashing mixed case names. See warnings for details.");
			}

			boolean packageClashDetected = false;
			Iterator<Entry<String, Set<String>>> packageEntryIterator = packageClashGraph.getAdjacencyEntries().iterator();

			while (packageEntryIterator.hasNext()) {
				Set<String> clashingPackageNames = packageEntryIterator.next().getValue();
				if (clashingPackageNames.size() > 1) {
					String quotedPackageNames = GlobalUtils.stringArrayToQuotedList(clashingPackageNames.toArray(new String[clashingPackageNames.size()]));
					if (FileUtils.IS_UNIX) {
						this.logger.warn("WARNING:", "Packages will be saved to the file system with names that would clash in a case insensitive environment : " + quotedPackageNames);
					} else {
						packageClashDetected = true;
						this.logger.warn("WARNING:", "Packages would be saved to the file system with clashing names : " + quotedPackageNames);
					}
				}
			}

			if (packageClashDetected) {
				this.logger.fatal("FATAL ERROR:", "Packages would be saved to the file system with clashing mixed case package names.");
			}
		}
	}

	private String getClassFilePathInDirectory(ClassNode classNode) {
		String internalName = classNode.isModule() ? "module-info" : classNode.getInternalName();
		String packagePath = internalName.substring(0, internalName.lastIndexOf(47) + 1);
		StringTokenizer packagePathTokenizer = new StringTokenizer(packagePath, "/");

		StringBuilder pathBuffer;
		for (pathBuffer = new StringBuilder(); packagePathTokenizer.hasMoreElements(); pathBuffer.append(packagePathTokenizer.hasMoreElements() ? FILE_SEPARATOR : "")) {
			pathBuffer.append(packagePathTokenizer.nextToken());
			String directoryPath = this.outputTarget.getAbsolutePath() + FILE_SEPARATOR + pathBuffer.toString();
			File directory = new File(directoryPath);
			if (!directory.exists()) {
				if (!directory.mkdirs()) {
					this.scriptContext.logWarning("Creation of directories '" + directory.getAbsolutePath() + "' failed while preparing to savie class '" + classNode.getBinaryName() + "'.");
				}
			}
		}

		String simpleName = internalName.substring(internalName.lastIndexOf(47) + 1);
		String classDirectoryPath = this.outputTarget.getAbsolutePath() + (packagePath.length() > 0 ? FILE_SEPARATOR : "") + pathBuffer.toString();
		return classDirectoryPath + FILE_SEPARATOR + simpleName + ".class";
	}

	private void backupExistingOutputFile(File outputFile) {
		String absolutePath = outputFile.getAbsolutePath();

		try {
			String backupPath = absolutePath + ".BACKUP";
			File backupFile = new File(backupPath);
			if (backupFile.exists() && !backupFile.delete()) {
				throw new Exception("Could not delete '" + backupFile.getAbsolutePath() + "'");
			}

			if (Options.ZKM_CREATE_BACKUP) {
				if (!outputFile.renameTo(backupFile)) {
					throw new Exception("Could not rename '" + outputFile.getAbsolutePath() + "' to '" + backupFile.getAbsolutePath() + "'");
				}

				this.scriptContext
					.logWarning(
						"File '" + absolutePath + "' already exists" + (this.isOutputToArchive ? "" : " in '" + this.outputTarget.getAbsolutePath()) + "'. Renaming to '" + backupPath + "'."
					);
			}
		} catch (DebugAssertFailedException debugAssertException) {
			throw debugAssertException;
		} catch (Exception backupException) {
			this.logger.error("FILE ERROR:", "Couldn't back up and recreate '" + absolutePath + "' : " + backupException.getMessage());
		}
	}

	private void closeZipStream(ZipOutputStream zipOutputStream, String outputPath) {
		try {
			zipOutputStream.close();
		} catch (IOException ioException) {
			this.scriptContext.logWarning("FILE ERROR (2) '" + outputPath + "' : " + ioException.getMessage());
		}
	}

	private void closeAllStreams() {
		Iterator<Entry<ArchiveNode, ZipFile>> archiveEntryIterator = this.inputArchiveFileCache.entrySet().iterator();

		while (archiveEntryIterator.hasNext()) {
			ZipFile zipFile = archiveEntryIterator.next().getValue();
			String archiveFilePath = zipFile.getName();

			try {
				zipFile.close();
			} catch (IOException archiveCloseException) {
				this.scriptContext.logWarning("FILE ERROR (1) '" + archiveFilePath + "' : " + archiveCloseException.getMessage());
			}
		}

		HashSet<ZipOutputStream> closedStreams = GlobalUtils.newHashSet();
		if (this.mainOutputZipStream != null) {
			this.closeZipStream(this.mainOutputZipStream, this.outputTarget.getAbsolutePath());
			closedStreams.add(this.mainOutputZipStream);
		}

		for (ArchiveNode archiveNode : this.outputStreamMap.keySet()) {
			if (archiveNode.isRoot()) {
				ZipOutputStream zipOutputStream = this.outputStreamMap.get(archiveNode);
				if (!closedStreams.contains(zipOutputStream)) {
					DebugUtils.assertNotNull(zipOutputStream, "Null zip outputstream '" + archiveNode.getDisplayPath() + "' : '" + archiveNode.getFilePath() + "'");
					this.closeZipStream(zipOutputStream, archiveNode.getRelativePath());
					File outputFile = this.outputFileHandleMap.get(archiveNode);
					if (outputFile != null && outputFile.exists()) {
						if (archiveNode.getExpectedFinalHash() != null) {
							this.validateOutputFileHash(new File(archiveNode.getDisplayPath()), outputFile, archiveNode.getExpectedFinalHash(), this.scriptContext);
						}

						if (!Options.ZKM_USE_PARALLEL && Options.ZKM_REPRODUCIBLE_RANDOM && Options.ZKM_RANDOM_SEED != null) {
							try {
								String sha256Hash = new SHA256Utils(outputFile.getAbsolutePath()).getHexDigest().toLowerCase();
								this.scriptContext.getWriter().println("\tFile '" + outputFile.getAbsolutePath() + "' saved with SHA256 hash '" + sha256Hash + "'");
							} catch (NoSuchAlgorithmException algorithmException) {
								this.scriptContext.logFatalError("'" + outputFile.getAbsolutePath() + "' couldn't be analyzed : '" + algorithmException + "' (5)");
							} catch (IOException ioException) {
								this.scriptContext.logFatalError("'" + outputFile.getAbsolutePath() + "' couldn't be analyzed : '" + ioException + "' (6)");
							}
						}
					}
				}
			}
		}
	}

	private boolean isEntryAlreadyWritten(ZipOutputStream zipOutputStream, String entryName) {
		return this.isEntryAlreadyWritten(zipOutputStream, entryName, true);
	}

	private void processAndCopyAllResources(
		ReferenceGraph<ZipOutputStream, String> writtenEntryGraph, ReferenceGraph<ArchiveNode, String> rootEntryGraph, ReferenceGraph<ArchiveNode, String> directoryEntryGraph
	) {
		Iterator<Entry<ArchiveNode, ZipFile>> inputArchiveEntryIterator = this.inputArchiveFileCache.entrySet().iterator();
		if (this.inputArchiveFileCache.size() > 0) {
			if (!Options.ZKM_COPY_ARCHIVE_DIRECTORIES) {
				this.logger.message("INFO:", "'ZKM_COPY_ARCHIVE_DIRECTORIES' command line option set to 'false'. No archive directories will not be copied.");
			} else if (Options.ZKM_DELETE_EMPTY_DIRECTORIES) {
				this.logger.message("INFO:", "'ZKM_DELETE_EMPTY_DIRECTORIES' command line option set to 'true'. Empty archive directories will not be copied.");
			}
		}

		while (inputArchiveEntryIterator.hasNext()) {
			Entry<ArchiveNode, ZipFile> inputArchiveEntry = inputArchiveEntryIterator.next();
			ArchiveNode archiveNode = inputArchiveEntry.getKey();
			String relativePath = archiveNode.getRelativePath();
			ZipFile zipFile = inputArchiveEntry.getValue();
			ZipOutputStream outputStream = this.outputStreamMap.get(archiveNode);
			if (outputStream != null) {
				boolean emptyDirectoriesDeleted = false;
				Set<String> emptyDirectories = findEmptyDirectories(zipFile.entries(), writtenEntryGraph.getReferences(outputStream));
				if (Options.ZKM_DELETE_EMPTY_DIRECTORIES || this.deleteEmptyDirectories) {
					emptyDirectoriesDeleted = true;
				}

				Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();
				HashSet<String> seenDirectoryPaths = GlobalUtils.newHashSet();

				while (zipEntryEnumeration.hasMoreElements()) {
					ZipEntry zipEntry = zipEntryEnumeration.nextElement();
					String entryName = zipEntry.getName();
					boolean deflated = this.compressionFlag == 1 || this.compressionFlag == 3 && zipEntry.getMethod() == 8;
					if (this.isNestedArchive(zipFile, zipEntry, this.logger)) {
						if (this.deletedResources == null || !this.deletedResources.contains(archiveNode, entryName)) {
							ArchiveNode childArchive = archiveNode.getChildByPath(entryName);
							if (this.skippedArchives != null && this.skippedArchives.contains(childArchive)) {
								try {
									InputStream entryInputStream = null;

									try {
										entryInputStream = zipFile.getInputStream(zipEntry);
										if (entryInputStream != null) {
											if (!this.isEntryAlreadyWritten(outputStream, entryName, false)) {
												ZipEntryWriter zipEntryWriter = new ZipEntryWriter(outputStream, entryName, deflated, entryInputStream, (int)zipEntry.getSize());
												if (Options.ZKM_KEEP_SKIPPED_LAST_MODIFIED_DATE) {
													zipEntryWriter.writeEntry(zipEntry.getTime());
												} else {
													zipEntryWriter.writeEntry(this.globalTimestamp);
												}

												this.writtenEntryTracker.addReference(outputStream, entryName);
											}
										} else {
											this.scriptContext
												.logWarning("File Error: Couldn't copy '" + entryName + "' from '" + relativePath + "' to new archive because the entry appears to be corrupt (A)");
										}
									} finally {
										if (entryInputStream != null) {
											try {
												entryInputStream.close();
											} catch (IOException ignored) {
											}
										}
									}
								} catch (IOException copyException) {
									this.logger.error("FILE ERROR:", "Couldn't copy '" + entryName + "' from '" + relativePath + "' to new archive (A) : " + copyException.getMessage());
								}
							}
						}
					} else if ((!entryName.endsWith(".class") || this.skippedClassResources != null && this.skippedClassResources.contains(archiveNode, entryName))
						&& !this.isNestedArchive(zipFile, zipEntry, this.logger)) {
						String upperEntryName = entryName.toUpperCase();
						String lowerEntryName = entryName.toLowerCase();
						this.statusReporter.set("Saving " + entryName);
						if (zipEntry.isDirectory()) {
							if (Options.ZKM_COPY_ARCHIVE_DIRECTORIES) {
								String remappedPath = this.pathRemapper.remapPathWithCache(entryName);
								if (remappedPath.length() > 0 && seenDirectoryPaths.add(remappedPath) && (!emptyDirectoriesDeleted || emptyDirectories == null || !emptyDirectories.contains(entryName))) {
									if (this.isEntryAlreadyWritten(outputStream, remappedPath)) {
										continue;
									}

									this.copyZipEntry(zipFile, zipEntry, outputStream, entryName, remappedPath, deflated);
								}
							}
						} else if ((this.deletedResources == null || !this.deletedResources.contains(archiveNode, entryName))
							&& !upperEntryName.equals("META-INF/MANIFEST.MF")
							&& (!upperEntryName.startsWith("META-INF/services/".toUpperCase()) || entryName.length() <= ServiceProviderFileProcessor.SERVICE_PREFIX_LENGTH)) {
							if (this.skippedXmlResources != null && this.skippedXmlResources.contains(archiveNode, entryName)) {
								if (this.isEntryAlreadyWritten(outputStream, entryName)) {
									continue;
								}

								this.copyZipEntry(zipFile, zipEntry, outputStream, entryName, entryName, deflated, true);
							} else if (FileUtils.isXmlFile(lowerEntryName)) {
								try {
									AtomicReference<String> xmlEncoding = new AtomicReference<>();
									AtomicInt xmlBomSkipLength = new AtomicInt(0);
									AtomicInt xmlBomLength = new AtomicInt(0);
									AtomicInt xmlByteOrder = new AtomicInt(Integer.MAX_VALUE);

									String xmlContent;
									try {
										xmlContent = XmlParser.readZipEntry(zipFile, zipEntry, xmlEncoding, xmlBomSkipLength, xmlBomLength, xmlByteOrder);
									} catch (OutOfMemoryError outOfMemoryError) {
										this.logger.error("FILE ERROR:", "OutOfMemoryError reading or writing '" + entryName + "' in '" + relativePath + "' : " + zipEntry.getSize() + " : " + outOfMemoryError);
										throw outOfMemoryError;
									}

									if (this.isEntryAlreadyWritten(outputStream, this.pathRemapper.remapPathWithCache(entryName))) {
										continue;
									}

									XmlHandler xmlHandler = AbstractFrameworkXmlHandler.createHandler(
										FileUtils.getResolvedZipEntryPath(zipFile, zipEntry),
										xmlContent,
										this.classRenameMap,
										this.fullRemappingMap,
										this.fieldRemappingRegistry,
										this.methodRemappingRegistry,
										this.pathRemapper,
										this.fileAdaptor,
										this.workspace,
										this.typeResolver.getClassProvider(),
										this.logger
									);
									new XmlResourceProcessor(
										zipFile, zipEntry, this.globalTimestamp, xmlBomSkipLength, xmlBomLength, xmlByteOrder, xmlEncoding.get(), outputStream, this.pathRemapper, xmlHandler, deflated, this.deleteXmlComments
									);
									this.writtenEntryTracker.addReference(outputStream, entryName);
								} catch (ZKMException xmlException) {
									this.logger.error("FILE ERROR:", "Invalid '" + entryName + "' in '" + relativePath + "' : " + xmlException.getMessage());
								} catch (IOException xmlIoException) {
									this.logger.error("FILE ERROR:", "Error reading or writing '" + entryName + "' in '" + relativePath + "' : " + xmlIoException);
								}
							} else if (this.skippedYamlResources != null && this.skippedYamlResources.contains(archiveNode, entryName)) {
								if (this.isEntryAlreadyWritten(outputStream, entryName)) {
									continue;
								}

								this.copyZipEntry(zipFile, zipEntry, outputStream, entryName, entryName, deflated, true);
							} else if (FileUtils.isYamlFile(lowerEntryName)) {
								try {
									if (this.isEntryAlreadyWritten(outputStream, this.pathRemapper.remapPathWithCache(entryName))) {
										continue;
									}

									AtomicInt yamlBomSkipLength = new AtomicInt(0);
									AtomicInt yamlBomLength = new AtomicInt(0);
									AtomicInt yamlByteOrder = new AtomicInt(Integer.MAX_VALUE);
									InputStream yamlInputStream = zipFile.getInputStream(zipEntry);
									PushbackInputStream yamlPushbackStream = new PushbackInputStream(yamlInputStream, FileUtils.BOM_BUFFER_SIZE);
									String yamlEncoding = FileUtils.detectEncodingFromBOM(FileUtils.peekBytes(yamlPushbackStream, FileUtils.BOM_BUFFER_SIZE), yamlBomSkipLength, yamlBomLength, yamlByteOrder);
									yamlPushbackStream.close();
									if (yamlEncoding == null) {
										yamlEncoding = "UTF-8";
									}

									new GenericTextFileRemapper(
										zipFile, zipEntry, this.globalTimestamp, yamlBomSkipLength, yamlBomLength, yamlByteOrder, yamlEncoding, outputStream, this.pathRemapper, deflated, this.classRenameMap
									);
									this.writtenEntryTracker.addReference(outputStream, entryName);
								} catch (IOException yamlIoException) {
									this.logger.error("FILE ERROR:", "Error reading or writing '" + entryName + "' in '" + relativePath + "' : " + yamlIoException);
								}
							} else if (this.skippedPropertiesResources != null && this.skippedPropertiesResources.contains(archiveNode, entryName)) {
								if (this.isEntryAlreadyWritten(outputStream, entryName)) {
									continue;
								}

								this.copyZipEntry(zipFile, zipEntry, outputStream, entryName, entryName, deflated, true);
							} else if (FileUtils.isPropertiesFile(lowerEntryName)) {
								try {
									if (this.isEntryAlreadyWritten(outputStream, this.pathRemapper.remapPathWithCache(entryName))) {
										continue;
									}

									AtomicInt propertiesBomSkipLength = new AtomicInt(0);
									AtomicInt propertiesBomLength = new AtomicInt(0);
									AtomicInt propertiesByteOrder = new AtomicInt(Integer.MAX_VALUE);
									InputStream propertiesInputStream = zipFile.getInputStream(zipEntry);
									PushbackInputStream propertiesPushbackStream = new PushbackInputStream(propertiesInputStream, FileUtils.BOM_BUFFER_SIZE);
									String propertiesEncoding = FileUtils.detectEncodingFromBOM(FileUtils.peekBytes(propertiesPushbackStream, FileUtils.BOM_BUFFER_SIZE), propertiesBomSkipLength, propertiesBomLength, propertiesByteOrder);
									propertiesPushbackStream.close();
									if (propertiesEncoding == null) {
										if (Options.ZKM_PROPERTIES_ENCODING != null) {
											propertiesEncoding = Options.ZKM_PROPERTIES_ENCODING.trim();
										} else {
											propertiesEncoding = "UTF-8";
										}
									}

									new PropertiesFileRemapper(
										zipFile,
										zipEntry,
										this.globalTimestamp,
										propertiesBomSkipLength,
										propertiesBomLength,
										propertiesByteOrder,
										propertiesEncoding,
										outputStream,
										this.pathRemapper,
										deflated,
										this.classRenameMap,
										new AtomicReference<>()
									);
									this.writtenEntryTracker.addReference(outputStream, entryName);
								} catch (IOException propertiesIoException) {
									this.logger.error("FILE ERROR:", "Error reading or writing '" + entryName + "' in '" + relativePath + "' : " + propertiesIoException);
								}
							} else if (entryName.equalsIgnoreCase("META-INF/INDEX.LIST")) {
								try {
									IndexListProcessor indexListProcessor = new IndexListProcessor(archiveNode, zipFile, zipEntry);
									if (this.isEntryAlreadyWritten(outputStream, "META-INF/INDEX.LIST")) {
										continue;
									}

									indexListProcessor.writeRemappedIndex(outputStream, this.globalTimestamp, rootEntryGraph, directoryEntryGraph, deflated);
									this.writtenEntryTracker.addReference(outputStream, "META-INF/INDEX.LIST");
								} catch (ZKMException indexListException) {
									this.logger.error("FILE ERROR:", "Invalid INDEX.LIST file '" + entryName + "' in '" + relativePath + "' : " + indexListException.getMessage());
								} catch (IOException indexListIoException) {
									this.logger.error("FILE ERROR:", "Error reading or writing INDEX.LIST file '" + entryName + "' in '" + relativePath + "' : " + indexListIoException);
								}
							} else if (!upperEntryName.startsWith("META-INF/") || !upperEntryName.endsWith(".SF") && !upperEntryName.endsWith(".DSA") && !upperEntryName.endsWith(".RSA") && !upperEntryName.endsWith(".PGP")) {
								String entryPath;
								if (this.skippedClassResources != null && this.skippedClassResources.contains(archiveNode, entryName)) {
									entryPath = entryName;
								} else {
									entryPath = this.pathRemapper.remapPathWithCache(entryName);
								}

								if (this.isEntryAlreadyWritten(outputStream, entryPath)) {
									continue;
								}

								this.copyZipEntry(zipFile, zipEntry, outputStream, entryName, entryPath, deflated);
							} else if (upperEntryName.endsWith(".SF")) {
								this.scriptContext.logWarning("File '" + relativePath + "' appears to have been signed. Signature data removed. You must re-sign AFTER obfuscation.");
							}
						}

						this.statusReporter.set(" ");
					}
				}
			}
		}
	}

	private void saveGeneratedClasses(
		IReferenceGraph<ClassNode, ClassNode> generatedClassGraph,
		Map<ClasspathResource, String> classDirPaths,
		ReferenceGraph<ArchiveNode, String> rootEntryGraph,
		ReferenceGraph<ArchiveNode, String> directoryEntryGraph
	) {
		if (!generatedClassGraph.isEmpty()) {
			int archiveCount = this.allArchiveNodes.length;
			ReferenceGraphMap<ArchiveNode, ClassNode, String> classesByArchive = new ReferenceGraphMap<>(archiveCount, generatedClassGraph.size(), 7);
			HashSet<ClassNode> classesWithoutArchive = GlobalUtils.newHashSet();

			for (ClassNode classNode : generatedClassGraph.getMembers()) {
				Iterator<ClassNode> referenceIterator = generatedClassGraph.getReferencesAsList(classNode).iterator();

				while (referenceIterator.hasNext()) {
					Enumeration<ClasspathResource> classSourceEnumeration = referenceIterator.next().getClassSources();

					while (classSourceEnumeration.hasMoreElements()) {
						ClasspathResource classResource = classSourceEnumeration.nextElement();
						ArchiveNode sourceArchiveNode = classResource.getArchiveNode();
						if (sourceArchiveNode != null) {
							String classDirPath = classDirPaths.get(classResource);
							classesByArchive.addReference(sourceArchiveNode, classNode, classDirPath);
						} else {
							classesWithoutArchive.add(classNode);
						}
					}
				}
			}

			for (ClassNode unarchivedClass : classesWithoutArchive) {
				if (!this.isOutputToArchive) {
					this.saveClassToDirectory(unarchivedClass, (byte[])null);
				} else {
					String classEntryPath = this.getClassEntryPath(unarchivedClass);
					boolean mainEntryDeflated = this.compressionFlag == 1;
					if (!this.isEntryAlreadyWritten(this.mainOutputZipStream, classEntryPath)) {
						this.saveClassToArchive(unarchivedClass, null, classEntryPath, this.mainOutputZipStream, this.outputTarget, this.outputTarget.getAbsolutePath(), mainEntryDeflated);
					}
				}
			}

			boolean archiveEntryDeflated = this.compressionFlag == 1 || this.compressionFlag == 3;

			for (Entry<ArchiveNode, ReferenceGraph<ClassNode, String>> archiveEntry : classesByArchive.getEntrySet()) {
				ArchiveNode archiveNode = archiveEntry.getKey();
				ZipFile zipFile = this.inputArchiveFileCache.get(archiveNode);
				ZipOutputStream outputStream = this.outputStreamMap.get(archiveNode);

				for (Entry<ClassNode, Set<String>> adjacencyEntry : archiveEntry.getValue().getAdjacencyEntries()) {
					ClassNode adjacentClassNode = adjacencyEntry.getKey();

					for (String dirPath : adjacencyEntry.getValue()) {
						String entryPath = dirPath + adjacentClassNode.getInternalName() + ".class";
						if (!this.isEntryAlreadyWritten(outputStream, entryPath)) {
							this.saveClassToArchive(adjacentClassNode, null, entryPath, outputStream, this.outputFileHandleMap.get(archiveNode), zipFile.getName(), archiveEntryDeflated);
							this.trackResourcePath(entryPath, archiveNode, rootEntryGraph, directoryEntryGraph);
						}
					}
				}
			}
		}
	}

	private void writeNestedArchives() {
		this.statusReporter.set("Writing nested archive files");

		for (int i = 0; i < this.rootArchives.size(); i++) {
			ArchiveNode rootArchive = this.rootArchives.get(i);
			Enumeration<ArchiveNode> childArchiveEnumeration = rootArchive.getChildren();

			while (childArchiveEnumeration.hasMoreElements()) {
				ArchiveNode childArchive = childArchiveEnumeration.nextElement();
				if (this.skippedArchives == null || !this.skippedArchives.contains(childArchive)) {
					this.writeNestedArchive(childArchive, rootArchive);
				}
			}
		}

		this.statusReporter.set(" ");
	}

	private void processServiceFiles() {
		MultiMap<ZipOutputStream, ArchiveNode> archivesByOutputStream = new MultiMap<>();

		for (Entry<ArchiveNode, ZipOutputStream> outputMapEntry : this.outputStreamMap.entrySet()) {
			archivesByOutputStream.put(outputMapEntry.getValue(), outputMapEntry.getKey());
		}

		for (Entry<ZipOutputStream, List<ArchiveNode>> outputStreamEntry : archivesByOutputStream.entrySet()) {
			ZipOutputStream outputStream = outputStreamEntry.getKey();
			List<ArchiveNode> archives = outputStreamEntry.getValue();
			MultiMap<String, ServiceProviderFileProcessor> processorsByServiceName = new MultiMap<>();

			for (ArchiveNode archiveNode : archives) {
				ZipFile zipFile = this.inputArchiveFileCache.get(archiveNode);
				Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();

				while (zipEntryEnumeration.hasMoreElements()) {
					ZipEntry zipEntry = zipEntryEnumeration.nextElement();
					String entryName = zipEntry.getName();
					if (entryName.toUpperCase().startsWith("META-INF/services/".toUpperCase()) && entryName.length() > ServiceProviderFileProcessor.SERVICE_PREFIX_LENGTH) {
						try {
							ServiceProviderFileProcessor serviceProcessor = new ServiceProviderFileProcessor(zipFile, zipEntry);
							String serviceInterfaceName = serviceProcessor.getServiceInterfaceName();
							processorsByServiceName.put(serviceInterfaceName, serviceProcessor);
						} catch (IOException serviceReadException) {
							this.logger.error("FILE ERROR:", "Error reading service provider in '" + zipFile.getName() + "' : " + serviceReadException);
						}
					}
				}
			}

			for (Entry<String, List<ServiceProviderFileProcessor>> serviceEntry : processorsByServiceName.entrySet()) {
				String serviceName = serviceEntry.getKey();
				Iterator<ServiceProviderFileProcessor> processorIterator = serviceEntry.getValue().iterator();
				ServiceProviderFileProcessor mergedProcessor = processorIterator.next();

				while (processorIterator.hasNext()) {
					ServiceProviderFileProcessor additionalProcessor = processorIterator.next();
					mergedProcessor.merge(additionalProcessor);
				}

				String remappedPath = mergedProcessor.getRemappedFilePath(this.classRenameMap);
				if (!this.isEntryAlreadyWritten(outputStream, remappedPath)) {
					boolean deflated = this.compressionFlag == 1 || this.compressionFlag == 3 && mergedProcessor.isDeflated();

					try {
						mergedProcessor.writeRemappedServiceFile(outputStream, this.globalTimestamp, this.classRenameMap, deflated);
					} catch (IOException serviceWriteException) {
						this.logger.error("FILE ERROR:", "Error writing service provider 'META-INF/services/" + serviceName + "' : " + serviceWriteException);
					}

					this.writtenEntryTracker.addReference(outputStream, remappedPath);
				}
			}
		}
	}

	private Set<String> buildClassResourcePathMaps(Map<ClasspathResource, String> classEntryPaths, Map<ClasspathResource, String> classDirPaths) {
		HashSet<String> packagePrefixes = GlobalUtils.newHashSet();

		for (ClassNode classNode : this.allClassNodes) {
			Enumeration<ClasspathResource> classSourceEnumeration = classNode.getClassSources();

			while (classSourceEnumeration.hasMoreElements()) {
				ClasspathResource classResource = classSourceEnumeration.nextElement();
				if (classResource.isZipEntry()) {
					this.buildClassResourcePathEntry(classResource, classNode, packagePrefixes, classEntryPaths, classDirPaths);
				}
			}

			if (classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClassNode : classNode.getInnerClasses()) {
					ClasspathResource innerClassResource = innerClassNode.getPrimaryClassSource();
					this.buildClassResourcePathEntry(innerClassResource, (ClassNode)innerClassNode, packagePrefixes, classEntryPaths, classDirPaths);
				}
			}
		}

		return packagePrefixes;
	}

	private boolean hasCustomOutputPath(ArchiveNode archiveNode) {
		return this.customArchiveOutputPaths != null && this.customArchiveOutputPaths.containsKey(archiveNode);
	}

	private void buildResourceGraphs(
		Map<ClasspathResource, String> classEntryPaths, PathRemapper pathRemapper, ReferenceGraph<ArchiveNode, String> rootEntryGraph, ReferenceGraph<ArchiveNode, String> directoryEntryGraph
	) {
		for (int i = 0; i < this.allClassNodes.length; i++) {
			Enumeration<ClasspathResource> classSourceEnumeration = this.allClassNodes[i].getClassSources();

			while (classSourceEnumeration.hasMoreElements()) {
				ClasspathResource classResource = classSourceEnumeration.nextElement();
				if (classResource.isZipEntry()) {
					ArchiveNode sourceArchiveNode = classResource.getArchiveNode();
					String classEntryPath = classEntryPaths.get(classResource);
					this.trackResourcePath(classEntryPath, sourceArchiveNode, rootEntryGraph, directoryEntryGraph);
				}
			}
		}

		for (Entry<ArchiveNode, ZipFile> inputArchiveEntry : this.inputArchiveFileCache.entrySet()) {
			ArchiveNode archiveNode = inputArchiveEntry.getKey();
			Enumeration<? extends ZipEntry> zipEntryEnumeration = inputArchiveEntry.getValue().entries();

			while (zipEntryEnumeration.hasMoreElements()) {
				ZipEntry zipEntry = zipEntryEnumeration.nextElement();
				if (!zipEntry.isDirectory()) {
					String entryName = zipEntry.getName();
					String upperEntryName = entryName.toUpperCase();
					if (!entryName.toLowerCase().endsWith(".class") && !upperEntryName.startsWith("META-INF/")) {
						String remappedPath = pathRemapper.remapPathWithCache(entryName);
						int lastSeparator = remappedPath.lastIndexOf("/");
						if (lastSeparator == -1) {
							rootEntryGraph.addReference(archiveNode, remappedPath);
						} else {
							String directoryPath = remappedPath.substring(0, lastSeparator);
							directoryEntryGraph.addReference(archiveNode, directoryPath);
						}
					}
				}
			}
		}
	}

	private String getClassEntryPath(ClassNode classNode) {
		String internalName = classNode.isModule() ? "module-info" : classNode.getInternalName();
		String packagePath = StringUtils.replace(AbstractClassNode.getPackageName(internalName), FILE_SEPARATOR, "/");
		String simpleName = AbstractClassNode.getSimpleNameFromInternalName(internalName);
		return (packagePath.length() > 0 ? packagePath + "/" : "") + simpleName + ".class";
	}

	private void buildClassResourcePathEntry(
		ClasspathResource classResource, ClassNode classNode, Set<String> packagePrefixes, Map<ClasspathResource, String> classEntryPaths, Map<ClasspathResource, String> classDirPaths
	) {
		String resourceName = classResource.getResourceName();
		resourceName = resourceName.substring(0, resourceName.lastIndexOf(".class"));
		String internalName = classNode.isModule() ? "module-info" : classNode.getInternalName();
		String mappedInternalName = GlobalUtils.getMappedOrDefault(internalName, this.fullRemappingMap);
		int mappedNameIndex = resourceName.lastIndexOf(mappedInternalName);
		String entryPath;
		if (mappedNameIndex > 0) {
			String classDirPath = resourceName.substring(0, mappedNameIndex);
			entryPath = classDirPath + internalName + ".class";
			if (classDirPath.charAt(0) == '/') {
				classDirPath = classDirPath.substring(1);
			}

			packagePrefixes.add(classDirPath);
			classDirPaths.put(classResource, classDirPath);
		} else {
			entryPath = internalName + ".class";
			classDirPaths.put(classResource, "");
		}

		classEntryPaths.put(classResource, entryPath);
	}

	private boolean isNestedArchive(ZipFile zipFile, ZipEntry zipEntry, AbstractLogger logger) {
		try {
			return FileUtils.isNestedArchive(zipFile, zipEntry);
		} catch (IOException ioException) {
			logger.error("FILE ERROR:", "File read error : " + ioException.getMessage());
			return false;
		}
	}

	private void validateOutputFileHash(File sourceFile, BufferedInputStream outputFile, String expectedHash, ScriptContext scriptContext) {
		try {
			String actualHash = new SHA256Utils(outputFile).getHexDigest().toLowerCase();
			if (!expectedHash.equals(actualHash)) {
				scriptContext.logFatalError(
					"File '" + sourceFile.getAbsolutePath() + "' was expected to be saved with the SHA256 hash '" + expectedHash + "' but the actual hash was '" + actualHash + "'"
				);
			}
		} catch (NoSuchAlgorithmException noSuchAlgorithmException) {
			scriptContext.logFatalError("'" + sourceFile.getAbsolutePath() + "' couldn't be analyzed : '" + noSuchAlgorithmException + "' (9)");
		} catch (IOException ioException) {
			scriptContext.logFatalError("'" + sourceFile.getAbsolutePath() + "' couldn't be analyzed : '" + ioException + "' (10)");
		}
	}

	private void processExternalTextFiles(ClasspathResource[] textFiles, HashMap<?, File> customOutputPaths, boolean useDefaultEncoding) {
		if (textFiles != null) {
			for (ClasspathResource textFileResource : textFiles) {
				String textFilePath = textFileResource.getFullPath();
				File textFile = textFileResource.getAsFile();
				String expectedFinalHash = textFileResource.getExpectedFinalHash();
				AtomicInt bomBytesToSkip = new AtomicInt(0);
				AtomicInt bomType = new AtomicInt(0);
				AtomicInt byteOrder = new AtomicInt(Integer.MAX_VALUE);
				FileInputStream fileInputStream = null;
				PushbackInputStream pushbackInputStream = null;
				boolean resourceGuard = false;

				try {
					resourceGuard = true;
					fileInputStream = new FileInputStream(textFileResource.getAsFile());
					pushbackInputStream = new PushbackInputStream(fileInputStream, FileUtils.BOM_BUFFER_SIZE);
					String detectedCharset = FileUtils.detectEncodingFromBOM(FileUtils.peekBytes(pushbackInputStream, FileUtils.BOM_BUFFER_SIZE), bomBytesToSkip, bomType, byteOrder);
					pushbackInputStream.close();
					fileInputStream = null;
					pushbackInputStream = null;
					if (detectedCharset == null && useDefaultEncoding) {
						if (Options.ZKM_PROPERTIES_ENCODING != null) {
							detectedCharset = Options.ZKM_PROPERTIES_ENCODING.trim();
						} else {
							detectedCharset = "UTF-8";
						}
					}

					if (this.isOutputToArchive) {
						ZipOutputStream outputZipStream = this.mainOutputZipStream;
						if (!this.isEntryAlreadyWritten(outputZipStream, textFileResource.getResourceName())) {
							AtomicReference<byte[]> remappedBytes = new AtomicReference<>();
							if (FileUtils.isYamlFile(textFileResource.getResourceName())) {
								boolean yamlDeflated = this.compressionFlag == 1;
								ReadOnlyMap<String, String> yamlClassRenameMap = this.classRenameMap;
								new GenericTextFileRemapper(textFile, bomBytesToSkip, bomType, byteOrder, detectedCharset, outputZipStream, yamlDeflated, yamlClassRenameMap, remappedBytes);
							} else {
								boolean propertiesDeflated = this.compressionFlag == 1;
								ReadOnlyMap<String, String> propertiesClassRenameMap = this.classRenameMap;
								new PropertiesFileRemapper(textFile, bomBytesToSkip, bomType, byteOrder, detectedCharset, outputZipStream, propertiesDeflated, propertiesClassRenameMap, remappedBytes);
							}

							if (expectedFinalHash != null) {
								this.validateOutputFileHash(textFile, new BufferedInputStream(new ByteArrayInputStream(remappedBytes.get())), expectedFinalHash, this.scriptContext);
							}

							this.writtenEntryTracker.addReference(outputZipStream, textFileResource.getResourceName());
							resourceGuard = false;
						} else {
							resourceGuard = false;
						}
						continue;
					}

					File outputFile;
					if (customOutputPaths != null && customOutputPaths.containsKey(textFileResource)) {
						outputFile = customOutputPaths.get(textFileResource);
					} else {
						outputFile = new File(this.outputTarget, textFile.getName());
					}

					if (FileUtils.isYamlFile(textFile.getName())) {
						new GenericTextFileRemapper(textFile, bomBytesToSkip, bomType, byteOrder, detectedCharset, outputFile, this.classRenameMap);
					} else {
						new PropertiesFileRemapper(textFile, bomBytesToSkip, bomType, byteOrder, detectedCharset, outputFile, this.classRenameMap);
					}

					if (this.scriptContext.isVerbose()) {
						this.scriptContext
							.getWriter()
							.println(
								"\t"
									+ (FileUtils.isYamlFile(textFileResource.getResourceName()) ? "YAML" : "PROPERTIES")
									+ " file '"
									+ textFile.getAbsolutePath()
									+ "' saved to '"
									+ outputFile.getAbsolutePath()
									+ "'"
									+ (!Options.ZKM_USE_PARALLEL && Options.ZKM_REPRODUCIBLE_RANDOM ? " with SHA256 hash '" + getSha256Hash(outputFile, this.scriptContext) + "'" : "")
									+ "."
							);
					}

					if (expectedFinalHash != null) {
						this.validateOutputFileHash(textFile, outputFile, expectedFinalHash, this.scriptContext);
						resourceGuard = false;
					} else {
						resourceGuard = false;
					}
					continue;
				} catch (IOException ioException) {
					this.logger
						.error(
							"FILE ERROR:", "Error reading or writing " + (FileUtils.isYamlFile(textFileResource.getResourceName()) ? "YAML" : "PROPERTIES") + " file '" + textFilePath + "' : " + ioException
						);
					resourceGuard = false;
				} finally {
					if (resourceGuard) {
						if (pushbackInputStream != null) {
							try {
								pushbackInputStream.close();
							} catch (IOException ignoredPushbackClose) {
							}
						} else if (fileInputStream != null) {
							try {
								fileInputStream.close();
							} catch (IOException ignoredFileClose) {
							}
						}
					}
				}

				if (pushbackInputStream != null) {
					try {
						pushbackInputStream.close();
					} catch (IOException ignoredPushbackCloseAfter) {
					}
				} else if (fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (IOException ignoredFileCloseAfter) {
					}
				}
			}
		}
	}

	private void saveClassToArchive(ClassNode classNode, byte[] classBytes, String entryName, ZipOutputStream zipOutputStream, File outputFile, String sourceArchivePath, boolean isDeflated) {
		try {
			ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream, entryName, isDeflated);
			DataOutputStream dataOutputStream = new DataOutputStream(zipEntryWriter.getOutputStream());

			try {
				if (this.scriptContext.isVerbose()) {
					String originalName = TempFileManager.getOriginalName(outputFile.getAbsolutePath());
					this.scriptContext
						.getWriter()
						.println("\tClass file '" + classNode.getSourcePath() + "' saved to '" + (originalName == null ? outputFile.getAbsolutePath() : originalName) + "' as '" + entryName + "'.");
				}

				if (classBytes != null) {
					dataOutputStream.write(classBytes);
				} else {
					classNode.write(dataOutputStream, this.typeResolver, this.workspace, this.scriptContext);
					dataOutputStream.flush();
				}

				zipEntryWriter.writeEntry(this.globalTimestamp);
				this.writtenEntryTracker.addReference(zipOutputStream, entryName);
			} catch (MethodAnalyzerException methodAnalyzerException) {
				this.logger.fatal("ERROR:", "Method '" + methodAnalyzerException.getMethodName() + "' could not be analyzed (G). Consider excluding it from Flow and Exception Obfuscation.");
			} catch (ProcessingException processingException) {
				this.logger.fatal("ERROR:", processingException.getMessage());
			}
		} catch (IOException ioException) {
			this.logger
				.fatal("FILE ERROR:", "Couldn't save '" + classNode.getBinaryName() + "' from '" + sourceArchivePath + "' to new archive (C) : '" + entryName + "' : " + ioException.getMessage());
		}
	}

	private void processManifestFiles() {
		for (ArchiveNode inputArchive : this.inputArchiveFileCache.keySet()) {
			if (this.skippedArchives == null || !this.skippedArchives.contains(inputArchive)) {
				ZipOutputStream outputZipStream = this.outputStreamMap.get(inputArchive);
				ManifestFile manifestFile = inputArchive.getManifest();
				if (outputZipStream != null
					&& manifestFile != null
					&& (this.deletedResources == null || !this.deletedResources.contains(inputArchive, "META-INF/MANIFEST.MF"))
					&& !this.isEntryAlreadyWritten(outputZipStream, "META-INF/MANIFEST.MF")) {
					boolean isDeflated = this.compressionFlag == 1 || this.compressionFlag == 3 && manifestFile.isDeflated();

					try {
						manifestFile.write(outputZipStream, this.globalTimestamp, this.packageRenameMap, this.classRenameMap, this.pathRemapper, isDeflated, this.logger);
						this.writtenEntryTracker.addReference(outputZipStream, "META-INF/MANIFEST.MF");
					} catch (IOException ioException) {
						this.logger.error("FILE ERROR:", "Couldn't write MANIFEST.MF to '" + this.outputStreamPathMap.get(outputZipStream) + "' : " + ioException);
					}
				}
			}
		}
	}

	private void processJadFiles() {
		if (this.jadFileMap != null) {
			for (Entry<JadFileProcessor, ArchiveNode> jadFileEntry : this.jadFileMap.entrySet()) {
				JadFileProcessor jadFileProcessor = jadFileEntry.getKey();
				ArchiveNode archiveNode = jadFileEntry.getValue();
				if (archiveNode != null) {
					File archiveOutputFile = this.outputFileHandleMap.get(archiveNode);
					if (archiveOutputFile != null) {
						jadFileProcessor.setJarSize(archiveOutputFile.length());
					}
				}

				File jadOutputFile;
				if (this.customJadOutputPaths != null && this.customJadOutputPaths.containsKey(jadFileProcessor)) {
					jadOutputFile = this.customJadOutputPaths.get(jadFileProcessor);
					if (this.scriptContext.isVerbose()) {
						this.scriptContext.getWriter().println("\tJAD file '" + jadFileProcessor.getFullPath() + "' saved to '" + jadOutputFile.getAbsolutePath() + "'.");
					}
				} else {
					jadOutputFile = new File(this.outputTarget, jadFileProcessor.getFileName());
				}

				try {
					jadFileProcessor.writeRemappedJad(jadOutputFile, this.packageRenameMap, this.classRenameMap, this.pathRemapper, archiveNode.getFilePath(), this.logger);
				} catch (ZKMException zkmException) {
					this.logger.error("JAD FILE ERROR:", "JAD file write error : " + zkmException.getMessage());
				}
			}
		}
	}

	private void copyClassFileToArchive(String sourceFilePath, String targetEntryName, ZipOutputStream zipOutputStream, boolean isDeflated) {
		ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream, targetEntryName, isDeflated, new File(sourceFilePath));

		try {
			zipEntryWriter.writeEntry(this.globalTimestamp);
			this.writtenEntryTracker.addReference(zipOutputStream, targetEntryName);
		} catch (IOException ioException) {
			this.logger.error("FILE ERROR:", "Couldn't save '" + sourceFilePath + "' to new archive (A) : " + ioException.getMessage());
		}
	}

	private void buildCaseClashWarning(StringBuilder warning, List<ClassNode> clashingClasses, String saveLocation) {
		if (clashingClasses.size() > 1) {
			warning.append("Classes ");
		} else {
			warning.append("Class ");
		}

		String[] sourcePaths = new String[clashingClasses.size()];

		for (int sourcePathIndex = 0; sourcePathIndex < clashingClasses.size(); sourcePathIndex++) {
			sourcePaths[sourcePathIndex] = clashingClasses.get(sourcePathIndex).getSourcePath();
		}

		warning.append(GlobalUtils.stringArrayToQuotedList(sourcePaths));
		String[] binaryNames = new String[clashingClasses.size()];

		for (int binaryNameIndex = 0; binaryNameIndex < clashingClasses.size(); binaryNameIndex++) {
			binaryNames[binaryNameIndex] = clashingClasses.get(binaryNameIndex).getBinaryName();
		}

		warning.append(" will be saved to ");
		warning.append(saveLocation);
		warning.append(" as ");
		warning.append(GlobalUtils.stringArrayToQuotedList(binaryNames));
		warning.append(". ");
	}

	private void writeNestedArchive(ArchiveNode nestedArchive, ArchiveNode parentArchive) {
		Enumeration<ArchiveNode> childArchives = nestedArchive.getChildren();

		while (childArchives.hasMoreElements()) {
			ArchiveNode childArchive = childArchives.nextElement();
			if (this.skippedArchives == null || !this.skippedArchives.contains(childArchive)) {
				this.writeNestedArchive(childArchive, nestedArchive);
			}
		}

		ZipOutputStream parentZipStream = this.outputStreamMap.get(parentArchive);
		ZipOutputStream nestedZipStream = this.outputStreamMap.get(nestedArchive);
		DebugUtils.assertNotNull(nestedZipStream, "Problem with " + nestedArchive.getFullPath() + " : " + nestedArchive.getFilePath());

		try {
			try {
				nestedZipStream.close();
				File nestedOutputFile = this.outputFileHandleMap.get(nestedArchive);
				ZipEntry parentEntry = this.inputArchiveFileCache.get(parentArchive).getEntry(nestedArchive.getRelativePath());
				boolean isDeflated = this.compressionFlag == 1 || this.compressionFlag == 3 && parentEntry.getMethod() == 8;
				new ZipEntryWriter(parentZipStream, nestedArchive.getRelativePath(), isDeflated, nestedOutputFile).writeEntry(this.globalTimestamp);
				this.writtenEntryTracker.addReference(nestedZipStream, nestedArchive.getRelativePath());
			} catch (IOException ioException) {
				this.logger.error("FILE ERROR:", "Couldn't write " + nestedArchive.getRelativePath() + " to '" + parentArchive.getRelativePath() + "' : " + ioException);
			}
		} finally {
			;
		}
	}

	private void processAndSaveAllClasses(
		IReferenceGraph<ClassNode, ClassNode> generatedClassGraph,
		Map<ClasspathResource, String> classEntryPaths,
		ReferenceGraph<ZipOutputStream, String> writtenEntryGraph,
		Map<ClasspathResource, String> classDirPaths,
		ReferenceGraph<ArchiveNode, String> rootEntryGraph,
		ReferenceGraph<ArchiveNode, String> directoryEntryGraph
	) {
		Map<ClassNode, byte[]> classBytesByClassNode = GlobalUtils.newHashMap(this.allClassNodes.length);
		if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
			List<KeyValuePair<ClassNode, byte[]>> classBytesPairs = Arrays.stream(this.allClassNodes).map(classNode -> new KeyValuePair<ClassNode, byte[]>(classNode, null)).collect(Collectors.toList());
			Vector<Exception> generationExceptions = new Vector<>();
			classBytesByClassNode = classBytesPairs.parallelStream().map(classBytesPair -> {
				try {
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
					classBytesPair.getKey().write(dataOutputStream, this.typeResolver, this.workspace, this.scriptContext);
					dataOutputStream.flush();
					classBytesPair.setValue(byteArrayOutputStream.toByteArray());
					dataOutputStream.close();
				} catch (IOException ioException) {
					generationExceptions.add(ioException);
				} catch (ProcessingException processingException) {
					generationExceptions.add(processingException);
				}

				return classBytesPair;
			}).collect(Collectors.toMap(KeyValuePair::getKey, KeyValuePair::getValue));
			if (!generationExceptions.isEmpty()) {
				for (Exception generationException : generationExceptions) {
					if (generationException instanceof MethodAnalyzerException) {
						this.logger
							.fatal(
								"ERROR:",
								"Method '" + ((MethodAnalyzerException)generationException).getMethodName() + "' could not be analyzed (I). Consider excluding it from Flow and Exception Obfuscation."
							);
					} else {
						this.logger.fatal("ERROR:", generationException.getMessage());
					}
				}
			}
		}

		for (int classNodeIndex = 0; classNodeIndex < this.allClassNodes.length; classNodeIndex++) {
			ClassNode classNode = this.allClassNodes[classNodeIndex];
			String binaryName = GlobalUtils.internalToBinaryName(GlobalUtils.getMappedOrDefault(classNode.getInternalName(), this.fullRemappingMap));
			if (!classNode.isGeneratedClass()) {
				Enumeration<ClasspathResource> classSources = classNode.getClassSources();

				while (classSources.hasMoreElements()) {
					ClasspathResource classSource = classSources.nextElement();
					ZipFile inputZipFile = null;
					ZipEntry inputEntry = null;
					ZipOutputStream outputZipStream = null;
					File outputFile = null;
					boolean isDeflated = false;
					if (!classSource.isZipEntry()) {
						if (classSource.isFileSystemResource() && this.isOutputToArchive) {
							outputZipStream = this.mainOutputZipStream;
							isDeflated = this.compressionFlag == 1;
						}
					} else {
						ArchiveNode archiveNode = classSource.getArchiveNode();
						inputZipFile = this.inputArchiveFileCache.get(archiveNode);
						inputEntry = inputZipFile.getEntry(classSource.getResourceName());
						outputZipStream = this.outputStreamMap.get(archiveNode);
						isDeflated = this.compressionFlag == 1 || this.compressionFlag == 3 && inputEntry.getMethod() == 8;
						outputFile = this.outputFileHandleMap.get(archiveNode);
					}

					boolean fixedTotallyUnchanged = this.fixedClassManager != null && this.fixedClassManager.isInFixedClasses(classNode) && Options.ZKM_FIXED_TOTALLY_UNCHANGED;
					if (fixedTotallyUnchanged && !this.fixedClassManager.isChangedClass(classNode)) {
						if (this.scriptContext.isVerbose()) {
							this.scriptContext.getWriter().println("\tClass '" + binaryName + "' saved unchanged because 'ZKM_FIXED_TOTALLY_UNCHANGED' set.");
						}

						if (!classSource.isFileSystemResource()) {
							if (outputZipStream == null) {
								this.copyUnchangedClassFile(classNode, classSource.getResourceName());
							} else if (!this.isEntryAlreadyWritten(outputZipStream, inputEntry.getName())) {
								this.copyUnchangedClassEntry(inputZipFile, inputEntry, outputZipStream, isDeflated);
								writtenEntryGraph.addReference(outputZipStream, inputEntry.getName());
								if (classNode.hasInnerClasses()) {
									Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

									while (innerClassIterator.hasNext()) {
										ClasspathResource unchangedInnerClassSource = innerClassIterator.next().getPrimaryClassSource();
										ZipEntry unchangedInnerClassEntry = inputZipFile.getEntry(unchangedInnerClassSource.getResourceName());
										this.copyUnchangedClassEntry(inputZipFile, unchangedInnerClassEntry, outputZipStream, isDeflated);
										writtenEntryGraph.addReference(outputZipStream, unchangedInnerClassEntry.getName());
									}
								}
							}
						} else if (this.isOutputToArchive) {
							String unchangedClassEntryPath = this.getClassEntryPath(classNode);
							if (!this.isEntryAlreadyWritten(outputZipStream, unchangedClassEntryPath)) {
								this.copyClassFileToArchive(classSource.getResourceName(), unchangedClassEntryPath, outputZipStream, isDeflated);
							}
						} else {
							this.copyUnchangedClassFile(classNode, classSource.getResourceName());
						}
					} else {
						if (fixedTotallyUnchanged) {
							String changeReason = this.fixedClassManager.getChangeReason(classNode);
							this.scriptContext
								.logError("'Fixed' class '" + binaryName + "' NOT saved unchanged in spite of 'ZKM_FIXED_TOTALLY_UNCHANGED' being set because : '" + changeReason + "'.");
						}

						byte[] classBytes = classBytesByClassNode.get(classNode);
						if (!classSource.isFileSystemResource()) {
							if (outputZipStream != null) {
								String resourceName = classSource.getResourceName();
								String classSourceEntryPath = classEntryPaths.get(classSource);
								if (!this.isEntryAlreadyWritten(outputZipStream, classSourceEntryPath)) {
									this.saveClassToArchive(classNode, classBytes, classSourceEntryPath, outputZipStream, outputFile, inputZipFile.getName(), isDeflated);
									writtenEntryGraph.addReference(outputZipStream, resourceName);
									if (classNode.hasInnerClasses()) {
										for (AbstractClassNode innerClassNode : classNode.getInnerClasses()) {
											ClasspathResource innerClassSource = innerClassNode.getPrimaryClassSource();
											String innerClassEntryPath = classEntryPaths.get(innerClassSource);
											this.saveClassToArchive((ClassNode)innerClassNode, classBytesByClassNode.get(innerClassNode), innerClassEntryPath, outputZipStream, outputFile, inputZipFile.getName(), isDeflated);
											writtenEntryGraph.addReference(outputZipStream, innerClassEntryPath);
										}
									}
								}
							} else {
								this.saveClassToDirectory(this.allClassNodes[classNodeIndex], classBytes);
							}
						} else if (this.isOutputToArchive) {
							String classEntryPath = this.getClassEntryPath(this.allClassNodes[classNodeIndex]);
							if (!this.isEntryAlreadyWritten(outputZipStream, classEntryPath)) {
								this.saveClassToArchive(this.allClassNodes[classNodeIndex], classBytes, classEntryPath, outputZipStream, this.outputTarget, this.outputTarget.getAbsolutePath(), isDeflated);
							}
						} else {
							this.saveClassToDirectory(this.allClassNodes[classNodeIndex], classBytes);
						}
					}
				}
			}
		}

		this.saveGeneratedClasses(generatedClassGraph, classDirPaths, rootEntryGraph, directoryEntryGraph);
	}
}

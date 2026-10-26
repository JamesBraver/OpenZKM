package com.zelix.workspace.resources;

import com.zelix.exceptions.ZKMException;
import com.zelix.framework.manifest.ManifestFile;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.ZipFileWrapper;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.resources.xml.AbstractFrameworkXmlHandler;
import com.zelix.workspace.resources.xml.XmlHandler;
import com.zelix.workspace.resources.xml.XmlParser;
import com.zelix.workspace.resources.xml.XmlParserRunner;
import com.zelix.workspace.writer.JadFileProcessor;
import com.zelix.workspace.writer.ServiceProviderFileProcessor;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceFileAnalyzer {
	public Map<ArchiveNode, ZipFile> zipFilesByArchive = GlobalUtils.newHashMap();
	public ArchiveNode[] archives;
	private Set<ArchiveNode> warnedArchives;
	private final ResourceIndex fileAdaptor;
	private final AbstractWorkspace workspace;
	private final ClassProvider classProvider;
	public AbstractLogger logger;

	public ResourceFileAnalyzer(
		ArchiveNode[] archives,
		Set<ArchiveNode> warnedArchives,
		DualKeyMap<ArchiveNode, String, ClasspathResource> nonClassResources,
		DualKeyMap<ArchiveNode, String, ClasspathResource> declaredFieldResources,
		DualKeyMap<ArchiveNode, String, ClasspathResource> deletedResources,
		Enumeration<JadFileProcessor> jadFiles,
		Enumeration<ClasspathResource> xmlResources,
		ResourceIndex fileAdaptor,
		Enumeration<ClasspathResource> yamlResources,
		Enumeration<ClasspathResource> propertyResources,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		AbstractLogger logger,
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> methodReferencesByResource,
		Map<MethodNode, String> protectedStaticMethods
	) {
		this.archives = archives;
		this.warnedArchives = warnedArchives;
		this.fileAdaptor = fileAdaptor;
		this.workspace = workspace;
		this.classProvider = classProvider;
		this.logger = logger;

		try {
			this.openArchives();
			this.analyzeJadFiles(jadFiles, classReferences);
			this.analyzeXmlResources(xmlResources, classReferences, fieldReferences, methodReferences, methodReferencesByResource);
			this.analyzeYamlResources(yamlResources, classReferences);
			this.analyzePropertyResources(propertyResources, classReferences);
			this.analyzeArchives(classReferences, fieldReferences, methodReferences, methodReferencesByResource, protectedStaticMethods, nonClassResources, declaredFieldResources, deletedResources);
		} finally {
			this.closeArchives();
		}
	}

	private void analyzeArchives(
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> methodReferencesByResource,
		Map<MethodNode, String> protectedStaticMethods,
		DualKeyMap<ArchiveNode, String, ClasspathResource> nonClassResources,
		DualKeyMap<ArchiveNode, String, ClasspathResource> declaredFieldResources,
		DualKeyMap<ArchiveNode, String, ClasspathResource> deletedResources
	) {
		for (ArchiveNode archiveNode : this.zipFilesByArchive.keySet()) {
			String relativePath = archiveNode.getRelativePath();
			ZipFile zipFile = this.zipFilesByArchive.get(archiveNode);
			Map<String, ClasspathResource> nonClassResourceGroup = null;
			if (nonClassResources != null) {
				nonClassResourceGroup = nonClassResources.getGroup(archiveNode);
			}

			Map<String, ClasspathResource> declaredFieldResourceGroup = null;
			if (declaredFieldResources != null) {
				declaredFieldResourceGroup = declaredFieldResources.getGroup(archiveNode);
			}

			Map<String, ClasspathResource> deletedResourceGroup = null;
			if (deletedResources != null) {
				deletedResourceGroup = deletedResources.getGroup(archiveNode);
			}

			Enumeration<? extends ZipEntry> entryEnumeration = zipFile.entries();

			while (entryEnumeration.hasMoreElements()) {
				ZipEntry zipEntry = entryEnumeration.nextElement();
				String entryName = zipEntry.getName();
				if ((deletedResourceGroup == null || !deletedResourceGroup.containsKey(entryName))
					&& (this.warnedArchives == null || !this.warnedArchives.contains(archiveNode))
					&& (nonClassResourceGroup == null || !nonClassResourceGroup.containsKey(entryName))
					&& (declaredFieldResourceGroup == null || !declaredFieldResourceGroup.containsKey(entryName))
					&& !entryName.endsWith(".class")
					&& !this.isNestedArchive(zipFile, zipEntry, this.logger)) {
					String upperEntryName = entryName.toUpperCase();
					if (!zipEntry.isDirectory() && upperEntryName.equals("META-INF/MANIFEST.MF")) {
						ManifestFile manifestFile = archiveNode.getManifest();
						if (manifestFile != null) {
							manifestFile.collectAttributeReferences(relativePath, classReferences, methodReferences, protectedStaticMethods);
						}
					} else if (!zipEntry.isDirectory() && upperEntryName.startsWith("META-INF/services/".toUpperCase()) && entryName.length() > ServiceProviderFileProcessor.SERVICE_PREFIX_LENGTH) {
						try {
							ServiceProviderFileProcessor serviceProviderProcessor = new ServiceProviderFileProcessor(zipFile, zipEntry);
							serviceProviderProcessor.collectServiceClasses(relativePath, classReferences);
						} catch (IOException serviceProviderException) {
							this.logger.error("FILE ERROR:", "Error reading or writing service provider in '" + relativePath + "' : " + serviceProviderException);
						}
					} else if (!zipEntry.isDirectory() && FileUtils.isXmlFile(entryName)) {
						try {
							AtomicReference<String> encoding = new AtomicReference<>();
							AtomicInt bomBytesToSkip = new AtomicInt(0);
							AtomicInt bomLength = new AtomicInt(0);
							AtomicInt byteOrder = new AtomicInt(Integer.MAX_VALUE);
							String xmlContent = XmlParser.readZipEntry(zipFile, zipEntry, encoding, bomBytesToSkip, bomLength, byteOrder);
							XmlHandler xmlHandler = AbstractFrameworkXmlHandler.createHandler(FileUtils.getResolvedZipEntryPath(zipFile, zipEntry), xmlContent, this.fileAdaptor, this.workspace, this.classProvider, this.logger);
							new XmlParserRunner(zipFile, zipEntry, bomBytesToSkip, bomLength, byteOrder, encoding.get(), this.workspace, xmlHandler, classReferences, fieldReferences, methodReferences, methodReferencesByResource);
						} catch (ZKMException invalidXmlException) {
							this.logger.error("FILE ERROR:", "Invalid XML file '" + entryName + "' in '" + relativePath + "' : " + invalidXmlException.getMessage());
						} catch (IOException xmlIOException) {
							this.logger.error("FILE ERROR:", "Error reading or writing '" + entryName + "' in '" + relativePath + "' : " + xmlIOException);
						}
					} else if (!zipEntry.isDirectory() && FileUtils.isYamlFile(entryName)) {
						String yamlEntryPath = FileUtils.getZipEntryPath(zipFile, zipEntry);

						try {
							BufferedReader yamlReader = FileUtils.createBufferedReader(zipFile.getInputStream(zipEntry));
							new YAMLFileProcessor(yamlReader, yamlEntryPath, classReferences);
						} catch (IOException yamlIOException) {
							this.logger.error("FILE ERROR:", "Unexpected error analyzing YAML file '" + yamlEntryPath + "' (A) : " + yamlIOException.getMessage());
						}
					} else if (!zipEntry.isDirectory() && FileUtils.isPropertiesFile(entryName)) {
						String propertiesEntryPath = FileUtils.getZipEntryPath(zipFile, zipEntry);

						try {
							BufferedReader propertiesReader = FileUtils.createBufferedReader(zipFile.getInputStream(zipEntry));
							new PropertyFileParser(propertiesReader, propertiesEntryPath, classReferences);
						} catch (IOException propertiesIOException) {
							this.logger.error("FILE ERROR:", "Unexpected error analyzing PROPERTIES file '" + propertiesEntryPath + "' (A) : " + propertiesIOException.getMessage());
						}
					} else if (!zipEntry.isDirectory()) {
					}
				}
			}
		}
	}

	public void openArchives() {
		for (int i = 0; i < this.archives.length; i++) {
			String archivePath = this.archives[i].getFilePath();

			try {
				ZipFileWrapper zipFileWrapper = new ZipFileWrapper(archivePath);
				this.zipFilesByArchive.put(this.archives[i], zipFileWrapper);
			} catch (IOException ioException) {
				this.logger.warn("FILE ERROR (3)", "Couldn't open '" + archivePath + "'. : " + ioException.getMessage());
			}
		}
	}

	private boolean isNestedArchive(ZipFile zipFile, ZipEntry zipEntry, AbstractLogger logger) {
		try {
			return FileUtils.isNestedArchive(zipFile, zipEntry);
		} catch (IOException ioException) {
			logger.error("FILE ERROR:", "File read error : " + ioException.getMessage());
			return false;
		}
	}

	private void analyzeXmlResources(
		Enumeration<ClasspathResource> xmlResources,
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> methodReferencesByResource
	) {
		if (xmlResources != null) {
			while (xmlResources.hasMoreElements()) {
				ClasspathResource xmlResource = xmlResources.nextElement();
				String resourcePath = xmlResource.getFullPath();

				try {
					AtomicReference<String> encoding = new AtomicReference<>();
					AtomicInt bomBytesToSkip = new AtomicInt(0);
					AtomicInt bomLength = new AtomicInt(0);
					AtomicInt byteOrder = new AtomicInt(Integer.MAX_VALUE);
					String xmlContent = XmlParser.readFile(xmlResource.getAsFile(), encoding, bomBytesToSkip, bomLength, byteOrder);
					XmlHandler xmlHandler = AbstractFrameworkXmlHandler.createHandler(resourcePath, xmlContent, this.fileAdaptor, this.workspace, this.classProvider, this.logger);
					new XmlParserRunner(xmlResource.getAsFile(), bomBytesToSkip, bomLength, byteOrder, encoding.get(), this.workspace, xmlHandler, classReferences, fieldReferences, methodReferences, methodReferencesByResource);
				} catch (ZKMException invalidXmlException) {
					this.logger.error("FILE ERROR:", "Invalid XML file '" + resourcePath + "' (A) : " + invalidXmlException.getMessage());
				} catch (IOException xmlIOException) {
					this.logger.error("FILE ERROR:", "Error reading or writing XML file '" + resourcePath + "' (A) : " + xmlIOException.getMessage());
				} catch (Exception unexpectedException) {
					this.logger.error("FILE ERROR:", "Unexpected error analyzing XML file '" + resourcePath + "' (A) : " + unexpectedException.getMessage());
				}
			}
		}
	}

	private void closeArchives() {
		for (ZipFile zipFile : this.zipFilesByArchive.values()) {
			String zipFileName = zipFile.getName();

			try {
				zipFile.close();
			} catch (IOException ioException) {
				this.logger.warn("FILE ERROR (4)", "FILE ERROR (4) '" + zipFileName + "' : " + ioException.getMessage());
			}
		}
	}

	private void analyzeYamlResources(Enumeration<ClasspathResource> yamlResources, Map<ClassNode, String> classReferences) {
		if (yamlResources != null) {
			while (yamlResources.hasMoreElements()) {
				ClasspathResource yamlResource = yamlResources.nextElement();
				String resourcePath = yamlResource.getFullPath();

				try {
					BufferedReader yamlReader = FileUtils.createBufferedReader(new FileInputStream(yamlResource.getAsFile()));
					new YAMLFileProcessor(yamlReader, resourcePath, classReferences);
				} catch (IOException ioException) {
					this.logger.error("FILE ERROR:", "Unexpected error analyzing YAML file '" + resourcePath + "' (B) : " + ioException.getMessage());
				}
			}
		}
	}

	private void analyzeJadFiles(Enumeration<JadFileProcessor> jadFiles, Map<ClassNode, String> classReferences) {
		if (jadFiles != null) {
			while (jadFiles.hasMoreElements()) {
				JadFileProcessor jadFileProcessor = jadFiles.nextElement();
				jadFileProcessor.collectClassReferences(jadFileProcessor.getFullPath(), classReferences);
			}
		}
	}

	private void analyzePropertyResources(Enumeration<ClasspathResource> propertyResources, Map<ClassNode, String> classReferences) {
		if (propertyResources != null) {
			while (propertyResources.hasMoreElements()) {
				ClasspathResource propertyResource = propertyResources.nextElement();
				String resourcePath = propertyResource.getFullPath();

				try {
					BufferedReader propertiesReader = FileUtils.createBufferedReader(new FileInputStream(propertyResource.getAsFile()));
					new PropertyFileParser(propertiesReader, resourcePath, classReferences);
				} catch (IOException ioException) {
					this.logger.error("FILE ERROR:", "Unexpected error analyzing PROPERTIES file '" + resourcePath + "' (C) : " + ioException.getMessage());
				}
			}
		}
	}
}

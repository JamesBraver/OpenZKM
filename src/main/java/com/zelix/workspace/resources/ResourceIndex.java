package com.zelix.workspace.resources;

import com.zelix.enums.PlatformType;
import com.zelix.framework.reference.MultiLevelReferenceMap;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.ZipFileWrapper;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.resources.xml.AbstractFrameworkXmlHandler;
import com.zelix.workspace.resources.xml.XmlParser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.zip.ZipEntry;

public class ResourceIndex {
	private MultiMap<String, ArchiveNode> archivesByResourcePath = new MultiMap<>();
	private Map<String, File> filesByResourcePath = GlobalUtils.newHashMap();
	private final MultiLevelReferenceMap<String, String, String, ClassNode> defaultMethodClasses = new MultiLevelReferenceMap<>();
	private DualKeyMap<String, String, ClassNode> beanClassesByResource = new DualKeyMap<>();
	private DualKeyMap<String, String, String> beanParentsByResource = new DualKeyMap<>();
	private MultiMap<String, String> importersByResource = new MultiMap<>();
	private MultiMap<String, String> importsByResource = new MultiMap<>();

	public void addBeanClasses(String resourcePath, Map<String, ClassNode> beanClasses) {
		for (Entry<String, ClassNode> beanClassEntry : beanClasses.entrySet()) {
			this.beanClassesByResource.put(resourcePath, beanClassEntry.getKey(), beanClassEntry.getValue());
		}
	}

	public List<ResourceIndex.XmlResourceInfo> collectPlatformResources(String resourcePattern, String basePath, PlatformType platformType, AbstractLogger logger) {
		List<String> resourcePaths = this.findResourcePaths(resourcePattern, basePath);
		if (resourcePaths.size() == 0 && basePath != null) {
			resourcePaths = this.findResourcePaths(resourcePattern, null);
		}

		ArrayList<ResourceIndex.XmlResourceInfo> resourceInfos = new ArrayList<>();

		for (String resourcePath : resourcePaths) {
			if (this.archivesByResourcePath.containsKey(resourcePath)) {
				for (ArchiveNode archiveNode : this.archivesByResourcePath.get(resourcePath)) {
					String archiveFilePath = archiveNode.getFilePath();

					ZipFileWrapper zipFile;
					try {
						zipFile = new ZipFileWrapper(archiveFilePath);
					} catch (IOException archiveOpenException) {
						logger.error("ERROR:", "Could not open '" + archiveFilePath + "' as an archive file : " + archiveNode.getDisplayPath() + " : " + archiveOpenException.getMessage());
						continue;
					}

					ZipEntry zipEntry = zipFile.getEntry(resourcePath);
					AtomicReference<String> entryEncoding = new AtomicReference<>();
					AtomicInt entrySkipBytes = new AtomicInt(0);
					AtomicInt entryBomLength = new AtomicInt(0);
					AtomicInt entryByteOrder = new AtomicInt(Integer.MAX_VALUE);

					String entryContent;
					try {
						entryContent = XmlParser.readZipEntry(zipFile, zipEntry, entryEncoding, entrySkipBytes, entryBomLength, entryByteOrder);
					} catch (IOException entryReadException) {
						logger.error("ERROR:", "Could not get contents of  '" + zipEntry.getName() + "' in '" + archiveFilePath + "' : " + archiveNode.getDisplayPath() + " : " + entryReadException.getMessage());
						continue;
					}

					if (AbstractFrameworkXmlHandler.detectPlatformType(resourcePath, entryContent) == platformType) {
						resourceInfos.add(new ResourceIndex.XmlResourceInfo(entryEncoding, entrySkipBytes, entryBomLength, entryByteOrder, FileUtils.getResolvedZipEntryPath(zipFile, zipEntry), entryContent));
					}
				}
			}

			if (this.filesByResourcePath.containsKey(resourcePath)) {
				AtomicReference<String> fileEncoding = new AtomicReference<>();
				AtomicInt fileSkipBytes = new AtomicInt(0);
				AtomicInt fileBomLength = new AtomicInt(0);
				AtomicInt fileByteOrder = new AtomicInt(Integer.MAX_VALUE);

				try {
					String fileContent = XmlParser.readFile(this.filesByResourcePath.get(resourcePath), fileEncoding, fileSkipBytes, fileBomLength, fileByteOrder);
					if (AbstractFrameworkXmlHandler.detectPlatformType(resourcePath, fileContent) == platformType) {
						resourceInfos.add(new ResourceIndex.XmlResourceInfo(fileEncoding, fileSkipBytes, fileBomLength, fileByteOrder, resourcePath, fileContent));
					}
				} catch (IOException fileReadException) {
					logger.error("ERROR:", "Could not get read contents of  '" + resourcePath + "' : " + fileReadException.getMessage());
				}
			}
		}

		return resourceInfos;
	}

	public void clear() {
		this.archivesByResourcePath.clear();
		this.filesByResourcePath.clear();
		this.defaultMethodClasses.clear();
		this.beanClassesByResource.clearAll();
		this.beanParentsByResource.clearAll();
		this.importersByResource.clear();
		this.importsByResource.clear();
	}

	public void addClasspathFile(File file) {
		this.filesByResourcePath.put(file.getAbsolutePath(), file);
	}

	public List<String> getImporters(String resourcePath) {
		return this.importersByResource.get(resourcePath);
	}

	public List<String> getImports(String resourcePath) {
		return this.importsByResource.get(resourcePath);
	}

	private boolean matchesPath(String basePath, String resourcePath, String wildcard) {
		String normalizedResourcePath = resourcePath.replace('/', FileUtils.FILE_SEPARATOR_CHAR);
		StringBuilder fullPathBuilder = new StringBuilder();
		if (basePath != null && basePath.length() != 0) {
			fullPathBuilder.append(basePath.replace('/', FileUtils.FILE_SEPARATOR_CHAR));
			if (fullPathBuilder.charAt(fullPathBuilder.length() - 1) != FileUtils.FILE_SEPARATOR_CHAR && normalizedResourcePath.charAt(normalizedResourcePath.length() - 1) != FileUtils.FILE_SEPARATOR_CHAR) {
				fullPathBuilder.append(FileUtils.FILE_SEPARATOR_CHAR);
			}

			fullPathBuilder.append(normalizedResourcePath);
		} else {
			fullPathBuilder.append(normalizedResourcePath);
		}

		String wildcardPattern = "*" + wildcard.replace('/', FileUtils.FILE_SEPARATOR_CHAR);
		return StringUtils.matchesWildcard(fullPathBuilder.toString(), wildcardPattern);
	}

	private List<String> findResourcePaths(String resourcePattern, String basePath) {
		ArrayList<String> resourcePaths = new ArrayList<>();
		Enumeration<String> archivePathEnumeration = this.archivesByResourcePath.keys();

		while (archivePathEnumeration.hasMoreElements()) {
			String archiveResourcePath = archivePathEnumeration.nextElement();
			if (this.matchesPath(basePath, archiveResourcePath, resourcePattern)) {
				resourcePaths.add(archiveResourcePath);
			}
		}

		Iterator<Entry<String, File>> fileEntryIterator = this.filesByResourcePath.entrySet().iterator();

		while (fileEntryIterator.hasNext()) {
			String filePath = fileEntryIterator.next().getKey();
			if (this.matchesPath(basePath, filePath, resourcePattern)) {
				resourcePaths.add(filePath);
			}
		}

		return resourcePaths;
	}

	public ReferenceGraph<String, ClassNode> getDefaultMethodClasses(String resourcePath, String elementName) {
		return this.defaultMethodClasses.getGraph(resourcePath, elementName);
	}

	public void addBeanParents(String resourcePath, Map<String, String> beanParents) {
		for (Entry<String, String> beanParentEntry : beanParents.entrySet()) {
			String discardedPreviousBeanParent = this.beanParentsByResource.put(resourcePath, beanParentEntry.getKey(), beanParentEntry.getValue());
		}
	}

	public Map<String, ClassNode> getBeanClasses(String resourcePath) {
		return this.beanClassesByResource.getGroup(resourcePath);
	}

	public void addResourceArchive(String resourcePath, ArchiveNode archiveNode) {
		this.archivesByResourcePath.put(resourcePath, archiveNode);
	}

	public void addDefaultMethodClass(String resourcePath, String elementName, String attributeName, ClassNode classNode) {
		this.defaultMethodClasses.addReference(resourcePath, elementName, attributeName, classNode);
	}

	public Map<String, String> getBeanParents(String resourcePath) {
		return this.beanParentsByResource.getGroup(resourcePath);
	}

	public void addImport(String importedResourcePath, String importingResourcePath) {
		this.importersByResource.put(importedResourcePath, importingResourcePath);
		this.importsByResource.put(importingResourcePath, importedResourcePath);
	}

	public static class XmlResourceInfo {
		public final AtomicReference<String> encoding;
		public final AtomicInt skipBytes;
		public final AtomicInt bomLength;
		public final AtomicInt byteOrder;
		public final String path;
		public final String content;

		public XmlResourceInfo(AtomicReference<String> encoding, AtomicInt skipBytes, AtomicInt bomLength, AtomicInt byteOrder, String path, String content) {
			this.encoding = encoding;
			this.skipBytes = skipBytes;
			this.bomLength = bomLength;
			this.byteOrder = byteOrder;
			this.path = path;
			this.content = content;
		}
	}
}

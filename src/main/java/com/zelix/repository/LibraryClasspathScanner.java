package com.zelix.repository;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.LibraryMethodNode;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.ZipFileWrapper;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.io.ClassFilenameFilter;
import com.zelix.workspace.PackageManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

public class LibraryClasspathScanner {
	private LibraryClassNode[] classNodes;
	private Map<String, LibraryClassNode> classNodesByInternalName;
	private MultiMap<String, LibraryClassNode> classNodesByMethodDisplayName;

	public LibraryClasspathScanner(File[] rawClasspath) throws IOException {
		ArrayList<ClasspathResource> resources = new ArrayList<>(Math.max(rawClasspath.length * 5, 10));

		for (int classpathIndex = 0; classpathIndex < rawClasspath.length; classpathIndex++) {
			if (rawClasspath[classpathIndex].exists() && rawClasspath[classpathIndex].isDirectory()) {
				String directoryPath = rawClasspath[classpathIndex].getAbsolutePath();
				String[] classFileNames = rawClasspath[classpathIndex].list(new ClassFilenameFilter());
				if (classFileNames != null) {
					for (int fileNameIndex = 0; fileNameIndex < classFileNames.length; fileNameIndex++) {
						String classFilePath = directoryPath + (directoryPath.endsWith(SystemConstants.fileSeparator) ? "" : SystemConstants.fileSeparator) + classFileNames[fileNameIndex];
						resources.add(new ClasspathResource(classFilePath));
					}
				}
			} else {
				try {
					ZipFileWrapper zipFile = new ZipFileWrapper(rawClasspath[classpathIndex]);
					Enumeration<? extends ZipEntry> zipEntryEnumeration = zipFile.entries();

					while (zipEntryEnumeration.hasMoreElements()) {
						ZipEntry zipEntry = zipEntryEnumeration.nextElement();
						if (!zipEntry.isDirectory() && zipEntry.getName().endsWith(".class")) {
							resources.add(new ClasspathResource(zipFile, zipEntry));
						}
					}
				} catch (IOException ioException) {
					throw new ProcessingException(ioException.toString() + " while reading " + rawClasspath[classpathIndex]);
				}
			}
		}

		this.classNodes = new LibraryClassNode[resources.size()];
		this.classNodesByInternalName = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(resources.size()));
		this.classNodesByMethodDisplayName = new MultiMap<>();

		for (int classIndex = 0; classIndex < resources.size(); classIndex++) {
			LibraryClassNode classNode = ClassRepository.loadClassFromResource(resources.get(classIndex));
			this.classNodes[classIndex] = classNode;
			this.classNodesByInternalName.put(classNode.getInternalName(), classNode);
			LibraryMethodNode[] methodNodes = classNode.getMethodNodes();

			for (int methodIndex = 0; methodIndex < methodNodes.length; methodIndex++) {
				this.classNodesByMethodDisplayName.put(methodNodes[methodIndex].getDisplayNameWithDescriptor(), classNode);
			}
		}

		for (int resourceIndex = 0; resourceIndex < resources.size(); resourceIndex++) {
			resources.get(resourceIndex).close();
		}
	}

	public List<LibraryClassNode> getClassesWithMethod(String methodDisplayName) {
		List<LibraryClassNode> classNodes = this.classNodesByMethodDisplayName.get(methodDisplayName);
		return classNodes == null ? null : new ArrayList<>(classNodes);
	}

	public synchronized List<LibraryClassNode> getDirectSubclasses(String superclassName) {
		ArrayList<LibraryClassNode> subclasses = null;

		for (int i = 0; i < this.classNodes.length; i++) {
			AbstractClassNode classNode = this.classNodes[i];

			while (true) {
				String directSuperclassName = classNode.getSuperclassName();
				if (directSuperclassName.equals(superclassName)) {
					if (subclasses == null) {
						subclasses = new ArrayList<>();
					}

					subclasses.add(this.classNodes[i]);
					break;
				}

				if (directSuperclassName.equals("java/lang/Object")) {
					break;
				}

				AbstractClassNode superclassNode = this.classNodesByInternalName.get(directSuperclassName);
				if (superclassNode == null) {
					break;
				}

				classNode = superclassNode;
			}
		}

		return subclasses;
	}

	public PackageManager buildPackageManager() {
		PackageManager packageManager = new PackageManager();
		packageManager.rebuildPackageTree(this.classNodes);
		return packageManager;
	}
}

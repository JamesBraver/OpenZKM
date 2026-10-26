package com.zelix.framework.tree;

import com.zelix.repository.ClasspathManager;
import com.zelix.utils.ZipFileWrapper;
import com.zelix.utils.dataset.AtomicInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClasspathResource {
	public ZipFile zipFile;
	public String zipFilePath;
	public ZipEntry zipEntry;
	public Path jrtPath;
	public ClasspathManager classpathManager;
	public File file;
	public int resourceType;
	public String resourceName;
	public String expectedFinalHash;
	public ArchiveNode archiveNode;

	private ClasspathResource(StringBuilder resourceName) {
		this.file = null;
		this.resourceType = 4;
		this.resourceName = resourceName.toString();
	}

	public ClasspathResource(String filePath) {
		this.file = new File(filePath);
		this.resourceType = 1;
		this.resourceName = this.file.getAbsolutePath();
	}

	public ClasspathResource(File file) {
		this(null, file);
	}

	public ClasspathResource(Path jrtPath, ClasspathManager classpathManager) {
		this.jrtPath = jrtPath.toAbsolutePath();
		this.classpathManager = classpathManager;
		this.resourceType = 3;
		this.resourceName = jrtPath.toString();
	}

	public ClasspathResource(ZipFile zipFile, ZipEntry zipEntry, ArchiveNode archiveNode) {
		this.zipFile = zipFile;
		this.zipFilePath = zipFile.getName();
		this.zipEntry = zipEntry;
		this.resourceType = 2;
		this.resourceName = zipEntry.getName();
		this.archiveNode = archiveNode;
	}

	public ClasspathResource(String expectedFinalHash, File file) {
		this.file = file;
		this.resourceType = 1;
		this.resourceName = file.getAbsolutePath();
		if (expectedFinalHash != null) {
			this.expectedFinalHash = expectedFinalHash.toLowerCase();
		}
	}

	public ClasspathResource(ZipFile zipFile, ZipEntry zipEntry) {
		this(zipFile, zipEntry, null);
	}

	public static ClasspathResource createSynthetic(String resourceName) {
		return new ClasspathResource(new StringBuilder(resourceName));
	}

	public String getContainerPath() {
		return this.archiveNode != null ? this.archiveNode.getFullPath() : this.zipFilePath;
	}

	public void close() {
		this.file = null;
		this.zipEntry = null;
		if (this.zipFile != null) {
			try {
				this.zipFile.close();
			} catch (IOException ignored) {
			}

			this.zipFile = null;
		}
	}

	public String getExpectedFinalHash() {
		return this.expectedFinalHash;
	}

	public boolean isZipEntry() {
		return this.resourceType == 2;
	}

	public String getResourceName() {
		return this.resourceName;
	}

	public File getAsFile() {
		switch (this.resourceType) {
			case 1:
				if (this.file == null) {
					this.file = new File(this.resourceName);
				}

				return this.file;
			case 2:
				return null;
			default:
				throw new RuntimeException("Bad type=" + this.resourceType + " in " + this.getClass().getName());
		}
	}

	public String getFullPath() {
		StringBuilder pathBuilder = new StringBuilder();
		String containerPath = this.getContainerPath();
		if (containerPath != null) {
			pathBuilder.append(containerPath);
			pathBuilder.append("!");
		}

		pathBuilder.append(this.getResourceName());
		return pathBuilder.toString();
	}

	private String getUniqueKey() {
		if (this.resourceType == 1) {
			return this.resourceName;
		} else {
			return this.resourceType == 3 ? this.jrtPath.toString() : this.getContainerPath() + '!' + this.resourceName;
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ClasspathResource ? this.getUniqueKey().equals(((ClasspathResource)obj).getUniqueKey()) : false;
	}

	@Override
	public int hashCode() {
		return this.getUniqueKey().hashCode();
	}

	public ArchiveNode getArchiveNode() {
		return this.archiveNode;
	}

	public InputStream getInputStream(AtomicInt resourceSize) throws IOException {
		switch (this.resourceType) {
			case 1:
				FileInputStream fileInputStream = new FileInputStream(this.resourceName);
				if (resourceSize != null) {
					resourceSize.set(fileInputStream.available());
				}

				return fileInputStream;
			case 2:
				if (this.zipFile == null) {
					this.zipFile = new ZipFileWrapper(this.zipFilePath);
				}

				if (this.zipEntry == null) {
					this.zipEntry = this.zipFile.getEntry(this.resourceName);
				}

				InputStream zipInputStream = this.zipFile.getInputStream(this.zipEntry);
				if (resourceSize != null) {
					resourceSize.set((int)this.zipEntry.getSize());
				}

				return zipInputStream;
			default:
				throw new RuntimeException("Bad type=" + this.resourceType + " in " + this.getClass().getName());
		}
	}

	@Override
	public String toString() {
		return this.getResourceName();
	}

	public boolean isFileSystemResource() {
		return this.resourceType == 1;
	}
}

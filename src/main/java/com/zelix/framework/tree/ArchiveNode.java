package com.zelix.framework.tree;

import com.zelix.framework.manifest.ManifestFile;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

public class ArchiveNode implements Comparable<ArchiveNode> {
	private Vector<ArchiveNode> children = new Vector<>();
	private String tempFilePath;
	private String relativePath;
	private ArchiveNode parent;
	private String fullPath;
	private String expectedInitialHash;
	private String expectedFinalHash;
	private long sizeInBytes;
	private String moduleName;
	private ManifestFile manifestFile;

	public ArchiveNode(String tempFilePath, long sizeInBytes, String relativePath, ArchiveNode parent) {
		this.tempFilePath = tempFilePath;
		this.relativePath = relativePath;
		this.parent = parent;
		if (parent != null) {
			this.fullPath = parent.getFullPath() + "!" + relativePath;
			parent.addChild(this);
		} else {
			this.fullPath = relativePath;
		}

		this.sizeInBytes = sizeInBytes;
	}

	public ArchiveNode(String tempFilePath, long sizeInBytes, String relativePath, String expectedInitialHash, String expectedFinalHash) {
		this.tempFilePath = tempFilePath;
		this.relativePath = relativePath;
		this.sizeInBytes = sizeInBytes;
		this.fullPath = relativePath;
		this.expectedInitialHash = expectedInitialHash;
		this.expectedFinalHash = expectedFinalHash;
	}

	public ArchiveNode(String tempFilePath, long sizeInBytes, String relativePath) {
		this(tempFilePath, sizeInBytes, relativePath, null, null);
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getShortPath() {
		int separatorIndex = this.fullPath.indexOf("!");
		return separatorIndex == -1 ? this.getFileName() : new File(this.fullPath.substring(0, separatorIndex)).getName() + this.fullPath.substring(separatorIndex);
	}

	public ArchiveNode getChildByPath(String relativePath) {
		if (this.children != null) {
			for (ArchiveNode child : this.children) {
				if (child.getRelativePath().equals(relativePath)) {
					return child;
				}
			}
		}

		return null;
	}

	public String getDisplayPath() {
		return this.isRoot() ? this.fullPath : this.relativePath;
	}

	public ArchiveNode getRootArchive() {
		if (this.parent != null && this.parent.getParent() != null) {
			return this.parent.getRootArchive();
		} else {
			return this.parent != null ? this.parent : null;
		}
	}

	public boolean isRoot() {
		return this.parent == null;
	}

	public String getFileName() {
		return new File(this.relativePath).getName();
	}

	public int compareToNode(ArchiveNode other) {
		return this.getFullPath().compareToIgnoreCase(other.getFullPath());
	}

	public ArchiveNode getParent() {
		return this.parent;
	}

	public boolean hasModuleName() {
		return this.moduleName != null;
	}

	public String getExpectedFinalHash() {
		return this.expectedFinalHash;
	}

	public boolean isMicroEdition() {
		return this.manifestFile != null ? this.manifestFile.isMicroEdition() : false;
	}

	@Override
	public int compareTo(ArchiveNode o) {
		return this.compareToNode(o);
	}

	public String getModuleName() {
		return this.moduleName;
	}

	public String getFullPath() {
		return this.fullPath;
	}

	private void addChild(ArchiveNode child) {
		this.children.addElement(child);
	}

	public String getRelativePath() {
		return this.relativePath;
	}

	public long getSizeInBytes() {
		return this.sizeInBytes;
	}

	public String getFilePath() {
		return this.tempFilePath == null ? this.relativePath : this.tempFilePath;
	}

	public ManifestFile getManifest() {
		return this.manifestFile;
	}

	public void setManifest(ManifestFile manifestFile) {
		this.manifestFile = manifestFile;
	}

	public boolean isCldc10() {
		return this.manifestFile != null ? this.manifestFile.isCldc10() : false;
	}

	public String getFullPathForChild(String childPath) {
		return this.fullPath + "!" + childPath;
	}

	public Enumeration<ArchiveNode> getChildren() {
		return this.children.elements();
	}
}

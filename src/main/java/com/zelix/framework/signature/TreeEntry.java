package com.zelix.framework.signature;

import com.zelix.utils.treeNode.TreeNavigator;

public class TreeEntry {
	private String prefix;
	private String name;
	private boolean dotSeparated;
	private TreeEntry parent;
	private TreeNavigator.TreeNode<TreeEntry> treeNode;

	public TreeEntry(String prefix, String name) {
		this.prefix = prefix;
		this.name = name;
	}

	public TreeEntry(String prefix, String name, boolean dotSeparated) {
		this.prefix = prefix;
		this.name = name;
		this.dotSeparated = dotSeparated;
	}

	public void setParent(TreeEntry parent) {
		this.parent = parent;
	}

	public String getFullInternalName() {
		return (this.prefix == null ? "" : this.prefix) + (this.name == null ? "" : this.name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public TreeEntry getParent() {
		return this.parent;
	}

	public String getName() {
		return this.name;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setTreeNode(TreeNavigator.TreeNode<TreeEntry> treeNode) {
		this.treeNode = treeNode;
	}

	public boolean isDotSeparated() {
		return this.dotSeparated;
	}

	public void setDotSeparated(boolean dotSeparated) {
		this.dotSeparated = dotSeparated;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public boolean startsNameChain() {
		return this.parent != null && !this.dotSeparated;
	}
}

package com.zelix.utils.treeNode;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class TreeIterator<T> implements Iterator<TreeNavigator.TreeNode<T>> {
	public final TreeNavigator<T> tree;
	private TreeNavigator<T> navigator;
	private TreeNavigator.TreeNode<T> lastNodeReturned;
	private TreeNavigator.TreeNode<T> nextNode;
	private boolean removed;

	public TreeIterator(TreeNavigator<T> tree, TreeNavigator<T> navigator) {
		this.tree = tree;
		this.navigator = navigator;
	}

	@Override
	public boolean hasNext() {
		this.nextNode = this.findNext();
		return this.nextNode != null;
	}

	@Override
	public TreeNavigator.TreeNode<T> next() {
		return this.nextNode();
	}

	public TreeNavigator.TreeNode<T> nextNode() {
		this.lastNodeReturned = this.findNext();
		this.removed = false;
		this.nextNode = null;
		if (this.lastNodeReturned == null) {
			throw new NoSuchElementException();
		} else {
			return this.lastNodeReturned;
		}
	}

	@Override
	public void remove() {
		if (this.lastNodeReturned != null && !this.removed) {
			this.removed = true;
			this.nextNode = null;
			this.nextNode = this.findNextNodeAfterRemoval(this.lastNodeReturned);
			TreeNavigator.removeNode(this.navigator, this.lastNodeReturned);
			this.lastNodeReturned = null;
		} else {
			throw new IllegalStateException();
		}
	}

	private TreeNavigator.TreeNode<T> findNextNodeAfterRemoval(TreeNavigator.TreeNode<T> removedNode) {
		TreeNavigator.TreeNode<T> parentNode = TreeNavigator.TreeNode.getStaticParent$synthetic(removedNode);
		if (parentNode == null) {
			return null;
		} else {
			int removedIndex = TreeNavigator.TreeNode.getChildrenList$synthetic(parentNode).indexOf(removedNode);
			return removedIndex < TreeNavigator.TreeNode.getChildrenList$synthetic(parentNode).size() - 1
				? TreeNavigator.TreeNode.getChildrenList$synthetic(parentNode).get(removedIndex + 1)
				: this.findNextNodeAfterRemoval(parentNode);
		}
	}

	private TreeNavigator.TreeNode<T> findNext() {
		if (this.removed || this.nextNode != null) {
			return this.nextNode;
		} else if (this.lastNodeReturned == null) {
			return TreeNavigator.getRootNode(this.navigator) != null ? TreeNavigator.getRootNode(this.navigator) : null;
		} else {
			return TreeNavigator.TreeNode.getChildrenList$synthetic(this.lastNodeReturned).size() > 0
				? TreeNavigator.TreeNode.getChildrenList$synthetic(this.lastNodeReturned).get(0)
				: this.findNextNodeAfterRemoval(this.lastNodeReturned);
		}
	}
}

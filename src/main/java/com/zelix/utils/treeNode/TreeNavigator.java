package com.zelix.utils.treeNode;

import com.zelix.exceptions.LookupException;
import com.zelix.utils.GlobalUtils;

import java.util.*;

public class TreeNavigator<T> {
	private Map<T, TreeNode<T>> nodeLookupMap = GlobalUtils.newHashMap();
	private TreeNode<T> rootNode;
	private TreeNode<T> currentNode;

	public TreeNavigator(T rootData) {
		this.rootNode = new TreeNode<>(this, rootData, null);
		this.currentNode = this.rootNode;
		this.nodeLookupMap.put(rootData, this.currentNode);
	}

	public Iterator<TreeNode<T>> iterator() {
		return new TreeIterator<>(this, this);
	}

	public void addChildToCurrent(T data) {
		this.currentNode = TreeNode.addChild(this.currentNode, data);
		this.nodeLookupMap.put(data, this.currentNode);
	}

	public boolean navigateTo(T data) {
		TreeNode<T> lookupNode = this.nodeLookupMap.get(data);
		if (lookupNode != null) {
			this.currentNode = lookupNode;
			return true;
		} else {
			return false;
		}
	}

	public void navigateToRoot() {
		this.currentNode = this.rootNode;
	}

	public void navigateToParent() {
		TreeNode<T> parentNode = TreeNode.getParent$synthetic(this.currentNode);
		if (parentNode != null) {
			this.currentNode = parentNode;
		} else {
			throw new LookupException("Root node has no parent");
		}
	}

	private void removeNode(TreeNode<T> node) {
		if (node == null) {
			throw new IllegalArgumentException("Null node");
		} else if (node == this.rootNode) {
			throw new LookupException("Cannot remove root");
		} else {
			TreeNode<T> ancestorNode = node;

			while (ancestorNode != null && ancestorNode != this.rootNode) {
				ancestorNode = TreeNode.getParent$synthetic(ancestorNode);
			}

			if (ancestorNode != this.rootNode) {
				throw new LookupException("Node not a member of Tree");
			} else {
				TreeNode.removeChild(TreeNode.getParent$synthetic(node), node);
			}
		}
	}

	public boolean isCurrentNodeRoot() {
		return this.currentNode == this.rootNode;
	}

	public Enumeration<T> getAllDataAsEnumeration() {
		ArrayList<T> dataList = new ArrayList<>();
		Iterator<TreeNode<T>> nodeIterator = this.iterator();

		while (nodeIterator.hasNext()) {
			dataList.add(nodeIterator.next().getData());
		}

		return Collections.enumeration(dataList);
	}

	public void addSiblingToCurrent(T data) {
		if (this.currentNode != this.rootNode) {
			this.currentNode = TreeNode.addChild(TreeNode.getParent$synthetic(this.currentNode), data);
			this.nodeLookupMap.put(data, this.currentNode);
		} else {
			throw new LookupException("Cannot add sibling to root");
		}
	}

	public static <T> TreeNode<T> getRootNode(TreeNavigator<T> treeNavigator) {
		return treeNavigator.rootNode;
	}

	public static <T> void removeNode(TreeNavigator<T> treeNavigator, TreeNode<T> node) {
		treeNavigator.removeNode(node);
	}

	public static class TreeNode<T> {
		public final TreeNavigator<T> ownerTree;
		private ArrayList<TreeNode<T>> children;
		private T data;
		private TreeNode<T> parent;

		private TreeNode(TreeNavigator<T> ownerTree, T data, TreeNode<T> parent) {
			this.ownerTree = ownerTree;
			this.children = new ArrayList<>();
			this.data = data;
			this.parent = parent;
		}

		public static <T> boolean removeChild(TreeNode<T> parentNode, TreeNode<T> child) {
			return parentNode.removeChildNode(child);
		}

		public static <T> TreeNode<T> getStaticParent$synthetic(TreeNode<T> node) {
			return node.parent;
		}

		public static <T> TreeNode<T> getParent$synthetic(TreeNode<T> node) {
			return node.getParentNode();
		}

		public static <T> ArrayList<TreeNode<T>> getChildrenList$synthetic(TreeNode<T> node) {
			return node.children;
		}

		public static <T> TreeNode<T> addChild(TreeNode<T> parentNode, T data) {
			return parentNode.addChildNode(data);
		}

		public T getData() {
			return this.data;
		}

		private TreeNode<T> getParentNode() {
			return this.parent;
		}

		private boolean removeChildNode(TreeNode<T> child) {
			return this.children.remove(child);
		}

		private TreeNode<T> addChildNode(T data) {
			TreeNode<T> newChild = new TreeNode<>(this.ownerTree, data, this);
			this.children.add(newChild);
			return newChild;
		}

		public Enumeration<TreeNode<T>> getChildren() {
			return Collections.enumeration(this.children);
		}
	}
}

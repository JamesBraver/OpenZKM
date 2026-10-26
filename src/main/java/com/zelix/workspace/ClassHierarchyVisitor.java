package com.zelix.workspace;

public abstract class ClassHierarchyVisitor<T> {
	protected abstract void visitNode(ClassHierarchyNode node, int depth, T collectedNodes);
}

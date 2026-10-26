package com.zelix.workspace;

import java.util.List;

public class ClassHierarchyCollector extends ClassHierarchyVisitor<List<ClassHierarchyNode>> {
	protected void visitNode(ClassHierarchyNode node, int depth, List<ClassHierarchyNode> collectedNodes) {
		node.setDepth(depth);
		collectedNodes.add(node);
	}
}

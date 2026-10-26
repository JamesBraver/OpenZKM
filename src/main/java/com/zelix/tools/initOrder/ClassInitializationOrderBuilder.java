package com.zelix.tools.initOrder;

import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.command.ClassInitializationOrderStatement;
import com.zelix.utils.GlobalUtils;
import com.zelix.workspace.ClassHierarchyNode;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

public class ClassInitializationOrderBuilder {
	private final ReferenceGraph<ClassNode, ClassNode> referenceGraph = new ReferenceGraph<>();
	private final Set<ClassNode> initializationOrderClasses = GlobalUtils.newHashSet();

	public ClassInitializationOrderBuilder(ScriptContext context) {
		ReferenceGraph<?, ?> contextReferenceGraph = context.getReferenceGraph();
		if (contextReferenceGraph != null) {
			for (Entry<?, ?> contextAdjacencyEntry : contextReferenceGraph.getAdjacencyEntries()) {
				String memberClassName = (String)contextAdjacencyEntry.getKey();
				ClassNode memberClassNode = ClassHierarchyNode.getWorkspaceClassNode(memberClassName);
				if (memberClassNode != null) {
					for (Object referenceClassNameObj : (Set<?>)contextAdjacencyEntry.getValue()) {
						String referenceClassName = (String)referenceClassNameObj;
						ClassNode referenceClassNode = ClassHierarchyNode.getWorkspaceClassNode(referenceClassName);
						if (referenceClassNode != null) {
							this.referenceGraph.addReference(memberClassNode, referenceClassNode);
							this.initializationOrderClasses.add(referenceClassNode);
							ClassHierarchyNode referenceHierarchyNode = ClassHierarchyNode.getNodeByInternalName(referenceClassName);
							this.initializationOrderClasses.addAll(referenceHierarchyNode.getOuterWorkspaceClasses());
						} else {
							context.logWarning(
								"Class name '"
									+ GlobalUtils.internalToBinaryName(referenceClassName)
									+ "' appears in a '"
									+ ClassInitializationOrderStatement.COMMAND_NAME
									+ "' statement but it has not been opened for obfuscation. It will be ignored."
							);
							context.logError(
								"Class '"
									+ GlobalUtils.internalToBinaryName(referenceClassName)
									+ "' not opened. It was specified as initialized after class '"
									+ memberClassNode.getClassNameBinary()
									+ " in a '"
									+ ClassInitializationOrderStatement.COMMAND_NAME
									+ "' statement. (5)"
							);
						}
					}
				} else {
					context.logError(
						"Class '"
							+ GlobalUtils.internalToBinaryName(memberClassName)
							+ "' not opened. It was specified as initialized before "
							+ (((Set<?>)contextAdjacencyEntry.getValue()).size() == 1 ? "another class" : "other classes")
							+ " in a '"
							+ ClassInitializationOrderStatement.COMMAND_NAME
							+ "' statement. (6)"
					);
				}
			}

			if (!Options.dontInferTransitiveClassInitOrder) {
				boolean referenceGraphChanged;
				do {
					referenceGraphChanged = false;
					ReferenceGraph<ClassNode, ClassNode> referenceGraphSnapshot = this.referenceGraph.deepClone();

					for (Entry<ClassNode, Set<ClassNode>> snapshotAdjacencyEntry : referenceGraphSnapshot.getAdjacencyEntries()) {
						ClassNode snapshotMemberClassNode = snapshotAdjacencyEntry.getKey();

					for (ClassNode snapshotReferencedClassNode : snapshotAdjacencyEntry.getValue()) {
						Set<ClassNode> transitiveReferences = referenceGraphSnapshot.getReferences(snapshotReferencedClassNode);
						if (transitiveReferences != null) {
							for (ClassNode transitiveClassNode : transitiveReferences) {
								if (this.referenceGraph.addReference(snapshotMemberClassNode, transitiveClassNode)) {
									referenceGraphChanged = true;
								}
							}
						}
					}
				}
			} while (referenceGraphChanged);
			}

			for (Entry<ClassNode, Set<ClassNode>> referenceGraphAdjacencyEntry : this.referenceGraph.getAdjacencyEntries()) {
				ClassNode startClassNode = referenceGraphAdjacencyEntry.getKey();
				HashSet<ClassNode> visitedClasses = GlobalUtils.newHashSet();
				if (this.hasReferenceLoop(startClassNode, this.referenceGraph.getReferences(startClassNode), visitedClasses)) {
					context.logSpecialError("Loop inside of '" + ClassInitializationOrderStatement.COMMAND_NAME + "' statement : '" + startClassNode.getSourcePath() + "'");
				}
			}
		}
	}

	public ReferenceGraph<ClassNode, ClassNode> getReferenceGraph() {
		return this.referenceGraph.deepClone();
	}

	public boolean isInInitializationOrder(ClassNode classNode) {
		return this.initializationOrderClasses.contains(classNode);
	}

	private boolean hasReferenceLoop(ClassNode startClass, Set<ClassNode> currentReferences, Set<ClassNode> visited) {
		if (currentReferences != null) {
			if (currentReferences.contains(startClass)) {
				return true;
			}

			for (ClassNode referencedClass : currentReferences) {
				if (visited.add(referencedClass) && this.hasReferenceLoop(startClass, this.referenceGraph.getReferences(referencedClass), visited)) {
					return true;
				}
			}
		}

		return false;
	}
}

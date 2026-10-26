package com.zelix.framework.analyzer;

import com.zelix.framework.code.InsnList;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ControlFlowNode implements Comparable<ControlFlowNode> {
	private List<AnalyzingNode> sourceNodes = new ArrayList<>();
	private Set<AnalyzingNode> reachableNodes = GlobalUtils.newHashSet();
	private AnalyzingNode anchorNode;
	private AnalyzingNode fallThroughNode;
	private AnalyzingNode alternateNode;

	public ControlFlowNode(AnalyzingNode anchorNode, AnalyzingNode sourceNode) {
		this.anchorNode = anchorNode;
		this.sourceNodes.add(sourceNode);
		if (sourceNode.isFirstOfTwoSuccessors(anchorNode)) {
			this.fallThroughNode = sourceNode.getSecondSuccessor();
			if (anchorNode.getLastSuccessor() != this.fallThroughNode) {
				this.alternateNode = anchorNode.getLastSuccessor();
			} else {
				this.alternateNode = anchorNode;
			}
		} else {
			this.fallThroughNode = anchorNode.getSecondSuccessor();
			this.alternateNode = anchorNode.getFirstSuccessor();
		}
	}

	public AnalyzingNode getAnchorNode() {
		return this.anchorNode;
	}

	public AnalyzingNode getAlternateNode() {
		return this.alternateNode;
	}

	public boolean containsReachableNode(AnalyzingNode node) {
		return this.reachableNodes.contains(node);
	}

	public int compareByStartIndex(ControlFlowNode other) {
		return this.anchorNode.getStartIndex() - other.anchorNode.getStartIndex();
	}

	public List<AnalyzingNode> getSourceNodes() {
		return new InsnList<>(this.sourceNodes);
	}

	public AnalyzingNode getFirstSourceNode() {
		return this.sourceNodes.get(0);
	}

	@Override
	public int compareTo(ControlFlowNode other) {
		return this.compareByStartIndex(other);
	}

	public AnalyzingNode getFallThroughNode() {
		return this.fallThroughNode;
	}

	public boolean containsReachableInstruction(int instructionIndex) {
		Iterator<AnalyzingNode> nodeIterator = this.reachableNodes.iterator();

		while (nodeIterator.hasNext()) {
			if (nodeIterator.next().containsInstructionIndex(instructionIndex)) {
				return true;
			}
		}

		return false;
	}

	private void addMarkedNodes(BitSet markedIndexes, List<AnalyzingNode> nodes) {
		int nodeCount = nodes.size();

		for (int i = 0; i < nodeCount; i++) {
			if (markedIndexes.get(i)) {
				this.reachableNodes.add(nodes.get(i));
			}
		}
	}

	public void computeReachableNodeSet(List<AnalyzingNode> nodes, DualKeyMap<AnalyzingNode, AnalyzingNode, AnalyzingNode> protectedNodesByCatchNode) {
		HashSet<AnalyzingNode> visitedNodes = GlobalUtils.newHashSet();
		BitSet markedIndexes = new BitSet(nodes.size());

		for (AnalyzingNode sourceNode : this.sourceNodes) {
			markedIndexes.clear();
			this.markReachableNodes(sourceNode, markedIndexes, visitedNodes, nodes);
		}

		if (protectedNodesByCatchNode != null) {
			for (AnalyzingNode catchNode : protectedNodesByCatchNode.getGroupKeySet()) {
				if (this.reachableNodes.contains(catchNode)) {
					for (AnalyzingNode protectedNode : protectedNodesByCatchNode.getGroup(catchNode).keySet()) {
						for (AnalyzingNode candidateSourceNode : this.sourceNodes) {
							markedIndexes.clear();
							if (protectedNode.canReach(candidateSourceNode, markedIndexes)) {
								this.reachableNodes.add(protectedNode);
							}
						}
					}
				}
			}
		}
	}

	private void markReachableNodes(AnalyzingNode node, BitSet markedIndexes, Set<AnalyzingNode> visitedNodes, List<AnalyzingNode> nodes) {
		if (visitedNodes.add(node)) {
			markedIndexes.set(node.getNodeIndex());
			if (node == this.anchorNode) {
				this.addMarkedNodes(markedIndexes, nodes);
			} else {
				List<AnalyzingNode> predecessors = node.getPredecessors();
				if (predecessors != null) {
					int predecessorCount = predecessors.size();

					for (AnalyzingNode predecessor : predecessors) {
						this.markReachableNodes(predecessor, predecessorCount > 0 ? (BitSet)markedIndexes.clone() : markedIndexes, visitedNodes, nodes);
					}
				}
			}
		} else {
			this.addMarkedNodes(markedIndexes, nodes);
		}
	}

	public void addSourceNode(AnalyzingNode sourceNode) {
		this.sourceNodes.add(sourceNode);
	}
}

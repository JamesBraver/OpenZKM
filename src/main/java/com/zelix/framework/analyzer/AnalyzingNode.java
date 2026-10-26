package com.zelix.framework.analyzer;

import com.zelix.framework.analyzer.dataset.FlagData;
import com.zelix.framework.code.InsnList;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.LicenseCheckConstants3;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.Workspace;

import java.util.*;

public class AnalyzingNode extends LicenseCheckConstants3 implements Comparable<AnalyzingNode> {
	public static int integrityStatus;
	private int minPathInstructionCount = -1;
	private List<AnalyzingNode> successors;
	private String name;
	private List<AnalyzingNode> jsrRetNodes;
	private List<AnalyzingNode> catchNodes;
	private AnalyzingNode jsrCallSiteNode;
	private int startIndex;
	private int endIndex;
	private int nodeIndex;
	private AnalyzingNode jsrContinuationNode;
	private List<AnalyzingNode> predecessors;
	private List<AnalyzingNode> jsrCallSiteNodes;

	private static BitSet propagateFlagData(
		AnalyzingNode fromNode,
		FlagData fromFlagData,
		AnalyzingNode toNode,
		Map<AnalyzingNode, ControlFlowNode> controlFlowNodes,
		Set<AnalyzingNode> visitedNodes,
		Map<AnalyzingNode, FlagData> flagDataByNode,
		InsnList<AbstractInsnNode> insnList
	) {
		if (visitedNodes.contains(toNode)) {
			if (fromFlagData.bitSet.get(toNode.getNodeIndex())) {
				if (!controlFlowNodes.containsKey(toNode)) {
					ControlFlowNode controlFlowNode = new ControlFlowNode(toNode, fromNode);
					controlFlowNodes.put(toNode, controlFlowNode);
				} else {
					controlFlowNodes.get(toNode).addSourceNode(fromNode);
				}
			} else {
				toNode.addPredecessor(fromNode);
				flagDataByNode.get(toNode).bitSet.set(fromNode.getNodeIndex());
			}

			return null;
		} else {
			visitedNodes.add(toNode);
			toNode.addPredecessor(fromNode);
			BitSet flagBitSet = (BitSet)fromFlagData.bitSet.clone();
			flagBitSet.set(fromNode.getNodeIndex());
			flagBitSet.set(toNode.getNodeIndex());
			return flagBitSet;
		}
	}

	public static void collectControlFlowNodes(
		FlagData startFlagData, Map<AnalyzingNode, ControlFlowNode> controlFlowNodes, Set<AnalyzingNode> visitedNodes, Map<AnalyzingNode, FlagData> flagDataByNode, InsnList<AbstractInsnNode> insnList
	) {
		LinkedList<KeyValuePair<AnalyzingNode, AnalyzingNode>> preferredPairs = new LinkedList<>();
		LinkedList<KeyValuePair<AnalyzingNode, AnalyzingNode>> otherPairs = new LinkedList<>();
		AnalyzingNode startNode = startFlagData.node;
		if (startNode.successors != null) {
			int startSuccessorCount = startNode.successors.size();

			for (int startSuccessorIndex = 0; startSuccessorIndex < startSuccessorCount; startSuccessorIndex++) {
				AnalyzingNode startSuccessor = startNode.successors.get(startSuccessorIndex);
				KeyValuePair<AnalyzingNode, AnalyzingNode> startSuccessorPair = new KeyValuePair<>(startNode, startSuccessor);
				if (startSuccessorIndex == startSuccessorCount - 1) {
					preferredPairs.addFirst(startSuccessorPair);
				} else {
					otherPairs.addFirst(startSuccessorPair);
				}
			}
		}

		if (startNode.jsrContinuationNode != null) {
			KeyValuePair<AnalyzingNode, AnalyzingNode> startJsrContinuationPair = new KeyValuePair<>(startNode, startNode.jsrContinuationNode);
			preferredPairs.addFirst(startJsrContinuationPair);
		}

		while (!preferredPairs.isEmpty() || !otherPairs.isEmpty()) {
			KeyValuePair<AnalyzingNode, AnalyzingNode> currentPair;
			if (!preferredPairs.isEmpty()) {
				currentPair = preferredPairs.remove();
			} else {
				currentPair = otherPairs.remove();
			}

			AnalyzingNode fromNode = currentPair.getKey();
			AnalyzingNode toNode = currentPair.getValue();
			FlagData fromFlagData = flagDataByNode.get(fromNode);
			BitSet flagBitSet = propagateFlagData(fromNode, fromFlagData, toNode, controlFlowNodes, visitedNodes, flagDataByNode, insnList);
			if (flagBitSet != null) {
				FlagData toFlagData = new FlagData(toNode, flagBitSet);
				flagDataByNode.put(toNode, toFlagData);
				if (toNode.successors != null) {
					int toSuccessorCount = toNode.successors.size();

					for (int toSuccessorIndex = 0; toSuccessorIndex < toSuccessorCount; toSuccessorIndex++) {
						AnalyzingNode toSuccessor = toNode.successors.get(toSuccessorIndex);
						KeyValuePair<AnalyzingNode, AnalyzingNode> toSuccessorPair = new KeyValuePair<>(toNode, toSuccessor);
						if (toSuccessorIndex == toSuccessorCount - 1) {
							preferredPairs.addFirst(toSuccessorPair);
						} else {
							otherPairs.addFirst(toSuccessorPair);
						}
					}
				}

				if (toNode.jsrContinuationNode != null) {
					KeyValuePair<AnalyzingNode, AnalyzingNode> toJsrContinuationPair = new KeyValuePair<>(toNode, toNode.jsrContinuationNode);
					preferredPairs.addFirst(toJsrContinuationPair);
				}
			}
		}
	}

	public static int getIntegrityStatus() {
		return integrityStatus;
	}

	public void setNodeIndex(int nodeIndex) {
		this.nodeIndex = nodeIndex;
	}

	public boolean hasJsrRetNodes() {
		return this.jsrRetNodes != null && this.jsrRetNodes.size() > 0;
	}

	public AbstractInsnNode getLastInsn(InsnList<AbstractInsnNode> insnList) {
		int insnIndex = this.endIndex;

		AbstractInsnNode insn;
		do {
			insn = insnList.get(insnIndex);
		} while (insn.isLabel() && --insnIndex > this.startIndex);

		return insn;
	}

	public void setJsrCallSiteNode(AnalyzingNode jsrCallSiteNode) {
		this.jsrCallSiteNode = jsrCallSiteNode;
	}

	public int compareByStartIndex(AnalyzingNode otherNode) {
		if (this.startIndex < otherNode.startIndex) {
			return -1;
		} else {
			return this.startIndex == otherNode.startIndex ? 0 : 1;
		}
	}

	public int findCompatibleFrameIndex(
		FrameNode frame,
		Set<StackFrameType> monitorStackTypes,
		FrameNode[] frames,
		Set<AnalyzingNode> visitedNodes,
		AnalyzingNode startNode,
		AnalyzingNode stopNode,
		AnalyzingNode excludedNode,
		InsnList<AbstractInsnNode> insnList,
		TypeResolver typeResolver,
		ScriptContext scriptContext,
		AtomicReference<AnalyzingNode> matchedNodeRef,
		boolean checkMonitorState,
		String contextDescription
	) {
		if (this == excludedNode) {
			return -1;
		} else {
			int scanStartIndex;
			if (this == startNode && insnList.get(this.startIndex).isLabel()) {
				scanStartIndex = this.startIndex + 1;
			} else {
				scanStartIndex = this.startIndex;
			}

			for (int i = this.endIndex - 1; i >= scanStartIndex; i--) {
				if (frames[i].isFullyResolved()) {
					FrameNode candidateFrame = frames[i];
					int compatibilityOverride = (Options.relaxStackFrameCompatibility || Options.stackFrameCompatibilityOverride == null && !Options.requireStrictStackFrameCompatibility || Options.forceRelaxStackFrameCompatibility)
							&& !this.checkIntegrity(((Workspace)typeResolver.getClassHierarchy()).getChangedResourceSet(), scriptContext)
						? -1
						: 1;
					if (frame.isCompatibleWith(typeResolver, candidateFrame, monitorStackTypes, checkMonitorState, contextDescription, compatibilityOverride) && frame.getInitializationLabel() == frames[i].getInitializationLabel()) {
						matchedNodeRef.set(this);
						return i;
					}
				}
			}

			if (this != stopNode) {
				if (this.successors != null) {
					int successorCount = this.successors.size();

					for (int successorIndex = 0; successorIndex < successorCount; successorIndex++) {
						AnalyzingNode successor = this.successors.get(successorIndex);
						if (visitedNodes.add(successor)) {
							int compatibleFrameIndex = successor.findCompatibleFrameIndex(frame, monitorStackTypes, frames, visitedNodes, startNode, stopNode, excludedNode, insnList, typeResolver, scriptContext, matchedNodeRef, checkMonitorState, contextDescription);
							if (compatibleFrameIndex != -1) {
								return compatibleFrameIndex;
							}
						}
					}
				}

				if (this.jsrContinuationNode != null && visitedNodes.add(this.jsrContinuationNode)) {
					int jsrContinuationIndex = this.jsrContinuationNode.findCompatibleFrameIndex(frame, monitorStackTypes, frames, visitedNodes, startNode, stopNode, excludedNode, insnList, typeResolver, scriptContext, matchedNodeRef, checkMonitorState, contextDescription);
					if (jsrContinuationIndex != -1) {
						return jsrContinuationIndex;
					}
				}
			}

			return -1;
		}
	}

	public boolean containsInstructionIndex(int instructionIndex) {
		return instructionIndex >= this.startIndex && instructionIndex <= this.endIndex;
	}

	public List<AnalyzingNode> getJsrRetNodes() {
		return this.jsrRetNodes;
	}

	public AbstractInsnNode findLastInsn(List<AbstractInsnNode> insnList) {
		for (int insnIndex = this.endIndex; insnIndex > this.startIndex; insnIndex--) {
			if (!insnList.get(insnIndex).isLabel()) {
				return insnList.get(insnIndex);
			}
		}

		return null;
	}

	public boolean isCatchNode() {
		return this.name != null && this.name.startsWith("catchNode");
	}

	public void addCatchNode(AnalyzingNode catchNode) {
		if (this.catchNodes == null) {
			this.catchNodes = new ArrayList<>();
		}

		this.catchNodes.add(catchNode);
	}

	private boolean canReachRecursively(AnalyzingNode node, BitSet visitedBitset) {
		if (visitedBitset.get(node.getNodeIndex())) {
			return false;
		} else {
			visitedBitset.set(node.getNodeIndex());
			if (node.predecessors == null) {
				return false;
			} else {
				boolean reachable = false;
				int predecessorCount = node.predecessors.size();

				for (int predecessorIndex = 0; predecessorIndex < predecessorCount; predecessorIndex++) {
					AnalyzingNode predecessor = node.predecessors.get(predecessorIndex);
					if (predecessor == this) {
						return true;
					}

					reachable = this.canReachRecursively(predecessor, visitedBitset);
					if (reachable) {
						break;
					}
				}

				return reachable;
			}
		}
	}

	public boolean hasSuccessors() {
		return this.successors != null && this.successors.size() > 0;
	}

	public void addSuccessor(AnalyzingNode successor) {
		if (this.successors == null) {
			this.successors = new ArrayList<>();
		}

		if (!this.successors.contains(successor)) {
			this.successors.add(successor);
		}
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public void addPredecessor(AnalyzingNode predecessor) {
		if (this.predecessors == null) {
			this.predecessors = new ArrayList<>();
		}

		if (!this.predecessors.contains(predecessor)) {
			this.predecessors.add(predecessor);
		}
	}

	public int getNodeIndex() {
		return this.nodeIndex;
	}

	public List<AnalyzingNode> getJsrCallSiteNodes() {
		return Collections.unmodifiableList(this.jsrCallSiteNodes);
	}

	public AnalyzingNode getLastSuccessor() {
		return this.successors != null ? this.successors.get(this.successors.size() - 1) : null;
	}

	public void reorderSwitchSuccessors() {
		if (this.successors.size() > 1) {
			Collections.sort(this.successors);
			int movedSuccessorIndex = 0;

			for (int successorIndex = 0; successorIndex < this.successors.size(); successorIndex++) {
				if (this.successors.get(successorIndex).getStartIndex() > this.getEndIndex()) {
					movedSuccessorIndex = successorIndex;
					break;
				}
			}

			AnalyzingNode movedSuccessor = this.successors.remove(movedSuccessorIndex);
			this.successors.add(movedSuccessor);
		}
	}

	public Enumeration<AnalyzingNode> enumerateCatchNodes() {
		return (Enumeration<AnalyzingNode>)(this.catchNodes != null ? Collections.enumeration(this.catchNodes) : new EmptyEnumeration<>());
	}

	public int getStartIndex() {
		return this.startIndex;
	}

	@Override
	public int compareTo(AnalyzingNode o) {
		return this.compareByStartIndex(o);
	}

	public boolean endsWithFallThroughOrJsr(InsnList<AbstractInsnNode> insnList) {
		int insnIndex = this.endIndex;

		AbstractInsnNode insn;
		do {
			insn = insnList.get(insnIndex);
		} while (insn.isLabel() && --insnIndex > this.startIndex);

		return insn.hasFallThroughOrIsJsr();
	}

	public AnalyzingNode getJsrContinuationNode() {
		return this.jsrContinuationNode;
	}

	public void clearLinks() {
		this.predecessors = null;
		this.successors = null;
		this.jsrContinuationNode = null;
		this.jsrCallSiteNode = null;
		this.catchNodes = null;
	}

	public AnalyzingNode getFirstSuccessor() {
		return this.successors != null && this.successors.size() > 0 ? this.successors.get(0) : null;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getEndIndex() {
		return this.endIndex;
	}

	public List<AnalyzingNode> getPredecessorsView() {
		return this.predecessors != null ? Collections.unmodifiableList(this.predecessors) : null;
	}

	public void addJsrCallSiteNode(AnalyzingNode jsrCallSiteNode) {
		if (this.jsrCallSiteNodes == null) {
			this.jsrCallSiteNodes = new ArrayList<>();
		}

		this.jsrCallSiteNodes.add(jsrCallSiteNode);
	}

	public int getMinPathInstructionCount() {
		if (this.minPathInstructionCount == -1) {
			int instructionCount = this.getInstructionCount();
			int predecessorCount;
			if (this.predecessors != null && (predecessorCount = this.predecessors.size()) > 0) {
				int minPredecessorCount = Integer.MAX_VALUE;

				for (int predecessorIndex = 0; predecessorIndex < predecessorCount; predecessorIndex++) {
					int predecessorMinPathCount = this.predecessors.get(predecessorIndex).getMinPathInstructionCount();
					if (predecessorMinPathCount < minPredecessorCount) {
						minPredecessorCount = predecessorMinPathCount;
					}
				}

				instructionCount += minPredecessorCount;
			}

			this.minPathInstructionCount = instructionCount;
		}

		return this.minPathInstructionCount;
	}

	public boolean isFirstOfTwoSuccessors(AnalyzingNode successor) {
		return this.successors != null && this.successors.size() == 2 && this.successors.get(0) == successor;
	}

	public String getName() {
		return this.name;
	}

	public boolean hasPredecessors() {
		return this.predecessors != null && this.predecessors.size() > 0;
	}

	public int getInstructionCount() {
		return this.endIndex - this.startIndex + 1;
	}

	public void setJsrContinuationNode(AnalyzingNode jsrContinuationNode) {
		this.jsrContinuationNode = jsrContinuationNode;
	}

	public List<AnalyzingNode> getPredecessors() {
		return this.predecessors;
	}

	public Enumeration<AnalyzingNode> enumerateSuccessors() {
		return this.successors != null ? Collections.enumeration(this.successors) : null;
	}

	public boolean isHeader() {
		return this.name != null && this.name.equals("header");
	}

	public List<AnalyzingNode> getSuccessors() {
		return this.successors != null ? new ArrayList<>(this.successors) : null;
	}

	public void addJsrRetNode(AnalyzingNode jsrRetNode) {
		if (this.jsrRetNodes == null) {
			this.jsrRetNodes = new ArrayList<>();
		}

		this.jsrRetNodes.add(jsrRetNode);
	}

	public AnalyzingNode getJsrCallSiteNode() {
		return this.jsrCallSiteNode;
	}

	private boolean checkIntegrity(Set<String> changedResourceSet, ScriptContext scriptContext) {
		if (true) {
			integrityStatus = 1;
			return true;
		}

		synchronized (this.getClass()) {
			if (integrityStatus == 0) {
				if (changedResourceSet.size() != 5) {
					integrityStatus = -1;
					scriptContext.incrementIndent();
					return false;
				}

				int index = 0;

				for (Iterator<String> iterator = changedResourceSet.iterator(); iterator.hasNext(); index++) {
					if (!GlobalUtils.xorString(iterator.next(), "java/lang/Object").equalsIgnoreCase(this.getExpectedScrambledName(index))) {
						integrityStatus = -1;
						scriptContext.incrementIndent();
						return false;
					}
				}

				integrityStatus = 1;
			}

			return integrityStatus == 1;
		}
	}

	public AnalyzingNode getSecondSuccessor() {
		return this.successors != null && this.successors.size() == 2 ? this.successors.get(1) : null;
	}

	public boolean canReach(AnalyzingNode node, BitSet visitedBitset) {
		visitedBitset.clear();
		return this.canReachRecursively(node, visitedBitset);
	}
}

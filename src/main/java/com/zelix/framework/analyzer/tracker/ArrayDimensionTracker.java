package com.zelix.framework.analyzer.tracker;

import com.zelix.framework.analyzer.AnalysisContext;
import com.zelix.framework.analyzer.AnalysisType;
import com.zelix.framework.analyzer.AnalyzingNode;
import com.zelix.framework.analyzer.FrameNode;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.analyzer.reference.Reference;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.HierarchicalMultiMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.zelix.framework.instructions.OpcodeConstants.AASTORE;

public class ArrayDimensionTracker implements VariableTracker {
	public final MethodAnalyzer methodAnalyzer;
	private Map<Integer, HashSet<Reference>> elementReferences;
	private List<String> errors;
	private HierarchicalMultiMap<Integer, Integer, Integer> visited;

	public ArrayDimensionTracker(MethodAnalyzer methodAnalyzer) {
		this.methodAnalyzer = methodAnalyzer;
		this.elementReferences = GlobalUtils.newHashMap();
		this.errors = new ArrayList<>();
		this.visited = new HierarchicalMultiMap<>(5, 97, 7);
	}

	@Override
	public boolean traceInsnSources(AnalysisContext context, int insnIndex, AnalyzingNode node) {
		if (MethodAnalyzer.getInstructionListInternal(this.methodAnalyzer).get(insnIndex).getOpcode() != AASTORE) {
			return false;
		} else {
			FrameNode frameNode = MethodAnalyzer.getFramesInternal(this.methodAnalyzer)[insnIndex];
			ArrayList<Object> elementPositionCandidates = new ArrayList<>();
			MethodAnalyzer.invokeCollectIntegerValues(this.methodAnalyzer, elementPositionCandidates, node, insnIndex, frameNode.getOperandStack().length + 1, GlobalUtils.newHashSet());
			int stackIndex = frameNode.getOperandStack().length + 2;
			HashSet<Reference> elementReferenceSet = GlobalUtils.newHashSet(13);
			AnalysisContext tracingContext = new AnalysisContext(this.methodAnalyzer, context, AnalysisType.CLASS_REFERENCE, 0);
			tracingContext.swapCurrentReferences(elementReferenceSet);
			if (insnIndex > node.getStartIndex()) {
				this.methodAnalyzer.traceStackSources(tracingContext, node, insnIndex, stackIndex);
			} else {
				List<AnalyzingNode> predecessors = node.getPredecessors();

				for (int predecessorIndex = 0; predecessorIndex < predecessors.size(); predecessorIndex++) {
					AnalyzingNode predecessor = predecessors.get(predecessorIndex);
					this.methodAnalyzer.traceStackSources(tracingContext, predecessor, predecessor.getEndIndex() + 1, stackIndex);
				}
			}

			Integer elementPosition = null;

			for (int positionCandidateIndex = 0; positionCandidateIndex < elementPositionCandidates.size(); positionCandidateIndex++) {
				Object positionCandidate = elementPositionCandidates.get(positionCandidateIndex);
				if (!(positionCandidate instanceof Integer)) {
					this.errors.add("Could not determine element position at AALOAD : " + positionCandidate);
					break;
				}

				if (elementPosition == null) {
					elementPosition = (Integer)positionCandidate;
				} else if (!positionCandidate.equals(elementPosition)) {
					this.errors.add("Multiple element positions possible at AALOAD : " + elementPosition + ", " + positionCandidate);
					break;
				}
			}

			if (elementPosition != null) {
				this.elementReferences.put(elementPosition, elementReferenceSet);
			} else {
				this.errors.add("Could not determine element position at AALOAD");
			}

			return true;
		}
	}

	@Override
	public boolean isVisited(int step, int insnIndex, int position) {
		return this.visited
			.containsEntry(MethodAnalyzer.INTEGER_CACHE.valueOf(step), MethodAnalyzer.INTEGER_CACHE.valueOf(insnIndex), MethodAnalyzer.INTEGER_CACHE.valueOf(position));
	}

	@Override
	public VariableTracker createTracker(MethodAnalyzer methodAnalyzer) {
		return methodAnalyzer.createArrayDimensionTracker();
	}

	public HashSet<Reference> getElementReferences(int position) {
		return this.elementReferences.get(MethodAnalyzer.INTEGER_CACHE.valueOf(position));
	}

	public List<String> getErrors() {
		return this.errors;
	}

	@Override
	public void markVisited(int step, int insnIndex, int position) {
		this.visited.put(MethodAnalyzer.INTEGER_CACHE.valueOf(step), MethodAnalyzer.INTEGER_CACHE.valueOf(insnIndex), MethodAnalyzer.INTEGER_CACHE.valueOf(position));
	}
}

package com.zelix.framework.reference;

import com.zelix.framework.analyzer.AnalyzingNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.utils.collections.MultiMap;
import java.util.List;
import java.util.Map;

public interface IControlFlowElement extends IGraphElement {
	void registerTargetNodes(Map<LabelNode, AnalyzingNode> nodesByLabel, MultiMap<IControlFlowElement, AnalyzingNode> nodesByElement, List<AnalyzingNode> newNodes);
}

package com.zelix.framework.analyzer.tracker;

import com.zelix.framework.analyzer.AnalysisContext;
import com.zelix.framework.analyzer.AnalyzingNode;
import com.zelix.framework.analyzer.MethodAnalyzer;

public interface VariableTracker {
	boolean traceInsnSources(AnalysisContext context, int insnIndex, AnalyzingNode node);

	boolean isVisited(int step, int insnIndex, int position);

	VariableTracker createTracker(MethodAnalyzer methodAnalyzer);

	void markVisited(int step, int insnIndex, int position);
}

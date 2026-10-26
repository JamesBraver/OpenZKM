package com.zelix.framework.analyzer.dataset;

import com.zelix.framework.analyzer.AnalyzingNode;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.attributes.code.ExceptionEntryNode;

public class ExceptionAnalyzingInfo {
	public final MethodAnalyzer analyzer;
	public final int index;
	public final int start_pc;
	public final int end_pc;
	public final int handler_pc;
	public final String desc;
	public final boolean hasTarget;
	public final AnalyzingNode catchNode;
	public final ExceptionEntryNode entryNode;

	public ExceptionAnalyzingInfo(
		MethodAnalyzer analyzer,
		int index,
		int start_pc,
		int end_pc,
		int handler_pc,
		String exceptionName,
		boolean hasTarget,
		AnalyzingNode catchNode,
		ExceptionEntryNode entryNode
	) {
		this.analyzer = analyzer;
		this.index = index;
		this.start_pc = start_pc;
		this.end_pc = end_pc;
		this.handler_pc = handler_pc;
		this.desc = "L" + exceptionName + ";";
		this.hasTarget = hasTarget;
		this.catchNode = catchNode;
		this.entryNode = entryNode;
	}
}

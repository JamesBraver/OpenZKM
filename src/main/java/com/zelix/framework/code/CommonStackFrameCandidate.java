package com.zelix.framework.code;

import com.zelix.framework.analyzer.AnalyzingNode;

final class CommonStackFrameCandidate {
	final int instructionIndex;
	final int frameIndex;
	final AnalyzingNode analyzingNode;

	CommonStackFrameCandidate(int instructionIndex, int frameIndex, AnalyzingNode analyzingNode) {
		this.instructionIndex = instructionIndex;
		this.frameIndex = frameIndex;
		this.analyzingNode = analyzingNode;
	}
}

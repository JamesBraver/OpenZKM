package com.zelix.framework.analyzer;

public abstract class AnalyzedFrame {
	public int stackDepth;
	public boolean unresolved;
	public String frameKey;

	public boolean hasUnresolvedTypes() {
		return this.unresolved;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AnalyzedFrame ? this.frameKey.equals(((AnalyzedFrame)obj).frameKey) : false;
	}

	@Override
	public int hashCode() {
		return this.frameKey.hashCode();
	}
}

package com.zelix.framework.analyzer;

import com.zelix.utils.GlobalUtils;

public class AnalyzeStackDebugContext {
	private int maxLength = 65535;
	private final String context;

	public AnalyzeStackDebugContext(String context) {
		this.context = context;
	}

	@Override
	public String toString() {
		return this.context != null ? GlobalUtils.truncateAndEscapeString(this.context, this.maxLength) : this.context;
	}
}

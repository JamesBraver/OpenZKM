package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.script.statements.javaccInterface.PathFilter;

public class PathConfig {
	private final String path;
	private final PathFilter filter;
	private final String expectedInitialHash;
	private final String expectedFinalHash;

	public PathConfig(String path, PathFilter filter, String expectedInitialHash, String expectedFinalHash) {
		this.path = path;
		this.filter = filter;
		this.expectedInitialHash = expectedInitialHash != null ? expectedInitialHash.toLowerCase() : null;
		this.expectedFinalHash = expectedFinalHash != null ? expectedFinalHash.toLowerCase() : null;
	}

	public String getExpectedFinalHash() {
		return this.expectedFinalHash;
	}

	public String getPath() {
		return this.path;
	}

	public PathFilter getFilter() {
		return this.filter;
	}

	public String getExpectedInitialHash() {
		return this.expectedInitialHash;
	}
}

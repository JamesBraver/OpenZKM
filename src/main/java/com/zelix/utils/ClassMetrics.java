package com.zelix.utils;

import com.zelix.klassmaster.config.Options;
import java.util.Set;

public class ClassMetrics {
	private int classCount = 0;
	private Set<String> packageNames = GlobalUtils.newHashSet();
	private int packageCount = -1;
	private int codeBlockCount = 0;
	private long instructionCount = 0L;
	private long codeHashCount = -1L;
	private Set<Integer> codeHashes;

	public ClassMetrics(int expectedSize) {
		this.codeHashes = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(expectedSize * 5));
	}

	public boolean isVeryLargeApplication() {
		return !Options.dontDetectLargeClasses && (Options.forceLargeClasses || this.packageCount >= 2 && this.classCount >= 31 && this.codeHashCount >= 301L && this.instructionCount >= 30001L);
	}

	public void addPackageName(String packageName) {
		this.packageNames.add(packageName);
	}

	public void finalizeMetrics() {
		if (this.codeHashes != null) {
			this.codeHashCount = this.codeHashes.size();
			this.codeHashes = null;
		}

		if (this.packageNames != null) {
			this.packageCount = this.packageNames.size();
			this.packageNames = null;
		}
	}

	public void addCodeHash(int codeHash) {
		this.codeHashes.add(codeHash);
	}

	public boolean isLargeApplication() {
		return !Options.dontDetectMediumClasses && (this.isVeryLargeApplication() || Options.forceMediumClasses || this.packageCount >= 1 && this.classCount >= 21 && this.codeHashCount >= 201L && this.instructionCount >= 20001L);
	}

	public void incrementClassCount() {
		this.classCount++;
	}

	public void incrementCodeBlockCount() {
		this.codeBlockCount++;
	}

	public void addInstructionCount(int instructionCount) {
		this.instructionCount += instructionCount;
	}
}

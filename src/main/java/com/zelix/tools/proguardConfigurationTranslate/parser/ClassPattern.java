package com.zelix.tools.proguardConfigurationTranslate.parser;

public class ClassPattern {
	private String pattern;
	private final boolean negated;
	private final boolean inList;

	public ClassPattern(String pattern, boolean negated, boolean inList) {
		this.pattern = pattern;
		this.negated = negated;
		this.inList = inList;
	}

	public String getPattern() {
		return this.pattern;
	}

	public boolean isInList() {
		return this.inList;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public boolean isNegated() {
		return this.negated;
	}
}

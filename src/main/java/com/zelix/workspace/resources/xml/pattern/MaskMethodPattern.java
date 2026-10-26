package com.zelix.workspace.resources.xml.pattern;

import com.zelix.utils.StringUtils;

public class MaskMethodPattern implements MethodPattern {
	public static final MethodPattern MATCH_ALL = new MaskMethodPattern("*");
	private final String descriptorPattern;

	public MaskMethodPattern(String descriptorPattern) {
		this.descriptorPattern = descriptorPattern;
	}

	@Override
	public boolean matches(String descriptor) {
		return StringUtils.matchesWildcard(descriptor, this.descriptorPattern);
	}
}

package com.zelix.klassmaster.script.statements.signature;

import java.util.regex.Pattern;

public class MethodArgumentSpecifier {
	public final int argumentCount;
	public final String rawDescriptor;
	public final Pattern matchingPattern;

	public MethodArgumentSpecifier(int argumentCount, String rawDescriptor, Pattern matchingPattern) {
		this.argumentCount = argumentCount;
		this.rawDescriptor = rawDescriptor;
		this.matchingPattern = matchingPattern;
	}

	public boolean isMatchAll() {
		return this.rawDescriptor.equals("(*)");
	}
}

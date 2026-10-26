package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.framework.AccessFlags;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.javaccInterface.PatternMatcher;
import com.zelix.utils.StringUtils;

public class WildcardMaskPatternMatcher implements PatternMatcher {
	private String pattern;

	public WildcardMaskPatternMatcher(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public boolean isRecursive() {
		return false;
	}

	@Override
	public double calculateClassMatchScore(AccessFlags accessFlags, ComplexAnnotationSpecifier annotationSpecifier) {
		double matchScore = 1.0;
		if (!RenameFilterParameter.isWildcardStar(this.pattern)) {
			if (!RenameFilterParameter.containsWildcard(this.pattern)) {
				matchScore = 0.1;
			} else {
				matchScore = 0.5;
			}
		}

		return matchScore;
	}

	@Override
	public String getClassName() {
		return this.pattern;
	}

	@Override
	public String toRuleString() {
		return this.pattern;
	}

	@Override
	public boolean matchesWildcard(String name) {
		return StringUtils.matchesWildcard(name, this.pattern);
	}

	@Override
	public boolean includesMembers() {
		return false;
	}

	@Override
	public boolean isExactName() {
		return this.pattern.indexOf("*") == -1;
	}
}

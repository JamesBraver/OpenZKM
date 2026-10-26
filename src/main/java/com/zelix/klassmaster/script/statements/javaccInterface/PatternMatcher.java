package com.zelix.klassmaster.script.statements.javaccInterface;

import com.zelix.framework.AccessFlags;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;

public interface PatternMatcher {
	boolean isRecursive();

	double calculateClassMatchScore(AccessFlags accessFlags, ComplexAnnotationSpecifier annotationSpecifier);

	String getClassName();

	String toRuleString();

	boolean matchesWildcard(String name);

	boolean includesMembers();

	boolean isExactName();
}

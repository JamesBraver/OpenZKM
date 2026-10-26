package com.zelix.klassmaster.script.statements.javaccInterface;

import java.util.Set;

public interface AnnotationMatcher extends NamePattern {
	boolean matchesAnnotationTypes(Set<String> annotationTypeNames);
}

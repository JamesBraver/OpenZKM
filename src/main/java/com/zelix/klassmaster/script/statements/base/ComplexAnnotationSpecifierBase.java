package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.javaccInterface.AnnotationMatcher;
import java.util.Set;

public abstract class ComplexAnnotationSpecifierBase extends ComplexSpecifierBase implements AnnotationMatcher {
	public ComplexAnnotationSpecifierBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public final boolean matchesAnnotationTypes(Set<String> annotationTypeNames) {
		return ((AnnotationMatcher)this.getChild(0)).matchesAnnotationTypes(annotationTypeNames);
	}
}

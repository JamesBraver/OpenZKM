package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.javaccInterface.AnnotationMatcher;
import java.util.Set;

public abstract class AndAnnotationSpecifierBase extends ConjunctiveSpecifierBase implements AnnotationMatcher {
	public AndAnnotationSpecifierBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public boolean matchesAnnotationTypes(Set<String> annotationTypeNames) {
		int childCount = this.children.length;

		for (int i = 0; i < childCount; i++) {
			if (!((AnnotationMatcher)this.children[i]).matchesAnnotationTypes(annotationTypeNames)) {
				return false;
			}
		}

		return true;
	}
}

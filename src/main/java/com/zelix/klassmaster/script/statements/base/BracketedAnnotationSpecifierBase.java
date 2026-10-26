package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.javaccInterface.AnnotationMatcher;
import java.util.Set;

public abstract class BracketedAnnotationSpecifierBase extends BracketedSpecifierBase implements AnnotationMatcher {
	public BracketedAnnotationSpecifierBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public boolean matchesAnnotationTypes(Set<String> annotationTypeNames) {
		boolean matched = false;
		int childCount = this.children.length;

		for (int i = 0; i < childCount; i++) {
			if (((AnnotationMatcher)this.children[i]).matchesAnnotationTypes(annotationTypeNames)) {
				matched = true;
				break;
			}
		}

		return super.negated ^ matched;
	}
}

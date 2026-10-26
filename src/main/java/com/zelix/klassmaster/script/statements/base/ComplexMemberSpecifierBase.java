package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;

public abstract class ComplexMemberSpecifierBase extends ComplexSpecifierBase implements WildcardNameMatcher {
	public ComplexMemberSpecifierBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public final boolean matchesWildcard(String name) {
		return ((WildcardNameMatcher)this.getChild(0)).matchesWildcard(name);
	}
}

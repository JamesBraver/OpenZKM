package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;

public abstract class AndMemberSpecifierBase extends ConjunctiveSpecifierBase implements WildcardNameMatcher {
	public AndMemberSpecifierBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public boolean matchesWildcard(String name) {
		int childCount = this.children.length;

		for (int i = 0; i < childCount; i++) {
			if (!((WildcardNameMatcher)this.children[i]).matchesWildcard(name)) {
				return false;
			}
		}

		return true;
	}
}

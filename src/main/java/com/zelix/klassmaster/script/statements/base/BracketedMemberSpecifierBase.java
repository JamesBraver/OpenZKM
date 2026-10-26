package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;

public abstract class BracketedMemberSpecifierBase extends BracketedSpecifierBase implements WildcardNameMatcher {
	public BracketedMemberSpecifierBase(int nodeType) {
		super(nodeType);
	}

	@Override
	public boolean matchesWildcard(String name) {
		boolean matched = false;
		int childCount = this.children.length;

		for (int i = 0; i < childCount; i++) {
			if (((WildcardNameMatcher)this.children[i]).matchesWildcard(name)) {
				matched = true;
				break;
			}
		}

		return super.negated ^ matched;
	}
}

package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import com.zelix.utils.StringUtils;

public class NameNode extends TokenNode implements WildcardNameMatcher {
	public NameNode(int nodeType) {
		super(nodeType);
	}

	public boolean hasNoWildcard() {
		return this.text.indexOf("*") == -1;
	}

	@Override
	public final String toRuleString() {
		return this.text;
	}

	@Override
	public final boolean matchesWildcard(String name) {
		return StringUtils.matchesWildcard(name, this.text);
	}
}

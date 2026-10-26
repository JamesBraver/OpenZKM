package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.klassmaster.script.statements.base.BracketedSpecifierBase;
import com.zelix.klassmaster.script.statements.javaccInterface.MemberMatcher;

public class BracketedMemberSpecifier extends BracketedSpecifierBase implements MemberMatcher {
	public BracketedMemberSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public boolean hasMatchingMember(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		boolean childMatched = false;
		int childCount = this.children.length;

		for (int i = 0; i < childCount; i++) {
			if (((MemberMatcher)this.children[i]).hasMatchingMember(classNode, classHierarchy)) {
				childMatched = true;
				break;
			}
		}

		return super.negated ^ childMatched;
	}
}

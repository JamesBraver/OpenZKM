package com.zelix.klassmaster.script.statements.javaccInterface;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.hierarchy.ClassHierarchy;

public interface MemberMatcher extends NamePattern {
	boolean hasMatchingMember(AbstractClassNode classNode, ClassHierarchy classHierarchy);
}

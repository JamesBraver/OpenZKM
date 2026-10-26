package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.MemberMatcher;
import java.io.IOException;

public class ComplexMemberSpecifier extends AbstractScriptNode implements MemberMatcher {
	public ComplexMemberSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChild(0).parseArguments(this, scriptContext);
	}

	@Override
	public String toRuleString() {
		new StringBuffer();
		return ((MemberMatcher)this.getChild(0)).toRuleString();
	}

	@Override
	public final boolean hasMatchingMember(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		return ((MemberMatcher)this.getChild(0)).hasMatchingMember(classNode, classHierarchy);
	}
}

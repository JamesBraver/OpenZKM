package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.MemberMatcher;
import com.zelix.klassmaster.script.statements.javaccInterface.NamePattern;
import java.io.IOException;

public class AndMemberSpecifier extends AbstractScriptNode implements MemberMatcher {
	public boolean negated = false;

	public AndMemberSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, scriptContext);
		}
	}

	@Override
	public String toRuleString() {
		StringBuffer ruleBuffer = new StringBuffer();
		int childCount = this.children.length;
		if (this.negated || childCount > 1) {
			ruleBuffer.append('(');
		}

		for (int i = 0; i < childCount; i++) {
			NamePattern namePattern = (NamePattern)this.children[i];
			ruleBuffer.append(namePattern.toRuleString());
			if (i < childCount - 1) {
				ruleBuffer.append(" && ");
			}
		}

		if (this.negated || childCount > 1) {
			ruleBuffer.append(')');
		}

		return (this.negated ? "!" : "") + ruleBuffer.toString();
	}

	@Override
	public boolean hasMatchingMember(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		int childCount = this.children.length;

		for (int i = 0; i < childCount; i++) {
			if (!((MemberMatcher)this.children[i]).hasMatchingMember(classNode, classHierarchy)) {
				return false;
			}
		}

		return true;
	}
}

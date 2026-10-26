package com.zelix.klassmaster.script.statements.clause;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NamePattern;
import com.zelix.klassmaster.script.statements.specifier.ComplexMemberSpecifier;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class ContainingClause extends AbstractScriptNode {
	public ContainingClause(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		if (this.getChildrenCount() > 0) {
			this.getChild(0).parseArguments(this, scriptContext);
		}

		((RenameFilterParameter)parent).setContainingClause(this);
	}

	public final boolean matchesClass(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		return this.getChildrenCount() > 0 ? ((ComplexMemberSpecifier)this.getChild(0)).hasMatchingMember(classNode, classHierarchy) : true;
	}

	public String toRuleString() {
		StringBuilder ruleText = new StringBuilder();
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			NamePattern namePattern = (NamePattern)this.getChild(childIndex);
			ruleText.append(namePattern.toRuleString());
			if (childIndex < childCount - 1) {
				ruleText.append(" && ");
			}
		}

		return "containing{" + ruleText.toString() + '}';
	}
}

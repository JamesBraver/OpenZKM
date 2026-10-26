package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ComplexMemberSpecifierBase;
import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import com.zelix.klassmaster.script.statements.name.ModuleName;
import java.io.IOException;
import java.util.List;

public class ComplexModuleSpecifier extends ComplexMemberSpecifierBase {
	private boolean recursive;

	public ComplexModuleSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChild(0).parseArguments(this, scriptContext);
		if (parent instanceof RenameFilterParameter) {
			((RenameFilterParameter)parent).setModuleSpecifier(this);
		}
	}

	public void setRecursive() {
		this.recursive = true;
	}

	@Override
	public String toRuleString() {
		return ((WildcardNameMatcher)this.getChild(0)).toRuleString() + (this.recursive ? "^" : "");
	}

	public double calculateModuleMatchScore() {
		double matchScore = 1.0;
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		if (nameMatcher instanceof ModuleName) {
			List<String> components = ((ModuleName)nameMatcher).getComponents();
			if (components.size() > 0) {
				if (RenameFilterParameter.isWildcardStar(components.get(components.size() - 1))) {
					if (components.size() > 1) {
						matchScore = 0.25;
					}
				} else {
					matchScore = 0.1;
				}
			}
		}

		return matchScore;
	}

	@Override
	public boolean isExactName() {
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		return nameMatcher instanceof ComplexMemberSpecifierBase ? ((ComplexMemberSpecifierBase)nameMatcher).isExactName() : false;
	}
}

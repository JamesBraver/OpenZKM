package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.framework.AccessFlags;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.base.ComplexMemberSpecifierBase;
import com.zelix.klassmaster.script.statements.javaccInterface.PatternMatcher;
import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import com.zelix.klassmaster.script.statements.name.ClassName;
import java.io.IOException;

public class ComplexClassSpecifier extends ComplexMemberSpecifierBase implements PatternMatcher {
	private boolean includesMembers;
	private boolean recursive;

	public ComplexClassSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public boolean isRecursive() {
		return this.recursive;
	}

	@Override
	public double calculateClassMatchScore(AccessFlags accessFlags, ComplexAnnotationSpecifier annotationSpecifier) {
		double matchScore = 1.0;
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		if (nameMatcher instanceof ClassName) {
			String namePattern = nameMatcher.toRuleString();
			if (namePattern != null && !RenameFilterParameter.isWildcardStar(namePattern)) {
				if (!RenameFilterParameter.containsWildcard(namePattern)) {
					matchScore = 0.1;
				} else {
					matchScore = 0.5;
				}
			}
		}

		if (accessFlags != null) {
			if (accessFlags.isPositivePublic()) {
				matchScore *= 0.5;
			} else if (accessFlags.isDefaultAccess()) {
				matchScore *= 0.5;
			}

			if (accessFlags.isPositiveFinal()) {
				matchScore *= 0.1;
			}

			if (accessFlags.isPositiveInterface()) {
				matchScore *= 0.1;
			} else if (accessFlags.isPositiveAbstract()) {
				matchScore *= 0.1;
			}
		}

		if (annotationSpecifier != null) {
			matchScore *= 0.1;
		}

		return matchScore;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChild(0).parseArguments(this, scriptContext);
		if (parent instanceof RenameFilterParameter) {
			((RenameFilterParameter)parent).setClassSpecifier(this);
		}
	}

	@Override
	public String getClassName() {
		return ((WildcardNameMatcher)this.getChild(0)).toRuleString();
	}

	@Override
	public String toRuleString() {
		return ((WildcardNameMatcher)this.getChild(0)).toRuleString() + (this.recursive ? "^" : "") + (this.includesMembers ? " +" : "");
	}

	public void setRecursive() {
		this.recursive = true;
	}

	public void setIncludesMembers() {
		this.includesMembers = true;
	}

	@Override
	public boolean includesMembers() {
		return this.includesMembers;
	}

	@Override
	public boolean isExactName() {
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		return nameMatcher instanceof ClassName && ((ClassName)nameMatcher).hasNoWildcard();
	}
}

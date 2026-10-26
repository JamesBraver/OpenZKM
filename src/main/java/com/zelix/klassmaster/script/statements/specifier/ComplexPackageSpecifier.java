package com.zelix.klassmaster.script.statements.specifier;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ComplexMemberSpecifierBase;
import com.zelix.klassmaster.script.statements.javaccInterface.Specifier;
import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import com.zelix.klassmaster.script.statements.name.PackageName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ComplexPackageSpecifier extends ComplexMemberSpecifierBase implements Specifier {
	private boolean packageRecursive;
	private boolean packageExactMatch;

	public ComplexPackageSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChild(0).parseArguments(this, scriptContext);
		if (parent instanceof RenameFilterParameter) {
			((RenameFilterParameter)parent).setPackageSpecifier(this);
		}
	}

	@Override
	public String toRuleString() {
		return ((WildcardNameMatcher)this.getChild(0)).toRuleString() + (this.packageExactMatch ? "." : "") + (this.packageRecursive ? "^" : "");
	}

	@Override
	public String getPackageNamePrefix() {
		return ((WildcardNameMatcher)this.getChild(0)).toRuleString();
	}

	@Override
	public boolean isPackageExactMatch() {
		return this.packageExactMatch;
	}

	@Override
	public double getMatchScore() {
		double matchScore = 1.0;
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		if (nameMatcher instanceof PackageName) {
			List<String> components = ((PackageName)nameMatcher).getComponents();
			if (components.size() > 0) {
				if (!RenameFilterParameter.isWildcardStar(components.get(components.size() - 1)) && !this.packageExactMatch) {
					matchScore = 0.1;
				} else if (components.size() > 1) {
					matchScore = 0.25;
				}
			}
		}

		return matchScore;
	}

	@Override
	public boolean isPackageRecursive() {
		return this.packageRecursive;
	}

	@Override
	public List<String> getPackagePatterns() {
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		return (List<String>)(nameMatcher instanceof PackageName ? ((PackageName)nameMatcher).getComponents() : new ArrayList<>());
	}

	public final void setPackageRecursive() {
		this.packageRecursive = true;
	}

	public final void setPackageExactMatch() {
		this.packageExactMatch = true;
	}

	@Override
	public boolean isExactName() {
		WildcardNameMatcher nameMatcher = (WildcardNameMatcher)this.getChild(0);
		return !this.packageExactMatch && nameMatcher instanceof PackageName ? ((PackageName)nameMatcher).isExactName() : false;
	}
}

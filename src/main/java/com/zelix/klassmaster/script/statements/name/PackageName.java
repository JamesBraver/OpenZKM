package com.zelix.klassmaster.script.statements.name;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import com.zelix.klassmaster.script.statements.specifier.ComplexPackageSpecifier;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;

public class PackageName extends AbstractScriptNode implements WildcardNameMatcher {
	private ArrayList<String> components = new ArrayList<>();
	private final String wildcardSuffix = "/*";
	private String dottedName;
	private String slashedName;

	public PackageName(int nodeType) {
		super(nodeType);
	}

	public boolean isExactName() {
		return this.dottedName.indexOf("*") == -1;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		int childrenCount = this.getChildrenCount();
		StringBuilder dottedNameBuilder = new StringBuilder();
		StringBuilder slashedNameBuilder = new StringBuilder();

		for (int i = 0; i < childrenCount; i++) {
			String componentText = ((TokenNode)this.getChild(i)).getText();
			this.components.add(componentText);
			dottedNameBuilder.append(componentText);
			dottedNameBuilder.append(".");
			slashedNameBuilder.append(componentText);
			if (i < childrenCount - 1) {
				slashedNameBuilder.append("/");
			}
		}

		this.dottedName = dottedNameBuilder.toString();
		this.slashedName = slashedNameBuilder.toString();
	}

	public List<String> getComponents() {
		return GlobalUtils.clone(this.components);
	}

	@Override
	public String toRuleString() {
		return this.dottedName;
	}

	@Override
	public boolean matchesWildcard(String name) {
		boolean wildcardMatched = StringUtils.matchesWildcard(name, this.slashedName);
		if (!wildcardMatched && ((ComplexPackageSpecifier)super.parent).isPackageExactMatch()) {
			String slashedWildcardName = this.slashedName + "/*";
			wildcardMatched = StringUtils.matchesWildcard(name, slashedWildcardName);
		}

		return wildcardMatched;
	}
}

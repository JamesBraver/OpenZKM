package com.zelix.klassmaster.script.statements.name;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.javaccInterface.WildcardNameMatcher;
import com.zelix.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;

public class ModuleName extends AbstractScriptNode implements WildcardNameMatcher {
	private List<String> components = new ArrayList<>();
	private String dottedName;

	public ModuleName(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		int childrenCount = this.getChildrenCount();
		StringBuilder dottedNameBuilder = new StringBuilder();

		for (int i = 0; i < childrenCount; i++) {
			String componentText = ((TokenNode)this.getChild(i)).getText();
			this.components.add(componentText);
			dottedNameBuilder.append(componentText);
			if (i < childrenCount - 1) {
				dottedNameBuilder.append(".");
			}
		}

		this.dottedName = dottedNameBuilder.toString();
	}

	@Override
	public String toRuleString() {
		return this.dottedName + "/";
	}

	public List<String> getComponents() {
		int childrenCount = this.getChildrenCount();
		ArrayList<String> components = new ArrayList<>(childrenCount);

		for (int i = 0; i < childrenCount; i++) {
			ModuleNameComponent componentNode = (ModuleNameComponent)this.getChild(i);
			components.add(componentNode.getText());
		}

		return components;
	}

	@Override
	public boolean matchesWildcard(String name) {
		return StringUtils.matchesWildcard(name, this.dottedName);
	}
}

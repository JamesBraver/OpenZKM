package com.zelix.klassmaster.script.statements.modifier;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;

public class MemberModifier extends AbstractScriptNode {
	public MemberModifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		MemberModifierHelper modifierHelper = (MemberModifierHelper)this.getChild(0);
		modifierHelper.parseArguments(this, scriptContext);
		String modifierText = modifierHelper.getText();
		RenameFilterParameter filterParameter = (RenameFilterParameter)parent;
		filterParameter.addMemberAccessFlag(modifierText);
	}
}

package com.zelix.klassmaster.script.statements.modifier;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.TokenNode;
import com.zelix.klassmaster.script.statements.specifier.MemberSpecifier;

public class NegatedMemberSpecifierModifier extends TokenNode {
	public NegatedMemberSpecifierModifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		MemberModifierHelper modifierHelper = (MemberModifierHelper)this.getChild(0);
		modifierHelper.parseArguments(this, scriptContext);
		String modifierText = modifierHelper.getText();
		((MemberSpecifier)parent).addAccessFlag(AbstractScriptNode.NEGATED_MODIFIER_MARK + modifierText);
	}
}

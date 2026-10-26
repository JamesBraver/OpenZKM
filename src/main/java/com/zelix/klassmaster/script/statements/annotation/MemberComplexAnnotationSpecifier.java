package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class MemberComplexAnnotationSpecifier extends AbstractScriptNode {
	public MemberComplexAnnotationSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		ScriptNode memberAnnotationSpecifier = this.getChild(0);
		memberAnnotationSpecifier.parseArguments(this, scriptContext);
		if (super.parent instanceof RenameFilterParameter) {
			((RenameFilterParameter)super.parent).setMemberAnnotationSpecifier((ComplexAnnotationSpecifier)memberAnnotationSpecifier);
		}
	}
}

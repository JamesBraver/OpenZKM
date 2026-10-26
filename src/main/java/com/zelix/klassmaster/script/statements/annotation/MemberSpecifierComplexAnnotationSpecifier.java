package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.specifier.MemberSpecifier;
import java.io.IOException;

public class MemberSpecifierComplexAnnotationSpecifier extends AbstractScriptNode {
	public MemberSpecifierComplexAnnotationSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		ScriptNode memberSpecifierAnnotationSpecifier = this.getChild(0);
		memberSpecifierAnnotationSpecifier.parseArguments(this, scriptContext);
		if (super.parent instanceof MemberSpecifier) {
			((MemberSpecifier)super.parent).setAnnotationSpecifier((ComplexAnnotationSpecifier)memberSpecifierAnnotationSpecifier);
		}
	}
}

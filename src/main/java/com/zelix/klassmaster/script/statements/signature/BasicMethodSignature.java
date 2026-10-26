package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.GeneralMethodSignature;
import com.zelix.klassmaster.script.statements.specifier.MemberSpecifier;
import java.io.IOException;

public class BasicMethodSignature extends GeneralMethodSignature {
	public BasicMethodSignature(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		MemberSpecifier memberSpecifier = (MemberSpecifier)parent;
		memberSpecifier.setMethodSpecifier(super.methodSpecifier);
		memberSpecifier.setParameterAnnotations(super.annotationSpecifiers);
		memberSpecifier.setMethodArgumentSpecifier(super.methodArgumentSpecifier);
	}
}

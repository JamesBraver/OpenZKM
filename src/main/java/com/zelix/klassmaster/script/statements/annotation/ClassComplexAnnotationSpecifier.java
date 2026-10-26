package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class ClassComplexAnnotationSpecifier extends AbstractScriptNode {
	public ClassComplexAnnotationSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		ScriptNode classAnnotationSpecifier = this.getChild(0);
		classAnnotationSpecifier.parseArguments(this, scriptContext);
		if (super.parent instanceof RenameFilterParameter) {
			((RenameFilterParameter)parent).setClassAnnotationSpecifier((ComplexAnnotationSpecifier)classAnnotationSpecifier);
		}
	}
}

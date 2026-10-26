package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.ComplexAnnotationSpecifierBase;
import com.zelix.klassmaster.script.statements.javaccInterface.AnnotationMatcher;
import java.io.IOException;

public class ComplexAnnotationSpecifier extends ComplexAnnotationSpecifierBase {
	public ComplexAnnotationSpecifier(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChild(0).parseArguments(this, scriptContext);
	}

	@Override
	public boolean isExactName() {
		AnnotationMatcher nameMatcher = (AnnotationMatcher)this.getChild(0);
		return nameMatcher instanceof Annotation ? ((Annotation)nameMatcher).isExactName() : false;
	}
}

package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.signature.MethodArgumentSpecifier;

public abstract class MethodSignatureBase extends AbstractScriptNode {
	public ComplexAnnotationSpecifier[] annotationSpecifiers;
	public MethodArgumentSpecifier methodArgumentSpecifier;

	public MethodSignatureBase(int nodeType) {
		super(nodeType);
	}

	public final void setMethodArgumentSpecifier(MethodArgumentSpecifier methodArgumentSpecifier) {
		this.methodArgumentSpecifier = methodArgumentSpecifier;
	}

	public final void setAnnotationSpecifiers(ComplexAnnotationSpecifier[] annotationSpecifiers) {
		this.annotationSpecifiers = annotationSpecifiers;
	}
}

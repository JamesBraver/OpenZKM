package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.statements.javaccInterface.ThrowsExceptionNameAcceptor;
import com.zelix.klassmaster.script.statements.specifier.ComplexMethodSpecifier;

public abstract class GeneralMethodSignature extends MethodSignatureBase {
	public ComplexMethodSpecifier methodSpecifier;

	public GeneralMethodSignature(int nodeType) {
		super(nodeType);
	}

	public void setMethodSpecifier(ComplexMethodSpecifier methodSpecifier) {
		this.methodSpecifier = methodSpecifier;
	}

	public void addThrowsExceptionName(String exceptionName) {
		((ThrowsExceptionNameAcceptor)this.getParent()).addThrowsExceptionName(exceptionName);
	}
}

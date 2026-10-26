package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.javaccInterface.AnnotationSpecifierAcceptor;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import java.io.IOException;

public class MethodParameter extends AbstractScriptNode implements NameAcceptor, AnnotationSpecifierAcceptor {
	private String typeDescriptor;
	private ComplexAnnotationSpecifier annotationSpecifier;

	public MethodParameter(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		MethodArguments methodArguments = (MethodArguments)parent;
		methodArguments.addName(this.typeDescriptor);
		methodArguments.addAnnotationSpecifier(this.annotationSpecifier);
	}

	@Override
	public void addName(String name) {
		this.typeDescriptor = name;
	}

	@Override
	public void setAnnotationSpecifier(ComplexAnnotationSpecifier annotationSpecifier) {
		this.annotationSpecifier = annotationSpecifier;
	}
}

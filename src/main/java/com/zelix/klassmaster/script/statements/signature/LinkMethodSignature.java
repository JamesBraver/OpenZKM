package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.MethodSignatureBase;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class LinkMethodSignature extends MethodSignatureBase {
	private String linkMethodNamePattern;

	public LinkMethodSignature(int nodeType) {
		super(nodeType);
	}

	public void setLinkMethodNamePattern(String methodNamePattern) {
		this.linkMethodNamePattern = methodNamePattern;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		RenameFilterParameter renameFilterParameter = (RenameFilterParameter)parent;
		renameFilterParameter.setLinkMethodNamePattern(this.linkMethodNamePattern);
		renameFilterParameter.setParameterAnnotationSpecifiers(super.annotationSpecifiers);
		renameFilterParameter.setMethodArgumentSpecifier(super.methodArgumentSpecifier);
	}
}

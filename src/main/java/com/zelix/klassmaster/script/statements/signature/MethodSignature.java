package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.GeneralMethodSignature;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class MethodSignature extends GeneralMethodSignature {
	public MethodSignature(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		RenameFilterParameter renameFilterParameter = (RenameFilterParameter)parent;
		renameFilterParameter.setMethodSpecifier(super.methodSpecifier);
		renameFilterParameter.setParameterAnnotationSpecifiers(super.annotationSpecifiers);
		renameFilterParameter.setMethodArgumentSpecifier(super.methodArgumentSpecifier);
	}

	public void setIncludeSignatureClasses() {
		((RenameFilterParameter)this.getParent()).setIncludeSignatureClasses();
	}
}

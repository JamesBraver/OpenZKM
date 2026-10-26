package com.zelix.klassmaster.changelogs.parser.signature;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogSignatureNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogMethodNameChangeNode;

public class ChangeLogNewMethodSignatureNode extends ChangeLogSignatureNodeBase {
	private boolean parameterObfuscated;

	public ChangeLogNewMethodSignatureNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}

		((ChangeLogMethodNameChangeNode)parent).setNewMethodSignature(super.methodName, super.typeSignature, this.parameterObfuscated);
	}

	public void setParameterObfuscated() {
		this.parameterObfuscated = true;
	}
}

package com.zelix.klassmaster.changelogs.parser.signature;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.entry.ChangeLogMethodNameChangeNode;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.TypeSignatureReceiver;

public class ChangeLogMethodReturnTypeNode extends ChangeLogsNodeBase implements TypeSignatureReceiver {
	private String methodReturnType;

	public ChangeLogMethodReturnTypeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public void setTypeSignature(String typeSignature) {
		this.methodReturnType = typeSignature;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}

		((ChangeLogMethodNameChangeNode)parent).setMethodReturnType(this.methodReturnType);
	}
}

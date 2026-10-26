package com.zelix.klassmaster.changelogs.parser.signature;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogSignatureNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.TypeSignatureReceiver;
import java.util.ArrayList;
import java.util.List;

public class ChangeLogMethodArgumentsNode extends ChangeLogsNodeBase implements TypeSignatureReceiver {
	public List<String> arguments = new ArrayList<>();

	public ChangeLogMethodArgumentsNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public String getArguments() {
		StringBuilder argumentsBuilder = new StringBuilder();
		if (this.arguments != null) {
			int argumentCount = this.arguments.size();

			for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
				String argument = this.arguments.get(argumentIndex);
				argumentsBuilder.append(argument);
				if (argumentIndex < this.arguments.size() - 1) {
					argumentsBuilder.append(", ");
				}
			}
		}

		return argumentsBuilder.toString();
	}

	@Override
	public void setTypeSignature(String typeSignature) {
		this.arguments.add(typeSignature);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}

		((ChangeLogSignatureNodeBase)parent).setTypeSignature(this.getArguments());
	}
}

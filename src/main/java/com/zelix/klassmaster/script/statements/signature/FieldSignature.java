package com.zelix.klassmaster.script.statements.signature;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.FieldSpecifierAcceptor;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import com.zelix.klassmaster.script.statements.specifier.ComplexFieldSpecifier;
import java.io.IOException;

public class FieldSignature extends AbstractScriptNode implements NameAcceptor {
	private String fieldTypeDescriptor;
	private ComplexFieldSpecifier fieldSpecifier;

	public FieldSignature(int nodeType) {
		super(nodeType);
	}

	public void setFieldSpecifier(ComplexFieldSpecifier fieldSpecifier) {
		this.fieldSpecifier = fieldSpecifier;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		FieldSpecifierAcceptor fieldSpecifierAcceptor = (FieldSpecifierAcceptor)parent;
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		if (this.fieldSpecifier != null) {
			fieldSpecifierAcceptor.setFieldSpecifier(this.fieldSpecifier);
		}

		if (this.fieldTypeDescriptor != null) {
			fieldSpecifierAcceptor.setFieldTypeDescriptor(this.fieldTypeDescriptor);
		}
	}

	@Override
	public void addName(String name) {
		this.fieldTypeDescriptor = name;
	}
}

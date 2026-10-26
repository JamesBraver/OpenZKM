package com.zelix.klassmaster.script.statements.clause;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class ExtendsClause extends AbstractScriptNode implements NameAcceptor {
	private String extendsClassName;

	public ExtendsClause(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		RenameFilterParameter renameFilterParameter = (RenameFilterParameter)parent;
		if (this.extendsClassName != null) {
			renameFilterParameter.setExtendsClassName(this.extendsClassName);
		}

		if (childCount > 1) {
			for (int annotationChildIndex = 0; annotationChildIndex < childCount; annotationChildIndex++) {
				ScriptNode childNode = this.getChild(annotationChildIndex);
				if (childNode instanceof ComplexAnnotationSpecifier) {
					renameFilterParameter.setExtendsAnnotationSpecifier((ComplexAnnotationSpecifier)childNode);
				}
			}
		}
	}

	@Override
	public void addName(String name) {
		this.extendsClassName = name;
	}
}

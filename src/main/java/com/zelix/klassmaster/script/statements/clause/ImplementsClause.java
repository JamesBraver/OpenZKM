package com.zelix.klassmaster.script.statements.clause;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import com.zelix.klassmaster.script.statements.name.QualifiedClassName;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;
import java.util.ArrayList;

public class ImplementsClause extends AbstractScriptNode implements NameAcceptor {
	public ArrayList<String> interfaceNames = new ArrayList<>();

	public ImplementsClause(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		RenameFilterParameter renameFilterParameter = (RenameFilterParameter)parent;

		for (int interfaceIndex = 0; interfaceIndex < this.interfaceNames.size(); interfaceIndex++) {
			renameFilterParameter.addImplementsInterface(this.interfaceNames.get(interfaceIndex));
		}

		boolean annotationSpecifierSeen = false;

		for (int annotationChildIndex = 0; annotationChildIndex < childCount; annotationChildIndex++) {
			ScriptNode childNode = this.getChild(annotationChildIndex);
			if (childNode instanceof ComplexAnnotationSpecifier) {
				renameFilterParameter.addImplementsAnnotationSpecifier((ComplexAnnotationSpecifier)childNode);
				annotationSpecifierSeen = true;
			} else if (childNode instanceof QualifiedClassName) {
				if (!annotationSpecifierSeen) {
					renameFilterParameter.addImplementsAnnotationSpecifier(null);
				}

				annotationSpecifierSeen = false;
			}
		}
	}

	@Override
	public void addName(String name) {
		this.interfaceNames.add(name);
	}
}

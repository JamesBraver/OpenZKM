package com.zelix.klassmaster.script.statements.annotation;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public class ReferencingAnnotation extends AbstractScriptNode implements NameAcceptor {
	private String annotationName;

	public ReferencingAnnotation(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		this.getChildrenCount();
		this.getChild(0).parseArguments(this, scriptContext);
		RenameFilterParameter referencingFilterParameter = (RenameFilterParameter)parent;
		if (this.annotationName != null) {
			referencingFilterParameter.setSearchByAnnotationName(this.annotationName);
		}
	}

	@Override
	public void addName(String name) {
		this.annotationName = name;
	}
}

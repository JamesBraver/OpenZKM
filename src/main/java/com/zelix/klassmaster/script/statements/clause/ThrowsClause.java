package com.zelix.klassmaster.script.statements.clause;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.base.GeneralMethodSignature;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import java.io.IOException;
import java.util.Vector;

public class ThrowsClause extends AbstractScriptNode implements NameAcceptor {
	public Vector<String> throwsExceptionNames = new Vector<>();

	public ThrowsClause(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptContext);
		}

		GeneralMethodSignature methodSignature = (GeneralMethodSignature)parent;

		for (int exceptionIndex = 0; exceptionIndex < this.throwsExceptionNames.size(); exceptionIndex++) {
			methodSignature.addThrowsExceptionName(this.throwsExceptionNames.elementAt(exceptionIndex));
		}
	}

	@Override
	public void addName(String name) {
		this.throwsExceptionNames.addElement(name);
	}
}

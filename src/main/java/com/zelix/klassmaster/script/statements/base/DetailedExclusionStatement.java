package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.clause.ContainedInClause;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import java.io.IOException;

public abstract class DetailedExclusionStatement extends ExclusionInclusionStatement {
	public DetailedExclusionStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public void collectFilterParameters(int childCount, ScriptContext scriptContext) throws IOException {
		ContainedInClause containedInClause = null;

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			ScriptNode childNode = this.getChild(childIndex);
			if (childNode instanceof ContainedInClause) {
				containedInClause = (ContainedInClause)childNode;
			} else {
				RenameFilterParameter filterParameter = (RenameFilterParameter)childNode;
				if (containedInClause != null) {
					filterParameter.setContainedInClause(containedInClause);
					containedInClause = null;
				}

				super.filterParameters.add(filterParameter);
			}

			childNode.parseArguments(this, scriptContext);
		}
	}
}

package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.NameAcceptor;
import java.io.IOException;

public class ClassInitializationOrder extends AbstractScriptNode implements NameAcceptor {
	private String beforeClass;
	private String afterClass;

	public ClassInitializationOrder(int nodeType) {
		super(nodeType);
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		int childCount = this.getChildrenCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).parseArguments(this, scriptContext);
		}

		((ClassInitializationOrderStatement)parent).addInitializationOrder(this.beforeClass, this.afterClass);
	}

	@Override
	public void addName(String name) {
		if (this.beforeClass == null) {
			this.beforeClass = name;
		} else {
			this.afterClass = name;
		}
	}
}

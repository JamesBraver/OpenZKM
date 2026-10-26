package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import java.util.ArrayList;
import java.util.List;

public abstract class ChangeLogFlowObfuscationNode extends ChangeLogEntryNodeBase {
	public List<String> nameList = new ArrayList<>();
	public String className;

	public ChangeLogFlowObfuscationNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public final void setClassName(String className) {
		this.className = className;
	}

	@Override
	protected final void prepareParse(ChangeLogsNode parent, AbstractChangeLogContext context, int childCount) {
	}

	public final void addName(String name) {
		this.nameList.add(name);
	}
}

package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.ChangeLogsTokenConstants;

public abstract class ChangeLogsNodeBase implements ChangeLogsNode {
	protected int nodeType;
	protected ChangeLogsNode[] children;
	protected ChangeLogsNode parent;

	public ChangeLogsNodeBase(int nodeKindIndex) {
		this.nodeType = nodeKindIndex;
	}

	public String getNodeTypeName() {
		return ChangeLogsTokenConstants.nodeTypeNames[this.nodeType];
	}

	@Override
	public void setParent(ChangeLogsNode parent) {
		this.parent = parent;
	}

	@Override
	public void close() {
	}

	@Override
	public void dispose() {
		if (this.children != null) {
			for (int i = 0; i < this.children.length; i++) {
				this.children[i].dispose();
				this.children[i] = null;
			}

			this.children = null;
		}
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		throw new Error("Node class needed for or interpret() not defined for " + this.getNodeTypeName());
	}

	@Override
	public ChangeLogsNode getChild(int index) {
		return this.children[index];
	}

	@Override
	public int getChildCount() {
		return this.children == null ? 0 : this.children.length;
	}

	@Override
	public void open() {
	}

	@Override
	public void addChild(ChangeLogsNode child, int index) {
		if (this.children == null) {
			this.children = new ChangeLogsNode[index + 1];
		} else if (index >= this.children.length) {
			ChangeLogsNode[] newChildren = new ChangeLogsNode[index + 1];
			System.arraycopy(this.children, 0, newChildren, 0, this.children.length);
			this.children = newChildren;
		}

		this.children[index] = child;
	}
}

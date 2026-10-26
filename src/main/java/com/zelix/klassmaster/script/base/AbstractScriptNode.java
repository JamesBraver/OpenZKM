package com.zelix.klassmaster.script.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ZKMScriptNodeTypes;
import com.zelix.klassmaster.script.statements.base.ScriptCommand;
import com.zelix.utils.GlobalUtils;
import java.io.IOException;

public abstract class AbstractScriptNode implements ScriptNode {
	protected static String NEGATED_MODIFIER_MARK = "!";
	protected int nodeType;
	protected ScriptNode parent;
	protected ScriptNode[] children;

	public AbstractScriptNode(int nodeType) {
		this.nodeType = nodeType;
	}

	public static String getTimestampPrefix() {
		return "[" + GlobalUtils.getCurrentTime() + "]";
	}

	@Override
	public int getChildrenCount() {
		return this.children == null ? 0 : this.children.length;
	}

	@Override
	public void parseArguments(ScriptNode parent, ScriptContext scriptContext) throws IOException {
		throw new Error("Node class needed or interpret() not defined for " + this.getNodeTypeName());
	}

	@Override
	public void setChild(ScriptNode child, int index) {
		if (this.children == null) {
			this.children = new ScriptNode[index + 1];
		} else if (index >= this.children.length) {
			ScriptNode[] expandedChildren = new ScriptNode[index + 1];
			System.arraycopy(this.children, 0, expandedChildren, 0, this.children.length);
			this.children = expandedChildren;
		}

		this.children[index] = child;
	}

	@Override
	public void setParent(ScriptNode parent) {
		this.parent = parent;
	}

	@Override
	public ScriptNode getParent() {
		return this.parent;
	}

	@Override
	public ScriptCommand findScriptCommand() {
		return this.parent.findScriptCommand();
	}

	public String getNodeTypeName() {
		return ZKMScriptNodeTypes.nodeTypeNames[this.nodeType];
	}

	@Override
	public void onParseComplete() {
	}

	@Override
	public void onParseStart() {
	}

	@Override
	public ScriptNode getChild(int index) {
		return this.children[index];
	}
}

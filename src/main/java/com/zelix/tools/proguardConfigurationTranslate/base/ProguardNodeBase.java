package com.zelix.tools.proguardConfigurationTranslate.base;

import com.zelix.tools.proguardConfigurationTranslate.ProguardNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;

public class ProguardNodeBase implements ProguardNode {
	protected int commandId;
	protected ProguardNode parent;
	protected ProguardNode[] children;

	public ProguardNodeBase(int commandId) {
		this.commandId = commandId;
	}

	@Override
	public int getChildrenCount() {
		return this.children == null ? 0 : this.children.length;
	}

	@Override
	public void setParent(ProguardNode parent) {
		this.parent = parent;
	}

	@Override
	public ProguardNode getParent() {
		return this.parent;
	}

	@Override
	public void onParseStart() {
	}

	@Override
	public ProguardNode getChild(int index) {
		return this.children[index];
	}

	@Override
	public void parseArguments(ProguardNode parent, ProguardScriptTranslator scriptTranslator) {
		int childCount = this.getChildrenCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).parseArguments(this, scriptTranslator);
		}
	}

	@Override
	public void setChild(ProguardNode child, int index) {
		if (this.children == null) {
			this.children = new ProguardNode[index + 1];
		} else if (index >= this.children.length) {
			ProguardNode[] newChildren = new ProguardNode[index + 1];
			System.arraycopy(this.children, 0, newChildren, 0, this.children.length);
			this.children = newChildren;
		}

		this.children[index] = child;
	}

	@Override
	public void onParseComplete() {
	}
}

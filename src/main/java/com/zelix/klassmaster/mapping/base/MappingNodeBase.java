package com.zelix.klassmaster.mapping.base;

import com.zelix.klassmaster.mapping.javaccInterface.MappingConstants;
import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import com.zelix.tools.proguardConfigurationTranslate.ProguardTranslateWriter;

public abstract class MappingNodeBase implements MappingNode {
	protected int nodeType;
	protected MappingNode[] children;
	protected MappingNode parent;

	public MappingNodeBase(int nodeType) {
		this.nodeType = nodeType;
	}

	@Override
	public void close() {
	}

	@Override
	public void open() {
	}

	@Override
	public void setParent(MappingNode parent) {
		this.parent = parent;
	}

	@Override
	public void addChild(MappingNode child, int index) {
		if (this.children == null) {
			this.children = new MappingNode[index + 1];
		} else if (index >= this.children.length) {
			MappingNode[] grownChildren = new MappingNode[index + 1];
			System.arraycopy(this.children, 0, grownChildren, 0, this.children.length);
			this.children = grownChildren;
		}

		this.children[index] = child;
	}

	@Override
	public MappingNode getChild(int index) {
		return this.children[index];
	}

	@Override
	public String toString() {
		return MappingConstants.nodeNames[this.nodeType];
	}

	@Override
	public int getChildCount() {
		return this.children == null ? 0 : this.children.length;
	}

	@Override
	public void interpret(MappingNode parent, ProguardTranslateWriter writer) {
		int childCount = this.getChildCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).interpret(this, writer);
		}
	}
}

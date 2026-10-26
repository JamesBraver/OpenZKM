package com.zelix.ui.dataset;

import com.zelix.framework.AbstractClassNode;

public class ClassNodeEntry {
	private final AbstractClassNode classNode;
	private final boolean mainClass;

	public ClassNodeEntry(AbstractClassNode classNode, boolean mainClass) {
		this.classNode = classNode;
		this.mainClass = mainClass;
	}

	public boolean isMainClass() {
		return this.mainClass;
	}

	public AbstractClassNode getClassNode() {
		return this.classNode;
	}

	public String getBinaryName() {
		return this.classNode.getBinaryName();
	}

	@Override
	public String toString() {
		return this.getBinaryName();
	}
}

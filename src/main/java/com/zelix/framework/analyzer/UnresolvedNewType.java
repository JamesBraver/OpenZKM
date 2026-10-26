package com.zelix.framework.analyzer;

import com.zelix.framework.instructions.NewNode;

public class UnresolvedNewType extends UnresolvedType {
	public NewNode newInstruction;

	public UnresolvedNewType(String descriptor, NewNode newInstruction) {
		super(descriptor);
		this.newInstruction = newInstruction;
	}

	public NewNode getNewInstruction() {
		return this.newInstruction;
	}

	@Override
	public boolean isUninitialized() {
		return false;
	}
}

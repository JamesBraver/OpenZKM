package com.zelix.framework.analyzer;

public class UnresolvedType extends StackFrameType {
	public UnresolvedType(String descriptor) {
		super(descriptor);
	}

	@Override
	public final boolean isResolved() {
		return false;
	}
}

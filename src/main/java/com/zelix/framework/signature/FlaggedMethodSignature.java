package com.zelix.framework.signature;

public class FlaggedMethodSignature extends MethodSignature {
	private final boolean flag;

	public FlaggedMethodSignature(String name, String parameterDescriptor, String returnDescriptor, boolean flag) {
		super(name, parameterDescriptor, returnDescriptor);
		this.flag = flag;
	}

	public boolean getFlag() {
		return this.flag;
	}
}

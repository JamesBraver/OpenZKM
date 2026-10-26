package com.zelix.tools.retrace;

import com.zelix.framework.wrappers.MethodNode;

public class StacktraceMethodInfo {
	public String nameAndDescriptor;
	public MethodNode method;

	public StacktraceMethodInfo(String nameAndDescriptor, MethodNode method) {
		this.nameAndDescriptor = nameAndDescriptor;
		this.method = method;
	}
}

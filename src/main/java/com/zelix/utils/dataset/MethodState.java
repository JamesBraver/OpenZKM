package com.zelix.utils.dataset;

import com.zelix.framework.signature.AbstractMethodKey;

public class MethodState {
	public final AbstractMethodKey methodInfo;
	public final int state;

	public MethodState(int state) {
		this.methodInfo = null;
		this.state = state;
	}

	public MethodState(AbstractMethodKey methodInfo) {
		this.methodInfo = methodInfo;
		this.state = 0;
	}
}

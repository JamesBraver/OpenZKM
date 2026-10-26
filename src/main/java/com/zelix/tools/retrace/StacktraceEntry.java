package com.zelix.tools.retrace;

import com.zelix.framework.wrappers.MethodNode;

public class StacktraceEntry {
	private boolean traceBreak = false;
	private StacktraceClassInfo classInfo;
	private MethodNode method;
	private int hash;

	public StacktraceEntry(StacktraceClassInfo classInfo, MethodNode method, boolean traceBreak) {
		this.classInfo = classInfo;
		this.method = method;
		this.traceBreak = traceBreak;
		this.hash = classInfo.getLine().hashCode() ^ method.getDisplayNameWithDescriptor().hashCode() ^ method.getInternalName().hashCode();
	}

	public StacktraceEntry(StacktraceClassInfo classInfo, boolean traceBreak) {
		this.classInfo = classInfo;
		this.method = null;
		this.traceBreak = traceBreak;
		this.hash = classInfo.getLine().hashCode();
	}

	public MethodNode getMethod() {
		return this.method;
	}

	public boolean isTraceBreak() {
		return this.traceBreak;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StacktraceEntry)) {
			return false;
		} else {
			StacktraceEntry var9 = (StacktraceEntry)obj;
			return this.hash == var9.hash && (this.classInfo == null && var9.classInfo == null || this.classInfo != null && var9.classInfo != null && this.classInfo.getLine().equals(var9.classInfo.getLine()))
				? this.method == null && var9.method == null || this.method != null && var9.method != null && this.method.isSameMethodKey(var9.method)
				: false;
		}
	}

	@Override
	public int hashCode() {
		return this.hash;
	}

	public StacktraceClassInfo getClassInfo() {
		return this.classInfo;
	}
}

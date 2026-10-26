package com.zelix.tools.retrace;

import com.zelix.framework.wrappers.MethodNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class Stacktrace {
	private List<StacktraceEntry> entries = new ArrayList<>();

	public void addUnresolvedEntry(StacktraceClassInfo classInfo) {
		this.addUnresolvedEntry(classInfo, false);
	}

	@Override
	public Object clone() {
		Stacktrace clonedStacktrace = new Stacktrace();
		clonedStacktrace.entries = new ArrayList<>(this.entries);
		return clonedStacktrace;
	}

	public void addResolvedEntry(StacktraceClassInfo classInfo, MethodNode method, boolean traceBreak) {
		StacktraceEntry entry = new StacktraceEntry(classInfo, method, traceBreak);
		this.entries.add(entry);
	}

	public Enumeration<StacktraceEntry> getEntries() {
		return Collections.enumeration(this.entries);
	}

	public MethodNode getCurrentMethod() {
		return this.entries.size() > 0 ? this.entries.get(this.entries.size() - 1).getMethod() : null;
	}

	public void addUnresolvedEntry(StacktraceClassInfo classInfo, boolean traceBreak) {
		StacktraceEntry entry = new StacktraceEntry(classInfo, traceBreak);
		this.entries.add(entry);
	}

	public void addResolvedEntry(StacktraceClassInfo classInfo, MethodNode method) {
		this.addResolvedEntry(classInfo, method, false);
	}
}

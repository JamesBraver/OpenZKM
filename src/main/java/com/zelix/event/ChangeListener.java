package com.zelix.event;

public interface ChangeListener {
	void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg);
}

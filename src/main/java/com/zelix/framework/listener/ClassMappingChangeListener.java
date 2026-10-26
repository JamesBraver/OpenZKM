package com.zelix.framework.listener;

import com.zelix.event.INotifier;

public interface ClassMappingChangeListener {
	void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg);
}

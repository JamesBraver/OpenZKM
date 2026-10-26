package com.zelix.framework.listener;

import com.zelix.event.INotifier;
import com.zelix.framework.writer.base.TypeWriter;

public interface ConstantPoolChangeListener {
	TypeWriter getConstantPoolEntry(int constantPoolIndex);

	void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg);
}

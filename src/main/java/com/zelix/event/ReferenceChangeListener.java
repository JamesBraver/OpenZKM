package com.zelix.event;

public interface ReferenceChangeListener {
	void onChange(INotifier notifier, Object changeCode, Object changedObject, Object unused);
}

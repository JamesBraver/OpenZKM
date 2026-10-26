package com.zelix.event;

public interface ResourceChangedListener {
	void onChanged(INotifier notifier, Object changeCode, Object changedObject, Object unused);
}

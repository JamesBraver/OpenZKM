package com.zelix.event;

public interface INotifier {
	void markAndNotify();

	void markChanged();

	void subscribe(ChangeListener listener);

	void onChange(ChangeListener listener);

	void notify(Object changeCode, Object changedObject, Object unused);

	void unsubscribe(ChangeListener listener);

	void onChange(ChangeListener listener, Object changeCode, Object changedObject, Object unused);

	void notifySimpleChange();
}

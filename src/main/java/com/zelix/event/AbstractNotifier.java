package com.zelix.event;

import java.util.List;

public abstract class AbstractNotifier implements INotifier {
	public List<ChangeListener> listeners;

	@Override
	public final void markAndNotify() {
		this.markChanged();
		this.notifySimpleChange();
	}

	@Override
	public synchronized void markChanged() {
	}

	@Override
	public abstract void subscribe(ChangeListener listener);

	@Override
	public final void onChange(ChangeListener listener) {
		this.onChange(listener, null, null, null);
	}

	@Override
	public abstract void notify(Object changeCode, Object changedObject, Object unused);

	@Override
	public abstract void unsubscribe(ChangeListener listener);

	@Override
	public final void onChange(ChangeListener listener, Object changeCode, Object changedObject, Object unused) {
		listener.onChange(this, changeCode, changedObject, unused);
	}

	@Override
	public void notifySimpleChange() {
		this.notify(null, null, null);
	}
}

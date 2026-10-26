package com.zelix.event;

import java.util.ArrayList;

public class AtomicNotifier extends AbstractNotifier {
	public static final String USER_DIRECTORY = System.getProperty("user.dir");

	@Override
	public final synchronized void subscribe(ChangeListener listener) {
		if (this.listeners == null) {
			this.listeners = new ArrayList<>(2);
		}

		if (!this.listeners.contains(listener)) {
			this.listeners.add(listener);
		}
	}

	@Override
	public void notify(Object changeCode, Object changedObject, Object unused) {
		int listenersCount;
		ChangeListener[] copiedListeners;
		synchronized (this) {
			if (this.listeners == null) {
				return;
			}

			listenersCount = this.listeners.size();
			copiedListeners = this.listeners.toArray(new ChangeListener[0]);
		}

		for (int i = listenersCount - 1; i >= 0; i--) {
			if (copiedListeners[i] != null) {
				copiedListeners[i].onChange(this, changeCode, changedObject, unused);
			}
		}
	}

	@Override
	public synchronized void unsubscribe(ChangeListener listener) {
		if (this.listeners != null) {
			this.listeners.remove(listener);
			if (this.listeners.isEmpty()) {
				this.listeners = null;
			}
		}
	}

	@Override
	public void notifySimpleChange() {
		this.notify(null, null, null);
	}
}

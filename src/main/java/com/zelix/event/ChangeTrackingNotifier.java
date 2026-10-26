package com.zelix.event;

public class ChangeTrackingNotifier extends AtomicNotifier {
	public static final String DEFAULT_CLASS_PATH = System.getProperty("java.class.path");
	public boolean changed = false;
	private ChangeListener[] copiedListeners;

	public synchronized boolean isChanged() {
		return this.changed;
	}

	protected synchronized void unchanged() {
		this.changed = false;
	}

	@Override
	public synchronized void markChanged() {
		this.changed = true;
	}

	@Override
	public final void notify(Object changeCode, Object changedObject, Object unused) {
		int listenersCount;
		synchronized (this) {
			if (!this.isChanged() || this.listeners == null) {
				return;
			}

			listenersCount = this.listeners.size();
			if (this.copiedListeners == null) {
				this.copiedListeners = new ChangeListener[listenersCount];
			} else if (listenersCount > this.copiedListeners.length) {
				this.copiedListeners = new ChangeListener[listenersCount];
			}

			this.copiedListeners = this.listeners.toArray(this.copiedListeners);
			this.unchanged();
		}

		for (int listenerIndex = listenersCount - 1; listenerIndex >= 0; listenerIndex--) {
			if (this.copiedListeners[listenerIndex] != null) {
				this.copiedListeners[listenerIndex].onChange(this, changeCode, changedObject, unused);
			}
		}
	}
}

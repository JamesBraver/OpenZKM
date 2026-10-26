package com.zelix.framework.writer.base;

import com.zelix.event.ChangeListener;
import com.zelix.event.INotifier;
import com.zelix.framework.constant.ConstantNode;
import java.util.ArrayList;
import java.util.List;

public abstract class ConstantWriter extends TypeWriter implements INotifier, ConstantNode {
	public List<ChangeListener> changeListeners;

	public ConstantWriter(int tag, AbstractConstantPoolWriter constantPool) {
		super(tag, constantPool);
	}

	@Override
	public final void markAndNotify() {
		this.markChanged();
		this.notifySimpleChange();
	}

	@Override
	public synchronized void markChanged() {
	}

	@Override
	public final synchronized void subscribe(ChangeListener listener) {
		if (this.changeListeners == null) {
			this.changeListeners = new ArrayList<>(2);
		}

		if (!this.changeListeners.contains(listener)) {
			this.changeListeners.add(listener);
		}
	}

	@Override
	public final void onChange(ChangeListener listener) {
		this.onChange(listener, null, null, null);
	}

	@Override
	public void notify(Object changeCode, Object changedObject, Object unused) {
		int listenerCount;
		ChangeListener[] listeners;
		synchronized (this) {
			if (this.changeListeners == null) {
				return;
			}

			listenerCount = this.changeListeners.size();
			listeners = new ChangeListener[listenerCount];
			listeners = this.changeListeners.toArray(listeners);
		}

		for (int i = listenerCount - 1; i >= 0; i--) {
			if (listeners[i] != null) {
				listeners[i].onChange(this, changeCode, changedObject, unused);
			}
		}
	}

	@Override
	public synchronized void unsubscribe(ChangeListener listener) {
		if (this.changeListeners != null) {
			this.changeListeners.remove(listener);
		}
	}

	@Override
	public final void onChange(ChangeListener listener, Object changeCode, Object changedObject, Object unused) {
		listener.onChange(this, changeCode, changedObject, unused);
	}

	@Override
	public void notifySimpleChange() {
		this.notify(null, null, null);
	}
}

package com.zelix.event;

import javax.swing.SwingUtilities;

public class ResourceChangeDispatcher {
	public ResourceChangeDispatcher(ResourceChangedListener listener, INotifier notifier, Object changeCode, Object changedObject, Object unused) {
		if (SwingUtilities.isEventDispatchThread()) {
			this.notifyChanged(listener, notifier, changeCode, changedObject, unused);
		} else {
			SwingUtilities.invokeLater(new ResourceChangeDispatcher.ChangeNotifier(this, listener, notifier, changeCode, changedObject, unused));
		}
	}

	public final void notifyChanged(ResourceChangedListener listener, INotifier notifier, Object changeCode, Object changedObject, Object unused) {
		listener.onChanged(notifier, changeCode, changedObject, unused);
	}

	public static class ChangeNotifier implements Runnable {
		private ResourceChangeDispatcher changeListener;
		private ResourceChangedListener changedListener;
		private INotifier notifier;
		private Object changeCode;
		private Object changedObject;
		private Object unused;

		public ChangeNotifier(
			ResourceChangeDispatcher changeListener, ResourceChangedListener changedListener, INotifier notifier, Object changeCode, Object changedObject, Object unused
		) {
			this.changeListener = changeListener;
			this.changedListener = changedListener;
			this.notifier = notifier;
			this.changeCode = changeCode;
			this.changedObject = changedObject;
			this.unused = unused;
		}

		@Override
		public void run() {
			this.changeListener.notifyChanged(this.changedListener, this.notifier, this.changeCode, this.changedObject, this.unused);
		}
	}
}

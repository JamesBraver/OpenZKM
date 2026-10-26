package com.zelix.event;

import com.zelix.utils.references.AtomicReference;

public class ObservableReference extends AtomicReference<Object> implements ChangeListener {
	public void updateSubscription(AbstractNotifier notifier) {
		if (notifier != this.value) {
			if (this.value != null) {
				((AbstractNotifier)this.value).unsubscribe(this);
			}

			super.set(notifier);
			if (notifier != null) {
				notifier.subscribe(this);
			}
		}
	}

	@Override
	public void set(Object value) {
		this.setNotifier((AbstractNotifier)value);
	}

	public AbstractNotifier getNotifier() {
		return (AbstractNotifier)super.get();
	}

	public void setNotifier(AbstractNotifier notifier) {
		this.updateSubscription(notifier);
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		this.markChanged();
		this.notify(changeKind, changedNode, changeArg);
	}
}

package com.zelix.utils.references;

import com.zelix.event.ChangeTrackingNotifier;

public class AtomicReference<T> extends ChangeTrackingNotifier {
	protected T value;

	public AtomicReference(T value) {
		this.set(value);
	}

	public AtomicReference() {
		this.set(null);
	}

	public boolean isNull() {
		return this.value == null;
	}

	public void set(T value) {
		this.value = value;
		this.markChanged();
		this.notify(value, null, null);
	}

	public AtomicReference<T> setNull() {
		this.set(null);
		return this;
	}

	public T get() {
		return this.value;
	}
}

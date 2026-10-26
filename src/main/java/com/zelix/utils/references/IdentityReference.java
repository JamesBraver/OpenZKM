package com.zelix.utils.references;

public class IdentityReference extends AtomicReference<Object> {
	public IdentityReference() {
		super(null);
	}

	public IdentityReference(Object value) {
		super(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IdentityReference)) {
			return false;
		} else {
			return this.value != null ? this.value == ((IdentityReference)obj).get() : obj == null;
		}
	}

	@Override
	public int hashCode() {
		return this.value != null ? System.identityHashCode(this.value) : 0;
	}
}

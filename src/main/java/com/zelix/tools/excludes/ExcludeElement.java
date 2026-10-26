package com.zelix.tools.excludes;

import java.io.Serializable;

public class ExcludeElement implements Serializable {
	private Object linkedClass;
	private Object prefix;
	private Object suffix;

	public ExcludeElement(Object linkedClass, Object prefix, Object suffix) {
		this.linkedClass = linkedClass;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public Object getPrefix() {
		return this.prefix;
	}

	public Object getLinkedClass() {
		return this.linkedClass;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExcludeElement)) {
			return false;
		} else {
			ExcludeElement other = (ExcludeElement)obj;
			return (this.linkedClass == null && other.linkedClass == null || this.linkedClass != null && other.linkedClass != null && this.linkedClass.equals(other.linkedClass))
				&& (this.prefix == null && other.prefix == null || this.prefix != null && other.prefix != null && this.prefix.equals(other.prefix))
				&& (this.suffix == null && other.suffix == null || this.suffix != null && other.suffix != null && this.suffix.equals(other.suffix));
		}
	}

	@Override
	public int hashCode() {
		int hash = 0;
		if (this.linkedClass != null) {
			hash = 0 ^ this.linkedClass.hashCode();
		}

		if (this.prefix != null) {
			hash ^= this.prefix.hashCode();
		}

		if (this.suffix != null) {
			hash ^= this.suffix.hashCode();
		}

		return hash;
	}

	public Object getSuffix() {
		return this.suffix;
	}
}

package com.zelix.klassmaster.changelogs;

public class FieldKey extends AbstractMemberKey implements Comparable<FieldKey> {
	public FieldKey(String name, String descriptor, int accessFlags) {
		super(name, descriptor, accessFlags);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FieldKey)) {
			return false;
		} else {
			FieldKey other = (FieldKey)obj;
			return this.name.equals(other.name)
				&& (this.descriptor == null && other.descriptor == null || this.descriptor != null && other.descriptor != null && this.descriptor.equals(other.descriptor));
		}
	}

	@Override
	public int hashCode() {
		return this.descriptor == null ? this.name.hashCode() : this.name.hashCode() ^ this.descriptor.hashCode();
	}

	public final int compareTo(FieldKey o) {
		return this.name.compareTo(o.name);
	}
}

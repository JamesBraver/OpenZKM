package com.zelix.klassmaster.changelogs;

public class MethodKey extends AbstractMemberKey implements Comparable<MethodKey> {
	private final String parameterDescriptor;

	public MethodKey(String name, String parameterDescriptor, String descriptor, int accessFlags) {
		super(name, descriptor, accessFlags);
		this.parameterDescriptor = parameterDescriptor;
	}

	public String getParameterDescriptor() {
		return this.parameterDescriptor;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MethodKey)) {
			return false;
		} else {
			MethodKey other = (MethodKey)obj;
			return this.name.equals(other.name)
				&& this.parameterDescriptor.equals(other.parameterDescriptor)
				&& (this.descriptor == null && other.descriptor == null || this.descriptor != null && other.descriptor != null && this.descriptor.equals(other.descriptor));
		}
	}

	@Override
	public int hashCode() {
		return this.descriptor == null
			? this.name.hashCode() ^ this.parameterDescriptor.hashCode()
			: this.name.hashCode() ^ this.parameterDescriptor.hashCode() ^ this.descriptor.hashCode();
	}

	public final int compareTo(MethodKey o) {
		int comparison = this.name.compareTo(o.name);
		if (comparison != 0) {
			return comparison;
		} else {
			comparison = this.parameterDescriptor.compareTo(o.parameterDescriptor);
			return comparison == 0 && this.descriptor != null && o.descriptor != null ? this.descriptor.compareTo(o.descriptor) : comparison;
		}
	}
}

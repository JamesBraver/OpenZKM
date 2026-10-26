package com.zelix.framework.signature;

public class MethodParameterKey extends AbstractMethodKey {
	private final String returnDescriptor;
	private final int cachedHashCode;

	public MethodParameterKey(String name, String descriptor, String returnDescriptor) {
		super(name, descriptor);
		if (returnDescriptor != null) {
			if (returnDescriptor.length() == 0) {
				this.returnDescriptor = null;
			} else {
				this.returnDescriptor = returnDescriptor.intern();
			}
		} else {
			this.returnDescriptor = null;
		}

		this.cachedHashCode = (name + descriptor).hashCode();
	}

	public MethodParameterKey(String name, String descriptor) {
		super(name, descriptor);
		String returnDescriptorPart = descriptor.substring(this.parameterDescriptor.length());
		if (returnDescriptorPart.length() == 0) {
			this.returnDescriptor = null;
		} else {
			this.returnDescriptor = returnDescriptorPart.intern();
		}

		this.cachedHashCode = (name + this.parameterDescriptor).hashCode();
	}

	public boolean hasReturnType() {
		return this.returnDescriptor != null;
	}

	@Override
	public final String getReturnDescriptor() {
		return this.returnDescriptor;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof MethodParameterKey) {
			MethodParameterKey other = (MethodParameterKey)obj;
			return this.cachedHashCode == other.cachedHashCode && this.name == other.name && this.parameterDescriptor == other.parameterDescriptor;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.cachedHashCode;
	}

	public boolean matchesSignature(MethodSignature methodSignature) {
		return this.equals(methodSignature.getParameterKey());
	}
}

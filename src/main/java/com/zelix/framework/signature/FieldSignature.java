package com.zelix.framework.signature;

import java.util.Map;

public class FieldSignature {
	public static FieldSignature SERIAL_VERSION_UID = new FieldSignature("serialVersionUID", "J");
	private final String name;
	private final String descriptor;
	private final int cachedHashCode;

	public FieldSignature(String name, String descriptor) {
		this.name = name;
		this.descriptor = descriptor;
		this.cachedHashCode = name.hashCode() ^ descriptor.hashCode();
	}

	private FieldSignature(String name, String descriptor, int cachedHashCode) {
		this.name = name;
		this.descriptor = descriptor;
		this.cachedHashCode = cachedHashCode;
	}

	public String getName() {
		return this.name;
	}

	public String getDescriptor() {
		return this.descriptor;
	}

	@Override
	public Object clone() {
		return new FieldSignature(this.name, this.descriptor, this.cachedHashCode);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FieldSignature)) {
			return false;
		} else {
			FieldSignature other = (FieldSignature)obj;
			return this.cachedHashCode == other.cachedHashCode && this.name.equals(other.name) && this.descriptor.equals(other.descriptor);
		}
	}

	@Override
	public int hashCode() {
		return this.cachedHashCode;
	}

	public String toRemappedString(Map<String, String> classNameMappings) {
		return MethodSignature.formatRemappedType(this.descriptor, classNameMappings) + " " + this.name;
	}
}

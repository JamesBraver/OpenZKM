package com.zelix.framework.signature;

public class MethodID {
	private final String ownerName;
	private final String methodName;
	private final String methodDescriptor;
	private final int cachedHash;

	public MethodID(String ownerName, String methodName, String methodDesc) {
		this.ownerName = ownerName;
		this.methodName = methodName;
		this.methodDescriptor = methodDesc;
		this.cachedHash = ownerName.hashCode() ^ methodName.hashCode() ^ methodDesc.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MethodID)) {
			return false;
		} else {
			MethodID other = (MethodID)obj;
			return this.cachedHash == other.cachedHash && this.ownerName.equals(other.ownerName) && this.methodName.equals(other.methodName) && this.methodDescriptor.equals(other.methodDescriptor);
		}
	}

	@Override
	public int hashCode() {
		return this.cachedHash;
	}
}

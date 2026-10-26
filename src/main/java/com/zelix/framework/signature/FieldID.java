package com.zelix.framework.signature;

public class FieldID {
	private final String ownerName;
	private final String fieldName;
	private final String fieldDescriptor;
	private final int cachedHash;

	public FieldID(String ownerName, String fieldName, String fieldDesc) {
		this.ownerName = ownerName;
		this.fieldName = fieldName;
		this.fieldDescriptor = fieldDesc;
		this.cachedHash = ownerName.hashCode() ^ fieldName.hashCode() ^ fieldDesc.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FieldID)) {
			return false;
		} else {
			FieldID other = (FieldID)obj;
			return this.cachedHash == other.cachedHash && this.ownerName.equals(other.ownerName) && this.fieldName.equals(other.fieldName) && this.fieldDescriptor.equals(other.fieldDescriptor);
		}
	}

	@Override
	public int hashCode() {
		return this.cachedHash;
	}
}

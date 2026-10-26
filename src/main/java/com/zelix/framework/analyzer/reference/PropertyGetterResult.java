package com.zelix.framework.analyzer.reference;

import java.util.HashSet;
import java.util.Iterator;

public class PropertyGetterResult implements Reference {
	private HashSet<Reference> keyProperties;
	private HashSet<Reference> defaultProperties;
	private HashSet<Reference> fileProperties;

	public PropertyGetterResult(HashSet<Reference> keyProperties, HashSet<Reference> defaultProperties, HashSet<Reference> fileProperties) {
		this.keyProperties = keyProperties;
		this.defaultProperties = defaultProperties;
		this.fileProperties = fileProperties;
	}

	public PropertyGetterResult(HashSet<Reference> keyProperties, HashSet<Reference> fileProperties) {
		this.keyProperties = keyProperties;
		this.fileProperties = fileProperties;
	}

	private boolean setsEqual(HashSet<Reference> set1, HashSet<Reference> set2) {
		if (set1 != null) {
			return set2 != null ? set1.equals(set2) : false;
		} else {
			return set2 == null;
		}
	}

	@Override
	public boolean isSingleValue() {
		return true;
	}

	@Override
	public boolean isResolved() {
		return false;
	}

	@Override
	public String getTypeName() {
		return this.getValue();
	}

	@Override
	public int getInstructionIndex() {
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PropertyGetterResult)) {
			return false;
		} else {
			PropertyGetterResult other = (PropertyGetterResult)obj;
			return this.setsEqual(this.keyProperties, other.keyProperties)
				&& this.setsEqual(this.defaultProperties, other.defaultProperties)
				&& this.setsEqual(this.fileProperties, other.fileProperties);
		}
	}

	@Override
	public int hashCode() {
		int hash = 0;
		if (this.keyProperties != null) {
			hash = this.keyProperties.hashCode();
		}

		if (this.defaultProperties != null) {
			hash ^= this.defaultProperties.hashCode();
		}

		if (this.fileProperties != null) {
			hash ^= this.fileProperties.hashCode();
		}

		return hash;
	}

	@Override
	public String getValue() {
		return null;
	}

	@Override
	public String getKey() {
		return this.getValue();
	}

	@Override
	public boolean isPending() {
		return false;
	}

	private String formatPropertySet(HashSet<Reference> properties) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		Iterator<Reference> referenceIterator = properties.iterator();

		while (referenceIterator.hasNext()) {
			Reference reference = referenceIterator.next();
			buffer.append(reference.getDisplayString());
			if (referenceIterator.hasNext()) {
				buffer.append(", ");
			}
		}

		buffer.append("]");
		return buffer.toString();
	}

	@Override
	public String getDisplayString() {
		return "<Properties key="
			+ this.formatPropertySet(this.keyProperties)
			+ (this.defaultProperties != null && this.defaultProperties.size() != 0 ? " default=" + this.formatPropertySet(this.defaultProperties) : "")
			+ ", file="
			+ this.formatPropertySet(this.fileProperties)
			+ ">";
	}
}

package com.zelix.framework.analyzer.reference;

import com.zelix.framework.writer.MethodDescriptorWriter;

public class MethodTypeReference implements Reference {
	public MethodDescriptorWriter descriptorWriter;

	public MethodTypeReference(MethodDescriptorWriter descriptorWriter) {
		if (descriptorWriter == null) {
			throw new IllegalArgumentException();
		} else {
			this.descriptorWriter = descriptorWriter;
		}
	}

	@Override
	public boolean isSingleValue() {
		return true;
	}

	@Override
	public boolean isResolved() {
		return true;
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
		if (obj instanceof MethodTypeReference) {
			MethodTypeReference other = (MethodTypeReference)obj;
			if (this.descriptorWriter != null) {
				return other.descriptorWriter != null ? this.descriptorWriter.equals(other.descriptorWriter) : false;
			} else {
				return other.descriptorWriter == null;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.descriptorWriter != null ? this.descriptorWriter.hashCode() : 0;
	}

	@Override
	public String getValue() {
		return this.descriptorWriter != null ? this.descriptorWriter.getDescriptor() : null;
	}

	@Override
	public String getKey() {
		return this.getValue();
	}

	@Override
	public boolean isPending() {
		return false;
	}

	@Override
	public String getDisplayString() {
		return this.descriptorWriter != null ? this.getTypeName() : "null";
	}
}

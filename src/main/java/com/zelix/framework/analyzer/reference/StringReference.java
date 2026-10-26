package com.zelix.framework.analyzer.reference;

import com.zelix.framework.constant.StringNode;

public class StringReference implements Reference {
	public StringNode stringNode;
	private boolean nameAndDescriptor;
	private String accessorPrefix;

	private StringReference(StringNode stringNode, boolean nameAndDescriptor, String accessorPrefix) {
		if (stringNode == null) {
			throw new IllegalArgumentException();
		} else {
			this.stringNode = stringNode;
			this.nameAndDescriptor = nameAndDescriptor;
			this.accessorPrefix = accessorPrefix;
		}
	}

	public StringReference(StringNode stringNode, String accessorPrefix) {
		this(stringNode, false, accessorPrefix);
	}

	public StringReference(StringNode stringNode) {
		this(stringNode, false, null);
	}

	public StringReference(StringNode stringNode, boolean nameAndDescriptor) {
		this(stringNode, nameAndDescriptor, null);
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
		String typeName = this.getValue();
		if (typeName == null) {
			return null;
		} else {
			int lastBracketIndex = typeName.lastIndexOf(91);
			if (lastBracketIndex > -1) {
				typeName = typeName.substring(lastBracketIndex + 1);
			}

			if (typeName.startsWith("L") && typeName.endsWith(";")) {
				typeName = typeName.substring(1, typeName.length() - 1);
			}

			return typeName;
		}
	}

	public final StringNode getStringNode() {
		return this.stringNode;
	}

	public boolean isNameAndDescriptor() {
		return this.nameAndDescriptor;
	}

	public boolean hasAccessorPrefix() {
		return this.accessorPrefix != null && this.accessorPrefix.length() > 0;
	}

	public String getAccessorPrefix() {
		return this.accessorPrefix;
	}

	@Override
	public int getInstructionIndex() {
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && this.getClass().equals(obj.getClass())) {
			StringReference other = (StringReference)obj;
			if (this.stringNode == null) {
				return other.stringNode == null;
			} else {
				return other.stringNode != null ? this.stringNode.equals(other.stringNode) : false;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.stringNode != null ? this.stringNode.hashCode() : 0;
	}

	@Override
	public final String getValue() {
		return this.stringNode != null ? this.stringNode.getDisplayValue() : null;
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
	public final String getDisplayString() {
		return this.stringNode != null ? this.getTypeName() : "null";
	}
}

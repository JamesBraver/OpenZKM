package com.zelix.framework.analyzer.reference;

import com.zelix.framework.constant.StringNode;

public class ClassTypeReference extends StringReference {
	public ClassTypeReference(StringNode stringNode) {
		super(stringNode);
	}

	@Override
	public String getTypeName() {
		String typeName = null;
		if (super.stringNode != null) {
			typeName = super.stringNode.getDisplayValue();
			int lastBracketIndex = typeName.lastIndexOf("[");
			if (lastBracketIndex > -1) {
				typeName = typeName.substring(lastBracketIndex + 1);
			}

			if (typeName.endsWith(";")) {
				typeName = typeName.substring(1, typeName.length() - 1);
			}
		}

		return typeName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && this.getClass().equals(obj.getClass())) {
			ClassTypeReference other = (ClassTypeReference)obj;
			if (super.stringNode == null) {
				return other.stringNode == null;
			} else {
				return other.stringNode != null ? super.stringNode.equals(other.stringNode) : false;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return super.stringNode != null ? super.stringNode.hashCode() + 1 : 0;
	}
}

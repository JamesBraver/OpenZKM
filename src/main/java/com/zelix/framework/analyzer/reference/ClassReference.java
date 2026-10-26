package com.zelix.framework.analyzer.reference;

import com.zelix.framework.writer.ClassInfoWriter;

public class ClassReference implements Reference {
	public ClassInfoWriter classInfoWriter;

	public ClassReference(ClassInfoWriter classInfoWriter) {
		if (classInfoWriter == null) {
			throw new IllegalArgumentException();
		} else {
			this.classInfoWriter = classInfoWriter;
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
		String typeName = this.getValue();
		if (typeName == null) {
			return null;
		} else {
			int lastBracketIndex = typeName.lastIndexOf(91);
			if (lastBracketIndex > -1) {
				typeName = typeName.substring(lastBracketIndex + 1);
			}

			if (typeName.endsWith(";")) {
				typeName = typeName.substring(1, typeName.length() - 1);
			}

			return typeName;
		}
	}

	@Override
	public int getInstructionIndex() {
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ClassReference) {
			ClassReference other = (ClassReference)obj;
			if (this.classInfoWriter != null) {
				return other.classInfoWriter != null ? this.classInfoWriter.equals(other.classInfoWriter) : false;
			} else {
				return other.classInfoWriter == null;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.classInfoWriter != null ? this.classInfoWriter.hashCode() : 0;
	}

	@Override
	public String getValue() {
		return this.classInfoWriter != null ? this.classInfoWriter.getInternalName() : null;
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
		return this.classInfoWriter != null ? this.getTypeName() : "null";
	}
}

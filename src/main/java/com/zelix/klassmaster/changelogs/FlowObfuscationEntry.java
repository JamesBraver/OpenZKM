package com.zelix.klassmaster.changelogs;

import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.references.AtomicReference;

public class FlowObfuscationEntry implements Comparable<FlowObfuscationEntry> {
	private final String originalClassName;
	private String originalFieldName;
	private String originalFieldDescriptor;
	private String originalSetterName;
	private String originalGetterName;
	private String originalBooleanGetterName;
	public boolean processingFlag;
	private String packageName;
	private String humanReadableFieldType;
	private String remappedClassName;
	private String remappedFieldName;
	private String remappedSetterName;
	private String remappedGetterName;
	private String remappedBooleanGetterName;

	public FlowObfuscationEntry(
		String originalClassName,
		String originalFieldName,
		String originalFieldDescriptor,
		String originalSetterName,
		String originalGetterName,
		String originalBooleanGetterName,
		boolean processingFlag,
		String packageName,
		ChangeLogContext changeLogContext,
		String lineType,
		AtomicReference<String> errorMessage
	) {
		errorMessage.setNull();
		this.originalClassName = originalClassName;
		this.originalFieldName = originalFieldName;
		this.originalFieldDescriptor = originalFieldDescriptor;
		this.originalSetterName = originalSetterName;
		this.originalGetterName = originalGetterName;
		this.originalBooleanGetterName = originalBooleanGetterName;
		this.processingFlag = processingFlag;
		this.packageName = packageName;
		if (originalFieldDescriptor != null) {
			this.humanReadableFieldType = TypeWriter.descriptorToHumanReadable(originalFieldDescriptor);
		}

		if (changeLogContext.hasOldClass(originalClassName)) {
			this.remappedClassName = changeLogContext.getNewClassName(originalClassName);
			if (this.remappedClassName == null) {
				this.remappedClassName = originalClassName;
			}
		} else {
			errorMessage.set("Class '" + originalClassName + "' appears in a '" + lineType + "' line but it is not present in the main body of the change log.");
		}

		if (originalFieldName != null) {
			if (changeLogContext.hasFieldMapping(originalClassName, originalFieldName, this.humanReadableFieldType)) {
				this.remappedFieldName = changeLogContext.getNewFieldName(originalClassName, originalFieldName, this.humanReadableFieldType);
				if (this.remappedFieldName == null) {
					this.remappedFieldName = originalFieldName;
				}
			}

			if (originalSetterName != null) {
				String[] setterParameterDescriptors = new String[]{this.humanReadableFieldType};
				if (changeLogContext.hasMethodMapping(originalClassName, originalSetterName, setterParameterDescriptors, "void")) {
					this.remappedSetterName = changeLogContext.getNewMethodName(originalClassName, originalSetterName, setterParameterDescriptors, "void");
					if (this.remappedSetterName == null) {
						this.remappedSetterName = originalSetterName;
					}
				} else {
					errorMessage.set(
						"Method 'void "
							+ originalSetterName
							+ '('
							+ this.humanReadableFieldType
							+ ")' in class '"
							+ originalClassName
							+ "' appears in a '"
							+ lineType
							+ "' line but it is not present in the main body of the change log. (1)"
					);
				}
			}

			String[] emptyParameterDescriptors = new String[0];
			if (originalGetterName != null) {
				if (changeLogContext.hasMethodMapping(originalClassName, originalGetterName, emptyParameterDescriptors, this.humanReadableFieldType)) {
					this.remappedGetterName = changeLogContext.getNewMethodName(originalClassName, originalGetterName, emptyParameterDescriptors, this.humanReadableFieldType);
					if (this.remappedGetterName == null) {
						this.remappedGetterName = originalGetterName;
					}
				} else {
					errorMessage.set(
						"Method '"
							+ this.humanReadableFieldType
							+ " "
							+ originalGetterName
							+ "()' in class '"
							+ originalClassName
							+ "' appears in a '"
							+ lineType
							+ "' line but it is not present in the main body of the change log. (2)"
					);
				}
			}

			if (originalBooleanGetterName != null) {
				if (changeLogContext.hasMethodMapping(originalClassName, originalBooleanGetterName, emptyParameterDescriptors, this.humanReadableFieldType)) {
					this.remappedBooleanGetterName = changeLogContext.getNewMethodName(originalClassName, originalBooleanGetterName, emptyParameterDescriptors, this.humanReadableFieldType);
					if (this.remappedBooleanGetterName == null) {
						this.remappedBooleanGetterName = originalBooleanGetterName;
					}
				} else {
					errorMessage.set(
						"Method '"
							+ this.humanReadableFieldType
							+ " "
							+ originalBooleanGetterName
							+ "()' in class '"
							+ originalClassName
							+ "' appears in a '"
							+ lineType
							+ "' line but it is not present in the main body of the change log. (3)"
					);
				}
			}
		}
	}

	public String getRemappedFieldName() {
		return this.remappedFieldName;
	}

	public String getRemappedSetterName() {
		return this.remappedSetterName;
	}

	public boolean getProcessingFlag() {
		return this.processingFlag;
	}

	public String getOriginalFieldName() {
		return this.originalFieldName;
	}

	public String getFieldTypeInternalName() {
		return this.originalFieldDescriptor.substring(2, this.originalFieldDescriptor.length() - 1);
	}

	public String getOriginalGetterName() {
		return this.originalGetterName;
	}

	public boolean isPartiallyMapped() {
		return this.remappedFieldName != null && this.originalSetterName == null
			|| this.remappedSetterName != null && this.originalGetterName == null
			|| this.remappedGetterName != null && this.originalBooleanGetterName == null
			|| this.remappedBooleanGetterName != null;
	}

	public void setProcessingFlag(boolean processingFlag) {
		this.processingFlag = processingFlag;
	}

	public String getRemappedClassName() {
		return this.remappedClassName;
	}

	public boolean isNonStringObjectField() {
		return this.originalFieldDescriptor != null && this.originalFieldDescriptor.endsWith(";") && !this.originalFieldDescriptor.endsWith("Ljava/lang/String;");
	}

	public void setOriginalBooleanGetterName(String originalBooleanGetterName) {
		this.originalBooleanGetterName = originalBooleanGetterName;
	}

	public String getOriginalSetterName() {
		return this.originalSetterName;
	}

	public boolean isClassOnlyEntry() {
		return this.originalFieldName == null;
	}

	public String getRemappedGetterName() {
		return this.remappedGetterName;
	}

	public String getOriginalBooleanGetterName() {
		return this.originalBooleanGetterName;
	}

	@Override
	public int compareTo(FlowObfuscationEntry o) {
		return this.compareToEntry(o);
	}

	public void setRemappedGetterName(String remappedGetterName) {
		this.remappedGetterName = remappedGetterName;
	}

	public int compareToEntry(FlowObfuscationEntry otherEntry) {
		if (this.originalClassName.equals(otherEntry.originalClassName)) {
			if (this.originalFieldName != null) {
				return otherEntry.originalFieldName != null ? this.originalFieldName.compareTo(otherEntry.originalFieldName) : -1;
			} else {
				return otherEntry.originalFieldName != null ? 1 : 0;
			}
		} else {
			return this.originalClassName.compareTo(otherEntry.originalClassName);
		}
	}

	public String getHumanReadableFieldType() {
		return this.humanReadableFieldType;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FlowObfuscationEntry)) {
			return false;
		} else {
			FlowObfuscationEntry other = (FlowObfuscationEntry)obj;
			return this.originalClassName.equals(other.originalClassName)
				&& (
					this.originalFieldName == null && other.originalFieldName == null && this == obj
						|| this.originalFieldName != null && other.originalFieldName != null && this.originalFieldName.equals(other.originalFieldName)
				);
		}
	}

	public String getInternalClassName() {
		return GlobalUtils.binaryToInternalName(this.originalClassName);
	}

	public void setRemappedBooleanGetterName(String remappedBooleanGetterName) {
		this.remappedBooleanGetterName = remappedBooleanGetterName;
	}

	@Override
	public int hashCode() {
		return this.originalFieldName != null ? this.originalClassName.hashCode() ^ this.originalFieldName.hashCode() : System.identityHashCode(this);
	}

	public String getOriginalFieldDescriptor() {
		return this.originalFieldDescriptor;
	}

	public void setOriginalSetterName(String originalSetterName) {
		this.originalSetterName = originalSetterName;
	}

	public void setRemappedSetterName(String remappedSetterName) {
		this.remappedSetterName = remappedSetterName;
	}

	public String getOriginalClassName() {
		return this.originalClassName;
	}

	public void setOriginalGetterName(String originalGetterName) {
		this.originalGetterName = originalGetterName;
	}

	public boolean isTraceBackClassEntry() {
		return this.packageName == null;
	}

	public String getRemappedBooleanGetterName() {
		return this.remappedBooleanGetterName;
	}

	public boolean hasOriginalSetter() {
		return this.originalSetterName != null;
	}
}

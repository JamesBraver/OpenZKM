package com.zelix.framework.writer.reflect;

public class ReflectionParameter {
	private final int parameterIndex;
	private final ReflectionLookup lookupType;
	private final boolean isArray;
	private final boolean isTargetClass;
	private final boolean isFieldTypeClass;

	ReflectionParameter(int parameterIndex, ReflectionLookup lookupType, boolean isArray, boolean isTargetClass, boolean isFieldTypeClass) {
		this.parameterIndex = parameterIndex;
		this.lookupType = lookupType;
		this.isArray = isArray;
		this.isTargetClass = isTargetClass;
		this.isFieldTypeClass = isFieldTypeClass;
	}

	public boolean isClassNameParameter() {
		return this.lookupType == ReflectionLookup.CLASS_NAME_PARAM_TYPE;
	}

	public boolean isClassOrPropertiesNameParameter() {
		return this.lookupType == ReflectionLookup.CLASS_OR_PROPERTIES_NAME_PARAM_TYPE;
	}

	public boolean isMethodNameParameter() {
		return this.lookupType == ReflectionLookup.METHOD_NAME_PARAM_TYPE;
	}

	public boolean isClassParameter() {
		return this.lookupType == ReflectionLookup.CLASS_PARAM_TYPE;
	}

	public int getParameterIndex() {
		return this.parameterIndex;
	}

	public boolean isPackageNameParameter() {
		return this.lookupType == ReflectionLookup.PACKAGE_NAME_PARAM_TYPE;
	}

	public boolean isArray() {
		return this.isArray;
	}

	public ReflectionLookup getLookupType() {
		return this.lookupType;
	}

	public boolean isFieldNameParameter() {
		return this.lookupType == ReflectionLookup.FIELD_NAME_PARAM_TYPE;
	}

	public boolean isFieldTypeClass() {
		return this.isFieldTypeClass;
	}

	public boolean isTargetClass() {
		return this.isTargetClass;
	}
}

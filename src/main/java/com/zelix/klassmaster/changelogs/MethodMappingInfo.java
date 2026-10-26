package com.zelix.klassmaster.changelogs;

public class MethodMappingInfo {
	private final String newMethodName;
	private final String newMethodSignature;
	private final String parameterChangeData;
	private final boolean parameterChangeDeclared;
	private String parameterChangeLookupData;
	private final boolean parameterObfuscated;
	private final boolean manufactured;

	public MethodMappingInfo(
		String newMethodName,
		String newMethodSignature,
		boolean parameterObfuscated,
		String parameterChangeData,
		String parameterChangeLookupData,
		boolean parameterChangeDeclared,
		boolean manufactured
	) {
		this.newMethodName = newMethodName;
		this.newMethodSignature = newMethodSignature;
		this.parameterChangeData = parameterChangeData;
		this.parameterChangeDeclared = parameterChangeDeclared;
		this.parameterChangeLookupData = parameterChangeLookupData;
		this.parameterObfuscated = parameterObfuscated;
		this.manufactured = manufactured;
	}

	public MethodMappingInfo(String newMethodName) {
		this(newMethodName, "", false, null, null, false, false);
	}

	public MethodMappingInfo(String newMethodName, String newMethodSignature) {
		this(newMethodName, newMethodSignature, false, null, null, false, false);
	}

	public boolean hasParameterChangeData() {
		return this.parameterChangeData != null;
	}

	public String getParameterChangeLookupData() {
		return this.parameterChangeLookupData;
	}

	public boolean isParameterObfuscated() {
		return this.parameterObfuscated;
	}

	public boolean hasParameterChangeLookupData() {
		return this.parameterChangeLookupData != null;
	}

	@Override
	public boolean equals(Object obj) {
		boolean equal;
		if (obj instanceof MethodMappingInfo) {
			MethodMappingInfo other = (MethodMappingInfo)obj;
			if (this.newMethodName.equals(other.newMethodName) && this.newMethodSignature.equals(other.newMethodSignature)) {
				if ((this.parameterChangeData == null || other.parameterChangeData != null)
					&& (this.parameterChangeData != null || other.parameterChangeData == null)
					&& (this.parameterChangeData == null || other.parameterChangeData == null || this.parameterChangeData.equals(other.parameterChangeData))) {
					equal = this.parameterObfuscated == other.parameterObfuscated;
				} else {
					equal = false;
				}
			} else {
				equal = false;
			}
		} else {
			equal = false;
		}

		return equal;
	}

	public String getParameterChangeData() {
		return this.parameterChangeData;
	}

	@Override
	public int hashCode() {
		int hash = this.newMethodName.hashCode() ^ this.newMethodSignature.hashCode();
		if (this.parameterChangeData != null) {
			hash ^= this.parameterChangeData.hashCode();
		}

		if (this.parameterObfuscated) {
			hash ^= 2064239927;
		}

		return hash;
	}

	public String getNewParametersDescriptor() {
		return this.newMethodSignature;
	}

	public String getNewName() {
		return this.newMethodName;
	}

	public final String getNewMethodSignature() {
		StringBuilder signatureBuffer = new StringBuilder();
		signatureBuffer.append(this.newMethodName);
		if (this.hasParameterChangeData() || this.newMethodSignature != null && this.newMethodSignature.length() > 0) {
			signatureBuffer.append('(');
			signatureBuffer.append(this.newMethodSignature);
			signatureBuffer.append(")");
		}

		return signatureBuffer.toString();
	}

	public String getEffectiveParameterList() {
		return this.parameterObfuscated ? "java.lang.Object[]" : this.newMethodSignature;
	}

	public boolean isParameterChangeDeclared() {
		return this.parameterChangeDeclared;
	}
}

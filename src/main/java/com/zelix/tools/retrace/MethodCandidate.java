package com.zelix.tools.retrace;

public class MethodCandidate implements Comparable<MethodCandidate> {
	private String methodName;
	private String[] parameterTypes;
	private String displayName;

	public MethodCandidate(String methodName, String[] parameterTypes) {
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
	}

	public String[] getParameterTypes() {
		return this.parameterTypes;
	}

	public String getMethodName() {
		return this.methodName;
	}

	private String getDisplayName() {
		if (this.displayName == null) {
			StringBuffer displayNameBuffer = new StringBuffer();
			displayNameBuffer.append(this.methodName + "(");
			if (this.parameterTypes != null) {
				for (int i = 0; i < this.parameterTypes.length; i++) {
					displayNameBuffer.append(this.parameterTypes[i]);
					if (i < this.parameterTypes.length - 1) {
						displayNameBuffer.append(",");
					}
				}
			} else {
				displayNameBuffer.append("unknown");
			}

			displayNameBuffer.append(")");
			this.displayName = displayNameBuffer.toString();
		}

		return this.displayName;
	}

	@Override
	public int compareTo(MethodCandidate o) {
		return this.getDisplayName().compareTo(o.getDisplayName());
	}
}

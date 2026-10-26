package com.zelix.klassmaster.changelogs.parser.context;

import com.zelix.ZKMChangeLog;

public class SignatureEntry {
	public final ZKMChangeLog changeLog;
	public String returnType;
	public String methodName;
	public String[] parameterTypes;

	public SignatureEntry(ZKMChangeLog changeLog, String returnType, String methodName, String[] parameterTypes) {
		this.changeLog = changeLog;
		this.returnType = returnType;
		this.methodName = methodName;
		this.parameterTypes = parameterTypes;
	}

	public String getSignature() {
		StringBuilder signatureBuilder = new StringBuilder();
		signatureBuilder.append(this.returnType);
		signatureBuilder.append(' ');
		signatureBuilder.append(this.methodName);
		signatureBuilder.append('(');

		for (int i = 0; i < this.parameterTypes.length; i++) {
			signatureBuilder.append(this.parameterTypes[i]);
			if (i < this.parameterTypes.length - 1) {
				signatureBuilder.append(",");
			}
		}

		signatureBuilder.append(')');
		return signatureBuilder.toString();
	}
}

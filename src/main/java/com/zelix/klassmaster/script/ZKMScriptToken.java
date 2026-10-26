package com.zelix.klassmaster.script;

import com.zelix.utils.DebugUtils;

public class ZKMScriptToken {
	public static final String[] parameterDescriptions = new String[9];
	public ZKMScriptToken specialToken;
	public int endLine;
	public int beginLine;
	public String image;
	public int beginColumn;
	public ZKMScriptToken next;
	public int kind;
	public int endColumn;
	public int descriptionIndex;

	public static final ZKMScriptToken newToken(int kind) {
		switch (kind) {
			default:
				return new ZKMScriptToken();
		}
	}

	public void setDescriptionIndex(int descriptionIndex) {
		this.descriptionIndex = descriptionIndex;
	}

	@Override
	public final String toString() {
		return this.image == null ? "<" + DebugUtils.getSimpleName(this) + " NULL >" : this.image;
	}

	public String getDescription() {
		return parameterDescriptions[this.descriptionIndex];
	}

	static {
		parameterDescriptions[0] = "";
		parameterDescriptions[2] = " \"package name\" parameter";
		parameterDescriptions[3] = " \"class name\" parameter";
		parameterDescriptions[4] = " \"field name\" parameter";
		parameterDescriptions[5] = " \"method name\" parameter";
		parameterDescriptions[6] = " \"link class name\" parameter";
		parameterDescriptions[7] = " \"link search path\" parameter";
		parameterDescriptions[8] = " \"link method name\" parameter";
	}
}

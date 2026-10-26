package com.zelix.tools.proguardConfigurationTranslate;

import com.zelix.utils.DebugUtils;
import java.io.Serializable;

public class ProguardToken implements Serializable {
	public int kind;
	public String image;
	public ProguardToken specialToken;
	public int beginColumn;
	public ProguardToken next;
	public int beginLine;
	public int endLine;
	public int endColumn;

	public ProguardToken() {
	}

	public ProguardToken(int kind, String image) {
		this.kind = kind;
		this.image = image;
	}

	public static ProguardToken newToken(int kind) {
		return newToken(kind, null);
	}

	public static ProguardToken newToken(int kind, String image) {
		switch (kind) {
			default:
				return new ProguardToken(kind, image);
		}
	}

	@Override
	public String toString() {
		return this.image == null ? "<" + DebugUtils.getSimpleName(this) + " NULL >" : this.image;
	}
}

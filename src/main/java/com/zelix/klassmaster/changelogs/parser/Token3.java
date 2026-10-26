package com.zelix.klassmaster.changelogs.parser;

import com.zelix.utils.DebugUtils;
import java.io.Serializable;

public class Token3 implements Serializable {
	public int kind;
	public String image;
	public int beginColumn;
	public int endColumn;
	public int beginLine;
	public int endLine;
	public Token3 specialToken;
	public Token3 next;

	public Token3() {
	}

	public Token3(int kind, String image) {
		this.kind = kind;
		this.image = image;
	}

	public static Token3 newToken(int kind) {
		return newToken(kind, null);
	}

	public static Token3 newToken(int kind, String image) {
		switch (kind) {
			default:
				return new Token3(kind, image);
		}
	}

	@Override
	public String toString() {
		return this.image == null ? "<" + DebugUtils.getSimpleName(this) + " NULL >" : this.image;
	}
}

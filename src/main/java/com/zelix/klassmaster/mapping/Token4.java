package com.zelix.klassmaster.mapping;

import com.zelix.utils.DebugUtils;
import java.io.Serializable;

public class Token4 implements Serializable {
	public int kind;
	public String image;
	public int endColumn;
	public Token4 specialToken;
	public int endLine;
	public Token4 next;
	public int beginLine;
	public int beginColumn;

	public Token4(int kind, String image) {
		this.kind = kind;
		this.image = image;
	}

	public Token4() {
	}

	public static Token4 newToken(int kind, String image) {
		switch (kind) {
			default:
				return new Token4(kind, image);
		}
	}

	@Override
	public String toString() {
		return this.image == null ? "<" + DebugUtils.getSimpleName(this) + " NULL >" : this.image;
	}
}

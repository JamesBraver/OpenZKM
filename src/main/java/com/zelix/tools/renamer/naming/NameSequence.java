package com.zelix.tools.renamer.naming;

public class NameSequence {
	private final char[] chars;
	private final String id;
	public NameSequence parentSequence;
	private int charIndex;
	public NameSequence childSequence;

	public NameSequence(char[] chars, String id) {
		this.chars = chars;
		this.id = id;
	}

	public NameSequence(char[] chars) {
		this(chars, "");
	}

	public StringBuilder appendName(StringBuilder builder) {
		builder.append(this.chars[this.charIndex]);
		if (this.childSequence != null) {
			this.childSequence.appendName(builder);
		}

		return builder;
	}

	public void setParent(NameSequence parent) {
		if (parent == this) {
			throw new IllegalArgumentException("Cannot specify same Object");
		} else {
			this.parentSequence = parent;
			parent.setChild(this);
		}
	}

	public boolean advance() {
		this.charIndex = ++this.charIndex % this.chars.length;
		if (this.charIndex == 0) {
			return this.parentSequence != null ? this.parentSequence.advance() : true;
		} else {
			return false;
		}
	}

	private void setChild(NameSequence child) {
		this.childSequence = child;
	}
}

package com.zelix.tools.proguardConfigurationTranslate;

public class CommentStripper implements Stripper {
	@Override
	public String stripComment(String line) {
		int commentStart = line.indexOf("//");
		return commentStart > -1 ? line.substring(0, commentStart) : line;
	}
}

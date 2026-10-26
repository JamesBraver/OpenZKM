package com.zelix.tools.renamer.dictionary;

import com.zelix.utils.GlobalUtils;
import java.util.Set;

public class JavaKeywords {
	public static final Set<String> keywords = GlobalUtils.newHashSet();

	static {
		keywords.add("_");
		keywords.add("abstract");
		keywords.add("assert");
		keywords.add("boolean");
		keywords.add("break");
		keywords.add("byte");
		keywords.add("case");
		keywords.add("catch");
		keywords.add("char");
		keywords.add("class");
		keywords.add("const");
		keywords.add("continue");
		keywords.add("default");
		keywords.add("do");
		keywords.add("double");
		keywords.add("else");
		keywords.add("enum");
		keywords.add("extends");
		keywords.add("false");
		keywords.add("final");
		keywords.add("finally");
		keywords.add("float");
		keywords.add("for");
		keywords.add("goto");
		keywords.add("if");
		keywords.add("implements");
		keywords.add("import");
		keywords.add("instanceof");
		keywords.add("int");
		keywords.add("interface");
		keywords.add("long");
		keywords.add("main");
		keywords.add("native");
		keywords.add("new");
		keywords.add("non-sealed");
		keywords.add("null");
		keywords.add("package");
		keywords.add("permits");
		keywords.add("private");
		keywords.add("protected");
		keywords.add("public");
		keywords.add("return");
		keywords.add("sealed");
		keywords.add("short");
		keywords.add("static");
		keywords.add("strictfp");
		keywords.add("super");
		keywords.add("switch");
		keywords.add("synchronized");
		keywords.add("this");
		keywords.add("throw");
		keywords.add("throws");
		keywords.add("transient");
		keywords.add("true");
		keywords.add("try");
		keywords.add("var");
		keywords.add("void");
		keywords.add("volatile");
		keywords.add("while");
		keywords.add("yield");
	}
}

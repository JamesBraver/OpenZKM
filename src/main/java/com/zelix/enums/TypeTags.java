package com.zelix.enums;

public enum TypeTags {
	UTF8(1),
	INTEGER(3),
	FLOAT(4),
	LONG(5),
	DOUBLE(6),
	CLASS(7),
	STRING(8),
	FIELDREF(9),
	METHODREF(10),
	INTERFACE_METHODREF(11),
	NAME_AND_TYPE(12),
	METHOD_HANDLE(15),
	METHOD_TYPE(16),
	CONSTANT_DYNAMIC(17),
	INVOKE_DYNAMIC(18),
	MODULE(19),
	PACKAGE(20),
	NULL(0);

	public final int tag;

	private TypeTags(int tag) {
		this.tag = tag;
	}

	public int getTagID() {
		return this.tag;
	}
}

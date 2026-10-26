package com.zelix.framework.writer.base;

import com.zelix.enums.TypeTags;

public abstract class BaseMethodTypeWriter extends TypeWriter {
	public static final TypeTags METHOD_TYPE = TypeTags.METHOD_TYPE;

	public BaseMethodTypeWriter(int tag, AbstractConstantPoolWriter constantPool) {
		super(tag, constantPool);
	}

	@Override
	public TypeTags getTypeTag() {
		return METHOD_TYPE;
	}
}

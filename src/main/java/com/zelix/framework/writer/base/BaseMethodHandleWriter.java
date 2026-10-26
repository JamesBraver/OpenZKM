package com.zelix.framework.writer.base;

import com.zelix.enums.TypeTags;

public abstract class BaseMethodHandleWriter extends TypeWriter {
	public static final TypeTags TYPE_TAG = TypeTags.METHOD_HANDLE;

	public BaseMethodHandleWriter(int tag, AbstractConstantPoolWriter constantPool) {
		super(tag, constantPool);
	}

	@Override
	public final TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}

package com.zelix.framework.writer.base;

import com.zelix.enums.TypeTags;

public abstract class AbstractClassInfoWriter extends TypeWriter {
	public static final TypeTags TYPE_TAG = TypeTags.CLASS;

	public AbstractClassInfoWriter(int tag, AbstractConstantPoolWriter constantPool) {
		super(tag, constantPool);
	}

	public abstract void setName(String newName);

	public abstract String getInternalName();

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}

	public abstract String getExternalName();
}

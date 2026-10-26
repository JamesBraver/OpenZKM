package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.AbstractMemberRefInfoWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;

public class FieldRefInfoWriter extends AbstractMemberRefInfoWriter implements Writer {
	public static final TypeTags TYPE_TAG = TypeTags.FIELDREF;

	public FieldRefInfoWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, dataBuffer, constantPool);
	}

	@Override
	public MemberTypeWriter createResolvedMemberWriter(
		AbstractMemberRefInfoWriter rawMemberRefWriter, ClassInfoWriter owner, NameAndTypeWriter target, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) {
		return new FieldTypeWriter(rawMemberRefWriter, owner, target, classReferences);
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}

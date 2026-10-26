package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.BaseDynamicInfoWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.io.PrintWriter;

public class ConstantDynamicInfoWriter extends BaseDynamicInfoWriter {
	public static final TypeTags TAG = TypeTags.CONSTANT_DYNAMIC;

	public ConstantDynamicInfoWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, dataBuffer, constantPool);
	}

	@Override
	public TypeWriter resolve(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	) {
		return this.createResolvedWriter(utf8References, nameAndTypeReferences, classReferences, memberReferences, errorWriter);
	}

	public DynamicConstantWriter createResolvedWriter(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	) {
		try {
			TypeWriter nameAndTypeEntry = this.constantPool.getConstantPoolEntry(super.nameAndTypeIndex);
			if (nameAndTypeEntry.isResolvable()) {
				return null;
			} else if (!(nameAndTypeEntry instanceof NameAndTypeWriter)) {
				String invalidIndexMessage = this.constantPool.getOwnerSourcePath()
					+ " : Invalid Constant Pool name and type index : "
					+ super.nameAndTypeIndex
					+ " : "
					+ TypeWriter.getConstantPoolTagName(nameAndTypeEntry.getTypeTag())
					+ " : "
					+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
					+ " entry at "
					+ this.getTag();
				throw new ClassParseFailedException(invalidIndexMessage);
			} else {
				DynamicConstantWriter dynamicConstantWriter = new DynamicConstantWriter(this.getTag(), this.constantPool, super.bootstrapMethodIndex, (NameAndTypeWriter)nameAndTypeEntry);
				if (nameAndTypeReferences != null) {
					nameAndTypeReferences.put((NameAndTypeWriter)nameAndTypeEntry, dynamicConstantWriter);
				}

				return dynamicConstantWriter;
			}
		} catch (ArrayIndexOutOfBoundsException ignored) {
			String outOfRangeMessage = this.constantPool.getOwnerSourcePath()
				+ " : Constant Pool index out of range : "
				+ ignored.getMessage()
				+ " : "
				+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
				+ " entry";
			throw new ClassParseFailedException(outOfRangeMessage);
		}
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}

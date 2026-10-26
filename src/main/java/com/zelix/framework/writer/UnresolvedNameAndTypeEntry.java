package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

//CONSTANT_NameAndType_info {
//    u1 tag;
//    u2 name_index;
//    u2 descriptor_index;
//}
public class UnresolvedNameAndTypeEntry extends TypeWriter implements Writer {
	public static final TypeTags TYPE_TAG = TypeTags.NAME_AND_TYPE;
	public int nameIndex;
	public int descriptorIndex;

	public UnresolvedNameAndTypeEntry(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.nameIndex = dataBuffer.readUnsignedShort();
		this.descriptorIndex = dataBuffer.readUnsignedShort();
	}

	@Override
	public TypeWriter resolve(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	) {
		try {
			TypeWriter nameUtf8Entry = this.constantPool.getConstantPoolEntry(this.nameIndex);
			if (!(nameUtf8Entry instanceof UTFStringWriter)) {
				String invalidNameIndexMessage = this.constantPool.getOwnerRawSourcePath()
					+ " : Invalid Constant Pool name index : "
					+ this.nameIndex
					+ " : "
					+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
					+ " entry";
				throw new ClassParseFailedException(invalidNameIndexMessage);
			} else {
				TypeWriter descriptorUtf8Entry = this.constantPool.getConstantPoolEntry(this.descriptorIndex);
				if (!(this.constantPool.getConstantPoolEntry(this.descriptorIndex) instanceof UTFStringWriter)) {
					String invalidDescriptorIndexMessage = this.constantPool.getOwnerRawSourcePath()
						+ " : Invalid Constant Pool descriptor index : "
						+ this.descriptorIndex
						+ " : "
						+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
						+ " entry";
					throw new ClassParseFailedException(invalidDescriptorIndexMessage);
				} else {
					String descriptor = ((UTFStringWriter)descriptorUtf8Entry).getValue();
					if (TypeWriter.descriptorToHumanReadable(descriptor, true) == null) {
						String invalidTypeDescriptorMessage = this.constantPool.getOwnerRawSourcePath()
							+ " : Invalid type descriptor : '"
							+ descriptor
							+ "' : "
							+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
							+ " entry";
						throw new ClassParseFailedException(invalidTypeDescriptorMessage);
					} else {
						NameAndTypeWriter nameAndTypeWriter = new NameAndTypeWriter(this, (UTFStringWriter)nameUtf8Entry, (UTFStringWriter)descriptorUtf8Entry);
						if (utf8References != null) {
							utf8References.put((UTFStringWriter)nameUtf8Entry, nameAndTypeWriter);
							utf8References.put((UTFStringWriter)descriptorUtf8Entry, nameAndTypeWriter);
						}

						return nameAndTypeWriter;
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException ignored) {
			String outOfRangeMessage = this.constantPool.getOwnerRawSourcePath()
				+ " : Constant Pool index out of range : "
				+ ignored.getMessage()
				+ " : "
				+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
				+ " entry";
			throw new ClassParseFailedException(outOfRangeMessage);
		}
	}

	@Override
	public boolean isResolvable() {
		return true;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TYPE_TAG.getTagID());
		dos.writeShort(this.nameIndex);
		dos.writeShort(this.descriptorIndex);
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}

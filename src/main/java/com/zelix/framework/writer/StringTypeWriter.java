package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.constant.StringNode;
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

public class StringTypeWriter extends TypeWriter implements Writer {
	public static final TypeTags TAG = TypeTags.STRING;
	public int stringIndex;

	public StringTypeWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.stringIndex = dataBuffer.readUnsignedShort();
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
			TypeWriter utf8Entry = this.constantPool.getConstantPoolEntry(this.stringIndex);
			if (utf8Entry instanceof UTFStringWriter) {
				return new StringNode(this, (UTFStringWriter)utf8Entry, utf8References);
			} else {
				String invalidIndexMessage = this.constantPool.getOwnerRawSourcePath()
					+ " : Invalid Constant Pool string index : "
					+ this.stringIndex
					+ " : "
					+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
					+ " entry";
				throw new ClassParseFailedException(invalidIndexMessage);
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
		dos.writeByte(TAG.getTagID());
		dos.writeShort(this.stringIndex);
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}

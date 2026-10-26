package com.zelix.framework.writer;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.base.AbstractClassInfoWriter;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class RawClassInfoWriter extends AbstractClassInfoWriter implements Writer {
	public int nameIndex;

	public RawClassInfoWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.nameIndex = dataBuffer.readUnsignedShort();
	}

	@Override
	public void setName(String newName) {
		((UTFStringWriter)this.constantPool.getConstantPoolEntry(this.nameIndex)).setValue(newName);
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
			if (nameUtf8Entry instanceof UTFStringWriter) {
				return new ClassInfoWriter(this, (UTFStringWriter)nameUtf8Entry, utf8References);
			} else {
				String invalidIndexMessage = this.constantPool.getOwnerRawSourcePath()
					+ " : Invalid Constant Pool name index : "
					+ this.nameIndex
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
		dos.writeByte(AbstractClassInfoWriter.TYPE_TAG.getTagID());
		dos.writeShort(this.nameIndex);
	}

	@Override
	public String getInternalName() {
		return ((UTFStringWriter)this.constantPool.getConstantPoolEntry(this.nameIndex)).getValue();
	}

	@Override
	public String getExternalName() {
		return ((UTFStringWriter)this.constantPool.getConstantPoolEntry(this.nameIndex)).getValue().replace('/', '.');
	}
}

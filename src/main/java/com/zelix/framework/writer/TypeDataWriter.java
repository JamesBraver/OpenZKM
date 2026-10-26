package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import java.io.DataOutputStream;
import java.io.IOException;

public class TypeDataWriter extends TypeWriter {
	private TypeTags typeTag;
	public int slotSize;
	public byte[] rawBytes;

	public TypeDataWriter(int tag, AbstractConstantPoolWriter constantPool) {
		super(tag, constantPool);
	}

	@Override
	public String getValueAsString() {
		return this.getClass().getName() + " tag=" + this.typeTag;
	}

	@Override
	public boolean isResolutionValid() {
		return false;
	}

	@Override
	public int getSlotSize() {
		return this.slotSize;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.write(this.rawBytes);
	}

	@Override
	public TypeTags getTypeTag() {
		return this.typeTag;
	}
}

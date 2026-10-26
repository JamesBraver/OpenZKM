package com.zelix.framework.writer.base;

import com.zelix.framework.writer.Writer;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class BaseDynamicInfoWriter extends BaseDynamicWriter implements Writer {
	public final int bootstrapMethodIndex;
	public final int nameAndTypeIndex;

	public BaseDynamicInfoWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.bootstrapMethodIndex = dataBuffer.readUnsignedShort();
		this.nameAndTypeIndex = dataBuffer.readUnsignedShort();
	}

	@Override
	public boolean isResolvable() {
		return true;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(this.getTypeTag().getTagID());
		dos.writeShort(this.bootstrapMethodIndex);
		dos.writeShort(this.nameAndTypeIndex);
	}
}

package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public class UTFStringWriter extends TypeWriter implements IVisitable, StringableConstant {
	public static final TypeTags TYPE_TAG = TypeTags.UTF8;
	public String value;

	public UTFStringWriter(int tag, AbstractConstantPoolWriter constantPool, String value) {
		super(tag, constantPool);
		this.value = value;
	}

	public UTFStringWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		byte[] utf8Bytes = new byte[dataBuffer.readUnsignedShort()];
		dataBuffer.read(utf8Bytes);
		this.value = GlobalUtils.fromUTF8(utf8Bytes);
		this.value = this.value.intern();
	}

	@Override
	public String getValueAsString() {
		return this.getValue();
	}

	@Override
	public String getAsString() {
		return this.value;
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public String getValue() {
		return this.value;
	}

	public final int getLength() {
		return this.value.length();
	}

	@Override
	public String toLoggableString() {
		String escapedValue = GlobalUtils.escapeString(this.getValueAsString());
		if (escapedValue.length() > 200) {
			escapedValue = escapedValue.substring(0, 50) + "...<" + (escapedValue.length() - 100) + " CHARS>..." + escapedValue.substring(escapedValue.length() - 50);
		}

		return escapedValue;
	}

	public void setValue(String value) {
		this.value = value;
		this.value = this.value.intern();
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TYPE_TAG.getTagID());
		byte[] utf8Bytes = GlobalUtils.toUTF8(this.value);
		DebugUtils.assertTrue(
			utf8Bytes.length <= 65535, new String[]{"String too long in class '" + this.getOwnerSourcePath() + "' : " + utf8Bytes.length + " : " + this.value.length()}
		);
		dos.writeShort(utf8Bytes.length);
		dos.write(utf8Bytes);
	}

	public UTFStringWriter copy(int constantPoolIndex) {
		return new UTFStringWriter(constantPoolIndex, this.constantPool, this.value);
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}

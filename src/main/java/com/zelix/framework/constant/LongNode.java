package com.zelix.framework.constant;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.writer.StringableConstant;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public class LongNode extends ConstantWriter implements StringableConstant {
	public static final TypeTags TAG = TypeTags.LONG;
	public long value;
	private boolean needLongObf;

	public LongNode(int tag, AbstractConstantPoolWriter constantPool, long value, boolean needLongObf) {
		super(tag, constantPool);
		this.value = value;
		this.needLongObf = needLongObf;
	}

	public LongNode(int tag, AbstractConstantPoolWriter constantPool, LongNode sourceNode) {
		super(tag, constantPool);
		this.value = sourceNode.value;
		this.needLongObf = sourceNode.needLongObf;
	}

	public LongNode(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.value = dataBuffer.readLong();
		this.needLongObf = constantPool.shouldObfuscateConstants();
	}

	public LongNode(int tag, AbstractConstantPoolWriter constantPool, long value) {
		super(tag, constantPool);
		this.value = value;
		this.needLongObf = false;
	}

	@Override
	public String getTypeName() {
		return "long";
	}

	@Override
	public String getValueAsString() {
		return String.valueOf(this.value);
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	@Override
	public String getAsString() {
		return String.valueOf(this.value);
	}

	@Override
	public int getSlotSize() {
		return 2;
	}

	public boolean isEqual(long value) {
		return this.value == value;
	}

	@Override
	public String getDisplayValue() {
		return this.getValueAsString();
	}

	public long getValue() {
		return this.value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	@Override
	public void setValueFromString(String valueAsString) {
		long parsedValue;
		try {
			parsedValue = Long.valueOf(valueAsString);
		} catch (NumberFormatException ignoredLongParse) {
			throw new ValidationException(valueAsString + " is not a valid long");
		}

		this.value = parsedValue;
		this.notifySimpleChange();
	}

	public boolean isNeedLongObf() {
		return this.needLongObf;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TAG.getTagID());
		dos.writeLong(this.value);
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}

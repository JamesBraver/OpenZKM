package com.zelix.framework.constant;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.writer.StringableConstant;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public class IntegerNode extends ConstantWriter implements StringableConstant {
	public static final TypeTags TAG = TypeTags.INTEGER;
	public int value;
	private boolean needIntObf;

	public IntegerNode(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.value = dataBuffer.readInt();
		this.needIntObf = constantPool.shouldObfuscateConstants();
	}

	public IntegerNode(int tag, AbstractConstantPoolWriter constantPool, IntegerNode sourceNode) {
		super(tag, constantPool);
		this.value = sourceNode.value;
		this.needIntObf = sourceNode.needIntObf;
	}

	public IntegerNode(int tag, AbstractConstantPoolWriter constantPool, int value, boolean needIntObf) {
		super(tag, constantPool);
		this.value = value;
		this.needIntObf = needIntObf;
	}

	public boolean isEqual(int value) {
		return this.value == value;
	}

	public boolean isNeedIntObf() {
		return this.needIntObf;
	}

	@Override
	public String getTypeName() {
		return "integer";
	}

	@Override
	public String getValueAsString() {
		return String.valueOf(this.value);
	}

	@Override
	public String getAsString() {
		return String.valueOf(this.value);
	}

	@Override
	public boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent) {
		return visitor.visit(this, instance, parent);
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String getDisplayValue() {
		return this.getValueAsString();
	}

	public int getValue() {
		return this.value;
	}

	@Override
	public void setValueFromString(String valueAsString) {
		int parsedValue;
		try {
			parsedValue = Integer.parseInt(valueAsString);
		} catch (NumberFormatException ignoredIntegerParse) {
			throw new ValidationException(valueAsString + " is not a valid integer");
		}

		this.value = parsedValue;
		this.notifySimpleChange();
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TAG.getTagID());
		dos.writeInt(this.value);
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}

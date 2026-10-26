package com.zelix.framework.constant;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public class DoubleNode extends ConstantWriter {
	public static final TypeTags TAG = TypeTags.DOUBLE;
	public double value;

	public DoubleNode(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.value = dataBuffer.readDouble();
	}

	@Override
	public String getTypeName() {
		return "double";
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
	public int getSlotSize() {
		return 2;
	}

	@Override
	public String getDisplayValue() {
		return this.getValueAsString();
	}

	@Override
	public void setValueFromString(String valueAsString) {
		double parsedValue;
		try {
			parsedValue = Double.valueOf(valueAsString);
		} catch (NumberFormatException ignoredDoubleParse) {
			throw new ValidationException(valueAsString + " is not a valid double");
		}

		this.value = parsedValue;
		this.notifySimpleChange();
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TAG.getTagID());
		dos.writeDouble(this.value);
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}

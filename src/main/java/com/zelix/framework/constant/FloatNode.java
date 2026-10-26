package com.zelix.framework.constant;

import com.zelix.enums.TypeTags;
import com.zelix.exceptions.ValidationException;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.ConstantWriter;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public class FloatNode extends ConstantWriter {
	public static final TypeTags TAG = TypeTags.FLOAT;
	public float value;

	public FloatNode(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.value = dataBuffer.readFloat();
	}

	@Override
	public String getTypeName() {
		return "float";
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
	public String getDisplayValue() {
		return this.getValueAsString();
	}

	@Override
	public void setValueFromString(String valueAsString) {
		float parsedValue;
		try {
			parsedValue = Float.valueOf(valueAsString);
		} catch (NumberFormatException ignoredFloatParse) {
			throw new ValidationException(valueAsString + " is not a valid float");
		}

		this.value = parsedValue;
		this.notifySimpleChange();
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(TAG.getTagID());
		dos.writeFloat(this.value);
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}

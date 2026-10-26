package com.zelix.framework.constant;

import com.zelix.framework.writer.IVisitable;

public interface ConstantNode extends IVisitable {
	String getTypeName();

	@Override
	boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent);

	String getDisplayValue();

	void setValueFromString(String valueAsString);
}

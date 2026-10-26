package com.zelix.framework.constant;

import com.zelix.framework.writer.base.TypeWriter;

public interface ConstantPool {
	TypeWriter getConstantPoolEntry(int index);
}

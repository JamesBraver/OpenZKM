package com.zelix.framework.writer;

import com.zelix.framework.constant.ConstantPoolVisitor;

public interface IVisitable {
	boolean accept(ConstantPoolVisitor visitor, Object instance, Object parent);
}

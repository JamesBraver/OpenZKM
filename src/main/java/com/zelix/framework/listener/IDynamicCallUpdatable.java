package com.zelix.framework.listener;

import com.zelix.framework.writer.InvokeDynamicCallWriter;

public interface IDynamicCallUpdatable {
	void updateDynamicCall(InvokeDynamicCallWriter oldReference, InvokeDynamicCallWriter newReference);
}

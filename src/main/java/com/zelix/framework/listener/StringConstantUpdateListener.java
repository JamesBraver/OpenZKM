package com.zelix.framework.listener;

import com.zelix.framework.constant.StringNode;

public interface StringConstantUpdateListener {
	void updateStringConstant(StringNode oldReference, StringNode newReference);
}

package com.zelix.framework.listener;

import com.zelix.framework.writer.UTFStringWriter;

public interface SimpleReferenceListener {
	void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference);
}

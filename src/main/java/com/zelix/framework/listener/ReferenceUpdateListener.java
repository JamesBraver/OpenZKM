package com.zelix.framework.listener;

import com.zelix.framework.writer.ClassInfoWriter;

public interface ReferenceUpdateListener {
	void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference);
}
